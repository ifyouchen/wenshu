package com.czx.wenshu.application.project;

import com.czx.wenshu.application.llm.LlmClient;
import com.czx.wenshu.application.llm.PromptTemplate;
import com.czx.wenshu.application.task.AsyncTaskService;
import com.czx.wenshu.domain.project.ChapterSummary;
import com.czx.wenshu.domain.project.ChapterSummaryRepository;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 章节摘要异步生成执行器（P6-01）。 */
@Component
public class SummaryTaskRunner {

    private static final Logger log = LoggerFactory.getLogger(SummaryTaskRunner.class);
    /** 摘要最大长度（字符），超出时截断。 */
    private static final int MAX_SUMMARY_LENGTH = 500;

    private final AsyncTaskService asyncTaskService;
    private final ChapterSummaryRepository summaryRepository;
    private final LlmClient utilityLlmClient;
    private final Clock clock;

    public SummaryTaskRunner(AsyncTaskService asyncTaskService,
                              ChapterSummaryRepository summaryRepository,
                              @Qualifier("utilityLlmClient") LlmClient utilityLlmClient,
                              Clock clock) {
        this.asyncTaskService = asyncTaskService;
        this.summaryRepository = summaryRepository;
        this.utilityLlmClient = utilityLlmClient;
        this.clock = clock;
    }

    @Async("aiTaskExecutor")
    @Transactional
    public void run(UUID taskId, UUID chapterId, UUID projectId, String chapterTitle, String content) {
        log.info("[SummaryTaskRunner] 开始生成章节摘要 taskId={} chapterId={} projectId={}", taskId, chapterId, projectId);
        try {
            asyncTaskService.markRunning(taskId, 2, "生成章节摘要");

            PromptTemplate tpl = PromptTemplate.fromClasspath("prompts/chapter_summary.txt");
            String prompt = tpl.fill(Map.of(
                    "chapterTitle", chapterTitle != null ? chapterTitle : "",
                    "content", content != null ? content : ""));

            asyncTaskService.updateProgress(taskId, 1, "调用 LLM", 40);
            String summary = utilityLlmClient.chat(null, prompt);

            // 截断至最大长度
            if (summary != null && summary.length() > MAX_SUMMARY_LENGTH) {
                summary = summary.substring(0, MAX_SUMMARY_LENGTH);
            }
            String finalSummary = summary != null ? summary.strip() : "";

            ChapterSummary chapterSummary = ChapterSummary.create(chapterId, projectId, finalSummary, clock);
            summaryRepository.save(chapterSummary);

            asyncTaskService.completeWithJson(taskId, "{\"chapterId\":\"" + chapterId + "\"}");
            log.info("[SummaryTaskRunner] 章节摘要生成完成 chapterId={}", chapterId);
        } catch (Exception e) {
            log.warn("[SummaryTaskRunner] 章节摘要生成失败 taskId={}", taskId, e);
            asyncTaskService.fail(taskId, e.getMessage());
        }
    }
}
