# Financial Accumulators — Specification (v2.0.0 KMP)

## 1. Bounded Context

Responsible for the singleton cash accumulator (`financial_accumulators`), real-time balance tracking, period-based financial reports (month/year), and atomic transaction reversal with full cash/stock integrity. This module is the single source of truth for all financial totals and enforces zero-latency reads via the accumulator pattern.

---

## 2. Domain Entities & Value Objects

All identifiers, fields, and types in code and database are in **English**. Only visible UI strings remain in Spanish via i18n keys (`composeResources/values-es/strings.xml`).

### 2.1 FinancialSummary (Aggregate Root)

**File:** `commonMain/domain/entities/FinancialSummary.kt`

```kotlin
package com.tpvmovil.domain.entities

import com.tpvmovil.domain.valueobjects.Money

data class FinancialSummary(
    val totalSales: Money,
    val totalPurchases: Money,
    val totalExpenses: Money,
    val netBalance: Money,
    val updatedAt: String
)
```

### 2.2 PeriodSummary (Value Object)

**File:** `commonMain/domain/entities/PeriodSummary.kt`

```kotlin
package com.tpvmovil.domain.entities

import com.tpvmovil.domain.valueobjects.Money

enum class Period { MONTH, YEAR }

data class PeriodSummary(
    val period: Period,
    val totals: List<TypeTotal>,
    val totalSales: Money,
    val totalPurchases: Money,
    val totalExpenses: Money,
    val net: Money
)

data class TypeTotal(
    val type: TransactionType,
    val total: Money
)
```

### 2.3 TransactionType (Shared)

**File:** `commonMain/domain/entities/TransactionType.kt` — shared with `sales-checkout` and `expenses-cashflow` modules.

```kotlin
package com.tpvmovil.domain.entities

sealed class TransactionType {
    object SALE : TransactionType()
    object PURCHASE : TransactionType()
    object EXPENSE : TransactionType()
}

sealed class PaymentMethodCode {
    object CASH : PaymentMethodCode()
    object CARD : PaymentMethodCode()
    object TRANSFER : PaymentMethodCode()
    object QR : PaymentMethodCode()
}
```

### 2.4 Money / Currency (Shared Kernel)

**Files:** `commonMain/domain/valueobjects/Money.kt`, `Currency.kt` — see `system-settings/spec.md` section 2.1.

---

## 3. Domain Rules & Invariants

| ID | Rule | Description |
|---|---|---|
| RULE-FA-01 | Singleton accumulator | `financial_accumulators` table has exactly one row (`id = 1`, `CHECK (id = 1)`). Never deleted. |
| RULE-FA-02 | Single-row read | `getCashSummary()` reads only `SELECT ... FROM financial_accumulators WHERE id = 1`. No runtime aggregation. |
| RULE-FA-03 | Atomic reversal | `reverseTransaction` adjusts stock and cash within SQLDelight transaction block. Full rollback on any failure. |
| RULE-FA-04 | Stock safety on purchase reversal | If reversing a purchase would make `currentStock < 0`, throw `StockError` before opening transaction. |
| RULE-FA-05 | Already reversed check | If `deletedAt !== null`, throw error: "already reversed". |
| RULE-FA-06 | Net calculation | `net = totalSales - totalPurchases - totalExpenses` for both accumulator and period summaries. |
| RULE-FA-07 | Period filtering | Month: `strftime('%Y-%m', registered_at/1000, 'unixepoch', 'localtime') = strftime('%Y-%m', 'now', 'localtime')`. Year: `strftime('%Y', registered_at/1000, 'unixepoch', 'localtime') = strftime('%Y', 'now', 'localtime')`. |

Global invariants: **INV-01** (non-negative financial math), **INV-03** (atomic transaction block), **INV-04** (single payment method), **INV-06** (singleton id=1).

---

## 4. Database Contract (SQLDelight)

### 4.1 Target Schema (v2.0.0 — English Columns)

**File:** `commonMain/sqldelight/schema.sq` (accumulators section)

