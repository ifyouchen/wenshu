package com.czx.wenshu.application.team;

import com.czx.wenshu.domain.team.TeamMember;
import java.time.Instant;
import java.util.UUID;

/**
 * 团队成员摘要 DTO（P9-07）。
 */
public record MemberInfo(
        /** 成员记录主键（team_members.id）。 */
        UUID id,
        /** 成员用户 ID。 */
        UUID userId,
        /** 成员角色（admin/member）。 */
        String role,
        /** 成员状态（pending/active/removed）。 */
        String status,
        /** 邀请码（pending 状态时有值，发给受邀用户）。 */
        String inviteCode,
        /** 加入时间（active 后设置）。 */
        Instant joinedAt,
        /** 创建（邀请）时间。 */
        Instant createdAt) {

    /** 从领域对象创建 DTO。 */
    public static MemberInfo from(TeamMember member) {
        return new MemberInfo(
                member.id(), member.userId(), member.role(), member.status(),
                member.inviteCode(), member.joinedAt(), member.createdAt());
    }
}
