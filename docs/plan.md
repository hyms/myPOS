# TPV Móvil Express — Global Implementation Plan v2.0.0 (KMP Migration)

## Executive Summary

This document consolidates the implementation roadmaps for all five bounded contexts into a single coordinated plan for the v2.0.0 migration to **Kotlin Multiplatform (KMP) + Compose Multiplatform + SQLDelight**. The migration enforces strict Clean Architecture + DDD layer boundaries, English-only code/DB identifiers, atomic SQLite transactions via SQLDelight, and centralized AdWare governance.

**Migration Strategy:** Single SQLDelight migration runner (`.sqm` files) with ordered, atomic migrations sharing `PRAGMA user_version = 2`. All domain logic in `commonMain`, platform-specific implementations in `androidMain`/`iosMain`.

> **Status (2026-11-12):** KMP migration planning phase. Phase 0 (Shared Kernel definition) complete on paper. Implementation begins with Phase 1.

> **Workspace layout:** the legacy React Native/Expo codebase lives in `rn-app/`; the new Kotlin Multiplatform project lives in `kmp-app/`. Every `commonMain/`, `composeApp/`, and Gradle path in this plan is relative to `kmp-app/`. Shared specs and plans stay in `/docs/`.

**Total Effort:** ~6 phases across 5 modules, coordinated via shared `commonMain` dependencies.

---

## 1. Module Overview & Migration Order

| Order | Module | Bounded Context | Key Tables | Migration Depends On |
|-------|--------|-----------------|------------|---------------------|
| 1 | **catalog-inventory** | Products, categories, stock, images | `products`, `categories` | — (base) |
| 2 | **sales-checkout** | Cart, sales/purchases, payment, receipts | `sales_transactions`, `transaction_details` | catalog-inventory (products) |
| 3 | **financial-accumulators** | Cash accumulator, reports, reversal | `financial_accumulators`, `sales_transactions` | sales-checkout (transactions) |
| 4 | **expenses-cashflow** | Expenses, expense categories | `sales_transactions` (type=EXPENSE), `expense_categories` | financial-accumulators (accumulator) |
| 5 | **system-settings** | Currency, pagination, PIN/biometric, backup, AD_ENABLED | `settings` (singleton) | expenses-cashflow (last) |

**Cross-Module Contracts (Implemented Once in `commonMain`, Imported Everywhere):**
- `Currency`, `Money`, `Stock`, `SKU` → `commonMain/domain/value-objects/`
- `PaymentMethod`, `ChangeAmount`, `TransactionType` → `commonMain/domain/entities/`
- `stockRules`, `cartRules`, `expenseRules`, `settingsRules` → `commonMain/domain/rules/`
- `ReverseTransaction` use case → `commonMain/application/transactions/`

---

## 2. Phased Implementation Roadmap (KMP)

### Phase 0: Foundation — Shared Kernel Definition (Week 1)
**Goal:** Define all shared value objects, domain rules, and SQLDelight schema in `commonMain` using Kotlin idioms (value classes, immutable data classes, sealed interfaces).

| Task | File | Module | Status |
|------|------|--------|--------|
| Define `Currency` value class | `commonMain/domain/value-objects/Currency.kt` | system-settings | 🆕 |
| Define `Money` value class | `commonMain/domain/value-objects/Money.kt` | system-settings | 🆕 |
| Define `Stock` value class | `commonMain/domain/value-objects/Stock.kt` | catalog-inventory | 🆕 |
| Define `SKU` value class | `commonMain/domain/value-objects/SKU.kt` | catalog-inventory | 🆕 |
| Define `PaymentMethod` sealed class | `commonMain/domain/entities/PaymentMethod.kt` | sales-checkout | 🆕 |
| Define `ChangeAmount` value class | `commonMain/domain/value-objects/ChangeAmount.kt` | sales-checkout | 🆕 |
| Define `TransactionType` sealed class | `commonMain/domain/entities/TransactionType.kt` | shared | 🆕 |
| Define `stockRules` | `commonMain/domain/rules/StockRules.kt` | catalog-inventory | 🆕 |
| Define `cartRules` | `commonMain/domain/rules/CartRules.kt` | sales-checkout | 🆕 |
| Define `expenseRules` | `commonMain/domain/rules/ExpenseRules.kt` | expenses-cashflow | 🆕 |
| Define `settingsRules` | `commonMain/domain/rules/SettingsRules.kt` | system-settings | 🆕 |
| Create SQLDelight schema (`.sq`) | `commonMain/sqldelight/schema.sq` | all | 🆕 |
| Create SQLDelight migrations (`.sqm`) | `commonMain/sqldelight/migrations/` | all | 🆕 |

