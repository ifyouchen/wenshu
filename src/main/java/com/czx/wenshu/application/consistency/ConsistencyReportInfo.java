package com.czx.wenshu.application.consistency;

import java.util.List;

/**
 * 一致性审查报告响应 DTO（P6-06）。
 * 包含报告元信息和所有审查条目。
 */
public record ConsistencyReportInfo(
        String reportId,
        String projectId,
        int totalItems,
        int openItems,
        String createdAt,
        List<ConsistencyItemInfo> items) {
}
