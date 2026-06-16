package com.czx.wenshu.application.imports;

import java.util.List;

/** 文件解析预览结果，供用户确认章节切分是否正确。 */
public record ImportPreviewInfo(String parseId, int totalChapters,
                                 List<ChapterPreviewItem> chapters, String expiresAt) {
}
