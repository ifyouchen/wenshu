-- V14: 项目协作者表（P9-08）
-- project_collaborators: 记录哪些用户（非所有者）可以访问某个作品

CREATE TABLE project_collaborators (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID        NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        VARCHAR(20) NOT NULL DEFAULT 'editor',
    -- 角色：editor（可读写章节）| viewer（只读）
    added_by    UUID        REFERENCES users(id),
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (project_id, user_id)
);

CREATE INDEX idx_proj_collabs_project ON project_collaborators(project_id);
CREATE INDEX idx_proj_collabs_user ON project_collaborators(user_id);
