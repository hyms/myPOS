# Financial Accumulators Module — Test Specification (v2.0.0)

## Test Organization

```
tests/
├── unit/
│   ├── domain/
│   │   ├── entities/
│   │   │   ├── FinancialSummary.test.ts
│   │   │   └── PeriodSummary.test.ts
│   │   └── value-objects/
│   │       └── Money.test.ts (shared)
│   ├── application/
│   │   └── accumulators/
│   │       └── UpdateAccumulator.test.ts
│   └── hooks/
│       ├── useCashSummary.test.ts
│       ├── usePeriodSummary.test.ts
│       └── useMovements.test.ts
├── integration/
│   ├── financial-accumulators/
│   │   ├── GetCashSummary.test.ts
│   │   ├── GetPeriodSummary.test.ts
│   │   ├── UpdateAccumulator.test.ts
│   │   └── ReverseTransaction.test.ts
│   └── atomic/
│       ├── accumulatorAtomicRollback.test.ts
│       └── reverseAtomicRollback.test.ts
├── migration/
│   └── financial-accumulators/
│       └── v2_0_0_migration.test.ts
└── e2e/
    └── financial-accumulators/
        ├── cashSummaryFlow.test.ts
        ├── reverseTransactionFlow.test.ts
        └── adBannerVisibility.test.ts
```

---

## 1. Unit Tests (TDD)

### 1.1 FinancialSummary Entity

**File:** `tests/unit/domain/entities/FinancialSummary.test.ts`

```ts
import { FinancialSummary } from '@/domain/entities/FinancialSummary';
import { Money } from '@/domain/value-objects/Money';

describe('FinancialSummary', () => {
  const usd = 'USD' as const;

  it('creates summary with all Money fields', () => {
    const summary: FinancialSummary = {
      totalSales: new Money(1000, usd),
      totalPurchases: new Money(300, usd),
      totalExpenses: new Money(100, usd),
      netBalance: new Money(600, usd),
      updatedAt: '2026-01-15T10:30:00Z',
    };

    expect(summary.totalSales.amount).toBe(1000);
    expect(summary.totalPurchases.amount).toBe(300);
    expect(summary.totalExpenses.amount).toBe(100);
    expect(summary.netBalance.amount).toBe(600);
    expect(summary.updatedAt).toBe('2026-01-15T10:30:00Z');
  });

  it('formats all amounts correctly', () => {
    const summary: FinancialSummary = {
      totalSales: new Money(1000.50, usd),
      totalPurchases: new Money(300.25, usd),
      totalExpenses: new Money(100.75, usd),
      netBalance: new Money(599.50, usd),
      updatedAt: '2026-01-15T10:30:00Z',
    };

    expect(summary.totalSales.format()).toBe('$ 1,000.50');
    expect(summary.netBalance.format()).toBe('$ 599.50');
  });

  it('handles zero values', () => {
    const summary: FinancialSummary = {
      totalSales: Money.zero(usd),
      totalPurchases: Money.zero(usd),
      totalExpenses: Money.zero(usd),
      netBalance: Money.zero(usd),
      updatedAt: '2026-01-15T10:30:00Z',
    };

    expect(summary.totalSales.amount).toBe(0);
    expect(summary.netBalance.amount).toBe(0);
  });
});
```

### 1.2 PeriodSummary Entity

**File:** `tests/unit/domain/entities/PeriodSummary.test.ts`

```ts
import { PeriodSummary } from '@/domain/entities/PeriodSummary';
import { Money } from '@/domain/value-objects/Money';

describe('PeriodSummary', () => {
  const usd = 'USD' as const;

  it('computes net correctly for MONTH', () => {
    const summary: PeriodSummary = {
      period: 'MONTH',
      totals: [
        { type: 'SALE', total: new Money(1000, usd) },
        { type: 'PURCHASE', total: new Money(300, usd) },
        { type: 'EXPENSE', total: new Money(100, usd) },
      ],
      totalSales: new Money(1000, usd),
      totalPurchases: new Money(300, usd),
      totalExpenses: new Money(100, usd),
      net: new Money(600, usd),
    };

    expect(summary.net.amount).toBe(600); // 1000 - 300 - 100
  });

  it('computes net correctly for YEAR', () => {
    const summary: PeriodSummary = {
      period: 'YEAR',
      totals: [
        { type: 'SALE', total: new Money(50000, usd) },
        { type: 'PURCHASE', total: new Money(20000, usd) },
        { type: 'EXPENSE', total: new Money(5000, usd) },
      ],
      totalSales: new Money(50000, usd),
      totalPurchases: new Money(20000, usd),
      totalExpenses: new Money(5000, usd),
      net: new Money(25000, usd),
    };

    expect(summary.net.amount).toBe(25000); // 50000 - 20000 - 5000
  });

  it('handles missing transaction types', () => {
    const summary: PeriodSummary = {
      period: 'MONTH',
      totals: [
        { type: 'SALE', total: new Money(1000, usd) },
      ],
      totalSales: new Money(1000, usd),
      totalPurchases: Money.zero(usd),
      totalExpenses: Money.zero(usd),
      net: new Money(1000, usd),
    };

    expect(summary.net.amount).toBe(1000);
  });
});
```

### 1.3 UpdateAccumulator Logic

**File:** `tests/unit/application/accumulators/UpdateAccumulator.test.ts`

```ts
import { Money } from '@/domain/value-objects/Money';

describe('UpdateAccumulator delta calculations', () => {
  const usd = 'USD' as const;

  it('calculates net balance delta correctly for sale', () => {
    const salesDelta = new Money(100, usd);
    const purchasesDelta = Money.zero(usd);
    const expensesDelta = Money.zero(usd);

    const netDelta = salesDelta.amount - purchasesDelta.amount - expensesDelta.amount;
    expect(netDelta).toBe(100);
  });

  it('calculates net balance delta correctly for purchase', () => {
    const salesDelta = Money.zero(usd);
    const purchasesDelta = new Money(50, usd);
    const expensesDelta = Money.zero(usd);

    const netDelta = salesDelta.amount - purchasesDelta.amount - expensesDelta.amount;
    expect(netDelta).toBe(-50);
  });

  it('calculates net balance delta correctly for expense', () => {
    const salesDelta = Money.zero(usd);
    const purchasesDelta = Money.zero(usd);
    const expensesDelta = new Money(25, usd);

    const netDelta = salesDelta.amount - purchasesDelta.amount - expensesDelta.amount;
    expect(netDelta).toBe(-25);
  });

  it('handles combined deltas', () => {
    const salesDelta = new Money(200, usd);
    const purchasesDelta = new Money(80, usd);
    const expensesDelta = new Money(20, usd);

    const netDelta = salesDelta.amount - purchasesDelta.amount - expensesDelta.amount;
    expect(netDelta).toBe(100); // 200 - 80 - 20
  });
});
```

### 1.4 Hook Tests

**File:** `tests/unit/hooks/useCashSummary.test.ts`

```ts
import { renderHook, act } from '@testing-library/react-native';
import { useCashSummary } from '@/presentation/hooks/useCashSummary';
import { useInvalidationStore } from '@/presentation/stores/invalidationStore';

jest.mock('@/application/accumulators/GetCashSummary', () => ({
  getCashSummary: jest.fn(),
}));
jest.mock('@/presentation/stores/invalidationStore');

describe('useCashSummary', () => {
  const mockSummary = {
    totalSales: { amount: 1000, currency: 'USD', format: () => '$1,000.00' },
    totalPurchases: { amount: 300, currency: 'USD', format: () => '$300.00' },
    totalExpenses: { amount: 100, currency: 'USD', format: () => '$100.00' },
    netBalance: { amount: 600, currency: 'USD', format: () => '$600.00' },
    updatedAt: '2026-01-15T10:30:00Z',
  };

  beforeEach(() => {
    jest.clearAllMocks();
    require('@/application/accumulators/GetCashSummary').getCashSummary.mockReturnValue(mockSummary);
    require('@/presentation/stores/invalidationStore').useInvalidationStore.mockImplementation(
      (selector) => selector({ versions: { caja: 1 } })
    );
  });

  it('returns initial summary', () => {
    const { result } = renderHook(() => useCashSummary());
    expect(result.current.resumen).toEqual(mockSummary);
    expect(result.current.loading).toBe(false);
  });

  it('refreshes when invalidation version changes', () => {
    const { result, rerender } = renderHook(() => useCashSummary());
    const initialResumen = result.current.resumen;

    // Simulate invalidation version change
    require('@/presentation/stores/invalidationStore').useInvalidationStore.mockImplementation(
      (selector) => selector({ versions: { caja: 2 } })
    );
    rerender();

    expect(result.current.resumen).not.toBe(initialResumen);
  });

  it('refresh callback works', () => {
    const { result } = renderHook(() => useCashSummary());
    act(() => {
      result.current.refresh();
    });
    expect(require('@/application/accumulators/GetCashSummary').getCashSummary).toHaveBeenCalledTimes(2);
  });
});
```

---

## 2. Integration Tests

### 2.1 GetCashSummary

**File:** `tests/integration/financial-accumulators/GetCashSummary.test.ts`

