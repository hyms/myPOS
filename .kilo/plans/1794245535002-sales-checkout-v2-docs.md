# Sales & Checkout Module — v2.0.0 Documentation Triad Plan

## Goal
Create v2.0.0 specification, implementation plan, and test specification for the Sales & Checkout module following SemVer 2.0.0 with:
- English identifiers (code/DB) — replacing Spanish `transacciones`/`detalle_transacciones`
- Composite Pattern for cart items (`ICartItem` + `SimpleCartItem`)
- Atomic transactions via `withTransactionSync`
- Zero ads on checkout screen (`AD_ENABLED` ignored)
- v1→v2 zero-data-loss migration (atomic DDL in single transaction)

## Context & Constraints

| Aspect | Requirement |
|---|---|
| **Version** | `v2.0.0` — breaking changes: table/column renames, English identifiers |
| **Language** | 100% English identifiers (code, DB, entities, VOs, repos); Spanish UI only via i18n keys |
| **Stock** | Fractional quantities supported (3dp), validated against `currentStock` |
| **Migration** | Atomic SQLite transaction: rename tables/columns, recreate indexes, preserve all data |
| **Payments** | Single payment method per transaction (INV-04); cash change calculator |
| **Ads** | ZERO ads on checkout screens — `AD_ENABLED` flag ignored/disabled |

## Affected Files (Module Triad)

| File | Action |
|---|---|
| `/docs/modules/sales-checkout/spec.md` | New file: v2.0.0 specification |
| `/docs/modules/sales-checkout/plan.md` | New file: implementation plan with migration checklist |
| `/docs/modules/sales-checkout/test.md` | New file: complete TDD test suite spec |

## Implementation Sequence

### Phase 1: Specification (`spec.md`)

**Tasks:**
1. Define bounded context (consumes `catalog-inventory`, updates `financial-accumulators`)
2. Domain entities with English identifiers:
   - `CartItem` (Composite Pattern): `ICartItem`, `SimpleCartItem`, `SimpleCartItemFactory`
   - `SaleTransaction` (Aggregate Root): `type`, `totalAmount`, `paymentMethod`, `detail`, `registeredAt`, `deletedAt`
   - `TransactionDetail`: `transactionId`, `productId`, `quantity`, `unitPrice`, `lineTotal`
   - `PaymentMethod` (VO): `CASH` | `CARD` | `TRANSFER` | `QR`
   - `ChangeAmount` (VO): cash change calculation with color coding
3. Value Objects (shared kernel): `Money`, `Currency` (ref `system-settings/spec.md`)
4. Domain rules & invariants (RULE-SC-01 to RULE-SC-08, INV-01, INV-03, INV-04, INV-10)
5. Database Contract:
   - Target schema: `sales_transactions`, `transaction_details` (English columns)
   - Column mapping table: v1.0.0 Spanish → v2.0.0 English
   - Migration DDL: atomic `withTransactionSync` with `ALTER TABLE RENAME`
6. Repository interfaces: `ISaleRepository`
7. Use cases: `RegisterSale`, `RegisterPurchase`, `RevertTransaction`
8. Domain rules: `cartRules.ts` functions (`getUnitPrice`, `lineSubtotal`, `calcTotals`, `calcChange`, `applyDiscount`)
9. UI state machines (Spanish visible text via i18n keys)
10. UI components list with memoization status
11. Hooks: `useCart`, `useTransactions`, `useStockAlert`
12. Zustand stores: `saleCartStore`, `purchaseCartStore` (granular selectors)
13. AdWare governance: ZERO ads on checkout
14. Applicable NFRs (NFR-02, NFR-03, NFR-04, NFR-07, NFR-08, NFR-09, NFR-10)
15. Extension points (bundles, split payments, thermal printing)
16. i18n keys dictionary (45+ Spanish UI strings)
17. Cross-references to other module specs

### Phase 2: Implementation Plan (`plan.md`)

**Tasks:**
1. Component breakdown table (existing + new: `ReceiptPreview`, `ChangeCalculator`)
2. Step-by-step execution order with dependencies
3. **Database Migration Task Checklist** (critical):
   - [ ] Create migration v2 in `migrations.ts` (version 2)
   - [ ] Write `v2_0_0_sales_migration.sql` with atomic transaction
   - [ ] Rename tables: `transacciones` → `sales_transactions`, `detalle_transacciones` → `transaction_details`
   - [ ] Rename columns in `sales_transactions`: `tipo`→`type`, `monto_total`→`total_amount`, `tipo_pago`→`payment_method`, `detalle`→`detail`, `fecha_registro`→`registered_at`, `fecha_delete`→`deleted_at`
   - [ ] Rename columns in `transaction_details`: `transaccion_id`→`transaction_id`, `producto_id`→`product_id`, `cantidad`→`quantity`, `precio_unitario`→`unit_price`
   - [ ] Recreate indexes with English names
   - [ ] Update `user_version` to 2
   - [ ] Verify row counts match pre-migration
   - [ ] Test rollback scenario (restore from backup)
4. Domain layer updates: rename entities, add Composite Pattern, add VOs
5. Data layer updates: rename repos to English, update mappers for English columns
6. Application layer updates: `RegisterSale.ts`, `RegisterPurchase.ts`, `RevertTransaction.ts`
7. Presentation layer updates: stores, hooks, components
8. AdWare: Remove `AdBanner` from checkout screens (`CarritoScreen`, `PaymentModal`, `ReceiptPreview`)
9. i18n file creation: `src/presentation/i18n/es/checkout.json`
10. Risk mitigation table
11. Definition of Done checklist

