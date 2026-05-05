CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE deals (
    id                      UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id                 UUID          NOT NULL,
    renter_id               UUID          NOT NULL,
    owner_id                UUID          NOT NULL,
    start_date              TIMESTAMPTZ   NOT NULL,
    end_date                TIMESTAMPTZ   NOT NULL,
    pricing_mode            VARCHAR(10)   NOT NULL
        CHECK (pricing_mode IN ('HOUR', 'DAY')),
    price_per_day_snapshot  DECIMAL(10,2),
    price_per_hour_snapshot DECIMAL(10,2),
    total_price             DECIMAL(10,2) NOT NULL,
    deposit_amount          DECIMAL(10,2) NOT NULL DEFAULT 0,
    status                  VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'CONFIRMED', 'PAYMENT_PENDING', 'ACTIVE', 'COMPLETED', 'REJECTED', 'CANCELLED')),
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
        (pricing_mode = 'DAY' AND price_per_day_snapshot IS NOT NULL
        AND price_per_hour_snapshot IS NULL)
            OR
        (pricing_mode = 'HOUR' AND price_per_hour_snapshot IS NOT NULL
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

CREATE TABLE deal_reviews (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    deal_id          UUID        NOT NULL REFERENCES deals(id) ON DELETE CASCADE,
    item_id          UUID        NOT NULL,
    reviewer_id      UUID        NOT NULL,
    reviewed_user_id UUID        NOT NULL,
    review_type      VARCHAR(30) NOT NULL
        CHECK (review_type IN ('RENTER_TO_OWNER', 'OWNER_TO_RENTER')),
    rating           INT         NOT NULL CHECK (rating BETWEEN 1 AND 5),
    text             TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE (deal_id, reviewer_id)
);

CREATE INDEX deal_reviews_deal_id_idx
    ON deal_reviews(deal_id);

CREATE INDEX deal_reviews_item_id_idx
    ON deal_reviews(item_id);

CREATE INDEX deal_reviews_reviewer_id_idx
    ON deal_reviews(reviewer_id);

CREATE INDEX deal_reviews_reviewed_user_id_idx
    ON deal_reviews(reviewed_user_id);

CREATE TABLE transactions (
    id                         UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    deal_id                    UUID          NOT NULL REFERENCES deals(id) ON DELETE CASCADE,
    type                       VARCHAR(20)   NOT NULL
        CHECK (type IN ('RENTAL', 'DEPOSIT_HOLD', 'DEPOSIT_RELEASE', 'PENALTY')),
    amount                     DECIMAL(10,2) NOT NULL CHECK (amount >= 0),
    status                     VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'HELD', 'CAPTURED', 'REFUNDED', 'FAILED', 'CANCELLED')),
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
        CHECK (change_source IN ('USER', 'SYSTEM', 'PAYMENT_WEBHOOK', 'MODERATOR')),
    comment       TEXT,
    changed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX deal_status_history_deal_id_idx
    ON deal_status_history(deal_id);

CREATE INDEX deal_status_history_changed_at_idx
    ON deal_status_history(changed_at);

CREATE TABLE deal_confirmations (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    deal_id      UUID        NOT NULL REFERENCES deals(id) ON DELETE CASCADE,
    user_id      UUID        NOT NULL,
    action       VARCHAR(20) NOT NULL CHECK (action IN ('START', 'COMPLETE')),
    confirmed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE (deal_id, user_id, action)
);

CREATE INDEX deal_confirmations_deal_id_idx
    ON deal_confirmations(deal_id);

CREATE TABLE complaints (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id     UUID         NOT NULL,
    target_type   VARCHAR(20)  NOT NULL CHECK (target_type IN ('USER', 'ITEM')),
    target_id     UUID         NOT NULL,
    reason        TEXT         NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'DISMISSED')),
    handled_by    UUID,
    resolution    TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX complaints_status_idx
    ON complaints(status);

CREATE INDEX complaints_author_id_idx
    ON complaints(author_id);

CREATE INDEX complaints_target_idx
    ON complaints(target_type, target_id);

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

CREATE TRIGGER trg_deal_reviews_updated_at
BEFORE UPDATE ON deal_reviews
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_transactions_updated_at
BEFORE UPDATE ON transactions
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();