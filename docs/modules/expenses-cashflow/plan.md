# Expenses & Cash Flow — Implementation Plan (v2.0.0)

> **Status (2026-11-11):** Phase 2 (atomic migration + repository/use-case SQL foundation) is complete and tested. Domain entities and presentation work remain pending.

## Overview

This plan covers the implementation of the Expenses & Cash Flow module v2.0.0, including:
- Domain layer: English entities (`Expense`, `ExpenseCategory`), Value Objects, rules
- Data layer: Repository interfaces, mappers, migration coordination
- Application layer: `RegisterExpense`, integration with `ReverseTransaction`
- Presentation layer: Stores, hooks, components, AdWare governance
- Testing: Unit, integration, migration, atomic, cash integrity

---

## Phase 1: Database Migration (Critical Path)

### 1.1 Migration Runner Update
**File:** `src/data/database/migrations.ts`
- Register the expenses step in the version-2 group (all v2 modules share one `withTransactionSync`)
- `user_version = 2` is set centrally by `runMigrations` after every v2 step succeeds
- Migration order: catalog-inventory → sales-checkout → financial-accumulators → expenses-cashflow → system-settings

### 1.2 Migration SQL
**File:** `src/data/database/migrations/v2_0_0_expenses_cashflow_migration.sql.ts` (`EXPENSES_CASHFLOW_MIGRATION_SQL`)

```sql
-- Runs inside the shared v2.0.0 transaction (single db.withTransactionSync)
-- NOTE: sales_transactions and financial_accumulators are migrated by their own modules.

-- Step 1: Create expense_categories table (new in v2.0.0)
CREATE TABLE IF NOT EXISTS expense_categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL
);

-- Step 2: Create index for expense_categories
CREATE INDEX IF NOT EXISTS idx_expense_categories_name
  ON expense_categories(name) WHERE deleted_at IS NULL;

-- Step 3: user_version = 2 is set centrally by `runMigrations`
```

> **Note:** Expenses are still stored in `sales_transactions` with the legacy
> stored value `'GASTO'`. No dedicated `type = 'EXPENSE'` index is created;
> expense queries are covered by the composite indexes from `sales-checkout`.

### 1.3 Migration Checklist
- [x] Register the expenses step in `migrations.ts` v2 group
- [x] Write `v2_0_0_expenses_cashflow_migration.sql.ts` with atomic transaction
- [x] Create `expense_categories` table with index
- [x] `financial_accumulators` rename owned by `financial-accumulators` module (no duplication)
- [x] Expense queries covered by composite `sales_transactions` indexes
- [x] `user_version` = 2 (centralized)
- [x] Verify row counts match pre-migration for expenses
- [ ] Test restore from backup scenario

---

## Phase 2: Domain Layer

### 2.1 Entity Files
| New File | Purpose |
|---|---|
| `src/domain/entities/Expense.ts` | Expense aggregate root with `CreateExpenseInput`, `UpdateExpenseInput` |
| `src/domain/entities/ExpenseCategory.ts` | ExpenseCategory aggregate root with `CreateExpenseCategoryInput` |

### 2.2 Domain Rules
**File:** `src/domain/rules/expenseRules.ts`
- `validateExpenseAmount(amount: number): void` — throws if `amount <= 0` or not finite
- `validateExpenseDescription(description: string): void` — throws if empty after trim
- `roundAmount(amount: number): number` — `Math.round(amount * 100) / 100` (2dp)

### 2.3 Shared Value Objects
- `Money.ts`, `Currency.ts` — from `system-settings/spec.md` section 2.1
- `PaymentMethod` — from `sales-checkout/spec.md` section 2.4

---

## Phase 3: Data Layer

### 3.1 Repository Interface
**File:** `src/data/repositories/IExpenseRepository.ts`
- English interface per spec.md section 5.1
- Methods: `list`, `findById`, `create`, `update`, `softDelete`, `listCategories`, `findCategoryById`, `createCategory`, `softDeleteCategory`

### 3.2 Repository Implementation
**File:** `src/data/repositories/SqliteExpenseRepository.ts`
- Implements `IExpenseRepository`
- Queries `sales_transactions` filtered by `type = 'EXPENSE'`
- Maps Spanish → English columns via `rowMappers.ts`
- Uses `withTransactionSync` for create/update/softDelete

### 3.3 Mappers
**File:** `src/data/mappers/rowMappers.ts`
- `mapExpenseRow` — `sales_transactions` row → `Expense` entity
- `mapExpenseCategoryRow` — `expense_categories` row → `ExpenseCategory` entity

### 3.4 Container Registration
**File:** `src/data/repositories/container.ts`
- Register `IExpenseRepository` → `SqliteExpenseRepository`

