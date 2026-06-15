CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE users (
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

CREATE TABLE projects (
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

CREATE TABLE volumes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    title       VARCHAR(200),
    conflict    TEXT,
    sort_order  INT NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE chapters (
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

CREATE TABLE chapter_snapshots (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chapter_id    UUID NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    content       TEXT NOT NULL,
    word_count    INT,
    snapshot_type VARCHAR(30),
    label         VARCHAR(200),
    created_at    TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE chapter_summaries (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chapter_id  UUID NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    project_id  UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    summary     VARCHAR(500) NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT now(),
    UNIQUE(chapter_id)
);

CREATE TABLE characters (
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

CREATE TABLE world_elements (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    type        VARCHAR(50),
    name        VARCHAR(200),
    description TEXT,
    is_locked   BOOLEAN DEFAULT FALSE,
    created_at  TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE world_dict (
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

CREATE TABLE script_drafts (
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

CREATE TABLE script_episodes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    draft_id    UUID NOT NULL REFERENCES script_drafts(id) ON DELETE CASCADE,
    episode_no  INT NOT NULL,
    title       VARCHAR(200),
    sort_order  INT NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE script_scenes (
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

CREATE TABLE user_style_profiles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(100) DEFAULT '我的文风',
    sample_1    TEXT,
    sample_2    TEXT,
    sample_3    TEXT,
    style_tags  JSONB DEFAULT '[]',
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMPTZ DEFAULT now(),
    updated_at  TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE ai_operation_logs (
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

CREATE TABLE quota_usage (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users(id),
    year_month       VARCHAR(7),
    used_chars       BIGINT DEFAULT 0,
    used_adaptations INT DEFAULT 0,
    updated_at       TIMESTAMPTZ DEFAULT now(),
    UNIQUE(user_id, year_month)
);

CREATE TABLE writing_daily_stats (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id        UUID REFERENCES projects(id) ON DELETE SET NULL,
    stat_date         DATE NOT NULL,
    manual_chars      INT DEFAULT 0,
    ai_accepted_chars INT DEFAULT 0,
    total_chars       INT DEFAULT 0,
    peak_hour         SMALLINT,
    updated_at        TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE consistency_report_items (
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

CREATE TABLE ai_task_progress (
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
    error_message  TEXT,
    created_at     TIMESTAMPTZ DEFAULT now(),
    updated_at     TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE chapter_key_events (
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

CREATE TABLE character_event_embeddings (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    character_id UUID REFERENCES characters(id) ON DELETE CASCADE,
    project_id   UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    event_text   TEXT,
    chapter_id   UUID REFERENCES chapters(id),
    embedding    VECTOR(1024),
    created_at   TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE email_verifications (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE password_resets (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_projects_user ON projects(user_id, updated_at DESC);
CREATE INDEX idx_chapters_project ON chapters(project_id, sort_order);
CREATE INDEX idx_characters_project ON characters(project_id);
CREATE INDEX idx_summaries_project ON chapter_summaries(project_id, chapter_id);
CREATE INDEX idx_world_dict_project ON world_dict(project_id);
CREATE INDEX idx_script_episodes_draft ON script_episodes(draft_id, episode_no);
CREATE INDEX idx_script_scenes_draft ON script_scenes(draft_id, scene_index);
CREATE INDEX idx_quota_user_month ON quota_usage(user_id, year_month);
CREATE INDEX idx_writing_stats_user_date ON writing_daily_stats(user_id, stat_date DESC);
CREATE UNIQUE INDEX ux_writing_stats_project_daily
    ON writing_daily_stats(user_id, project_id, stat_date)
    WHERE project_id IS NOT NULL;
CREATE UNIQUE INDEX ux_writing_stats_global_daily
    ON writing_daily_stats(user_id, stat_date)
    WHERE project_id IS NULL;
CREATE INDEX idx_key_events_project ON chapter_key_events(project_id, importance DESC);
CREATE INDEX idx_embeddings_vector
    ON character_event_embeddings USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