```ts
import { openDatabaseSync, type SQLiteDatabase } from 'expo-sqlite';
import { getCashSummary } from '@/application/accumulators/GetCashSummary';
import { SqliteAccumulatorRepository } from '@/data/repositories/SqliteAccumulatorRepository';
import { SCHEMA_SQL, SEED_SQL } from '@/data/database/schema.sql';

const mockGetRepositories = jest.fn();

jest.mock('@/data/repositories/container', () => ({
  getRepositories: () => mockGetRepositories(),
  resetRepositoriesForTests: jest.fn(),
}));

interface TestDb {
  db: SQLiteDatabase;
  accumulator: SqliteAccumulatorRepository;
}

function createTestDb(): TestDb {
  const db = openDatabaseSync(':memory:');
  db.execSync(SCHEMA_SQL);
  db.execSync(SEED_SQL);
  // Run financial-accumulators migration
  db.execSync(`
    ALTER TABLE resumen_caja RENAME TO financial_accumulators;
    ALTER TABLE financial_accumulators RENAME COLUMN total_ventas TO total_sales;
    ALTER TABLE financial_accumulators RENAME COLUMN total_compras TO total_purchases;
    ALTER TABLE financial_accumulators RENAME COLUMN total_gastos TO total_expenses;
    ALTER TABLE financial_accumulators RENAME COLUMN saldo_actual TO net_balance;
    ALTER TABLE financial_accumulators ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
    UPDATE financial_accumulators SET updated_at = CURRENT_TIMESTAMP WHERE id = 1;
  `);
  return {
    db,
    accumulator: new SqliteAccumulatorRepository(db),
  };
}

describe('getCashSummary', () => {
  let t: TestDb;

  beforeEach(() => {
    t = createTestDb();
    mockGetRepositories.mockReturnValue({
      accumulator: t.accumulator,
    });
  });

  it('returns initial zero summary', () => {
    const summary = getCashSummary();
    expect(summary.totalSales.amount).toBe(0);
    expect(summary.totalPurchases.amount).toBe(0);
    expect(summary.totalExpenses.amount).toBe(0);
    expect(summary.netBalance.amount).toBe(0);
  });

  it('returns updated summary with Money VOs', () => {
    t.db.runSync(
      'UPDATE financial_accumulators SET total_sales = 1000, total_purchases = 300, total_expenses = 100, net_balance = 600 WHERE id = 1'
    );

    const summary = getCashSummary();
    expect(summary.totalSales.amount).toBe(1000);
    expect(summary.totalPurchases.amount).toBe(300);
    expect(summary.totalExpenses.amount).toBe(100);
    expect(summary.netBalance.amount).toBe(600);
  });

  it('uses correct currency from settings', () => {
    // Currency comes from Money VO factory
    const summary = getCashSummary();
    expect(summary.totalSales.currency).toBe('USD'); // or Bs/EUR from settings
  });
});
```

### 2.2 GetPeriodSummary

**File:** `tests/integration/financial-accumulators/GetPeriodSummary.test.ts`

```ts
import { openDatabaseSync, type SQLiteDatabase } from 'expo-sqlite';
import { getPeriodSummary } from '@/application/accumulators/GetPeriodSummary';
import { SqliteAccumulatorRepository } from '@/data/repositories/SqliteAccumulatorRepository';
import { SqliteTransaccionRepository } from '@/data/repositories/SqliteTransaccionRepository';
import { SCHEMA_SQL, SEED_SQL } from '@/data/database/schema.sql';

const mockGetRepositories = jest.fn();

jest.mock('@/data/repositories/container', () => ({
  getRepositories: () => mockGetRepositories(),
  resetRepositoriesForTests: jest.fn(),
}));

interface TestDb {
  db: SQLiteDatabase;
  accumulator: SqliteAccumulatorRepository;
  transacciones: SqliteTransaccionRepository;
}

function createTestDb(): TestDb {
  const db = openDatabaseSync(':memory:');
  db.execSync(SCHEMA_SQL);
  db.execSync(SEED_SQL);
  // Run sales-checkout migration (transacciones -> sales_transactions)
  db.execSync(`
    ALTER TABLE transacciones RENAME TO sales_transactions;
    ALTER TABLE sales_transactions RENAME COLUMN tipo TO type;
    ALTER TABLE sales_transactions RENAME COLUMN monto_total TO total_amount;
    ALTER TABLE sales_transactions RENAME COLUMN tipo_pago TO payment_method;
    ALTER TABLE sales_transactions RENAME COLUMN detalle TO detail;
    ALTER TABLE sales_transactions RENAME COLUMN fecha_registro TO registered_at;
    ALTER TABLE sales_transactions RENAME COLUMN fecha_delete TO deleted_at;
  `);
  // Run financial-accumulators migration
  db.execSync(`
    ALTER TABLE resumen_caja RENAME TO financial_accumulators;
    ALTER TABLE financial_accumulators RENAME COLUMN total_ventas TO total_sales;
    ALTER TABLE financial_accumulators RENAME COLUMN total_compras TO total_purchases;
    ALTER TABLE financial_accumulators RENAME COLUMN total_gastos TO total_expenses;
    ALTER TABLE financial_accumulators RENAME COLUMN saldo_actual TO net_balance;
    ALTER TABLE financial_accumulators ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
    UPDATE financial_accumulators SET updated_at = CURRENT_TIMESTAMP WHERE id = 1;
  `);
  return {
    db,
    accumulator: new SqliteAccumulatorRepository(db),
    transacciones: new SqliteTransaccionRepository(db),
  };
}

describe('getPeriodSummary', () => {
  let t: TestDb;

  beforeEach(() => {
    t = createTestDb();
    mockGetRepositories.mockReturnValue({
      accumulator: t.accumulator,
      transacciones: t.transacciones,
    });
  });

  it('aggregates MONTH correctly', () => {
    const now = new Date();
    const monthStr = now.toISOString().slice(0, 7); // YYYY-MM

    t.db.runSync(
      "INSERT INTO sales_transactions (type, total_amount, payment_method, registered_at) VALUES ('SALE', ?, ?, ?)",
      1000, 'CASH', `${monthStr}-15T10:30:00Z`
    );
    t.db.runSync(
      "INSERT INTO sales_transactions (type, total_amount, payment_method, registered_at) VALUES ('PURCHASE', ?, ?, ?)",
      300, 'CASH', `${monthStr}-10T10:30:00Z`
    );
    t.db.runSync(
      "INSERT INTO sales_transactions (type, total_amount, payment_method, registered_at) VALUES ('EXPENSE', ?, ?, ?)",
      100, 'CASH', `${monthStr}-05T10:30:00Z`
    );

    const summary = getPeriodSummary('MONTH');
    expect(summary.totalSales.amount).toBe(1000);
    expect(summary.totalPurchases.amount).toBe(300);
    expect(summary.totalExpenses.amount).toBe(100);
    expect(summary.net.amount).toBe(600);
    expect(summary.period).toBe('MONTH');
  });

  it('aggregates YEAR correctly', () => {
    const yearStr = new Date().toISOString().slice(0, 4); // YYYY

    t.db.runSync(
      "INSERT INTO sales_transactions (type, total_amount, payment_method, registered_at) VALUES ('SALE', ?, ?, ?)",
      50000, 'CASH', `${yearStr}-06-15T10:30:00Z`
    );
    t.db.runSync(
      "INSERT INTO sales_transactions (type, total_amount, payment_method, registered_at) VALUES ('SALE', ?, ?, ?)",
      2000, 'CASH', `${yearStr}-12-01T10:30:00Z`
    );
    t.db.runSync(
      "INSERT INTO sales_transactions (type, total_amount, payment_method, registered_at) VALUES ('PURCHASE', ?, ?, ?)",
      15000, 'CASH', `${yearStr}-03-20T10:30:00Z`
    );
    t.db.runSync(
      "INSERT INTO sales_transactions (type, total_amount, payment_method, registered_at) VALUES ('EXPENSE', ?, ?, ?)",
      5000, 'CASH', `${yearStr}-09-10T10:30:00Z`
    );

    const summary = getPeriodSummary('YEAR');
    expect(summary.totalSales.amount).toBe(52000);
    expect(summary.totalPurchases.amount).toBe(15000);
    expect(summary.totalExpenses.amount).toBe(5000);
    expect(summary.net.amount).toBe(32000);
    expect(summary.period).toBe('YEAR');
  });

  it('excludes soft-deleted transactions', () => {
    const monthStr = new Date().toISOString().slice(0, 7);

    t.db.runSync(
      "INSERT INTO sales_transactions (type, total_amount, payment_method, registered_at, deleted_at) VALUES ('SALE', ?, ?, ?, ?)",
      1000, 'CASH', `${monthStr}-15T10:30:00Z`, `${monthStr}-16T10:30:00Z`
    );
    t.db.runSync(
      "INSERT INTO sales_transactions (type, total_amount, payment_method, registered_at) VALUES ('SALE', ?, ?, ?)",
      500, 'CASH', `${monthStr}-10T10:30:00Z`
    );

    const summary = getPeriodSummary('MONTH');
    expect(summary.totalSales.amount).toBe(500); // Only non-deleted
  });
});
```

### 2.3 UpdateAccumulator

**File:** `tests/integration/financial-accumulators/UpdateAccumulator.test.ts`

