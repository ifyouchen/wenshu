package com.czx.wenshu.application.novel;

import com.czx.wenshu.application.llm.JsonExtractor;
import com.czx.wenshu.application.llm.LlmClient;
import com.czx.wenshu.application.llm.PromptTemplate;
import com.czx.wenshu.application.task.AsyncTaskService;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 骨架生成异步执行器（P5-04）。
 * 独立 Bean 以确保 Spring @Async 代理正确生效（同类调用不走代理）。
 */
@Component
public class SkeletonTaskRunner {

    private static final Logger log = LoggerFactory.getLogger(SkeletonTaskRunner.class);

    private static final String SYSTEM_PROMPT =
            "你是一位专业的网文策划编辑，擅长设计有张力的故事骨架。请只输出 JSON，不要有其他内容。";

    private final AsyncTaskService asyncTaskService;
    private final LlmClient creativeLlmClient;

    public SkeletonTaskRunner(AsyncTaskService asyncTaskService,
                               @Qualifier("creativeLlmClient") LlmClient creativeLlmClient) {
        this.asyncTaskService = asyncTaskService;
        this.creativeLlmClient = creativeLlmClient;
    }

    @Async("aiTaskExecutor")
    public void run(UUID taskId, SkeletonInput input) {
        try {
            asyncTaskService.markRunning(taskId, 3, "初始化");
            asyncTaskService.updateProgress(taskId, 1, "构建 Prompt", 10);

            PromptTemplate tpl = PromptTemplate.fromClasspath("prompts/skeleton.txt");
            String prompt = tpl.fill(Map.of(
                    "genre", safeStr(input.genre()),
                    "synopsis", safeStr(input.synopsis()),
                    "worldview", safeStr(input.worldview()),
                    "targetWords", input.targetWords() != null ? input.targetWords().toString() : "100000"
            ));

            asyncTaskService.updateProgress(taskId, 2, "调用 LLM 生成骨架", 30);
            String llmResponse = creativeLlmClient.chat(SYSTEM_PROMPT, prompt);

            asyncTaskService.updateProgress(taskId, 3, "解析结果", 80);
            String json = JsonExtractor.extractFirstObject(llmResponse);
            if (json == null || json.isBlank()) {
                asyncTaskService.fail(taskId, "LLM 返回内容无法提取 JSON");
                return;
            }

            asyncTaskService.completeWithJson(taskId, json);
            log.info("骨架生成任务完成 taskId={}", taskId);
        } catch (Exception e) {
            log.warn("骨架生成任务失败 taskId={} error={}", taskId, e.getMessage());
            asyncTaskService.fail(taskId, e.getMessage());
        }
    }

    private String safeStr(String s) {
        return s != null ? s : "";
    }
}
