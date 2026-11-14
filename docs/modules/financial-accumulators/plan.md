# Financial Accumulators Module — Implementation Plan (v2.0.0 KMP)

> **Status (2026-11-14 audit):** Phase 0 Shared Kernel artifacts present in `kmp-app/composeApp` (VOs, rules, SQLDelight schema/migration, unit tests). Gradle compile/codegen/test gates ⏳ PENDING (network/sync/build not verified). Implementation of Phase 3+ items remains unverified/incomplete.

## Overview

This plan covers the implementation of the Financial Accumulators module v2.0.0 KMP, including:
- Domain layer: English entities (`FinancialSummary`, `PeriodSummary`), shared `TransactionType`
- Data layer: Table rename `resumen_caja` → `financial_accumulators` with English columns, `IAccumulatorRepository`
- Application layer: `GetCashSummary`, `GetPeriodSummary`, `UpdateAccumulator`, `ReverseTransaction`
- Presentation layer: Caja dashboard, Movements list, Transaction detail with PIN gate, AdBanner integration
- Testing: Unit, integration, migration, atomic, E2E

---

## Phase 1: Database Migration (Coordinate with sales-checkout)

### 1.1 Migration Runner Update
**File:** `commonMain/data/database/DatabaseDriverFactory.kt`
- Register the accumulators step in the version-2 migration list (all v2 modules share one transaction)
- `user_version = 2` is set centrally by `DatabaseDriverFactory` after every v2 step succeeds

### 1.2 Migration SQL
**File:** `commonMain/sqldelight/migrations/v2_financial_accumulators.sqm` (see spec.md section 4.3)

### 1.3 Recovery Migration
**File:** `commonMain/sqldelight/migrations/v2_financial_accumulators_columns.sqm`
- For databases from an earlier half-applied v2.0.0 attempt (table already renamed, columns still Spanish)
- Applies column renames, `updated_at` backfill, and index without re-renaming the table

### 1.4 Migration Checklist
- [ ] Register the accumulators step in `DatabaseDriverFactory` v2 migration list
- [ ] Write `v2_financial_accumulators.sqm` with atomic transaction
- [ ] Rename table: `resumen_caja` → `financial_accumulators`
- [ ] Rename columns: `total_ventas`→`total_sales`, `total_compras`→`total_purchases`, `total_gastos`→`total_expenses`, `saldo_actual`→`net_balance`
- [ ] Add `updated_at` with `DEFAULT NULL` + backfill (SQLite ALTER TABLE limitation)
- [ ] Provide half-migrated recovery variant
- [ ] `user_version` = 2 (centralized)
- [ ] Verify row counts match pre-migration (single row id=1)
- [ ] Test restore from backup scenario

---

## Phase 2: Domain Layer (`commonMain/domain/`)

### 2.1 Entity Files
| New File | Purpose |
|---|---|
| `entities/FinancialSummary.kt` | English fields, `Money` VOs, `updatedAt` |
| `entities/PeriodSummary.kt` | Period aggregation with `Money` VOs |
| `entities/TransactionType.kt` | Shared with sales-checkout: `SALE` \| `PURCHASE` \| `EXPENSE` |

### 2.2 Shared Value Objects (from sales-checkout)
- `entities/PaymentMethod.kt` — `CASH` \| `CARD` \| `TRANSFER` \| `QR`
- `valueobjects/Money.kt` / `Currency.kt` — from `system-settings`

---

## Phase 3: Data Layer (`commonMain/data/repositories/`)

### 3.1 Repository
| File | Description |
|---|---|
| `IAccumulatorRepository.kt` | English interface with `Money` VOs |
| `impl/SqlDelightAccumulatorRepository.kt` | Implementation with SQLDelight queries |

### 3.2 SQLDelight Mappers
- `mapFinancialSummaryRow` — row → `FinancialSummary` entity
- `mapPeriodSummaryRow` — aggregation from `sales_transactions`

### 3.3 Registration
**File:** `commonMain/data/database/DatabaseDriverFactory.kt`
- Register `SqlDelightAccumulatorRepository` as `IAccumulatorRepository`

---

## Phase 4: Application Layer (`commonMain/application/`)

### 4.1 GetCashSummary.kt
**File:** `accumulators/GetCashSummary.kt`
- Direct read from `financial_accumulators` where `id = 1`
- Returns `FinancialSummary` with `Money` VOs

### 4.2 GetPeriodSummary.kt
**File:** `accumulators/GetPeriodSummary.kt`
- Input: `period: Period` (MONTH | YEAR)
- Aggregates from `sales_transactions` by `type` for period
- Returns `PeriodSummary` with computed `net`

