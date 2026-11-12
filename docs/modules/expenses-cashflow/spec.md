# Expenses & Cash Flow — Specification (v2.0.0 KMP)

## 1. Bounded Context

Responsible for the registration of operational cash outflows (expenses), expense categorization, confirmation dialogs, and their direct impact on the singleton financial accumulator (`financial_accumulators`). Expenses are modeled as a transaction type (`EXPENSE`) inside the unified `sales_transactions` table, sharing the atomic transaction infrastructure with Sales & Checkout and Financial Accumulators modules.

---

## 2. Domain Entities & Value Objects

All identifiers, fields, and types in code and database are in **English**. Only visible UI strings remain in Spanish via i18n keys (`composeResources/values-es/strings.xml`).

### 2.1 Expense (Aggregate Root)

**File:** `commonMain/domain/entities/Expense.kt`

```kotlin
package com.tpvmovil.domain.entities

import com.tpvmovil.domain.valueobjects.Money

@JvmInline
value class ExpenseId(val value: Long) : Comparable<ExpenseId> {
    override fun compareTo(other: ExpenseId) = value.compareTo(other.value)
}

@JvmInline
value class ExpenseCategoryId(val value: Long) : Comparable<ExpenseCategoryId> {
    override fun compareTo(other: ExpenseCategoryId) = value.compareTo(other.value)
}

data class Expense(
    val id: ExpenseId,
    val categoryId: ExpenseCategoryId?,
    val amount: Money,
    val description: String,
    val paymentMethod: PaymentMethod,
    val registeredAt: String,    // ISO 8601
    val deletedAt: String?
) {
    companion object {
        fun create(input: CreateExpenseInput): Expense = TODO()
    }
}

data class CreateExpenseInput(
    val categoryId: ExpenseCategoryId?,
    val amount: Double,         // amount in base currency, > 0
    val description: String,    // non-empty
    val paymentMethod: PaymentMethod
)

data class UpdateExpenseInput(
    val categoryId: ExpenseCategoryId? = null,
    val amount: Double? = null,
    val description: String? = null,
    val paymentMethod: PaymentMethod? = null
)
```

### 2.2 ExpenseCategory (Aggregate Root)

**File:** `commonMain/domain/entities/ExpenseCategory.kt`

```kotlin
package com.tpvmovil.domain.entities

data class ExpenseCategory(
    val id: ExpenseCategoryId,
    val name: String,
    val registeredAt: String,
    val deletedAt: String?
) {
    companion object {
        fun create(input: CreateExpenseCategoryInput): ExpenseCategory = TODO()
    }
}

data class CreateExpenseCategoryInput(val name: String)
```

### 2.3 Money / Currency (Shared Kernel)

**Files:** `commonMain/domain/valueobjects/Money.kt`, `Currency.kt` — see `system-settings/spec.md` section 2.1.

---

## 3. Domain Rules & Invariants

| ID | Rule | Description |
|---|---|---|
| RULE-EC-01 | Amount valid | `amount > 0` and `amount.isFinite()`. |
| RULE-EC-02 | Description required | `description.trim().isNotEmpty()`. |
| RULE-EC-03 | No product detail rows | `EXPENSE` type does **not** insert rows into `transaction_details`. |
| RULE-EC-04 | ConfirmDialog interpolated | Text: `"¿Está seguro de registrar este gasto por un monto de [Monto] [Divisa] con el detalle [Descripción]?"`. Options: **Cancelar** \| **Confirmar**. |
| RULE-EC-05 | Category optional | `categoryId` may be `null` (no mandatory category for expenses). |
| RULE-EC-06 | Single payment method | Exactly one `PaymentMethod` per expense. No mixed/split payments (INV-04). |
| RULE-EC-07 | Atomic transaction | Expense registration: insert `sales_transactions` (type='EXPENSE') + update `financial_accumulators` — all in SQLDelight transaction block. |
| RULE-EC-08 | Rounded amounts | `round2(v) = (v * 100).roundToInt() / 100.0` on amount, totals. |

Global invariants: **INV-01** (non-negative financial math), **INV-03** (atomic transaction block), **INV-04** (single payment method), **INV-10** (strict Kotlin).

---

## 4. Database Contract (SQLDelight)

### 4.1 Target Schema (v2.0.0 — English Columns)

**File:** `commonMain/sqldelight/schema.sq` (expenses section)

