package com.czx.wenshu.application.script;

import com.czx.wenshu.application.task.AsyncTaskService;
import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.project.ProjectRepository;
import com.czx.wenshu.domain.script.ScriptDraft;
import com.czx.wenshu.domain.script.ScriptDraftRepository;
import com.czx.wenshu.domain.script.ScriptEpisode;
import com.czx.wenshu.domain.script.ScriptEpisodeRepository;
import com.czx.wenshu.domain.script.ScriptScene;
import com.czx.wenshu.domain.script.ScriptSceneRepository;
import com.czx.wenshu.domain.task.AsyncTask;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 剧本草稿服务（P7-01）。
 * 提供草稿列表/详情查询、场景分页、场景编辑（含乐观锁）。
 */
@Service
public class ScriptService {

    private static final Logger log = LoggerFactory.getLogger(ScriptService.class);

    private final ScriptDraftRepository draftRepository;
    private final ScriptSceneRepository sceneRepository;
    private final ScriptEpisodeRepository episodeRepository;
    private final ProjectRepository projectRepository;
    private final AsyncTaskService asyncTaskService;
    private final ScriptExportTaskRunner exportTaskRunner;
    private final Clock clock;

    public ScriptService(ScriptDraftRepository draftRepository,
                          ScriptSceneRepository sceneRepository,
                          ScriptEpisodeRepository episodeRepository,
                          ProjectRepository projectRepository,
                          AsyncTaskService asyncTaskService,
                          ScriptExportTaskRunner exportTaskRunner,
                          Clock clock) {
        this.draftRepository = draftRepository;
        this.sceneRepository = sceneRepository;
        this.episodeRepository = episodeRepository;
        this.projectRepository = projectRepository;
        this.asyncTaskService = asyncTaskService;
        this.exportTaskRunner = exportTaskRunner;
        this.clock = clock;
    }

    /**
     * 查询作品的剧本草稿列表（P7-01）。
     *
     * @param projectId 作品 ID
     * @param userId    当前用户 ID
     * @return 草稿信息列表
     */
    @Transactional(readOnly = true)
    public List<ScriptDraftInfo> listDrafts(UUID projectId, UUID userId) {
        if (!projectRepository.existsByIdAndUserId(projectId, userId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "作品不存在");
        }
        List<ScriptDraftInfo> drafts = draftRepository.findByProjectId(projectId)
                .stream().map(ScriptDraftInfo::from).toList();
        log.debug("[ScriptService] 查询草稿列表 projectId={} count={}", projectId, drafts.size());
        return drafts;
    }

    /**
     * 查询指定草稿详情（P7-01）。
     *
     * @param draftId 草稿 ID
     * @param userId  当前用户 ID
     * @return 草稿详情
     */
    @Transactional(readOnly = true)
    public ScriptDraftInfo getDraft(UUID draftId, UUID userId) {
        ScriptDraft draft = loadDraftAndVerify(draftId, userId);
        return ScriptDraftInfo.from(draft);
    }

    /**
     * 分页查询草稿场景列表（P7-01）。
     *
     * @param draftId 草稿 ID
     * @param userId  当前用户 ID
     * @param page    页码（从 0 开始）
     * @param size    每页条数（默认 20，最大 100）
     * @return 场景分页结果
     */
    @Transactional(readOnly = true)
    public ScenePageResult listScenes(UUID draftId, UUID userId, int page, int size) {
        loadDraftAndVerify(draftId, userId);
        int safeSize = Math.min(Math.max(1, size), 100);
        int offset = Math.max(0, page) * safeSize;
        int total = sceneRepository.countByDraftId(draftId);
        List<ScriptSceneInfo> scenes = sceneRepository.findByDraftId(draftId, offset, safeSize)
                .stream().map(ScriptSceneInfo::from).toList();
        log.debug("[ScriptService] 查询场景分页 draftId={} page={} size={} total={}", draftId, page, safeSize, total);
        return new ScenePageResult(total, page, safeSize, scenes);
    }

