# TPV Móvil Express — Global Implementation Plan v2.0.0

## Executive Summary

This document consolidates the implementation roadmaps for all five bounded contexts into a single coordinated plan for the v2.0.0 migration. The migration enforces strict Clean Architecture + DDD layer boundaries, English-only code/DB identifiers, atomic SQLite transactions, and centralized AdWare governance.

**Migration Strategy:** Single `migrations.ts` runner with ordered, atomic migrations sharing `PRAGMA user_version = 2`.

**Total Effort:** ~6 phases across 5 modules, coordinated via shared dependencies.

---

## 1. Module Overview & Migration Order

| Order | Module | Bounded Context | Key Tables | Migration Depends On |
|-------|--------|-----------------|------------|---------------------|
| 1 | **catalog-inventory** | Products, categories, stock, images | `products`, `categories` | — (base) |
| 2 | **sales-checkout** | Cart, sales/purchases, payment, receipts | `sales_transactions`, `transaction_details` | catalog-inventory (products) |
| 3 | **financial-accumulators** | Cash accumulator, reports, reversal | `financial_accumulators`, `sales_transactions` | sales-checkout (transactions) |
| 4 | **expenses-cashflow** | Expenses, expense categories | `sales_transactions` (type=EXPENSE), `expense_categories` | financial-accumulators (accumulator) |
| 5 | **system-settings** | Currency, pagination, PIN/biometric, backup, AD_ENABLED | `settings` (singleton) | expenses-cashflow (last) |

**Cross-Module Contracts (Must Implement Once, Import Everywhere):**
- `Currency`, `Money`, `Stock`, `SKU` → `src/domain/value-objects/`
- `PaymentMethod`, `ChangeAmount`, `TransactionType` → `src/domain/entities/`
- `stockRules`, `cartRules`, `expenseRules`, `settingsRules` → `src/domain/rules/`
- `ReverseTransaction` use case → `src/application/transactions/`

---

## 2. Phased Implementation Roadmap

### Phase 0: Foundation (Week 1) — Shared Kernel
**Goal:** Establish shared value objects, domain rules, and migration infrastructure.

| Task | File | Module | Status |
|------|------|--------|--------|
| Create `Currency.ts` | `src/domain/value-objects/Currency.ts` | system-settings | 🆕 |
| Create `Money.ts` | `src/domain/value-objects/Money.ts` | system-settings | 🆕 |
| Create `Stock.ts` | `src/domain/value-objects/Stock.ts` | catalog-inventory | 🆕 |
| Create `SKU.ts` | `src/domain/value-objects/SKU.ts` | catalog-inventory | 🆕 |
| Create `PaymentMethod.ts` | `src/domain/entities/PaymentMethod.ts` | sales-checkout | 🆕 |
| Create `ChangeAmount.ts` | `src/domain/value-objects/ChangeAmount.ts` | sales-checkout | 🆕 |
| Create `TransactionType.ts` | `src/domain/entities/TransactionType.ts` | shared | 🆕 |
| Create `stockRules.ts` | `src/domain/rules/stockRules.ts` | catalog-inventory | 🆕 |
| Create `cartRules.ts` | `src/domain/rules/cartRules.ts` | sales-checkout | 🆕 |
| Create `expenseRules.ts` | `src/domain/rules/expenseRules.ts` | expenses-cashflow | 🆕 |
| Create `settingsRules.ts` | `src/domain/rules/settingsRules.ts` | system-settings | 🆕 |
| Update `migrations.ts` runner | `src/data/database/migrations.ts` | all | 🔄 |
| Update `schema.sql.ts` target | `src/data/database/schema.sql.ts` | all | 🔄 |
| Update `rowMappers.ts` | `src/data/mappers/rowMappers.ts` | all | 🔄 |

**Gate:** All shared VOs compile, unit tests pass (100% coverage), migration runner executes ordered transactions.

---

### Phase 1: Catalog & Inventory (Week 2)
**Depends on:** Phase 0 complete

