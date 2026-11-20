# TPV Móvil Express — Global Implementation Plan v2.0.0 (KMP Migration)

## Executive Summary

This document consolidates the implementation roadmaps for all five bounded contexts into a single coordinated plan for the v2.0.0 migration to **Kotlin Multiplatform (KMP) + Compose Multiplatform + SQLDelight**. The migration enforces strict Clean Architecture + DDD layer boundaries, English-only code/DB identifiers, atomic SQLite transactions via SQLDelight, and centralized AdWare governance.

**Migration Strategy:** Single SQLDelight migration runner (`.sqm` files) with ordered, atomic migrations sharing `PRAGMA user_version = 2`. All domain logic in `commonMain`, platform-specific implementations in `androidMain`/`iosMain`.

> **Status (2026-11-19):** Phases 0–5 are implemented and compile under Gradle 9.9.1 / AGP 9.5.1. Verified gates: `:composeApp:generateCommonMainTpvDatabaseInterface`, `:composeApp:compileAndroidMain`, `:composeApp:testAndroidHostTest` (50 tests, 0 failures), `:androidApp:assembleDebug`. **Phase 6 (feature finalization) is the active scope** — see §2 Phase 6 and `/docs/MIGRATION_CHECKLIST.md`.

> **Workspace layout:** the legacy React Native/Expo codebase lives in `rn-app/`; the new Kotlin Multiplatform project lives in `kmp-app/`. Source paths in this plan may refer to `commonMain/` conceptually; the current consolidated implementation lives under `kmp-app/composeApp/src/commonMain/` (package `com.copitoB.mypos.*`), mobile-only (Android/iOS). Shared specs and plans stay in `/docs/`.

**Total Effort:** ~6 phases across 5 modules, coordinated via shared `commonMain` dependencies.

---

## 1. Module Overview & Migration Order

| Order | Module | Bounded Context | Key Tables | Migration Depends On |
|-------|--------|-----------------|------------|---------------------|
| 1 | **catalog-inventory** | Products, categories, stock, images | `products`, `categories` | — (base) |
| 2 | **sales-checkout** | Cart, sales/purchases, payment, receipts | `sales_transactions`, `transaction_details` | catalog-inventory (products) |
| 3 | **financial-accumulators** | Cash accumulator, reports, reversal | `financial_accumulators`, `sales_transactions` | sales-checkout (transactions) |
| 4 | **expenses-cashflow** | Expenses, expense categories | `sales_transactions` (type=EXPENSE), `expense_categories` | financial-accumulators (accumulator) |
| 5 | **system-settings** | Currency, pagination, PIN/biometric, backup/import | `settings` (singleton) | expenses-cashflow (last) |

**Cross-Module Contracts (Implemented Once in `commonMain`, Imported Everywhere):**
- `Currency`, `Money`, `Stock`, `SKU` → `commonMain/domain/value-objects/`
- `PaymentMethod`, `ChangeAmount`, `TransactionType` → `commonMain/domain/entities/`
- `stockRules`, `cartRules`, `expenseRules`, `settingsRules` → `commonMain/domain/rules/`
- `ReverseTransaction` use case → `commonMain/application/transactions/`

---

## 2. Phased Implementation Roadmap (KMP)

### Phase 0: Foundation — Shared Kernel Definition (Week 1)
**Goal:** Define all shared value objects, domain rules, and SQLDelight schema in `commonMain` using Kotlin idioms (value classes, immutable data classes, sealed interfaces).