```sql
-- sales_transactions (unified table — see sales-checkout/spec.md)
-- Expenses are stored here with type = 'EXPENSE'
-- No rows in transaction_details for expenses

-- financial_accumulators (renamed from resumen_caja — see financial-accumulators/spec.md)
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

-- expense_categories
CREATE TABLE expense_categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    registered_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
    deleted_at INTEGER
);

-- Partial indexes (deleted_at IS NULL)
CREATE INDEX idx_expense_categories_name ON expense_categories(name) WHERE deleted_at IS NULL;

-- Indexes for expense queries on sales_transactions
CREATE INDEX idx_sales_transactions_expense_date ON sales_transactions(registered_at) WHERE type = 'EXPENSE' AND deleted_at IS NULL;
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

**File:** `commonMain/sqldelight/migrations/v2_expenses_cashflow.sqm`

```sql
-- v2_expenses_cashflow.sqm
-- Runs inside the shared v2.0.0 transaction (single transaction block)
-- NOTE: sales_transactions and financial_accumulators are migrated by their own modules.
-- This migration only handles expense_categories.

-- Step 1: Create expense_categories table (new in v2.0.0)
CREATE TABLE IF NOT EXISTS expense_categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    registered_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
    deleted_at INTEGER
);

-- Step 2: Create index for expense_categories
CREATE INDEX IF NOT EXISTS idx_expense_categories_name
  ON expense_categories(name) WHERE deleted_at IS NULL;

-- Step 3: user_version = 2 is set centrally by DatabaseDriverFactory
```

> **Important:** The `resumen_caja` → `financial_accumulators` rename and column renames are coordinated with the `financial-accumulators` module migration. Both modules must agree on migration ordering in `DatabaseDriverFactory`.

---

## 5. Repository Interfaces

### 5.1 IExpenseRepository

**File:** `commonMain/data/repositories/IExpenseRepository.kt`

```kotlin
package com.tpvmovil.data.repositories

import com.tpvmovil.domain.entities.*

interface IExpenseRepository {
    // Expenses (queried from sales_transactions where type = 'EXPENSE')
    fun list(query: ExpenseQuery): List<Expense>
    fun findById(id: ExpenseId): Expense?
    fun create(input: CreateExpenseInput): Expense
    fun update(id: ExpenseId, input: UpdateExpenseInput): Expense
    fun softDelete(id: ExpenseId)  // sets deleted_at = CURRENT_TIMESTAMP

    // Expense Categories
    fun listCategories(): List<ExpenseCategory>
    fun findCategoryById(id: ExpenseCategoryId): ExpenseCategory?
    fun createCategory(input: CreateExpenseCategoryInput): ExpenseCategory
    fun softDeleteCategory(id: ExpenseCategoryId)
}

data class ExpenseQuery(
    val period: Period = Period.MONTH,
    val categoryId: ExpenseCategoryId? = null,
    val search: String? = null,
    val limit: Int = 15,
    val offset: Int = 0
)

enum class Period { MONTH, YEAR }
```

---

## 6. Use Cases (Application Layer — `commonMain`)

### 6.1 RegisterExpense

**File:** `commonMain/application/expenses/RegisterExpense.kt`

```kotlin
package com.tpvmovil.application.expenses

import com.tpvmovil.domain.entities.PaymentMethod

data class RegisterExpenseInput(
    val categoryId: ExpenseCategoryId?,
    val amount: Double,
    val description: String,
    val paymentMethod: PaymentMethod
)

data class RegisterExpenseResult(
    val expenseId: ExpenseId
)

suspend fun registerExpense(input: RegisterExpenseInput): RegisterExpenseResult
```

**Atomic Flow (SQLDelight transaction block):**

1. Validate `amount > 0` and `description.trim().isNotEmpty()` (throw `ValidationError` if fails).
2. `INSERT INTO sales_transactions (type='EXPENSE', total_amount=amount, payment_method=paymentMethod.code, detail=description)` → `lastInsertRowId`.
3. `UPDATE financial_accumulators SET total_expenses = total_expenses + amount, net_balance = net_balance - amount, updated_at = CURRENT_TIMESTAMP WHERE id = 1`.
4. Return `{ expenseId: lastInsertRowId }`.

### 6.2 ReverseExpense (via ReverseTransaction)

**File:** `commonMain/application/transactions/ReverseTransaction.kt` — shared with `sales-checkout` and `financial-accumulators`.

```kotlin
data class ReverseTransactionResult(
    val transactionId: TransactionId,
    val type: TransactionType
)

