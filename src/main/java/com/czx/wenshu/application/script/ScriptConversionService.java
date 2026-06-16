package com.czx.wenshu.application.script;

import com.czx.wenshu.application.task.AsyncTaskService;
import com.czx.wenshu.application.user.QuotaService;
import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.project.ProjectRepository;
import com.czx.wenshu.domain.script.ScriptDraft;
import com.czx.wenshu.domain.script.ScriptDraftRepository;
import com.czx.wenshu.domain.task.AsyncTask;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 剧本改编提交服务（P7-04）。
 * 校验权限和配额后，创建草稿并触发异步转换任务。
 */
@Service
public class ScriptConversionService {

    private static final Logger log = LoggerFactory.getLogger(ScriptConversionService.class);

    private final ScriptDraftRepository draftRepository;
    private final ProjectRepository projectRepository;
    private final AsyncTaskService asyncTaskService;
    private final QuotaService quotaService;
    private final ScriptConversionTaskRunner taskRunner;
    private final Clock clock;

    public ScriptConversionService(ScriptDraftRepository draftRepository,
                                    ProjectRepository projectRepository,
                                    AsyncTaskService asyncTaskService,
                                    QuotaService quotaService,
                                    ScriptConversionTaskRunner taskRunner,
                                    Clock clock) {
        this.draftRepository = draftRepository;
        this.projectRepository = projectRepository;
        this.asyncTaskService = asyncTaskService;
        this.quotaService = quotaService;
        this.taskRunner = taskRunner;
        this.clock = clock;
    }

    /**
     * 提交剧本改编任务（P7-04）。
     * 消耗一次改编配额，创建草稿并启动异步转换。
     *
     * @param projectId          作品 ID
     * @param userId             当前用户 ID
     * @param title              草稿标题（可为 null，默认使用作品标题）
     * @param psychologyStrategy 心理外化策略（action/dialogue/voiceover/skip）
     * @return 包含 taskId 和 draftId 的 Map
     */
    @Transactional
    public Map<String, String> submitConversion(UUID projectId, UUID userId,
                                                  String title, String psychologyStrategy) {
        log.info("[ScriptConversionService] 提交改编任务 projectId={} userId={}", projectId, userId);
        if (!projectRepository.existsByIdAndUserId(projectId, userId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "作品不存在");
        }

        // 检查并扣减改编配额
        quotaService.checkAndIncrementAdaptation(userId);

        String strategy = PsychologyStrategy.fromValue(psychologyStrategy).value();
        String draftTitle = title != null && !title.isBlank() ? title : "改编草稿";
        ScriptDraft draft = ScriptDraft.create(projectId, userId, draftTitle, strategy, clock);
        draftRepository.save(draft);

        AsyncTask task = asyncTaskService.createTask(userId, projectId, "script_conversion");
        taskRunner.run(task.id(), draft.id(), projectId, strategy);

        log.info("[ScriptConversionService] 改编任务已提交 taskId={} draftId={}", task.id(), draft.id());
        return Map.of("taskId", task.id().toString(), "draftId", draft.id().toString());
    }
}
