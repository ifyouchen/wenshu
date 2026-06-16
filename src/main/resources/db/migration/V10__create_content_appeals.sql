-- V10: 内容安全申诉表（P9-05）
-- content_appeals: 用户对 AI 输出被内容安全过滤后的误报申诉记录

CREATE TABLE content_appeals (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content     TEXT        NOT NULL,
    reason      TEXT        NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'pending',
    reviewer_note TEXT,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_content_appeals_user ON content_appeals(user_id);
CREATE INDEX idx_content_appeals_status ON content_appeals(status);
