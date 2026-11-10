# Financial Accumulators — Specification (v2.0.0)

## 1. Bounded Context

Responsible for the singleton cash accumulator (`financial_accumulators`), real-time balance tracking, period-based financial reports (month/year), and atomic transaction reversal with full cash/stock integrity. This module is the single source of truth for all financial totals and enforces zero-latency reads via the accumulator pattern.

---

## 2. Domain Entities & Value Objects

All identifiers, fields, and types in code and database are in **English**. Only visible UI strings remain in Spanish via i18n keys.

### 2.1 FinancialSummary (Aggregate Root)

**File:** `src/domain/entities/FinancialSummary.ts`

```ts
import type { Money } from '../value-objects/Money';

export interface FinancialSummary {
  readonly totalSales: Money;
  readonly totalPurchases: Money;
  readonly totalExpenses: Money;
  readonly netBalance: Money;
  readonly updatedAt: string;
}
```

### 2.2 PeriodSummary (Value Object)

**File:** `src/domain/entities/PeriodSummary.ts`

```ts
import type { Money } from '../value-objects/Money';
import type { TransactionType } from './Transaction';

export type Period = 'MONTH' | 'YEAR';

export interface PeriodSummary {
  readonly period: Period;
  readonly totals: ReadonlyArray<{ type: TransactionType; total: Money }>;
  readonly totalSales: Money;
  readonly totalPurchases: Money;
  readonly totalExpenses: Money;
  readonly net: Money;
}
```

### 2.3 TransactionType (Shared)

**File:** `src/domain/entities/Transaction.ts` — shared with `sales-checkout` and `expenses-cashflow` modules.

```ts
export type TransactionType = 'SALE' | 'PURCHASE' | 'EXPENSE';
export type PaymentMethodCode = 'CASH' | 'CARD' | 'TRANSFER' | 'QR';
```

### 2.4 Money / Currency (Shared Kernel)

**Files:** `src/domain/value-objects/Money.ts`, `src/domain/value-objects/Currency.ts` — see `system-settings/spec.md` section 2.1.

---

## 3. Domain Rules & Invariants

| ID | Rule | Description |
|---|---|---|
| RULE-FA-01 | Singleton accumulator | `financial_accumulators` table has exactly one row (`id = 1`, `CHECK (id = 1)`). Never deleted. |
| RULE-FA-02 | Single-row read | `getCashSummary()` reads only `SELECT ... FROM financial_accumulators WHERE id = 1`. No runtime aggregation. |
| RULE-FA-03 | Atomic reversal | `reverseTransaction` adjusts stock and cash within `db.withTransactionSync`. Full rollback on any failure. |
| RULE-FA-04 | Stock safety on purchase reversal | If reversing a purchase would make `currentStock < 0`, throw `StockError` before opening transaction. |
| RULE-FA-05 | Already reversed check | If `deletedAt !== null`, throw error: "already reversed". |
| RULE-FA-06 | Net calculation | `net = totalSales - totalPurchases - totalExpenses` for both accumulator and period summaries. |
| RULE-FA-07 | Period filtering | Month: `strftime('%Y-%m', registered_at) = strftime('%Y-%m', 'now', 'localtime')`. Year: `strftime('%Y', registered_at) = strftime('%Y', 'now', 'localtime')`. |

Global invariants: **INV-01** (non-negative financial math), **INV-03** (atomic `withTransactionSync`), **INV-04** (single payment method), **INV-06** (singleton id=1).

---

## 4. Database Contract

### 4.1 Target Schema (v2.0.0 — English Columns)

```sql
-- financial_accumulators (renamed from resumen_caja)
CREATE TABLE IF NOT EXISTS financial_accumulators (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    total_sales REAL DEFAULT 0.0,
    total_purchases REAL DEFAULT 0.0,
    total_expenses REAL DEFAULT 0.0,
    net_balance REAL DEFAULT 0.0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT OR IGNORE INTO financial_accumulators (id, total_sales, total_purchases, total_expenses, net_balance)
VALUES (1, 0.0, 0.0, 0.0, 0.0);

-- sales_transactions (unified table — see sales-checkout/spec.md)
-- Relevant indexes for period queries:
CREATE INDEX IF NOT EXISTS idx_sales_transactions_period
  ON sales_transactions(registered_at, type) WHERE deleted_at IS NULL;
```

