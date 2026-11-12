export const SALES_CHECKOUT_MIGRATION_SQL = `
-- v2.0.0 · sales-checkout
-- Renames the legacy Spanish sales schema to English identifiers.

ALTER TABLE transacciones RENAME TO sales_transactions;
ALTER TABLE sales_transactions RENAME COLUMN tipo TO type;
ALTER TABLE sales_transactions RENAME COLUMN monto_total TO total_amount;
ALTER TABLE sales_transactions RENAME COLUMN tipo_pago TO payment_method;
ALTER TABLE sales_transactions RENAME COLUMN detalle TO detail;
ALTER TABLE sales_transactions RENAME COLUMN fecha_registro TO registered_at;
ALTER TABLE sales_transactions RENAME COLUMN fecha_delete TO deleted_at;

ALTER TABLE detalle_transacciones RENAME TO transaction_details;
ALTER TABLE transaction_details RENAME COLUMN transaccion_id TO transaction_id;
ALTER TABLE transaction_details RENAME COLUMN producto_id TO product_id;
ALTER TABLE transaction_details RENAME COLUMN cantidad TO quantity;
ALTER TABLE transaction_details RENAME COLUMN precio_unitario TO unit_price;

DROP INDEX IF EXISTS idx_transacciones_fecha;
DROP INDEX IF EXISTS idx_transacciones_tipo;
DROP INDEX IF EXISTS idx_transacciones_fecha_tipo;
DROP INDEX IF EXISTS idx_detalle_trx;
DROP INDEX IF EXISTS idx_detalle_producto;
DROP INDEX IF EXISTS idx_detalle_transaccion;

CREATE INDEX IF NOT EXISTS idx_sales_transactions_registered_at
  ON sales_transactions(registered_at) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_sales_transactions_type
  ON sales_transactions(type) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_sales_transactions_registered_type
  ON sales_transactions(registered_at, type, deleted_at);
CREATE INDEX IF NOT EXISTS idx_transaction_details_transaction
  ON transaction_details(transaction_id);
CREATE INDEX IF NOT EXISTS idx_transaction_details_product
  ON transaction_details(product_id);
`;
