ALTER TABLE budgets DROP CONSTRAINT IF EXISTS uq_user_category_month;

CREATE UNIQUE INDEX uq_user_category_month_active
ON budgets (user_id, category_id, "month")
WHERE deleted_at IS NULL;