export const EXPENSES_CASHFLOW_MIGRATION_SQL = `
-- v2.0.0 · expenses-cashflow
-- Expenses are stored in sales_transactions (type = 'GASTO'). v2.0.0 adds the
-- expense category catalog for forward-compatible expense classification.

CREATE TABLE IF NOT EXISTS expense_categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL
);

CREATE INDEX IF NOT EXISTS idx_expense_categories_name
  ON expense_categories(name) WHERE deleted_at IS NULL;
`;
