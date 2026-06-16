package com.czx.wenshu.application.project;

import com.czx.wenshu.application.llm.JsonExtractor;
import com.czx.wenshu.application.llm.LlmClient;
import com.czx.wenshu.application.llm.PromptTemplate;
import com.czx.wenshu.application.task.AsyncTaskService;
import com.czx.wenshu.domain.project.ChapterKeyEvent;
import com.czx.wenshu.domain.project.ChapterKeyEventRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 章节关键事件异步提取执行器（P6-03）。
 * 作为独立 Bean 保证 Spring @Async 代理正确工作。
 */
@Component
public class KeyEventTaskRunner {

    private static final Logger log = LoggerFactory.getLogger(KeyEventTaskRunner.class);
    private static final TypeReference<List<KeyEventJson>> KEY_EVENT_LIST_TYPE = new TypeReference<>() {};
    /** 章节内容截断长度，避免超出 LLM 上下文限制。 */
    private static final int MAX_CONTENT_LENGTH = 3000;

    private final AsyncTaskService asyncTaskService;
    private final ChapterKeyEventRepository keyEventRepository;
    private final LlmClient utilityLlmClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public KeyEventTaskRunner(AsyncTaskService asyncTaskService,
                               ChapterKeyEventRepository keyEventRepository,
                               @Qualifier("utilityLlmClient") LlmClient utilityLlmClient,
                               ObjectMapper objectMapper,
                               Clock clock) {
        this.asyncTaskService = asyncTaskService;
        this.keyEventRepository = keyEventRepository;
        this.utilityLlmClient = utilityLlmClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * 异步提取并保存章节关键事件。
     *
     * @param taskId       进度任务 ID
     * @param chapterId    目标章节 ID
     * @param projectId    所属作品 ID
     * @param chapterTitle 章节标题
     * @param content      章节正文
     */
    @Async("aiTaskExecutor")
    @Transactional
    public void run(UUID taskId, UUID chapterId, UUID projectId, String chapterTitle, String content) {
        log.info("[KeyEventTaskRunner] 开始提取关键事件 taskId={} chapterId={}", taskId, chapterId);
        try {
            asyncTaskService.markRunning(taskId, 3, "准备提取关键事件");

            // 截断过长内容
            String truncatedContent = content != null && content.length() > MAX_CONTENT_LENGTH
                    ? content.substring(0, MAX_CONTENT_LENGTH) + "…" : content;

            asyncTaskService.updateProgress(taskId, 1, "调用 LLM 提取关键事件", 30);
            PromptTemplate tpl = PromptTemplate.fromClasspath("prompts/key_events.txt");
            String prompt = tpl.fill(Map.of(
                    "chapterTitle", chapterTitle != null ? chapterTitle : "",
                    "content", truncatedContent != null ? truncatedContent : ""));

            String response = utilityLlmClient.chat(null, prompt);
            log.debug("[KeyEventTaskRunner] LLM 响应完成 chapterId={}", chapterId);

            asyncTaskService.updateProgress(taskId, 2, "解析并入库", 70);
            List<KeyEventJson> events = JsonExtractor.parseArray(response, KEY_EVENT_LIST_TYPE, objectMapper);

            if (events == null || events.isEmpty()) {
                log.warn("[KeyEventTaskRunner] LLM 未返回有效关键事件 chapterId={}", chapterId);
                asyncTaskService.fail(taskId, "LLM 未能提取到有效关键事件");
                return;
            }

            // 先清除旧记录，再写入新提取的事件
            keyEventRepository.deleteByChapterId(chapterId);
            int savedCount = 0;
            for (KeyEventJson json : events) {
                if (json.eventText() == null || json.eventText().isBlank()) continue;
                String charsJson = buildCharactersJson(json.characters());
                ChapterKeyEvent event = ChapterKeyEvent.create(
                        projectId, chapterId, json.eventText(),
                        json.eventType(), charsJson,
                        json.importance() != null ? json.importance() : 0.5, clock);
                keyEventRepository.save(event);
                savedCount++;
            }

            asyncTaskService.completeWithJson(taskId, "{\"count\":" + savedCount + "}");
            log.info("[KeyEventTaskRunner] 关键事件提取完成 chapterId={} 事件数={}", chapterId, savedCount);
        } catch (Exception e) {
            log.warn("[KeyEventTaskRunner] 关键事件提取失败 taskId={} error={}", taskId, e.getMessage());
            asyncTaskService.fail(taskId, e.getMessage());
        }
    }

    /**
     * 将角色名称列表序列化为 JSON 数组字符串。
     *
     * @param characters 角色名称列表（可为 null）
     */
    private String buildCharactersJson(List<String> characters) {
        if (characters == null || characters.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(characters);
        } catch (Exception e) {
            return "[]";
        }
    }
}
