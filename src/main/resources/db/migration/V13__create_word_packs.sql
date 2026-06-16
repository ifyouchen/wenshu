-- V13: 字数包与体验额度表（P9-09）
-- word_packs: 用户购买的不过期字数包，可叠加到月度配额

CREATE TABLE word_packs (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    pack_key     VARCHAR(64) NOT NULL,          -- 'trial'（体验额度）/ 'topup_100k' 等
    pack_type    VARCHAR(20) NOT NULL DEFAULT 'topup',   -- 'trial' | 'topup'
    chars_total  BIGINT      NOT NULL,           -- 总字符数
    chars_used   BIGINT      NOT NULL DEFAULT 0, -- 已消耗字符数
    -- 注：body_packs 不过期（expires_at = null 视为永久有效）
    expires_at   TIMESTAMP WITH TIME ZONE,
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_word_packs_user ON word_packs(user_id);
CREATE INDEX idx_word_packs_user_active ON word_packs(user_id, chars_used)
    WHERE chars_used < chars_total;