```sql
-- financial_accumulators (renamed from resumen_caja)
CREATE TABLE financial_accumulators (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    total_sales REAL DEFAULT 0.0,
    total_purchases REAL DEFAULT 0.0,
    total_expenses REAL DEFAULT 0.0,
    net_balance REAL DEFAULT 0.0,
    updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
);

INSERT OR IGNORE INTO financial_accumulators (id, total_sales, total_purchases, total_expenses, net_balance, updated_at)
VALUES (1, 0.0, 0.0, 0.0, 0.0, strftime('%s', 'now') * 1000);

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

**File:** `commonMain/sqldelight/migrations/v2_financial_accumulators.sqm`

```sql
-- v2_financial_accumulators.sqm
-- Atomic migration transaction (single transaction block)

-- Step 1: Rename table
ALTER TABLE resumen_caja RENAME TO financial_accumulators;

-- Step 2: Rename columns
ALTER TABLE financial_accumulators RENAME COLUMN total_ventas TO total_sales;
ALTER TABLE financial_accumulators RENAME COLUMN total_compras TO total_purchases;
ALTER TABLE financial_accumulators RENAME COLUMN total_gastos TO total_expenses;
ALTER TABLE financial_accumulators RENAME COLUMN saldo_actual TO net_balance;

-- Step 3: Add updated_at column
-- SQLite forbids non-constant defaults in ALTER TABLE ADD COLUMN, so the
-- default is NULL and existing rows are backfilled immediately after.
ALTER TABLE financial_accumulators ADD COLUMN updated_at INTEGER DEFAULT NULL;

-- Step 4: Update existing row's updated_at
UPDATE financial_accumulators SET updated_at = strftime('%s', 'now') * 1000 WHERE id = 1;

-- Step 5: Index for freshness checks
CREATE INDEX IF NOT EXISTS idx_financial_accumulators_updated_at
  ON financial_accumulators(updated_at);

-- Step 6: user_version = 2 is set centrally by DatabaseDriverFactory
```

> **Important:** This migration must run AFTER or in the same transaction as the `sales-checkout` migration (shared `user_version = 2`). Coordinate via `DatabaseDriverFactory` ordering.

> **Recovery:** databases from an earlier half-applied v2.0.0 attempt (table already renamed, columns still Spanish) are handled by a recovery variant `v2_financial_accumulators_columns.sqm`, which applies the column renames, `updated_at` backfill, and index without re-renaming the table.

---

## 5. Repository Interfaces

### 5.1 IAccumulatorRepository

**File:** `commonMain/data/repositories/IAccumulatorRepository.kt`

```kotlin
package com.tpvmovil.data.repositories

import com.tpvmovil.domain.entities.*
import com.tpvmovil.domain.valueobjects.Money

data class UpdateAccumulatorInput(
    val salesDelta: Money,
    val purchasesDelta: Money,
    val expensesDelta: Money
)

interface IAccumulatorRepository {
    fun getSummary(): FinancialSummary
    fun updateAccumulator(input: UpdateAccumulatorInput)
    fun getPeriodSummary(period: Period): PeriodSummary
}
```

---

## 6. Use Cases (Application Layer — `commonMain`)

### 6.1 GetCashSummary

**File:** `commonMain/application/accumulators/GetCashSummary.kt`

```kotlin
package com.tpvmovil.application.accumulators

import com.tpvmovil.domain.entities.FinancialSummary

suspend fun getCashSummary(): FinancialSummary
```

**Flow:** Direct read from `financial_accumulators` where `id = 1`. Returns `FinancialSummary` with `Money` value objects.

### 6.2 GetPeriodSummary

**File:** `commonMain/application/accumulators/GetPeriodSummary.kt`

```kotlin
package com.tpvmovil.application.accumulators

import com.tpvmovil.domain.entities.PeriodSummary

suspend fun getPeriodSummary(period: Period): PeriodSummary
```

**Flow:** Aggregates from `sales_transactions` by `type` for the given period. Returns `PeriodSummary` with `Money` values and computed `net`.

### 6.3 UpdateAccumulator (Internal)

**File:** `commonMain/application/accumulators/UpdateAccumulator.kt`

```kotlin
package com.tpvmovil.application.accumulators

