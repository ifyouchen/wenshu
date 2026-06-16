package com.czx.wenshu.domain.user;

import java.util.List;
import java.util.UUID;

/**
 * 字数包仓储端口（P9-09）。
 * 由基础设施层 MyBatis 实现。
 */
public interface WordPackRepository {

    /** 保存字数包（新增或更新消耗量）。 */
    void save(WordPack pack);

    /** 查询用户所有有效字数包（未耗尽，按创建时间升序消耗）。 */
    List<WordPack> findActiveByUserId(UUID userId);

    /** 检查用户是否已有体验额度（避免重复发放）。 */
    boolean existsTrialByUserId(UUID userId);
}
