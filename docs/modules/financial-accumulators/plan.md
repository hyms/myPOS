# Financial Accumulators Module — Implementation Plan (v2.0.0)

> **Status (2026-11-11):** Phase 2 (atomic migration + repository/use-case SQL foundation) is complete and tested. Domain entities still use their existing names; presentation/entity renaming remains pending.

## Overview

This plan covers the implementation of the Financial Accumulators module v2.0.0, including:
- Domain layer: English entities (`FinancialSummary`, `PeriodSummary`), shared `TransactionType`
- Data layer: Table rename `resumen_caja` → `financial_accumulators` with English columns, `IAccumulatorRepository`
- Application layer: `GetCashSummary`, `GetPeriodSummary`, `UpdateAccumulator`, `ReverseTransaction`
- Presentation layer: Caja dashboard, Movements list, Transaction detail with PIN gate, AdBanner integration
- Testing: Unit, integration, migration, atomic, E2E

---

## Phase 1: Database Migration (Coordinate with sales-checkout)

### 1.1 Migration Runner Update
**File:** `src/data/database/migrations.ts`
- Register the accumulators step in the version-2 group (all v2 modules share one `withTransactionSync`)
- `user_version = 2` is set centrally by `runMigrations` after every v2 step succeeds

### 1.2 Migration SQL
**File:** `src/data/database/migrations/v2_0_0_financial_accumulators_migration.sql.ts` (`FINANCIAL_ACCUMULATORS_MIGRATION_SQL`, plus `FINANCIAL_ACCUMULATORS_COLUMNS_MIGRATION_SQL` recovery variant)

```sql
-- Runs inside the shared v2.0.0 transaction (single db.withTransactionSync)

-- Step 1: Rename table
ALTER TABLE resumen_caja RENAME TO financial_accumulators;

-- Step 2: Rename columns
ALTER TABLE financial_accumulators RENAME COLUMN total_ventas TO total_sales;
ALTER TABLE financial_accumulators RENAME COLUMN total_compras TO total_purchases;
ALTER TABLE financial_accumulators RENAME COLUMN total_gastos TO total_expenses;
ALTER TABLE financial_accumulators RENAME COLUMN saldo_actual TO net_balance;

-- Step 3: Add updated_at (SQLite forbids non-constant defaults in ALTER TABLE ADD COLUMN)
ALTER TABLE financial_accumulators ADD COLUMN updated_at TIMESTAMP DEFAULT NULL;

-- Step 4: Backfill the existing singleton row
UPDATE financial_accumulators SET updated_at = CURRENT_TIMESTAMP WHERE id = 1;

-- Step 5: Index for freshness checks
CREATE INDEX IF NOT EXISTS idx_financial_accumulators_updated_at
  ON financial_accumulators(updated_at);

-- Step 6: user_version = 2 is set centrally by `runMigrations`
```

> **Recovery:** databases from an earlier half-applied v2.0.0 attempt (table already
> renamed, columns still Spanish) are handled by
> `FINANCIAL_ACCUMULATORS_COLUMNS_MIGRATION_SQL`, which applies the column renames,
> `updated_at` backfill, and index without re-renaming the table.

### 1.3 Migration Checklist
- [x] Register the accumulators step in `migrations.ts` v2 group
- [x] Write `v2_0_0_financial_accumulators_migration.sql.ts` with atomic transaction
- [x] Rename table: `resumen_caja` → `financial_accumulators`
- [x] Rename columns: `total_ventas`→`total_sales`, `total_compras`→`total_purchases`, `total_gastos`→`total_expenses`, `saldo_actual`→`net_balance`
- [x] Add `updated_at` with `DEFAULT NULL` + backfill (SQLite ALTER TABLE limitation)
- [x] Provide half-migrated recovery variant
- [x] `user_version` = 2 (centralized)
- [x] Verify row counts match pre-migration (single row id=1)
- [ ] Test restore from backup scenario

---

## Phase 2: Domain Layer

### 2.1 Entity Renames & New Entities
| Old File | New File | Changes |
|---|---|---|
| `ResumenCaja.ts` | `FinancialSummary.ts` | English fields, `Money` VOs, `updatedAt` |
| *(new)* | `PeriodSummary.ts` | Period aggregation with `Money` VOs |
| `Transaccion.ts` | `Transaction.ts` | English fields, shared with sales-checkout |

### 2.2 Shared Value Objects (from sales-checkout)
- `TransactionType.ts` — `SALE` | `PURCHASE` | `EXPENSE`
- `PaymentMethod.ts` — `CASH` | `CARD` | `TRANSFER` | `QR`
- `Money.ts` / `Currency.ts` — from `system-settings`

