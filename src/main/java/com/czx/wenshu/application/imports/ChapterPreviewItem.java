package com.czx.wenshu.application.imports;

/** 导入预览中的单个章节摘要（不含全文，节省响应体积）。 */
public record ChapterPreviewItem(int index, String title, String contentPreview, int wordCount) {
}
