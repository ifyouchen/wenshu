package com.czx.wenshu.domain.team;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * 团队成员领域对象（P9-07）。
 *
 * <p>记录用户与团队的归属关系，包含角色和邀请状态。</p>
 *
 * <p>状态流转：pending（邀请中）→ active（接受邀请）→ removed（被移除）</p>
 *
 * <p>角色：admin（管理员，可邀请/移除成员）/ member（普通成员）</p>
 */
public class TeamMember {

    /** 记录主键。 */
    private final UUID id;

    /** 所属团队 ID。 */
    private final UUID teamId;

    /** 成员用户 ID。 */
    private final UUID userId;

    /** 成员角色：admin / member。 */
    private String role;

    /** 成员状态：pending / active / removed。 */
    private String status;

    /** 邀请人 ID（可为 null）。 */
    private final UUID invitedBy;

    /** 邀请码（pending 时有效，接受后无效）。 */
    private final String inviteCode;

    /** 加入时间（accept 后设置）。 */
    private Instant joinedAt;

    /** 创建时间。 */
    private final Instant createdAt;

    /** 私有全参数构造器。 */
    private TeamMember(UUID id, UUID teamId, UUID userId, String role, String status,
                       UUID invitedBy, String inviteCode, Instant joinedAt, Instant createdAt) {
        this.id = id;
        this.teamId = teamId;
        this.userId = userId;
        this.role = role;
        this.status = status;
        this.invitedBy = invitedBy;
        this.inviteCode = inviteCode;
        this.joinedAt = joinedAt;
        this.createdAt = createdAt;
    }

    /**
     * 创建新邀请（状态 pending，含邀请码）。
     *
     * @param teamId     团队 ID
     * @param userId     受邀用户 ID
     * @param invitedBy  邀请人 ID
     * @param inviteCode 随机邀请码
     * @param clock      时钟
     * @return 新成员记录（pending 状态）
     */
    public static TeamMember invite(UUID teamId, UUID userId, UUID invitedBy,
                                    String inviteCode, Clock clock) {
        return new TeamMember(UUID.randomUUID(), teamId, userId, "member", "pending",
                invitedBy, inviteCode, null, clock.instant());
    }

    /**
     * 创建团队所有者成员记录（active admin 状态）。
     *
     * @param teamId 团队 ID
     * @param userId 所有者 ID
     * @param clock  时钟
     * @return 管理员成员记录
     */
    public static TeamMember createOwner(UUID teamId, UUID userId, Clock clock) {
        Instant now = clock.instant();
        return new TeamMember(UUID.randomUUID(), teamId, userId, "admin", "active",
                null, null, now, now);
    }

    /**
     * 从持久层还原成员对象。
     */
    public static TeamMember rehydrate(UUID id, UUID teamId, UUID userId, String role,
                                       String status, UUID invitedBy, String inviteCode,
                                       Instant joinedAt, Instant createdAt) {
        return new TeamMember(id, teamId, userId, role, status,
                invitedBy, inviteCode, joinedAt, createdAt);
    }

    /**
     * 接受邀请（pending → active）。
     *
     * @param clock 时钟
     */
    public void accept(Clock clock) {
        this.status = "active";
        this.joinedAt = clock.instant();
    }

    /**
     * 移除成员（→ removed）。
     */
    public void remove() {
        this.status = "removed";
    }

    /**
     * 修改角色（admin ↔ member）。
     *
     * @param newRole 新角色（admin/member）
     */
    public void changeRole(String newRole) {
        this.role = newRole;
    }

    // ── Getters ─────────────────────────────────────────────────────────────

    /** @return 记录主键 */
    public UUID id() { return id; }

    /** @return 所属团队 ID */
    public UUID teamId() { return teamId; }

    /** @return 成员用户 ID */
    public UUID userId() { return userId; }

    /** @return 角色 */
    public String role() { return role; }

    /** @return 状态 */
    public String status() { return status; }

    /** @return 邀请人 ID */
    public UUID invitedBy() { return invitedBy; }

    /** @return 邀请码 */
    public String inviteCode() { return inviteCode; }

    /** @return 加入时间 */
    public Instant joinedAt() { return joinedAt; }

    /** @return 创建时间 */
    public Instant createdAt() { return createdAt; }
}