suspend fun reverseTransaction(id: TransactionId): ReverseTransactionResult
```

**Atomic Flow for EXPENSE type (SQLDelight transaction block):**

1. Find transaction by ID (must exist, `type = 'EXPENSE'`, `deleted_at IS NULL`).
2. `UPDATE financial_accumulators SET total_expenses = total_expenses - amount, net_balance = net_balance + amount, updated_at = CURRENT_TIMESTAMP WHERE id = 1`.
3. `UPDATE sales_transactions SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?`.
4. Return `{ transactionId: id, type: EXPENSE }`.

> **No stock changes** for expenses (RULE-EC-03).

---

## 7. Domain Rules (Expense Math)

**File:** `commonMain/domain/rules/ExpenseRules.kt`

| Function | Signature | Description |
|---|---|---|
| `validateExpenseAmount` | `(amount: Money) -> Unit` | Throws if the amount is not positive. |
| `validateExpenseDescription` | `(description: String) -> Unit` | Throws if empty after trim. |
| `roundAmount` | `(amount: Double) -> Double` | `(amount * 100).roundToInt() / 100.0` (2dp) |

---

## 8. UI State Machines (Spanish visible text via `stringResource`)

### 8.1 Expense Form (`ExpenseFormScreen`)

| State | Trigger | Next State | Visible Text (ES) |
|---|---|---|---|
| `FormEmpty` | Open form | `Editing` | `"Registrar gasto"` (top bar title) |
| `Editing` | Amount & description valid | `ConfirmDialog` | `"¿Registrar gasto?"` (button) |
| `ConfirmDialog` | User taps Confirmar | `Registered` | `"¿Está seguro de registrar este gasto por un monto de [Monto] [Divisa] con el detalle [Descripción]?"` |
| `Registered` | ConfirmDialog accepted | `ExpenseList` | `"Gasto registrado"` (Snackbar) |
| `Editing` | User taps Cancelar | `FormEmpty` | `"Cancelar"` |

### 8.2 Expense List (`ExpenseListScreen`)

| State | Trigger | Next State | Visible Text (ES) |
|---|---|---|---|
| `ListLoading` | Mount / pull-to-refresh | `ListLoaded` / `ListEmpty` | Skeleton rows |
| `ListLoaded` | Data received | — | Paginated list, FAB to add |
| `ListEmpty` | No expenses this period | — | `"Sin gastos este mes"` + "Registrar gasto" action |
| `ListLoaded` | Tap expense row | `Detail` | Navigate to detail (read-only) |

---

## 9. Compose UI Components

| Component | File | Type | Memoized | Description |
|---|---|---|---|---|
| `ConfirmDialog` | `composeApp/.../feedback/ConfirmDialog.kt` | Composable | No | Reusable confirmation with interpolated text |
| `EmptyState` | `composeApp/.../feedback/EmptyState.kt` | Composable | No | Illustration + title + description + action |
| `ExpenseListScreen` | `composeApp/.../expenses/ExpenseListScreen.kt` | Screen | N/A | Main list with `LazyColumn` |
| `ExpenseListItem` | `composeApp/.../expenses/ExpenseListItem.kt` | Composable | `key = expense.id` | Row: category badge, amount, description, date |
| `CategorySelector` | `composeApp/.../expenses/CategorySelector.kt` | Composable | No | Dropdown with `[+]` for quick category creation |
| `QuickCategoryDialog` | `composeApp/.../expenses/QuickCategoryDialog.kt` | Composable | No | Bottom sheet: name input → creates category → auto-selects |
| `AdBanner` | `composeApp/.../ads/AdBanner.kt` | Composable | No | **ALLOWED** in Expense list footer (AD_ENABLED check) |
| `SearchBar` | `composeApp/.../filters/SearchBar.kt` | Composable | No | Debounce 200ms |
| `CategoryChipsRow` | `composeApp/.../filters/CategoryChipsRow.kt` | Composable | No | Clean text chips for category filter |

---

## 10. ViewModels

| ViewModel | File | Responsibility |
|---|---|---|
| `ExpensesViewModel` | `commonMain/presentation/viewmodels/ExpensesViewModel.kt` | Paginated expense list (month/year filter), infinite scroll, search, category filter |
| `ExpenseFormViewModel` | `commonMain/presentation/viewmodels/ExpenseFormViewModel.kt` | Form state, validation, category management, ConfirmDialog state |
| `ExpenseCategoriesViewModel` | `commonMain/presentation/viewmodels/ExpenseCategoriesViewModel.kt` | Active expense categories list + manual refresh |

---

## 11. AdWare Governance

**Rule:** Banner ads **ARE ALLOWED** on the Expenses list screen (`ExpenseListScreen`).

**Implementation:**
- Check `AD_ENABLED` flag via `AdEnabledProvider` (CompositionLocal).
- If `true`, render `AdBanner` in `LazyColumn` footer item, **below** expenses and **above** FAB.
- Do not interrupt scroll or card tactile feedback.
- Respect `prefersReducedMotion` for banner animations.
- **NOT** allowed in `ExpenseFormScreen`, `ConfirmDialog`, or any critical flow.

---

## 12. Applicable NFRs

- **NFR-01:** Offline-first SQLDelight (JSI SQLite driver, transaction blocks).
- **NFR-02:** `LazyColumn` single column, sub-16ms render, stable keys.
- **NFR-03:** 0ms cash calculations — read from `financial_accumulators` accumulator only.
- **NFR-04:** Pagination `LIMIT ? OFFSET ?` with `pageSize` from settings (default 15).
- **NFR-07:** Atomic rollback via SQLDelight transaction blocks on expense registration and reversal.
- **NFR-08:** UI strings in Spanish via `stringResource`; code/DB/identifiers in English.
- **NFR-09:** Strict Kotlin (`explicit-api`, `-Xlint`).
- **NFR-10:** Tests: 100% pass unit + integration + migration + atomic + cash integrity.

---

## 13. Extension Points (v2.0.0 Roadmap)

- [ ] **Expense attachments**: Receipt photo capture (`CameraX` + `ImageOptimizer`) linked to expense.
- [ ] **Recurring expenses**: Template with frequency (daily/weekly/monthly) + auto-registration.
- [ ] **Expense budgets**: Monthly budget per category with progress bars and alerts.
- [ ] **Expense reports**: PDF/CSV export with filters (date range, category, payment method).
- [ ] **Multi-currency expenses**: Separate accumulator per currency with FX rates.
- [ ] **Vendor management**: Payee/vendor entity linked to expenses for reporting.

---

## 14. i18n Keys (Spanish UI Dictionary — `composeResources/values-es/strings.xml`)

```xml
<!-- Expenses List -->
<string name="expenses_title">Gastos</string>
<string name="expenses_fab_label">Registrar gasto</string>
<string name="expenses_empty_title">Sin gastos este mes</string>
<string name="expenses_empty_desc">Aún no has registrado ningún gasto en el período actual.</string>
<string name="expenses_empty_action">Registrar gasto</string>
<string name="expenses_list_search_placeholder">Buscar en gastos…</string>
<string name="expenses_list_category_all">Todas</string>
<string name="expenses_list_amount_prefix">-</string>

