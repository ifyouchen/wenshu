package com.czx.wenshu.interfaces.rest.imports;

import com.czx.wenshu.application.imports.AdjustChapterItem;
import com.czx.wenshu.application.imports.ImportApplicationService;
import com.czx.wenshu.application.imports.ImportPreviewInfo;
import com.czx.wenshu.application.project.ChapterInfo;
import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.common.result.Result;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.interfaces.rest.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Import", description = "内容导入（文件/粘贴）")
@Validated
@RestController
@RequestMapping("/api/v1/import")
public class ImportController {

    private static final Logger log = LoggerFactory.getLogger(ImportController.class);

    private final ImportApplicationService importService;
    private final CurrentUserProvider currentUserProvider;

    public ImportController(ImportApplicationService importService,
                             CurrentUserProvider currentUserProvider) {
        this.importService = importService;
        this.currentUserProvider = currentUserProvider;
    }

    @Operation(summary = "文件解析预览", description = "上传 TXT 或 DOCX 文件，解析章节切分并返回预览，有效期 24 小时。")
    @PostMapping(value = "/parse", consumes = "multipart/form-data")
    public Result<ImportPreviewInfo> parseFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam("projectId") UUID projectId) {
        User user = currentUserProvider.getCurrentUser();
        log.info("[ImportController] 文件解析 userId={} projectId={} 文件名={}", user.id(), projectId, file.getOriginalFilename());
        try {
            ImportPreviewInfo preview = importService.parseFile(
                    user.id(), projectId,
                    file.getInputStream(),
                    file.getOriginalFilename()
            );
            return Result.ok(preview);
        } catch (IOException e) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "文件读取失败：" + e.getMessage());
        }
    }

    @Operation(summary = "调整切分点", description = "在确认导入前调整章节标题和切分边界。")
    @PutMapping("/{parseId}/adjust")
    public Result<ImportPreviewInfo> adjustSplitPoints(
            @PathVariable UUID parseId,
            @RequestBody AdjustSplitRequest request) {
        User user = currentUserProvider.getCurrentUser();
        log.info("[ImportController] 调整切分点 userId={} parseId={}", user.id(), parseId);
        List<AdjustChapterItem> items = request.chapters() == null
                ? List.of()
                : request.chapters().stream()
                        .map(c -> new AdjustChapterItem(c.title(), c.content()))
                        .toList();
        return Result.ok(importService.adjustSplitPoints(parseId, user.id(), items));
    }

    @Operation(summary = "确认导入", description = "将解析结果写入指定卷，生成章节数据。")
    @PostMapping("/{parseId}/apply")
    public Result<List<ChapterInfo>> applyImport(
            @PathVariable UUID parseId,
            @Valid @RequestBody ApplyImportRequest request) {
        User user = currentUserProvider.getCurrentUser();
        log.info("[ImportController] 确认导入 userId={} parseId={} volumeId={}", user.id(), parseId, request.volumeId());
        return Result.ok(importService.applyImport(parseId, user.id(), request.volumeId()));
    }

    @Operation(summary = "粘贴文本导入", description = "直接粘贴文本内容，按章节标题切分后写入指定卷。")
    @PostMapping("/paste")
    public Result<List<ChapterInfo>> pasteImport(@Valid @RequestBody PasteImportRequest request) {
        User user = currentUserProvider.getCurrentUser();
        log.info("[ImportController] 粘贴文本导入 userId={} projectId={} volumeId={}", user.id(), request.projectId(), request.volumeId());
        return Result.ok(importService.pasteImport(
                user.id(), request.projectId(), request.volumeId(), request.text()));
    }
}
