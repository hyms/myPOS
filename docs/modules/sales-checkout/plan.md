# Sales & Checkout Module — Implementation Plan (v2.0.0)

## Overview

This plan covers the implementation of the Sales & Checkout module v2.0.0, including:
- Domain layer: English entities, Composite Pattern, Value Objects
- Data layer: Repository renames, English column mappers, migration
- Application layer: RegisterSale, RegisterPurchase, RevertTransaction
- Presentation layer: Stores, hooks, components, AdWare removal
- Testing: Unit, integration, migration, atomic, E2E

---

## Phase 1: Database Migration (Critical Path)

### 1.1 Migration Runner Update
**File:** `src/data/database/migrations.ts`
- Add migration version 2 with `v2_0_0_sales_migration.sql`
- Ensure `withTransactionSync` wrapper for atomicity
- Update `user_version` to 2

### 1.2 Migration SQL
**File:** `src/data/database/migrations/v2_0_0_sales_migration.sql`

```sql
-- Atomic migration transaction (single db.withTransactionSync)

-- Step 1: Rename tables
ALTER TABLE transacciones RENAME TO sales_transactions;
ALTER TABLE detalle_transacciones RENAME TO transaction_details;

-- Step 2: Rename columns in sales_transactions
ALTER TABLE sales_transactions RENAME COLUMN tipo TO type;
ALTER TABLE sales_transactions RENAME COLUMN monto_total TO total_amount;
ALTER TABLE sales_transactions RENAME COLUMN tipo_pago TO payment_method;
ALTER TABLE sales_transactions RENAME COLUMN detalle TO detail;
ALTER TABLE sales_transactions RENAME COLUMN fecha_registro TO registered_at;
ALTER TABLE sales_transactions RENAME COLUMN fecha_delete TO deleted_at;

-- Step 3: Rename columns in transaction_details
ALTER TABLE transaction_details RENAME COLUMN transaccion_id TO transaction_id;
ALTER TABLE transaction_details RENAME COLUMN producto_id TO product_id;
ALTER TABLE transaction_details RENAME COLUMN cantidad TO quantity;
ALTER TABLE transaction_details RENAME COLUMN precio_unitario TO unit_price;

-- Step 4: Recreate indexes with English names
CREATE INDEX IF NOT EXISTS idx_sales_transactions_date ON sales_transactions(registered_at) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_sales_transactions_type ON sales_transactions(type) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_transaction_details_transaction ON transaction_details(transaction_id);
CREATE INDEX IF NOT EXISTS idx_transaction_details_product ON transaction_details(product_id);

-- Step 5: Update user_version to 2
PRAGMA user_version = 2;
```

### 1.3 Migration Checklist
- [ ] Create migration v2 in `migrations.ts` (version 2)
- [ ] Write `v2_0_0_sales_migration.sql` with atomic transaction
- [ ] Rename tables: `transacciones` → `sales_transactions`, `detalle_transacciones` → `transaction_details`
- [ ] Rename columns in `sales_transactions`: `tipo`→`type`, `monto_total`→`total_amount`, `tipo_pago`→`payment_method`, `detalle`→`detail`, `fecha_registro`→`registered_at`, `fecha_delete`→`deleted_at`
- [ ] Rename columns in `transaction_details`: `transaccion_id`→`transaction_id`, `producto_id`→`product_id`, `cantidad`→`quantity`, `precio_unitario`→`unit_price`
- [ ] Recreate indexes with English names
- [ ] Update `user_version` to 2
- [ ] Verify row counts match pre-migration
- [ ] Test restore from backup scenario

---

## Phase 2: Domain Layer

### 2.1 Entity Renames & New Entities
| Old File | New File | Changes |
|---|---|---|
| `Transaccion.ts` | `SaleTransaction.ts` | English fields, `TransactionType` enum |
| `DetalleTransaccion.ts` | `TransactionDetail.ts` | English fields |
| `CarritoItem.ts` | `CartItem.ts` | Composite Pattern: `ICartItem`, `SimpleCartItem`, `SimpleCartItemFactory` |

### 2.2 New Value Objects
- `PaymentMethod.ts` — `CASH` | `CARD` | `TRANSFER` | `QR` with `isCash()` method
- `ChangeAmount.ts` — cash change calculation with color coding

### 2.3 Domain Rules
**File:** `src/domain/rules/cartRules.ts`
- `getUnitPrice(item, type)` — returns sellingPrice (SALE) or purchasePrice (PURCHASE)
- `lineSubtotal(item, type)` — unitPrice × quantity
- `calcTotals(items, type)` — { subtotal, discount, total } rounded
- `calcChange(total, received)` — ChangeAmount (received - total)
- `applyDiscount(subtotal, discount)` — clamp [0, subtotal]

