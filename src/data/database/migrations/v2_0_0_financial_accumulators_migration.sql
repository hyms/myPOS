-- Atomic migration transaction (single db.withTransactionSync)

-- Step 1: Rename table
ALTER TABLE resumen_caja RENAME TO financial_accumulators;

-- Step 2: Rename columns
ALTER TABLE financial_accumulators RENAME COLUMN total_ventas TO total_sales;
ALTER TABLE financial_accumulators RENAME COLUMN total_compras TO total_purchases;
ALTER TABLE financial_accumulators RENAME COLUMN total_gastos TO total_expenses;
ALTER TABLE financial_accumulators RENAME COLUMN saldo_actual TO net_balance;

-- Step 3: Add updated_at column with default
ALTER TABLE financial_accumulators ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Step 4: Update existing row's updated_at
UPDATE financial_accumulators SET updated_at = CURRENT_TIMESTAMP WHERE id = 1;

-- Step 5: Update user_version to 2 (coordinated with sales-checkout)
PRAGMA user_version = 2;