### 4.2 Column Mapping: v1.0.0 (Spanish) → v2.0.0 (English)

| v1.0.0 Table | v1.0.0 Column | v2.0.0 Table | v2.0.0 Column |
|---|---|---|---|
| `resumen_caja` | `id` | `financial_accumulators` | `id` |
| `resumen_caja` | `total_ventas` | `financial_accumulators` | `total_sales` |
| `resumen_caja` | `total_compras` | `financial_accumulators` | `total_purchases` |
| `resumen_caja` | `total_gastos` | `financial_accumulators` | `total_expenses` |
| `resumen_caja` | `saldo_actual` | `financial_accumulators` | `net_balance` |
| *(new)* | — | `financial_accumulators` | `updated_at` |

> **Note:** The `transacciones` → `sales_transactions` migration is handled by the `sales-checkout` module.

### 4.3 Migration DDL (v1.0.0 → v2.0.0)

**File:** `src/data/database/migrations/v2_0_0_financial_accumulators_migration.sql`

```sql
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
```

> **Important:** This migration must run AFTER or in the same transaction as the `sales-checkout` migration (shared `user_version = 2`). Coordinate via `migrations.ts` ordering.

---

## 5. Repository Interfaces

### 5.1 IAccumulatorRepository

**File:** `src/data/repositories/IAccumulatorRepository.ts`

```ts
import type { FinancialSummary } from '@/domain/entities/FinancialSummary';
import type { PeriodSummary } from '@/domain/entities/PeriodSummary';
import type { Money } from '@/domain/value-objects/Money';
import type { TransactionType } from '@/domain/entities/Transaction';

export interface UpdateAccumulatorInput {
  readonly salesDelta: Money;
  readonly purchasesDelta: Money;
  readonly expensesDelta: Money;
}

export interface IAccumulatorRepository {
  getSummary(): FinancialSummary;
  updateAccumulator(input: UpdateAccumulatorInput): void;
  getPeriodSummary(period: 'MONTH' | 'YEAR'): PeriodSummary;
}
```

---

## 6. Use Cases (Application Layer)

### 6.1 GetCashSummary

**File:** `src/application/accumulators/GetCashSummary.ts`

```ts
import type { FinancialSummary } from '@/domain/entities/FinancialSummary';

export function getCashSummary(): FinancialSummary;
```

**Flow:** Direct read from `financial_accumulators` where `id = 1`. Returns `FinancialSummary` with `Money` value objects.

### 6.2 GetPeriodSummary

**File:** `src/application/accumulators/GetPeriodSummary.ts`

```ts
import type { PeriodSummary } from '@/domain/entities/PeriodSummary';

export function getPeriodSummary(period: 'MONTH' | 'YEAR'): PeriodSummary;
```

**Flow:** Aggregates from `sales_transactions` by `type` for the given period. Returns `PeriodSummary` with `Money` values and computed `net`.

### 6.3 UpdateAccumulator (Internal)

**File:** `src/application/accumulators/UpdateAccumulator.ts`

```ts
import type { Money } from '@/domain/value-objects/Money';

export interface UpdateAccumulatorInput {
  readonly salesDelta: Money;
  readonly purchasesDelta: Money;
  readonly expensesDelta: Money;
}

export function updateAccumulator(input: UpdateAccumulatorInput): void;
```

**Atomic Flow (`db.withTransactionSync`):**
1. `UPDATE financial_accumulators SET total_sales = total_sales + salesDelta, total_purchases = total_purchases + purchasesDelta, total_expenses = total_expenses + expensesDelta, net_balance = net_balance + salesDelta - purchasesDelta - expensesDelta, updated_at = CURRENT_TIMESTAMP WHERE id = 1`.