---

## Phase 4: Application Layer

### 4.1 RegisterExpense
**File:** `src/application/expenses/RegisterExpense.ts`
- Input: `categoryId`, `amount`, `description`, `paymentMethod`
- Atomic `withTransactionSync` flow:
  1. Validate `amount > 0`, `description.trim().length > 0`
  2. `INSERT INTO sales_transactions (type='EXPENSE', total_amount, payment_method, detail)` → `lastInsertRowId`
  3. `UPDATE financial_accumulators SET total_expenses += amount, net_balance -= amount, updated_at = CURRENT_TIMESTAMP WHERE id = 1`
  4. Return `{ expenseId }`

### 4.2 ReverseExpense (via ReverseTransaction)
**File:** `src/application/transactions/ReverseTransaction.ts` — shared
- Handles `EXPENSE` type:
  1. Find transaction (must exist, `type = 'EXPENSE'`, `deleted_at IS NULL`)
  2. `UPDATE financial_accumulators SET total_expenses -= amount, net_balance += amount, updated_at = CURRENT_TIMESTAMP WHERE id = 1`
  3. `UPDATE sales_transactions SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?`
  4. Return `{ transactionId, type: 'EXPENSE' }`
- **No stock changes** (RULE-EC-03)

---

## Phase 5: Presentation Layer

### 5.1 Zustand Stores
| Store | File | Changes |
|---|---|---|
| `expenseDraftStore.ts` | `src/presentation/stores/expenseDraftStore.ts` | New store: form draft (amount, description, categoryId, paymentMethod), cached categories |
| `settingsStore.ts` | `src/presentation/stores/settingsStore.ts` | Existing: `currency`, `pageSize`, `biometricEnabled` |
| `invalidationStore.ts` | `src/presentation/stores/invalidationStore.ts` | Existing: manual invalidation for `expenses`, `expenseCategories` |

### 5.2 Hooks
| Hook | File | Changes |
|---|---|---|
| `useExpenses` | `src/presentation/hooks/useExpenses.ts` | Paginated list (month/year filter), infinite scroll, search, category filter |
| `useExpenseCategories` | `src/presentation/hooks/useExpenseCategories.ts` | Active categories list + manual refresh |
| `useCurrency` | `src/presentation/hooks/useCurrency.ts` | Existing: `Intl.NumberFormat` per `settingsStore.currency` |
| `useInfiniteScroll` | `src/presentation/hooks/useInfiniteScroll.ts` | Existing: `onEndReached` threshold 0.5 |

### 5.3 Components
| Component | File | Changes |
|---|---|---|
| `ConfirmDialog` | `src/presentation/components/feedback/ConfirmDialog.tsx` | Reusable, interpolated text |
| `EmptyState` | `src/presentation/components/feedback/EmptyState.tsx` | Reusable |
| `ExpenseListItem` | `src/presentation/components/expenses/ExpenseListItem.tsx` | New: memoized row (category badge, amount, description, date) |
| `CategorySelector` | `src/presentation/components/expenses/CategorySelector.tsx` | New: dropdown + `[+]` for `QuickCategoryModal` |
| `QuickCategoryModal` | `src/presentation/components/expenses/QuickCategoryModal.tsx` | New: native sheet, creates category, invalidates cache, auto-selects |
| `AdBanner` | `src/presentation/components/ads/AdBanner.tsx` | **ALLOWED** in list footer (`AD_ENABLED` check) |
| `SearchBar` | `src/presentation/components/filters/SearchBar.tsx` | Reusable, debounce 200ms |
| `CategoryChipsRow` | `src/presentation/components/filters/CategoryChipsRow.tsx` | Reusable, clean text chips |

### 5.4 AdWare Integration
**Files to modify:**
- `app/gastos/index.tsx` — Add `AdBanner` in FlashList `ListFooterComponent` when `AD_ENABLED`
- `app/gastos/nuevo.tsx` — **No ads** (critical flow)

**Rule:** `AD_ENABLED` flag respected in list; ignored in form, ConfirmDialog, PIN gate.

### 5.5 i18n
**File:** `src/presentation/i18n/es/expenses.json`
- All 45+ keys from spec.md section 15
- Namespace: `expenses`

---

## Phase 6: Migration Verification

### 6.1 Data Integrity Checks
- Row counts: `sales_transactions` (type='EXPENSE') preserved
- PKs preserved (id values unchanged)
- Timestamps preserved (`registered_at`, `deleted_at`)
- Soft deletes preserved (`deleted_at NOT NULL` rows)
- `financial_accumulators` accumulator unchanged (coordinated)
- `expense_categories` table created empty (new in v2.0.0)

