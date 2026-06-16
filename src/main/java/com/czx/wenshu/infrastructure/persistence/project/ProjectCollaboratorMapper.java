package com.czx.wenshu.infrastructure.persistence.project;

import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** MyBatis Mapper，对应 project_collaborators 表（P9-08）。 */
@Mapper
public interface ProjectCollaboratorMapper {

    /** 查询指定作品+用户的协作记录。 */
    @Select("SELECT id, project_id, user_id, role, added_by, created_at FROM project_collaborators WHERE project_id = CAST(#{projectId} AS UUID) AND user_id = CAST(#{userId} AS UUID)")
    CollaboratorRecord findByProjectIdAndUserId(@Param("projectId") String projectId, @Param("userId") String userId);

    /** 查询作品所有协作者。 */
    @Select("SELECT id, project_id, user_id, role, added_by, created_at FROM project_collaborators WHERE project_id = CAST(#{projectId} AS UUID) ORDER BY created_at")
    List<CollaboratorRecord> findByProjectId(@Param("projectId") String projectId);

    /** 插入协作者记录。 */
    @Insert("""
            INSERT INTO project_collaborators (id, project_id, user_id, role, added_by, created_at)
            VALUES (CAST(#{id} AS UUID), CAST(#{projectId} AS UUID), CAST(#{userId} AS UUID),
                    #{role},
                    CASE WHEN #{addedBy} IS NOT NULL THEN CAST(#{addedBy} AS UUID) ELSE NULL END,
                    #{createdAt})
            """)
    void insert(CollaboratorRecord record);

    /** 删除协作者。 */
    @Delete("DELETE FROM project_collaborators WHERE project_id = CAST(#{projectId} AS UUID) AND user_id = CAST(#{userId} AS UUID)")
    void deleteByProjectIdAndUserId(@Param("projectId") String projectId, @Param("userId") String userId);

    // ── 内嵌 Record 类 ─────────────────────────────────────────────────────

    /**
     * MyBatis 持久化记录，对应 project_collaborators 表（P9-08）。
     */
    class CollaboratorRecord {
        private String id;
        private String projectId;
        private String userId;
        private String role;
        private String addedBy;
        private Instant createdAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getAddedBy() { return addedBy; }
        public void setAddedBy(String addedBy) { this.addedBy = addedBy; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    }
}