    /**
     * 编辑单个场景内容（P7-01/P7-06），含乐观锁校验。
     * 若 version 不匹配则抛出 {@code ApiException(VERSION_CONFLICT)}。
     *
     * @param sceneId         场景 ID
     * @param userId          当前用户 ID
     * @param content         新剧本内容
     * @param location        场景地点
     * @param timeDesc        时间描述
     * @param expectedVersion 调用方持有的版本号
     * @return 更新后的场景 DTO
     */
    @Transactional
    public ScriptSceneInfo updateScene(UUID sceneId, UUID userId, String content,
                                        String location, String timeDesc, int expectedVersion) {
        ScriptScene scene = sceneRepository.findById(sceneId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "场景不存在"));
        loadDraftAndVerify(scene.draftId(), userId);
        try {
            scene.updateContent(content, location, timeDesc, expectedVersion, clock);
        } catch (IllegalStateException e) {
            log.warn("[ScriptService] 场景编辑版本冲突 sceneId={} current={} expected={}",
                    sceneId, scene.version(), expectedVersion);
            throw new ApiException(ErrorCode.VERSION_CONFLICT, e.getMessage());
        }
        sceneRepository.save(scene);
        log.info("[ScriptService] 场景已更新 sceneId={} version={}", sceneId, scene.version());
        return ScriptSceneInfo.from(scene);
    }

    /**
     * 创建集数（P7-07）。
     *
     * @param draftId   草稿 ID
     * @param userId    当前用户 ID
     * @param title     集标题
     * @param episodeNo 集序号（从 1 开始）
     * @return 集数 DTO
     */
    @Transactional
    public ScriptEpisodeInfo createEpisode(UUID draftId, UUID userId, String title, int episodeNo) {
        loadDraftAndVerify(draftId, userId);
        int sortOrder = episodeRepository.findByDraftId(draftId).size();
        ScriptEpisode episode = ScriptEpisode.create(draftId, episodeNo, title, sortOrder, clock);
        episodeRepository.save(episode);
        log.info("[ScriptService] 创建集数 draftId={} episodeNo={}", draftId, episodeNo);
        return ScriptEpisodeInfo.from(episode);
    }

    /**
     * 查询草稿的集数列表（P7-07）。
     *
     * @param draftId 草稿 ID
     * @param userId  当前用户 ID
     * @return 集数列表（按 sort_order 正序）
     */
    @Transactional(readOnly = true)
    public List<ScriptEpisodeInfo> listEpisodes(UUID draftId, UUID userId) {
        loadDraftAndVerify(draftId, userId);
        return episodeRepository.findByDraftId(draftId).stream()
                .map(ScriptEpisodeInfo::from).toList();
    }

    /**
     * 删除集数（P7-07）。
     *
     * @param draftId   草稿 ID（用于权限验证）
     * @param episodeId 集数 ID
     * @param userId    当前用户 ID
     */
    @Transactional
    public void deleteEpisode(UUID draftId, UUID episodeId, UUID userId) {
        loadDraftAndVerify(draftId, userId);
        episodeRepository.deleteById(episodeId);
        log.info("[ScriptService] 删除集数 episodeId={}", episodeId);
    }

    /**
     * 提交剧本导出任务（P7-08）。
     * 创建异步任务，任务完成后 result_json 中包含 COS 预签名下载 URL。
     * 当前为桩实现：任务创建后立即标记为 FAILED（无真实 COS），
     * 生产环境替换 task runner 即可。
     *
     * @param draftId 草稿 ID
     * @param userId  当前用户 ID
     * @param format  导出格式（docx/fdx/storyboard）
     * @return 包含 taskId 的 Map
     */
    /**
     * 提交剧本导出任务（P7-08 / P9-06）。
     *
     * <p>异步生成带 "AI 辅助生成" 声明的导出文件，上传至腾讯云 COS，
     * 任务完成后 resultJson 包含预签名下载 URL。</p>
     *
     * @param draftId 草稿 ID
     * @param userId  当前用户 ID
     * @param format  导出格式（docx/fdx/storyboard，当前统一生成 TXT）
     * @return taskId 和 draftId
     */
    @Transactional
    public Map<String, String> submitExport(UUID draftId, UUID userId, String format) {
        loadDraftAndVerify(draftId, userId);
        AsyncTask task = asyncTaskService.createTask(userId, null, "script_export");
        log.info("[ScriptService] 提交导出任务 draftId={} format={} taskId={}", draftId, format, task.id());
        // P9-06：触发真实导出（含 AI 辅助生成声明）
        exportTaskRunner.run(task.id(), draftId, userId, format);
        return Map.of("taskId", task.id().toString(), "draftId", draftId.toString());
    }

    // ── 私有工具方法 ─────────────────────────────────────────────────────────

    /**
     * 加载草稿并验证用户权限。
     *
     * @param draftId 草稿 ID
     * @param userId  用户 ID
     * @return 草稿实体
     */
    private ScriptDraft loadDraftAndVerify(UUID draftId, UUID userId) {
        ScriptDraft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "剧本草稿不存在"));
        if (!userId.equals(draft.userId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "无权访问该草稿");
        }
        return draft;
    }

    /**
     * 场景分页结果封装。
     *
     * @param total  总场景数
     * @param page   当前页码
     * @param size   每页条数
     * @param scenes 当前页场景列表
     */
    public record ScenePageResult(int total, int page, int size, List<ScriptSceneInfo> scenes) {}
}
