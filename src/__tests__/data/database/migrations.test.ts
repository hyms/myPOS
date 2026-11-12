import { type SQLiteDatabase } from 'expo-sqlite';

import { runMigrations, TARGET_VERSION } from '@/data/database/migrations';
import {
  createLegacyDatabase,
  createMigratedDatabase,
} from '@/__tests__/support/database';

function userVersion(db: SQLiteDatabase): number {
  return db.getFirstSync<{ user_version: number }>('PRAGMA user_version;')?.user_version ?? 0;
}

function tableExists(db: SQLiteDatabase, table: string): boolean {
  const row = db.getFirstSync<{ name: string }>(
    "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
    table,
  );
  return row !== null;
}

function columnNames(db: SQLiteDatabase, table: string): ReadonlyArray<string> {
  return db.getAllSync<{ name: string }>(`PRAGMA table_info(${table});`).map((row) => row.name);
}

function seedLegacyData(db: SQLiteDatabase): void {
  db.runSync("INSERT INTO categorias (nombre) VALUES ('Bebidas')");
  db.runSync(
    `INSERT INTO productos
       (categoria_id, nombre, precio_venta, precio_compra, stock_actual, stock_minimo, imagen_uri)
     VALUES (1, 'Coca Cola', 100, 60, 10, 2, NULL)`,
  );
  db.runSync(
    "INSERT INTO transacciones (tipo, monto_total, tipo_pago, detalle) VALUES ('VENTA', 200, 'EFECTIVO', 'Venta legacy')",
  );
  db.runSync(
    'INSERT INTO detalle_transacciones (transaccion_id, producto_id, cantidad, precio_unitario) VALUES (1, 1, 2, 100)',
  );
  db.runSync('UPDATE resumen_caja SET total_ventas = 200, saldo_actual = 200 WHERE id = 1');
}