### Phase 3: Test Specification (`test.md`)

**Tasks:**
1. Unit tests (TDD):
   - `cartRules.ts`: `getUnitPrice`, `lineSubtotal`, `calcTotals`, `calcChange`, `applyDiscount` (rounding, edge cases)
   - `PaymentMethod` VO: validation, equality, `isCash()`
   - `ChangeAmount` VO: `isSufficient`, `getColorClass`, `zero`
   - `SimpleCartItemFactory`: creation from product, discount clamping
2. Integration tests:
   - `RegisterSale`: success, insufficient stock (throws `StockError`), empty cart, cash change calculation, atomic rollback on failure
   - `RegisterPurchase`: success, stock increment, balance decrement
   - `RevertTransaction`: SALE (stock +, balance -), PURCHASE (stock -, balance +), EXPENSE (balance +), stock safety check before PURCHASE revert
3. **DB Data Migration Tests** (critical):
   - Seed v1.0.0 schema with Spanish columns + sample data (SALE, PURCHASE, EXPENSE types, soft-deleted rows)
   - Run migration v2
   - Assert: all rows migrated, English columns populated, PKs preserved, timestamps preserved, soft-deletes preserved, row count unchanged
   - Assert: indexes recreated with English names
   - Assert: `resumen_caja` accumulator unchanged
4. Atomic transaction tests:
   - Simulate DB failure mid-transaction → verify full rollback
   - Concurrent sale attempts → verify stock consistency
5. Hook tests:
   - `useCart`: granular selectors prevent unnecessary re-renders
   - `useTransactions`: pagination, filtering by type
   - `useStockAlert`: triggers Toast when `currentStock < minStock` post-sale
6. UI component tests (React Native Testing Library):
   - `PaymentModal`: change calculator (green ≥0, red <0), exact amount button
   - `LineItemModal`: quantity validation, discount ≤ subtotal
   - `CartSummaryBar`: item count, total, checkout/clear buttons
   - `ReceiptPreview`: renders transaction data correctly
7. E2E scenarios:
   - Full SALE flow: add items → payment (cash with change) → receipt → share
   - Full PURCHASE flow: add items → payment (card) → receipt
   - Revert SALE → verify stock restored, balance corrected
   - Migration idempotency: run twice → no errors, no data duplication

## Dependencies

- `catalog-inventory/spec.md` — Product, Stock VOs, `validateStockAvailable`
- `financial-accumulators/spec.md` — `resumen_caja` accumulator, `RevertTransaction` stock math
- `system-settings/spec.md` — Currency/Money VOs, pagination settings, PIN gate for deletion
- `expenses-cashflow/spec.md` — Expense registration (unified in `sales_transactions` type `EXPENSE`)

## Key Design Decisions (Resolved)

| Decision | Resolution |
|---|---|
| **Table rename** | `transacciones` → `sales_transactions`, `detalle_transacciones` → `transaction_details` |
| **Column rename** | All Spanish → English (see mapping table in spec) |
| **Migration approach** | Single `withTransactionSync` with `ALTER TABLE RENAME COLUMN` (SQLite ≥ 3.25 confirmed) |
| **Composite Pattern** | `ICartItem` interface + `SimpleCartItem` leaf; ready for `BundleCartItem` |
| **Payment methods** | Exactly one per transaction (INV-04) — no split payments in v2.0.0 |
| **Cash change** | Calculated live in `PaymentModal`, color-coded (green/red) |
| **AdWare on checkout** | Completely disabled — `AD_ENABLED` ignored |
| **i18n namespace** | `checkout` prefix for all keys |
| **Receipt format** | HTML → PDF via `expo-print` + share (current approach) |
| **Data volume** | Small (< 1,000 rows) — fast migration, no background execution needed |
| **Rollback strategy** | Forward-only + backup restore (no rollback DDL needed) |

## Validation Plan

| Check | Method |
|---|---|
| TypeScript strict | `tsc --noEmit` passes |
| Lint | `eslint .` passes |
| Unit tests | `jest --testPathPattern=domain` 100% pass |
| Integration tests | `jest --testPathPattern=integration` 100% pass |
| Migration test | `jest --testPathPattern=migration` passes with seeded v1 data |
| Atomic rollback test | `jest --testPathPattern=atomic` passes |
| FlashList perf | Manual profile: <50ms/frame on device |
| AdWare check | CI: grep confirms no `AdBanner` in checkout components |
| i18n coverage | Script: 0 hardcoded Spanish strings in checkout components |

## Risks & Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| `RENAME COLUMN` unsupported | Migration fails on old devices | SQLite ≥ 3.25 confirmed; no fallback needed |
| Fractional quantities break UI | Integer-only inputs reject decimals | Update `keyboardType` to `decimal-pad`, `step={0.001}` |
| Atomic transaction partial failure | Data corruption | Single `withTransactionSync` wraps ALL writes; test failure injection |
| AdBanner slips into checkout | Policy violation | CI grep check + manual QA on checkout screens |
| Migration long-running | App freeze on startup | Small data volume; runs in <1s on startup |
| i18n keys missing | Hardcoded strings slip through | CI check: `grep -r "checkout\." --include="*.tsx" src/presentation/components/cart` |

## Next Steps

1. **Write `spec.md`** with full v2.0.0 content → `/docs/modules/sales-checkout/spec.md`
2. **Write `plan.md`** with migration checklist → `/docs/modules/sales-checkout/plan.md`
3. **Write `test.md`** with complete test matrix → `/docs/modules/sales-checkout/test.md`
4. **Call `plan_exit`** with plan file path

---

*All design decisions resolved. Plan is implementation-ready.*