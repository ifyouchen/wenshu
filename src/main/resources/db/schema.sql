-- ============================================================================
-- 文枢 wenshu — 完整数据库 Schema
-- 整合自 Flyway V1~V15 迁移脚本，适用于全新部署（首次启动）
-- 生成时间：2026-06-16
-- 注意：使用 IF NOT EXISTS 保护，支持重复执行不报错
-- ============================================================================

-- ── 扩展 ──────────────────────────────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS vector;

-- ── 用户体系 ──────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email               VARCHAR(255) UNIQUE NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    nickname            VARCHAR(100),
    avatar_url          VARCHAR(500),
    identity_type       VARCHAR(20) DEFAULT 'new_author',
    plan                VARCHAR(20) DEFAULT 'free',
    plan_expires        TIMESTAMPTZ,
    is_email_verified   BOOLEAN DEFAULT FALSE,
    email_verify_token  VARCHAR(64),
    email_verify_exp    TIMESTAMPTZ,
    reset_token         VARCHAR(64),
    reset_token_exp     TIMESTAMPTZ,
    login_fail_count    SMALLINT DEFAULT 0,
    locked_until        TIMESTAMPTZ,
    last_login_ip       INET,
    last_login_at       TIMESTAMPTZ,
    daily_char_goal     INT DEFAULT 2000,
    ai_train_consent    BOOLEAN DEFAULT TRUE,
    is_deleted          BOOLEAN DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ DEFAULT now(),
    updated_at          TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash     VARCHAR(128) UNIQUE NOT NULL,
    expires_at     TIMESTAMPTZ NOT NULL,
    revoked_at     TIMESTAMPTZ,
    replaced_by_id UUID REFERENCES refresh_tokens(id),
    created_at     TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS access_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(128) UNIQUE NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS account_restore_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(128) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS email_verifications (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS registration_email_codes (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email      VARCHAR(255) NOT NULL,
    code_hash  VARCHAR(128) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_registration_email_codes_email_created
    ON registration_email_codes(email, created_at DESC);

CREATE TABLE IF NOT EXISTS password_resets (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- ── 作品、卷、章节 ────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS projects (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    title           VARCHAR(200) NOT NULL,
    genre           VARCHAR(50),
    synopsis        TEXT,
    worldview       TEXT,
    total_words     INT DEFAULT 0,
    daily_char_goal INT DEFAULT 0,
    status          VARCHAR(20) DEFAULT 'draft',
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS volumes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    title       VARCHAR(200),
    conflict    TEXT,
    sort_order  INT NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS chapters (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    volume_id   UUID NOT NULL REFERENCES volumes(id) ON DELETE CASCADE,
    project_id  UUID NOT NULL REFERENCES projects(id),
    title       VARCHAR(200),
    outline     TEXT,
    content     TEXT DEFAULT '',
    word_count  INT DEFAULT 0,
    sort_order  INT NOT NULL,
    status      VARCHAR(20) DEFAULT 'pending',
    created_at  TIMESTAMPTZ DEFAULT now(),
    updated_at  TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS chapter_snapshots (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chapter_id    UUID NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    content       TEXT NOT NULL,
    word_count    INT,
    snapshot_type VARCHAR(30),
    label         VARCHAR(200),
    created_at    TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS chapter_summaries (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chapter_id  UUID NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    project_id  UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    summary     VARCHAR(500) NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT now(),
    UNIQUE(chapter_id)
);

CREATE TABLE IF NOT EXISTS chapter_key_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    chapter_id  UUID NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    event_text  VARCHAR(500) NOT NULL,
    event_type  VARCHAR(30),
    characters  JSONB DEFAULT '[]',
    importance  DECIMAL(3,2) DEFAULT 0.5,
    created_at  TIMESTAMPTZ DEFAULT now(),
    UNIQUE(chapter_id, event_text)
);

-- ── 角色库与世界观词典 ────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS characters (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id             UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name                   VARCHAR(200) NOT NULL,
    role                   VARCHAR(50),
    appearance             TEXT,
    personality            TEXT,
    abilities              JSONB DEFAULT '[]',
    speech_style           TEXT,
    status                 JSONB DEFAULT '{}',
    is_locked              BOOLEAN DEFAULT FALSE,
    first_chapter_id       UUID REFERENCES chapters(id),
    last_active_chapter_id UUID REFERENCES chapters(id),
    created_at             TIMESTAMPTZ DEFAULT now(),
    updated_at             TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS world_elements (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    type        VARCHAR(50),
    name        VARCHAR(200),
    description TEXT,
    aliases     TEXT NOT NULL DEFAULT '[]',    -- V5 合并：专有名词别名 JSON 数组
    is_locked   BOOLEAN DEFAULT FALSE,
    created_at  TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS world_dict (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id       UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    term             VARCHAR(200) NOT NULL,
    aliases          JSONB DEFAULT '[]',
    term_type        VARCHAR(50),
    definition       VARCHAR(500),
    first_chapter_id UUID REFERENCES chapters(id),
    created_at       TIMESTAMPTZ DEFAULT now(),
    updated_at       TIMESTAMPTZ DEFAULT now(),
    UNIQUE(project_id, term)
);

-- ── 向量嵌入 ──────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS character_event_embeddings (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    character_id UUID REFERENCES characters(id) ON DELETE CASCADE,
    project_id   UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    event_text   TEXT,
    chapter_id   UUID REFERENCES chapters(id),
    embedding    VECTOR(1024),
    created_at   TIMESTAMPTZ DEFAULT now()
);

-- ── 导入会话 ──────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS import_parse_sessions (
    id              UUID PRIMARY KEY,
    project_id      UUID NOT NULL,
    user_id         UUID NOT NULL,
    parsed_chapters TEXT NOT NULL DEFAULT '[]',
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT now()
);

-- ── 写作统计 ──────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS writing_daily_stats (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id        UUID REFERENCES projects(id) ON DELETE SET NULL,
    stat_date         DATE NOT NULL,
    manual_chars      INT DEFAULT 0,
    ai_accepted_chars INT DEFAULT 0,
    total_chars       INT DEFAULT 0,
    peak_hour         INT DEFAULT -1,    -- V15 合并：当日写作高峰小时（-1=未记录）
    updated_at        TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS quota_usage (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users(id),
    year_month       VARCHAR(7),
    used_chars       BIGINT DEFAULT 0,
    used_adaptations INT DEFAULT 0,
    updated_at       TIMESTAMPTZ DEFAULT now(),
    UNIQUE(user_id, year_month)
);

-- ── AI 任务与操作日志 ─────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS ai_task_progress (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES users(id),
    project_id     UUID REFERENCES projects(id),
    task_type      VARCHAR(50) NOT NULL,
    status         VARCHAR(20) DEFAULT 'pending',
    current_step   INT DEFAULT 0,
    total_steps    INT,
    step_label     VARCHAR(200),
    progress_pct   SMALLINT DEFAULT 0,
    result_id      UUID,
    result_json    TEXT,           -- V7 合并：异步任务结果存储（骨架 JSON 等 LLM 输出）
    error_message  TEXT,
    created_at     TIMESTAMPTZ DEFAULT now(),
    updated_at     TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ai_operation_logs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID REFERENCES users(id),
    project_id    UUID REFERENCES projects(id),
    operation     VARCHAR(50),
    model         VARCHAR(100),
    input_tokens  INT,
    output_tokens INT,
    latency_ms    INT,
    quality_flag  VARCHAR(20) DEFAULT 'normal',
    created_at    TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS consistency_report_items (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id    UUID NOT NULL REFERENCES ai_operation_logs(id) ON DELETE CASCADE,
    project_id   UUID NOT NULL REFERENCES projects(id),
    type         VARCHAR(30) NOT NULL,
    character    VARCHAR(200),
    chapter_hint VARCHAR(200),
    description  TEXT NOT NULL,
    suggestion   TEXT,
    status       VARCHAR(20) DEFAULT 'open',
    created_at   TIMESTAMPTZ DEFAULT now(),
    updated_at   TIMESTAMPTZ DEFAULT now()
);

-- ── 用户文风档案 ──────────────────────────────────────────────────────────────
-- 注：V8 重定义，替换 V1 中的 sample_1/2/3 为 sample_text + style_tags TEXT

CREATE TABLE IF NOT EXISTS user_style_profiles (
    id               UUID PRIMARY KEY,
    user_id          UUID NOT NULL UNIQUE,
    sample_text      TEXT,
    style_tags       TEXT NOT NULL DEFAULT '[]',
    analysis_task_id UUID,
    created_at       TIMESTAMPTZ DEFAULT now(),
    updated_at       TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS style_templates (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name          VARCHAR(100) NOT NULL,
    template_type VARCHAR(20) NOT NULL,
    genres        TEXT NOT NULL DEFAULT '[]',
    prompt        TEXT NOT NULL,
    is_active     BOOLEAN DEFAULT FALSE,
    created_at    TIMESTAMPTZ DEFAULT now(),
    updated_at    TIMESTAMPTZ DEFAULT now()
);

-- ── 剧本系统 ──────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS script_drafts (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id     UUID NOT NULL REFERENCES projects(id),
    user_id        UUID NOT NULL REFERENCES users(id),
    title          VARCHAR(200),
    source_range   JSONB,
    strategy       VARCHAR(30),
    status         VARCHAR(20) DEFAULT 'processing',
    total_scenes   INT,
    created_at     TIMESTAMPTZ DEFAULT now(),
    updated_at     TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS script_episodes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    draft_id    UUID NOT NULL REFERENCES script_drafts(id) ON DELETE CASCADE,
    episode_no  INT NOT NULL,
    title       VARCHAR(200),
    sort_order  INT NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS script_scenes (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    draft_id       UUID NOT NULL REFERENCES script_drafts(id) ON DELETE CASCADE,
    episode_id     UUID REFERENCES script_episodes(id),
    scene_index    INT NOT NULL,
    location       VARCHAR(200),
    time_desc      VARCHAR(100),
    is_interior    BOOLEAN,
    characters     JSONB DEFAULT '[]',
    importance     DECIMAL(3,2),
    content        TEXT,
    source_content TEXT,
    format_passed  BOOLEAN,
    version        INT DEFAULT 0,
    created_at     TIMESTAMPTZ DEFAULT now(),
    updated_at     TIMESTAMPTZ DEFAULT now()
);

-- ── 订阅套餐 ──────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS subscription_plans (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_key                 VARCHAR(32)  UNIQUE NOT NULL,
    name                     VARCHAR(100) NOT NULL,
    monthly_char_limit       BIGINT       NOT NULL,
    monthly_adaptation_limit INT          NOT NULL,
    price_per_month          NUMERIC(10, 2) NOT NULL DEFAULT 0,
    description              TEXT,
    is_active                BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO subscription_plans (plan_key, name, monthly_char_limit, monthly_adaptation_limit, price_per_month, description) VALUES
('free',       '免费版',  100000,    5,   0.00, '体验全部核心写作功能，每月 10 万字 AI 配额，5 次改编/审查'),
('pro',        '专业版',  2000000,   50,  39.00, '无限续写，每月 200 万字 AI 配额，50 次改编/审查，优先响应'),
('enterprise', '企业版',  10000000,  200, 199.00,'团队协作，每月 1000 万字 AI 配额，200 次改编/审查，专属客服')
ON CONFLICT (plan_key) DO NOTHING;

CREATE TABLE IF NOT EXISTS user_subscriptions (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan_key   VARCHAR(32) NOT NULL REFERENCES subscription_plans(plan_key),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    status     VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ── 内容安全申诉 ──────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS content_appeals (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content      TEXT        NOT NULL,
    reason       TEXT        NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'pending',
    reviewer_note TEXT,
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ── 支付订单 ──────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS payment_orders (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    order_no         VARCHAR(64) UNIQUE NOT NULL,
    product_type     VARCHAR(32) NOT NULL,           -- 'subscription' | 'topup'
    product_key      VARCHAR(64) NOT NULL,           -- plan_key 或字数包 key
    amount_fen       BIGINT      NOT NULL,           -- 金额（分）
    currency         VARCHAR(8)  NOT NULL DEFAULT 'CNY',
    payment_channel  VARCHAR(32),                    -- 'wechat' | 'alipay' | 'credit_card'
    status           VARCHAR(20) NOT NULL DEFAULT 'pending',
    -- 状态：pending / paid / cancelled / refunded
    paid_at          TIMESTAMP WITH TIME ZONE,
    channel_order_no VARCHAR(128),                    -- 支付渠道返回的订单号
    raw_callback     TEXT,                            -- 原始回调报文（用于核查）
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ── 团队版 ────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS teams (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id                  UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name                      VARCHAR(100) NOT NULL,
    plan_key                  VARCHAR(32) NOT NULL DEFAULT 'enterprise',
    monthly_char_limit        BIGINT      NOT NULL DEFAULT 10000000,
    monthly_adaptation_limit  INT         NOT NULL DEFAULT 200,
    created_at                TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS team_members (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id     UUID        NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        VARCHAR(20) NOT NULL DEFAULT 'member',
    -- 角色：admin | editor | writer | member
    status      VARCHAR(20) NOT NULL DEFAULT 'pending',
    -- 状态：pending（邀请中）| active（已接受）| removed（已移除）
    invited_by  UUID REFERENCES users(id),
    invite_code VARCHAR(64) UNIQUE,
    joined_at   TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (team_id, user_id)
);

-- ── 字数包与体验额度 ──────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS word_packs (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    pack_key     VARCHAR(64) NOT NULL,              -- 'trial' / 'topup_100k' 等
    pack_type    VARCHAR(20) NOT NULL DEFAULT 'topup',  -- 'trial' | 'topup'
    chars_total  BIGINT      NOT NULL,               -- 总字符数
    chars_used   BIGINT      NOT NULL DEFAULT 0,     -- 已消耗字符数
    -- 注：字数包不过期（expires_at = null 视为永久有效）
    expires_at   TIMESTAMP WITH TIME ZONE,
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ── 项目协作者 ────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS project_collaborators (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID        NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        VARCHAR(20) NOT NULL DEFAULT 'editor',
    -- 角色：editor（可读写章节）| viewer（只读）
    added_by    UUID        REFERENCES users(id),
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (project_id, user_id)
);

-- ============================================================================
-- 索引
-- ============================================================================

-- 用户与认证
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_hash ON refresh_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_access_tokens_hash  ON access_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_account_restore_tokens_hash ON account_restore_tokens(token_hash);

-- 作品与章节
CREATE INDEX IF NOT EXISTS idx_projects_user      ON projects(user_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_chapters_project   ON chapters(project_id, sort_order);

-- 角色与世界观
CREATE INDEX IF NOT EXISTS idx_characters_project ON characters(project_id);
CREATE INDEX IF NOT EXISTS idx_world_dict_project ON world_dict(project_id);

-- 章节摘要与事件
CREATE INDEX IF NOT EXISTS idx_summaries_project  ON chapter_summaries(project_id, chapter_id);
CREATE INDEX IF NOT EXISTS idx_key_events_project ON chapter_key_events(project_id, importance DESC);

-- 向量检索
CREATE INDEX IF NOT EXISTS idx_embeddings_vector
    ON character_event_embeddings USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- 剧本
CREATE INDEX IF NOT EXISTS idx_script_episodes_draft ON script_episodes(draft_id, episode_no);
CREATE INDEX IF NOT EXISTS idx_script_scenes_draft   ON script_scenes(draft_id, scene_index);

-- 配额与统计
CREATE INDEX IF NOT EXISTS idx_quota_user_month      ON quota_usage(user_id, year_month);
CREATE INDEX IF NOT EXISTS idx_writing_stats_user_date ON writing_daily_stats(user_id, stat_date DESC);
CREATE INDEX IF NOT EXISTS idx_style_templates_user_type ON style_templates(user_id, template_type);
CREATE UNIQUE INDEX IF NOT EXISTS ux_writing_stats_project_daily
    ON writing_daily_stats(user_id, project_id, stat_date)
    WHERE project_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_writing_stats_global_daily
    ON writing_daily_stats(user_id, stat_date)
    WHERE project_id IS NULL;

-- 订阅与支付
CREATE INDEX IF NOT EXISTS idx_user_subscriptions_user   ON user_subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_subscriptions_active  ON user_subscriptions(user_id, status) WHERE status = 'active';
CREATE INDEX IF NOT EXISTS idx_payment_orders_user        ON payment_orders(user_id);
CREATE INDEX IF NOT EXISTS idx_payment_orders_order_no    ON payment_orders(order_no);
CREATE INDEX IF NOT EXISTS idx_payment_orders_status      ON payment_orders(status);

-- 内容安全
CREATE INDEX IF NOT EXISTS idx_content_appeals_user   ON content_appeals(user_id);
CREATE INDEX IF NOT EXISTS idx_content_appeals_status ON content_appeals(status);

-- 团队
CREATE INDEX IF NOT EXISTS idx_team_members_team    ON team_members(team_id);
CREATE INDEX IF NOT EXISTS idx_team_members_user    ON team_members(user_id);
CREATE INDEX IF NOT EXISTS idx_team_members_invite  ON team_members(invite_code);

-- 字数包
CREATE INDEX IF NOT EXISTS idx_word_packs_user        ON word_packs(user_id);
CREATE INDEX IF NOT EXISTS idx_word_packs_user_active ON word_packs(user_id, chars_used)
    WHERE chars_used < chars_total;

-- 项目协作者
CREATE INDEX IF NOT EXISTS idx_proj_collabs_project ON project_collaborators(project_id);
CREATE INDEX IF NOT EXISTS idx_proj_collabs_user    ON project_collaborators(user_id);

