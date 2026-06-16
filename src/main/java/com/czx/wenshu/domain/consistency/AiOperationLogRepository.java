package com.czx.wenshu.domain.consistency;

import java.util.Optional;
import java.util.UUID;

/**
 * AI 操作日志仓储端口（P6-06）。
 */
public interface AiOperationLogRepository {

    /**
     * 保存新日志条目。
     *
     * @param log 操作日志实体
     */
    void save(AiOperationLog log);

    /**
     * 按 ID 查找日志/报告。
     *
     * @param id 日志 ID
     * @return Optional 包装的日志对象
     */
    Optional<AiOperationLog> findById(UUID id);
}