describe('runMigrations', () => {
  describe('v1 → v2 upgrade', () => {
    it('renames the schema to English and preserves every row', () => {
      const db = createLegacyDatabase();
      seedLegacyData(db);

      runMigrations(db);

      expect(userVersion(db)).toBe(TARGET_VERSION);
      expect(tableExists(db, 'categorias')).toBe(false);
      expect(tableExists(db, 'productos')).toBe(false);
      expect(tableExists(db, 'transacciones')).toBe(false);
      expect(tableExists(db, 'detalle_transacciones')).toBe(false);
      expect(tableExists(db, 'resumen_caja')).toBe(false);

      expect(tableExists(db, 'categories')).toBe(true);
      expect(tableExists(db, 'products')).toBe(true);
      expect(tableExists(db, 'sales_transactions')).toBe(true);
      expect(tableExists(db, 'transaction_details')).toBe(true);
      expect(tableExists(db, 'financial_accumulators')).toBe(true);
      expect(tableExists(db, 'expense_categories')).toBe(true);
      expect(tableExists(db, 'settings')).toBe(true);

      const category = db.getFirstSync<{ id: number; name: string }>(
        'SELECT id, name FROM categories WHERE id = 1',
      );
      expect(category).toEqual({ id: 1, name: 'Bebidas' });

      const product = db.getFirstSync<{
        category_id: number;
        name: string;
        selling_price: number;
        purchase_price: number;
        current_stock: number;
        min_stock: number;
      }>(
        'SELECT category_id, name, selling_price, purchase_price, current_stock, min_stock FROM products WHERE id = 1',
      );
      expect(product).toEqual({
        category_id: 1,
        name: 'Coca Cola',
        selling_price: 100,
        purchase_price: 60,
        current_stock: 10,
        min_stock: 2,
      });

      const transaction = db.getFirstSync<{
        type: string;
        total_amount: number;
        payment_method: string;
        detail: string;
      }>(
        'SELECT type, total_amount, payment_method, detail FROM sales_transactions WHERE id = 1',
      );
      expect(transaction).toEqual({
        type: 'VENTA',
        total_amount: 200,
        payment_method: 'EFECTIVO',
        detail: 'Venta legacy',
      });

      const detail = db.getFirstSync<{
        transaction_id: number;
        product_id: number;
        quantity: number;
        unit_price: number;
      }>(
        'SELECT transaction_id, product_id, quantity, unit_price FROM transaction_details WHERE id = 1',
      );
      expect(detail).toEqual({
        transaction_id: 1,
        product_id: 1,
        quantity: 2,
        unit_price: 100,
      });

      const accumulator = db.getFirstSync<{
        total_sales: number;
        total_purchases: number;
        total_expenses: number;
        net_balance: number;
      }>(
        'SELECT total_sales, total_purchases, total_expenses, net_balance FROM financial_accumulators WHERE id = 1',
      );
      expect(accumulator).toEqual({
        total_sales: 200,
        total_purchases: 0,
        total_expenses: 0,
        net_balance: 200,
      });

      const settings = db.getFirstSync<{ currency_code: string; ad_enabled: number }>(
        'SELECT currency_code, ad_enabled FROM settings WHERE id = 1',
      );
      expect(settings).toEqual({ currency_code: 'Bs', ad_enabled: 0 });
    });

    it('backfills a NULL product category with the General category', () => {
      const db = createLegacyDatabase();
      db.runSync(
        `INSERT INTO productos (categoria_id, nombre, precio_venta, precio_compra)
         VALUES (NULL, 'Sin categoría', 10, 5)`,
      );

      runMigrations(db);

      const category = db.getFirstSync<{ id: number; name: string }>(
        "SELECT id, name FROM categories WHERE name = 'General'",
      );
      expect(category).not.toBeNull();

      const product = db.getFirstSync<{ category_id: number }>(
        'SELECT category_id FROM products WHERE name = ?',
        'Sin categoría',
      );
      expect(product!.category_id).toBe(category!.id);
      expect(columnNames(db, 'products')).toContain('category_id');
    });

    it('adds the NOT NULL constraint to category_id after backfill', () => {
      const db = createMigratedDatabase();
      expect(() =>
        db.runSync(
          `INSERT INTO products (category_id, name, selling_price, purchase_price)
           VALUES (NULL, 'Inválido', 1, 1)`,
        ),
      ).toThrow();
    });
  });

  describe('idempotency', () => {
    it('does nothing when the database is already at the target version', () => {
      const db = createLegacyDatabase();
      seedLegacyData(db);
      runMigrations(db);

      expect(() => runMigrations(db)).not.toThrow();
      expect(userVersion(db)).toBe(TARGET_VERSION);

      const count = db.getFirstSync<{ total: number }>(
        'SELECT COUNT(*) AS total FROM products',
      );
      expect(count!.total).toBe(1);
    });
  });

  describe('atomic rollback', () => {
    it('rolls back every v2 step when a later step fails', () => {
      const db = createLegacyDatabase();
      seedLegacyData(db);

      // Force the financial-accumulators step to fail: the rename target
      // already exists, so `ALTER TABLE resumen_caja RENAME TO ...` throws.
      db.execSync(`
        CREATE TABLE financial_accumulators (
          id INTEGER PRIMARY KEY CHECK (id = 1),
          total_ventas REAL DEFAULT 0.0,
          total_compras REAL DEFAULT 0.0,
          total_gastos REAL DEFAULT 0.0,
          saldo_actual REAL DEFAULT 0.0
        );
      `);

      expect(() => runMigrations(db)).toThrow();

      // Version is unchanged and no v2 table was committed.
      expect(userVersion(db)).toBe(1);
      expect(tableExists(db, 'categorias')).toBe(true);
      expect(tableExists(db, 'productos')).toBe(true);
      expect(tableExists(db, 'transacciones')).toBe(true);
      expect(tableExists(db, 'detalle_transacciones')).toBe(true);
      expect(tableExists(db, 'resumen_caja')).toBe(true);
      expect(tableExists(db, 'products')).toBe(false);
      expect(tableExists(db, 'sales_transactions')).toBe(false);
      expect(tableExists(db, 'settings')).toBe(false);

      // Foreign key enforcement is restored after the failure.
      const fk = db.getFirstSync<{ foreign_keys: number }>('PRAGMA foreign_keys;');
      expect(fk!.foreign_keys).toBe(1);
    });
  });

  describe('half-migrated recovery', () => {
    it('finishes a database where resumen_caja was renamed but columns were not', () => {
      const db = createLegacyDatabase();
      seedLegacyData(db);

      // Simulate the previous v2.0.0 attempt: table renamed, columns untouched,
      // version bumped, catalog and sales still in their legacy shape.
      db.execSync('ALTER TABLE resumen_caja RENAME TO financial_accumulators;');
      db.execSync('PRAGMA user_version = 2;');

      runMigrations(db);

      expect(userVersion(db)).toBe(TARGET_VERSION);
      expect(tableExists(db, 'products')).toBe(true);
      expect(tableExists(db, 'sales_transactions')).toBe(true);
      expect(columnNames(db, 'financial_accumulators')).toEqual(
        expect.arrayContaining([
          'total_sales',
          'total_purchases',
          'total_expenses',
          'net_balance',
          'updated_at',
        ]),
      );
      expect(columnNames(db, 'financial_accumulators')).not.toContain('total_ventas');

      const accumulator = db.getFirstSync<{ total_sales: number; net_balance: number }>(
        'SELECT total_sales, net_balance FROM financial_accumulators WHERE id = 1',
      );
      expect(accumulator).toEqual({ total_sales: 200, net_balance: 200 });
    });
  });
});
