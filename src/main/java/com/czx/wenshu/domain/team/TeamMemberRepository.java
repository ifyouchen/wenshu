package com.czx.wenshu.domain.team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 团队成员仓储端口（P9-07）。
 * 由基础设施层 MyBatis 实现。
 */
public interface TeamMemberRepository {

    /** 保存成员记录（新增或更新状态/角色）。 */
    void save(TeamMember member);

    /** 按邀请码查询（接受邀请时使用）。 */
    Optional<TeamMember> findByInviteCode(String inviteCode);

    /** 按团队 ID 和用户 ID 查询（鉴权用）。 */
    Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId);

    /** 查询团队所有有效成员（active 状态）。 */
    List<TeamMember> findActiveByTeamId(UUID teamId);

    /** 查询团队所有成员（含 pending/removed，用于列表展示）。 */
    List<TeamMember> findAllByTeamId(UUID teamId);
}
