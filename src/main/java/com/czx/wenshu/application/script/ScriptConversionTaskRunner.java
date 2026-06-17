package com.czx.wenshu.application.script;

import com.czx.wenshu.application.llm.JsonExtractor;
import com.czx.wenshu.application.llm.LlmClient;
import com.czx.wenshu.application.llm.PromptTemplate;
import com.czx.wenshu.application.task.AsyncTaskService;
import com.czx.wenshu.domain.project.Chapter;
import com.czx.wenshu.domain.project.ChapterRepository;
import com.czx.wenshu.domain.script.ScriptDraft;
import com.czx.wenshu.domain.script.ScriptDraftRepository;
import com.czx.wenshu.domain.script.ScriptScene;
import com.czx.wenshu.domain.script.ScriptSceneRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 剧本异步改编执行器（P7-04）。
 * 依次完成：场景分割（P7-02）→ 心理外化（P7-03）→ 场景保存 → 草稿标记就绪。
 */
@Component
public class ScriptConversionTaskRunner {

    private static final Logger log = LoggerFactory.getLogger(ScriptConversionTaskRunner.class);
    private static final TypeReference<List<ProtoScene>> PROTO_SCENE_LIST = new TypeReference<>() {};
    /** 单章内容截取上限，避免超出 LLM 上下文。 */
    private static final int MAX_CHAPTER_CONTENT = 2000;

