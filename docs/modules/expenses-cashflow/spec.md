# Expenses & Cash Flow — Specification (v2.0.0)

## 1. Bounded Context

Responsible for the registration of operational cash outflows (expenses), expense categorization, confirmation dialogs, and their direct impact on the singleton financial accumulator (`financial_accumulators`). Expenses are modeled as a transaction type (`EXPENSE`) inside the unified `sales_transactions` table, sharing the atomic transaction infrastructure with Sales & Checkout and Financial Accumulators modules.

---

## 2. Domain Entities & Value Objects

All identifiers, fields, and types in code and database are in **English**. Only visible UI strings remain in Spanish via i18n keys.

### 2.1 Expense (Aggregate Root)

**File:** `src/domain/entities/Expense.ts`

```ts
import type { Money } from '../value-objects/Money';
import type { ExpenseCategoryId } from './ExpenseCategory';

export type ExpenseId = number;

export interface Expense {
  readonly id: ExpenseId;
  readonly categoryId: ExpenseCategoryId | null;
  readonly amount: Money;
  readonly description: string;
  readonly paymentMethod: 'CASH' | 'CARD' | 'TRANSFER' | 'QR';
  readonly registeredAt: string;   // ISO 8601
  readonly deletedAt: string | null;
}

// DTOs for use cases
export type CreateExpenseInput = {
  readonly categoryId: ExpenseCategoryId | null;
  readonly amount: number;         // amount in base currency, > 0
  readonly description: string;    // non-empty
  readonly paymentMethod: 'CASH' | 'CARD' | 'TRANSFER' | 'QR';
};

export type UpdateExpenseInput = Partial<CreateExpenseInput>;
```

### 2.2 ExpenseCategory (Aggregate Root)

**File:** `src/domain/entities/ExpenseCategory.ts`

```ts
export type ExpenseCategoryId = number;

export interface ExpenseCategory {
  readonly id: ExpenseCategoryId;
  readonly name: string;
  readonly registeredAt: string;
  readonly deletedAt: string | null;
}

export type CreateExpenseCategoryInput = {
  readonly name: string;
};
```

### 2.3 Money / Currency (Shared Kernel)

**Files:** `src/domain/value-objects/Money.ts`, `src/domain/value-objects/Currency.ts` — see `system-settings/spec.md` section 2.1.

---

## 3. Domain Rules & Invariants

| ID | Rule | Description |
|---|---|---|
| RULE-EC-01 | Amount valid | `amount > 0` and `Number.isFinite(amount)`. |
| RULE-EC-02 | Description required | `description.trim().length > 0`. |
| RULE-EC-03 | No product detail rows | `EXPENSE` type does **not** insert rows into `transaction_details`. |
| RULE-EC-04 | ConfirmDialog interpolated | Text: `"¿Está seguro de registrar este gasto por un monto de [Monto] [Divisa] con el detalle [Descripción]?"`. Options: **Cancelar** \| **Confirmar**. |
| RULE-EC-05 | Category optional | `categoryId` may be `null` (no mandatory category for expenses). |
| RULE-EC-06 | Single payment method | Exactly one `PaymentMethod` per expense. No mixed/split payments (INV-04). |
| RULE-EC-07 | Atomic transaction | Expense registration: insert `sales_transactions` (type='EXPENSE') + update `financial_accumulators` — all in `db.withTransactionSync`. |
| RULE-EC-08 | Rounded amounts | `round2(v) = Math.round(v * 100) / 100` on amount, totals. |

Global invariants: **INV-01** (non-negative financial math), **INV-03** (atomic `withTransactionSync`), **INV-04** (single payment method), **INV-10** (strict TypeScript).

---

## 4. Database Contract

### 4.1 Target Schema (v2.0.0 — English Columns)

```sql
-- sales_transactions (unified table — see sales-checkout/spec.md)
-- Expenses are stored here with type = 'EXPENSE'
-- No rows in transaction_details for expenses

-- financial_accumulators (renamed from resumen_caja — see financial-accumulators/spec.md)
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

-- expense_categories
CREATE TABLE IF NOT EXISTS expense_categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL
);

-- Partial indexes (deleted_at IS NULL)
CREATE INDEX IF NOT EXISTS idx_expense_categories_name ON expense_categories(name) WHERE deleted_at IS NULL;

-- Indexes for expense queries on sales_transactions
CREATE INDEX IF NOT EXISTS idx_sales_transactions_expense_date ON sales_transactions(registered_at) WHERE type = 'EXPENSE' AND deleted_at IS NULL;
```