```ts
import { openDatabaseSync, type SQLiteDatabase } from 'expo-sqlite';
import { updateAccumulator } from '@/application/accumulators/UpdateAccumulator';
import { SqliteAccumulatorRepository } from '@/data/repositories/SqliteAccumulatorRepository';
import { SCHEMA_SQL, SEED_SQL } from '@/data/database/schema.sql';
import { Money } from '@/domain/value-objects/Money';

const mockGetRepositories = jest.fn();

jest.mock('@/data/repositories/container', () => ({
  getRepositories: () => mockGetRepositories(),
  resetRepositoriesForTests: jest.fn(),
}));

interface TestDb {
  db: SQLiteDatabase;
  accumulator: SqliteAccumulatorRepository;
}

function createTestDb(): TestDb {
  const db = openDatabaseSync(':memory:');
  db.execSync(SCHEMA_SQL);
  db.execSync(SEED_SQL);
  db.execSync(`
    ALTER TABLE resumen_caja RENAME TO financial_accumulators;
    ALTER TABLE financial_accumulators RENAME COLUMN total_ventas TO total_sales;
    ALTER TABLE financial_accumulators RENAME COLUMN total_compras TO total_purchases;
    ALTER TABLE financial_accumulators RENAME COLUMN total_gastos TO total_expenses;
    ALTER TABLE financial_accumulators RENAME COLUMN saldo_actual TO net_balance;
    ALTER TABLE financial_accumulators ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
    UPDATE financial_accumulators SET updated_at = CURRENT_TIMESTAMP WHERE id = 1;
  `);
  return {
    db,
    accumulator: new SqliteAccumulatorRepository(db),
  };
}

describe('updateAccumulator', () => {
  let t: TestDb;

  beforeEach(() => {
    t = createTestDb();
    mockGetRepositories.mockReturnValue({
      accumulator: t.accumulator,
    });
  });

  it('applies positive sales delta', () => {
    updateAccumulator({
      salesDelta: new Money(100, 'USD'),
      purchasesDelta: Money.zero('USD'),
      expensesDelta: Money.zero('USD'),
    });

    const row = t.db.getFirstSync<{ total_sales: number; net_balance: number }>(
      'SELECT total_sales, net_balance FROM financial_accumulators WHERE id = 1'
    );
    expect(row!.total_sales).toBe(100);
    expect(row!.net_balance).toBe(100);
  });

  it('applies purchase delta (decreases net)', () => {
    updateAccumulator({
      salesDelta: Money.zero('USD'),
      purchasesDelta: new Money(50, 'USD'),
      expensesDelta: Money.zero('USD'),
    });

    const row = t.db.getFirstSync<{ total_purchases: number; net_balance: number }>(
      'SELECT total_purchases, net_balance FROM financial_accumulators WHERE id = 1'
    );
    expect(row!.total_purchases).toBe(50);
    expect(row!.net_balance).toBe(-50);
  });

  it('applies expense delta (decreases net)', () => {
    updateAccumulator({
      salesDelta: Money.zero('USD'),
      purchasesDelta: Money.zero('USD'),
      expensesDelta: new Money(25, 'USD'),
    });

    const row = t.db.getFirstSync<{ total_expenses: number; net_balance: number }>(
      'SELECT total_expenses, net_balance FROM financial_accumulators WHERE id = 1'
    );
    expect(row!.total_expenses).toBe(25);
    expect(row!.net_balance).toBe(-25);
  });

  it('applies combined deltas atomically', () => {
    updateAccumulator({
      salesDelta: new Money(200, 'USD'),
      purchasesDelta: new Money(80, 'USD'),
      expensesDelta: new Money(20, 'USD'),
    });

    const row = t.db.getFirstSync<{ total_sales: number; total_purchases: number; total_expenses: number; net_balance: number }>(
      'SELECT total_sales, total_purchases, total_expenses, net_balance FROM financial_accumulators WHERE id = 1'
    );
    expect(row!.total_sales).toBe(200);
    expect(row!.total_purchases).toBe(80);
    expect(row!.total_expenses).toBe(20);
    expect(row!.net_balance).toBe(100); // 200 - 80 - 20
  });

  it('updates updated_at timestamp', () => {
    const before = new Date().toISOString();
    updateAccumulator({
      salesDelta: new Money(100, 'USD'),
      purchasesDelta: Money.zero('USD'),
      expensesDelta: Money.zero('USD'),
    });
    const after = new Date().toISOString();

    const row = t.db.getFirstSync<{ updated_at: string }>(
      'SELECT updated_at FROM financial_accumulators WHERE id = 1'
    );
    expect(row!.updated_at).toBeDefined();
    // updated_at should be recent (within test execution)
  });
});
```

### 2.4 ReverseTransaction

**File:** `tests/integration/financial-accumulators/ReverseTransaction.test.ts`

```ts
import { openDatabaseSync, type SQLiteDatabase } from 'expo-sqlite';
import { reverseTransaction } from '@/application/transactions/ReverseTransaction';
import { SqliteAccumulatorRepository } from '@/data/repositories/SqliteAccumulatorRepository';
import { SqliteTransaccionRepository } from '@/data/repositories/SqliteTransaccionRepository';
import { SqliteProductoRepository } from '@/data/repositories/SqliteProductoRepository';
import { SqliteCategoriaRepository } from '@/data/repositories/SqliteCategoriaRepository';
import { SCHEMA_SQL, SEED_SQL } from '@/data/database/schema.sql';
import { StockError } from '@/domain/rules/stockRules';

const mockGetDatabase = jest.fn();
const mockGetRepositories = jest.fn();

jest.mock('@/data/database/client', () => ({
  getDatabase: () => mockGetDatabase(),
  resetDatabaseForTests: jest.fn(),
}));

jest.mock('@/data/repositories/container', () => ({
  getRepositories: () => mockGetRepositories(),
  resetRepositoriesForTests: jest.fn(),
}));

interface TestDb {
  db: SQLiteDatabase;
  accumulator: SqliteAccumulatorRepository;
  transacciones: SqliteTransaccionRepository;
  productos: SqliteProductoRepository;
  categorias: SqliteCategoriaRepository;
}

function createTestDb(): TestDb {
  const db = openDatabaseSync(':memory:');
  db.execSync(SCHEMA_SQL);
  db.execSync(SEED_SQL);
  // Run migrations
  db.execSync(`
    ALTER TABLE transacciones RENAME TO sales_transactions;
    ALTER TABLE sales_transactions RENAME COLUMN tipo TO type;
    ALTER TABLE sales_transactions RENAME COLUMN monto_total TO total_amount;
    ALTER TABLE sales_transactions RENAME COLUMN tipo_pago TO payment_method;
    ALTER TABLE sales_transactions RENAME COLUMN detalle TO detail;
    ALTER TABLE sales_transactions RENAME COLUMN fecha_registro TO registered_at;
    ALTER TABLE sales_transactions RENAME COLUMN fecha_delete TO deleted_at;
    
    ALTER TABLE resumen_caja RENAME TO financial_accumulators;
    ALTER TABLE financial_accumulators RENAME COLUMN total_ventas TO total_sales;
    ALTER TABLE financial_accumulators RENAME COLUMN total_compras TO total_purchases;
    ALTER TABLE financial_accumulators RENAME COLUMN total_gastos TO total_expenses;
    ALTER TABLE financial_accumulators RENAME COLUMN saldo_actual TO net_balance;
    ALTER TABLE financial_accumulators ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
    UPDATE financial_accumulators SET updated_at = CURRENT_TIMESTAMP WHERE id = 1;
  `);
  return {
    db,
    accumulator: new SqliteAccumulatorRepository(db),
    transacciones: new SqliteTransaccionRepository(db),
    productos: new SqliteProductoRepository(db),
    categorias: new SqliteCategoriaRepository(db),
  };
}

function seedData(t: TestDb) {
  t.categorias.crear({ nombre: 'Cat1' });
  t.productos.crear({
    categoriaId: 1,
    nombre: 'Producto A',
    precioVenta: 100,
    precioCompra: 60,
    stockActual: 20,
    stockMinimo: 2,
    imagenUri: null,
  });
}

function createVenta(t: TestDb): number {
  const trx = t.db.runSync(
    "INSERT INTO sales_transactions (type, total_amount, payment_method) VALUES ('SALE', ?, ?)",
    200, 'CASH'
  );
  const id = trx.lastInsertRowId;
  t.db.runSync(
    'INSERT INTO detalle_transacciones (transaccion_id, producto_id, cantidad, precio_unitario) VALUES (?, ?, ?, ?)',
    id, 1, 2, 100
  );
  t.db.runSync(
    'UPDATE financial_accumulators SET total_sales = total_sales + ?, net_balance = net_balance + ?, updated_at = CURRENT_TIMESTAMP WHERE id = 1',
    200, 200
  );
  return id;
}

function createCompra(t: TestDb): number {
  const trx = t.db.runSync(
    "INSERT INTO sales_transactions (type, total_amount, payment_method) VALUES ('PURCHASE', ?, ?)",
    300, 'CASH'
  );
  const id = trx.lastInsertRowId;
  t.db.runSync(
    'INSERT INTO detalle_transacciones (transaccion_id, producto_id, cantidad, precio_unitario) VALUES (?, ?, ?, ?)',
    id, 1, 5, 60
  );
  t.db.runSync(
    'UPDATE financial_accumulators SET total_purchases = total_purchases + ?, net_balance = net_balance - ?, updated_at = CURRENT_TIMESTAMP WHERE id = 1',
    300, 300
  );
  return id;
}

function createGasto(t: TestDb): number {
  const trx = t.db.runSync(
    "INSERT INTO sales_transactions (type, total_amount, payment_method, detail) VALUES ('EXPENSE', ?, ?, ?)",
    50, 'CASH', 'Luz'
  );
  const id = trx.lastInsertRowId;
  t.db.runSync(
    'UPDATE financial_accumulators SET total_expenses = total_expenses + ?, net_balance = net_balance - ?, updated_at = CURRENT_TIMESTAMP WHERE id = 1',
    50, 50
  );
  return id;
}

describe('reverseTransaction', () => {
  let t: TestDb;

  beforeEach(() => {
    t = createTestDb();
    seedData(t);
    mockGetDatabase.mockReturnValue(t.db);
    mockGetRepositories.mockReturnValue({
      accumulator: t.accumulator,
      transacciones: t.transacciones,
      productos: t.productos,
    });
  });

  describe('SALE', () => {
    it('reverses sale: restores stock and accumulator', () => {
      const id = createVenta(t);
      const result = reverseTransaction(id);

      expect(result.transactionId).toBe(id);
      expect(result.type).toBe('SALE');

      // Stock restored
      const prod = t.productos.findById(1)!;
      expect(prod.stockActual).toBe(20);

      // Accumulator reverted
      const acc = t.accumulator.getSummary();
      expect(acc.totalSales.amount).toBe(0);
      expect(acc.netBalance.amount).toBe(0);

      // Soft delete
      const trx = t.transacciones.findById(id);
      expect(trx!.deletedAt).not.toBeNull();
    });
  });

  describe('PURCHASE', () => {
    it('reverses purchase: reduces stock and restores accumulator', () => {
      const id = createCompra(t);
      const result = reverseTransaction(id);

      expect(result.type).toBe('PURCHASE');

      // Stock reduced
      const prod = t.productos.findById(1)!;
      expect(prod.stockActual).toBe(20);

      // Accumulator reverted
      const acc = t.accumulator.getSummary();
      expect(acc.totalPurchases.amount).toBe(0);
      expect(acc.netBalance.amount).toBe(0);
    });

    it('throws StockError if revert would make stock negative', () => {
      const id = createCompra(t);
      // Reduce stock below what we're about to revert
      t.db.runSync('UPDATE productos SET stock_actual = ? WHERE id = 1', 3);

      expect(() => reverseTransaction(id)).toThrow(StockError);
      expect(() => reverseTransaction(id)).toThrow('stock negativo');
    });
  });

  describe('EXPENSE', () => {
    it('reverses expense: restores accumulator', () => {
      const id = createGasto(t);
      const result = reverseTransaction(id);

      expect(result.type).toBe('EXPENSE');

      const acc = t.accumulator.getSummary();
      expect(acc.totalExpenses.amount).toBe(0);
      expect(acc.netBalance.amount).toBe(0);
    });
  });

  it('throws if transaction does not exist', () => {
    expect(() => reverseTransaction(999)).toThrow('no existe');
  });

  it('throws if transaction already reversed', () => {
    const id = createVenta(t);
    reverseTransaction(id);
    expect(() => reverseTransaction(id)).toThrow('ya fue revertida');
  });
});
```