    private final AsyncTaskService asyncTaskService;
    private final ScriptDraftRepository draftRepository;
    private final ScriptSceneRepository sceneRepository;
    private final ChapterRepository chapterRepository;
    private final LlmClient creativeLlmClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ScriptConversionTaskRunner(AsyncTaskService asyncTaskService,
                                       ScriptDraftRepository draftRepository,
                                       ScriptSceneRepository sceneRepository,
                                       ChapterRepository chapterRepository,
                                       @Qualifier("creativeLlmClient") LlmClient creativeLlmClient,
                                       ObjectMapper objectMapper,
                                       Clock clock) {
        this.asyncTaskService = asyncTaskService;
        this.draftRepository = draftRepository;
        this.sceneRepository = sceneRepository;
        this.chapterRepository = chapterRepository;
        this.creativeLlmClient = creativeLlmClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * 异步执行剧本改编全流程（P7-04）。
     *
     * @param taskId   进度任务 ID
     * @param draftId  目标草稿 ID
     * @param projectId 作品 ID
     * @param psychologyStrategy 心理外化策略（P7-03）
     * @param chapterIds 改编章节范围，空表示整部作品
     */
    @Async("aiTaskExecutor")
    @Transactional
    public void run(UUID taskId, UUID draftId, UUID projectId, String psychologyStrategy, List<UUID> chapterIds) {
        log.info("[ScriptConversionTaskRunner] 开始剧本改编 taskId={} draftId={}", taskId, draftId);
        try {
            asyncTaskService.markRunning(taskId, 3, "收集章节内容");
            List<Chapter> chapters = chapterRepository.findByProjectId(projectId);
            if (chapterIds != null && !chapterIds.isEmpty()) {
                Set<UUID> selected = new HashSet<>(chapterIds);
                chapters = chapters.stream()
                        .filter(chapter -> selected.contains(chapter.id()))
                        .collect(Collectors.toList());
            }
            if (chapters.isEmpty()) {
                asyncTaskService.fail(taskId, "作品暂无章节内容，无法改编");
                markDraftFailed(draftId);
                return;
            }

            asyncTaskService.updateProgress(taskId, 1, "场景分割与改编中", 20);
            AtomicInteger globalSceneIndex = new AtomicInteger(0);
            int totalScenes = 0;

            for (Chapter chapter : chapters) {
                if (chapter.content() == null || chapter.content().isBlank()) continue;
                String content = truncate(chapter.content(), MAX_CHAPTER_CONTENT);
                List<ProtoScene> protoScenes = splitChapterIntoScenes(chapter.title(), content);
                for (ProtoScene proto : protoScenes) {
                    String scriptContent = convertProtoScene(proto, psychologyStrategy);
                    ScriptScene scene = ScriptScene.create(draftId, globalSceneIndex.getAndIncrement(), proto.sourceContent(), clock);
                    String charsJson = buildCharactersJson(proto.characters());
                    // 通过临时对象设置场景属性
                    ScriptScene filled = ScriptScene.rehydrate(scene.id(), draftId, null,
                            scene.sceneIndex(), proto.location(), proto.timeDesc(),
                            proto.isInterior(), charsJson, scriptContent,
                            proto.sourceContent(), 0, scene.createdAt(), scene.updatedAt());
                    sceneRepository.save(filled);
                    totalScenes++;
                }
            }

            asyncTaskService.updateProgress(taskId, 2, "保存草稿信息", 90);
            markDraftReady(draftId, totalScenes);
            asyncTaskService.completeWithJson(taskId,
                    "{\"draftId\":\"" + draftId + "\",\"scenes\":" + totalScenes + "}");
            log.info("[ScriptConversionTaskRunner] 剧本改编完成 draftId={} scenes={}", draftId, totalScenes);
        } catch (Exception e) {
            log.warn("[ScriptConversionTaskRunner] 剧本改编失败 taskId={}", taskId, e);
            asyncTaskService.fail(taskId, e.getMessage());
            markDraftFailed(draftId);
        }
    }

    /**
     * 调用 LLM 将章节内容切分为场景列表（P7-02）。
     *
     * @param title   章节标题
     * @param content 章节内容（已截断）
     * @return 场景列表（解析失败时返回单场景列表）
     */
    private List<ProtoScene> splitChapterIntoScenes(String title, String content) {
        try {
            PromptTemplate tpl = PromptTemplate.fromClasspath("prompts/scene_split.txt");
            String prompt = tpl.fill(Map.of(
                    "chapterTitle", title != null ? title : "",
                    "content", content));
            String response = creativeLlmClient.chat(null, prompt);
            List<ProtoScene> scenes = JsonExtractor.parseArray(response, PROTO_SCENE_LIST, objectMapper);
            return scenes != null && !scenes.isEmpty() ? scenes
                    : List.of(new ProtoScene(null, null, null, List.of(), content));
        } catch (Exception e) {
            log.warn("[ScriptConversionTaskRunner] 场景分割失败，使用整章作为单场景 chapter={}", title);
            return List.of(new ProtoScene(null, null, null, List.of(), content));
        }
    }

    /**
     * 调用 LLM 将原文场景片段转换为剧本格式（P7-03）。
     *
     * @param proto              场景原始数据
     * @param psychologyStrategy 心理外化策略
     * @return 转换后的剧本文本
     */
    private String convertProtoScene(ProtoScene proto, String psychologyStrategy) {
        if (proto.sourceContent() == null || proto.sourceContent().isBlank()) return "";
        try {
            PromptTemplate tpl = PromptTemplate.fromClasspath("prompts/scene_convert.txt");
            String prompt = tpl.fill(Map.of(
                    "psychologyStrategy", psychologyStrategy.toUpperCase(),
                    "location", proto.location() != null ? proto.location() : "未指定",
                    "interior", Boolean.TRUE.equals(proto.isInterior()) ? "室内" : "室外",
                    "timeDesc", proto.timeDesc() != null ? proto.timeDesc() : "",
                    "characters", proto.characters() != null ? String.join("、", proto.characters()) : "",
                    "sourceContent", truncate(proto.sourceContent(), 1000)));
            return creativeLlmClient.chat(null, prompt);
        } catch (Exception e) {
            log.debug("[ScriptConversionTaskRunner] 场景转换失败，保留原文 error={}", e.getMessage());
            return proto.sourceContent();
        }
    }

    private void markDraftReady(UUID draftId, int totalScenes) {
        draftRepository.findById(draftId).ifPresent(d -> {
            d.markReady(totalScenes, clock);
            draftRepository.save(d);
        });
    }

    private void markDraftFailed(UUID draftId) {
        draftRepository.findById(draftId).ifPresent(d -> {
            d.markFailed(clock);
            draftRepository.save(d);
        });
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "…";
    }

    private String buildCharactersJson(List<String> characters) {
        if (characters == null || characters.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(characters);
        } catch (Exception e) {
            return "[]";
        }
    }
}
