package com.czx.wenshu.application.consistency;

import com.czx.wenshu.application.llm.JsonExtractor;
import com.czx.wenshu.application.llm.LlmClient;
import com.czx.wenshu.application.llm.PromptTemplate;
import com.czx.wenshu.application.task.AsyncTaskService;
import com.czx.wenshu.domain.consistency.ConsistencyReportItem;
import com.czx.wenshu.domain.consistency.ConsistencyReportItemRepository;
import com.czx.wenshu.domain.project.Chapter;
import com.czx.wenshu.domain.project.Character;
import com.czx.wenshu.domain.project.CharacterRepository;
import com.czx.wenshu.domain.project.ChapterRepository;
import com.czx.wenshu.domain.project.Project;
import com.czx.wenshu.domain.project.ProjectRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 一致性审查异步执行器（P6-06 / P0-2 修复）。
 *
 * <p>采用分层 LLM 策略：</p>
 * <ol>
 *   <li>使用 {@code utilityLlmClient}（DeepSeek）对全量章节内容进行批量扫描，成本低。</li>
 *   <li>对扫描出的疑似问题，使用 {@code creativeLlmClient}（Claude）逐条做二次验证，
 *       最多验证 {@code MAX_CLAUDE_VERIFICATIONS} 条，确保精准度。</li>
 * </ol>
 */
@Component
public class ConsistencyTaskRunner {

    private static final Logger log = LoggerFactory.getLogger(ConsistencyTaskRunner.class);
    private static final TypeReference<List<ConsistencyItemJson>> ITEM_LIST_TYPE = new TypeReference<>() {};
    /** 每次审查的章节内容最大截取字符数（避免超出 LLM 上下文）。 */
    private static final int MAX_CONTENT_PER_CHAPTER = 500;
    /** 每次审查中 Claude 二次验证的最大条数（P0-2 修复）。 */
    private static final int MAX_CLAUDE_VERIFICATIONS = 5;