| Task | File | Status |
|------|------|--------|
| Update `Product.ts` / `Category.ts` entities (English) | `src/domain/entities/` | 🔄 |
| Migration v2: `v2_0_0_catalog_migration.sql` | `src/data/database/migrations/` | 🆕 |
| Register migration v2 in `migrations.ts` | `src/data/database/migrations.ts` | 🔄 |
| Update repositories: `IProductRepository`, `SqliteProductRepository`, `ICategoryRepository`, `SqliteCategoryRepository` | `src/data/repositories/` | 🔄 |
| Use case: `ProductManagement.ts` | `src/application/products/` | 🔄 |
| Verify ImageOptimizer/ImageStorage (400×400, JPEG 70%) | `src/infrastructure/image/` | ✅ |
| Hooks: `useProducts`, `useCategories`, `useStockAlert` | `src/presentation/hooks/` | 🔄 |
| Store: `productDraftStore` (add `sku`) | `src/presentation/stores/` | 🔄 |
| UI Components: `SKUInput`, `AdBanner` | `src/presentation/components/` | 🆕 |
| i18n: `es/catalog.json` (45 keys) | `src/presentation/i18n/es/` | 🆕 |
| Screens: `products.tsx`, `new.tsx`, `[id].tsx` | `app/` | 🔄 |
| Tests: Unit + Integration + Migration + UI + E2E | `tests/` | 🆕 |

**Definition of Done (Catalog):**
- [ ] Migration v1→v2 zero data loss, idempotent, rollback verified
- [ ] All entities/repos/use cases use English identifiers + VOs
- [ ] Fractional stock (3dp) works end-to-end
- [ ] SKU unique validated, category required enforced
- [ ] AdBanner renders in Inventory list footer when `AD_ENABLED`
- [ ] Stock alert Toast fires post-commit when `currentStock < minStock`
- [ ] Zero hardcoded Spanish strings in catalog components
- [ ] All tests pass, TypeScript strict, Lint clean

---

### Phase 2: Sales & Checkout (Week 3)
**Depends on:** Phase 0 + Phase 1 (products exist)

| Task | File | Status |
|------|------|--------|
| Entities: `SaleTransaction.ts`, `TransactionDetail.ts`, `CartItem.ts` (Composite) | `src/domain/entities/` | 🔄/🆕 |
| VOs: `PaymentMethod`, `ChangeAmount` (shared) | `src/domain/value-objects/` | 🆕 |
| Domain rules: `cartRules.ts` (5 functions) | `src/domain/rules/` | 🆕 |
| Migration v2: `v2_0_0_sales_migration.sql` | `src/data/database/migrations/` | 🆕 |
| Repositories: `ISaleRepository`, `SqliteSaleRepository` | `src/data/repositories/` | 🔄 |
| Use cases: `RegisterSale`, `RegisterPurchase`, `RevertTransaction` | `src/application/` | 🆕 |
| Stores: `saleCartStore`, `purchaseCartStore` (granular selectors) | `src/presentation/stores/` | 🔄/🆕 |
| Hooks: `useCart`, `useTransactions`, `useStockAlert` | `src/presentation/hooks/` | 🔄 |
| Components: `CartLine`, `CartSummaryBar`, `LineItemModal`, `PaymentModal`, `ReceiptPreview` | `src/presentation/components/cart/` | 🔄/🆕 |
| AdWare removal from checkout screens | `app/(tabs)/registro.tsx`, components | 🔄 |
| i18n: `es/checkout.json` (45+ keys) | `src/presentation/i18n/es/` | 🆕 |
| Tests: Unit + Integration + Migration + Atomic + UI + E2E | `tests/` | 🆕 |

**Definition of Done (Sales):**
- [ ] Migration preserves all transaction data, coordinates with catalog
- [ ] Composite Pattern: `ICartItem` + `SimpleCartItem` + Factory
- [ ] `RegisterSale`/`RegisterPurchase` atomic: stock + accumulator + transaction
- [ ] `PaymentModal` calculates change correctly (green ≥0, red <0)
- [ ] `RevertTransaction` handles SALE/PURCHASE/EXPENSE with stock safety
- [ ] Zero AdBanner in checkout, payment, receipt screens
- [ ] All 45+ i18n keys implemented
- [ ] Atomic rollback tests pass (simulated DB failure mid-transaction)
- [ ] All tests pass, TypeScript strict, Lint clean

