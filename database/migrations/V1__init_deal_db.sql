CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE deals (
    id                      UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id                 UUID          NOT NULL,
    renter_id               UUID          NOT NULL,
    owner_id                UUID          NOT NULL,
    start_date              TIMESTAMPTZ   NOT NULL,
    end_date                TIMESTAMPTZ   NOT NULL,
    pricing_mode            VARCHAR(10)   NOT NULL
        CHECK (pricing_mode IN ('hour', 'day')),
    price_per_day_snapshot  DECIMAL(10,2),
    price_per_hour_snapshot DECIMAL(10,2),
    total_price             DECIMAL(10,2) NOT NULL,
    deposit_amount          DECIMAL(10,2) NOT NULL DEFAULT 0,
    status                  VARCHAR(20)   NOT NULL DEFAULT 'new'
        CHECK (status IN ('new', 'confirmed', 'active', 'completed', 'rejected')),
    rejection_reason        TEXT,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CHECK (end_date > start_date),
    CHECK (total_price >= 0),
    CHECK (deposit_amount >= 0),
    CHECK (price_per_day_snapshot IS NULL OR
        price_per_day_snapshot >= 0),
    CHECK (price_per_hour_snapshot IS NULL OR
        price_per_hour_snapshot >= 0),
    CHECK (
        (pricing_mode = 'day' AND price_per_day_snapshot IS NOT NULL
        AND price_per_hour_snapshot IS NULL)
            OR
        (pricing_mode = 'hour' AND price_per_hour_snapshot IS NOT NULL
        AND price_per_day_snapshot IS NULL)
    )
);

CREATE INDEX deals_item_id_idx
    ON deals(item_id);

CREATE INDEX deals_renter_id_idx
    ON deals(renter_id);

CREATE INDEX deals_owner_id_idx
    ON deals(owner_id);

CREATE INDEX deals_status_idx
    ON deals(status);

CREATE INDEX deals_start_date_idx
    ON deals(start_date);

CREATE INDEX deals_end_date_idx
    ON deals(end_date);

CREATE TABLE deal_comments (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    deal_id    UUID        NOT NULL REFERENCES deals(id) ON DELETE CASCADE,
    author_id  UUID        NOT NULL,
    text       TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX deal_comments_deal_id_idx
    ON deal_comments(deal_id);

CREATE INDEX deal_comments_author_id_idx
    ON deal_comments(author_id);

CREATE TABLE transactions (
    id                         UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    deal_id                    UUID          NOT NULL REFERENCES deals(id) ON DELETE CASCADE,
    type                       VARCHAR(20)   NOT NULL
        CHECK (type IN ('rental', 'deposit_hold', 'deposit_release', 'penalty')),
    amount                     DECIMAL(10,2) NOT NULL CHECK (amount >= 0),
    status                     VARCHAR(20)   NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'held', 'captured', 'refunded', 'failed', 'cancelled')),
    yookassa_payment_id        VARCHAR(100),
    yookassa_payment_method_id VARCHAR(100),
    gateway_response           JSONB,
    created_at                 TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX transactions_deal_id_idx
    ON transactions(deal_id);

CREATE INDEX transactions_status_idx
    ON transactions(status);

CREATE INDEX transactions_type_idx
    ON transactions(type);

CREATE INDEX transactions_yookassa_payment_id_idx
    ON transactions(yookassa_payment_id);

CREATE TABLE deal_status_history (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    deal_id       UUID        NOT NULL REFERENCES deals(id) ON DELETE CASCADE,
    old_status    VARCHAR(20),
    new_status    VARCHAR(20) NOT NULL,
    changed_by    UUID,
    change_source VARCHAR(20) NOT NULL
        CHECK (change_source IN ('user', 'system', 'payment_webhook', 'moderator')),
    comment       TEXT,
    changed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX deal_status_history_deal_id_idx
    ON deal_status_history(deal_id);

CREATE INDEX deal_status_history_changed_at_idx
    ON deal_status_history(changed_at);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_deals_updated_at
BEFORE UPDATE ON deals
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_deal_comments_updated_at
BEFORE UPDATE ON deal_comments
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_transactions_updated_at
BEFORE UPDATE ON transactions
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();