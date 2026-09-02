CREATE TABLE recurring_transactions
(
    id                  UUID PRIMARY KEY                  DEFAULT gen_random_uuid(),
    user_id             UUID                     NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id         UUID                     NOT NULL REFERENCES categories (id) ON DELETE RESTRICT,
    type                VARCHAR(20)              NOT NULL,
    amount              NUMERIC(12, 2)           NOT NULL,
    payment_method      VARCHAR(50),
    note                TEXT,
    frequency           VARCHAR(20)              NOT NULL,
    next_execution_date DATE                     NOT NULL,
    is_active           BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_recurring_tx_user ON recurring_transactions (user_id);
CREATE INDEX idx_recurring_tx_due ON recurring_transactions (is_active, next_execution_date) WHERE is_active = TRUE;
CREATE INDEX idx_recurring_tx_category ON recurring_transactions (category_id);