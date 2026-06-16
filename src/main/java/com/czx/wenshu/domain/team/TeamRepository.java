package com.czx.wenshu.domain.team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 团队仓储端口（P9-07）。
 * 由基础设施层 MyBatis 实现。
 */
public interface TeamRepository {

    /** 保存或更新团队。 */
    void save(Team team);

    /** 按主键查询。 */
    Optional<Team> findById(UUID id);

    /** 查询用户所在的所有团队（包括作为成员的团队）。 */
    List<Team> findByMemberUserId(UUID userId);
}