---

## Phase 3: Data Layer

### 3.1 Repository
| File | Description |
|---|---|
| `src/data/repositories/IAccumulatorRepository.ts` | English interface with `Money` VOs |
| `src/data/repositories/SqliteAccumulatorRepository.ts` | Implementation with English column mappers |

### 3.2 Mappers
**File:** `src/data/mappers/rowMappers.ts`
- `mapFinancialSummaryRow` — Spanish → English column mapping
- `mapPeriodSummaryRow` — Aggregation from `sales_transactions`

### 3.3 Container Registration
**File:** `src/data/repositories/container.ts`
- Register `SqliteAccumulatorRepository` as `IAccumulatorRepository`

---

## Phase 4: Application Layer

### 4.1 GetCashSummary.ts
**File:** `src/application/accumulators/GetCashSummary.ts`
- Direct read from `financial_accumulators` where `id = 1`
- Returns `FinancialSummary` with `Money` VOs

### 4.2 GetPeriodSummary.ts
**File:** `src/application/accumulators/GetPeriodSummary.ts`
- Input: `period: 'MONTH' | 'YEAR'`
- Aggregates from `sales_transactions` by `type` for period
- Returns `PeriodSummary` with computed `net`

### 4.3 UpdateAccumulator.ts
**File:** `src/application/accumulators/UpdateAccumulator.ts`
- Input: `salesDelta`, `purchasesDelta`, `expensesDelta` (Money)
- Atomic `withTransactionSync`:
  1. Single UPDATE on `financial_accumulators` with all deltas + `updated_at`
- Called by: `RegisterSale`, `RegisterPurchase`, `RegisterExpense`, `ReverseTransaction`

### 4.4 ReverseTransaction.ts
**File:** `src/application/transactions/ReverseTransaction.ts` (shared)
- Handles `SALE`, `PURCHASE`, `EXPENSE` reversal
- Pre-check: PURCHASE revert validates stock won't go negative
- Atomic `withTransactionSync` for stock + accumulator updates
- Soft delete: `UPDATE deleted_at = CURRENT_TIMESTAMP`

---

## Phase 5: Presentation Layer

### 5.1 Zustand Stores (existing)
| Store | File | Description |
|---|---|---|
| `sessionStore` | `src/presentation/stores/sessionStore.ts` | `unlocked: boolean` |
| `invalidationStore` | `src/presentation/stores/invalidationStore.ts` | Cache invalidation versions |

### 5.2 Hooks
| Hook | File | Description |
|---|---|---|
| `useCashSummary` | `src/presentation/hooks/useCashSummary.ts` | Reads accumulator, auto-refresh |
| `usePeriodSummary` | `src/presentation/hooks/usePeriodSummary.ts` | MONTH/YEAR period summaries |
| `useMovements` | `src/presentation/hooks/useMovements.ts` | Paginated, filterable, searchable list |
| `usePinGate` | `src/presentation/hooks/usePinGate.ts` | PIN verification gate |

### 5.3 Components
| Component | File | Description |
|---|---|---|
| `CashBalanceCard` | `src/presentation/components/accumulators/CashBalanceCard.tsx` | Large balance with tone |
| `StatCard` | `src/presentation/components/reports/Stat.tsx` | Memoized stat (exists) |
| `PeriodBreakdown` | `src/presentation/components/reports/BreakdownBar.tsx` | Horizontal bars (exists) |
| `ReportRow` | `src/presentation/components/reports/ReportRow.tsx` | Key-value row (exists) |
| `MovementListItem` | `src/presentation/components/movimientos/MovimientoListItem.tsx` | Transaction row (exists) |
| `TypeFilterChips` | `src/presentation/components/movimientos/TipoFilterChips.tsx` | Filter chips (exists) |
| `AdBanner` | `src/presentation/components/ad/AdBanner.tsx` | **ALLOWED** in Caja/Movimientos footers |

### 5.4 AdWare Integration
**Files to modify:**
- `app/(tabs)/caja.tsx` — Add `AdBanner` to footer when `AD_ENABLED=true`
- `app/movimientos/index.tsx` — Add `AdBanner` to `ListFooterComponent` when `AD_ENABLED=true`
- `app/transacciones/[id].tsx` — **NO** ads
- PIN gate / ConfirmDialog — **NO** ads

**Rule:** `AD_ENABLED` flag respected in dashboard/report views only.

### 5.5 i18n
**File:** `src/presentation/i18n/es/accumulator.json`
- All 40+ keys from spec.md section 14
- Namespace: `accumulator`

