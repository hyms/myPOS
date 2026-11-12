# Expenses & Cash Flow — Implementation Plan (v2.0.0 KMP)

> **Status (2026-11-12):** KMP migration planning phase. Phase 0 (Shared Kernel definition) complete on paper. Implementation begins with Phase 4 after Financial Accumulators.

## Overview

This plan covers the implementation of the Expenses & Cash Flow module v2.0.0 KMP, including:
- Domain layer: English entities (`Expense`, `ExpenseCategory`), Value Objects, rules
- Data layer: Repository interfaces, SQLDelight mappers, migration coordination
- Application layer: `RegisterExpense`, integration with `ReverseTransaction`
- Presentation layer: ViewModels, Compose UI components, AdWare governance
- Testing: Unit, integration, migration, atomic, cash integrity

---

## Phase 1: Database Migration (Critical Path)

### 1.1 Migration Runner Update
**File:** `commonMain/data/database/DatabaseDriverFactory.kt`
- Register the expenses step in the version-2 migration list (all v2 modules share one transaction)
- `user_version = 2` is set centrally by `DatabaseDriverFactory` after every v2 step succeeds
- Migration order: catalog-inventory → sales-checkout → financial-accumulators → expenses-cashflow → system-settings

### 1.2 Migration SQL
**File:** `commonMain/sqldelight/migrations/v2_expenses_cashflow.sqm` (see spec.md section 4.3)

### 1.3 Migration Checklist
- [ ] Register the expenses step in `DatabaseDriverFactory` v2 migration list
- [ ] Write `v2_expenses_cashflow.sqm` with atomic transaction
- [ ] Create `expense_categories` table with index
- [ ] `financial_accumulators` rename owned by `financial-accumulators` module (no duplication)
- [ ] Expense queries covered by composite `sales_transactions` indexes
- [ ] `user_version` = 2 (centralized)
- [ ] Verify row counts match pre-migration for expenses
- [ ] Test restore from backup scenario

---

## Phase 2: Domain Layer (`commonMain/domain/`)

### 2.1 Entity Files
| New File | Purpose |
|---|---|
| `entities/Expense.kt` | Expense aggregate root with `CreateExpenseInput`, `UpdateExpenseInput` |
| `entities/ExpenseCategory.kt` | ExpenseCategory aggregate root with `CreateExpenseCategoryInput` |

### 2.2 Domain Rules
**File:** `rules/ExpenseRules.kt`
- `validateExpenseAmount(amount: Double): Unit` — throws if `amount <= 0` or not finite
- `validateExpenseDescription(description: String): Unit` — throws if empty after trim
- `roundAmount(amount: Double): Double` — `(amount * 100).roundToInt() / 100.0` (2dp)

### 2.3 Shared Value Objects
- `valueobjects/Money.kt`, `Currency.kt` — from `system-settings/spec.md` section 2.1
- `entities/PaymentMethod.kt` — from `sales-checkout/spec.md` section 2.4

---

## Phase 3: Data Layer (`commonMain/data/repositories/`)

### 3.1 Repository Interface
**File:** `IExpenseRepository.kt`
- English interface per spec.md section 5.1
- Methods: `list`, `findById`, `create`, `update`, `softDelete`, `listCategories`, `findCategoryById`, `createCategory`, `softDeleteCategory`

### 3.2 Repository Implementation
**File:** `impl/SqlDelightExpenseRepository.kt`
- Implements `IExpenseRepository`
- Queries `sales_transactions` filtered by `type = 'EXPENSE'`
- Maps rows → entities via SQLDelight mappers
- Uses transaction block for create/update/softDelete

### 3.3 SQLDelight Mappers
- `mapExpenseRow` — `sales_transactions` row → `Expense` entity
- `mapExpenseCategoryRow` — `expense_categories` row → `ExpenseCategory` entity

### 3.4 Registration
**File:** `commonMain/data/database/DatabaseDriverFactory.kt`
- Register `IExpenseRepository` → `SqlDelightExpenseRepository`

---

## Phase 4: Application Layer (`commonMain/application/`)

### 4.1 RegisterExpense
**File:** `expenses/RegisterExpense.kt`
- Input: `categoryId`, `amount`, `description`, `paymentMethod`
- Atomic transaction block:
  1. Validate `amount > 0`, `description.trim().isNotEmpty()`
  2. `INSERT INTO sales_transactions (type='EXPENSE', total_amount, payment_method, detail)` → `lastInsertRowId`
  3. `UPDATE financial_accumulators SET total_expenses += amount, net_balance -= amount, updated_at = CURRENT_TIMESTAMP WHERE id = 1`
  4. Return `{ expenseId }`

### 4.2 ReverseExpense (via ReverseTransaction)
**File:** `transactions/ReverseTransaction.kt` (shared)
- Handles `EXPENSE` type:
  1. Find transaction (must exist, `type = 'EXPENSE'`, `deleted_at IS NULL`)
  2. `UPDATE financial_accumulators SET total_expenses -= amount, net_balance += amount, updated_at = CURRENT_TIMESTAMP WHERE id = 1`
  3. `UPDATE sales_transactions SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?`
  4. Return `{ transactionId, type: EXPENSE }`
- **No stock changes** (RULE-EC-03)

---

## Phase 5: Presentation Layer

### 5.1 ViewModels (`commonMain/presentation/viewmodels/`)
| ViewModel | File | Description |
|---|---|---|
| `ExpensesViewModel` | `ExpensesViewModel.kt` | Paginated list (month/year filter), infinite scroll, search, category filter |
| `ExpenseFormViewModel` | `ExpenseFormViewModel.kt` | Form draft (amount, description, categoryId, paymentMethod), cached categories |
| `ExpenseCategoriesViewModel` | `ExpenseCategoriesViewModel.kt` | Active categories list + manual refresh |

