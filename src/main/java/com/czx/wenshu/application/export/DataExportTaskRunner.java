package com.czx.wenshu.application.export;

import com.czx.wenshu.application.storage.FileStorageService;
import com.czx.wenshu.application.task.AsyncTaskService;
import com.czx.wenshu.domain.project.Chapter;
import com.czx.wenshu.domain.project.ChapterRepository;
import com.czx.wenshu.domain.project.CharacterRepository;
import com.czx.wenshu.domain.project.Project;
import com.czx.wenshu.domain.project.ProjectRepository;
import com.czx.wenshu.domain.project.Volume;
import com.czx.wenshu.domain.project.VolumeRepository;
import com.czx.wenshu.domain.project.WorldElementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 用户数据导出异步执行器（P9-04）。
 *
 * <p>将用户全部作品（含卷、章节、角色库、世界观词典）打包为 ZIP 文件，
 * 上传至腾讯云 COS，任务完成后返回带时效的预签名下载 URL。</p>
 *
 * <p>ZIP 结构：</p>
 * <pre>
 * {userId}_export.zip
 *   ├── profile.json        用户基本信息占位
 *   └── projects/
 *       └── {projectTitle}.json  每部作品含卷章、角色库、词典
 * </pre>
 *
 * <p>COS 对象键：{@code exports/{userId}/data_{timestamp}.zip}</p>
 */
@Component
public class DataExportTaskRunner {

    private static final Logger log = LoggerFactory.getLogger(DataExportTaskRunner.class);

    private final AsyncTaskService asyncTaskService;
    private final ProjectRepository projectRepository;
    private final VolumeRepository volumeRepository;
    private final ChapterRepository chapterRepository;
    private final CharacterRepository characterRepository;
    private final WorldElementRepository worldElementRepository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    /** 构造函数注入所有依赖。 */
    public DataExportTaskRunner(AsyncTaskService asyncTaskService,
                                ProjectRepository projectRepository,
                                VolumeRepository volumeRepository,
                                ChapterRepository chapterRepository,
                                CharacterRepository characterRepository,
                                WorldElementRepository worldElementRepository,
                                FileStorageService fileStorageService,
                                ObjectMapper objectMapper) {
        this.asyncTaskService = asyncTaskService;
        this.projectRepository = projectRepository;
        this.volumeRepository = volumeRepository;
        this.chapterRepository = chapterRepository;
        this.characterRepository = characterRepository;
        this.worldElementRepository = worldElementRepository;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
    }

    /**
     * 异步执行数据导出任务。
     *
     * @param taskId 异步任务 ID（通过 GET /tasks/{taskId}/progress 轮询进度）
     * @param userId 目标用户 ID
     */
    @Async("aiTaskExecutor")
    public void run(UUID taskId, UUID userId) {
        log.info("[DataExportTaskRunner] 开始数据导出 taskId={} userId={}", taskId, userId);
        try {
            asyncTaskService.markRunning(taskId, 4, "初始化");

            // ── 步骤 1：收集所有项目数据 ──────────────────────────────────
            asyncTaskService.updateProgress(taskId, 1, "收集作品数据", 10);
            List<Project> projects = projectRepository.findByUserId(userId);
            log.info("[DataExportTaskRunner] 用户 {} 共 {} 部作品待导出", userId, projects.size());

            // ── 步骤 2：构建 ZIP 内容 ─────────────────────────────────────
            asyncTaskService.updateProgress(taskId, 2, "打包数据", 30);
            byte[] zipBytes = buildZip(userId, projects);

            // ── 步骤 3：上传到 COS ────────────────────────────────────────
            asyncTaskService.updateProgress(taskId, 3, "上传到云存储", 70);
            String objectKey = buildObjectKey(userId);

            if (!fileStorageService.isAvailable()) {
                log.warn("[DataExportTaskRunner] COS 未配置，导出文件无法上传 taskId={}", taskId);
                asyncTaskService.completeWithJson(taskId,
                        "{\"downloadUrl\":\"COS_NOT_CONFIGURED\",\"note\":\"请配置腾讯云 COS 凭据\"}");
                return;
            }

            fileStorageService.upload(objectKey,
                    new ByteArrayInputStream(zipBytes), "application/zip", zipBytes.length);

            // ── 步骤 4：生成预签名 URL ────────────────────────────────────
            asyncTaskService.updateProgress(taskId, 4, "生成下载链接", 90);
            String downloadUrl = fileStorageService.generatePresignedUrl(
                    objectKey, Duration.ofMinutes(60));

            String resultJson = objectMapper.writeValueAsString(
                    Map.of("downloadUrl", downloadUrl, "objectKey", objectKey));
            asyncTaskService.completeWithJson(taskId, resultJson);
            log.info("[DataExportTaskRunner] 数据导出完成 taskId={} objectKey={}", taskId, objectKey);

        } catch (Exception e) {
            log.error("[DataExportTaskRunner] 数据导出失败 taskId={} error={}", taskId, e.getMessage(), e);
            asyncTaskService.fail(taskId, "导出失败：" + e.getMessage());
        }
    }