---

### Phase 3: Financial Accumulators (Week 3-4, parallel with Phase 2 after migration)
**Depends on:** Phase 2 migration complete (shared `sales_transactions`)

| Task | File | Status |
|------|------|--------|
| Entities: `FinancialSummary.ts`, `PeriodSummary.ts`, `Transaction.ts` | `src/domain/entities/` | 🔄/🆕 |
| Migration v2: `v2_0_0_financial_accumulators_migration.sql` | `src/data/database/migrations/` | 🆕 |
| Repository: `IAccumulatorRepository`, `SqliteAccumulatorRepository` | `src/data/repositories/` | 🆕 |
| Use cases: `GetCashSummary`, `GetPeriodSummary`, `UpdateAccumulator`, `ReverseTransaction` (shared) | `src/application/accumulators/` | 🆕 |
| Hooks: `useCashSummary`, `usePeriodSummary`, `useMovements`, `usePinGate` | `src/presentation/hooks/` | 🔄/🆕 |
| Components: `CashBalanceCard`, `StatCard`, `PeriodBreakdown`, `ReportRow`, `MovementListItem`, `TypeFilterChips` | `src/presentation/components/` | 🔄 |
| AdBanner in Caja/Movimientos footers | `app/(tabs)/caja.tsx`, `app/movimientos/index.tsx` | 🔄 |
| i18n: `es/accumulator.json` (40+ keys) | `src/presentation/i18n/es/` | 🆕 |
| Tests: Unit + Integration + Migration + Atomic + UI + E2E | `tests/` | 🆕 |

**Definition of Done (Accumulators):**
- [ ] Migration renames `resumen_caja` → `financial_accumulators` atomically
- [ ] All entities/use cases use `Money` VOs, English identifiers
- [ ] `GetCashSummary` O(1) read, `GetPeriodSummary` correct aggregation
- [ ] `UpdateAccumulator` atomic single UPDATE with all deltas
- [ ] Reversal restores accumulator + stock (PURCHASE pre-check)
- [ ] AdBanner renders in Caja/Movimientos when `AD_ENABLED`, absent in detail/PIN
- [ ] All tests pass, TypeScript strict, Lint clean

---

### Phase 4: Expenses & Cash Flow (Week 4)
**Depends on:** Phase 3 complete (accumulator exists)

| Task | File | Status |
|------|------|--------|
| Entities: `Expense.ts`, `ExpenseCategory.ts` | `src/domain/entities/` | 🆕 |
| Domain rules: `expenseRules.ts` (3 functions) | `src/domain/rules/` | 🆕 |
| Migration v2: `v2_0_0_expenses_migration.sql` (creates `expense_categories`, coordinates accumulator) | `src/data/database/migrations/` | 🆕 |
| Repository: `IExpenseRepository`, `SqliteExpenseRepository` | `src/data/repositories/` | 🆕 |
| Use cases: `RegisterExpense`, `ReverseTransaction` (EXPENSE) | `src/application/expenses/` | 🆕 |
| Stores: `expenseDraftStore` (preserves draft during category creation) | `src/presentation/stores/` | 🆕 |
| Hooks: `useExpenses`, `useExpenseCategories`, `useCurrency`, `useInfiniteScroll` | `src/presentation/hooks/` | 🔄/🆕 |
| Components: `ConfirmDialog`, `ExpenseListItem`, `CategorySelector`, `QuickCategoryModal`, `AdBanner` | `src/presentation/components/` | 🔄/🆕 |
| AdBanner in Expenses list footer only | `app/gastos/index.tsx` | 🔄 |
| i18n: `es/expenses.json` (45+ keys) | `src/presentation/i18n/es/` | 🆕 |
| Tests: Unit + Integration + Migration + Atomic + UI + E2E | `tests/` | 🆕 |