> **Usage:** Called by `RegisterSale`, `RegisterPurchase`, `RegisterExpense`, and `ReverseTransaction` use cases.

### 6.4 ReverseTransaction (Unified)

**File:** `src/application/transactions/ReverseTransaction.ts` — shared with `sales-checkout` and `expenses-cashflow`.

```ts
export interface ReverseTransactionResult {
  readonly transactionId: number;
  readonly type: 'SALE' | 'PURCHASE' | 'EXPENSE';
}

export function reverseTransaction(id: number): ReverseTransactionResult;
```

**Atomic Flow (`db.withTransactionSync`):**
1. Find transaction by ID (must exist, `deleted_at IS NULL`).
2. If `type === 'SALE'`:
   - For each detail: `UPDATE products SET current_stock = current_stock + quantity, updated_at = CURRENT_TIMESTAMP WHERE id = ?`
   - `UPDATE financial_accumulators SET total_sales = total_sales - amount, net_balance = net_balance - amount, updated_at = CURRENT_TIMESTAMP WHERE id = 1`
3. Else if `type === 'PURCHASE'`:
   - Pre-check: For each detail, verify `product.currentStock - quantity >= 0` (throw `StockError` if fails)
   - For each detail: `UPDATE products SET current_stock = current_stock - quantity WHERE id = ?`
   - `UPDATE financial_accumulators SET total_purchases = total_purchases - amount, net_balance = net_balance + amount, updated_at = CURRENT_TIMESTAMP WHERE id = 1`
4. Else (`type === 'EXPENSE'`):
   - `UPDATE financial_accumulators SET total_expenses = total_expenses - amount, net_balance = net_balance + amount, updated_at = CURRENT_TIMESTAMP WHERE id = 1`
5. `UPDATE sales_transactions SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?`

---

## 7. UI State Machines (Spanish visible text)

### 7.1 Cash Summary Screen (`app/(tabs)/caja.tsx`)

| State | Trigger | Next State | Visible Text (ES) |
|---|---|---|---|
| `Loading` | Mount | `Loaded` / `Error` | Skeleton cards |
| `Loaded` | Data received | — | Balance card (green/red/neutral), 3 Stat cards, Month/Year breakdown |
| `Refreshing` | Pull-to-refresh | `Loaded` | Spinner, 60s auto-refresh interval |

### 7.2 Transaction Detail / Reversal (`app/transacciones/[id].tsx`)

| State | Trigger | Next State | Visible Text (ES) |
|---|---|---|---|
| `DetailLoaded` | Navigate to transaction | — | Amount, date, payment method, description, product lines |
| `PinGate` | Tap "Revertir" | `ConfirmDialog` | `"Ingrese su PIN"` |
| `ConfirmDialog` | PIN correct | `Reverting` | `"Esta acción restaurará el stock y ajustará la caja. No se puede deshacer."` |
| `Reverting` | Confirm tapped | `Success` / `Error` | Loading spinner |
| `Success` | Atomic commit | Back to list | Toast: `"Transacción revertida"` |
| `Error` | PIN wrong / StockError / DB error | `DetailLoaded` | Toast: `"PIN incorrecto"` / `"Stock insuficiente"` / `"No se pudo revertir"` |

### 7.3 Movements List (`app/movimientos/index.tsx`)

| State | Trigger | Next State | Visible Text (ES) |
|---|---|---|---|
| `ListLoading` | Mount / filter change | `ListLoaded` / `ListEmpty` | Skeleton list |
| `ListLoaded` | Data received | — | Paginated list with search, type filter chips, total badge |
| `ListEmpty` | No matching transactions | — | `"Sin movimientos"` + "Ir a Registro" action |

---

## 8. UI Components