**Gate:** All shared VOs compile in `commonMain`, unit tests pass (100% coverage), SQLDelight generates query APIs.

---

### Phase 1: Catalog & Inventory (Week 2)
**Depends on:** Phase 0 complete

| Task | File | Status |
|------|------|--------|
| Define `Product` / `Category` entities | `commonMain/domain/entities/` | 🆕 |
| SQLDelight migration v2: catalog schema | `commonMain/sqldelight/migrations/v2_catalog_inventory.sqm` | 🆕 |
| Repository interfaces (Ports) | `commonMain/data/repositories/` | 🆕 |
| SQLDelight Repository implementations | `commonMain/data/repositories/impl/` | 🆕 |
| Use case: `ProductManagement` | `commonMain/application/products/` | 🆕 |
| ImageOptimizer/ImageStorage (expect/actual) | `commonMain/infrastructure/image/` | 🆕 |
| ViewModels: `ProductsViewModel`, `ProductFormViewModel` | `commonMain/presentation/viewmodels/` | 🆕 |
| Compose UI: `ProductGridScreen`, `ProductFormScreen` | `composeApp/src/commonMain/compose/` | 🆕 |
| i18n: `composeResources/values-es/strings.xml` (catalog) | `composeApp/src/commonMain/composeResources/` | 🆕 |
| Android/iOS platform specifics | `composeApp/src/androidMain/`, `iosMain/` | 🆕 |
| Tests: Unit + Integration + Migration + UI | `commonTest/`, `androidTest/` | 🆕 |

**Definition of Done (Catalog):**
- [ ] Migration v1→v2 zero data loss, idempotent, rollback verified
- [ ] All entities/repos/use cases use English identifiers + VOs
- [ ] Fractional stock (3dp) works end-to-end
- [ ] SKU unique validated, category required enforced
- [ ] AdBanner renders in Inventory list footer when `AD_ENABLED`
- [ ] Stock alert Toast fires post-commit when `currentStock < minStock`
- [ ] Zero hardcoded Spanish strings in catalog components (i18n only)
- [ ] All tests pass, Kotlin strict, Detekt clean

---

### Phase 2: Sales & Checkout (Week 3)
**Depends on:** Phase 0 + Phase 1 (products exist)

| Task | File | Status |
|------|------|--------|
| Entities: `SaleTransaction`, `TransactionDetail`, `CartItem` (Composite) | `commonMain/domain/entities/` | 🆕 |
| VOs: `PaymentMethod`, `ChangeAmount` (shared) | `commonMain/domain/` | 🆕 |
| Domain rules: `CartRules` (5 functions) | `commonMain/domain/rules/` | 🆕 |
| SQLDelight migration v2: sales schema | `commonMain/sqldelight/migrations/v2_sales_checkout.sqm` | 🆕 |
| Repositories: `ISaleRepository`, `SqlDelightSaleRepository` | `commonMain/data/repositories/` | 🆕 |
| Use cases: `RegisterSale`, `RegisterPurchase`, `ReverseTransaction` | `commonMain/application/` | 🆕 |
| ViewModels: `SaleCartViewModel`, `PurchaseCartViewModel`, `PaymentViewModel` | `commonMain/presentation/viewmodels/` | 🆕 |
| Compose UI: `CartScreen`, `LineItemDialog`, `PaymentDialog`, `ReceiptPreview` | `composeApp/src/commonMain/compose/` | 🆕 |
| AdWare removal from checkout screens | Compose UI | 🆕 |
| i18n: `composeResources/values-es/strings.xml` (checkout) | `composeApp/src/commonMain/composeResources/` | 🆕 |
| Tests: Unit + Integration + Migration + Atomic + UI | `commonTest/`, `androidTest/` | 🆕 |