**Definition of Done (Expenses):**
- [ ] Migration creates `expense_categories`, adds expense index, coordinates accumulator
- [ ] `RegisterExpense` atomic: transaction + accumulator update
- [ ] `ReverseTransaction` EXPENSE: balance only, no stock changes
- [ ] `ConfirmDialog` shows interpolated Spanish text correctly
- [ ] Category quick-create modal preserves form draft
- [ ] AdBanner in list footer only when `AD_ENABLED`
- [ ] All tests pass, TypeScript strict, Lint clean

---

### Phase 5: System Settings (Week 5)
**Depends on:** Phase 4 complete (last module)

| Task | File | Status |
|------|------|--------|
| VOs: `Currency.ts`, `Money.ts` (from Phase 0, verify) | `src/domain/value-objects/` | 🔄 |
| Entities: `UserSettings.ts`, `Session.ts` | `src/domain/entities/` | 🆕 |
| Domain rules: `settingsRules.ts` (2 validators) | `src/domain/rules/` | 🆕 |
| Migration v2: `v2_0_0_settings_migration.sql` (creates `settings` table) | `src/data/database/migrations/` | 🆕 |
| AsyncStorage → SQLite migration logic | `src/application/settings/MigrateSettings.ts` | 🆕 |
| Repository: `ISettingsRepository`, `SqliteSettingsRepository` | `src/data/repositories/` | 🆕 |
| Use cases: `GetSettings`, `UpdateSettings`, `VerifyPin`, `SetPin`, `ExportDatabase`, `RestoreDatabase` | `src/application/settings/`, `src/application/security/`, `src/application/backup/` | 🆕 |
| Stores: `settingsStore` (hydrated from SQLite), `sessionStore` | `src/presentation/stores/` | 🔄 |
| Hooks: `useSettings`, `useSettingsCurrency`, `useSettingsPageSize`, `useSettingsSecurity`, `useSettingsBackup`, `usePinGate`, `useCurrency`, `useAdEnabled` | `src/presentation/hooks/` | 🔄/🆕 |
| Components: `CurrencySelector`, `PageSizeInput`, `PinForm`, `BiometricToggle`, `BackupActions`, `AdEnabledToggle` | `src/presentation/components/settings/` | 🆕 |
| i18n: `es/settings.json` (60+ keys) + `lock`, `pin_gate`, `pin_form`, `biometric`, `toast` | `src/presentation/i18n/es/` | 🆕 |
| Tests: Unit + Integration + Migration + PIN Gate + UI + E2E | `tests/` | 🆕 |

**Definition of Done (Settings):**
- [ ] Migration creates `settings` singleton, migrates AsyncStorage on first `GetSettings`
- [ ] `Currency`/`Money` VOs used globally across all modules
- [ ] PIN change requires current PIN (except first time), constant-time compare
- [ ] Biometric toggle requires auth before enabling
- [ ] Restore backup requires PIN, reloads app, data intact
- [ ] `adEnabled` master toggle controls Expenses/Catalog/Caja/Movimientos banners
- [ ] All 60+ i18n keys implemented
- [ ] All tests pass, TypeScript strict, Lint clean

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
| i18n completeness audit | CI grep for hardcoded Spanish strings |
| Performance: FlashList <50ms/frame, image <40KB | Physical device testing |
| PIN gate on critical actions | Change PIN, delete transaction, restore backup |
| E2E critical flows | Checkout, PIN change, backup/restore, ad toggle |
| TypeScript strict build | `tsc --noEmit` zero errors |
| Lint | `eslint .` zero errors |
| Test suite: all pass | Unit + Integration + Migration + Atomic + Hooks + UI + E2E |

---

## 3. Shared Artifacts (Implement Once)