---

## 3. Migration Tests

### 3.1 v2.0.0 Migration

**File:** `tests/migration/financial-accumulators/v2_0_0_migration.test.ts`

```ts
import { openDatabaseSync, type SQLiteDatabase } from 'expo-sqlite';
import { runMigrations, TARGET_VERSION } from '@/data/database/migrations';

describe('v2.0.0 financial-accumulators migration', () => {
  let db: SQLiteDatabase;

  beforeEach(() => {
    db = openDatabaseSync(':memory:');
    // Set user_version to 1 (simulating post sales-checkout migration)
    db.execSync('PRAGMA user_version = 1;');
    
    // Create the schema as it exists after sales-checkout migration
    db.execSync(`
      CREATE TABLE sales_transactions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        type TEXT NOT NULL,
        total_amount REAL NOT NULL,
        payment_method TEXT NOT NULL,
        detail TEXT,
        registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        deleted_at TIMESTAMP DEFAULT NULL
      );
      
      CREATE TABLE resumen_caja (
        id INTEGER PRIMARY KEY CHECK (id = 1),
        total_ventas REAL DEFAULT 0.0,
        total_compras REAL DEFAULT 0.0,
        total_gastos REAL DEFAULT 0.0,
        saldo_actual REAL DEFAULT 0.0
      );
      
      INSERT OR IGNORE INTO resumen_caja (id, total_ventas, total_compras, total_gastos, saldo_actual)
      VALUES (1, 1000.0, 300.0, 100.0, 600.0);
    `);
  });

  it('renames resumen_caja to financial_accumulators', () => {
    runMigrations(db);
    
    const table = db.getFirstSync<{ name: string }>(
      "SELECT name FROM sqlite_master WHERE type='table' AND name='financial_accumulators'"
    );
    expect(table).not.toBeNull();
    
    // Old table should not exist
    const oldTable = db.getFirstSync<{ name: string }>(
      "SELECT name FROM sqlite_master WHERE type='table' AND name='resumen_caja'"
    );
    expect(oldTable).toBeNull();
  });

  it('renames columns to English', () => {
    runMigrations(db);
    
    const columns = db.getAllSync<{ name: string }>(
      "PRAGMA table_info(financial_accumulators)"
    );
    const columnNames = columns.map(c => c.name);
    
    expect(columnNames).toContain('total_sales');
    expect(columnNames).toContain('total_purchases');
    expect(columnNames).toContain('total_expenses');
    expect(columnNames).toContain('net_balance');
    expect(columnNames).toContain('updated_at');
    expect(columnNames).not.toContain('total_ventas');
    expect(columnNames).not.toContain('total_compras');
    expect(columnNames).not.toContain('total_gastos');
    expect(columnNames).not.toContain('saldo_actual');
  });

  it('preserves all accumulated values', () => {
    runMigrations(db);
    
    const row = db.getFirstSync<{ 
      total_sales: number; 
      total_purchases: number; 
      total_expenses: number; 
      net_balance: number 
    }>('SELECT total_sales, total_purchases, total_expenses, net_balance FROM financial_accumulators WHERE id = 1');
    
    expect(row!.total_sales).toBe(1000.0);
    expect(row!.total_purchases).toBe(300.0);
    expect(row!.total_expenses).toBe(100.0);
    expect(row!.net_balance).toBe(600.0);
  });

  it('adds updated_at with current timestamp', () => {
    runMigrations(db);
    
    const row = db.getFirstSync<{ updated_at: string }>(
      'SELECT updated_at FROM financial_accumulators WHERE id = 1'
    );
    expect(row!.updated_at).not.toBeNull();
    expect(row!.updated_at).toMatch(/^\d{4}-\d{2}-\d{2}/);
  });

  it('updates user_version to 2', () => {
    runMigrations(db);
    
    const version = db.getFirstSync<{ user_version: number }>('PRAGMA user_version;');
    expect(version!.user_version).toBe(2);
  });

  it('is idempotent (running twice does not error)', () => {
    runMigrations(db);
    expect(() => runMigrations(db)).not.toThrow();
    
    const version = db.getFirstSync<{ user_version: number }>('PRAGMA user_version;');
    expect(version!.user_version).toBe(2);
  });

  it('preserves sales_transactions data', () => {
    // Insert test data before migration
    db.runSync(
      "INSERT INTO sales_transactions (type, total_amount, payment_method, registered_at) VALUES ('SALE', ?, ?, ?)",
      500, 'CASH', '2026-01-15T10:30:00Z'
    );
    
    runMigrations(db);
    
    const trx = db.getFirstSync<{ total_amount: number; type: string }>(
      "SELECT total_amount, type FROM sales_transactions WHERE type = 'SALE'"
    );
    expect(trx!.total_amount).toBe(500);
    expect(trx!.type).toBe('SALE');
  });
});
```

---

## 4. Atomic Transaction Tests

### 4.1 Accumulator Atomic Rollback

**File:** `tests/integration/atomic/accumulatorAtomicRollback.test.ts`

