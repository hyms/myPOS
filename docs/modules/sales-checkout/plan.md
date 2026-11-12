# Sales & Checkout Module — Implementation Plan (v2.0.0 KMP)

> **Status (2026-11-12):** KMP migration planning phase. Phase 0 (Shared Kernel definition) complete on paper. Implementation begins with Phase 2 after Catalog.

## Overview

This plan covers the implementation of the Sales & Checkout module v2.0.0 KMP, including:
- Domain layer: English entities, Composite Pattern, Value Objects
- Data layer: SQLDelight repositories, English column mappers, migration
- Application layer: RegisterSale, RegisterPurchase, RevertTransaction
- Presentation layer: ViewModels, Compose UI components, AdWare removal
- Testing: Unit, integration, migration, atomic, E2E

---

## Phase 1: Database Migration (Critical Path)

### 1.1 Migration Runner Update
**File:** `commonMain/data/database/DatabaseDriverFactory.kt`
- Register the sales step in the version-2 migration list (all v2 modules share one transaction)
- `user_version = 2` is set centrally by `DatabaseDriverFactory` after every v2 step succeeds

### 1.2 Migration SQL
**File:** `commonMain/sqldelight/migrations/v2_sales_checkout.sqm` (see spec.md section 4.3)

### 1.3 Migration Checklist
- [ ] Create migration v2 in `DatabaseDriverFactory` migration list
- [ ] Write `v2_sales_checkout.sqm` with atomic transaction
- [ ] Rename tables: `transacciones` → `sales_transactions`, `detalle_transacciones` → `transaction_details`
- [ ] Rename columns in `sales_transactions`: `tipo`→`type`, `monto_total`→`total_amount`, `tipo_pago`→`payment_method`, `detalle`→`detail`, `fecha_registro`→`registered_at`, `fecha_delete`→`deleted_at`
- [ ] Rename columns in `transaction_details`: `transaccion_id`→`transaction_id`, `producto_id`→`product_id`, `cantidad`→`quantity`, `precio_unitario`→`unit_price`
- [ ] Recreate indexes with English names
- [ ] `user_version` = 2 (centralized)
- [ ] Verify row counts match pre-migration
- [ ] Test restore from backup scenario

---

## Phase 2: Domain Layer (`commonMain/domain/`)

### 2.1 Entity Files
| New File | Purpose |
|---|---|
| `entities/SaleTransaction.kt` | English fields, `TransactionType` sealed class |
| `entities/TransactionDetail.kt` | English fields |
| `entities/CartItem.kt` | Composite Pattern: `ICartItem`, `SimpleCartItem`, `SimpleCartItemFactory` |

### 2.2 Value Objects
- `entities/PaymentMethod.kt` — `CASH` | `CARD` | `TRANSFER` | `QR` with `isCash()` method
- `valueobjects/ChangeAmount.kt` — cash change calculation with color coding

### 2.3 Domain Rules
**File:** `rules/CartRules.kt`
- `selectUnitPrice(item, type)` — returns sellingPrice (SALE) or purchasePrice (PURCHASE)
- `lineSubtotal(item, type)` — unitPrice × quantity
- `calcTotals(items, type)` — { subtotal, discount, total } rounded
- `calcChange(total, received)` — ChangeAmount (received - total)
- `applyDiscount(subtotal, discount)` — clamp [0, subtotal]

### 2.4 Stock Rules (Shared)
**File:** `rules/StockRules.kt` — `validateStockAvailable`, `isBelowMin`, `StockError`

---

## Phase 3: Data Layer (`commonMain/data/repositories/`)

### 3.1 Repository Interfaces & Implementations
| Interface | Implementation | Notes |
|---|---|---|
| `ISaleRepository.kt` | `impl/SqlDelightSaleRepository.kt` | English interface, SQLDelight queries |

### 3.2 SQLDelight Queries
**File:** `commonMain/sqldelight/queries.sq` (or embedded in repository)
- All queries use English column names
- `mapSaleTransactionRow`, `mapTransactionDetailRow` via SQLDelight mappers

---

## Phase 4: Application Layer (`commonMain/application/`)

### 4.1 RegisterSale.kt
**File:** `application/sales/RegisterSale.kt`
- Input: `items: List<ICartItem>`, `paymentMethod: PaymentMethod`, `detail: String?`
- Atomic SQLDelight transaction block:
  1. Validate cart not empty
  2. `calcTotals` → total > 0
  3. `validateStockAvailable` for each item
  4. INSERT sales_transactions (type='SALE')
  5. For each item: INSERT transaction_details + UPDATE products stock
  6. UPDATE financial_accumulators (total_sales += total, net_balance += total)
  7. Post-commit: collectStockAlerts → Snackbar

### 4.2 RegisterPurchase.kt
**File:** `application/purchases/RegisterPurchase.kt`
- Mirrors RegisterSale with:
  - type = 'PURCHASE'
  - stock increment (+quantity)
  - balance decrement (net_balance -= total)
  - total_purchases += total

### 4.3 RevertTransaction.kt
**File:** `application/transactions/RevertTransaction.kt` (shared)
- Handles `SALE`, `PURCHASE`, `EXPENSE`
- Pre-check: PURCHASE revert validates stock won't go negative
- Soft delete: UPDATE deleted_at = CURRENT_TIMESTAMP
- Stock/balance adjustments per type

---

## Phase 5: Presentation Layer

### 5.1 ViewModels (`commonMain/presentation/viewmodels/`)
| ViewModel | File | Changes |
|---|---|---|
| `SaleCartViewModel` | `SaleCartViewModel.kt` | Granular `StateFlow` selectors, `type = SALE` |
| `PurchaseCartViewModel` | `PurchaseCartViewModel.kt` | Granular `StateFlow` selectors, `type = PURCHASE` |
| `PaymentViewModel` | `PaymentViewModel.kt` | Payment flow, change calculation |