### 4.2 Column Mapping: v1.0.0 (Spanish) → v2.0.0 (English)

The `transacciones` → `sales_transactions` table migration is handled by the `sales-checkout` module. Expenses are filtered by `type = 'EXPENSE'`.

| v1.0.0 Table | v1.0.0 Column | v2.0.0 Table | v2.0.0 Column |
|---|---|---|---|
| `transacciones` | `id` | `sales_transactions` | `id` |
| `transacciones` | `tipo` | `sales_transactions` | `type` |
| `transacciones` | `monto_total` | `sales_transactions` | `total_amount` |
| `transacciones` | `tipo_pago` | `sales_transactions` | `payment_method` |
| `transacciones` | `detalle` | `sales_transactions` | `detail` |
| `transacciones` | `fecha_registro` | `sales_transactions` | `registered_at` |
| `transacciones` | `fecha_delete` | `sales_transactions` | `deleted_at` |
| `resumen_caja` | `id` | `financial_accumulators` | `id` |
| `resumen_caja` | `total_ventas` | `financial_accumulators` | `total_sales` |
| `resumen_caja` | `total_compras` | `financial_accumulators` | `total_purchases` |
| `resumen_caja` | `total_gastos` | `financial_accumulators` | `total_expenses` |
| `resumen_caja` | `saldo_actual` | `financial_accumulators` | `net_balance` |
| *(new)* | — | `financial_accumulators` | `updated_at` |
| `categorias` (expense) | `id` | `expense_categories` | `id` |
| `categorias` (expense) | `nombre` | `expense_categories` | `name` |
| `categorias` (expense) | `fecha_registro` | `expense_categories` | `registered_at` |
| `categorias` (expense) | `fecha_delete` | `expense_categories` | `deleted_at` |

> **Note:** `expense_categories` is a new table in v2.0.0. v1.0.0 had no expense categorization.

### 4.3 Migration DDL (v1.0.0 → v2.0.0)

**File:** `src/data/database/migrations/v2_0_0_expenses_cashflow_migration.sql.ts`
(`EXPENSES_CASHFLOW_MIGRATION_SQL`, executed by `runMigrations` inside the shared v2 transaction)

```sql
-- Atomic migration transaction (single db.withTransactionSync)
-- NOTE: sales_transactions migration is handled by sales-checkout module.
-- This migration only handles expense_categories and financial_accumulators.

-- Step 1: Create expense_categories table (new in v2.0.0)
CREATE TABLE IF NOT EXISTS expense_categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL
);

-- Step 2: Create index for expense_categories
CREATE INDEX IF NOT EXISTS idx_expense_categories_name ON expense_categories(name) WHERE deleted_at IS NULL;

-- Step 3: Rename resumen_caja to financial_accumulators (coordinated with financial-accumulators module)
-- ALTER TABLE resumen_caja RENAME TO financial_accumulators;
-- ALTER TABLE financial_accumulators RENAME COLUMN total_ventas TO total_sales;
-- ALTER TABLE financial_accumulators RENAME COLUMN total_compras TO total_purchases;
-- ALTER TABLE financial_accumulators RENAME COLUMN total_gastos TO total_expenses;
-- ALTER TABLE financial_accumulators RENAME COLUMN saldo_actual TO net_balance;
-- ALTER TABLE financial_accumulators ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
-- UPDATE financial_accumulators SET updated_at = CURRENT_TIMESTAMP WHERE id = 1;

-- Step 4: Expense queries on sales_transactions are covered by the
-- idx_sales_transactions_type and idx_sales_transactions_registered_type
-- indexes created by the sales-checkout migration (stored type is 'GASTO').

-- Step 5: user_version = 2 (coordinated with sales-checkout and financial-accumulators)
-- user_version = 2 is set centrally by `runMigrations` after every v2.0.0
-- module step succeeds inside the same transaction.
```

> **Important:** The `resumen_caja` → `financial_accumulators` rename and column renames are coordinated with the `financial-accumulators` module migration. Both modules must agree on migration ordering in `migrations.ts`.

---

## 5. Repository Interfaces

### 5.1 IExpenseRepository

**File:** `src/data/repositories/IExpenseRepository.ts`