**Definition of Done (Sales):**
- [ ] Migration preserves all transaction data, coordinates with catalog
- [ ] Composite Pattern: `ICartItem` + `SimpleCartItem` + Factory
- [ ] `RegisterSale`/`RegisterPurchase` atomic: stock + accumulator + transaction
- [ ] `PaymentDialog` calculates change correctly (green ≥0, red <0)
- [ ] `ReverseTransaction` handles SALE/PURCHASE/EXPENSE with stock safety
- [ ] Zero AdBanner in checkout, payment, receipt screens
- [ ] All 45+ i18n keys implemented
- [ ] Atomic rollback tests pass (simulated DB failure mid-transaction)
- [ ] All tests pass, Kotlin strict, Detekt clean

---

### Phase 3: Financial Accumulators (Week 3-4, parallel with Phase 2 after migration)
**Depends on:** Phase 2 migration complete (shared `sales_transactions`)

| Task | File | Status |
|------|------|--------|
| Entities: `FinancialSummary`, `PeriodSummary`, `Transaction` | `commonMain/domain/entities/` | 🆕 |
| SQLDelight migration v2: accumulators schema (+ columns recovery) | `commonMain/sqldelight/migrations/v2_financial_accumulators.sqm` | 🆕 |
| Repository: `IAccumulatorRepository`, `SqlDelightAccumulatorRepository` | `commonMain/data/repositories/` | 🆕 |
| Use cases: `GetCashSummary`, `GetPeriodSummary`, `UpdateAccumulator`, `ReverseTransaction` | `commonMain/application/accumulators/` | 🆕 |
| ViewModels: `CashSummaryViewModel`, `PeriodSummaryViewModel`, `MovementsViewModel` | `commonMain/presentation/viewmodels/` | 🆕 |
| Compose UI: `CashBalanceCard`, `StatCard`, `BreakdownBar`, `MovementListItem`, `TypeFilterChips` | `composeApp/src/commonMain/compose/` | 🆕 |
| AdBanner in Caja/Movimientos footers | Compose UI | 🆕 |
| i18n: `composeResources/values-es/strings.xml` (accumulator) | `composeApp/src/commonMain/composeResources/` | 🆕 |
| Tests: Unit + Integration + Migration + Atomic + UI | `commonTest/`, `androidTest/` | 🆕 |

**Definition of Done (Accumulators):**
- [ ] Migration renames `resumen_caja` → `financial_accumulators` atomically
- [ ] All entities/use cases use `Money` VOs, English identifiers
- [ ] `GetCashSummary` O(1) read, `GetPeriodSummary` correct aggregation
- [ ] `UpdateAccumulator` atomic single UPDATE with all deltas
- [ ] Reversal restores accumulator + stock (PURCHASE pre-check)
- [ ] AdBanner renders in Caja/Movimientos when `AD_ENABLED`, absent in detail/PIN
- [ ] All tests pass, Kotlin strict, Detekt clean

---

### Phase 4: Expenses & Cash Flow (Week 4)
**Depends on:** Phase 3 complete (accumulator exists)