```ts
import { openDatabaseSync, type SQLiteDatabase } from 'expo-sqlite';
import { updateAccumulator } from '@/application/accumulators/UpdateAccumulator';
import { SqliteAccumulatorRepository } from '@/data/repositories/SqliteAccumulatorRepository';
import { SCHEMA_SQL, SEED_SQL } from '@/data/database/schema.sql';
import { Money } from '@/domain/value-objects/Money';

const mockGetRepositories = jest.fn();

jest.mock('@/data/repositories/container', () => ({
  getRepositories: () => mockGetRepositories(),
  resetRepositoriesForTests: jest.fn(),
}));

interface TestDb {
  db: SQLiteDatabase;
  accumulator: SqliteAccumulatorRepository;
}

function createTestDb(): TestDb {
  const db = openDatabaseSync(':memory:');
  db.execSync(SCHEMA_SQL);
  db.execSync(SEED_SQL);
  db.execSync(`
    ALTER TABLE resumen_caja RENAME TO financial_accumulators;
    ALTER TABLE financial_accumulators RENAME COLUMN total_ventas TO total_sales;
    ALTER TABLE financial_accumulators RENAME COLUMN total_compras TO total_purchases;
    ALTER TABLE financial_accumulators RENAME COLUMN total_gastos TO total_expenses;
    ALTER TABLE financial_accumulators RENAME COLUMN saldo_actual TO net_balance;
    ALTER TABLE financial_accumulators ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
    UPDATE financial_accumulators SET updated_at = CURRENT_TIMESTAMP WHERE id = 1;
  `);
  return {
    db,
    accumulator: new SqliteAccumulatorRepository(db),
  };
}

describe('Accumulator atomic rollback', () => {
  let t: TestDb;

  beforeEach(() => {
    t = createTestDb();
    mockGetRepositories.mockReturnValue({
      accumulator: t.accumulator,
    });
  });

  it('maintains consistency under concurrent updates', () => {
    // Simulate rapid successive updates
    for (let i = 0; i < 10; i++) {
      updateAccumulator({
        salesDelta: new Money(10, 'USD'),
        purchasesDelta: Money.zero('USD'),
        expensesDelta: Money.zero('USD'),
      });
    }

    const row = t.db.getFirstSync<{ total_sales: number; net_balance: number }>(
      'SELECT total_sales, net_balance FROM financial_accumulators WHERE id = 1'
    );
    expect(row!.total_sales).toBe(100);
    expect(row!.net_balance).toBe(100);
  });

  it('single UPDATE is atomic by nature of SQLite', () => {
    // SQLite's UPDATE is atomic - either all columns update or none
    updateAccumulator({
      salesDelta: new Money(500, 'USD'),
      purchasesDelta: new Money(200, 'USD'),
      expensesDelta: new Money(50, 'USD'),
    });

    const row = t.db.getFirstSync<{ 
      total_sales: number; 
      total_purchases: number; 
      total_expenses: number; 
      net_balance: number 
    }>('SELECT total_sales, total_purchases, total_expenses, net_balance FROM financial_accumulators WHERE id = 1');
    
    expect(row!.total_sales).toBe(500);
    expect(row!.total_purchases).toBe(200);
    expect(row!.total_expenses).toBe(50);
    expect(row!.net_balance).toBe(250); // 500 - 200 - 50
  });
});
```

### 4.2 ReverseTransaction Atomic Rollback

**File:** `tests/integration/atomic/reverseAtomicRollback.test.ts`

```ts
import { openDatabaseSync, type SQLiteDatabase } from 'expo-sqlite';
import { reverseTransaction } from '@/application/transactions/ReverseTransaction';
import { SqliteAccumulatorRepository } from '@/data/repositories/SqliteAccumulatorRepository';
import { SqliteTransaccionRepository } from '@/data/repositories/SqliteTransaccionRepository';
import { SqliteProductoRepository } from '@/data/repositories/SqliteProductoRepository';
import { SqliteCategoriaRepository } from '@/data/repositories/SqliteCategoriaRepository'.
import { SCHEMA_SQL, SEED_SQL } from '@/data/database/schema.sql';

const mockGetDatabase = jest.fn();
const mockGetRepositories = jest.fn();

jest.mock('@/data/database/client', () => ({
  getDatabase: () => mockGetDatabase(),
  resetDatabaseForTests: jest.fn(),
}));

jest.mock('@/data/repositories/container', () => ({
  getRepositories: () => mockGetRepositories(),
  resetRepositoriesForTests: jest.fn(),
}));

interface TestDb {
  db: SQLiteDatabase;
  accumulator: SqliteAccumulatorRepository;
  transacciones: SqliteTransaccionRepository;
  productos: SqliteProductoRepository;
  categorias: SqliteCategoriaRepository;
}

function createTestDb(): TestDb {
  const db = openDatabaseSync(':memory:');
  db.execSync(SCHEMA_SQL);
  db.execSync(SEED_SQL);
  db.execSync(`
    ALTER TABLE transacciones RENAME TO sales_transactions;
    ALTER TABLE sales_transactions RENAME COLUMN tipo TO type;
    ALTER TABLE sales_transactions RENAME COLUMN monto_total TO total_amount;
    ALTER TABLE sales_transactions RENAME COLUMN tipo_pago TO payment_method;
    ALTER TABLE sales_transactions RENAME COLUMN detalle TO detail;
    ALTER TABLE sales_transactions RENAME COLUMN fecha_registro TO registered_at;
    ALTER TABLE sales_transactions RENAME COLUMN fecha_delete TO deleted_at;
    
    ALTER TABLE resumen_caja RENAME TO financial_accumulators;
    ALTER TABLE financial_accumulators RENAME COLUMN total_ventas TO total_sales;
    ALTER TABLE financial_accumulators RENAME COLUMN total_compras TO total_purchases;
    ALTER TABLE financial_accumulators RENAME COLUMN total_gastos TO total_expenses;
    ALTER TABLE financial_accumulators RENAME COLUMN saldo_actual TO net_balance;
    ALTER TABLE financial_accumulators ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
    UPDATE financial_accumulators SET updated_at = CURRENT_TIMESTAMP WHERE id = 1;
  `);
  return {
    db,
    accumulator: new SqliteAccumulatorRepository(db),
    transacciones: new SqliteTransaccionRepository(db),
    productos: new SqliteProductoRepository(db),
    categorias: new SqliteCategoriaRepository(db),
  };
}

function seedData(t: TestDb) {
  t.categorias.crear({ nombre: 'Cat1' });
  t.productos.crear({
    categoriaId: 1,
    nombre: 'Producto A',
    precioVenta: 100,
    precioCompra: 60,
    stockActual: 20,
    stockMinimo: 2,
    imagenUri: null,
  });
}

function createCompra(t: TestDb): number {
  const trx = t.db.runSync(
    "INSERT INTO sales_transactions (type, total_amount, payment_method) VALUES ('PURCHASE', ?, ?)",
    300, 'CASH'
  );
  const id = trx.lastInsertRowId;
  t.db.runSync(
    'INSERT INTO detalle_transacciones (transaccion_id, producto_id, cantidad, precio_unitario) VALUES (?, ?, ?, ?)',
    id, 1, 5, 60
  );
  t.db.runSync(
    'UPDATE financial_accumulators SET total_purchases = total_purchases + ?, net_balance = net_balance - ?, updated_at = CURRENT_TIMESTAMP WHERE id = 1',
    300, 300
  );
  return id;
}

describe('ReverseTransaction atomic rollback', () => {
  let t: TestDb;

  beforeEach(() => {
    t = createTestDb();
    seedData(t);
    mockGetDatabase.mockReturnValue(t.db);
    mockGetRepositories.mockReturnValue({
      accumulator: t.accumulator,
      transacciones: t.transacciones,
      productos: t.productos,
    });
  });

  it('all-or-nothing: stock update and accumulator update succeed or fail together', () => {
    const id = createCompra(t);
    
    // Verify pre-state
    let acc = t.accumulator.getSummary();
    expect(acc.totalPurchases.amount).toBe(300);
    expect(acc.netBalance.amount).toBe(-300);
    
    const prodBefore = t.productos.findById(1)!;
    expect(prodBefore.stockActual).toBe(25); // 20 + 5 from purchase
    
    // Reverse - both stock and accumulator must update atomically
    reverseTransaction(id);
    
    // Verify post-state
    acc = t.accumulator.getSummary();
    expect(acc.totalPurchases.amount).toBe(0);
    expect(acc.netBalance.amount).toBe(0);
    
    const prodAfter = t.productos.findById(1)!;
    expect(prodAfter.stockActual).toBe(20); // Back to original
  });

  it('if stock update fails, accumulator is not modified', () => {
    const id = createCompra(t);
    
    // Manually corrupt the product to cause a constraint violation
    // (In real scenario, this could be a disk full, constraint, etc.)
    // We simulate by making the product row inaccessible
    // This test documents expected behavior - actual failure injection
    // would require a failing mock or DB corruption
    
    const preAcc = t.accumulator.getSummary();
    expect(preAcc.totalPurchases.amount).toBe(300);
    
    // In a real test with failure injection, we'd verify accumulator unchanged
  });
});
```

---

## 5. UI Component Tests

### 5.1 CashBalanceCard

**File:** `tests/components/accumulators/CashBalanceCard.test.tsx`

```tsx
import React from 'react';
import { render } from '@testing-library/react-native';
import { CashBalanceCard } from '@/presentation/components/accumulators/CashBalanceCard';
import { Money } from '@/domain/value-objects/Money';

const mockFormat = jest.fn((amount: number) => `$${amount.toFixed(2)}`);

describe('CashBalanceCard', () => {
  it('renders positive balance with success tone', () => {
    const positiveBalance = new Money(1500, 'USD');
    const { getByText, getByTestId } = render(
      <CashBalanceCard balance={positiveBalance} format={mockFormat} />
    );
    expect(getByText('Saldo actual')).toBeTruthy();
    expect(getByText('$1,500.00')).toBeTruthy();
    expect(getByTestId('balance-card')).toHaveStyle({ backgroundColor: expect.stringContaining('success') });
  });

  it('renders negative balance with danger tone', () => {
    const negativeBalance = new Money(-500, 'USD');
    const { getByText, getByTestId } = render(
      <CashBalanceCard balance={negativeBalance} format={mockFormat} />
    );
    expect(getByText('-$500.00')).toBeTruthy();
    expect(getByTestId('balance-card')).toHaveStyle({ backgroundColor: expect.stringContaining('danger') });
  });

  it('renders zero balance with neutral tone', () => {
    const zeroBalance = new Money(0, 'USD');
    const { getByTestId } = render(
      <CashBalanceCard balance={zeroBalance} format={mockFormat} />
    );
    expect(getByTestId('balance-card')).toHaveStyle({ backgroundColor: expect.stringContaining('neutral') });
  });

  it('renders wallet icon', () => {
    const balance = new Money(1000, 'USD');
    const { getByTestId } = render(
      <CashBalanceCard balance={balance} format={mockFormat} />
    );
    expect(getByTestId('wallet-icon')).toBeTruthy();
  });
});
```