| Component | File | Type | Memoized | Description |
|---|---|---|---|---|
| `CashBalanceCard` | `src/presentation/components/accumulators/CashBalanceCard.tsx` | UI | No | Large balance display with tone (success/danger/neutral), icon |
| `StatCard` | `src/presentation/components/reports/Stat.tsx` | UI | Yes | Small stat: label, value, color tone (success/warning/danger/neutral) |
| `PeriodBreakdown` | `src/presentation/components/reports/BreakdownBar.tsx` | UI | Yes | Horizontal bars for Sales/Purchases/Expenses with percentage |
| `ReportRow` | `src/presentation/components/reports/ReportRow.tsx` | UI | Yes | Key-value row with tone, bold option |
| `MovementListItem` | `src/presentation/components/movimientos/MovimientoListItem.tsx` | UI | Yes | Transaction row: type badge, description, date, amount, detail count |
| `TypeFilterChips` | `src/presentation/components/movimientos/TipoFilterChips.tsx` | UI | No | Filter chips: TODOS / VENTA / COMPRA / GASTO |
| `SearchBar` | `src/presentation/components/filters/SearchBar.tsx` | UI | No | Debounced search input |
| `AdBanner` | `src/presentation/components/ad/AdBanner.tsx` | UI | No | **ALLOWED** in Caja and Movimientos footers (AD_ENABLED check) |

---

## 9. Hooks

| Hook | File | Responsibility |
|---|---|---|
| `useCashSummary` | `src/presentation/hooks/useCashSummary.ts` | Reads `FinancialSummary` via `GetCashSummary`, auto-refresh on invalidation |
| `usePeriodSummary` | `src/presentation/hooks/usePeriodSummary.ts` | Reads `PeriodSummary` for MONTH/YEAR |
| `useMovements` | `src/presentation/hooks/useMovements.ts` | Paginated, filterable, searchable transaction list |
| `usePinGate` | `src/presentation/hooks/usePinGate.ts` | PIN verification gate for critical actions |

---

## 10. Zustand Stores

| Store | File | Responsibility |
|---|---|---|
| `sessionStore` | `src/presentation/stores/sessionStore.ts` | `unlocked: boolean` — auth state for PIN/biometric |
| `invalidationStore` | `src/presentation/stores/invalidationStore.ts` | Manual cache invalidation versions (TanStack Query style) |

---

## 11. AdWare Governance

**Rule:** **Banner ads ARE ALLOWED** in financial summary (Caja) and reports (Movimientos) views.

**Implementation:**
- `AD_ENABLED` flag **respected** in `CajaScreen` and `MovimientosIndexScreen` → `ListFooterComponent` renders `AdBanner` when enabled.
- Ads shown in passive browsing views; **NOT** in transaction detail, PIN gate, or confirmation dialogs.
- Monetizes dashboard/report views without interrupting critical flows.

---

## 12. Applicable NFRs

- **NFR-03:** 0ms cash calculations — read from `financial_accumulators` accumulator only.
- **NFR-04:** Pagination `LIMIT ? OFFSET ?` with `pageSize` from settings (default 15).
- **NFR-07:** Atomic rollback via `withTransactionSync` on all accumulator updates and reversals.
- **NFR-08:** UI strings in Spanish; code/DB/identifiers in English.
- **NFR-09:** Strict TypeScript (`strict`, `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes`).
- **NFR-10:** Tests: 100% pass unit + integration + migration + atomic + cash/stock integrity.

---

## 13. Extension Points (v2.0.0 Roadmap)

- [ ] **Forensic reports**: Include `deleted_at IS NOT NULL` for audit trails.
- [ ] **PDF/CSV export**: Period summary export for accounting.
- [ ] **Multi-currency accumulator**: Separate accumulators per currency with FX rates.
- [ ] **Projected balance**: Forecast based on recurring expenses/income.
- [ ] **Real-time sync**: WebSocket push for multi-device balance updates.

---

## 14. i18n Keys (Spanish UI Dictionary)

