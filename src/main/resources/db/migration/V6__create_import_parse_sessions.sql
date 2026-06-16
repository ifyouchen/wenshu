-- P4-01/P4-02: 文件导入解析会话
-- parsed_chapters 存储章节 JSON 数组，格式: [{"index":0,"title":"第一章","content":"..."}]
CREATE TABLE import_parse_sessions (
    id          UUID PRIMARY KEY,
    project_id  UUID NOT NULL,
    user_id     UUID NOT NULL,
    parsed_chapters TEXT NOT NULL DEFAULT '[]',
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT now()
);
