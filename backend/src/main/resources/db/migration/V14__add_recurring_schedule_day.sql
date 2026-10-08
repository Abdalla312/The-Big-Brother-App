ALTER TABLE recurring_transactions
    ADD COLUMN scheduled_day_of_month INTEGER;

UPDATE recurring_transactions
SET scheduled_day_of_month = EXTRACT(DAY FROM next_execution_date)::INTEGER;

ALTER TABLE recurring_transactions
    ALTER COLUMN scheduled_day_of_month SET NOT NULL;

ALTER TABLE recurring_transactions
    ADD CONSTRAINT chk_recurring_scheduled_day
        CHECK ( recurring_transactions.scheduled_day_of_month BETWEEN 1 AND 31);