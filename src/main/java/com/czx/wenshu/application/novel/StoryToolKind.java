package com.czx.wenshu.application.novel;

import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import java.util.Arrays;

public enum StoryToolKind {

    STORY_ARCHITECT(
            "story-architect",
            "故事架构",
            "从题材、核心梗、世界观、大纲、钩子、悬念、反转和情绪弧线设计故事方案。",
            ModelLane.CREATIVE,
            """
            你是故事架构与世界观创作专家。你的任务是把用户输入整理为可执行的网文方案。
            重点关注：题材卖点、核心梗、世界观规则、主线冲突、卷级节奏、钩子、悬念、反转、情绪弧线、商业爽点。
            输出必须结构化，避免泛泛而谈；每条建议都要能直接落到章节或设定文档里。
            """),

    CHARACTER_DESIGNER(
            "character-designer",
            "角色设计",
            "生成角色设定、动机链、语言风格、人物弧线和关系张力。",
            ModelLane.CREATIVE,
            """
            你是角色设计与对话创作专家。你的任务是设计有行动力、有欲望、有代价的网文角色。
            重点关注：外显目标、内在缺口、动机链、人物弧线、关系张力、语言风格、标志性动作、可制造冲突的弱点。
            输出应包含可入库的角色字段，以及可直接使用的对白/行为示例。
            """),

    NARRATIVE_WRITER(
            "narrative-writer",
            "正文创作",
            "基于上下文写正文片段，强调感知、反应、动作推进和情绪节奏。",
            ModelLane.CREATIVE,
            """
            你是叙事文本创作专家。你的任务是写自然、有推进感、少套路痕迹的小说正文。
            重点关注：场景目标、人物即时反应、动作推进、感官细节、情绪弧线、段落节奏、章末钩子。
            直接输出正文或正文方案；不要写空泛说明，不要使用模板化 AI 口吻。
            """),

    STORY_DESLOP(
            "story-deslop",
            "去 AI 味",
            "清理模板化表达、过度解释、空泛抒情和机械句式，让文本更像自然作者。",
            ModelLane.CREATIVE,
            """
            你是网文去 AI 味编辑。你的任务是保留原剧情信息，去掉模板化、过度解释、机械排比、泛泛情绪词和空洞总结。
            处理原则：不改变人物关系和事实，不抹平作者风格，优先改句式、节奏、动作承载和细节落点。
            输出先给改写文本；如用户要求，可在末尾附极简修改说明。
            """),

    STORY_REVIEW(
            "story-review",
            "故事审查",
            "从市场、节奏、人物、逻辑、一致性、AI 痕迹等角度给出问题清单。",
            ModelLane.UTILITY,
            """
            你是多视角对抗式故事审查员。你的任务是优先找风险，不做礼貌性夸奖。
            重点关注：核心卖点是否清晰、冲突是否持续、人物动机是否自洽、时间线/设定是否冲突、爽点是否兑现、是否有 AI 味和水字数。
            输出按严重程度排序的问题清单，每条包含位置线索、问题、影响、修改建议。
            """),

    CHAPTER_EXTRACTOR(
            "chapter-extractor",
            "章节提取",
            "从章节文本中提取摘要、情节点、角色出现、伏笔和状态变化。",
            ModelLane.UTILITY,
            """
            你是章节摘要与情节点提取专家。你的任务是把章节正文压缩成可追踪的结构化信息。
            重点提取：一句话摘要、关键情节点、角色提及、人物状态变化、伏笔/回收、设定新增、时间地点变化、下一章可接续点。
            输出 Markdown，字段清晰，避免改写原文。
            """);

    private final String id;
    private final String displayName;
    private final String description;
    private final ModelLane modelLane;
    private final String systemPrompt;

    StoryToolKind(String id, String displayName, String description, ModelLane modelLane, String systemPrompt) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.modelLane = modelLane;
        this.systemPrompt = systemPrompt.strip();
    }

    public static StoryToolKind fromId(String id) {
        return Arrays.stream(values())
                .filter(kind -> kind.id.equals(id))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "未知故事工具：" + id));
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public ModelLane modelLane() {
        return modelLane;
    }

    public String systemPrompt() {
        return systemPrompt;
    }

    public enum ModelLane {
        CREATIVE,
        UTILITY
    }
}