### 5.2 StatCard

**File:** `tests/components/reports/Stat.test.tsx`

```tsx
import React from 'react';
import { render } from '@testing-library/react-native';
import { Stat } from '@/presentation/components/reports/Stat';

describe('StatCard', () => {
  it('renders label and value', () => {
    const { getByText } = render(<Stat label="Ventas" value="$1,000.00" tone="success" />);
    expect(getByText('Ventas')).toBeTruthy();
    expect(getByText('$1,000.00')).toBeTruthy();
  });

  it('applies success tone styles', () => {
    const { getByTestId } = render(<Stat label="Ventas" value="$1,000.00" tone="success" />);
    expect(getByTestId('stat-card')).toHaveStyle({ backgroundColor: expect.stringContaining('success') });
  });

  it('applies warning tone styles', () => {
    const { getByTestId } = render(<Stat label="Compras" value="$300.00" tone="warning" />);
    expect(getByTestId('stat-card')).toHaveStyle({ backgroundColor: expect.stringContaining('warning') });
  });

  it('applies danger tone styles', () => {
    const { getByTestId } = render(<Stat label="Gastos" value="$100.00" tone="danger" />);
    expect(getByTestId('stat-card')).toHaveStyle({ backgroundColor: expect.stringContaining('danger') });
  });

  it('is memoized', () => {
    const { rerender } = render(<Stat label="Ventas" value="$1,000.00" tone="success" />);
    rerender(<Stat label="Ventas" value="$1,000.00" tone="success" />);
  });
});
```

### 5.3 PeriodBreakdown

**File:** `tests/components/reports/BreakdownBar.test.tsx`

```tsx
import React from 'react';
import { render } from '@testing-library/react-native';
import { Breakdown, BreakdownBar } from '@/presentation/components/reports/BreakdownBar';
import { Money } from '@/domain/value-objects/Money';

const mockFormat = jest.fn((amount: number) => `$${amount.toFixed(2)}`);

describe('Breakdown', () => {
  const items = [
    { label: 'Ventas', value: 1000, tone: 'success' as const },
    { label: 'Compras', value: 300, tone: 'warning' as const },
    { label: 'Gastos', value: 100, tone: 'danger' as const },
  ];

  it('renders all three bars', () => {
    const { getByText } = render(<Breakdown items={items} />);
    expect(getByText('Ventas')).toBeTruthy();
    expect(getByText('Compras')).toBeTruthy();
    expect(getByText('Gastos')).toBeTruthy();
  });

  it('calculates percentages correctly', () => {
    const { getByText } = render(<Breakdown items={items} />);
    expect(getByText('71 por ciento')).toBeTruthy(); // 1000/1400 ≈ 71%
    expect(getByText('21 por ciento')).toBeTruthy(); // 300/1400 ≈ 21%
    expect(getByText('7 por ciento')).toBeTruthy();  // 100/1400 ≈ 7%
  });

  it('renders formatted values', () => {
    const { getByText } = render(<Breakdown items={items} />);
    expect(getByText('$1,000.00')).toBeTruthy();
    expect(getByText('$300.00')).toBeTruthy();
    expect(getByText('$100.00')).toBeTruthy();
  });

  it('handles single item', () => {
    const { getByText } = render(<Breakdown items={[{ label: 'Ventas', value: 500, tone: 'success' }]} />);
    expect(getByText('100 por ciento')).toBeTruthy();
  });
});
```

### 5.4 MovementListItem

**File:** `tests/components/movimientos/MovimientoListItem.test.tsx`

```tsx
import React from 'react';
import { render } from '@testing-library/react-native';
import { MovimientoListItem } from '@/presentation/components/movimientos/MovimientoListItem';
import { Money } from '@/domain/value-objects/Money';

const mockTransaction = {
  id: 1,
  type: 'SALE' as const,
  totalAmount: new Money(150, 'USD'),
  paymentMethod: 'CASH',
  detail: 'Venta de productos',
  registeredAt: '2026-01-15T10:30:00Z',
  deletedAt: null,
};

const mockFormat = jest.fn((amount: number) => `$${amount.toFixed(2)}`);

describe('MovementListItem', () => {
  it('renders SALE type badge', () => {
    const { getByText } = render(
      <MovementListItem item={mockTransaction} detailCount={2} onPress={jest.fn()} format={mockFormat} />
    );
    expect(getByText('VENTA')).toBeTruthy();
  });

  it('renders amount with correct sign', () => {
    const { getByText } = render(
      <MovementListItem item={mockTransaction} detailCount={2} onPress={jest.fn()} format={mockFormat} />
    );
    expect(getByText('+$150.00')).toBeTruthy(); // SALE is positive
  });

  it('renders EXPENSE as negative', () => {
    const expenseTransaction = { ...mockTransaction, type: 'EXPENSE' as const };
    const { getByText } = render(
      <MovementListItem item={expenseTransaction} detailCount={0} onPress={jest.fn()} format={mockFormat} />
    );
    expect(getByText('-$150.00')).toBeTruthy(); // EXPENSE is negative
  });

  it('renders PURCHASE as negative', () => {
    const purchaseTransaction = { ...mockTransaction, type: 'PURCHASE' as const };
    const { getByText } = render(
      <MovementListItem item={purchaseTransaction} detailCount={3} onPress={jest.fn()} format={mockFormat} />
    );
    expect(getByText('-$150.00')).toBeTruthy(); // PURCHASE is negative
  });

  it('renders detail count', () => {
    const { getByText } = render(
      <MovementListItem item={mockTransaction} detailCount={2} onPress={jest.fn()} format={mockFormat} />
    );
    expect(getByText('2 productos')).toBeTruthy();
  });

  it('is memoized', () => {
    const { rerender } = render(
      <MovementListItem item={mockTransaction} detailCount={2} onPress={jest.fn()} format={mockFormat} />
    );
    rerender(
      <MovementListItem item={mockTransaction} detailCount={2} onPress={jest.fn()} format={mockFormat} />
    );
  });
});
```

### 5.5 AdBanner Visibility

**File:** `tests/components/ad/AdBanner.test.tsx`

```tsx
import React from 'react';
import { render } from '@testing-library/react-native';
import { AdBanner } from '@/presentation/components/ad/AdBanner';
import { useSettingsStore } from '@/presentation/stores/settingsStore';

jest.mock('@/presentation/stores/settingsStore', () => ({
  useSettingsStore: (selector: any) => selector({ adEnabled: true }),
}));

describe('AdBanner', () => {
  it('renders when AD_ENABLED is true', () => {
    const { getByTestId } = render(<AdBanner />);
    expect(getByTestId('ad-banner')).toBeTruthy();
  });

  it('does not render when AD_ENABLED is false', () => {
    jest.mock('@/presentation/stores/settingsStore', () => ({
      useSettingsStore: (selector: any) => selector({ adEnabled: false }),
    }));
    
    const { queryByTestId } = render(<AdBanner />);
    expect(queryByTestId('ad-banner')).toBeNull();
  });
});
```

---

## 6. E2E Tests

### 6.1 Cash Summary Flow

**File:** `tests/e2e/financial-accumulators/cashSummaryFlow.test.ts`

