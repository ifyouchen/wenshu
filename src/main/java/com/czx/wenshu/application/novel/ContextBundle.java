package com.czx.wenshu.application.novel;

/**
 * 动态上下文包（P5-06）：供 AI 续写/润色等操作注入上下文。
 * systemContext 包含锁定角色档案和世界观设定，注入 system prompt；
 * recentContent 包含近期章节原文，注入 user prompt。
 */
public record ContextBundle(
        String systemContext,
        String recentContent,
        int estimatedTokens,
        int lockedCharacterCount,
        int lockedWorldElementCount,
        int includedChapterCount) {

    public String toSystemPrompt(String basePrompt) {
        if (systemContext == null || systemContext.isBlank()) {
            return basePrompt;
        }
        return basePrompt + "\n\n" + systemContext;
    }

    public String toUserPrompt(String instruction) {
        if (recentContent == null || recentContent.isBlank()) {
            return instruction;
        }
        return "【近期内容参考】\n" + recentContent + "\n\n" + instruction;
    }
}
