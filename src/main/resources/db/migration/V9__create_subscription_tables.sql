-- V9: 订阅套餐与用户订阅关系表（P9-01/P9-02）
-- subscription_plans: 套餐定义（预置三档：free/pro/enterprise）
-- user_subscriptions: 用户与套餐的关联关系

-- ── 套餐定义表 ──────────────────────────────────────────────────────────────
CREATE TABLE subscription_plans (
    id                         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_key                   VARCHAR(32)  UNIQUE NOT NULL,
    name                       VARCHAR(100) NOT NULL,
    monthly_char_limit         BIGINT       NOT NULL,
    monthly_adaptation_limit   INT          NOT NULL,
    price_per_month            NUMERIC(10, 2) NOT NULL DEFAULT 0,
    description                TEXT,
    is_active                  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                 TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 预置套餐数据
INSERT INTO subscription_plans (plan_key, name, monthly_char_limit, monthly_adaptation_limit, price_per_month, description) VALUES
('free',       '免费版',  100000,    5,   0.00, '体验全部核心写作功能，每月 10 万字 AI 配额，5 次改编/审查'),
('pro',        '专业版',  2000000,   50,  39.00, '无限续写，每月 200 万字 AI 配额，50 次改编/审查，优先响应'),
('enterprise', '企业版',  10000000,  200, 199.00,'团队协作，每月 1000 万字 AI 配额，200 次改编/审查，专属客服');

-- ── 用户订阅关系表 ────────────────────────────────────────────────────────────
CREATE TABLE user_subscriptions (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan_key   VARCHAR(32) NOT NULL REFERENCES subscription_plans(plan_key),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    status     VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_subscriptions_user ON user_subscriptions(user_id);
CREATE INDEX idx_user_subscriptions_active
    ON user_subscriptions(user_id, status)
    WHERE status = 'active';
