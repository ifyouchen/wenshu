package com.czx.wenshu.application.novel;

import com.czx.wenshu.application.task.AsyncTaskService;
import com.czx.wenshu.application.task.TaskProgressInfo;
import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.project.Chapter;
import com.czx.wenshu.domain.project.ChapterRepository;
import com.czx.wenshu.domain.project.Character;
import com.czx.wenshu.domain.project.CharacterRepository;
import com.czx.wenshu.domain.project.Project;
import com.czx.wenshu.domain.project.ProjectRepository;
import com.czx.wenshu.domain.project.Volume;
import com.czx.wenshu.domain.project.VolumeRepository;
import com.czx.wenshu.domain.task.AsyncTask;
import com.czx.wenshu.domain.task.AsyncTaskStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 骨架生成提交（P5-04）与应用入库（P5-05）。 */
@Service
public class SkeletonService {

    private static final Logger log = LoggerFactory.getLogger(SkeletonService.class);

    private final AsyncTaskService asyncTaskService;
    private final SkeletonTaskRunner skeletonTaskRunner;
    private final ProjectRepository projectRepository;
    private final VolumeRepository volumeRepository;
    private final ChapterRepository chapterRepository;
    private final CharacterRepository characterRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SkeletonService(AsyncTaskService asyncTaskService,
                            SkeletonTaskRunner skeletonTaskRunner,
                            ProjectRepository projectRepository,
                            VolumeRepository volumeRepository,
                            ChapterRepository chapterRepository,
                            CharacterRepository characterRepository,
                            ObjectMapper objectMapper,
                            Clock clock) {
        this.asyncTaskService = asyncTaskService;
        this.skeletonTaskRunner = skeletonTaskRunner;
        this.projectRepository = projectRepository;
        this.volumeRepository = volumeRepository;
        this.chapterRepository = chapterRepository;
        this.characterRepository = characterRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    // ── P5-04 提交骨架生成任务 ─────────────────────────────────────────────

    @Transactional
    public String submitSkeletonTask(UUID userId, SkeletonInput input) {
        verifyProjectOwnership(input.projectId(), userId);
        AsyncTask task = asyncTaskService.createTask(userId, input.projectId(), "novel_skeleton");
        log.info("[SkeletonService] 提交骨架生成任务 userId={} projectId={} taskId={}", userId, input.projectId(), task.id());
        skeletonTaskRunner.run(task.id(), input);  // 异步执行（@Async 代理）
        return task.id().toString();
    }

    // ── P5-05 应用骨架入库 ─────────────────────────────────────────────────

    @Transactional
    public SkeletonApplyResult applySkeletonTask(UUID taskId, UUID userId) {
        TaskProgressInfo progress = asyncTaskService.getProgress(taskId, userId);
        if (!AsyncTaskStatus.COMPLETED.value().equals(progress.status())) {
            throw new ApiException(ErrorCode.BAD_REQUEST,
                    "骨架任务尚未完成，当前状态：" + progress.status());
        }
        if (progress.resultJson() == null || progress.resultJson().isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "骨架任务无有效结果，请重新生成");
        }

        SkeletonJson skeleton = parseSkeleton(progress.resultJson());
        if (progress.projectId() == null) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "骨架任务未关联作品");
        }
        UUID projectId = UUID.fromString(progress.projectId());

        int volumeOrder = volumeRepository.countByProjectId(projectId);
        int createdVolumes = 0;
        int createdChapters = 0;
        int createdCharacters = 0;

        for (SkeletonJson.VolumeNode vNode : skeleton.volumes()) {
            Volume volume = Volume.create(projectId, vNode.title(), vNode.conflict(), volumeOrder++, clock);
            volumeRepository.save(volume);
            createdVolumes++;

            int chapterOrder = 0;
            for (SkeletonJson.ChapterNode cNode : vNode.chapters()) {
                Chapter chapter = Chapter.create(volume.id(), projectId, cNode.title(),
                        cNode.outline(), chapterOrder++, clock);
                chapterRepository.save(chapter);
                createdChapters++;
            }
        }

        for (SkeletonJson.CharacterNode cNode : skeleton.characters()) {
            Character character = Character.create(projectId, cNode.name(), cNode.role(), clock);
            characterRepository.save(character);
            createdCharacters++;
        }

        log.info("[SkeletonService] 骨架入库完成 projectId={} 卷数={} 章节数={} 角色数={}",
                projectId, createdVolumes, createdChapters, createdCharacters);
        return new SkeletonApplyResult(projectId.toString(), createdVolumes, createdChapters, createdCharacters,
                skeleton.title(), skeleton.theme());
    }

    // ── 私有工具方法 ───────────────────────────────────────────────────────

    private SkeletonJson parseSkeleton(String json) {
        try {
            return objectMapper.readValue(json, SkeletonJson.class);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "骨架 JSON 解析失败：" + e.getMessage());
        }
    }

    private void verifyProjectOwnership(UUID projectId, UUID userId) {
        if (projectId == null || !projectRepository.existsByIdAndUserId(projectId, userId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "作品不存在");
        }
    }
}
