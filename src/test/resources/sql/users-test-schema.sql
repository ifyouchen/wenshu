DROP TABLE IF EXISTS consistency_report_items;
DROP TABLE IF EXISTS ai_operation_logs;
DROP TABLE IF EXISTS script_scenes;
DROP TABLE IF EXISTS script_episodes;
DROP TABLE IF EXISTS script_drafts;
DROP TABLE IF EXISTS user_style_profiles;
DROP TABLE IF EXISTS chapter_key_events;
DROP TABLE IF EXISTS quota_usage;
DROP TABLE IF EXISTS chapter_summaries;
DROP TABLE IF EXISTS ai_task_progress;
DROP TABLE IF EXISTS import_parse_sessions;
DROP TABLE IF EXISTS world_elements;
DROP TABLE IF EXISTS characters;
DROP TABLE IF EXISTS writing_daily_stats;
DROP TABLE IF EXISTS chapter_snapshots;
DROP TABLE IF EXISTS chapters;
DROP TABLE IF EXISTS volumes;
DROP TABLE IF EXISTS projects;
DROP TABLE IF EXISTS access_tokens;
DROP TABLE IF EXISTS account_restore_tokens;
DROP TABLE IF EXISTS email_verifications;
DROP TABLE IF EXISTS password_resets;
DROP TABLE IF EXISTS refresh_tokens;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(100),
    avatar_url VARCHAR(500),
    identity_type VARCHAR(20) DEFAULT 'new_author',
    is_email_verified BOOLEAN DEFAULT FALSE,
    ai_train_consent BOOLEAN DEFAULT TRUE,
    daily_char_goal INT DEFAULT 2000,
    login_fail_count SMALLINT DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE email_verifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(128) UNIQUE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    replaced_by_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE password_resets (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE access_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(128) UNIQUE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE account_restore_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE projects (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    genre VARCHAR(50),
    synopsis TEXT,
    worldview TEXT,
    total_words INT DEFAULT 0,
    daily_char_goal INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'draft',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE volumes (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    title VARCHAR(200),
    conflict TEXT,
    sort_order INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE chapters (
    id UUID PRIMARY KEY,
    volume_id UUID NOT NULL,
    project_id UUID NOT NULL,
    title VARCHAR(200),
    outline TEXT,
    content TEXT DEFAULT '',
    word_count INT DEFAULT 0,
    sort_order INT NOT NULL,
    status VARCHAR(20) DEFAULT 'pending',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE chapter_snapshots (
    id UUID PRIMARY KEY,
    chapter_id UUID NOT NULL,
    content TEXT NOT NULL,
    word_count INT,
    snapshot_type VARCHAR(30),
    label VARCHAR(200),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE writing_daily_stats (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    project_id UUID,
    stat_date DATE NOT NULL,
    manual_chars INT DEFAULT 0,
    ai_accepted_chars INT DEFAULT 0,
    total_chars INT DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE characters (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    name VARCHAR(200),
    role VARCHAR(50),
    appearance TEXT,
    personality TEXT,
    abilities TEXT DEFAULT '[]',
    speech_style TEXT,
    status VARCHAR(200) DEFAULT '{}',
    is_locked BOOLEAN DEFAULT FALSE,
    first_chapter_id UUID,
    last_active_chapter_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE world_elements (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    type VARCHAR(50),
    name VARCHAR(200),
    description TEXT,
    aliases TEXT NOT NULL DEFAULT '[]',
    is_locked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE import_parse_sessions (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    user_id UUID NOT NULL,
    parsed_chapters TEXT NOT NULL DEFAULT '[]',
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_task_progress (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    project_id UUID,
    task_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) DEFAULT 'pending',
    current_step INT DEFAULT 0,
    total_steps INT,
    step_label VARCHAR(200),
    progress_pct SMALLINT DEFAULT 0,
    result_id UUID,
    result_json TEXT,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE chapter_summaries (
    id UUID PRIMARY KEY,
    chapter_id UUID NOT NULL UNIQUE,
    project_id UUID NOT NULL,
    summary VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_style_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    sample_text TEXT,
    style_tags TEXT NOT NULL DEFAULT '[]',
    analysis_task_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE chapter_key_events (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    chapter_id UUID NOT NULL,
    event_text VARCHAR(500) NOT NULL,
    event_type VARCHAR(30),
    characters TEXT DEFAULT '[]',
    importance DECIMAL(3,2) DEFAULT 0.5,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (chapter_id, event_text)
);

CREATE TABLE quota_usage (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    year_month VARCHAR(7) NOT NULL,
    used_chars BIGINT DEFAULT 0,
    used_adaptations INT DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, year_month)
);

CREATE TABLE ai_operation_logs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    project_id UUID,
    operation VARCHAR(50),
    model VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE consistency_report_items (
    id UUID PRIMARY KEY,
    report_id UUID NOT NULL,
    project_id UUID NOT NULL,
    type VARCHAR(30),
    character VARCHAR(200),
    chapter_hint VARCHAR(200),
    description TEXT NOT NULL,
    suggestion TEXT,
    status VARCHAR(20) DEFAULT 'open',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE script_drafts (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    user_id UUID NOT NULL,
    title VARCHAR(200),
    strategy VARCHAR(30),
    status VARCHAR(20) DEFAULT 'processing',
    total_scenes INT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE script_episodes (
    id UUID PRIMARY KEY,
    draft_id UUID NOT NULL,
    episode_no INT NOT NULL,
    title VARCHAR(200),
    sort_order INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE script_scenes (
    id UUID PRIMARY KEY,
    draft_id UUID NOT NULL,
    episode_id UUID,
    scene_index INT NOT NULL,
    location VARCHAR(200),
    time_desc VARCHAR(100),
    is_interior BOOLEAN,
    characters TEXT DEFAULT '[]',
    content TEXT,
    source_content TEXT,
    version INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);