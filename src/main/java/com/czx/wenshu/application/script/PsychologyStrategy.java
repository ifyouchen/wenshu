package com.czx.wenshu.application.script;

/**
 * 心理外化策略枚举（P7-03）。
 * 控制剧本改编时如何处理角色的内心想法和心理描写。
 */
public enum PsychologyStrategy {

    /**
     * 动作化：人物内心通过行为、表情、肢体动作体现，不直接表达内心。
     */
    ACTION("action"),

    /**
     * 对话化：将内心想法转化为角色的自言自语或与他人的对话。
     */
    DIALOGUE("dialogue"),

    /**
     * 旁白/画外音：保留内心声音，以 V.O.（Voice Over）形式呈现。
     */
    VOICEOVER("voiceover"),

    /**
     * 跳过：大幅压缩或省略心理描写，仅保留外部行动线。
     */
    SKIP("skip");

    /** 字符串值，用于 API 传参。 */
    private final String value;

    PsychologyStrategy(String value) {
        this.value = value;
    }

    public String value() { return value; }

    /**
     * 从字符串值解析枚举，不匹配时默认 ACTION。
     *
     * @param val 输入字符串
     * @return 对应枚举值
     */
    public static PsychologyStrategy fromValue(String val) {
        for (PsychologyStrategy s : values()) {
            if (s.value.equalsIgnoreCase(val)) return s;
        }
        return ACTION;
    }
}