### 2.4 Stock Rules (Shared)
**File:** `src/domain/rules/stockRules.ts` — `validateStockAvailable`, `isBelowMin`, `StockError`

---

## Phase 3: Data Layer

### 3.1 Repository Renames
| Old | New | Notes |
|---|---|---|
| `ITransaccionRepository.ts` | `ISaleRepository.ts` | English interface, updated types |
| `SqliteTransaccionRepository.ts` | `SqliteSaleRepository.ts` | English column mappers |
| `ITransaccionRepository.ts` (container) | Update injection | `container.ts` registration |

### 3.2 Mappers
**File:** `src/data/mappers/rowMappers.ts`
- `mapSaleTransactionRow` — Spanish → English column mapping
- `mapTransactionDetailRow` — Spanish → English column mapping

---

## Phase 4: Application Layer

### 4.1 RegisterSale.ts
**File:** `src/application/sales/RegisterSale.ts`
- Input: `items: CartItem[]`, `paymentMethod: PaymentMethod`, `detail: string | null`
- Atomic `withTransactionSync` flow:
  1. Validate cart not empty
  2. `calcTotals` → total > 0
  3. `validateStockAvailable` for each item
  4. INSERT sales_transactions (type='SALE')
  5. For each item: INSERT transaction_details + UPDATE products stock
  6. UPDATE resumen_caja (total_sales += total, current_balance += total)
  7. Post-commit: collectStockAlerts → Toast

### 4.2 RegisterPurchase.ts
**File:** `src/application/purchases/RegisterPurchase.ts`
- Mirrors RegisterSale with:
  - type = 'PURCHASE'
  - stock increment (+quantity)
  - balance decrement (current_balance -= total)
  - total_purchases += total

### 4.3 RevertTransaction.ts
**File:** `src/application/transactions/RevertTransaction.ts`
- Handles SALE, PURCHASE, EXPENSE
- Pre-check: PURCHASE revert validates stock won't go negative
- Soft delete: UPDATE deleted_at = CURRENT_TIMESTAMP
- Stock/balance adjustments per type

---

## Phase 5: Presentation Layer

### 5.1 Zustand Stores
| Store | File | Changes |
|---|---|---|
| `carritoStore.ts` | `saleCartStore.ts` | Rename, granular selectors, `type: 'SALE'` |
| `carritoStore.ts` (purchase) | `purchaseCartStore.ts` | New store, `type: 'PURCHASE'`, granular selectors |

**Granular Selectors Pattern:**
```ts
// Instead of subscribing to entire cart
const items = useSaleCartStore(state => state.items);
const total = useSaleCartStore(state => state.total);
const itemCount = useSaleCartStore(state => state.itemCount);
```

### 5.2 Hooks
| Hook | File | Changes |
|---|---|---|
| `useCarrito.ts` | `useCart.ts` | English types, granular selectors |
| `useTransacciones.ts` | `useTransactions.ts` | English types, pagination |
| `useStockAlert.ts` | `useStockAlert.ts` | Post-sale stock check, Toast emission |

### 5.3 Components
| Component | File | Changes |
|---|---|---|
| `CartLine.tsx` | `src/presentation/components/cart/CartLine.tsx` | Memoized, English props |
| `CartSummaryBar.tsx` | `src/presentation/components/cart/CartSummaryBar.tsx` | English labels via i18n |
| `LineItemModal.tsx` | `src/presentation/components/cart/LineItemModal.tsx` | Decimal pad, discount validation |
| `PaymentModal.tsx` | `src/presentation/components/cart/PaymentModal.tsx` | Change calculator (green/red), exact button |
| `ReceiptPreview.tsx` | `src/presentation/components/receipt/ReceiptPreview.tsx` | New component for preview |

### 5.4 AdWare Removal
**Files to modify:**
- `app/(tabs)/registro.tsx` (CarritoScreen) — Remove `AdBanner` from ListFooterComponent
- `PaymentModal.tsx` — No ads
- `ReceiptPreview.tsx` — No ads

**Rule:** `AD_ENABLED` flag ignored in checkout screens.

### 5.5 i18n
**File:** `src/presentation/i18n/es/checkout.json`
- All 45+ keys from spec.md section 15
- Namespace: `checkout`

---

## Phase 6: Migration Verification

