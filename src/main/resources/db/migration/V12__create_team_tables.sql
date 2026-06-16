-- V12: 团队版表（P9-07）
-- teams: 团队信息
-- team_members: 团队成员关系（含角色与邀请状态）

CREATE TABLE teams (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id        UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    plan_key        VARCHAR(32) NOT NULL DEFAULT 'enterprise',
    -- 团队共享配额（月度字符额度和改编次数，叠加计算）
    monthly_char_limit         BIGINT NOT NULL DEFAULT 10000000,
    monthly_adaptation_limit   INT    NOT NULL DEFAULT 200,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE team_members (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id     UUID        NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        VARCHAR(20) NOT NULL DEFAULT 'member',
    -- 角色：admin（团队管理员）| member（普通成员）
    status      VARCHAR(20) NOT NULL DEFAULT 'pending',
    -- 状态：pending（邀请中）| active（已接受）| removed（已移除）
    invited_by  UUID REFERENCES users(id),
    invite_code VARCHAR(64) UNIQUE,
    joined_at   TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (team_id, user_id)
);

CREATE INDEX idx_team_members_team ON team_members(team_id);
CREATE INDEX idx_team_members_user ON team_members(user_id);
CREATE INDEX idx_team_members_invite ON team_members(invite_code);