| Task | File (actual in `kmp-app/composeApp/src/commonMain/`) | Module | Status |
|------|--------------------------------------------------------|--------|--------|
| Define `Currency` value class | `kotlin/com/copitoB/mypos/domain/value-objects/Currency.kt` | system-settings | [x] ✅ DONE (file present) |
| Define `Money` value class | `kotlin/com/copitoB/mypos/domain/value-objects/Money.kt` | system-settings | [x] ✅ DONE (file present) |
| Define `Stock` value class | `kotlin/com/copitoB/mypos/domain/value-objects/Stock.kt` | catalog-inventory | [x] ✅ DONE (file present) |
| Define `SKU` value class | `kotlin/com/copitoB/mypos/domain/value-objects/SKU.kt` | catalog-inventory | [x] ✅ DONE (file present) |
| Define `PaymentMethod` | `kotlin/com/copitoB/mypos/domain/value-objects/PaymentMethod.kt` | sales-checkout | [x] ✅ DONE (file present; verify sealed-class form vs spec) |
| Define `ChangeAmount` value class | `kotlin/com/copitoB/mypos/domain/value-objects/ChangeAmount.kt` | sales-checkout | [x] ✅ DONE (file present) |
| Define `TransactionType` | `kotlin/com/copitoB/mypos/domain/value-objects/TransactionType.kt` | shared | [x] ✅ DONE (file present; verify sealed-class form vs spec) |
| Define `stockRules` | `kotlin/com/copitoB/mypos/domain/rules/StockRules.kt` | catalog-inventory | [x] ✅ DONE (file present) |
| Define `cartRules` | `kotlin/com/copitoB/mypos/domain/rules/CartRules.kt` | sales-checkout | [x] ✅ DONE (file present) |
| Define `expenseRules` | `kotlin/com/copitoB/mypos/domain/rules/ExpenseRules.kt` | expenses-cashflow | [x] ✅ DONE (file present) |
| Define `settingsRules` | `kotlin/com/copitoB/mypos/domain/rules/SettingsRules.kt` | system-settings | [x] ✅ DONE (file present) |
| Create SQLDelight schema (`.sq`) | `sqldelight/schema.sq` (+ `products.sq`, `sales.sq`, `expenses.sq`, `financial_accumulators.sq`, `settings.sq`) | all | [x] ✅ DONE (files present) |
| Create SQLDelight migrations (`.sqm`) | `sqldelight/migrations/v1_0_0_to_v2_0_0.sqm` | all | [~] ⏳ PARTIAL (migration file present; per-module split/ordering as originally planned not yet verified) |
| Unit tests (VOs/rules) | `src/commonTest/kotlin/com/copitoB/mypos/domain/{value-objects,rules}/*Test.kt` | shared | [~] ⏳ PARTIAL (test sources present; execution/100% coverage not verified) |

**Gate:** ✅ VERIFIED — `:composeApp:compileAndroidMain` and `:composeApp:testAndroidHostTest` pass (50 tests, 0 failures); SQLDelight generates query APIs.
**Known follow-up:** `:composeApp:compileKotlinIosArm64` fails on `java.math.BigDecimal` / `java.text.NumberFormat` in `commonMain` and Koin's `GlobalContext` in `App.kt`. Meets no Phase 6 requirement but blocks iOS shipping; tracked in `/docs/MIGRATION_CHECKLIST.md`.

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
- [ ] AdBanner renders in Inventory list footer only in the `free` flavor
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
- [ ] AdBanner renders in Caja/Movimientos only in the `free` flavor, absent in detail/PIN
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
- [ ] AdBanner in list footer only in the `free` flavor
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
| Use cases: `GetSettings`, `UpdateSettings`, `VerifyPin`, `SetPin`, `ExportBackupUseCase`, `ImportBackupUseCase` | `commonMain/application/` | 🆕 |
| ViewModels: `SettingsViewModel`, `SecurityViewModel`, `BackupViewModel` | `commonMain/presentation/viewmodels/` | 🆕 |
| Compose UI: `CurrencySelector`, `PageSizeInput`, `PinForm`, `BiometricToggle`, `BackupActions`, `FlavorInfoCard` | `composeApp/src/commonMain/compose/` | 🆕 |
| i18n: `composeResources/values-es/strings.xml` (settings + lock + pin + biometric + toast) | `composeApp/src/commonMain/composeResources/` | 🆕 |
| Tests: Unit + Integration + Migration + PIN Gate + UI | `commonTest/`, `androidTest/` | 🆕 |

