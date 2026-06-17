package com.czx.wenshu.application.novel;

import com.czx.wenshu.application.project.ChapterInfo;
import com.czx.wenshu.application.project.ProjectApplicationService;
import com.czx.wenshu.application.project.UpdateChapterCommand;
import com.czx.wenshu.application.stats.WritingStatsService;
import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 章节改稿流程服务：生成建议与应用结果。 */
@Service
public class ChapterRewriteService {

    private final ProjectApplicationService projectService;
    private final PolishService polishService;
    private final WritingStatsService writingStatsService;

    public ChapterRewriteService(ProjectApplicationService projectService,
                                 PolishService polishService,
                                 WritingStatsService writingStatsService) {
        this.projectService = projectService;
        this.polishService = polishService;
        this.writingStatsService = writingStatsService;
    }

    @Transactional(readOnly = true)
    public RewriteSuggestion rewrite(UUID chapterId, UUID userId, RewriteMode mode,
                                     String instruction, String selectedText) {
        ChapterInfo chapter = projectService.getChapter(chapterId, userId);
        String source = selectedText != null && !selectedText.isBlank()
                ? selectedText
                : stripHtml(chapter.content());
        if (source == null || source.isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "待改稿文本不能为空");
        }
        String finalInstruction = instructionFor(mode, instruction);
        PolishResult result = polishService.advancedPolish(source, finalInstruction);
        String rewritten = result.rewritten() != null ? result.rewritten() : source;
        return new RewriteSuggestion(mode.value(), source, rewritten, finalInstruction);
    }

    @Transactional
    public ChapterInfo apply(UUID chapterId, UUID userId, String content,
                             int acceptedChars, String snapshotLabel) {
        ChapterInfo before = projectService.getChapter(chapterId, userId);
        if (content == null || content.isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "应用内容不能为空");
        }
        projectService.createSnapshot(chapterId, userId, "rewrite_apply",
                snapshotLabel != null && !snapshotLabel.isBlank() ? snapshotLabel : "改稿应用前快照");
        ChapterInfo updated = projectService.updateChapter(chapterId, userId,
                new UpdateChapterCommand(before.title(), content, before.outline(), before.status()));
        if (acceptedChars > 0) {
            writingStatsService.recordAiAcceptedDelta(userId, UUID.fromString(updated.projectId()), acceptedChars);
        }
        return updated;
    }

    private static String instructionFor(RewriteMode mode, String custom) {
        String extra = custom != null ? custom.trim() : "";
        return switch (mode) {
            case POLISH -> append("润色文字，保留原意，让表达更自然、有画面感。", extra);
            case EXPAND -> append("扩写文本，补足动作、场景、情绪和节奏，但不要偏离原剧情。", extra);
            case SHORTEN -> append("缩写文本，压缩冗余表达，保留关键信息和情绪推进。", extra);
            case CUSTOM -> extra.isBlank() ? "按用户要求改写文本。" : extra;
        };
    }

    private static String append(String base, String extra) {
        return extra == null || extra.isBlank() ? base : base + "\n额外要求：" + extra;
    }

    private static String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", "\n")
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    public enum RewriteMode {
        POLISH("polish"),
        EXPAND("expand"),
        SHORTEN("shorten"),
        CUSTOM("custom");

        private final String value;

        RewriteMode(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        public static RewriteMode from(String value) {
            if (value == null || value.isBlank()) return POLISH;
            for (RewriteMode mode : values()) {
                if (mode.value.equalsIgnoreCase(value)) return mode;
            }
            throw new ApiException(ErrorCode.BAD_REQUEST, "改稿模式仅支持 polish、expand、shorten、custom");
        }
    }

    public record RewriteSuggestion(String mode, String source, String rewritten, String instruction) {}
}
