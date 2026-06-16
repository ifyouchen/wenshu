package com.czx.wenshu.application.search;

import java.util.List;

/** 全书替换结果：总替换次数、受影响章节列表及各自的快照 ID。 */
public record ReplaceResultInfo(int totalReplaced, List<AffectedChapterResult> affectedChapters,
                                 boolean characterNameSynced) {
}