### 6.1 Data Integrity Checks
- Row counts: `sales_transactions` = old `transacciones`, `transaction_details` = old `detalle_transacciones`
- PKs preserved (id values unchanged)
- Timestamps preserved (registered_at, deleted_at)
- Soft deletes preserved (deleted_at NOT NULL rows)
- `resumen_caja` accumulator unchanged

### 6.2 Functional Verification
- Create SALE → verify stock decrement, balance increment
- Create PURCHASE → verify stock increment, balance decrement
- Revert SALE → verify stock restored, balance corrected
- Revert PURCHASE → verify stock decremented, balance restored (pre-check)
- Receipt generation for all three types

---

## Phase 7: Testing

### 7.1 Unit Tests
**Directory:** `tests/unit/sales-checkout/`
- `cartRules.test.ts` — All 5 functions with edge cases
- `PaymentMethod.test.ts` — Validation, equality, isCash()
- `ChangeAmount.test.ts` — isSufficient, getColorClass, zero
- `SimpleCartItemFactory.test.ts` — Creation, discount clamping

### 7.2 Integration Tests
**Directory:** `tests/integration/sales-checkout/`
- `RegisterSale.test.ts` — Success, insufficient stock, empty cart, cash change, atomic rollback
- `RegisterPurchase.test.ts` — Success, stock/balance changes
- `RevertTransaction.test.ts` — SALE, PURCHASE, EXPENSE, stock safety check

### 7.3 Migration Tests
**Directory:** `tests/migration/sales-checkout/`
- `v2_0_0_migration.test.ts` — Seed v1 schema, run migration, assert all mappings

### 7.4 Atomic Transaction Tests
**Directory:** `tests/integration/atomic/`
- `saleAtomicRollback.test.ts` — Simulate DB failure mid-transaction → full rollback

### 7.5 Hook Tests
**Directory:** `tests/unit/hooks/`
- `useCart.test.ts` — Granular selectors, re-render optimization
- `useTransactions.test.ts` — Pagination, filtering
- `useStockAlert.test.ts` — Toast trigger post-sale

### 7.6 UI Component Tests
**Directory:** `tests/components/cart/`
- `PaymentModal.test.tsx` — Change calculator, exact button
- `LineItemModal.test.tsx` — Quantity validation, discount clamp
- `CartSummaryBar.test.tsx` — Item count, total, buttons
- `ReceiptPreview.test.tsx` — Render transaction data

### 7.7 E2E Tests
**Directory:** `tests/e2e/sales-checkout/`
- `saleFlow.test.ts` — Add items → cash payment with change → receipt → share
- `purchaseFlow.test.ts` — Add items → card payment → receipt
- `revertSale.test.ts` — Revert → verify stock/balance
- `migrationIdempotency.test.ts` — Run migration twice → no errors

---

## Dependencies & Ordering

```
Phase 1 (Migration) → Phase 2 (Domain) → Phase 3 (Data) → Phase 4 (Application) → Phase 5 (Presentation) → Phase 6 (Verification) → Phase 7 (Testing)
```

**Parallelizable:**
- Phase 2 & 3 can run in parallel after Phase 1
- Phase 7 tests can be written alongside implementation

---

## Risk Mitigation

| Risk | Mitigation |
|---|---|
| `RENAME COLUMN` fails | SQLite ≥ 3.25 confirmed; test on device |
| Fractional quantities break UI | Update `keyboardType="decimal-pad"`, `step={0.001}` |
| Atomic transaction partial failure | Single `withTransactionSync`; test failure injection |
| AdBanner in checkout | CI grep check: `grep -r "AdBanner" --include="*.tsx" src/presentation/components/cart` |
| Migration long-running | Small data volume (<1000 rows); <1s execution |
| i18n keys missing | CI check: `grep -r "checkout\." --include="*.tsx" src/presentation/components/cart` |

---

## Definition of Done

- [ ] Migration runs atomically, preserves all data
- [ ] All entities/repos/use cases use English identifiers
- [ ] Composite Pattern implemented (`ICartItem` + `SimpleCartItem`)
- [ ] RegisterSale/RegisterPurchase/RevertTransaction work atomically
- [ ] PaymentModal calculates change correctly (green ≥0, red <0)
- [ ] Zero AdBanner in checkout screens
- [ ] All 45+ i18n keys implemented
- [ ] Unit tests: 100% pass
- [ ] Integration tests: 100% pass
- [ ] Migration tests: 100% pass
- [ ] Atomic rollback tests: 100% pass
- [ ] TypeScript strict: no errors
- [ ] Lint: no errors

---

*Plan version: 1.0 — 2026-11-10*