```ts
import type { Expense, ExpenseId, CreateExpenseInput } from '@/domain/entities/Expense';
import type { ExpenseCategory, ExpenseCategoryId, CreateExpenseCategoryInput } from '@/domain/entities/ExpenseCategory';

export interface ExpenseQuery {
  readonly period: 'MONTH' | 'YEAR';
  readonly categoryId?: ExpenseCategoryId;
  readonly search?: string;
  readonly limit: number;
  readonly offset: number;
}

export interface IExpenseRepository {
  // Expenses (queried from sales_transactions where type = 'EXPENSE')
  list(query: ExpenseQuery): ReadonlyArray<Expense>;
  findById(id: ExpenseId): Expense | null;
  create(input: CreateExpenseInput): Expense;
  update(id: ExpenseId, input: Partial<CreateExpenseInput>): Expense;
  softDelete(id: ExpenseId): void;  // sets deleted_at = CURRENT_TIMESTAMP

  // Expense Categories
  listCategories(): ReadonlyArray<ExpenseCategory>;
  findCategoryById(id: ExpenseCategoryId): ExpenseCategory | null;
  createCategory(input: CreateExpenseCategoryInput): ExpenseCategory;
  softDeleteCategory(id: ExpenseCategoryId): void;
}
```

---

## 6. Use Cases (Application Layer)

### 6.1 RegisterExpense

**File:** `src/application/expenses/RegisterExpense.ts`

```ts
import type { CreateExpenseInput } from '@/domain/entities/Expense';
import type { Expense } from '@/domain/entities/Expense';

export interface RegisterExpenseInput {
  readonly categoryId: number | null;
  readonly amount: number;
  readonly description: string;
  readonly paymentMethod: 'CASH' | 'CARD' | 'TRANSFER' | 'QR';
}

export interface RegisterExpenseResult {
  readonly expenseId: number;
}

export function registerExpense(input: RegisterExpenseInput): RegisterExpenseResult;
```

**Atomic Flow (`db.withTransactionSync`):**

1. Validate `amount > 0` and `description.trim().length > 0` (throw `ValidationError` if fails).
2. `INSERT INTO sales_transactions (type='EXPENSE', total_amount=amount, payment_method=paymentMethod, detail=description)` → `lastInsertRowId`.
3. `UPDATE financial_accumulators SET total_expenses = total_expenses + amount, net_balance = net_balance - amount, updated_at = CURRENT_TIMESTAMP WHERE id = 1`.
4. Return `{ expenseId: lastInsertRowId }`.

### 6.2 ReverseExpense (via ReverseTransaction)

**File:** `src/application/transactions/ReverseTransaction.ts` — shared with `sales-checkout` and `financial-accumulators`.

```ts
export interface ReverseTransactionResult {
  readonly transactionId: number;
  readonly type: 'SALE' | 'PURCHASE' | 'EXPENSE';
}

export function reverseTransaction(id: number): ReverseTransactionResult;
```

**Atomic Flow for EXPENSE type (`db.withTransactionSync`):**

1. Find transaction by ID (must exist, `type = 'EXPENSE'`, `deleted_at IS NULL`).
2. `UPDATE financial_accumulators SET total_expenses = total_expenses - amount, net_balance = net_balance + amount, updated_at = CURRENT_TIMESTAMP WHERE id = 1`.
3. `UPDATE sales_transactions SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?`.
4. Return `{ transactionId: id, type: 'EXPENSE' }`.

> **No stock changes** for expenses (RULE-EC-03).

---

## 7. Domain Rules (Expense Math)

**File:** `src/domain/rules/expenseRules.ts`

| Function | Signature | Description |
|---|---|---|
| `validateExpenseAmount` | `(amount: Money) => void` | Throws if the amount is not positive. |
| `validateExpenseDescription` | `(description: string) => void` | Throws if empty after trim. |

---

## 8. UI State Machines (Spanish visible text)

### 8.1 Expense Form (`app/gastos/nuevo.tsx`)

| State | Trigger | Next State | Visible Text (ES) |
|---|---|---|---|
| `FormEmpty` | Open form | `Editing` | `"Registrar gasto"` (nav title) |
| `Editing` | Amount & description valid | `ConfirmDialog` | `"¿Registrar gasto?"` (button) |
| `ConfirmDialog` | User taps Confirmar | `Registered` | `"¿Está seguro de registrar este gasto por un monto de [Monto] [Divisa] con el detalle [Descripción]?"` |
| `Registered` | ConfirmDialog accepted | `ExpenseList` | `"Gasto registrado"` (Toast) |
| `Editing` | User taps Cancelar | `FormEmpty` | `"Cancelar"` |

