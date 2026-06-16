package com.czx.wenshu.application.imports;

import com.czx.wenshu.application.project.CharacterAnchorService;
import com.czx.wenshu.application.project.ChapterInfo;
import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.imports.ImportParseSession;
import com.czx.wenshu.domain.imports.ImportParseSessionRepository;
import com.czx.wenshu.domain.imports.ParsedChapterItem;
import com.czx.wenshu.domain.project.Chapter;
import com.czx.wenshu.domain.project.ChapterRepository;
import com.czx.wenshu.domain.project.ChapterStatus;
import com.czx.wenshu.domain.project.ProjectRepository;
import com.czx.wenshu.domain.project.Volume;
import com.czx.wenshu.domain.project.VolumeRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文件导入应用服务（P4-01 / P4-02 / P4-03 / P2-4 修复）。
 */
@Service
public class ImportApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ImportApplicationService.class);

    /**
     * 章节标题检测正则：支持常见中文网文章节格式和英文 Chapter 格式。
     * 匹配行首的章节标题模式，如"第一章 初入江湖"、"Chapter 1 Introduction"等。
     */
    private static final Pattern CHAPTER_HEADING = Pattern.compile(
            "^[ \\t]*(第[零一二三四五六七八九十百千万亿\\d]+[章节回卷集部篇][^\\n]*" +
            "|Chapter\\s+\\d+[^\\n]*" +
            "|CHAPTER\\s+\\d+[^\\n]*)",
            Pattern.MULTILINE
    );

    private static final int CONTENT_PREVIEW_LENGTH = 200;

    private final ImportParseSessionRepository sessionRepository;
    private final ProjectRepository projectRepository;
    private final VolumeRepository volumeRepository;
    private final ChapterRepository chapterRepository;
    private final CharacterAnchorService characterAnchorService;
    private final Clock clock;

    /**
     * 构造导入服务。
     *
     * @param sessionRepository    导入解析会话仓储
     * @param projectRepository    作品仓储
     * @param volumeRepository     卷仓储
     * @param chapterRepository    章节仓储
     * @param characterAnchorService 角色锚点服务（导入后自动提取）
     * @param clock                时钟
     */
    public ImportApplicationService(ImportParseSessionRepository sessionRepository,
                                     ProjectRepository projectRepository,
                                     VolumeRepository volumeRepository,
                                     ChapterRepository chapterRepository,
                                     CharacterAnchorService characterAnchorService,
                                     Clock clock) {
        this.sessionRepository = sessionRepository;
        this.projectRepository = projectRepository;
        this.volumeRepository = volumeRepository;
        this.chapterRepository = chapterRepository;
        this.characterAnchorService = characterAnchorService;
        this.clock = clock;
    }

    // ── P4-01: 文件解析预览 ─────────────────────────────────────────────────

    @Transactional
    public ImportPreviewInfo parseFile(UUID userId, UUID projectId,
                                        InputStream content, String filename) {
        verifyProjectOwnership(projectId, userId);
        String text = extractText(content, filename);
        List<ParsedChapterItem> chapters = splitIntoChapters(text);
        ImportParseSession session = ImportParseSession.create(projectId, userId, chapters, clock);
        sessionRepository.save(session);
        return toPreviewInfo(session);
    }

    // ── P4-02: 切分点调整 ───────────────────────────────────────────────────

    @Transactional
    public ImportPreviewInfo adjustSplitPoints(UUID parseId, UUID userId,
                                                List<AdjustChapterItem> adjustItems) {
        ImportParseSession session = loadSession(parseId, userId);
        List<ParsedChapterItem> updated = new ArrayList<>();
        for (int i = 0; i < adjustItems.size(); i++) {
            AdjustChapterItem item = adjustItems.get(i);
            updated.add(new ParsedChapterItem(i, item.title(), item.content()));
        }
        session.updateChapters(updated);
        sessionRepository.save(session);
        return toPreviewInfo(session);
    }

    // ── P4-02: 确认导入入库 ─────────────────────────────────────────────────

    @Transactional
    public List<ChapterInfo> applyImport(UUID parseId, UUID userId, UUID volumeId) {
        ImportParseSession session = loadSession(parseId, userId);
        Volume volume = volumeRepository.findById(volumeId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "卷不存在"));
        verifyProjectOwnership(volume.projectId(), userId);

        int startSortOrder = chapterRepository.findByVolumeId(volumeId).size();
        List<ChapterInfo> created = new ArrayList<>();
        for (ParsedChapterItem item : session.chapters()) {
            Chapter chapter = Chapter.create(volumeId, volume.projectId(),
                    item.title(), null, startSortOrder + item.index(), clock);
            chapter.saveContent(chapter.title(), item.content(), null,
                    ChapterStatus.DRAFT, clock);
            chapterRepository.save(chapter);
            created.add(ChapterInfo.from(chapter));
        }
        sessionRepository.deleteById(parseId);
        // P2-4：导入完成后，对前 3 章自动提取角色锚点（同步执行）
        applyImportAnchorExtraction(volume.projectId(), created);
        return created;
    }

    /**
     * 导入完成后对前 3 章同步提取角色锚点（P2-4 修复）。
     *
     * @param projectId      作品 ID
     * @param importedChapters 已导入章节列表
     */
    private void applyImportAnchorExtraction(UUID projectId, List<ChapterInfo> importedChapters) {
        int limit = Math.min(3, importedChapters.size());
        for (int i = 0; i < limit; i++) {
            ChapterInfo chapterInfo = importedChapters.get(i);
            try {
                Chapter chapter = chapterRepository.findById(UUID.fromString(chapterInfo.id())).orElse(null);
                if (chapter != null) {
                    characterAnchorService.updateAnchors(projectId, chapter.id(), chapter.content());
                    log.debug("[ImportApplicationService] 角色锚点提取 chapterId={}", chapter.id());
                }
            } catch (Exception e) {
                log.warn("[ImportApplicationService] 角色锚点提取失败 chapterId={} error={}",
                        chapterInfo.id(), e.getMessage());
            }
        }
        log.info("[ImportApplicationService] 导入后角色锚点提取完成 projectId={} 处理章节数={}", projectId, limit);
    }

    // ── P4-03: 粘贴文本导入（无预览步骤，直接入库）─────────────────────────

    @Transactional
    public List<ChapterInfo> pasteImport(UUID userId, UUID projectId, UUID volumeId, String text) {
        verifyProjectOwnership(projectId, userId);
        Volume volume = volumeRepository.findById(volumeId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "卷不存在"));
        verifyProjectOwnership(volume.projectId(), userId);

        List<ParsedChapterItem> chapters = splitIntoChapters(text);
        int startSortOrder = chapterRepository.findByVolumeId(volumeId).size();
        List<ChapterInfo> created = new ArrayList<>();
        for (ParsedChapterItem item : chapters) {
            Chapter chapter = Chapter.create(volumeId, volume.projectId(),
                    item.title(), null, startSortOrder + item.index(), clock);
            chapter.saveContent(chapter.title(), item.content(), null,
                    ChapterStatus.DRAFT, clock);
            chapterRepository.save(chapter);
            created.add(ChapterInfo.from(chapter));
        }
        return created;
    }

    // ── 私有工具方法 ────────────────────────────────────────────────────────

    private ImportParseSession loadSession(UUID parseId, UUID userId) {
        ImportParseSession session = sessionRepository.findById(parseId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "解析会话不存在或已过期"));
        if (!session.userId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "无权访问该解析会话");
        }
        if (session.isExpiredAt(Instant.now(clock))) {
            throw new ApiException(ErrorCode.NOT_FOUND, "解析会话已过期，请重新上传文件");
        }
        return session;
    }

    private void verifyProjectOwnership(UUID projectId, UUID userId) {
        if (!projectRepository.existsByIdAndUserId(projectId, userId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "作品不存在");
        }
    }

    /** 根据文件名判断格式，提取纯文本。 */
    private String extractText(InputStream stream, String filename) {
        String lower = filename == null ? "" : filename.toLowerCase();
        try {
            if (lower.endsWith(".docx")) {
                return extractDocxText(stream);
            } else {
                // TXT and fallback: auto-detect encoding (try UTF-8 then GBK)
                byte[] bytes = stream.readAllBytes();
                return tryDecodeText(bytes);
            }
        } catch (IOException e) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "文件读取失败：" + e.getMessage());
        }
    }

    private String extractDocxText(InputStream stream) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(stream)) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph para : doc.getParagraphs()) {
                sb.append(para.getText()).append("\n");
            }
            return sb.toString();
        }
    }

    private String tryDecodeText(byte[] bytes) {
        // Try UTF-8 first, fallback to GBK (common for Chinese TXT files)
        String utf8 = new String(bytes, StandardCharsets.UTF_8);
        if (!utf8.contains("�")) {
            return utf8;
        }
        return new String(bytes, Charset.forName("GBK"));
    }

    /**
     * 将纯文本按章节标题切分。
     * 若检测不到任何章节标题，则将全文作为一个章节（标题为"第一章"）返回。
     */
    public List<ParsedChapterItem> splitIntoChapters(String text) {
        if (text == null || text.isBlank()) {
            return List.of(new ParsedChapterItem(0, "第一章", ""));
        }
        Matcher m = CHAPTER_HEADING.matcher(text);
        List<int[]> positions = new ArrayList<>();  // [start, headingEnd]
        List<String> titles = new ArrayList<>();

        while (m.find()) {
            positions.add(new int[]{m.start(), m.end()});
            titles.add(m.group().strip());
        }

        if (positions.isEmpty()) {
            // 无法检测到章节标题 → 整体作为一章
            return List.of(new ParsedChapterItem(0, "第一章", text.strip()));
        }

        List<ParsedChapterItem> result = new ArrayList<>();
        // 如果第一个章节标题前有内容，作为序章
        if (positions.get(0)[0] > 0) {
            String prelude = text.substring(0, positions.get(0)[0]).strip();
            if (!prelude.isBlank()) {
                result.add(new ParsedChapterItem(0, "序章", prelude));
            }
        }

        for (int i = 0; i < positions.size(); i++) {
            int contentStart = positions.get(i)[1];
            int contentEnd = (i + 1 < positions.size()) ? positions.get(i + 1)[0] : text.length();
            String content = text.substring(contentStart, contentEnd).strip();
            result.add(new ParsedChapterItem(result.size(), titles.get(i), content));
        }
        return result;
    }

    private ImportPreviewInfo toPreviewInfo(ImportParseSession session) {
        List<ChapterPreviewItem> previews = session.chapters().stream()
                .map(c -> new ChapterPreviewItem(
                        c.index(),
                        c.title(),
                        c.content().length() > CONTENT_PREVIEW_LENGTH
                                ? c.content().substring(0, CONTENT_PREVIEW_LENGTH) + "…"
                                : c.content(),
                        countWords(c.content())))
                .toList();
        return new ImportPreviewInfo(session.id().toString(),
                session.chapters().size(), previews, session.expiresAt().toString());
    }

    private int countWords(String content) {
        if (content == null || content.isBlank()) return 0;
        return (int) content.codePoints().filter(cp -> cp > 0x4E00).count()
                + content.split("\\s+").length;
    }
}