| Key | Text (ES) | Context |
|---|---|---|
| `accumulator.cash.title` | Caja | Screen title |
| `accumulator.cash.balance_label` | Saldo actual | Balance card label |
| `accumulator.cash.positive` | Positivo | Balance > 0 tone |
| `accumulator.cash.negative` | Negativo | Balance < 0 tone |
| `accumulator.cash.neutral` | Neutro | Balance = 0 tone |
| `accumulator.cash.sales` | Ventas | Stat label |
| `accumulator.cash.purchases` | Compras | Stat label |
| `accumulator.cash.expenses` | Gastos | Stat label |
| `accumulator.cash.month_title` | Resumen del mes | Month card title |
| `accumulator.cash.year_title` | Resumen del año | Year card title |
| `accumulator.cash.net_month` | Neto del mes | Month net row |
| `accumulator.cash.net_year` | Neto del año | Year net row |
| `accumulator.cash.pull_refresh` | Actualizar | Pull-to-refresh |
| `accumulator.movements.title` | Movimientos | Screen title |
| `accumulator.movements.search_placeholder` | Buscar en {filter}… | Search input |
| `accumulator.movements.filter_all` | Todos | Filter chip |
| `accumulator.movements.filter_sale` | Ventas | Filter chip |
| `accumulator.movements.filter_purchase` | Compras | Filter chip |
| `accumulator.movements.filter_expense` | Gastos | Filter chip |
| `accumulator.movements.total_badge` | {count} · página {page} · lote {pageSize} | List header badge |
| `accumulator.movements.empty_title` | Sin movimientos | EmptyState title |
| `accumulator.movements.empty_desc_search` | No hay "{search}" en {filter}. | EmptyState with search |
| `accumulator.movements.empty_desc_none` | Aún no hay {filter}. Registra tu primera transacción en la pestaña Registro. | EmptyState no results |
| `accumulator.movements.empty_action` | Ir a Registro | EmptyState action |
| `accumulator.detail.title` | Detalle de transacción | Detail screen title |
| `accumulator.detail.type_venta` | VENTA | Type badge |
| `accumulator.detail.type_compra` | COMPRA | Type badge |
| `accumulator.detail.type_expense` | GASTO | Type badge |
| `accumulator.detail.amount` | Monto: {amount} | Amount display |
| `accumulator.detail.date` | Fecha: {date} | Date display |
| `accumulator.detail.payment` | Pago: {method} | Payment method display |
| `accumulator.detail.description` | Descripción: {description} | Description display |
| `accumulator.detail.products_header` | Productos | Products section header |
| `accumulator.detail.revert_button` | Revertir transacción | Revert button |
| `accumulator.revert.pin_title` | Ingrese su PIN | PIN gate title |
| `accumulator.revert.confirm_title` | Revertir | Confirm title |
| `accumulator.revert.confirm_message` | Esta acción restaurará el stock y ajustará la caja. No se puede deshacer. | Confirm message |
| `accumulator.revert.success` | Transacción revertida | Success toast |
| `accumulator.revert.error_pin` | PIN incorrecto | PIN error toast |
| `accumulator.revert.error_stock` | Stock insuficiente para {product}. Disponible: {stock} | Stock error toast |
| `accumulator.revert.error_generic` | No se pudo revertir | Generic error toast |
| `accumulator.toast.refresh` | Caja actualizada | Refresh toast |

---

## 15. Cross-References

- `sales-checkout/spec.md` — `RegisterSale`, `RegisterPurchase`, `ReverseTransaction` (SALE/PURCHASE), `sales_transactions` table, atomic accumulator updates.
- `expenses-cashflow/spec.md` — `RegisterExpense`, `ReverseTransaction` (EXPENSE), unified `sales_transactions` type EXPENSE.
- `system-settings/spec.md` — Currency/Money VOs, pagination settings, `AD_ENABLED` flag, PIN/biometric config.
- `catalog-inventory/spec.md` — Product stock adjustments during reversal (`currentStock` updates).

---

*Last updated: 2026-11-10 — v2.0.0 Specification*