| Artifact | Location | Consumers |
|----------|----------|-----------|
| `Currency.ts`, `Money.ts` | `src/domain/value-objects/` | All modules |
| `Stock.ts` | `src/domain/value-objects/` | Catalog, Sales, Financial |
| `SKU.ts` | `src/domain/value-objects/` | Catalog, Sales |
| `PaymentMethod.ts`, `ChangeAmount.ts` | `src/domain/value-objects/` | Sales, Expenses, Financial |
| `TransactionType.ts` | `src/domain/entities/` | Sales, Expenses, Financial |
| `stockRules.ts` | `src/domain/rules/` | Catalog, Sales, Financial |
| `cartRules.ts` | `src/domain/rules/` | Sales |
| `expenseRules.ts` | `src/domain/rules/` | Expenses |
| `settingsRules.ts` | `src/domain/rules/` | Settings |
| `ReverseTransaction.ts` | `src/application/transactions/` | Sales, Expenses, Financial |
| `migrations.ts` runner | `src/data/database/` | All modules (ordered) |
| `schema.sql.ts` | `src/data/database/` | All modules |
| `rowMappers.ts` | `src/data/mappers/` | All repositories |
| `container.ts` | `src/data/repositories/` | All repositories |
| `AdBanner.tsx` | `src/presentation/components/ads/` | Catalog, Expenses, Financial |
| `useAdEnabled.ts` | `src/presentation/hooks/` | All ad-enabled views |

---

## 4. Migration Coordination Protocol

### Single Source of Truth
- **File:** `src/data/database/migrations.ts`
- **Runner:** Executes migrations in fixed order within single process
- **Atomicity:** Each migration wrapped in `db.withTransactionSync()`
- **Version:** All modules target `user_version = 2`
- **Idempotency:** Migrations use `CREATE TABLE IF NOT EXISTS`, `INSERT OR IGNORE`, safe `ALTER TABLE`

### Execution Order (Enforced in Code)
```typescript
// migrations.ts - ORDER IS CRITICAL
const migrations = [
  { version: 2, name: 'catalog-inventory', sql: catalogSql },
  { version: 2, name: 'sales-checkout', sql: salesSql },
  { version: 2, name: 'financial-accumulators', sql: financialSql },
  { version: 2, name: 'expenses-cashflow', sql: expensesSql },
  { version: 2, name: 'system-settings', sql: settingsSql },
];
```

### Coordination Rules
1. **catalog-inventory** runs first — creates `products`, `categories` needed by sales
2. **sales-checkout** runs second — creates `sales_transactions`, `transaction_details`
3. **financial-accumulators** runs third — renames `resumen_caja` → `financial_accumulators`
4. **expenses-cashflow** runs fourth — creates `expense_categories`, adds expense index on `sales_transactions`
5. **system-settings** runs last — creates `settings` singleton, migrates AsyncStorage

### Conflict Avoidance
- `financial-accumulators` owns `resumen_caja` → `financial_accumulators` rename
- `expenses-cashflow` migration **comments out** accumulator rename (coordination)
- Shared `user_version = 2` updated once at end of all migrations
- If any migration fails → full rollback via backup restore

---

## 5. AdWare Governance Matrix

| View | Module | AdBanner Allowed? | Implementation |
|------|--------|-------------------|----------------|
| Catalog / Inventory List | catalog-inventory | ✅ Yes | `ListFooterComponent` in FlashList |
| Expenses List | expenses-cashflow | ✅ Yes | `ListFooterComponent` in FlashList |
| Caja (Cash Summary) | financial-accumulators | ✅ Yes | `ListFooterComponent` |
| Movimientos (Transactions) | financial-accumulators | ✅ Yes | `ListFooterComponent` |
| Checkout / Cart | sales-checkout | ❌ No | `AD_ENABLED` ignored |
| Payment Modal | sales-checkout | ❌ No | `AD_ENABLED` ignored |
| Receipt Preview | sales-checkout | ❌ No | `AD_ENABLED` ignored |
| New/Edit Product Form | catalog-inventory | ❌ No | `AD_ENABLED` ignored |
| New/Edit Expense Form | expenses-cashflow | ❌ No | `AD_ENABLED` ignored |
| ConfirmDialog | shared | ❌ No | `AD_ENABLED` ignored |
| PIN Gate / Lock Screen | system-settings | ❌ No | `AD_ENABLED` ignored |
| Settings Screen | system-settings | ❌ No | `AD_ENABLED` ignored |

