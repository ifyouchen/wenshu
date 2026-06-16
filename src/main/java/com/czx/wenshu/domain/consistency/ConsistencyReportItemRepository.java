package com.czx.wenshu.domain.consistency;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 一致性审查条目仓储端口（P6-06/P6-07）。
 */
public interface ConsistencyReportItemRepository {

    /**
     * 保存审查条目。
     *
     * @param item 审查条目实体
     */
    void save(ConsistencyReportItem item);

    /**
     * 按报告 ID 查询所有条目，按创建时间正序。
     *
     * @param reportId 报告 ID
     * @return 审查条目列表
     */
    List<ConsistencyReportItem> findByReportId(UUID reportId);

    /**
     * 按 ID 查询单条条目。
     *
     * @param id 条目 ID
     * @return Optional 包装
     */
    Optional<ConsistencyReportItem> findById(UUID id);
}