### 5.2 Compose UI Components (`composeApp/src/commonMain/compose/`)

| Component | File | Description |
|---|---|---|
| `ConfirmDialog.kt` | `feedback/ConfirmDialog.kt` | Reusable, interpolated text |
| `EmptyState.kt` | `feedback/EmptyState.kt` | Reusable |
| `ExpenseListScreen.kt` | `expenses/ExpenseListScreen.kt` | `LazyColumn` with footer `AdBanner` when `AD_ENABLED` |
| `ExpenseListItem.kt` | `expenses/ExpenseListItem.kt` | Memoized row (category badge, amount, description, date) |
| `CategorySelector.kt` | `expenses/CategorySelector.kt` | Dropdown + `[+]` for `QuickCategoryDialog` |
| `QuickCategoryDialog.kt` | `expenses/QuickCategoryDialog.kt` | Bottom sheet, creates category, invalidates cache, auto-selects |
| `AdBanner.kt` | `ads/AdBanner.kt` | **ALLOWED** in list footer (`AD_ENABLED` check) |
| `SearchBar.kt` | `filters/SearchBar.kt` | Reusable, debounce 200ms |
| `CategoryChipsRow.kt` | `filters/CategoryChipsRow.kt` | Reusable, clean text chips |

### 5.3 AdWare Integration
**Files to modify:**
- `ExpenseListScreen.kt` — Add `AdBanner` in `LazyColumn` footer when `AD_ENABLED`
- `ExpenseFormScreen.kt` — **No ads** (critical flow)

**Rule:** `AD_ENABLED` flag respected in list; ignored in form, ConfirmDialog, PIN gate.

### 5.4 i18n
**File:** `composeApp/src/commonMain/composeResources/values-es/strings.xml`
- All 45+ keys from spec.md section 14
- Namespace: `expenses_`

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
**Directory:** `commonTest/unit/expenses-cashflow/`
- `ExpenseRulesTest.kt` — All 3 functions with edge cases
- `ExpenseTest.kt` — Entity creation, validation
- `ExpenseCategoryTest.kt` — Entity creation, validation

### 7.2 Integration Tests
**Directory:** `commonTest/integration/expenses-cashflow/`
- `RegisterExpenseTest.kt` — Success, validation errors, atomic rollback, cash flow impact
- `ReverseExpenseTest.kt` — Reversal restores balance, soft delete, no stock changes

### 7.3 Migration Tests
**Directory:** `commonTest/migration/expenses-cashflow/`
- `V2ExpensesCashflowMigrationTest.kt` — Seed v1 schema, run migration, assert mappings

### 7.4 Atomic Transaction Tests
**Directory:** `commonTest/atomic/`
- `ExpenseAtomicRollbackTest.kt` — Simulate DB failure mid-transaction → full rollback

### 7.5 ViewModel Tests (Turbine)
**Directory:** `commonTest/viewmodels/expenses-cashflow/`
- `ExpensesViewModelTest.kt` — Pagination, filtering, infinite scroll
- `ExpenseFormViewModelTest.kt` — Form validation, category selection, ConfirmDialog flow
- `ExpenseCategoriesViewModelTest.kt` — Category list, refresh

### 7.6 Compose UI Tests (Robolectric)
**Directory:** `androidTest/compose/expenses/`
- `ExpenseListItemTest.kt` — Render expense data, category badge
- `CategorySelectorTest.kt` — Dropdown, quick category creation
- `QuickCategoryDialogTest.kt` — Create category, auto-select, preserve draft

### 7.7 E2E Tests (Maestro)
**Directory:** `maestro/expenses-cashflow/`
- `expenseFlow.yaml` — Add expense → confirm → verify list → verify cash summary
- `expenseReversal.yaml` — Reverse expense → verify balance restored
- `categoryFlow.yaml` — Create category via dialog → select → create expense
- `migrationIdempotency.yaml` — Run migration twice → no errors

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
| Migration order conflict | Single `DatabaseDriverFactory` with explicit ordering; `PRAGMA user_version` checked |
| `ALTER TABLE` fails | SQLite ≥ 3.25 confirmed; test on device |
| Atomic transaction partial failure | Single transaction block; test failure injection |
| AdBanner in form/confirm | CI grep check: `grep -r "AdBanner" --include="*.kt" composeApp/.../expenses/ExpenseFormScreen.kt composeApp/.../feedback/ConfirmDialog.kt` |
| Expense category draft loss | `ExpenseFormViewModel` preserves form state during `QuickCategoryDialog` |
| i18n keys missing | CI check: `grep -r "expenses_" --include="*.kt" composeApp/.../expenses` |
| `financial_accumulators` double rename | Only `financial-accumulators` module performs rename; expenses migration comments it out |

---

## Definition of Done

- [ ] Migration runs atomically, preserves all data, coordinates with other modules
- [ ] All entities/repos/use cases use English identifiers
- [ ] `RegisterExpense` works atomically with cash flow impact
- [ ] `ReverseTransaction` handles EXPENSE correctly (balance only, no stock)
- [ ] ConfirmDialog shows interpolated Spanish text correctly
- [ ] Category selector with quick-create dialog preserves form draft
- [ ] AdBanner appears in list footer only when `AD_ENABLED`
- [ ] All 45+ i18n keys implemented
- [ ] Unit tests: 100% pass
- [ ] Integration tests: 100% pass
- [ ] Migration tests: 100% pass
- [ ] Atomic rollback tests: 100% pass
- [ ] ViewModel tests: 100% pass
- [ ] Compose UI tests: 100% pass
- [ ] Kotlin strict: no errors
- [ ] Detekt: no errors

---

*Plan version: 2.0 — 2026-11-12 — KMP Migration*