-- P5-10: 用户文风档案（样本 + 异步分析生成的标签）
CREATE TABLE user_style_profiles (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL UNIQUE,
    sample_text         TEXT,
    style_tags          TEXT NOT NULL DEFAULT '[]',
    analysis_task_id    UUID,
    created_at          TIMESTAMPTZ DEFAULT now(),
    updated_at          TIMESTAMPTZ DEFAULT now()
);