### 5.6 Screens
| Screen | File | Changes |
|---|---|---|
| Caja Dashboard | `app/(tabs)/caja.tsx` | Use `CashBalanceCard`, `StatCard`, `PeriodBreakdown`, `ReportRow`, AdBanner |
| Movements List | `app/movimientos/index.tsx` | Use `MovementListItem`, `TypeFilterChips`, `SearchBar`, AdBanner |
| Transaction Detail | `app/transacciones/[id].tsx` | Already handles reversal, PIN gate |

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
**Directory:** `tests/unit/financial-accumulators/`
- `FinancialSummary.test.ts` — Entity construction with Money VOs
- `PeriodSummary.test.ts` — Entity construction, net calculation
- `UpdateAccumulator.test.ts` — Delta calculations, net formula

### 7.2 Integration Tests
**Directory:** `tests/integration/financial-accumulators/`
- `GetCashSummary.test.ts` — Direct read, updated values
- `GetPeriodSummary.test.ts` — Month/year aggregation, net calculation
- `UpdateAccumulator.test.ts` — Atomic update, multiple deltas
- `ReverseTransaction.test.ts` — SALE, PURCHASE, EXPENSE, stock safety check

### 7.3 Migration Tests
**Directory:** `tests/migration/financial-accumulators/`
- `v2_0_0_migration.test.ts` — Seed v1 schema, run migration, assert all mappings, idempotency

### 7.4 Atomic Transaction Tests
**Directory:** `tests/integration/atomic/`
- `accumulatorAtomicRollback.test.ts` — Simulate DB failure mid-transaction → full rollback
- `reverseAtomicRollback.test.ts` — Failure during reversal → no partial state

### 7.5 Hook Tests
**Directory:** `tests/unit/hooks/`
- `useCashSummary.test.ts` — Invalidation refresh, loading states
- `usePeriodSummary.test.ts` — Period switching, data refresh
- `useMovements.test.ts` — Pagination, filtering, search, infinite scroll

### 7.6 UI Component Tests
**Directory:** `tests/components/accumulators/`
- `CashBalanceCard.test.tsx` — Tone rendering (success/danger/neutral), formatted amount
- `StatCard.test.tsx` — Label, value, tone variants
- `PeriodBreakdown.test.tsx` — Bar percentages, max calculation
- `ReportRow.test.tsx` — Bold, tone, last-row border

**Directory:** `tests/components/movimientos/`
- `MovementListItem.test.tsx` — Type badge, amount, date, detail count
- `TypeFilterChips.test.tsx` — Filter selection, callback

**Directory:** `tests/components/ad/`
- `AdBanner.test.tsx` — Renders when enabled, hidden when disabled

### 7.7 E2E Tests
**Directory:** `tests/e2e/financial-accumulators/`
- `cashSummaryFlow.test.ts` — Create transactions → verify accumulator updates → verify period reports
- `reverseTransactionFlow.test.ts` — Revert each type → verify accumulator/stock restoration
- `adBannerVisibility.test.ts` — Ad in Caja/Movimientos, no ad in detail/PIN gate

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
| `AD_ENABLED` in wrong views | CI grep check: `grep -r "AdBanner" --include="*.tsx" src/presentation/components/accumulators src/presentation/components/movimientos` |
| Accumulator drift on reversal | Integration test: create + revert → assert balance = 0 |
| Stock negative on purchase revert | Pre-check throws `StockError` before transaction opens |
| i18n keys missing | CI check: `grep -r "accumulator\." --include="*.tsx" src/presentation/components/` |
| Fractional amounts | `Money` VO handles precision; `keyboardType="decimal-pad"` in forms |

---

## Definition of Done

- [x] Migration renames table/columns atomically, preserves all data
- [ ] All entities/repos/use cases use English identifiers with `Money` VOs
- [x] `GetCashSummary`/`GetPeriodSummary`/`UpdateAccumulator`/`ReverseTransaction` work correctly
- [x] Atomic accumulator updates on all transaction types (SALE/PURCHASE/EXPENSE)
- [x] Atomic reversal with stock safety for PURCHASE
- [ ] AdBanner renders in Caja/Movimientos footers when `AD_ENABLED=true`, absent elsewhere
- [ ] All 40+ i18n keys implemented
- [x] Unit tests: 100% pass
- [x] Integration tests: 100% pass
- [x] Migration tests: 100% pass
- [x] Atomic rollback tests: 100% pass
- [x] TypeScript strict: no errors
- [ ] Lint: no errors

---

*Plan version: 1.0 — 2026-11-10*