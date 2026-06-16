package com.czx.wenshu.application.project;

import com.czx.wenshu.application.task.AsyncTaskService;
import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.project.Chapter;
import com.czx.wenshu.domain.project.ChapterKeyEventRepository;
import com.czx.wenshu.domain.project.ChapterRepository;
import com.czx.wenshu.domain.project.ProjectRepository;
import com.czx.wenshu.domain.task.AsyncTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 章节关键事件服务（P6-03）。
 * 负责提交异步提取任务、查询已提取的事件列表。
 */
@Service
public class KeyEventService {

    private static final Logger log = LoggerFactory.getLogger(KeyEventService.class);

    private final ChapterRepository chapterRepository;
    private final ChapterKeyEventRepository keyEventRepository;
    private final ProjectRepository projectRepository;
    private final AsyncTaskService asyncTaskService;
    private final KeyEventTaskRunner keyEventTaskRunner;
    private final ObjectMapper objectMapper;

    public KeyEventService(ChapterRepository chapterRepository,
                            ChapterKeyEventRepository keyEventRepository,
                            ProjectRepository projectRepository,
                            AsyncTaskService asyncTaskService,
                            KeyEventTaskRunner keyEventTaskRunner,
                            ObjectMapper objectMapper) {
        this.chapterRepository = chapterRepository;
        this.keyEventRepository = keyEventRepository;
        this.projectRepository = projectRepository;
        this.asyncTaskService = asyncTaskService;
        this.keyEventTaskRunner = keyEventTaskRunner;
        this.objectMapper = objectMapper;
    }

    /**
     * 提交章节关键事件异步提取任务（P6-03）。
     * 章节内容为空时拒绝提交。
     *
     * @param chapterId 章节 ID
     * @param userId    当前用户 ID
     * @return 异步任务 ID 字符串
     */
    @Transactional
    public String submitKeyEventExtraction(UUID chapterId, UUID userId) {
        log.info("[KeyEventService] 提交关键事件提取任务 chapterId={} userId={}", chapterId, userId);
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "章节不存在"));
        if (!projectRepository.existsByIdAndUserId(chapter.projectId(), userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "无权操作该章节");
        }
        if (chapter.content() == null || chapter.content().isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "章节内容为空，无法提取关键事件");
        }
        AsyncTask task = asyncTaskService.createTask(userId, chapter.projectId(), "key_event_extraction");
        keyEventTaskRunner.run(task.id(), chapterId, chapter.projectId(),
                chapter.title(), chapter.content());
        log.info("[KeyEventService] 关键事件提取任务已提交 taskId={} chapterId={}", task.id(), chapterId);
        return task.id().toString();
    }

    /**
     * 查询章节的关键事件列表（P6-03）。
     *
     * @param chapterId 章节 ID
     * @param userId    当前用户 ID
     * @return 关键事件信息列表
     */
    @Transactional(readOnly = true)
    public List<KeyEventInfo> getKeyEvents(UUID chapterId, UUID userId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "章节不存在"));
        if (!projectRepository.existsByIdAndUserId(chapter.projectId(), userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "无权查看该章节关键事件");
        }
        List<KeyEventInfo> events = keyEventRepository.findByChapterId(chapterId)
                .stream().map(e -> KeyEventInfo.from(e, objectMapper)).toList();
        log.debug("[KeyEventService] 查询关键事件 chapterId={} 结果数={}", chapterId, events.size());
        return events;
    }

    /**
     * 查询作品的全部关键事件列表，按重要程度降序（P6-03）。
     *
     * @param projectId 作品 ID
     * @param userId    当前用户 ID
     * @return 关键事件信息列表
     */
    @Transactional(readOnly = true)
    public List<KeyEventInfo> getProjectKeyEvents(UUID projectId, UUID userId) {
        if (!projectRepository.existsByIdAndUserId(projectId, userId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "作品不存在");
        }
        List<KeyEventInfo> events = keyEventRepository.findByProjectId(projectId)
                .stream().map(e -> KeyEventInfo.from(e, objectMapper)).toList();
        log.debug("[KeyEventService] 查询作品关键事件 projectId={} 结果数={}", projectId, events.size());
        return events;
    }
}