**Granular Selectors Pattern:**
```kotlin
// Instead of subscribing to entire cart
val items = saleCartViewModel.itemsFlow // StateFlow<List<ICartItem>>
val total = saleCartViewModel.totalFlow // StateFlow<Money>
val itemCount = saleCartViewModel.itemCountFlow // StateFlow<Int>
```

### 5.2 Compose UI Components (`composeApp/src/commonMain/compose/`)

| Component | File | Changes |
|---|---|---|
| `CartScreen.kt` | `cart/CartScreen.kt` | `LazyColumn`, English props, i18n |
| `CartLine.kt` | `cart/CartLine.kt` | Memoized with `key`, English labels |
| `CartSummaryBar.kt` | `cart/CartSummaryBar.kt` | English labels via `stringResource` |
| `LineItemDialog.kt` | `cart/LineItemDialog.kt` | Decimal keyboard, discount validation |
| `PaymentDialog.kt` | `cart/PaymentDialog.kt` | Change calculator (green/red), exact button |
| `ReceiptPreview.kt` | `receipt/ReceiptPreview.kt` | New component for preview |

### 5.3 AdWare Removal
**Files to modify:**
- `CartScreen.kt` — Remove `AdBanner` from footer
- `PaymentDialog.kt` — No ads
- `ReceiptPreview.kt` — No ads

**Rule:** `AD_ENABLED` flag ignored in checkout screens.

### 5.4 i18n
**File:** `composeApp/src/commonMain/composeResources/values-es/strings.xml`
- All 45+ keys from spec.md section 14
- Namespace: `checkout_`

---

## Phase 6: Migration Verification

### 6.1 Data Integrity Checks
- Row counts: `sales_transactions` = old `transacciones`, `transaction_details` = old `detalle_transacciones`
- PKs preserved (id values unchanged)
- Timestamps preserved (registered_at, deleted_at)
- Soft deletes preserved (deleted_at NOT NULL rows)
- `financial_accumulators` accumulator unchanged

### 6.2 Functional Verification
- Create SALE → verify stock decrement, balance increment
- Create PURCHASE → verify stock increment, balance decrement
- Revert SALE → verify stock restored, balance corrected
- Revert PURCHASE → verify stock decremented, balance restored (pre-check)
- Receipt generation for all three types

---

## Phase 7: Testing

### 7.1 Unit Tests
**Directory:** `commonTest/unit/sales-checkout/`
- `CartRulesTest.kt` — All 5 functions with edge cases
- `PaymentMethodTest.kt` — Validation, equality, isCash()
- `ChangeAmountTest.kt` — isSufficient, colorClass, zero
- `SimpleCartItemFactoryTest.kt` — Creation, discount clamping

### 7.2 Integration Tests
**Directory:** `commonTest/integration/sales-checkout/`
- `RegisterSaleTest.kt` — Success, insufficient stock, empty cart, cash change, atomic rollback
- `RegisterPurchaseTest.kt` — Success, stock/balance changes
- `RevertTransactionTest.kt` — SALE, PURCHASE, EXPENSE, stock safety check

### 7.3 Migration Tests
**Directory:** `commonTest/migration/sales-checkout/`
- `V2SalesCheckoutMigrationTest.kt` — Seed v1 schema, run migration, assert all mappings

### 7.4 Atomic Transaction Tests
**Directory:** `commonTest/atomic/`
- `SaleAtomicRollbackTest.kt` — Simulate DB failure mid-transaction → full rollback

### 7.5 ViewModel Tests (Turbine)
**Directory:** `commonTest/viewmodels/sales-checkout/`
- `SaleCartViewModelTest.kt` — Granular selectors, re-render optimization
- `PurchaseCartViewModelTest.kt` — Granular selectors, re-render optimization
- `PaymentViewModelTest.kt` — Payment flow, change calculation

### 7.6 Compose UI Tests (Robolectric)
**Directory:** `androidTest/compose/cart/`
- `PaymentDialogTest.kt` — Change calculator, exact button
- `LineItemDialogTest.kt` — Quantity validation, discount clamp
- `CartSummaryBarTest.kt` — Item count, total, buttons
- `ReceiptPreviewTest.kt` — Render transaction data

### 7.7 E2E Tests (Maestro)
**Directory:** `maestro/sales-checkout/`
- `saleFlow.yaml` — Add items → cash payment with change → receipt → share
- `purchaseFlow.yaml` — Add items → card payment → receipt
- `revertSale.yaml` — Revert → verify stock/balance
- `migrationIdempotency.yaml` — Run migration twice → no errors

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
| `ALTER TABLE` fails | SQLite ≥ 3.25 confirmed; test on device |
| Fractional quantities break UI | Update `KeyboardType.Decimal`, step = 0.001 |
| Atomic transaction partial failure | Single transaction block; test failure injection |
| AdBanner in checkout | CI grep check: `grep -r "AdBanner" --include="*.kt" composeApp/.../cart` |
| Migration long-running | Small data volume (<1000 rows); <1s execution |
| i18n keys missing | CI check: `grep -r "checkout_" --include="*.kt" composeApp/.../cart` |

---

## Definition of Done

- [ ] Migration runs atomically, preserves all data
- [ ] All entities/repos/use cases use English identifiers
- [ ] Composite Pattern implemented (`ICartItem` + `SimpleCartItem`)
- [ ] RegisterSale/RegisterPurchase/RevertTransaction work atomically
- [ ] PaymentDialog calculates change correctly (green ≥0, red <0)
- [ ] Zero AdBanner in checkout screens
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