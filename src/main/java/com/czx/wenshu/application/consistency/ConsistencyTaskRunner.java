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
 * 一致性审查异步执行器（P6-06）。
 * 收集作品内容，调用 LLM 分析，将发现的问题写入审查条目表。
 */
@Component
public class ConsistencyTaskRunner {

    private static final Logger log = LoggerFactory.getLogger(ConsistencyTaskRunner.class);
    private static final TypeReference<List<ConsistencyItemJson>> ITEM_LIST_TYPE = new TypeReference<>() {};
    /** 每次审查的章节内容最大截取字符数（避免超出 LLM 上下文）。 */
    private static final int MAX_CONTENT_PER_CHAPTER = 500;

    private final AsyncTaskService asyncTaskService;
    private final ConsistencyReportItemRepository itemRepository;
    private final ProjectRepository projectRepository;
    private final ChapterRepository chapterRepository;
    private final CharacterRepository characterRepository;
    private final LlmClient creativeLlmClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ConsistencyTaskRunner(AsyncTaskService asyncTaskService,
                                  ConsistencyReportItemRepository itemRepository,
                                  ProjectRepository projectRepository,
                                  ChapterRepository chapterRepository,
                                  CharacterRepository characterRepository,
                                  @Qualifier("creativeLlmClient") LlmClient creativeLlmClient,
                                  ObjectMapper objectMapper,
                                  Clock clock) {
        this.asyncTaskService = asyncTaskService;
        this.itemRepository = itemRepository;
        this.projectRepository = projectRepository;
        this.chapterRepository = chapterRepository;
        this.characterRepository = characterRepository;
        this.creativeLlmClient = creativeLlmClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * 异步执行一致性审查（P6-06）。
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
            asyncTaskService.markRunning(taskId, 3, "收集作品内容");

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

            asyncTaskService.updateProgress(taskId, 1, "调用 LLM 审查", 40);
            PromptTemplate tpl = PromptTemplate.fromClasspath("prompts/consistency_check.txt");
            String prompt = tpl.fill(Map.of(
                    "synopsis", synopsis,
                    "characters", charNames.isEmpty() ? "（暂无角色档案）" : charNames,
                    "content", content.isEmpty() ? "（暂无内容）" : content));

            String response = creativeLlmClient.chat(null, prompt);
            log.debug("[ConsistencyTaskRunner] LLM 响应完成 projectId={}", projectId);

            asyncTaskService.updateProgress(taskId, 2, "解析结果并入库", 75);
            List<ConsistencyItemJson> items = JsonExtractor.parseArray(response, ITEM_LIST_TYPE, objectMapper);

            int savedCount = 0;
            if (items != null) {
                for (ConsistencyItemJson json : items) {
                    if (json.description() == null || json.description().isBlank()) continue;
                    ConsistencyReportItem item = ConsistencyReportItem.create(
                            reportId, projectId, json.type(), json.character(),
                            json.chapterHint(), json.description(), json.suggestion(), clock);
                    itemRepository.save(item);
                    savedCount++;
                }
            }

            asyncTaskService.completeWithJson(taskId,
                    "{\"reportId\":\"" + reportId + "\",\"items\":" + savedCount + "}");
            log.info("[ConsistencyTaskRunner] 审查完成 projectId={} 发现问题数={}", projectId, savedCount);
        } catch (Exception e) {
            log.warn("[ConsistencyTaskRunner] 审查失败 taskId={} error={}", taskId, e.getMessage());
            asyncTaskService.fail(taskId, e.getMessage());
        }
    }

    private String safeSubstring(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "…";
    }
}