**Definition of Done (Settings):**
- [ ] Migration creates `settings` singleton, migrates DataStore/Preferences on first `GetSettings`
- [ ] `Currency`/`Money` VOs used globally across all modules
- [ ] PIN change requires current PIN (except first time), constant-time compare
- [ ] Biometric toggle requires auth before enabling
- [ ] Restore backup requires PIN, reloads app, data intact
- [ ] Ads ship only via the `free` flavor (`ADS_ENABLED` build flag), never as a user setting
- [ ] All 60+ i18n keys implemented
- [ ] All tests pass, Kotlin strict, Detekt clean

---

### Phase 6: Feature Finalization & Parity Closure (Week 5-6)
**Goal:** Close every remaining RN → KMP parity gap, ship the compile-time flavor model, and land the versioned JSON backup pipeline. Full tracker: `/docs/MIGRATION_CHECKLIST.md`.

**Entry gate:** `:composeApp:testAndroidHostTest` green (50+ tests, 0 failures) and `:androidApp:assembleDebug` producing an APK.

#### 6.1 Products — Edit & Soft-Delete
| Task | Target |
|------|--------|
| Add `updateProduct(input)` / `deleteProduct(id)` to `ProductViewModel` | `presentation/viewmodel/ProductViewModel.kt` |
| Wire `ProductFormScreen(product = …)` for edit mode; pass existing entity from list | `App.kt`, `presentation/ui/common/ProductFormScreen.kt` |
| Soft-delete entry point + `ConfirmDialog` ("Eliminar producto") | `ProductListScreen.kt` |
| Category selector + quick-create dialog in the form | `ProductFormScreen.kt` |
| SKU duplicate error surfaced inline | `ProductViewModel` → `ProductManagement.createProduct` |

#### 6.2 Inventory — Stock Adjustments & Low-Stock Filters
| Task | Target |
|------|--------|
| `StockAdjustDialog` (delta + reason) calling `ProductManagement.adjustStock` | new `presentation/ui/components/StockAdjustDialog.kt` |
| Low-stock filter chip backed by `listLowStock()` | `ProductListScreen.kt`, `ProductViewModel` |
| 3-column `LazyVerticalGrid` with stable keys | `ProductListScreen.kt` |
| Search (debounced), category chips, sort `NAME_ASC` / `POPULARITY_DESC` | `ProductListScreen.kt`, `ProductViewModel` |
| Infinite scroll honoring `settings.pageSize` | `ProductViewModel.loadMore()` |
| Post-commit stock alert Snackbar | `presentation/ui/common/ProductListScreen.kt` |
| Image pipeline `expect`/`actual` (400×400, JPEG 70%, <40 KB) | `androidMain`/`iosMain` `ImageOptimizer` |

#### 6.3 Purchases — `RegisterPurchase` UI
| Task | Target |
|------|--------|
| `RegistroScreen` with Venta/Compra segmented control | new `presentation/ui/common/RegistroScreen.kt` |
| `PurchaseCartViewModel` mirroring `CartViewModel` over `RegisterPurchase` | `presentation/viewmodel/PurchaseCartViewModel.kt` |
| Reuse `CartScreen` for both carts (parameterized by cart type) | `CartScreen.kt` |
| Purchase receipt (HTML) + share | `expect`/`actual` `ReceiptPrinter` |

#### 6.4 Reports — Date Range, PaymentMethod Breakdown, Sale Details
| Task | Target |
|------|--------|
| `ReportsScreen` with date-range picker (month / year / custom) | new `presentation/ui/common/ReportsScreen.kt` |
| `salesSumByPaymentMethod` SQLDelight query + repository method | `sales.sq`, `SqliteAccumulatorRepository` |
| `PeriodSummary` extended with per-`PaymentMethod` totals | `domain/entities/PeriodSummary.kt`, `GetPeriodSummary` |
| `MovementsScreen` (search + type chips + infinite scroll) wired to existing `MovementsViewModel` | new `presentation/ui/common/MovementsScreen.kt` |
| `TransactionDetailScreen` with line items and revert entry | new `presentation/ui/common/TransactionDetailScreen.kt` |
| Revert flow guarded by PIN | `RevertTransaction` + `PinLockOverlay` |

