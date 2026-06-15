package com.czx.wenshu.application.project;

import com.czx.wenshu.application.stats.WritingStatsService;
import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.project.Chapter;
import com.czx.wenshu.domain.project.ChapterRepository;
import com.czx.wenshu.domain.project.ChapterSnapshot;
import com.czx.wenshu.domain.project.ChapterSnapshotRepository;
import com.czx.wenshu.domain.project.ChapterStatus;
import com.czx.wenshu.domain.project.Project;
import com.czx.wenshu.domain.project.ProjectRepository;
import com.czx.wenshu.domain.project.Volume;
import com.czx.wenshu.domain.project.VolumeRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectApplicationService {

    private final ProjectRepository projectRepository;
    private final VolumeRepository volumeRepository;
    private final ChapterRepository chapterRepository;
    private final ChapterSnapshotRepository snapshotRepository;
    private final WritingStatsService writingStatsService;
    private final Clock clock;

    public ProjectApplicationService(ProjectRepository projectRepository, VolumeRepository volumeRepository,
                                      ChapterRepository chapterRepository, ChapterSnapshotRepository snapshotRepository,
                                      WritingStatsService writingStatsService, Clock clock) {
        this.projectRepository = projectRepository;
        this.volumeRepository = volumeRepository;
        this.chapterRepository = chapterRepository;
        this.snapshotRepository = snapshotRepository;
        this.writingStatsService = writingStatsService;
        this.clock = clock;
    }

    @Transactional
    public ProjectInfo createProject(UUID userId, CreateProjectCommand command) {
        Project project = Project.create(userId, command.title(), command.genre(), command.synopsis(), command.worldview(), clock);
        projectRepository.save(project);
        return ProjectInfo.from(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectInfo> listProjects(UUID userId) {
        return projectRepository.findByUserId(userId).stream().map(ProjectInfo::from).toList();
    }

    @Transactional(readOnly = true)
    public ProjectInfo getProject(UUID projectId, UUID userId) {
        Project project = projectRepository.findById(projectId)
                .filter(p -> p.userId().equals(userId))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "作品不存在"));
        return ProjectInfo.from(project);
    }

    @Transactional
    public ProjectInfo updateProject(UUID projectId, UUID userId, UpdateProjectCommand command) {
        Project project = projectRepository.findById(projectId)
                .filter(p -> p.userId().equals(userId))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "作品不存在"));
        project.update(command.title(), command.genre(), command.synopsis(), command.worldview(), clock);
        projectRepository.save(project);
        return ProjectInfo.from(project);
    }

    @Transactional
    public void deleteProject(UUID projectId, UUID userId) {
        Project project = projectRepository.findById(projectId)
                .filter(p -> p.userId().equals(userId))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "作品不存在"));
        projectRepository.deleteById(projectId);
    }

    @Transactional
    public VolumeInfo createVolume(UUID projectId, UUID userId, CreateVolumeCommand command) {
        Project project = projectRepository.findById(projectId)
                .filter(p -> p.userId().equals(userId))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "作品不存在"));
        int sortOrder = command.sortOrder() >= 0 ? command.sortOrder() : volumeRepository.countByProjectId(projectId);
        Volume volume = Volume.create(projectId, command.title(), command.conflict(), sortOrder, clock);
        volumeRepository.save(volume);
        return VolumeInfo.from(volume);
    }

    @Transactional
    public VolumeInfo updateVolume(UUID volumeId, UUID userId, UpdateVolumeCommand command) {
        Volume volume = volumeRepository.findById(volumeId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "卷不存在"));
        verifyProjectOwnership(volume.projectId(), userId);
        volume.update(command.title(), command.conflict());
        volumeRepository.save(volume);
        return VolumeInfo.from(volume);
    }

    @Transactional
    public void deleteVolume(UUID volumeId, UUID userId) {
        Volume volume = volumeRepository.findById(volumeId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "卷不存在"));
        verifyProjectOwnership(volume.projectId(), userId);
        volumeRepository.deleteById(volumeId);
    }

    @Transactional
    public ChapterInfo createChapter(UUID volumeId, UUID userId, CreateChapterCommand command) {
        Volume volume = volumeRepository.findById(volumeId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "卷不存在"));
        verifyProjectOwnership(volume.projectId(), userId);
        int sortOrder = command.sortOrder() >= 0 ? command.sortOrder() : chapterRepository.findByVolumeId(volumeId).size();
        Chapter chapter = Chapter.create(volumeId, volume.projectId(), command.title(), command.outline(), sortOrder, clock);
        chapterRepository.save(chapter);
        return ChapterInfo.from(chapter);
    }

    @Transactional(readOnly = true)
    public ChapterInfo getChapter(UUID chapterId, UUID userId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "章节不存在"));
        verifyProjectOwnership(chapter.projectId(), userId);
        return ChapterInfo.from(chapter);
    }

    @Transactional
    public ChapterInfo updateChapter(UUID chapterId, UUID userId, UpdateChapterCommand command) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "章节不存在"));
        verifyProjectOwnership(chapter.projectId(), userId);
        int delta = chapter.wordCountDelta(command.content());
        Project project = projectRepository.findById(chapter.projectId()).orElseThrow();
        chapter.saveContent(command.title(), command.content(), command.outline(), ChapterStatus.fromValue(command.status()), clock);
        chapterRepository.save(chapter);
        writingStatsService.recordManualDelta(project.userId(), chapter.projectId(), delta);
        return ChapterInfo.from(chapter);
    }

    @Transactional
    public void deleteChapter(UUID chapterId, UUID userId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "章节不存在"));
        verifyProjectOwnership(chapter.projectId(), userId);
        chapterRepository.deleteById(chapterId);
    }

    @Transactional(readOnly = true)
    public OutlineInfo getOutline(UUID projectId, UUID userId) {
        Project project = projectRepository.findById(projectId)
                .filter(p -> p.userId().equals(userId))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "作品不存在"));
        List<Volume> volumes = volumeRepository.findByProjectId(projectId);
        List<Chapter> allChapters = chapterRepository.findByProjectId(projectId);
        java.util.Map<UUID, List<Chapter>> chaptersByVolume = allChapters.stream()
                .collect(java.util.stream.Collectors.groupingBy(Chapter::volumeId));
        List<OutlineInfo.VolumeNode> volumeNodes = volumes.stream()
                .map(v -> OutlineInfo.VolumeNode.from(v,
                        chaptersByVolume.getOrDefault(v.id(), List.of()).stream()
                                .map(OutlineInfo.ChapterNode::from).toList()))
                .toList();
        return new OutlineInfo(volumeNodes);
    }

    @Transactional
    public ProjectInfo updateWritingGoal(UUID projectId, UUID userId, int dailyCharGoal) {
        Project project = projectRepository.findById(projectId)
                .filter(p -> p.userId().equals(userId))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "作品不存在"));
        project.updateDailyCharGoal(dailyCharGoal, clock);
        projectRepository.save(project);
        return ProjectInfo.from(project);
    }

    @Transactional(readOnly = true)
    public List<SnapshotInfo> listSnapshots(UUID chapterId, UUID userId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "章节不存在"));
        verifyProjectOwnership(chapter.projectId(), userId);
        return snapshotRepository.findByChapterId(chapterId).stream().map(SnapshotInfo::from).toList();
    }

    @Transactional
    public SnapshotInfo createSnapshot(UUID chapterId, UUID userId, String snapshotType, String label) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "章节不存在"));
        verifyProjectOwnership(chapter.projectId(), userId);
        ChapterSnapshot snapshot = ChapterSnapshot.create(chapterId, chapter.content(), chapter.wordCount(),
                snapshotType, label, clock);
        snapshotRepository.save(snapshot);
        return SnapshotInfo.from(snapshot);
    }

    @Transactional
    public ChapterInfo restoreSnapshot(UUID snapshotId, UUID userId) {
        ChapterSnapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "快照不存在"));
        Chapter chapter = chapterRepository.findById(snapshot.chapterId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "章节不存在"));
        verifyProjectOwnership(chapter.projectId(), userId);
        int delta = chapter.wordCountDelta(snapshot.content());
        Project project = projectRepository.findById(chapter.projectId()).orElseThrow();
        ChapterSnapshot autoSnapshot = ChapterSnapshot.create(chapter.id(), chapter.content(), chapter.wordCount(),
                "auto_before_restore", "恢复前自动快照", clock);
        snapshotRepository.save(autoSnapshot);
        chapter.saveContent(chapter.title(), snapshot.content(), chapter.outline(), chapter.status(), clock);
        chapterRepository.save(chapter);
        writingStatsService.recordManualDelta(project.userId(), chapter.projectId(), delta);
        return ChapterInfo.from(chapter);
    }

    private void verifyProjectOwnership(UUID projectId, UUID userId) {
        if (!projectRepository.existsByIdAndUserId(projectId, userId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "作品不存在");
        }
    }
}