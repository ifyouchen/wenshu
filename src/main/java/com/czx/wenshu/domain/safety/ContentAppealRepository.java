package com.czx.wenshu.domain.safety;

import java.util.List;
import java.util.UUID;

/**
 * 内容安全申诉仓储端口（P9-05）。
 * 由基础设施层 MyBatis 实现。
 */
public interface ContentAppealRepository {

    /**
     * 保存申诉记录（新增）。
     *
     * @param appeal 申诉对象
     */
    void save(ContentAppeal appeal);

    /**
     * 查询用户的所有申诉记录（按创建时间倒序）。
     *
     * @param userId 用户 ID
     * @return 申诉列表
     */
    List<ContentAppeal> findByUserId(UUID userId);
}
