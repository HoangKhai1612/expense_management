-- ============================================================
-- V9: Reference data that is safe to ship in version control.
--   NOTE: no account rows are seeded here on purpose. The bootstrap
--   administrator is created at application start-up from environment
--   variables (see BootstrapDataInitializer) so that no password hash
--   is ever committed to the repository.
-- ============================================================

INSERT INTO roles (name, description) VALUES
    ('USER',  'Regular application user'),
    ('ADMIN', 'System administrator');

-- System expense categories
INSERT INTO categories (name, code, type, icon, color, is_system, is_active) VALUES
    ('Food & Drink',      'FOOD',         'EXPENSE', 'restaurant', '#EF5350', TRUE, TRUE),
    ('Transport',         'TRANSPORT',    'EXPENSE', 'directions_car', '#42A5F5', TRUE, TRUE),
    ('Shopping',          'SHOPPING',     'EXPENSE', 'shopping_bag',  '#AB47BC', TRUE, TRUE),
    ('Entertainment',     'ENTERTAINMENT','EXPENSE', 'movie',        '#26A69A', TRUE, TRUE),
    ('Bills & Utilities', 'BILLS',        'EXPENSE', 'receipt_long', '#FFA726', TRUE, TRUE),
    ('Health',            'HEALTH',       'EXPENSE', 'favorite',     '#EC407A', TRUE, TRUE),
    ('Education',         'EDUCATION',    'EXPENSE', 'school',       '#5C6BC0', TRUE, TRUE),
    ('Housing',           'HOUSING',      'EXPENSE', 'home',         '#8D6E63', TRUE, TRUE),
    ('Other Expense',     'OTHER_EXPENSE','EXPENSE', 'more_horiz',   '#78909C', TRUE, TRUE);

-- System income categories
INSERT INTO categories (name, code, type, icon, color, is_system, is_active) VALUES
    ('Salary',        'SALARY',    'INCOME', 'account_balance_wallet', '#66BB6A', TRUE, TRUE),
    ('Freelance',     'FREELANCE', 'INCOME', 'work',                  '#9CCC65', TRUE, TRUE),
    ('Investment',    'INVESTMENT','INCOME', 'trending_up',           '#26A69A', TRUE, TRUE),
    ('Gift',          'GIFT',      'INCOME', 'card_giftcard',         '#FF7043', TRUE, TRUE),
    ('Other Income',  'OTHER_INCOME','INCOME','more_horiz',            '#90A4AE', TRUE, TRUE);