#### 6.5 Settings & PIN
| Task | Target |
|------|--------|
| `PinViewModel` (`verify`, `set`, `change`, lock state) | `presentation/viewmodel/PinViewModel.kt` |
| `PinLockOverlay` + app-start lock when a PIN hash exists | new `presentation/ui/common/PinLockOverlay.kt`, `App.kt` |
| Change-PIN form (current / new / confirm, 4–6 digits) | new `presentation/ui/common/ChangePinScreen.kt` |
| Biometric `expect`/`actual` unlock | `androidMain`/`iosMain` |
| Currency picker (`Bs` / `$` / `€`) and page-size input (5–100) | `SettingsScreen.kt` |
| Backup section: export / import / legacy `.db` import | `SettingsScreen.kt`, `BackupViewModel` |
| Remove `onAdEnabledChange` switch → read-only flavor card | `SettingsScreen.kt`, `UserSettings` |

#### 6.6 Multiplatform i18n
| Task | Target |
|------|--------|
| Enable Compose resources (`compose.components.resources` dependency) | `composeApp/build.gradle.kts` |
| Create `values-es/strings.xml` with all ~60 keys across namespaces | `composeApp/src/commonMain/composeResources/values-es/strings.xml` |
| Replace every hardcoded Spanish literal with `stringResource(Res.string.…)` | all `presentation/ui/**` |
| CI guard against new hardcoded UI literals | `scripts/` |

#### 6.7 Build Flavors & Backup Pipeline
| Task | Target |
|------|--------|
| `free` / `pro` product flavors, `ADS_ENABLED` BuildConfig field | `androidApp/build.gradle.kts` |
| Shared `signingConfig` from gitignored `keystore.properties`, same `applicationId` | `androidApp/build.gradle.kts` |
| Shared signing; flavor drives a Gradle property that generates the ads constant (library cannot read app `BuildConfig`) | `androidApp/build.gradle.kts`, `composeApp/build.gradle.kts` |
| `expect fun isAdsEnabled()` + `LocalAdsEnabled`; delete runtime toggle path | `commonMain`/`androidMain`, `App.kt` |
| `ExportBackupUseCase` → versioned JSON (`schemaVersion: 2`) | `application/backup/ExportBackupUseCase.kt` |
| `ImportBackupUseCase` → `BackupSource.Json` + `BackupSource.LegacySqliteFile` | `application/backup/ImportBackupUseCase.kt` |
| Enum translation (`VENTA→SALE`, `EFECTIVO→CASH`, …) + timestamp normalization | `application/backup/LegacyMapper.kt` |
| Safe defaults (`sku = null`, `categories.updated_at = registered_at`, default expense category) | `LegacyMapper.kt` |
| Single-transaction import + `financial_accumulators` recomputation | `ImportBackupUseCase.kt` |
| `BackupViewModel` + PIN-gated UI | `presentation/viewmodel/BackupViewModel.kt` |

**Definition of Done (Phase 6):**
- [ ] Every `[ ]` item in `/docs/MIGRATION_CHECKLIST.md` is closed or explicitly deferred with a rationale
- [ ] `free` and `pro` flavors build; `pro` contains no ad code path; both share `applicationId` + `.jks`
- [ ] Legacy `rn-app` `.db` file imports into KMP with zero row loss and recomputed accumulators
- [ ] JSON export → wipe → import round-trip is idempotent and atomic
- [ ] Zero hardcoded Spanish literals remain in `presentation/`
- [ ] PIN gate protects change-PIN, revert, and restore

---

### Phase 7: Global Verification & Hardening (Week 6)
**Goal:** End-to-end integration, performance, store readiness.