### 6.2 Functional Verification
- Create EXPENSE → verify `total_expenses += amount`, `net_balance -= amount`
- Reverse EXPENSE → verify `total_expenses -= amount`, `net_balance += amount`
- Verify no `transaction_details` rows for expenses
- Verify category creation and selection works
- ConfirmDialog shows interpolated text correctly

---

## Phase 7: Testing

### 7.1 Unit Tests
**Directory:** `tests/unit/expenses-cashflow/`
- `expenseRules.test.ts` — All 3 functions with edge cases
- `Expense.test.ts` — Entity creation, validation
- `ExpenseCategory.test.ts` — Entity creation, validation

### 7.2 Integration Tests
**Directory:** `tests/integration/expenses-cashflow/`
- `RegisterExpense.test.ts` — Success, validation errors, atomic rollback, cash flow impact
- `ReverseExpense.test.ts` — Reversal restores balance, soft delete, no stock changes

### 7.3 Migration Tests
**Directory:** `tests/migration/expenses-cashflow/`
- `v2_0_0_migration.test.ts` — Seed v1 schema, run migration, assert mappings

### 7.4 Atomic Transaction Tests
**Directory:** `tests/integration/atomic/`
- `expenseAtomicRollback.test.ts` — Simulate DB failure mid-transaction → full rollback

### 7.5 Hook Tests
**Directory:** `tests/unit/hooks/`
- `useExpenses.test.ts` — Pagination, filtering, infinite scroll
- `useExpenseCategories.test.ts` — Category list, refresh

### 7.6 UI Component Tests
**Directory:** `tests/components/expenses/`
- `ExpenseListItem.test.tsx` — Render expense data, category badge
- `CategorySelector.test.tsx` — Dropdown, quick category creation
- `QuickCategoryModal.test.tsx` — Create category, auto-select, preserve draft

### 7.7 E2E Tests
**Directory:** `tests/e2e/expenses-cashflow/`
- `expenseFlow.test.ts` — Add expense → confirm → verify list → verify cash summary
- `expenseReversal.test.ts` — Reverse expense → verify balance restored
- `categoryFlow.test.ts` — Create category via modal → select → create expense
- `migrationIdempotency.test.ts` — Run migration twice → no errors

---

## Dependencies & Ordering

```
Phase 1 (Migration) → Phase 2 (Domain) → Phase 3 (Data) → Phase 4 (Application) → Phase 5 (Presentation) → Phase 6 (Verification) → Phase 7 (Testing)
```

**Parallelizable:**
- Phase 2 & 3 can run in parallel after Phase 1
- Phase 7 tests can be written alongside implementation

**Cross-module coordination:**
- `sales-checkout` migration runs first (creates `sales_transactions`)
- `financial-accumulators` migration runs second (renames `resumen_caja`)
- `expenses-cashflow` migration runs third (creates `expense_categories`, adds expense index)
- All share `user_version = 2`

---

## Risk Mitigation

| Risk | Mitigation |
|---|---|
| Migration order conflict | Single `migrations.ts` runner with explicit ordering; `PRAGMA user_version` checked |
| `RENAME COLUMN` fails | SQLite ≥ 3.25 confirmed; test on device |
| Atomic transaction partial failure | Single `withTransactionSync`; test failure injection |
| AdBanner in form/confirm | CI grep check: `grep -r "AdBanner" --include="*.tsx" app/gastos/nuevo.tsx src/presentation/components/feedback/ConfirmDialog.tsx` |
| Expense category draft loss | `expenseDraftStore` preserves form state during `QuickCategoryModal` |
| i18n keys missing | CI check: `grep -r "expenses\." --include="*.tsx" src/presentation/components/expenses` |
| `financial_accumulators` double rename | Only `financial-accumulators` module performs rename; expenses migration comments it out |

---

## Definition of Done

- [x] Migration runs atomically, preserves all data, coordinates with other modules
- [ ] All entities/repos/use cases use English identifiers
- [x] `RegisterExpense` works atomically with cash flow impact
- [x] `ReverseTransaction` handles EXPENSE correctly (balance only, no stock)
- [ ] ConfirmDialog shows interpolated Spanish text correctly
- [ ] Category selector with quick-create modal preserves form draft
- [ ] AdBanner appears in list footer only when `AD_ENABLED`
- [ ] All 45+ i18n keys implemented
- [x] Unit tests: 100% pass
- [x] Integration tests: 100% pass
- [x] Migration tests: 100% pass
- [x] Atomic rollback tests: 100% pass
- [x] TypeScript strict: no errors
- [ ] Lint: no errors

---

*Plan version: 1.0 — 2026-11-10*