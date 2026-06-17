package com.czx.wenshu.application.novel;

import com.czx.wenshu.application.llm.LlmClient;
import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.project.Chapter;
import com.czx.wenshu.domain.project.ChapterRepository;
import com.czx.wenshu.domain.project.Project;
import com.czx.wenshu.domain.project.ProjectRepository;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoryToolService {

    private static final Logger log = LoggerFactory.getLogger(StoryToolService.class);

    private static final int MAX_FREE_INPUT_CHARS = 12_000;
    private static final int MAX_CONTEXT_CHARS = 6_000;

    private final ProjectRepository projectRepository;
    private final ChapterRepository chapterRepository;
    private final ContextAssemblyService contextAssemblyService;
    private final LlmClient creativeLlmClient;
    private final LlmClient utilityLlmClient;

    public StoryToolService(ProjectRepository projectRepository,
                            ChapterRepository chapterRepository,
                            ContextAssemblyService contextAssemblyService,
                            @Qualifier("creativeLlmClient") LlmClient creativeLlmClient,
                            @Qualifier("utilityLlmClient") LlmClient utilityLlmClient) {
        this.projectRepository = projectRepository;
        this.chapterRepository = chapterRepository;
        this.contextAssemblyService = contextAssemblyService;
        this.creativeLlmClient = creativeLlmClient;
        this.utilityLlmClient = utilityLlmClient;
    }

    public List<StoryToolInfo> listTools() {
        return Arrays.stream(StoryToolKind.values())
                .map(StoryToolInfo::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StoryToolResult runTool(UUID userId, String toolId, UUID projectId, UUID chapterId,
                                   String input, String instruction, Integer targetWords) {
        StoryToolKind kind = StoryToolKind.fromId(toolId);
        Chapter chapter = loadChapter(userId, chapterId);
        Project project = loadProject(userId, projectId, chapter);

        if (project == null && chapter == null) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "projectId 和 chapterId 至少提供一个");
        }

        String effectiveInput = firstNonBlank(input, chapter != null ? chapter.content() : null);
        if (effectiveInput == null && isTextRequired(kind)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, kind.displayName() + "需要提供 input 或带正文的 chapterId");
        }

        String systemPrompt = buildSystemPrompt(kind, project, chapter);
        String userPrompt = buildUserPrompt(kind, project, chapter, effectiveInput, instruction, targetWords);
        String output = clientFor(kind).chat(systemPrompt, userPrompt);

        UUID resultProjectId = project != null ? project.id() : null;
        UUID resultChapterId = chapter != null ? chapter.id() : null;
        log.info("[StoryToolService] 工具执行完成 tool={} userId={} projectId={} chapterId={} outputLength={}",
                kind.id(), userId, resultProjectId, resultChapterId, output != null ? output.length() : 0);
        return new StoryToolResult(kind.id(), resultProjectId, resultChapterId, output);
    }

    private Chapter loadChapter(UUID userId, UUID chapterId) {
        if (chapterId == null) {
            return null;
        }
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "章节不存在"));
        if (!projectRepository.existsByIdAndUserId(chapter.projectId(), userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "无权访问该章节");
        }
        return chapter;
    }

    private Project loadProject(UUID userId, UUID projectId, Chapter chapter) {
        UUID effectiveProjectId = projectId != null ? projectId : (chapter != null ? chapter.projectId() : null);
        if (effectiveProjectId == null) {
            return null;
        }
        if (chapter != null && projectId != null && !chapter.projectId().equals(projectId)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "projectId 与 chapterId 不属于同一作品");
        }
        return projectRepository.findById(effectiveProjectId)
                .filter(project -> project.userId().equals(userId))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "作品不存在"));
    }

    private boolean isTextRequired(StoryToolKind kind) {
        return kind == StoryToolKind.STORY_LONG_ANALYZE
                || kind == StoryToolKind.STORY_SHORT_ANALYZE
                || kind == StoryToolKind.STORY_IMPORT
                || kind == StoryToolKind.STORY_DESLOP
                || kind == StoryToolKind.STORY_REVIEW
                || kind == StoryToolKind.CONSISTENCY_CHECKER
                || kind == StoryToolKind.CHAPTER_EXTRACTOR;
    }

    private LlmClient clientFor(StoryToolKind kind) {
        return kind.modelLane() == StoryToolKind.ModelLane.UTILITY ? utilityLlmClient : creativeLlmClient;
    }

    private String buildSystemPrompt(StoryToolKind kind, Project project, Chapter chapter) {
        StringBuilder prompt = new StringBuilder(kind.systemPrompt());
        if (project != null) {
            ContextBundle bundle = contextAssemblyService.assemble(project.id(), chapter != null ? chapter.id() : null, MAX_CONTEXT_CHARS);
            if (bundle.systemContext() != null && !bundle.systemContext().isBlank()) {
                prompt.append("\n\n【项目内已锁定上下文】\n").append(bundle.systemContext());
            }
        }
        return prompt.toString();
    }

    private String buildUserPrompt(StoryToolKind kind, Project project, Chapter chapter,
                                   String input, String instruction, Integer targetWords) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请执行工具：").append(kind.displayName()).append("\n\n");
        if (project != null) {
            prompt.append("【作品信息】\n")
                    .append("- 标题：").append(nullToEmpty(project.title())).append('\n')
                    .append("- 题材：").append(nullToEmpty(project.genre())).append('\n')
                    .append("- 简介：").append(nullToEmpty(project.synopsis())).append('\n')
                    .append("- 世界观：").append(nullToEmpty(project.worldview())).append("\n\n");
        }
        if (chapter != null) {
            prompt.append("【章节信息】\n")
                    .append("- 标题：").append(nullToEmpty(chapter.title())).append('\n')
                    .append("- 大纲：").append(nullToEmpty(chapter.outline())).append('\n')
                    .append("- 字数：").append(chapter.wordCount()).append("\n\n");
        }
        if (targetWords != null && targetWords > 0) {
            prompt.append("【目标字数】\n").append(targetWords).append("\n\n");
        }
        if (instruction != null && !instruction.isBlank()) {
            prompt.append("【用户指令】\n").append(instruction.trim()).append("\n\n");
        }
        if (input != null && !input.isBlank()) {
            prompt.append("【待处理内容】\n").append(truncate(input, MAX_FREE_INPUT_CHARS)).append("\n\n");
        }
        prompt.append(outputContract(kind));
        return prompt.toString();
    }

    private String outputContract(StoryToolKind kind) {
        return switch (kind) {
            case STORY_ROUTER -> "请输出：意图判断、推荐工具、所需输入、执行步骤、预期产物。";
            case STORY_LONG_WRITE -> "请输出：目标情绪、题材定位、故事核、长线冲突、人物功能位、卷/章推进方案；如果用户要求正文，则直接输出正文。";
            case STORY_SHORT_WRITE -> "请输出：目标情绪、核心反转、前三句钩子、情绪推进节点、结尾余韵；如果用户要求成稿，则直接输出短篇正文。";
            case STORY_LONG_ANALYZE -> "请输出：黄金三章拆解、主角欲望、冲突引爆、爽点承诺、人物功能位、节奏模板、可复用经验。";
            case STORY_SHORT_ANALYZE -> "请输出：故事核、目标情绪、开头钩子、铺垫节点、反转设计、释放段、传播点、可复用写法。";
            case STORY_LONG_SCAN -> "请输出：平台样本、重复模式、题材趋势、读者需求、风险阈值、适合作者条件、验证动作。";
            case STORY_SHORT_SCAN -> "请输出：样本日期、高频情绪、触发场景、反转类型、传播点、信号强度、饱和风险、复扫节点。";
            case STORY_IMPORT -> "请输出：篇幅判断、章节结构、故事核、角色表、设定表、伏笔表、当前进度、需复核项、后续写作建议。";
            case STORY_COVER -> "请输出：封面定位、主体画面、构图、色彩、字体、图像生成提示词、负面提示词、不同平台适配建议。";
            case STORY_RESEARCHER -> "请输出：资料卡、可写细节、冲突素材、常见误区、待核验问题。";
            case STORY_EXPLORER -> "请输出：已确定事实、项目内依据、推断、缺失信息、建议补录项。";
            case STORY_ARCHITECT -> "请输出：核心卖点、故事核、世界观规则、主线冲突、三幕/卷级推进、前 3 章钩子、风险点。";
            case CHARACTER_DESIGNER -> "请输出：角色卡、欲望/恐惧/弱点、动机链、人物弧线、关系张力、口癖/动作、可直接入库字段。";
            case NARRATIVE_WRITER -> "请输出可直接放入正文的文本；如需要说明，只放在正文后 3 条以内。";
            case STORY_DESLOP -> "请先输出改写后的完整文本，再输出不超过 5 条的修改要点。";
            case STORY_REVIEW -> "请按 S1-S4 严重度输出问题清单：位置线索、问题、影响、修改建议。";
            case CONSISTENCY_CHECKER -> "请输出：冲突位置、项目内依据、冲突类型、影响范围、修复建议。";
            case CHAPTER_EXTRACTOR -> "请输出：一句话摘要、关键情节点、角色状态变化、伏笔/回收、设定新增、时间地点、下一章接续点。";
        };
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String truncate(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars) + "\n\n【内容已截断，保留前 " + maxChars + " 字】";
    }
}
