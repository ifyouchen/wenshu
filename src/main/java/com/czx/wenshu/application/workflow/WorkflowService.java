package com.czx.wenshu.application.workflow;

import com.czx.wenshu.application.project.CharacterApplicationService;
import com.czx.wenshu.application.project.ChapterInfo;
import com.czx.wenshu.application.project.OutlineInfo;
import com.czx.wenshu.application.project.ProjectApplicationService;
import com.czx.wenshu.application.project.ProjectInfo;
import com.czx.wenshu.application.project.WorldElementApplicationService;
import com.czx.wenshu.application.script.ScriptDraftInfo;
import com.czx.wenshu.application.script.ScriptService;
import com.czx.wenshu.application.user.StyleTemplateService;
import com.czx.wenshu.application.workflow.WorkflowInfo.ChapterBrief;
import com.czx.wenshu.application.workflow.WorkflowInfo.Dashboard;
import com.czx.wenshu.application.workflow.WorkflowInfo.RewriteState;
import com.czx.wenshu.application.workflow.WorkflowInfo.ScriptState;
import com.czx.wenshu.application.workflow.WorkflowInfo.WorkflowTask;
import com.czx.wenshu.application.workflow.WorkflowInfo.WriteState;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 面向前端三条主线的只读聚合服务。 */
@Service
public class WorkflowService {

    private final ProjectApplicationService projectService;
    private final CharacterApplicationService characterService;
    private final WorldElementApplicationService worldElementService;
    private final StyleTemplateService styleTemplateService;
    private final ScriptService scriptService;

    public WorkflowService(ProjectApplicationService projectService,
                           CharacterApplicationService characterService,
                           WorldElementApplicationService worldElementService,
                           StyleTemplateService styleTemplateService,
                           ScriptService scriptService) {
        this.projectService = projectService;
        this.characterService = characterService;
        this.worldElementService = worldElementService;
        this.styleTemplateService = styleTemplateService;
        this.scriptService = scriptService;
    }

    @Transactional(readOnly = true)
    public Dashboard dashboard(UUID userId) {
        List<ProjectInfo> projects = projectService.listProjects(userId);
        ProjectInfo recentProject = projects.stream()
                .max(Comparator.comparing(ProjectInfo::updatedAt))
                .orElse(null);
        ChapterInfo continueChapter = recentProject != null
                ? firstChapter(projectService.getOutline(UUID.fromString(recentProject.id()), userId), userId).orElse(null)
                : null;
        ScriptDraftInfo recentDraft = recentProject != null
                ? latestDraft(UUID.fromString(recentProject.id()), userId).orElse(null)
                : null;

        List<WorkflowTask> tasks = List.of(
                new WorkflowTask("write", "写小说", "创建作品、整理大纲并进入章节写作", "/write", true),
                new WorkflowTask("rewrite", "改小说", "选择章节或导入稿件，润色、扩写、缩写并检查一致性", "/rewrite", true),
                new WorkflowTask("script", "小说改剧本", "选择作品生成剧本草稿，逐场校订并导出", "/script-flow", true)
        );
        return new Dashboard(projects, continueChapter, recentDraft, 0, tasks);
    }

    @Transactional(readOnly = true)
    public WriteState writeState(UUID projectId, UUID userId) {
        ProjectInfo project = projectService.getProject(projectId, userId);
        OutlineInfo outline = projectService.getOutline(projectId, userId);
        ChapterInfo first = firstChapter(outline, userId).orElse(null);
        int characterCount = characterService.listCharacters(projectId, userId).size();
        int worldCount = worldElementService.listWorldElements(projectId, userId).size();
        return new WriteState(project, outline, first, characterCount, worldCount, false);
    }

    @Transactional(readOnly = true)
    public RewriteState rewriteState(UUID projectId, UUID userId) {
        ProjectInfo project = projectService.getProject(projectId, userId);
        OutlineInfo outline = projectService.getOutline(projectId, userId);
        List<ChapterBrief> chapters = chapterBriefs(outline);
        return new RewriteState(project, outline, chapters,
                styleTemplateService.list(userId, "polish"), true, null);
    }

    @Transactional(readOnly = true)
    public ScriptState scriptState(UUID projectId, UUID userId) {
        ProjectInfo project = projectService.getProject(projectId, userId);
        OutlineInfo outline = projectService.getOutline(projectId, userId);
        List<ScriptDraftInfo> drafts = scriptService.listDrafts(projectId, userId);
        ScriptDraftInfo latest = drafts.stream()
                .max(Comparator.comparing(ScriptDraftInfo::updatedAt))
                .orElse(null);
        int sceneCount = latest != null
                ? scriptService.listScenes(UUID.fromString(latest.id()), userId, 0, 1).total()
                : 0;
        return new ScriptState(project, outline, drafts, latest, sceneCount, chapterBriefs(outline));
    }

    private Optional<ChapterInfo> firstChapter(OutlineInfo outline, UUID userId) {
        return outline.volumes().stream()
                .flatMap(v -> v.chapters().stream())
                .findFirst()
                .map(ch -> projectService.getChapter(UUID.fromString(ch.id()), userId));
    }

    private Optional<ScriptDraftInfo> latestDraft(UUID projectId, UUID userId) {
        return scriptService.listDrafts(projectId, userId).stream()
                .max(Comparator.comparing(ScriptDraftInfo::updatedAt));
    }

    private static List<ChapterBrief> chapterBriefs(OutlineInfo outline) {
        return outline.volumes().stream()
                .flatMap(volume -> volume.chapters().stream()
                        .map(chapter -> new ChapterBrief(
                                chapter.id().toString(),
                                chapter.title(),
                                volume.title(),
                                chapter.wordCount(),
                                chapter.status())))
                .toList();
    }
}
