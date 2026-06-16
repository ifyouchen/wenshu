-- V11: 支付订单表（P9-03）
-- payment_orders: 记录每笔支付订单（购买订阅 / 购买字数包）

CREATE TABLE payment_orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    order_no        VARCHAR(64) UNIQUE NOT NULL,
    product_type    VARCHAR(32) NOT NULL,    -- 'subscription' | 'topup'
    product_key     VARCHAR(64) NOT NULL,    -- plan_key 或字数包 key
    amount_fen      BIGINT      NOT NULL,    -- 金额（分）
    currency        VARCHAR(8)  NOT NULL DEFAULT 'CNY',
    payment_channel VARCHAR(32),             -- 'wechat' | 'alipay' | 'credit_card'
    status          VARCHAR(20) NOT NULL DEFAULT 'pending',
    -- 状态：pending / paid / cancelled / refunded
    paid_at         TIMESTAMP WITH TIME ZONE,
    channel_order_no VARCHAR(128),           -- 支付渠道返回的订单号
    raw_callback    TEXT,                    -- 原始回调报文（用于核查）
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_orders_user ON payment_orders(user_id);
CREATE INDEX idx_payment_orders_order_no ON payment_orders(order_no);
CREATE INDEX idx_payment_orders_status ON payment_orders(status);