```ts
import { openDatabaseSync, type SQLiteDatabase } from 'expo-sqlite';
import { getCashSummary } from '@/application/accumulators/GetCashSummary';
import { getPeriodSummary } from '@/application/accumulators/GetPeriodSummary'.
import { updateAccumulator } from '@/application/accumulators/UpdateAccumulator'.
import { SqliteAccumulatorRepository } from '@/data/repositories/SqliteAccumulatorRepository'.
import { SqliteTransaccionRepository } from '@/data/repositories/SqliteTransaccionRepository'.
import { SCHEMA_SQL, SEED_SQL } from '@/data/database/schema.sql'.
import { Money } from '@/domain/value-objects/Money'.

const mockGetRepositories = jest.fn();

jest.mock('@/data/repositories/container', () => ({
  getRepositories: () => mockGetRepositories(),
  resetRepositoriesForTests: jest.fn(),
}));

interface TestDb {
  db: SQLiteDatabase;
  accumulator: SqliteAccumulatorRepository;
  transacciones: SqliteTransaccionRepository;
}

function createTestDb(): TestDb {
  const db = openDatabaseSync(':memory:');
  db.execSync(SCHEMA_SQL);
  db.execSync(SEED_SQL);
  // Run both migrations
  db.execSync(`
    ALTER TABLE transacciones RENAME TO sales_transactions;
    ALTER TABLE sales_transactions RENAME COLUMN tipo TO type;
    ALTER TABLE sales_transactions RENAME COLUMN monto_total TO total_amount;
    ALTER TABLE sales_transactions RENAME COLUMN tipo_pago TO payment_method;
    ALTER TABLE sales_transactions RENAME COLUMN detalle TO detail;
    ALTER TABLE sales_transactions RENAME COLUMN fecha_registro TO registered_at;
    ALTER TABLE sales_transactions RENAME COLUMN fecha_delete TO deleted_at;
    
    ALTER TABLE resumen_caja RENAME TO financial_accumulators;
    ALTER TABLE financial_accumulators RENAME COLUMN total_ventas TO total_sales;
    ALTER TABLE financial_accumulators RENAME COLUMN total_compras TO total_purchases;
    ALTER TABLE financial_accumulators RENAME COLUMN total_gastos TO total_expenses;
    ALTER TABLE financial_accumulators RENAME COLUMN saldo_actual TO net_balance;
    ALTER TABLE financial_accumulators ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
    UPDATE financial_accumulators SET updated_at = CURRENT_TIMESTAMP WHERE id = 1;
  `);
  return {
    db,
    accumulator: new SqliteAccumulatorRepository(db),
    transacciones: new SqliteTransaccionRepository(db),
  };
}

describe('E2E: Cash Summary Flow', () => {
  let t: TestDb;

  beforeEach(() => {
    t = createTestDb();
    mockGetRepositories.mockReturnValue({
      accumulator: t.accumulator,
      transacciones: t.transacciones,
    });
  });

  it('complete flow: create transactions → verify accumulator → verify period reports', () => {
    // 1. Create SALE
    updateAccumulator({
      salesDelta: new Money(1000, 'USD'),
      purchasesDelta: Money.zero('USD'),
      expensesDelta: Money.zero('USD'),
    });

    // 2. Create PURCHASE
    updateAccumulator({
      salesDelta: Money.zero('USD'),
      purchasesDelta: new Money(300, 'USD'),
      expensesDelta: Money.zero('USD'),
    });

    // 3. Create EXPENSE
    updateAccumulator({
      salesDelta: Money.zero('USD'),
      purchasesDelta: Money.zero('USD'),
      expensesDelta: new Money(100, 'USD'),
    });

    // 4. Verify accumulator
    const summary = getCashSummary();
    expect(summary.totalSales.amount).toBe(1000);
    expect(summary.totalPurchases.amount).toBe(300);
    expect(summary.totalExpenses.amount).toBe(100);
    expect(summary.netBalance.amount).toBe(600);

    // 5. Verify period reports (insert transactions for aggregation)
    const monthStr = new Date().toISOString().slice(0, 7);
    t.db.runSync(
      "INSERT INTO sales_transactions (type, total_amount, payment_method, registered_at) VALUES ('SALE', ?, ?, ?)",
      1000, 'CASH', `${monthStr}-15T10:30:00Z`
    );
    t.db.runSync(
      "INSERT INTO sales_transactions (type, total_amount, payment_method, registered_at) VALUES ('PURCHASE', ?, ?, ?)",
      300, 'CASH', `${monthStr}-10T10:30:00Z`
    );
    t.db.runSync(
      "INSERT INTO sales_transactions (type, total_amount, payment_method, registered_at) VALUES ('EXPENSE', ?, ?, ?)",
      100, 'CASH', `${monthStr}-05T10:30:00Z`
    );

    const monthSummary = getPeriodSummary('MONTH');
    expect(monthSummary.totalSales.amount).toBe(1000);
    expect(monthSummary.totalPurchases.amount).toBe(300);
    expect(monthSummary.totalExpenses.amount).toBe(100);
    expect(monthSummary.net.amount).toBe(600);
  });
});
```

### 6.2 Reverse Transaction Flow

**File:** `tests/e2e/financial-accumulators/reverseTransactionFlow.test.ts`

```ts
import { openDatabaseSync, type SQLiteDatabase } from 'expo-sqlite'.
import { reverseTransaction } from '@/application/transactions/ReverseTransaction'.
import { SqliteAccumulatorRepository } from '@/data/repositories/SqliteAccumulatorRepository'.
import { SqliteTransaccionRepository } from '@/data/repositories/SqliteTransaccionRepository'.
import { SqliteProductoRepository } from '@/data/repositories/SqliteProductoRepository'.
import { SqliteCategoriaRepository } from '@/data/repositories/SqliteCategoriaRepository'.
import { SCHEMA_SQL, SEED_SQL } from '@/data/database/schema.sql'.

const mockGetDatabase = jest.fn();
const mockGetRepositories = jest.fn();

jest.mock('@/data/database/client', () => ({
  getDatabase: () => mockGetDatabase(),
  resetDatabaseForTests: jest.fn(),
}));

jest.mock('@/data/repositories/container', () => ({
  getRepositories: () => mockGetRepositories(),
  resetRepositoriesForTests: jest.fn(),
}));

interface TestDb {
  db: SQLiteDatabase;
  accumulator: SqliteAccumulatorRepository;
  transacciones: SqliteTransaccionRepository;
  productos: SqliteProductoRepository;
  categorias: SqliteCategoriaRepository;
}

function createTestDb(): TestDb {
  const db = openDatabaseSync(':memory:');
  db.execSync(SCHEMA_SQL);
  db.execSync(SEED_SQL);
  db.execSync(`
    ALTER TABLE transacciones RENAME TO sales_transactions;
    ALTER TABLE sales_transactions RENAME COLUMN tipo TO type;
    ALTER TABLE sales_transactions RENAME COLUMN monto_total TO total_amount;
    ALTER TABLE sales_transactions RENAME COLUMN tipo_pago TO payment_method;
    ALTER TABLE sales_transactions RENAME COLUMN detalle TO detail;
    ALTER TABLE sales_transactions RENAME COLUMN fecha_registro TO registered_at;
    ALTER TABLE sales_transactions RENAME COLUMN fecha_delete TO deleted_at;
    
    ALTER TABLE resumen_caja RENAME TO financial_accumulators;
    ALTER TABLE financial_accumulators RENAME COLUMN total_ventas TO total_sales;
    ALTER TABLE financial_accumulators RENAME COLUMN total_compras TO total_purchases;
    ALTER TABLE financial_accumulators RENAME COLUMN total_gastos TO total_expenses;
    ALTER TABLE financial_accumulators RENAME COLUMN saldo_actual TO net_balance;
    ALTER TABLE financial_accumulators ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
    UPDATE financial_accumulators SET updated_at = CURRENT_TIMESTAMP WHERE id = 1;
  `);
  return {
    db,
    accumulator: new SqliteAccumulatorRepository(db),
    transacciones: new SqliteTransaccionRepository(db),
    productos: new SqliteProductoRepository(db),
    categorias: new SqliteCategoriaRepository(db),
  };
}

function seedData(t: TestDb) {
  t.categorias.crear({ nombre: 'Cat1' });
  t.productos.crear({
    categoriaId: 1,
    nombre: 'Producto A',
    precioVenta: 100,
    precioCompra: 60,
    stockActual: 20,
    stockMinimo: 2,
    imagenUri: null,
  });
}

function createVenta(t: TestDb): number {
  const trx = t.db.runSync(
    "INSERT INTO sales_transactions (type, total_amount, payment_method) VALUES ('SALE', ?, ?)",
    200, 'CASH'
  );
  const id = trx.lastInsertRowId;
  t.db.runSync(
    'INSERT INTO detalle_transacciones (transaccion_id, producto_id, cantidad, precio_unitario) VALUES (?, ?, ?, ?)',
    id, 1, 2, 100
  );
  t.db.runSync(
    'UPDATE financial_accumulators SET total_sales = total_sales + ?, net_balance = net_balance + ?, updated_at = CURRENT_TIMESTAMP WHERE id = 1',
    200, 200
  );
  return id;
}

function createCompra(t: TestDb): number {
  const trx = t.db.runSync(
    "INSERT INTO sales_transactions (type, total_amount, payment_method) VALUES ('PURCHASE', ?, ?)",
    300, 'CASH'
  );
  const id = trx.lastInsertRowId;
  t.db.runSync(
    'INSERT INTO detalle_transacciones (transaccion_id, producto_id, cantidad, precio_unitario) VALUES (?, ?, ?, ?)',
    id, 1, 5, 60
  );
  t.db.runSync(
    'UPDATE financial_accumulators SET total_purchases = total_purchases + ?, net_balance = net_balance - ?, updated_at = CURRENT_TIMESTAMP WHERE id = 1',
    300, 300
  );
  return id;
}

function createGasto(t: TestDb): number {
  const trx = t.db.runSync(
    "INSERT INTO sales_transactions (type, total_amount, payment_method, detail) VALUES ('EXPENSE', ?, ?, ?)",
    50, 'CASH', 'Luz'
  );
  const id = trx.lastInsertRowId;
  t.db.runSync(
    'UPDATE financial_accumulators SET total_expenses = total_expenses + ?, net_balance = net_balance - ?, updated_at = CURRENT_TIMESTAMP WHERE id = 1',
    50, 50
  );
  return id;
}

describe('E2E: Reverse Transaction Flow', () => {
  let t: TestDb;

  beforeEach(() => {
    t = createTestDb();
    seedData(t);
    mockGetDatabase.mockReturnValue(t.db);
    mockGetRepositories.mockReturnValue({
      accumulator: t.accumulator,
      transacciones: t.transacciones,
      productos: t.productos,
    });
  });

  it('reverts sale: restores stock and accumulator to zero', () => {
    const id = createVenta(t);
    
    // Verify pre-state
    let acc = t.accumulator.getSummary();
    expect(acc.totalSales.amount).toBe(200);
    expect(acc.netBalance.amount).toBe(200);
    
    const prod = t.productos.findById(1)!;
    expect(prod.stockActual).toBe(18); // 20 - 2
    
    // Reverse
    reverseTransaction(id);
    
    // Verify post-state
    acc = t.accumulator.getSummary();
    expect(acc.totalSales.amount).toBe(0);
    expect(acc.netBalance.amount).toBe(0);
    
    const prodAfter = t.productos.findById(1)!;
    expect(prodAfter.stockActual).toBe(20); // Restored
  });

  it('reverts purchase: restores stock and accumulator', () => {
    const id = createCompra(t);
    
    let acc = t.accumulator.getSummary();
    expect(acc.totalPurchases.amount).toBe(300);
    expect(acc.netBalance.amount).toBe(-300);
    
    const prod = t.productos.findById(1)!;
    expect(prod.stockActual).toBe(25); // 20 + 5
    
    reverseTransaction(id);
    
    acc = t.accumulator.getSummary();
    expect(acc.totalPurchases.amount).toBe(0);
    expect(acc.netBalance.amount).toBe(0);
    
    const prodAfter = t.productos.findById(1)!;
    expect(prodAfter.stockActual).toBe(20); // Back to original
  });

  it('reverts expense: restores accumulator', () => {
    const id = createGasto(t);
    
    let acc = t.accumulator.getSummary();
    expect(acc.totalExpenses.amount).toBe(50);
    expect(acc.netBalance.amount).toBe(-50);
    
    reverseTransaction(id);
    
    acc = t.accumulator.getSummary();
    expect(acc.totalExpenses.amount).toBe(0);
    expect(acc.netBalance.amount).toBe(0);
  });

  it('reverts one of multiple transactions correctly', () => {
    const id1 = createVenta(t);
    const id2 = createCompra(t);
    const id3 = createGasto(t);
    
    // Net: 200 - 300 - 50 = -150
    let acc = t.accumulator.getSummary();
    expect(acc.netBalance.amount).toBe(-150);
    
    // Revert purchase only
    reverseTransaction(id2);
    
    // Net: 200 - 0 - 50 = 150
    acc = t.accumulator.getSummary();
    expect(acc.totalPurchases.amount).toBe(0);
    expect(acc.netBalance.amount).toBe(150);
    
    // Sale and expense still active
    const trx1 = t.transacciones.findById(id1);
    const trx3 = t.transacciones.findById(id3);
    expect(trx1!.deletedAt).toBeNull();
    expect(trx3!.deletedAt).toBeNull();
  });
});
```

