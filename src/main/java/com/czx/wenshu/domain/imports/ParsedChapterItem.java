package com.czx.wenshu.domain.imports;

/** 解析出的单个章节草稿，供导入预览和入库使用。 */
public record ParsedChapterItem(int index, String title, String content) {
}
