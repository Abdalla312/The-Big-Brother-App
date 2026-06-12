INSERT INTO categories (id, name, type, color, icon, user_id, created_at)
VALUES (gen_random_uuid(), 'Food & Dining', 'EXPENSE', '#FF6B6B', 'restaurant', null, NOW()),
       (gen_random_uuid(), 'Transportation', 'EXPENSE', '#4ECDC4', 'directions-car', null, NOW()),
       (gen_random_uuid(), 'Housing', 'EXPENSE', '#45B7D1', 'home', null, NOW()),
       (gen_random_uuid(), 'Utilities', 'EXPENSE', '#FFA07A', 'bolt', null, NOW());

-- Insert default Income categories
INSERT INTO categories (id, name, type, color, icon, created_at)
VALUES (gen_random_uuid(), 'Salary', 'INCOME', '#98D8C8', 'attach-money', NOW()),
       (gen_random_uuid(), 'Investments', 'INCOME', '#F6E58D', 'trending-up', NOW());
