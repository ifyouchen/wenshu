package com.czx.wenshu.application.novel;

import com.czx.wenshu.application.llm.PromptTemplate;
import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.project.Chapter;
import com.czx.wenshu.domain.project.ChapterRepository;
import com.czx.wenshu.domain.project.ProjectRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SSE 续写上下文准备（P5-07）。
 * 负责鉴权、加载章节、组装上下文，不涉及 Spring Web 类型。
 */
@Service
public class ContinueService {

    private static final String CONTINUE_SYSTEM =
            "你是一位专业的网文续写助手，文风流畅自然，擅长保持角色一致性与情节张力。请根据提供的上下文和近期内容自然续写，直接输出续写文本，无需说明。";

    private final ChapterRepository chapterRepository;
    private final ProjectRepository projectRepository;
    private final ContextAssemblyService contextAssemblyService;

    public ContinueService(ChapterRepository chapterRepository,
                            ProjectRepository projectRepository,
                            ContextAssemblyService contextAssemblyService) {
        this.chapterRepository = chapterRepository;
        this.projectRepository = projectRepository;
        this.contextAssemblyService = contextAssemblyService;
    }

    /**
     * 准备续写上下文（同步），返回 systemPrompt + userPrompt，
     * 供控制器层传给 StreamingLlmClient。
     *
     * @param userId    当前用户 ID
     * @param chapterId 当前正在编辑的章节
     * @param instruction 续写指示（可为 null/空）
     * @param length    期望续写字数（默认 500）
     */
    @Transactional(readOnly = true)
    public ContinueContext prepare(UUID userId, UUID chapterId, String instruction, int length) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "章节不存在"));
        if (!projectRepository.existsByIdAndUserId(chapter.projectId(), userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "无权访问该章节");
        }

        // 组装动态上下文（锁定角色、世界观、近期内容）
        ContextBundle ctx = contextAssemblyService.assemble(chapter.projectId(), chapterId, 3000);

        // 填充续写 Prompt 模板
        PromptTemplate tpl = PromptTemplate.fromClasspath("prompts/continue.txt");
        String safeInstruction = (instruction != null && !instruction.isBlank()) ? instruction : "自然延续当前情节";
        String userPrompt = tpl.fill(Map.of(
                "length", String.valueOf(Math.max(100, length)),
                "instruction", safeInstruction));

        // systemPrompt = base system + 角色/世界观上下文
        String systemPrompt = ctx.toSystemPrompt(CONTINUE_SYSTEM);

        // 若有近期内容，拼入 userPrompt
        if (ctx.recentContent() != null && !ctx.recentContent().isBlank()) {
            userPrompt = "【最近原文参考（仅供续写参考，无需复述）】\n"
                    + ctx.recentContent() + "\n\n" + userPrompt;
        }

        return new ContinueContext(systemPrompt, userPrompt);
    }
}