| Task | Description |
|------|-------------|
| Full migration run v1→v2 on seeded production-like data | Verify zero data loss across all 5 modules |
| Migration idempotency: run twice → no errors, no duplication | All 5 module migration tests |
| Backup round-trip: JSON export → wipe → import → assert counters | `ImportBackupUseCase` integration test |
| Legacy import: `rn-app` `.db` fixture → assert row counts + translated enums | `LegacyMapper` test |
| Cross-module integration flows | Sale → Accumulator → Caja → Reversal → Stock restore |
| AdWare governance audit | CI grep: no ad code in checkout/forms/settings |
| i18n completeness audit | CI check for hardcoded Spanish strings in Compose |
| Performance: LazyColumn <16ms/frame, image <40KB | Physical device testing (Android/iOS) |
| PIN gate on critical actions | Change PIN, revert transaction, restore backup |
| E2E critical flows | Checkout, purchase, PIN change, backup/restore, flavor install |
| Kotlin strict build | `./gradlew :composeApp:compileAndroidMain` zero warnings |
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
| `AdBanner` Composable | `composeApp/src/commonMain/kotlin/.../presentation/ui/components/ads/` | Catalog, Expenses, Financial |
| `isAdsEnabled()` / `LocalAdsEnabled` | `commonMain` + `androidMain` (`BuildConfig.ADS_ENABLED`) | All ad-enabled views |
| `ExportBackupUseCase` / `ImportBackupUseCase` | `commonMain/application/backup/` | System settings, migration tooling |
| `LegacyMapper` (enum/timestamp translation) | `commonMain/application/backup/` | Importing `rn-app` data |

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

## 5. AdWare Governance Matrix (Compile-Time Flavors)

Ads are **not** a runtime setting. The `free` flavor compiles with `ADS_ENABLED = true`; the `pro` flavor compiles with `ADS_ENABLED = false`. Both share `applicationId` and the same `.jks`, so upgrades preserve local SQLite data.

| View | Module | Ads in `free`? | Implementation |
|------|--------|----------------|----------------|
| Catalog / Inventory List | catalog-inventory | ✅ Yes | Footer item in `LazyVerticalGrid` when `isAdsEnabled()` |
| Expenses List | expenses-cashflow | ✅ Yes | Footer item in `LazyColumn` when `isAdsEnabled()` |
| Caja (Cash Summary) | financial-accumulators | ✅ Yes | Footer item when `isAdsEnabled()` |
| Movimientos (Transactions) | financial-accumulators | ✅ Yes | Footer item when `isAdsEnabled()` |
| **Checkout / Cart** | sales-checkout | ❌ No | Compiles into `pro`-only path; no `AdBanner` import |
| **Purchases flow** | sales-checkout | ❌ No | No ad code path |
| **Payment Dialog** | sales-checkout | ❌ No | No ad code path |
| **Receipt Preview** | sales-checkout | ❌ No | No ad code path |
| **Forms (create/edit)** | all | ❌ No | No ad code path |
| **ConfirmDialog** | shared | ❌ No | No ad code path |
| **PIN Gate / Lock Screen** | system-settings | ❌ No | No ad code path |
| **Settings Screen** | system-settings | ❌ No | Read-only flavor info card |

**Flag chain:** `BuildConfig.ADS_ENABLED` (androidApp) → `expect fun isAdsEnabled()` (`androidMain`) → `LocalAdsEnabled` CompositionLocal (`commonMain`) → `if (LocalAdsEnabled.current) AdBanner()`.

**Deprecated:** `settings.ad_enabled` is no longer read or written by the UI. The column is retained until `v3.0.0` to avoid a mid-phase schema migration and is dropped by a dedicated `.sqm` afterwards.

---

## 6. Testing Strategy (NFR-10: 100% Coverage Targets)

