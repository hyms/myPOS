export const FINANCIAL_ACCUMULATORS_MIGRATION_SQL = `
-- v2.0.0 · financial-accumulators
-- Renames the legacy cash summary singleton to English identifiers.

ALTER TABLE resumen_caja RENAME TO financial_accumulators;
ALTER TABLE financial_accumulators RENAME COLUMN total_ventas TO total_sales;
ALTER TABLE financial_accumulators RENAME COLUMN total_compras TO total_purchases;
ALTER TABLE financial_accumulators RENAME COLUMN total_gastos TO total_expenses;
ALTER TABLE financial_accumulators RENAME COLUMN saldo_actual TO net_balance;
ALTER TABLE financial_accumulators ADD COLUMN updated_at TIMESTAMP DEFAULT NULL;

UPDATE financial_accumulators SET updated_at = CURRENT_TIMESTAMP WHERE id = 1;

CREATE INDEX IF NOT EXISTS idx_financial_accumulators_updated_at
  ON financial_accumulators(updated_at);
`;

// Recovery path for databases created by an earlier v2.0.0 attempt that had
// already renamed `resumen_caja` to `financial_accumulators` while leaving the
// legacy Spanish column names in place.
export const FINANCIAL_ACCUMULATORS_COLUMNS_MIGRATION_SQL = `
ALTER TABLE financial_accumulators RENAME COLUMN total_ventas TO total_sales;
ALTER TABLE financial_accumulators RENAME COLUMN total_compras TO total_purchases;
ALTER TABLE financial_accumulators RENAME COLUMN total_gastos TO total_expenses;
ALTER TABLE financial_accumulators RENAME COLUMN saldo_actual TO net_balance;
ALTER TABLE financial_accumulators ADD COLUMN updated_at TIMESTAMP DEFAULT NULL;

UPDATE financial_accumulators SET updated_at = CURRENT_TIMESTAMP WHERE id = 1;

CREATE INDEX IF NOT EXISTS idx_financial_accumulators_updated_at
  ON financial_accumulators(updated_at);
`;
