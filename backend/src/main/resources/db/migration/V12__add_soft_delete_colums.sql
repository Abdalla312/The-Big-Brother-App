ALTER TABLE users
    ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE categories
    ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE budgets
    ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE transactions
    ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE recurring_transactions
    ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE recurring_transactions
    ALTER COLUMN updated_at DROP NOT NULL;