    // ── 私有工具方法 ─────────────────────────────────────────────────────────

    /**
     * 构建包含用户全部数据的 ZIP 字节数组。
     *
     * @param userId   用户 ID（用于生成 profile.json 占位）
     * @param projects 用户所有作品列表
     * @return ZIP 文件字节数组
     */
    private byte[] buildZip(UUID userId, List<Project> projects) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {

            // profile.json（用户信息占位）
            String profileJson = objectMapper.writeValueAsString(
                    Map.of("userId", userId.toString(), "exportedAt", Instant.now().toString()));
            addZipEntry(zos, "profile.json", profileJson);

            // 每部作品独立 JSON 文件
            for (Project project : projects) {
                Map<String, Object> projectData = buildProjectData(project);
                String projectJson = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(projectData);
                String fileName = sanitizeFileName(project.title()) + ".json";
                addZipEntry(zos, "projects/" + fileName, projectJson);
            }
        }
        return baos.toByteArray();
    }

    /**
     * 收集单部作品的全量数据（卷、章节、角色库、世界观词典）。
     *
     * @param project 作品领域对象
     * @return 可序列化的 Map 结构
     */
    private Map<String, Object> buildProjectData(Project project) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", project.id().toString());
        data.put("title", project.title());
        data.put("genre", project.genre());
        data.put("synopsis", project.synopsis());
        data.put("status", project.status() != null ? project.status().name() : null);
        data.put("totalWords", project.totalWords());

        // 卷与章节（树形结构）
        List<Map<String, Object>> volumeList = new ArrayList<>();
        List<Volume> volumes = volumeRepository.findByProjectId(project.id());
        for (Volume volume : volumes) {
            Map<String, Object> volumeData = new HashMap<>();
            volumeData.put("id", volume.id().toString());
            volumeData.put("title", volume.title());
            volumeData.put("sortOrder", volume.sortOrder());

            List<Map<String, Object>> chapterList = new ArrayList<>();
            List<Chapter> chapters = chapterRepository.findByVolumeId(volume.id());
            for (Chapter chapter : chapters) {
                Map<String, Object> chapterData = new HashMap<>();
                chapterData.put("id", chapter.id().toString());
                chapterData.put("title", chapter.title());
                chapterData.put("content", chapter.content());
                chapterData.put("wordCount", chapter.wordCount());
                chapterData.put("status", chapter.status() != null ? chapter.status().name() : null);
                chapterList.add(chapterData);
            }
            volumeData.put("chapters", chapterList);
            volumeList.add(volumeData);
        }
        data.put("volumes", volumeList);

        // 角色库
        List<Map<String, Object>> characterList = characterRepository.findByProjectId(project.id())
                .stream().map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", c.id().toString());
                    m.put("name", c.name());
                    m.put("role", c.role());
                    m.put("appearance", c.appearance());
                    m.put("personality", c.personality());
                    m.put("isLocked", c.locked());
                    return m;
                }).toList();
        data.put("characters", characterList);

        // 世界观词典
        List<Map<String, Object>> worldList = worldElementRepository.findByProjectId(project.id())
                .stream().map(w -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", w.id().toString());
                    m.put("type", w.type());
                    m.put("name", w.name());
                    m.put("description", w.description());
                    return m;
                }).toList();
        data.put("worldElements", worldList);

        return data;
    }

    /**
     * 向 ZIP 写入一个文本条目。
     *
     * @param zos      ZipOutputStream
     * @param name     条目路径名
     * @param content  UTF-8 文本内容
     */
    private void addZipEntry(ZipOutputStream zos, String name, String content) throws Exception {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    /**
     * 构建 COS 对象键：{@code exports/{userId}/data_{timestamp}.zip}。
     *
     * @param userId 用户 ID
     * @return 对象键字符串
     */
    private String buildObjectKey(UUID userId) {
        String ts = String.valueOf(Instant.now().getEpochSecond());
        return "exports/" + userId + "/data_" + ts + ".zip";
    }

    /**
     * 对文件名进行安全化处理（移除不合法字符）。
     *
     * @param name 原始文件名
     * @return 安全的文件名
     */
    private String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) return "untitled";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