| Layer | Framework | Scope | Gate |
|-------|-----------|-------|------|
| Unit (Domain) | Kotlin Test (JUnit5) | 100% functions/branches on VOs, Rules, Entities | Required |
| Integration (Data) | Kotlin Test + SQLDelight | 100% repo methods, use cases, atomic flows | Required |
| Migration | Kotlin Test + SQLDelight | v1→v2 zero-data-loss, idempotent, rollback | Required |
| ViewModels | Kotlin Test + Turbine | 100% ViewModel logic (state flows) | Required |
| Compose UI | Compose Test + Robolectric | 100% render/interaction | Required |
| E2E | Maestro / Compose UI Test | Critical flows (checkout, purchase, PIN, backup/restore, flavor install) | Required |
| CI Gate | All suites + `./gradlew detekt` + `./gradlew :composeApp:compileAndroidMain` | Must pass for merge | Required |

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
├── e2e/                        # checkoutFlow, purchaseFlow, pinChangeFlow, backupFlow, flavorInstallFlow...
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
| AdBanner appears in forbidden views | Low | High | CI grep checks on every PR; `pro` flavor compiles ads out entirely |
| Import corrupts data | Low | Critical | Single transaction + row-count assertions + rollback; `.db` fixture tests |
| Flavor upgrade wipes data | Low | Critical | Identical `applicationId` + shared `.jks`; no `applicationIdSuffix` |
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
- [ ] Backup: `ExportBackupUseCase` (JSON v2) + `ImportBackupUseCase` (JSON + legacy `.db`) with PIN gate and single-transaction import
- [ ] Legacy import translates RN enums (`VENTA/COMPRA/GASTO`, `EFECTIVO/…`) and recomputes `financial_accumulators`
- [ ] Ads are compile-time only: `free` flavor ships banners, `pro` contains no ad code path

### Presentation Layer
- [ ] All UI strings via `stringResource` (Spanish only in `composeResources/values-es/`)
- [ ] `LazyColumn` / `LazyVerticalGrid` virtualized, `columns = 3` (catalog), `columns = 1` (lists)
- [ ] ViewModels with `StateFlow` / `MutableStateFlow`, granular UI state
- [ ] Composables memoized via `remember` / `derivedStateOf` / `key`
- [ ] AdBanner only in 4 allowed views, absent in 8+ forbidden views

### AdWare Governance
- [ ] `free` / `pro` flavors declared with `ADS_ENABLED` build flags
- [ ] Shared `.jks` + identical `applicationId`; no `applicationIdSuffix`
- [ ] `isAdsEnabled()` `expect`/`actual` + `LocalAdsEnabled` CompositionLocal consumed by allowed views
- [ ] Deprecated `settings.ad_enabled` column unread until its `v3.0.0` removal migration
- [ ] CI grep checks enforce forbidden views clean

### Testing & CI
- [ ] Unit tests: 100% functions/branches on Domain layer
- [ ] Integration tests: 100% repo methods, use cases, atomic flows
- [ ] Migration tests: 100% v1→v2 zero-loss, idempotent, rollback
- [ ] Atomic rollback tests: simulated DB failure → full rollback
- [ ] ViewModel tests: 100% logic coverage (Turbine)
- [ ] Compose UI tests: 100% render/interaction
- [ ] E2E tests: checkout, purchase, PIN change, backup/restore, flavor install flows
- [ ] Kotlin strict: `./gradlew :composeApp:compileAndroidMain` zero errors
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
| 5-6 | Phase 6: Feature Finalization | Products edit, inventory, purchases, reports, PIN, i18n, flavors, backup JSON |
| 6 | Phase 7: Global Verification | E2E, performance, store readiness |

**Total Estimated Duration:** 6 weeks (can compress with parallelization)

---

## 10. Cross-References

- **Feature parity tracker:** `/docs/MIGRATION_CHECKLIST.md`
- Module Specs: `/docs/modules/*/spec.md`
- Module Plans: `/docs/modules/*/plan.md`
- Module Tests: `/docs/modules/*/test.md`
- Master Guidelines: `AGENTS.md`
- Architecture Diagram: `README.md` (Mermaid)

---

*Last updated: 2026-11-19 — v2.0.0 Phase 6 finalization scope*