**Master Toggle:** `settings.ad_enabled` (SQLite) → `settingsStore.adEnabled` (Zustand) → `useAdEnabled()` hook

**CI Enforcement:**
```bash
# Forbidden views must not contain AdBanner
grep -r "AdBanner" --include="*.tsx" app/(tabs)/registro.tsx src/presentation/components/cart/ src/presentation/components/feedback/ConfirmDialog.tsx app/lock.tsx app/settings/
# Must return empty
```

---

## 6. Testing Strategy (NFR-10: 100% Coverage Targets)

| Layer | Framework | Scope | Gate |
|-------|-----------|-------|------|
| Unit (Domain) | Jest | 100% functions/branches on VOs, Rules, Entities | Required |
| Integration (Data) | Jest + SQLite | 100% repo methods, use cases, atomic flows | Required |
| Migration | Jest + SQLite | v1→v2 zero-data-loss, idempotent, rollback | Required |
| Hooks | Jest + RTL | 100% hook logic | Required |
| Components | Jest + RTL | 100% render/interaction | Required |
| E2E | Detox / Maestro | Critical flows (checkout, PIN, backup, ad toggle) | Required |
| CI Gate | All suites + `tsc --noEmit` + `eslint` | Must pass for merge | Required |

### Test Directory Structure
```
tests/
├── unit/
│   ├── system-settings/        # Currency, Money, UserSettings, settingsRules
│   ├── catalog-inventory/      # Product, Category, Stock, SKU, stockRules
│   ├── sales-checkout/         # CartItem, SaleTransaction, cartRules, ChangeAmount
│   ├── expenses-cashflow/      # Expense, ExpenseCategory, expenseRules
│   └── financial-accumulators/ # FinancialSummary, PeriodSummary
├── integration/
│   ├── system-settings/        # GetSettings, UpdateSettings, VerifyPin, SetPin, Backup
│   ├── catalog-inventory/      # SqliteProductRepository, ProductManagement
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
│   ├── saleAtomicRollback.test.ts
│   ├── purchaseAtomicRollback.test.ts
│   ├── expenseAtomicRollback.test.ts
│   ├── accumulatorAtomicRollback.test.ts
│   └── reverseAtomicRollback.test.ts
├── hooks/                      # useProducts, useCart, useExpenses, useCashSummary...
├── components/                 # ProductGridCard, CartLine, ExpenseListItem, CashBalanceCard...
└── e2e/                        # checkoutFlow, pinChangeFlow, backupFlow, adEnabledFlow...
```

---

## 7. Risk Register & Mitigations

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| `RENAME COLUMN` unsupported (SQLite < 3.25) | Medium | High | Detect version at runtime; fallback to table copy approach |
| Migration order conflicts | Low | Critical | Single runner with explicit ordering; integration test full sequence |
| Fractional stock breaks integer-only UI | Medium | Medium | `keyboardType="decimal-pad"`, `step={0.001}`, `min={0}` in all inputs |
| Atomic transaction partial failure | Low | Critical | Single `withTransactionSync`; failure injection tests |
| AdBanner appears in forbidden views | Low | High | CI grep checks on every PR; code review checklist |
| i18n keys missing in components | Medium | Medium | CI check: `grep -r "hardcoded Spanish" --include="*.tsx" src/presentation/` |
| Migration long-running on large DB | Low | Medium | Background execution with progress; timeout guard (<5s expected) |
| Accumulator drift on reversal | Low | Critical | Integration test: create + revert → assert balance = 0 |
| PIN hash timing attack | Low | High | Constant-time comparison in `VerifyPin`; unit test verifies |
| Currency formatter mismatch | Low | Medium | Unit test asserts `format()` matches `Intl.NumberFormat` per locale |