| Task | File | Status |
|------|------|--------|
| Entities: `Expense`, `ExpenseCategory` | `commonMain/domain/entities/` | 🆕 |
| Domain rules: `ExpenseRules` (3 functions) | `commonMain/domain/rules/` | 🆕 |
| SQLDelight migration v2: expenses schema (creates `expense_categories`) | `commonMain/sqldelight/migrations/v2_expenses_cashflow.sqm` | 🆕 |
| Repository: `IExpenseRepository`, `SqlDelightExpenseRepository` | `commonMain/data/repositories/` | 🆕 |
| Use cases: `RegisterExpense`, `ReverseTransaction` (EXPENSE) | `commonMain/application/expenses/` | 🆕 |
| ViewModels: `ExpensesViewModel`, `ExpenseFormViewModel` | `commonMain/presentation/viewmodels/` | 🆕 |
| Compose UI: `ExpenseListScreen`, `ExpenseFormScreen`, `ConfirmDialog`, `CategorySelector`, `QuickCategoryDialog` | `composeApp/src/commonMain/compose/` | 🆕 |
| AdBanner in Expenses list footer only | Compose UI | 🆕 |
| i18n: `composeResources/values-es/strings.xml` (expenses) | `composeApp/src/commonMain/composeResources/` | 🆕 |
| Tests: Unit + Integration + Migration + Atomic + UI | `commonTest/`, `androidTest/` | 🆕 |

**Definition of Done (Expenses):**
- [ ] Migration creates `expense_categories`, adds expense index, coordinates accumulator
- [ ] `RegisterExpense` atomic: transaction + accumulator update
- [ ] `ReverseTransaction` EXPENSE: balance only, no stock changes
- [ ] `ConfirmDialog` shows interpolated Spanish text correctly
- [ ] Category quick-create dialog preserves form draft
- [ ] AdBanner in list footer only when `AD_ENABLED`
- [ ] All tests pass, Kotlin strict, Detekt clean

---

### Phase 5: System Settings (Week 5)
**Depends on:** Phase 4 complete (last module)

| Task | File | Status |
|------|------|--------|
| VOs: `Currency`, `Money` (from Phase 0, verify) | `commonMain/domain/value-objects/` | 🔄 |
| Entities: `UserSettings`, `SessionState` | `commonMain/domain/entities/` | 🆕 |
| Domain rules: `SettingsRules` (2 validators) | `commonMain/domain/rules/` | 🆕 |
| SQLDelight migration v2: settings schema (creates `settings` table) | `commonMain/sqldelight/migrations/v2_system_settings.sqm` | 🆕 |
| AsyncStorage → SQLite migration logic | `commonMain/application/settings/MigrateSettings.kt` | 🆕 |
| Repository: `ISettingsRepository`, `SqlDelightSettingsRepository` | `commonMain/data/repositories/` | 🆕 |
| Use cases: `GetSettings`, `UpdateSettings`, `VerifyPin`, `SetPin`, `ExportDatabase`, `RestoreDatabase` | `commonMain/application/` | 🆕 |
| ViewModels: `SettingsViewModel`, `SecurityViewModel`, `BackupViewModel` | `commonMain/presentation/viewmodels/` | 🆕 |
| Compose UI: `CurrencySelector`, `PageSizeInput`, `PinForm`, `BiometricToggle`, `BackupActions`, `AdEnabledToggle` | `composeApp/src/commonMain/compose/` | 🆕 |
| i18n: `composeResources/values-es/strings.xml` (settings + lock + pin + biometric + toast) | `composeApp/src/commonMain/composeResources/` | 🆕 |
| Tests: Unit + Integration + Migration + PIN Gate + UI | `commonTest/`, `androidTest/` | 🆕 |

**Definition of Done (Settings):**
- [ ] Migration creates `settings` singleton, migrates DataStore/Preferences on first `GetSettings`
- [ ] `Currency`/`Money` VOs used globally across all modules
- [ ] PIN change requires current PIN (except first time), constant-time compare
- [ ] Biometric toggle requires auth before enabling
- [ ] Restore backup requires PIN, reloads app, data intact
- [ ] `adEnabled` master toggle controls Expenses/Catalog/Caja/Movimientos banners
- [ ] All 60+ i18n keys implemented
- [ ] All tests pass, Kotlin strict, Detekt clean