import com.tpvmovil.domain.valueobjects.Money

data class UpdateAccumulatorInput(
    val salesDelta: Money,
    val purchasesDelta: Money,
    val expensesDelta: Money
)

suspend fun updateAccumulator(input: UpdateAccumulatorInput)
```

**Atomic Flow (SQLDelight transaction block):**
1. `UPDATE financial_accumulators SET total_sales = total_sales + salesDelta.amount, total_purchases = total_purchases + purchasesDelta.amount, total_expenses = total_expenses + expensesDelta.amount, net_balance = net_balance + salesDelta.amount - purchasesDelta.amount - expensesDelta.amount, updated_at = CURRENT_TIMESTAMP WHERE id = 1`.

> **Usage:** Called by `RegisterSale`, `RegisterPurchase`, `RegisterExpense`, and `ReverseTransaction` use cases.

### 6.4 ReverseTransaction (Unified)

**File:** `commonMain/application/transactions/ReverseTransaction.kt` — shared with `sales-checkout` and `expenses-cashflow`.

```kotlin
data class ReverseTransactionResult(
    val transactionId: TransactionId,
    val type: TransactionType
)

suspend fun reverseTransaction(id: TransactionId): ReverseTransactionResult
```

**Atomic Flow (SQLDelight transaction block):**
1. Find transaction by ID (must exist, `deleted_at IS NULL`).
2. If `type === SALE`:
   - For each detail: `UPDATE products SET current_stock = current_stock + quantity, updated_at = CURRENT_TIMESTAMP WHERE id = ?`
   - `UPDATE financial_accumulators SET total_sales = total_sales - amount, net_balance = net_balance - amount, updated_at = CURRENT_TIMESTAMP WHERE id = 1`
3. Else if `type === PURCHASE`:
   - Pre-check: For each detail, verify `product.currentStock - quantity >= 0` (throw `StockError` if fails)
   - For each detail: `UPDATE products SET current_stock = current_stock - quantity WHERE id = ?`
   - `UPDATE financial_accumulators SET total_purchases = total_purchases - amount, net_balance = net_balance + amount, updated_at = CURRENT_TIMESTAMP WHERE id = 1`
4. Else (`type === EXPENSE`):
   - `UPDATE financial_accumulators SET total_expenses = total_expenses - amount, net_balance = net_balance + amount, updated_at = CURRENT_TIMESTAMP WHERE id = 1`
5. `UPDATE sales_transactions SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?`

---

## 7. UI State Machines (Spanish visible text via `stringResource`)

### 7.1 Cash Summary Screen (`CashSummaryScreen`)

| State | Trigger | Next State | Visible Text (ES) |
|---|---|---|---|
| `Loading` | Mount | `Loaded` / `Error` | Skeleton cards |
| `Loaded` | Data received | — | Balance card (green/red/neutral), 3 Stat cards, Month/Year breakdown |
| `Refreshing` | Pull-to-refresh | `Loaded` | Spinner, 60s auto-refresh interval |

### 7.2 Transaction Detail / Reversal (`TransactionDetailScreen`)

| State | Trigger | Next State | Visible Text (ES) |
|---|---|---|---|
| `DetailLoaded` | Navigate to transaction | — | Amount, date, payment method, description, product lines |
| `PinGate` | Tap "Revertir" | `ConfirmDialog` | `"Ingrese su PIN"` |
| `ConfirmDialog` | PIN correct | `Reverting` | `"Esta acción restaurará el stock y ajustará la caja. No se puede deshacer."` |
| `Reverting` | Confirm tapped | `Success` / `Error` | Loading spinner |
| `Success` | Atomic commit | Back to list | Snackbar: `"Transacción revertida"` |
| `Error` | PIN wrong / StockError / DB error | `DetailLoaded` | Snackbar: `"PIN incorrecto"` / `"Stock insuficiente"` / `"No se pudo revertir"` |

### 7.3 Movements List (`MovementsListScreen`)

| State | Trigger | Next State | Visible Text (ES) |
|---|---|---|---|
| `ListLoading` | Mount / filter change | `ListLoaded` / `ListEmpty` | Skeleton list |
| `ListLoaded` | Data received | — | Paginated list with search, type filter chips, total badge |
| `ListEmpty` | No matching transactions | — | `"Sin movimientos"` + "Ir a Registro" action |

---

## 8. Compose UI Components

| Component | File | Type | Memoized | Description |
|---|---|---|---|---|
| `CashSummaryScreen` | `composeApp/.../accumulators/CashSummaryScreen.kt` | Screen | N/A | Main dashboard with balance, stats, breakdown |
| `CashBalanceCard` | `composeApp/.../accumulators/CashBalanceCard.kt` | Composable | No | Large balance display with tone (success/danger/neutral), icon |
| `StatCard` | `composeApp/.../reports/StatCard.kt` | Composable | `key = label` | Small stat: label, value, color tone (success/warning/danger/neutral) |
| `BreakdownBar` | `composeApp/.../reports/BreakdownBar.kt` | Composable | `key = period` | Horizontal bars for Sales/Purchases/Expenses with percentage |
| `ReportRow` | `composeApp/.../reports/ReportRow.kt` | Composable | `key = label` | Key-value row with tone, bold option |
| `MovementsListScreen` | `composeApp/.../movimientos/MovementsListScreen.kt` | Screen | N/A | List with search, filter chips, pagination |
| `MovementListItem` | `composeApp/.../movimientos/MovementListItem.kt` | Composable | `key = transaction.id` | Transaction row: type badge, description, date, amount, detail count |
| `TypeFilterChips` | `composeApp/.../movimientos/TypeFilterChips.kt` | Composable | No | Filter chips: TODOS / VENTA / COMPRA / GASTO |
| `SearchBar` | `composeApp/.../filters/SearchBar.kt` | Composable | No | Debounced search input |
| `AdBanner` | `composeApp/.../ads/AdBanner.kt` | Composable | No | **ALLOWED** in Caja and Movimientos footers (AD_ENABLED check) |

---

## 9. ViewModels

| ViewModel | File | Responsibility |
|---|---|---|
| `CashSummaryViewModel` | `commonMain/presentation/viewmodels/CashSummaryViewModel.kt` | Reads `FinancialSummary` via `GetCashSummary`, auto-refresh on invalidation |
| `PeriodSummaryViewModel` | `commonMain/presentation/viewmodels/PeriodSummaryViewModel.kt` | Reads `PeriodSummary` for MONTH/YEAR |
| `MovementsViewModel` | `commonMain/presentation/viewmodels/MovementsViewModel.kt` | Paginated, filterable, searchable transaction list |
| `PinGateViewModel` | `commonMain/presentation/viewmodels/PinGateViewModel.kt` | PIN verification gate for critical actions |

---

## 10. AdWare Governance

**Rule:** **Banner ads ARE ALLOWED** in financial summary (Caja) and reports (Movimientos) views.

**Implementation:**
- `AD_ENABLED` flag **respected** in `CashSummaryScreen` and `MovementsListScreen` → footer item renders `AdBanner` when enabled.
- Ads shown in passive browsing views; **NOT** in transaction detail, PIN gate, or confirmation dialogs.
- Monetizes dashboard/report views without interrupting critical flows.

---

## 11. Applicable NFRs

- **NFR-03:** 0ms cash calculations — read from `financial_accumulators` accumulator only.
- **NFR-04:** Pagination `LIMIT ? OFFSET ?` with `pageSize` from settings (default 15).
- **NFR-07:** Atomic rollback via SQLDelight transaction blocks on all accumulator updates and reversals.
- **NFR-08:** UI strings in Spanish via `stringResource`; code/DB/identifiers in English.
- **NFR-09:** Strict Kotlin (`explicit-api`, `-Xlint`).
- **NFR-10:** Tests: 100% pass unit + integration + migration + atomic + cash/stock integrity.

---

## 12. Extension Points (v2.0.0 Roadmap)

- [ ] **Forensic reports**: Include `deleted_at IS NOT NULL` for audit trails.
- [ ] **PDF/CSV export**: Period summary export for accounting.
- [ ] **Multi-currency accumulator**: Separate accumulators per currency with FX rates.
- [ ] **Projected balance**: Forecast based on recurring expenses/income.
- [ ] **Real-time sync**: WebSocket push for multi-device balance updates.

---

## 13. i18n Keys (Spanish UI Dictionary — `composeResources/values-es/strings.xml`)

```xml
<!-- Cash Summary (Caja) -->
<string name="accumulator_cash_title">Caja</string>
<string name="accumulator_cash_balance_label">Saldo actual</string>
<string name="accumulator_cash_positive">Positivo</string>
<string name="accumulator_cash_negative">Negativo</string>
<string name="accumulator_cash_neutral">Neutro</string>
<string name="accumulator_cash_sales">Ventas</string>
<string name="accumulator_cash_purchases">Compras</string>
<string name="accumulator_cash_expenses">Gastos</string>
<string name="accumulator_cash_month_title">Resumen del mes</string>
<string name="accumulator_cash_year_title">Resumen del año</string>
<string name="accumulator_cash_net_month">Neto del mes</string>
<string name="accumulator_cash_net_year">Neto del año</string>
<string name="accumulator_cash_pull_refresh">Actualizar</string>
<string name="accumulator_toast_refresh">Caja actualizada</string>