### 8.2 Expense List (`app/gastos/index.tsx`)

| State | Trigger | Next State | Visible Text (ES) |
|---|---|---|---|
| `ListLoading` | Mount / pull-to-refresh | `ListLoaded` / `ListEmpty` | Skeleton rows |
| `ListLoaded` | Data received | — | Paginated list, FAB to add |
| `ListEmpty` | No expenses this period | — | `"Sin gastos este mes"` + "Registrar gasto" action |
| `ListLoaded` | Tap expense row | `Detail` | Navigate to detail (read-only) |

---

## 9. UI Components

| Component | File | Type | Memoized | Description |
|---|---|---|---|---|
| `ConfirmDialog` | `src/presentation/components/feedback/ConfirmDialog.tsx` | UI | No | Reusable confirmation with interpolated text |
| `EmptyState` | `src/presentation/components/feedback/EmptyState.tsx` | UI | No | Illustration + title + description + action |
| `ExpenseListItem` | `src/presentation/components/expenses/ExpenseListItem.tsx` | UI | Yes | Row: category badge, amount, description, date |
| `CategorySelector` | `src/presentation/components/expenses/CategorySelector.tsx` | UI | No | Dropdown with `[+]` for quick category creation |
| `QuickCategoryModal` | `src/presentation/components/expenses/QuickCategoryModal.tsx` | UI | No | Native sheet: name input → creates category → auto-selects |
| `AdBanner` | `src/presentation/components/ads/AdBanner.tsx` | UI | No | **ALLOWED** in Expense list footer (AD_ENABLED check) |
| `SearchBar` | `src/presentation/components/filters/SearchBar.tsx` | UI | No | Debounce 200ms |
| `CategoryChipsRow` | `src/presentation/components/filters/CategoryChipsRow.tsx` | UI | No | Clean text chips for category filter |

---

## 10. Hooks

| Hook | File | Responsibility |
|---|---|---|
| `useExpenses` | `src/presentation/hooks/useExpenses.ts` | Paginated expense list (month/year filter), infinite scroll |
| `useExpenseCategories` | `src/presentation/hooks/useExpenseCategories.ts` | Active expense categories list + manual refresh |
| `useCurrency` | `src/presentation/hooks/useCurrency.ts` | `Intl.NumberFormat` formatter per `settingsStore.currency` |
| `useInfiniteScroll` | `src/presentation/hooks/useInfiniteScroll.ts` | `onEndReached` with threshold 0.5 for FlashList |

---

## 11. Zustand Stores

| Store | File | Responsibility |
|---|---|---|
| `expenseDraftStore` | `src/presentation/stores/expenseDraftStore.ts` | Expense form draft (survives `QuickCategoryModal`); includes cached `categories` |
| `settingsStore` | `src/presentation/stores/settingsStore.ts` | `currency`, `pageSize` (default 15), `biometricEnabled` |
| `invalidationStore` | `src/presentation/stores/invalidationStore.ts` | Manual invalidation TanStack Query style (`expenses`, `expenseCategories`) |

---

## 12. AdWare Governance

**Rule:** Banner ads **ARE ALLOWED** on the Expenses list screen (`app/gastos/index.tsx`).

**Implementation:**
- Check `AD_ENABLED` flag (configurable via remote config or build-time).
- If `true`, render `AdBanner` (AdMob wrapper) in FlashList `ListFooterComponent`, **below** expenses and **above** FAB.
- Do not interrupt scroll or card tactile feedback.
- Respect `useReducedMotion` for banner animations.
- **NOT** allowed in `app/gastos/nuevo.tsx` (form), `ConfirmDialog`, or any critical flow.

---

## 13. Applicable NFRs

- **NFR-01:** Offline-first SQLite JSI (`expo-sqlite`, `openDatabaseSync`, `withTransactionSync`).
- **NFR-02:** FlashList `numColumns={1}` (single column list), sub-50ms render, no `estimatedItemSize`.
- **NFR-03:** 0ms cash calculations — read from `financial_accumulators` accumulator only.
- **NFR-04:** Pagination `LIMIT ? OFFSET ?` with `pageSize` from settings (default 15).
- **NFR-07:** Atomic rollback via `withTransactionSync` on expense registration and reversal.
- **NFR-08:** UI strings in Spanish; code/DB/identifiers in English.
- **NFR-09:** Strict TypeScript (`strict`, `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes`).
- **NFR-10:** Tests: 100% pass unit + integration + migration + atomic + cash integrity.