---

### Phase 6: Global Verification & Hardening (Week 5-6)
**Goal:** End-to-end integration, performance, store readiness.

| Task | Description |
|------|-------------|
| Full migration run v1→v2 on seeded production-like data | Verify zero data loss across all 5 modules |
| Migration idempotency: run twice → no errors, no duplication | All 5 module migration tests |
| Migration rollback: restore backup → verify v1.0.0 schema intact | All 5 module rollback tests |
| Cross-module integration flows | Sale → Accumulator → Caja → Reversal → Stock restore |
| AdWare governance audit | CI grep checks on all forbidden views |
| i18n completeness audit | CI check for hardcoded Spanish strings in Compose |
| Performance: LazyColumn <16ms/frame, image <40KB | Physical device testing (Android/iOS) |
| PIN gate on critical actions | Change PIN, delete transaction, restore backup |
| E2E critical flows | Checkout, PIN change, backup/restore, ad toggle |
| Kotlin strict build | `./gradlew compileKotlinAndroid compileKotlinIos` zero errors |
| Detekt | `./gradlew detekt` zero errors |
| Test suite: all pass | Unit + Integration + Migration + Atomic + ViewModel + UI + E2E |

---

## 3. Shared Artifacts (Implement Once in `commonMain`)

| Artifact | Location | Consumers |
|----------|----------|-----------|
| `Currency`, `Money` | `commonMain/domain/value-objects/` | All modules |
| `Stock` | `commonMain/domain/value-objects/` | Catalog, Sales, Financial |
| `SKU` | `commonMain/domain/value-objects/` | Catalog, Sales |
| `PaymentMethod`, `ChangeAmount` | `commonMain/domain/` | Sales, Expenses, Financial |
| `TransactionType` | `commonMain/domain/entities/` | Sales, Expenses, Financial |
| `StockRules` | `commonMain/domain/rules/` | Catalog, Sales, Financial |
| `CartRules` | `commonMain/domain/rules/` | Sales |
| `ExpenseRules` | `commonMain/domain/rules/` | Expenses |
| `SettingsRules` | `commonMain/domain/rules/` | Settings |
| `ReverseTransaction` | `commonMain/application/transactions/` | Sales, Expenses, Financial |
| SQLDelight schema/migrations | `commonMain/sqldelight/` | All modules |
| `AdBanner` Composable | `composeApp/src/commonMain/compose/components/ads/` | Catalog, Expenses, Financial |
| `useAdEnabled` hook / `AdEnabledProvider` | `composeApp/src/commonMain/compose/hooks/` | All ad-enabled views |

---

## 4. Migration Coordination Protocol (SQLDelight)

### Single Source of Truth
- **Files:** `commonMain/sqldelight/schema.sq` + `commonMain/sqldelight/migrations/*.sqm`
- **Runner:** SQLDelight `DatabaseDriver` runs migrations in order within transaction
- **Atomicity:** Each `.sqm` migration runs in a transaction
- **Version:** All modules target `PRAGMA user_version = 2`
- **Idempotency:** Migrations use `CREATE TABLE IF NOT EXISTS`, `INSERT OR IGNORE`, safe `ALTER TABLE`

### Execution Order (Enforced in `DatabaseDriverFactory`)
```kotlin
// DatabaseDriverFactory.kt - ORDER IS CRITICAL
val migrations = listOf(
    "v2_catalog_inventory.sqm",
    "v2_sales_checkout.sqm", 
    "v2_financial_accumulators.sqm",
    "v2_expenses_cashflow.sqm",
    "v2_system_settings.sqm"
)
```