<!-- Movements (Movimientos) -->
<string name="accumulator_movements_title">Movimientos</string>
<string name="accumulator_movements_search_placeholder">Buscar en %1$s…</string>
<string name="accumulator_movements_filter_all">Todos</string>
<string name="accumulator_movements_filter_sale">Ventas</string>
<string name="accumulator_movements_filter_purchase">Compras</string>
<string name="accumulator_movements_filter_expense">Gastos</string>
<string name="accumulator_movements_total_badge">%1$d · página %2$d · lote %3$d</string>
<string name="accumulator_movements_empty_title">Sin movimientos</string>
<string name="accumulator_movements_empty_desc_search">No hay "%1$s" en %2$s.</string>
<string name="accumulator_movements_empty_desc_none">Aún no hay %1$s. Registra tu primera transacción en la pestaña Registro.</string>
<string name="accumulator_movements_empty_action">Ir a Registro</string>

<!-- Transaction Detail -->
<string name="accumulator_detail_title">Detalle de transacción</string>
<string name="accumulator_detail_type_venta">VENTA</string>
<string name="accumulator_detail_type_compra">COMPRA</string>
<string name="accumulator_detail_type_expense">GASTO</string>
<string name="accumulator_detail_amount">Monto: %1$s</string>
<string name="accumulator_detail_date">Fecha: %1$s</string>
<string name="accumulator_detail_payment">Pago: %1$s</string>
<string name="accumulator_detail_description">Descripción: %1$s</string>
<string name="accumulator_detail_products_header">Productos</string>
<string name="accumulator_detail_revert_button">Revertir transacción</string>