---

## 14. Extension Points (v2.0.0 Roadmap)

- [ ] **Expense attachments**: Receipt photo capture (`expo-camera` + `ImageOptimizer`) linked to expense.
- [ ] **Recurring expenses**: Template with frequency (daily/weekly/monthly) + auto-registration.
- [ ] **Expense budgets**: Monthly budget per category with progress bars and alerts.
- [ ] **Expense reports**: PDF/CSV export with filters (date range, category, payment method).
- [ ] **Multi-currency expenses**: Separate accumulator per currency with FX rates.
- [ ] **Vendor management**: Payee/vendor entity linked to expenses for reporting.

---

## 15. i18n Keys (Spanish UI Dictionary)

| Key | Text (ES) | Context |
|---|---|---|
| `expenses.title` | Gastos | Main screen title (list) |
| `expenses.fab.label` | Registrar gasto | FAB accessibility |
| `expenses.empty.title` | Sin gastos este mes | EmptyState title |
| `expenses.empty.desc` | Aún no has registrado ningún gasto en el período actual. | EmptyState description |
| `expenses.empty.action` | Registrar gasto | EmptyState button |
| `expenses.list.search_placeholder` | Buscar en gastos… | SearchBar |
| `expenses.list.category_all` | Todas | CategoryChipsRow |
| `expenses.list.amount_prefix` | - | Negative amount prefix (outflow) |
| `expenses.form.title` | Registrar gasto | Nav title |
| `expenses.form.label.amount` | Monto | Input label |
| `expenses.form.label.description` | Descripción | Input label |
| `expenses.form.label.category` | Categoría | CategorySelector label |
| `expenses.form.label.payment_method` | Tipo de pago | Payment method selector |
| `expenses.form.placeholder.amount` | 0.00 | Amount input placeholder |
| `expenses.form.placeholder.description` | Ej. Material de oficina | Description placeholder |
| `expenses.form.helper.category_optional` | Opcional | Category helper |
| `expenses.form.category.new` | Nueva | [+] button CategorySelector |
| `expenses.modal.category.title` | Nueva categoría de gasto | QuickCategoryModal |
| `expenses.modal.category.label` | Nombre | Modal input |
| `expenses.modal.category.placeholder` | Ej. Transporte | Modal placeholder |
| `expenses.btn.register` | Registrar | Submit button |
| `expenses.btn.cancel` | Cancelar | Cancel button |
| `expenses.confirm.title` | ¿Registrar gasto? | ConfirmDialog title |
| `expenses.confirm.message` | ¿Está seguro de registrar este gasto por un monto de {amount} {currency} con el detalle "{description}"? | ConfirmDialog message (interpolated) |
| `expenses.confirm.cancel` | Cancelar | ConfirmDialog cancel |
| `expenses.confirm.confirm` | Confirmar | ConfirmDialog confirm |
| `expenses.toast.registered` | Gasto registrado | Toast success |
| `expenses.toast.error` | No se pudo registrar el gasto. | Toast error |
| `expenses.toast.deleted` | Gasto eliminado | Toast success (reversal) |
| `expenses.validation.amount_positive` | El monto debe ser mayor a 0. | Validation error |
| `expenses.validation.description_required` | La descripción es obligatoria. | Validation error |
| `expenses.detail.title` | Detalle de gasto | Detail screen title |
| `expenses.detail.amount` | Monto: {amount} | Amount display |
| `expenses.detail.category` | Categoría: {category} | Category display |
| `expenses.detail.payment` | Pago: {method} | Payment method display |
| `expenses.detail.date` | Fecha: {date} | Date display |
| `expenses.detail.description` | Descripción: {description} | Description display |

---

## 16. Cross-References

- `sales-checkout/spec.md` — Section 4 (Database Contract), 6 (Use Cases), 12 (NFRs), unified `sales_transactions` table.
- `financial-accumulators/spec.md` — Section 4 (financial_accumulators schema), 6.3 (UpdateAccumulator), 6.4 (ReverseTransaction EXPENSE path).
- `system-settings/spec.md` — Section 2.1 (Currency/Money), 3 (RULE-SS-05, RULE-SS-06), 10 (useCurrency, useSettingsPageSize, AD_ENABLED).
- `catalog-inventory/spec.md` — Section 6 (RULE-CI-03: fractional amounts in UI).

---

*Last updated: 2026-11-10 — v2.0.0 Specification*