### 4.3 UpdateAccumulator.kt
**File:** `accumulators/UpdateAccumulator.kt`
- Input: `salesDelta`, `purchasesDelta`, `expensesDelta` (Money)
- Atomic transaction block:
  1. Single UPDATE on `financial_accumulators` with all deltas + `updated_at`
- Called by: `RegisterSale`, `RegisterPurchase`, `RegisterExpense`, `ReverseTransaction`

### 4.4 ReverseTransaction.kt
**File:** `transactions/ReverseTransaction.kt` (shared)
- Handles `SALE`, `PURCHASE`, `EXPENSE` reversal
- Pre-check: PURCHASE revert validates stock won't go negative
- Atomic transaction block for stock + accumulator updates
- Soft delete: `UPDATE deleted_at = CURRENT_TIMESTAMP`

---

## Phase 5: Presentation Layer

### 5.1 ViewModels (`commonMain/presentation/viewmodels/`)
| ViewModel | File | Description |
|---|---|---|
| `CashSummaryViewModel` | `CashSummaryViewModel.kt` | Reads accumulator, auto-refresh |
| `PeriodSummaryViewModel` | `PeriodSummaryViewModel.kt` | MONTH/YEAR period summaries |
| `MovementsViewModel` | `MovementsViewModel.kt` | Paginated, filterable, searchable list |
| `PinGateViewModel` | `PinGateViewModel.kt` | PIN verification gate |

### 5.2 Compose UI Components (`composeApp/src/commonMain/compose/`)

| Component | File | Description |
|---|---|---|
| `CashSummaryScreen.kt` | `accumulators/CashSummaryScreen.kt` | Uses `CashBalanceCard`, `StatCard`, `BreakdownBar`, `ReportRow`, `AdBanner` |
| `CashBalanceCard.kt` | `accumulators/CashBalanceCard.kt` | Large balance with tone |
| `StatCard.kt` | `reports/StatCard.kt` | Memoized stat |
| `BreakdownBar.kt` | `reports/BreakdownBar.kt` | Horizontal bars |
| `ReportRow.kt` | `reports/ReportRow.kt` | Key-value row |
| `MovementsListScreen.kt` | `movimientos/MovementsListScreen.kt` | Uses `MovementListItem`, `TypeFilterChips`, `SearchBar`, `AdBanner` |
| `MovementListItem.kt` | `movimientos/MovementListItem.kt` | Transaction row |
| `TypeFilterChips.kt` | `movimientos/TypeFilterChips.kt` | Filter chips |
| `AdBanner.kt` | `ads/AdBanner.kt` | **ALLOWED** in Caja/Movimientos footers |

### 5.3 AdWare Integration
**Files to modify:**
- `CashSummaryScreen.kt` — Add `AdBanner` to footer when `AD_ENABLED=true`
- `MovementsListScreen.kt` — Add `AdBanner` to footer when `AD_ENABLED=true`
- `TransactionDetailScreen.kt` — **NO** ads
- `PinGateDialog.kt` / `ConfirmDialog.kt` — **NO** ads

**Rule:** `AD_ENABLED` flag respected in dashboard/report views only.

### 5.4 i18n
**File:** `composeApp/src/commonMain/composeResources/values-es/strings.xml`
- All 40+ keys from spec.md section 13
- Namespace: `accumulator_`

---

## Phase 6: Migration Verification

### 6.1 Data Integrity Checks
- Single row `id=1` preserved in `financial_accumulators`
- All accumulated values preserved: `total_sales`, `total_purchases`, `total_expenses`, `net_balance`
- `updated_at` populated with migration timestamp
- `sales_transactions` migration handled by sales-checkout (verify coordination)
- Indexes created for period queries

### 6.2 Functional Verification
- `getCashSummary()` returns correct `FinancialSummary` with `Money` VOs
- `getPeriodSummary('MONTH')` and `'YEAR'` compute correct `net`
- Create SALE → verify `total_sales` increment, `net_balance` increment
- Create PURCHASE → verify `total_purchases` increment, `net_balance` decrement
- Create EXPENSE → verify `total_expenses` increment, `net_balance` decrement
- Revert SALE → verify `total_sales` decrement, `net_balance` decrement, stock restored
- Revert PURCHASE → verify `total_purchases` decrement, `net_balance` increment (pre-check)
- Revert EXPENSE → verify `total_expenses` decrement, `net_balance` increment
- AdBanner appears in Caja/Movimientos when `AD_ENABLED=true`, absent in detail

---

## Phase 7: Testing

### 7.1 Unit Tests
**Directory:** `commonTest/unit/financial-accumulators/`
- `FinancialSummaryTest.kt` — Entity construction with Money VOs
- `PeriodSummaryTest.kt` — Entity construction, net calculation
- `UpdateAccumulatorTest.kt` — Delta calculations, net formula

