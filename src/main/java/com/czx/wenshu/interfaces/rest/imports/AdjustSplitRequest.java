package com.czx.wenshu.interfaces.rest.imports;

import java.util.List;

public record AdjustSplitRequest(List<ChapterItem> chapters) {

    public record ChapterItem(String title, String content) {}
}
