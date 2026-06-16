package com.czx.wenshu.domain.project;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * 项目协作者领域对象（P9-08）。
 *
 * <p>记录非所有者用户对某个作品的访问权限。
 * 协作者角色：</p>
 * <ul>
 *   <li>{@code editor}：可读写章节、角色库、词典</li>
 *   <li>{@code viewer}：只读访问</li>
 * </ul>
 */
public class ProjectCollaborator {

    /** 协作记录主键。 */
    private final UUID id;

    /** 作品 ID。 */
    private final UUID projectId;

    /** 协作者用户 ID。 */
    private final UUID userId;

    /** 协作角色：editor / viewer。 */
    private String role;

    /** 添加者 ID（项目所有者）。 */
    private final UUID addedBy;

    /** 创建时间。 */
    private final Instant createdAt;

    /** 私有全参数构造器。 */
    private ProjectCollaborator(UUID id, UUID projectId, UUID userId,
                                String role, UUID addedBy, Instant createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.userId = userId;
        this.role = role;
        this.addedBy = addedBy;
        this.createdAt = createdAt;
    }

    /**
     * 添加协作者（owner 调用）。
     *
     * @param projectId 作品 ID
     * @param userId    协作者用户 ID
     * @param role      协作角色（editor/viewer）
     * @param addedBy   添加者 ID
     * @param clock     时钟
     * @return 新协作记录
     */
    public static ProjectCollaborator add(UUID projectId, UUID userId, String role,
                                          UUID addedBy, Clock clock) {
        return new ProjectCollaborator(UUID.randomUUID(), projectId, userId,
                role, addedBy, clock.instant());
    }

    /**
     * 从持久层还原对象。
     */
    public static ProjectCollaborator rehydrate(UUID id, UUID projectId, UUID userId,
                                                String role, UUID addedBy, Instant createdAt) {
        return new ProjectCollaborator(id, projectId, userId, role, addedBy, createdAt);
    }

    /** 修改角色。 */
    public void changeRole(String newRole) { this.role = newRole; }

    // ── Getters ─────────────────────────────────────────────────────────────

    /** @return 记录主键 */
    public UUID id() { return id; }
    /** @return 作品 ID */
    public UUID projectId() { return projectId; }
    /** @return 协作者用户 ID */
    public UUID userId() { return userId; }
    /** @return 协作角色 */
    public String role() { return role; }
    /** @return 添加者 ID */
    public UUID addedBy() { return addedBy; }
    /** @return 创建时间 */
    public Instant createdAt() { return createdAt; }
}