### Coordination Rules
1. **catalog-inventory** runs first — creates `products`, `categories` needed by sales
2. **sales-checkout** runs second — creates `sales_transactions`, `transaction_details`
3. **financial-accumulators** runs third — renames `resumen_caja` → `financial_accumulators`
4. **expenses-cashflow** runs fourth — creates `expense_categories`, adds expense index on `sales_transactions`
5. **system-settings** runs last — creates `settings` singleton, migrates DataStore/Preferences

### Conflict Avoidance
- `financial-accumulators` owns `resumen_caja` → `financial_accumulators` rename
- `expenses-cashflow` migration **comments out** accumulator rename (coordination)
- Shared `user_version = 2` updated once at end of all migrations
- If any migration fails → full rollback via backup restore

---

## 5. AdWare Governance Matrix

| View | Module | AdBanner Allowed? | Implementation |
|------|--------|-------------------|----------------|
| Catalog / Inventory List | catalog-inventory | ✅ Yes | `LazyColumn` footer item when `adEnabled` |
| Expenses List | expenses-cashflow | ✅ Yes | `LazyColumn` footer item when `adEnabled` |
| Caja (Cash Summary) | financial-accumulators | ✅ Yes | `LazyColumn` footer item |
| Movimientos (Transactions) | financial-accumulators | ✅ Yes | `LazyColumn` footer item |
| **Checkout / Cart** | sales-checkout | ❌ No | `adEnabled` ignored |
| **Payment Dialog** | sales-checkout | ❌ No | `adEnabled` ignored |
| **Receipt Preview** | sales-checkout | ❌ No | `adEnabled` ignored |
| **Forms (create/edit)** | all | ❌ No | `adEnabled` ignored |
| **ConfirmDialog** | shared | ❌ No | `adEnabled` ignored |
| **PIN Gate / Lock Screen** | system-settings | ❌ No | `adEnabled` ignored |
| **Settings Screen** | system-settings | ❌ No | `adEnabled` ignored |

**Master Toggle:** `settings.ad_enabled` (SQLite) → `SettingsDataStore.adEnabled` (DataStore) → `AdEnabledProvider` (CompositionLocal)

---

## 6. Testing Strategy (NFR-10: 100% Coverage Targets)

| Layer | Framework | Scope | Gate |
|-------|-----------|-------|------|
| Unit (Domain) | Kotlin Test (JUnit5) | 100% functions/branches on VOs, Rules, Entities | Required |
| Integration (Data) | Kotlin Test + SQLDelight | 100% repo methods, use cases, atomic flows | Required |
| Migration | Kotlin Test + SQLDelight | v1→v2 zero-data-loss, idempotent, rollback | Required |
| ViewModels | Kotlin Test + Turbine | 100% ViewModel logic (state flows) | Required |
| Compose UI | Compose Test + Robolectric | 100% render/interaction | Required |
| E2E | Maestro / Compose UI Test | Critical flows (checkout, PIN, backup, ad toggle) | Required |
| CI Gate | All suites + `./gradlew detekt` + `./gradlew compileKotlin` | Must pass for merge | Required |

