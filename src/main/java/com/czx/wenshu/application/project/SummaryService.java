package com.czx.wenshu.application.project;

import com.czx.wenshu.application.task.AsyncTaskService;
import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.project.Chapter;
import com.czx.wenshu.domain.project.ChapterRepository;
import com.czx.wenshu.domain.project.ChapterSummary;
import com.czx.wenshu.domain.project.ChapterSummaryRepository;
import com.czx.wenshu.domain.project.ProjectRepository;
import com.czx.wenshu.domain.task.AsyncTask;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 章节摘要服务（P6-01）。 */
@Service
public class SummaryService {

    private final ChapterRepository chapterRepository;
    private final ChapterSummaryRepository summaryRepository;
    private final ProjectRepository projectRepository;
    private final AsyncTaskService asyncTaskService;
    private final SummaryTaskRunner summaryTaskRunner;

    public SummaryService(ChapterRepository chapterRepository,
                           ChapterSummaryRepository summaryRepository,
                           ProjectRepository projectRepository,
                           AsyncTaskService asyncTaskService,
                           SummaryTaskRunner summaryTaskRunner) {
        this.chapterRepository = chapterRepository;
        this.summaryRepository = summaryRepository;
        this.projectRepository = projectRepository;
        this.asyncTaskService = asyncTaskService;
        this.summaryTaskRunner = summaryTaskRunner;
    }

    /**
     * 提交章节摘要异步生成任务（P6-01）。
     * 若章节内容为空，则直接返回 null（无需摘要）。
     *
     * @return taskId（UUID 字符串）
     */
    @Transactional
    public String submitSummaryTask(UUID chapterId, UUID userId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "章节不存在"));
        if (!projectRepository.existsByIdAndUserId(chapter.projectId(), userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "无权访问该章节");
        }
        if (chapter.content() == null || chapter.content().isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "章节内容为空，无法生成摘要");
        }

        AsyncTask task = asyncTaskService.createTask(userId, chapter.projectId(), "chapter_summary");
        summaryTaskRunner.run(task.id(), chapterId, chapter.projectId(),
                chapter.title(), chapter.content());
        return task.id().toString();
    }

    @Transactional(readOnly = true)
    public Optional<ChapterSummary> getSummary(UUID chapterId, UUID userId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "章节不存在"));
        if (!projectRepository.existsByIdAndUserId(chapter.projectId(), userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "无权访问该章节");
        }
        return summaryRepository.findByChapterId(chapterId);
    }
}