---

## 8. Master Checklist (Definition of Done — Global)

### Architecture & Code Quality
- [ ] All 5 modules follow Clean Architecture layer boundaries (Domain → Application → Infrastructure → Presentation)
- [ ] Zero Spanish identifiers in code/DB; zero English strings in UI (i18n only)
- [ ] All public APIs have explicit TypeScript types
- [ ] All entities/DTOs use `readonly` and branded ID types
- [ ] Domain layer has zero external dependencies
- [ ] All write use cases use `db.withTransactionSync()` for atomicity

### Database & Migration
- [ ] All 5 migrations run in order, atomic, share `user_version = 2`
- [ ] Migration v1→v2 zero data loss verified on seeded production data
- [ ] Migration idempotent: run twice → no errors, no duplication
- [ ] Migration rollback via backup restore verified
- [ ] All tables use English `snake_case` columns, soft delete (`deleted_at`), partial indexes

### Domain Layer
- [ ] Shared VOs (`Currency`, `Money`, `Stock`, `SKU`, `PaymentMethod`, `ChangeAmount`) used everywhere
- [ ] Domain rules (`stockRules`, `cartRules`, `expenseRules`, `settingsRules`) centralized
- [ ] All entities use English identifiers, VOs in signatures

### Application Layer
- [ ] `RegisterSale`/`RegisterPurchase`/`RegisterExpense` atomic with stock + accumulator
- [ ] `ReverseTransaction` handles SALE/PURCHASE/EXPENSE with safety checks
- [ ] `UpdateAccumulator` single atomic UPDATE with all deltas
- [ ] `GetCashSummary` O(1), `GetPeriodSummary` correct aggregation
- [ ] PIN gate on critical actions (change PIN, delete transaction, restore backup)

### Infrastructure Layer
- [ ] SQLite repositories implement Domain interfaces (Ports)
- [ ] Image pipeline: 400×400, JPEG 70%, <40KB verified
- [ ] SecureStore for PIN hash, Biometric auth
- [ ] Backup: Export/Restore with PIN gate
- [ ] AdBanner wrapper respects `AD_ENABLED` flag

### Presentation Layer
- [ ] All UI strings via i18n keys (Spanish only)
- [ ] FlashList virtualized, `numColumns={3}` (catalog), `{1}` (lists)
- [ ] Zustand stores with granular selectors, manual invalidation
- [ ] Components memoized (`React.memo`, `useCallback`, `useMemo`)
- [ ] AdBanner only in 4 allowed views, absent in 8+ forbidden views

### AdWare Governance
- [ ] `settings.ad_enabled` master toggle in SQLite
- [ ] `settingsStore.adEnabled` hydrated at startup
- [ ] `useAdEnabled()` hook consumed by allowed views
- [ ] CI grep checks enforce forbidden views clean

### Testing & CI
- [ ] Unit tests: 100% functions/branches on Domain layer
- [ ] Integration tests: 100% repo methods, use cases, atomic flows
- [ ] Migration tests: 100% v1→v2 zero-loss, idempotent, rollback
- [ ] Atomic rollback tests: simulated DB failure → full rollback
- [ ] Hook tests: 100% logic coverage
- [ ] Component tests: 100% render/interaction
- [ ] E2E tests: checkout, PIN change, backup/restore, ad toggle flows
- [ ] TypeScript strict: `tsc --noEmit` zero errors
- [ ] Lint: `eslint .` zero errors

### Performance
- [ ] FlashList render <50ms/frame on physical device
- [ ] Optimized images <40KB on 10 test images
- [ ] App startup <3s cold, <1s warm
- [ ] Database queries <10ms (indexed, partial indexes)

---

## 9. Timeline Summary

| Week | Focus | Deliverable |
|------|-------|-------------|
| 1 | Phase 0: Shared Kernel | VOs, Rules, Migration Runner, Schema, Mappers |
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

*Last updated: 2026-11-10 — v2.0.0 Global Implementation Plan*