### Test Directory Structure
```
commonTest/
├── unit/
│   ├── system-settings/        # Currency, Money, UserSettings, SettingsRules
│   ├── catalog-inventory/      # Product, Category, Stock, SKU, StockRules
│   ├── sales-checkout/         # CartItem, SaleTransaction, CartRules, ChangeAmount
│   ├── expenses-cashflow/      # Expense, ExpenseCategory, ExpenseRules
│   └── financial-accumulators/ # FinancialSummary, PeriodSummary
├── integration/
│   ├── system-settings/        # GetSettings, UpdateSettings, VerifyPin, SetPin, Backup
│   ├── catalog-inventory/      # SqlDelightProductRepository, ProductManagement
│   ├── sales-checkout/         # RegisterSale, RegisterPurchase, RevertTransaction
│   ├── expenses-cashflow/      # RegisterExpense, ReverseTransaction (EXPENSE)
│   ├── financial-accumulators/ # GetCashSummary, GetPeriodSummary, UpdateAccumulator
│   └── security/               # PIN gate, constant-time verify
├── migration/
│   ├── catalog-inventory/
│   ├── sales-checkout/
│   ├── expenses-cashflow/
│   ├── financial-accumulators/
│   └── system-settings/
├── atomic/
│   ├── SaleAtomicRollbackTest.kt
│   ├── PurchaseAtomicRollbackTest.kt
│   ├── ExpenseAtomicRollbackTest.kt
│   ├── AccumulatorAtomicRollbackTest.kt
│   └── ReverseAtomicRollbackTest.kt
├── viewmodels/                 # ProductsViewModel, CartViewModel, CashSummaryViewModel...
└── ui/                         # ProductGridScreen, CartScreen, CashBalanceCard...

androidTest/
├── e2e/                        # checkoutFlow, pinChangeFlow, backupFlow, adEnabledFlow...
└── compose/                    # Compose UI tests on device
```

---

## 7. Risk Register & Mitigations

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| `ALTER TABLE` unsupported (SQLite < 3.25) | Medium | High | Detect version at runtime; fallback to table copy in `.sqm` |
| Migration order conflicts | Low | Critical | Single SQLDelight driver with explicit ordering; integration test full sequence |
| Fractional stock breaks integer-only UI | Medium | Medium | `TextField` with `KeyboardOptions(keyboardType = KeyboardType.Decimal)` |
| Atomic transaction partial failure | Low | Critical | Single transaction block; failure injection tests |
| AdBanner appears in forbidden views | Low | High | CI grep checks on every PR; code review checklist |
| i18n keys missing in Compose | Medium | Medium | CI check: `grep -r "stringResource" --include="*.kt" composeApp/` |
| Migration long-running on large DB | Low | Medium | Background execution with progress; timeout guard (<5s expected) |
| Accumulator drift on reversal | Low | Critical | Integration test: create + revert → assert balance = 0 |
| PIN hash timing attack | Low | High | Constant-time comparison in `VerifyPin`; unit test verifies |
| Currency formatter mismatch | Low | Medium | Unit test asserts `format()` matches `NumberFormat` per locale |
| Compose Multiplatform iOS rendering differences | Medium | Medium | Test on both platforms; use `expect/actual` for platform specifics |

---

## 8. Master Checklist (Definition of Done — Global)

### Architecture & Code Quality
- [ ] All 5 modules follow Clean Architecture layer boundaries (Domain → Application → Infrastructure → Presentation)
- [ ] Zero Spanish identifiers in code/DB; zero English strings in UI (i18n only via `stringResource`)
- [ ] All public APIs have explicit Kotlin types
- [ ] All entities/DTOs use `data class` + `val` (immutable)
- [ ] Domain layer (`commonMain/domain`) has zero external dependencies
- [ ] All write use cases use SQLDelight transaction blocks for atomicity

### Database & Migration
- [ ] All 5 migrations run in order, atomic, share `user_version = 2`
- [ ] Migration v1→v2 zero data loss verified on seeded production data
- [ ] Migration idempotent: run twice → no errors, no duplication
- [ ] Migration rollback via backup restore verified
- [ ] All tables use English `snake_case` columns, soft delete (`deleted_at`), partial indexes

### Domain Layer
- [ ] Shared VOs (`Currency`, `Money`, `Stock`, `SKU`, `PaymentMethod`, `ChangeAmount`) used everywhere
- [ ] Domain rules (`StockRules`, `CartRules`, `ExpenseRules`, `SettingsRules`) centralized in `commonMain`
- [ ] All entities use English identifiers, VOs in signatures

