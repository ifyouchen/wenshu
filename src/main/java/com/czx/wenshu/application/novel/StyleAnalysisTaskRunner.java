package com.czx.wenshu.application.novel;

import com.czx.wenshu.application.llm.JsonExtractor;
import com.czx.wenshu.application.llm.LlmClient;
import com.czx.wenshu.application.llm.PromptTemplate;
import com.czx.wenshu.application.task.AsyncTaskService;
import com.czx.wenshu.domain.user.UserStyleProfile;
import com.czx.wenshu.domain.user.UserStyleProfileRepository;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 文风分析异步执行器（P5-10）。 */
@Component
public class StyleAnalysisTaskRunner {

    private static final Logger log = LoggerFactory.getLogger(StyleAnalysisTaskRunner.class);

    private final AsyncTaskService asyncTaskService;
    private final UserStyleProfileRepository profileRepository;
    private final LlmClient utilityLlmClient;
    private final Clock clock;

    public StyleAnalysisTaskRunner(AsyncTaskService asyncTaskService,
                                    UserStyleProfileRepository profileRepository,
                                    @Qualifier("utilityLlmClient") LlmClient utilityLlmClient,
                                    Clock clock) {
        this.asyncTaskService = asyncTaskService;
        this.profileRepository = profileRepository;
        this.utilityLlmClient = utilityLlmClient;
        this.clock = clock;
    }

    @Async("aiTaskExecutor")
    @Transactional
    public void run(UUID taskId, UUID userId, String sampleText) {
        try {
            asyncTaskService.markRunning(taskId, 2, "分析文风样本");

            PromptTemplate tpl = PromptTemplate.fromClasspath("prompts/style_analysis.txt");
            String prompt = tpl.fill(Map.of("sampleText", sampleText));
            asyncTaskService.updateProgress(taskId, 1, "调用 LLM 提取标签", 40);
            String response = utilityLlmClient.chat(null, prompt);

            String tagsJson = JsonExtractor.extractFirstArray(response);
            String finalTags = (tagsJson != null && !tagsJson.isBlank()) ? tagsJson : "[]";

            // 更新档案中的标签
            profileRepository.findByUserId(userId).ifPresent(profile -> {
                profile.updateTags(finalTags, clock);
                profileRepository.save(profile);
            });

            asyncTaskService.completeWithJson(taskId, finalTags);
            log.info("文风分析完成 userId={} tags={}", userId, finalTags);
        } catch (Exception e) {
            log.warn("文风分析失败 taskId={} error={}", taskId, e.getMessage());
            asyncTaskService.fail(taskId, e.getMessage());
        }
    }
}
