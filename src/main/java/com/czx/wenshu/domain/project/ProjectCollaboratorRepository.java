package com.czx.wenshu.domain.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 项目协作者仓储端口（P9-08）。
 */
public interface ProjectCollaboratorRepository {

    /** 保存协作者记录。 */
    void save(ProjectCollaborator collaborator);

    /** 删除协作者。 */
    void deleteByProjectIdAndUserId(UUID projectId, UUID userId);

    /** 按作品和用户查询（鉴权用）。 */
    Optional<ProjectCollaborator> findByProjectIdAndUserId(UUID projectId, UUID userId);

    /** 查询作品所有协作者。 */
    List<ProjectCollaborator> findByProjectId(UUID projectId);

    /**
     * 检查用户是否有访问该作品的权限（所有者 OR 协作者）。
     *
     * @param projectId 作品 ID
     * @param userId    用户 ID
     * @param ownerId   作品所有者 ID（通过 project.userId() 传入）
     * @return true 表示有权限访问
     */
    default boolean hasAccess(UUID projectId, UUID userId, UUID ownerId) {
        if (ownerId.equals(userId)) return true;
        return findByProjectIdAndUserId(projectId, userId).isPresent();
    }
}