### Application Layer
- [ ] `RegisterSale`/`RegisterPurchase`/`RegisterExpense` atomic with stock + accumulator
- [ ] `ReverseTransaction` handles SALE/PURCHASE/EXPENSE with safety checks
- [ ] `UpdateAccumulator` single atomic UPDATE with all deltas
- [ ] `GetCashSummary` O(1), `GetPeriodSummary` correct aggregation
- [ ] PIN gate on critical actions (change PIN, delete transaction, restore backup)

### Infrastructure Layer
- [ ] SQLDelight repositories implement Domain interfaces (Ports)
- [ ] Image pipeline: 400×400, JPEG 70%, <40KB verified (`expect/actual` ImageOptimizer)
- [ ] SecureStore for PIN hash (Android Keystore / iOS Keychain), Biometric auth
- [ ] Backup: Export/Restore with PIN gate
- [ ] AdBanner Composable respects `AD_ENABLED` flag via `CompositionLocal`

### Presentation Layer
- [ ] All UI strings via `stringResource` (Spanish only in `composeResources/values-es/`)
- [ ] `LazyColumn` / `LazyVerticalGrid` virtualized, `columns = 3` (catalog), `columns = 1` (lists)
- [ ] ViewModels with `StateFlow` / `MutableStateFlow`, granular UI state
- [ ] Composables memoized via `remember` / `derivedStateOf` / `key`
- [ ] AdBanner only in 4 allowed views, absent in 8+ forbidden views

### AdWare Governance
- [ ] `settings.ad_enabled` master toggle in SQLite
- [ ] `SettingsDataStore.adEnabled` hydrated at startup
- [ ] `AdEnabledProvider` CompositionLocal consumed by allowed views
- [ ] CI grep checks enforce forbidden views clean

### Testing & CI
- [ ] Unit tests: 100% functions/branches on Domain layer
- [ ] Integration tests: 100% repo methods, use cases, atomic flows
- [ ] Migration tests: 100% v1→v2 zero-loss, idempotent, rollback
- [ ] Atomic rollback tests: simulated DB failure → full rollback
- [ ] ViewModel tests: 100% logic coverage (Turbine)
- [ ] Compose UI tests: 100% render/interaction
- [ ] E2E tests: checkout, PIN change, backup/restore, ad toggle flows
- [ ] Kotlin strict: `./gradlew compileKotlinAndroid compileKotlinIos` zero errors
- [ ] Detekt: `./gradlew detekt` zero errors

### Performance
- [ ] LazyColumn render <16ms/frame on physical device (Android/iOS)
- [ ] Optimized images <40KB on 10 test images
- [ ] App startup <3s cold, <1s warm
- [ ] Database queries <10ms (indexed, partial indexes)

---

## 9. Timeline Summary

| Week | Focus | Deliverable |
|------|-------|-------------|
| 1 | Phase 0: Shared Kernel | VOs, Rules, SQLDelight Schema, Migrations |
| 2 | Phase 1: Catalog & Inventory | Full module v2.0.0, migration, tests |
| 3 | Phase 2: Sales & Checkout | Full module v2.0.0, migration, tests |
| 3-4 | Phase 3: Financial Accumulators | Full module v2.0.0, migration, tests |
| 4 | Phase 4: Expenses & Cash Flow | Full module v2.0.0, migration, tests |
| 5 | Phase 5: System Settings | Full module v2.0.0, migration, tests |
| 5-6 | Phase 6: Global Verification | E2E, Performance, Store Readiness |

**Total Estimated Duration:** 6 weeks (can compress with parallelization)

---

## 10. Cross-References

- Module Specs: `/docs/modules/*/spec.md`
- Module Plans: `/docs/modules/*/plan.md`
- Module Tests: `/docs/modules/*/test.md`
- Master Guidelines: `AGENTS.md`
- Architecture Diagram: `README.md` (Mermaid)

---

*Last updated: 2026-11-12 — v2.0.0 KMP Migration Global Implementation Plan*