### 7.2 Integration Tests
**Directory:** `commonTest/integration/financial-accumulators/`
- `GetCashSummaryTest.kt` — Direct read, updated values
- `GetPeriodSummaryTest.kt` — Month/year aggregation, net calculation
- `UpdateAccumulatorTest.kt` — Atomic update, multiple deltas
- `ReverseTransactionTest.kt` — SALE, PURCHASE, EXPENSE, stock safety check

### 7.3 Migration Tests
**Directory:** `commonTest/migration/financial-accumulators/`
- `V2FinancialAccumulatorsMigrationTest.kt` — Seed v1 schema, run migration, assert all mappings, idempotency

### 7.4 Atomic Transaction Tests
**Directory:** `commonTest/atomic/`
- `AccumulatorAtomicRollbackTest.kt` — Simulate DB failure mid-transaction → full rollback
- `ReverseAtomicRollbackTest.kt` — Failure during reversal → no partial state

### 7.5 ViewModel Tests (Turbine)
**Directory:** `commonTest/viewmodels/financial-accumulators/`
- `CashSummaryViewModelTest.kt` — Invalidation refresh, loading states
- `PeriodSummaryViewModelTest.kt` — Period switching, data refresh
- `MovementsViewModelTest.kt` — Pagination, filtering, search, infinite scroll
- `PinGateViewModelTest.kt` — PIN gate logic

### 7.6 Compose UI Tests (Robolectric)
**Directory:** `androidTest/compose/accumulators/`
- `CashBalanceCardTest.kt` — Tone rendering (success/danger/neutral), formatted amount
- `StatCardTest.kt` — Label, value, tone variants
- `BreakdownBarTest.kt` — Bar percentages, max calculation
- `ReportRowTest.kt` — Bold, tone, last-row border

**Directory:** `androidTest/compose/movimientos/`
- `MovementListItemTest.kt` — Type badge, amount, date, detail count
- `TypeFilterChipsTest.kt` — Filter selection, callback

**Directory:** `androidTest/compose/ads/`
- `AdBannerTest.kt` — Renders when enabled, hidden when disabled

### 7.7 E2E Tests (Maestro)
**Directory:** `maestro/financial-accumulators/`
- `cashSummaryFlow.yaml` — Create transactions → verify accumulator updates → verify period reports
- `reverseTransactionFlow.yaml` — Revert each type → verify accumulator/stock restoration
- `adBannerVisibility.yaml` — Ad in Caja/Movimientos, no ad in detail/PIN gate

---

## Dependencies & Ordering

```
Phase 1 (Migration) → Phase 2 (Domain) → Phase 3 (Data) → Phase 4 (Application) → Phase 5 (Presentation) → Phase 6 (Verification) → Phase 7 (Testing)
```

**Parallelizable:**
- Phase 2 & 3 can run in parallel after Phase 1
- Phase 7 tests can be written alongside implementation

**Coordination Required:**
- Migration Phase 1 must run AFTER or in same transaction as sales-checkout migration (shared `user_version = 2`)
- `TransactionType`, `PaymentMethod`, `Money` VOs shared with sales-checkout — implement once, import in both
- `ReverseTransaction` use case shared across three modules

---

## Risk Mitigation

| Risk | Mitigation |
|---|---|
| Migration order with sales-checkout | Single migration runner with ordered migrations; test combined migration |
| `AD_ENABLED` in wrong views | CI grep check: `grep -r "AdBanner" --include="*.kt" composeApp/.../accumulators composeApp/.../movimientos` |
| Accumulator drift on reversal | Integration test: create + revert → assert balance = 0 |
| Stock negative on purchase revert | Pre-check throws `StockError` before transaction opens |
| i18n keys missing | CI check: `grep -r "accumulator_" --include="*.kt" composeApp/.../` |
| Fractional amounts | `Money` VO handles precision; `KeyboardType.Decimal` in forms |

---

## Definition of Done

- [ ] Migration renames table/columns atomically, preserves all data
- [ ] All entities/repos/use cases use English identifiers with `Money` VOs
- [ ] `GetCashSummary`/`GetPeriodSummary`/`UpdateAccumulator`/`ReverseTransaction` work correctly
- [ ] Atomic accumulator updates on all transaction types (SALE/PURCHASE/EXPENSE)
- [ ] Atomic reversal with stock safety for PURCHASE
- [ ] AdBanner renders in Caja/Movimientos footers when `AD_ENABLED=true`, absent elsewhere
- [ ] All 40+ i18n keys implemented
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