<!-- Expense Form -->
<string name="expenses_form_title">Registrar gasto</string>
<string name="expenses_form_label_amount">Monto</string>
<string name="expenses_form_label_description">Descripción</string>
<string name="expenses_form_label_category">Categoría</string>
<string name="expenses_form_label_payment_method">Tipo de pago</string>
<string name="expenses_form_placeholder_amount">0.00</string>
<string name="expenses_form_placeholder_description">Ej. Material de oficina</string>
<string name="expenses_form_helper_category_optional">Opcional</string>
<string name="expenses_form_category_new">Nueva</string>

<!-- Quick Category Modal -->
<string name="expenses_modal_category_title">Nueva categoría de gasto</string>
<string name="expenses_modal_category_label">Nombre</string>
<string name="expenses_modal_category_placeholder">Ej. Transporte</string>

<!-- Buttons -->
<string name="expenses_btn_register">Registrar</string>
<string name="expenses_btn_cancel">Cancelar</string>

<!-- ConfirmDialog -->
<string name="expenses_confirm_title">¿Registrar gasto?</string>
<string name="expenses_confirm_message">¿Está seguro de registrar este gasto por un monto de %1$s %2$s con el detalle "%3$s"?</string>
<string name="expenses_confirm_cancel">Cancelar</string>
<string name="expenses_confirm_confirm">Confirmar</string>

<!-- Toasts -->
<string name="expenses_toast_registered">Gasto registrado</string>
<string name="expenses_toast_error">No se pudo registrar el gasto.</string>
<string name="expenses_toast_deleted">Gasto eliminado</string>

<!-- Validation -->
<string name="expenses_validation_amount_positive">El monto debe ser mayor a 0.</string>
<string name="expenses_validation_description_required">La descripción es obligatoria.</string>

<!-- Detail -->
<string name="expenses_detail_title">Detalle de gasto</string>
<string name="expenses_detail_amount">Monto: %1$s</string>
<string name="expenses_detail_category">Categoría: %1$s</string>
<string name="expenses_detail_payment">Pago: %1$s</string>
<string name="expenses_detail_date">Fecha: %1$s</string>
<string name="expenses_detail_description">Descripción: %1$s</string>
```

---

## 15. Cross-References

- `sales-checkout/spec.md` — Section 4 (Database Contract), 6 (Use Cases), 12 (NFRs), unified `sales_transactions` table.
- `financial-accumulators/spec.md` — Section 4 (financial_accumulators schema), 6.3 (UpdateAccumulator), 6.4 (ReverseTransaction EXPENSE path).
- `system-settings/spec.md` — Section 2.1 (Currency/Money), 3 (RULE-SS-05, RULE-SS-06), 10 (useCurrency, useSettingsPageSize, AD_ENABLED).
- `catalog-inventory/spec.md` — Section 6 (RULE-CI-03: fractional amounts in UI).

---

*Last updated: 2026-11-12 — v2.0.0 KMP Specification*