<!-- Reversal -->
<string name="accumulator_revert_pin_title">Ingrese su PIN</string>
<string name="accumulator_revert_confirm_title">Revertir</string>
<string name="accumulator_revert_confirm_message">Esta acción restaurará el stock y ajustará la caja. No se puede deshacer.</string>
<string name="accumulator_revert_success">Transacción revertida</string>
<string name="accumulator_revert_error_pin">PIN incorrecto</string>
<string name="accumulator_revert_error_stock">Stock insuficiente para %1$s. Disponible: %2$s</string>
<string name="accumulator_revert_error_generic">No se pudo revertir</string>
```

---

## 14. Cross-References

- `sales-checkout/spec.md` — `RegisterSale`, `RegisterPurchase`, `ReverseTransaction` (SALE/PURCHASE), `sales_transactions` table, atomic accumulator updates.
- `expenses-cashflow/spec.md` — `RegisterExpense`, `ReverseTransaction` (EXPENSE), unified `sales_transactions` type EXPENSE.
- `system-settings/spec.md` — Currency/Money VOs, pagination settings, `AD_ENABLED` flag, PIN/biometric config.
- `catalog-inventory/spec.md` — Product stock adjustments during reversal (`currentStock` updates).

---

*Last updated: 2026-11-12 — v2.0.0 KMP Specification*