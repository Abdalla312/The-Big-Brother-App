CREATE TABLE budgets (
    id UUID PRIMARY KEY,
    "month" VARCHAR(7) NOT NULL,
    limit_amount DECIMAL(19, 4) NOT NULL,
    user_id UUID NOT NULL,
    category_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_budget_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_budget_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_category_month UNIQUE (user_id, category_id, "month")
);