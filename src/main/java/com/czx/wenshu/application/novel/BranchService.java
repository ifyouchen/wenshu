package com.czx.wenshu.application.novel;

import com.czx.wenshu.application.llm.JsonExtractor;
import com.czx.wenshu.application.llm.LlmClient;
import com.czx.wenshu.application.llm.PromptTemplate;
import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.project.Chapter;
import com.czx.wenshu.domain.project.ChapterRepository;
import com.czx.wenshu.domain.project.ProjectRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 卡点分支建议（P5-08）。 */
@Service
public class BranchService {

    private static final TypeReference<List<BranchOption>> BRANCH_LIST_TYPE = new TypeReference<>() {};
    private static final String BRANCH_SYSTEM = "你是一位专业的网文剧情策划师，擅长设计有张力的剧情分支。请只输出 JSON 数组，不要其他内容。";

    private final ChapterRepository chapterRepository;
    private final ProjectRepository projectRepository;
    private final ContextAssemblyService contextAssemblyService;
    private final LlmClient creativeLlmClient;
    private final ObjectMapper objectMapper;

    public BranchService(ChapterRepository chapterRepository,
                          ProjectRepository projectRepository,
                          ContextAssemblyService contextAssemblyService,
                          @Qualifier("creativeLlmClient") LlmClient creativeLlmClient,
                          ObjectMapper objectMapper) {
        this.chapterRepository = chapterRepository;
        this.projectRepository = projectRepository;
        this.contextAssemblyService = contextAssemblyService;
        this.creativeLlmClient = creativeLlmClient;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<BranchOption> getBranches(UUID userId, UUID chapterId, int branchCount) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "章节不存在"));
        if (!projectRepository.existsByIdAndUserId(chapter.projectId(), userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "无权访问该章节");
        }

        ContextBundle ctx = contextAssemblyService.assemble(chapter.projectId(), chapterId, 2000);
        PromptTemplate tpl = PromptTemplate.fromClasspath("prompts/branch.txt");
        String userPrompt = tpl.fill(Map.of("branchCount", String.valueOf(Math.max(1, branchCount))));

        if (ctx.recentContent() != null && !ctx.recentContent().isBlank()) {
            userPrompt = "【当前情节】\n" + ctx.recentContent() + "\n\n" + userPrompt;
        }

        String systemPrompt = ctx.toSystemPrompt(BRANCH_SYSTEM);
        String response = creativeLlmClient.chat(systemPrompt, userPrompt);

        List<BranchOption> branches = JsonExtractor.parseArray(response, BRANCH_LIST_TYPE, objectMapper);
        if (branches == null || branches.isEmpty()) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "分支建议解析失败，请重试");
        }
        return branches;
    }
}