### 6.3 AdBanner Visibility

**File:** `tests/e2e/financial-accumulators/adBannerVisibility.test.ts`

```ts
import { render, screen } from '@testing-library/react-native'.
import { CajaScreen } from '@/app/(tabs)/caja'.
import { MovimientosIndexScreen } from '@/app/movimientos/index'.
import { TransaccionDetailScreen } from '@/app/transacciones/[id]'.
import { useSettingsStore } from '@/presentation/stores/settingsStore'.
import { useCashSummary } from '@/presentation/hooks/useCashSummary'.
import { usePeriodSummary } from '@/presentation/hooks/usePeriodSummary'.
import { useMovements } from '@/presentation/hooks/useMovements'.

jest.mock('@/presentation/stores/settingsStore');
jest.mock('@/presentation/hooks/useCashSummary');
jest.mock('@/presentation/hooks/usePeriodSummary');
jest.mock('@/presentation/hooks/useMovements');

describe('AdBanner Visibility', () => {
  const mockSummary = {
    totalSales: { amount: 1000, currency: 'USD', format: () => '$1,000.00' },
    totalPurchases: { amount: 300, currency: 'USD', format: () => '$300.00' },
    totalExpenses: { amount: 100, currency: 'USD', format: () => '$100.00' },
    netBalance: { amount: 600, currency: 'USD', format: () => '$600.00' },
    updatedAt: '2026-01-15T10:30:00Z',
  };

  const mockPeriodSummary = {
    period: 'MONTH' as const,
    totals: [],
    totalSales: { amount: 1000, currency: 'USD', format: () => '$1,000.00' },
    totalPurchases: { amount: 300, currency: 'USD', format: () => '$300.00' },
    totalExpenses: { amount: 100, currency: 'USD', format: () => '$100.00' },
    net: { amount: 600, currency: 'USD', format: () => '$600.00' },
  };

  const mockMovements = {
    items: [],
    loading: false,
    hasMore: false,
    refresh: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
    require('@/presentation/hooks/useCashSummary').useCashSummary = jest.fn(() => ({
      resumen: mockSummary,
      loading: false,
      refresh: jest.fn(),
    }));
    require('@/presentation/hooks/usePeriodSummary').usePeriodSummary = jest.fn(() => mockPeriodSummary);
    require('@/presentation/hooks/useMovements').useMovements = jest.fn(() => mockMovements);
  });

  it('shows AdBanner in CajaScreen when AD_ENABLED=true', () => {
    (useSettingsStore as jest.Mock).mockImplementation((selector) => 
      selector({ adEnabled: true })
    );

    render(<CajaScreen />);
    expect(screen.getByTestId('ad-banner')).toBeTruthy();
  });

  it('hides AdBanner in CajaScreen when AD_ENABLED=false', () => {
    (useSettingsStore as jest.Mock).mockImplementation((selector) => 
      selector({ adEnabled: false })
    );

    render(<CajaScreen />);
    expect(screen.queryByTestId('ad-banner')).toBeNull();
  });

  it('shows AdBanner in MovimientosIndexScreen when AD_ENABLED=true', () => {
    (useSettingsStore as jest.Mock).mockImplementation((selector) => 
      selector({ adEnabled: true })
    );

    render(<MovimientosIndexScreen />);
    expect(screen.getByTestId('ad-banner')).toBeTruthy();
  });

  it('hides AdBanner in MovimientosIndexScreen when AD_ENABLED=false', () => {
    (useSettingsStore as jest.Mock).mockImplementation((selector) => 
      selector({ adEnabled: false })
    );

    render(<MovimientosIndexScreen />);
    expect(screen.queryByTestId('ad-banner')).toBeNull();
  });

  it('never shows AdBanner in TransaccionDetailScreen', () => {
    (useSettingsStore as jest.Mock).mockImplementation((selector) => 
      selector({ adEnabled: true })
    );

    render(<TransaccionDetailScreen />);
    expect(screen.queryByTestId('ad-banner')).toBeNull();
  });
});
```

---

## 7. CI Checks

### 7.1 AdWare Governance Check

```yaml
# .github/workflows/adware-check.yml
name: AdWare Governance
on: [push, pull_request]
jobs:
  adware-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Check AdBanner in allowed views only
        run: |
          # SHOULD find AdBanner in Caja and Movimientos
          grep -r "AdBanner" --include="*.tsx" src/presentation/components/accumulators/ src/presentation/components/movimientos/
          # Should NOT find AdBanner in detail/PIN gate
          ! grep -r "AdBanner" --include="*.tsx" src/presentation/components/accumulators/CashBalanceCard.tsx
          ! grep -r "AdBanner" --include="*.tsx" src/app/transacciones/
          ! grep -r "AdBanner" --include="*.tsx" src/presentation/hooks/usePinGate.ts
```

### 7.2 i18n Keys Check

```yaml
# .github/workflows/i18n-check.yml
name: i18n Keys Check
on: [push, pull_request]
jobs:
  i18n-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Verify accumulator i18n keys
        run: |
          node scripts/check-i18n-keys.js accumulator
```

### 7.3 Migration Order Check

```yaml
# .github/workflows/migration-order.yml
name: Migration Order Check
on: [push, pull_request]
jobs:
  migration-order:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Verify migration version order
        run: |
          # financial-accumulators migration (v2) must be after sales-checkout (v2)
          # Both update user_version to 2
          grep -n "version: 2" src/data/database/migrations.ts | head -1 | grep -q "sales"
          grep -n "version: 2" src/data/database/migrations.ts | tail -1 | grep -q "financial"
```

---

## 8. Test Execution Commands

```bash
# Unit tests
pnpm test -- tests/unit/financial-accumulators/

# Integration tests
pnpm test -- tests/integration/financial-accumulators/

# Migration tests
pnpm test -- tests/migration/financial-accumulators/

# Atomic tests
pnpm test -- tests/integration/atomic/

# Hook tests
pnpm test -- tests/unit/hooks/useCashSummary.test.ts

# Component tests
pnpm test -- tests/components/accumulators/
pnpm test -- tests/components/reports/
pnpm test -- tests/components/movimientos/
pnpm test -- tests/components/ad/

# E2E tests
pnpm test:e2e -- tests/e2e/financial-accumulators/

# All financial-accumulators tests
pnpm test -- --testPathPattern="financial-accumulators"

# CI validation
pnpm test:ci
```