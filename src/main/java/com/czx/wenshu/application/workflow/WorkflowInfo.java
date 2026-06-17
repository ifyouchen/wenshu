package com.czx.wenshu.application.workflow;

import com.czx.wenshu.application.project.ChapterInfo;
import com.czx.wenshu.application.project.OutlineInfo;
import com.czx.wenshu.application.project.ProjectInfo;
import com.czx.wenshu.application.script.ScriptDraftInfo;
import com.czx.wenshu.application.user.StyleTemplateInfo;
import java.util.List;

/** 页面工作流聚合 DTO。 */
public record WorkflowInfo() {

    public record Dashboard(
            List<ProjectInfo> recentProjects,
            ChapterInfo continueChapter,
            ScriptDraftInfo recentScriptDraft,
            int openConsistencyItems,
            List<WorkflowTask> tasks) {}

    public record WorkflowTask(
            String key,
            String title,
            String description,
            String route,
            boolean enabled) {}

    public record WriteState(
            ProjectInfo project,
            OutlineInfo outline,
            ChapterInfo firstEditableChapter,
            int characterCount,
            int worldElementCount,
            boolean hasSkeletonTask) {}

    public record RewriteState(
            ProjectInfo project,
            OutlineInfo outline,
            List<ChapterBrief> chapters,
            List<StyleTemplateInfo> styleTemplates,
            boolean importAvailable,
            String latestReportId) {}

    public record ScriptState(
            ProjectInfo project,
            OutlineInfo outline,
            List<ScriptDraftInfo> drafts,
            ScriptDraftInfo latestDraft,
            int latestDraftSceneCount,
            List<ChapterBrief> adaptableChapters) {}

    public record ChapterBrief(
            String id,
            String title,
            String volumeTitle,
            int wordCount,
            String status) {}
}
