package com.czx.wenshu.application.script;

import com.czx.wenshu.application.storage.FileStorageService;
import com.czx.wenshu.application.task.AsyncTaskService;
import com.czx.wenshu.domain.script.ScriptDraft;
import com.czx.wenshu.domain.script.ScriptDraftRepository;
import com.czx.wenshu.domain.script.ScriptScene;
import com.czx.wenshu.domain.script.ScriptSceneRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 剧本导出异步执行器（P9-06）。
 *
 * <p>将已改编的剧本草稿导出为文本文件，上传腾讯云 COS 并返回预签名 URL。</p>
 *
 * <p>合规要求（P9-06）：导出文件必须在页眉/页脚添加 "本作品由 AI 辅助生成" 声明。</p>
 *
 * <p>当前支持格式：纯文本（TXT）。DOCX/FDX/分镜等复杂格式可在后续版本对接专用库实现。</p>
 *
 * <p>COS 对象键：{@code exports/{userId}/scripts/{draftId}.txt}</p>
 */
@Component
public class ScriptExportTaskRunner {

    private static final Logger log = LoggerFactory.getLogger(ScriptExportTaskRunner.class);

    /** P9-06 合规：AI 辅助生成声明文字。 */
    private static final String AI_ASSISTED_ANNOTATION =
            "【声明】本作品由 AI 辅助生成，版权归作者所有。Generated with AI assistance - 文枢 wenshu";

    private final AsyncTaskService asyncTaskService;
    private final ScriptDraftRepository draftRepository;
    private final ScriptSceneRepository sceneRepository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    /** 构造函数注入。 */
    public ScriptExportTaskRunner(AsyncTaskService asyncTaskService,
                                  ScriptDraftRepository draftRepository,
                                  ScriptSceneRepository sceneRepository,
                                  FileStorageService fileStorageService,
                                  ObjectMapper objectMapper) {
        this.asyncTaskService = asyncTaskService;
        this.draftRepository = draftRepository;
        this.sceneRepository = sceneRepository;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
    }

    /**
     * 异步执行剧本导出任务（P7-08 / P9-06）。
     *
     * @param taskId  异步任务 ID
     * @param draftId 剧本草稿 ID
     * @param userId  请求导出的用户 ID
     * @param format  导出格式（目前统一生成 TXT，docx/fdx/storyboard 识别后标注格式名）
     */
    @Async("aiTaskExecutor")
    public void run(UUID taskId, UUID draftId, UUID userId, String format) {
        log.info("[ScriptExportTaskRunner] 开始剧本导出 taskId={} draftId={} format={}", taskId, draftId, format);
        try {
            asyncTaskService.markRunning(taskId, 3, "初始化");

            // 步骤 1：加载草稿和场景
            asyncTaskService.updateProgress(taskId, 1, "加载剧本数据", 20);
            ScriptDraft draft = draftRepository.findById(draftId)
                    .orElseThrow(() -> new IllegalArgumentException("草稿不存在: " + draftId));
            List<ScriptScene> scenes = sceneRepository.findByDraftId(draftId, 0, Integer.MAX_VALUE);

            // 步骤 2：生成文本内容（含 AI 声明）
            asyncTaskService.updateProgress(taskId, 2, "生成文件", 50);
            String fileContent = buildFileContent(draft, scenes, format);
            byte[] fileBytes = fileContent.getBytes(StandardCharsets.UTF_8);

            // 步骤 3：上传 COS 或返回占位 URL
            asyncTaskService.updateProgress(taskId, 3, "上传文件", 80);

            if (!fileStorageService.isAvailable()) {
                log.warn("[ScriptExportTaskRunner] COS 未配置，返回占位 URL taskId={}", taskId);
                asyncTaskService.completeWithJson(taskId,
                        objectMapper.writeValueAsString(Map.of(
                                "downloadUrl", "COS_NOT_CONFIGURED",
                                "note", "请配置腾讯云 COS 凭据",
                                "aiAssisted", true)));
                return;
            }

            String objectKey = "exports/" + userId + "/scripts/" + draftId + "_" + format + ".txt";
            fileStorageService.upload(objectKey,
                    new ByteArrayInputStream(fileBytes), "text/plain; charset=utf-8", fileBytes.length);
            String downloadUrl = fileStorageService.generatePresignedUrl(objectKey, Duration.ofMinutes(60));

            String resultJson = objectMapper.writeValueAsString(Map.of(
                    "downloadUrl", downloadUrl,
                    "objectKey", objectKey,
                    "format", format,
                    "aiAssisted", true,          // P9-06：标注 AI 辅助生成标志
                    "annotation", AI_ASSISTED_ANNOTATION
            ));
            asyncTaskService.completeWithJson(taskId, resultJson);
            log.info("[ScriptExportTaskRunner] 剧本导出完成 taskId={} objectKey={}", taskId, objectKey);

        } catch (Exception e) {
            log.error("[ScriptExportTaskRunner] 剧本导出失败 taskId={} error={}", taskId, e.getMessage(), e);
            asyncTaskService.fail(taskId, "导出失败：" + e.getMessage());
        }
    }

    // ── 私有工具方法 ─────────────────────────────────────────────────────────

    /**
     * 构建包含所有场景内容及 AI 声明的导出文本。
     *
     * @param draft  草稿基本信息
     * @param scenes 场景列表
     * @param format 格式名（用于文件头注明）
     * @return 完整导出文本
     */
    private String buildFileContent(ScriptDraft draft, List<ScriptScene> scenes, String format) {
        StringBuilder sb = new StringBuilder();

        // ── 页眉（P9-06 AI 声明）──────────────────────────────────────────
        sb.append("═".repeat(60)).append("\n");
        sb.append(AI_ASSISTED_ANNOTATION).append("\n");
        sb.append("导出日期：").append(LocalDate.now()).append(" | 格式：").append(format.toUpperCase()).append("\n");
        sb.append("═".repeat(60)).append("\n\n");

        // ── 草稿标题 ────────────────────────────────────────────────────────
        sb.append("《").append(draft.title() != null ? draft.title() : "未命名草稿").append("》\n");
        sb.append("改编策略：").append(draft.strategy() != null ? draft.strategy() : "标准").append("\n");
        sb.append("─".repeat(60)).append("\n\n");

        // ── 场景内容 ────────────────────────────────────────────────────────
        for (ScriptScene scene : scenes) {
            sb.append("【场景 ").append(scene.sceneIndex() + 1).append("】");
            if (scene.location() != null && !scene.location().isBlank()) {
                sb.append(" ").append(scene.location());
            }
            if (scene.timeDesc() != null && !scene.timeDesc().isBlank()) {
                sb.append(" ").append(scene.timeDesc());
            }
            sb.append("\n");
            if (scene.content() != null && !scene.content().isBlank()) {
                sb.append(scene.content()).append("\n");
            }
            sb.append("\n");
        }

        // ── 页脚（P9-06 AI 声明重复）────────────────────────────────────────
        sb.append("─".repeat(60)).append("\n");
        sb.append(AI_ASSISTED_ANNOTATION).append("\n");

        return sb.toString();
    }
}