    private final AsyncTaskService asyncTaskService;
    private final ConsistencyReportItemRepository itemRepository;
    private final ProjectRepository projectRepository;
    private final ChapterRepository chapterRepository;
    private final CharacterRepository characterRepository;
    /** 主扫描客户端：DeepSeek（低成本、批量）。 */
    private final LlmClient utilityLlmClient;
    /** 验证客户端：Claude（高精度、按需）。 */
    private final LlmClient creativeLlmClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * 构造一致性审查执行器。
     *
     * @param asyncTaskService    异步任务服务
     * @param itemRepository      审查条目仓储
     * @param projectRepository   作品仓储
     * @param chapterRepository   章节仓储
     * @param characterRepository 角色仓储
     * @param utilityLlmClient    实用 LLM 客户端（DeepSeek，主扫描）
     * @param creativeLlmClient   创意 LLM 客户端（Claude，二次验证）
     * @param objectMapper        JSON 序列化器
     * @param clock               时钟
     */
    public ConsistencyTaskRunner(AsyncTaskService asyncTaskService,
                                  ConsistencyReportItemRepository itemRepository,
                                  ProjectRepository projectRepository,
                                  ChapterRepository chapterRepository,
                                  CharacterRepository characterRepository,
                                  @Qualifier("utilityLlmClient") LlmClient utilityLlmClient,
                                  @Qualifier("creativeLlmClient") LlmClient creativeLlmClient,
                                  ObjectMapper objectMapper,
                                  Clock clock) {
        this.asyncTaskService = asyncTaskService;
        this.itemRepository = itemRepository;
        this.projectRepository = projectRepository;
        this.chapterRepository = chapterRepository;
        this.characterRepository = characterRepository;
        this.utilityLlmClient = utilityLlmClient;
        this.creativeLlmClient = creativeLlmClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * 异步执行一致性审查（P6-06 / P0-2 修复）。
     *
     * <p>第一阶段：DeepSeek 批量扫描全量章节内容，找出疑似问题。</p>
     * <p>第二阶段：Claude 对疑似问题逐条二次验证（最多 {@code MAX_CLAUDE_VERIFICATIONS} 条）。</p>
     *
     * @param taskId    进度任务 ID
     * @param reportId  AI 操作日志 ID（即报告 ID）
     * @param projectId 目标作品 ID
     */
    @Async("aiTaskExecutor")
    @Transactional
    public void run(UUID taskId, UUID reportId, UUID projectId) {
        log.info("[ConsistencyTaskRunner] 开始一致性审查 taskId={} projectId={}", taskId, projectId);
        try {
            asyncTaskService.markRunning(taskId, 4, "收集作品内容");

            Project project = projectRepository.findById(projectId).orElse(null);
            String synopsis = project != null && project.synopsis() != null ? project.synopsis() : "";

            // 收集角色列表（最多 10 个角色，避免 Prompt 过长）
            List<Character> characters = characterRepository.findByProjectId(projectId);
            String charNames = characters.stream().limit(10)
                    .map(Character::name).collect(Collectors.joining("、"));

            // 收集近期章节内容（最多 5 章，每章截取前 MAX_CONTENT_PER_CHAPTER 字）
            List<Chapter> chapters = chapterRepository.findByProjectId(projectId);
            String content = chapters.stream().limit(5)
                    .map(c -> "【" + c.title() + "】" + safeSubstring(c.content(), MAX_CONTENT_PER_CHAPTER))
                    .collect(Collectors.joining("\n---\n"));

            // ── 第一阶段：DeepSeek 批量扫描 ──────────────────────────────────
            asyncTaskService.updateProgress(taskId, 1, "DeepSeek 批量扫描", 30);
            PromptTemplate tpl = PromptTemplate.fromClasspath("prompts/consistency_check.txt");
            String prompt = tpl.fill(Map.of(
                    "synopsis", synopsis,
                    "characters", charNames.isEmpty() ? "（暂无角色档案）" : charNames,
                    "content", content.isEmpty() ? "（暂无内容）" : content));

            String response = utilityLlmClient.chat(null, prompt);
            log.debug("[ConsistencyTaskRunner] DeepSeek 扫描完成 projectId={}", projectId);

            asyncTaskService.updateProgress(taskId, 2, "解析疑似问题", 55);
            List<ConsistencyItemJson> suspectedItems = JsonExtractor.parseArray(response, ITEM_LIST_TYPE, objectMapper);

            // ── 第二阶段：Claude 逐条二次验证（最多 MAX_CLAUDE_VERIFICATIONS 条）──
            List<ConsistencyItemJson> verifiedItems = new ArrayList<>();
            if (suspectedItems != null && !suspectedItems.isEmpty()) {
                asyncTaskService.updateProgress(taskId, 3, "Claude 二次验证", 75);
                int verifyCount = 0;
                for (ConsistencyItemJson item : suspectedItems) {
                    if (item.description() == null || item.description().isBlank()) continue;
                    if (verifyCount < MAX_CLAUDE_VERIFICATIONS) {
                        boolean confirmed = verifyWithClaude(item, synopsis, charNames);
                        if (confirmed) {
                            verifiedItems.add(item);
                        }
                        verifyCount++;
                        log.debug("[ConsistencyTaskRunner] Claude 验证条目 {}/{} confirmed={}",
                                verifyCount, MAX_CLAUDE_VERIFICATIONS, confirmed);
                    } else {
                        // 超出验证上限，直接采纳 DeepSeek 结果
                        verifiedItems.add(item);
                    }
                }
            }

            asyncTaskService.updateProgress(taskId, 4, "结果入库", 90);
            int savedCount = 0;
            for (ConsistencyItemJson json : verifiedItems) {
                ConsistencyReportItem item = ConsistencyReportItem.create(
                        reportId, projectId, json.type(), json.character(),
                        json.chapterHint(), json.description(), json.suggestion(), clock);
                itemRepository.save(item);
                savedCount++;
            }

            asyncTaskService.completeWithJson(taskId,
                    "{\"reportId\":\"" + reportId + "\",\"items\":" + savedCount + "}");
            log.info("[ConsistencyTaskRunner] 审查完成 projectId={} 发现问题数={}", projectId, savedCount);
        } catch (Exception e) {
            log.warn("[ConsistencyTaskRunner] 审查失败 taskId={}", taskId, e);
            asyncTaskService.fail(taskId, e.getMessage());
        }
    }

    /**
     * 使用 Claude 对单条疑似问题进行二次验证。
     *
     * @param item      疑似问题条目
     * @param synopsis  作品简介
     * @param charNames 角色名称列表
     * @return true 表示 Claude 确认该问题存在
     */
    private boolean verifyWithClaude(ConsistencyItemJson item, String synopsis, String charNames) {
        try {
            String verifyPrompt = String.format(
                    "请判断以下一致性问题是否确实存在（只回答 YES 或 NO）：\n" +
                    "作品简介：%s\n角色：%s\n疑似问题：%s\n描述：%s",
                    synopsis, charNames, item.type(), item.description());
            String result = creativeLlmClient.chat(null, verifyPrompt);
            return result != null && result.trim().toUpperCase().startsWith("YES");
        } catch (Exception e) {
            log.warn("[ConsistencyTaskRunner] Claude 验证失败，默认采纳 DeepSeek 结果", e);
            return true;
        }
    }

    private String safeSubstring(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "…";
    }
}
