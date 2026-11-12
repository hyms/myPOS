# Sales & Checkout — Specification (v2.0.0 KMP)

## 1. Bounded Context

Responsible for the shopping cart, sales/purchase registration, payment methods, change calculation, and receipt generation. This module consumes product data from `catalog-inventory` and updates `financial-accumulators` atomically.

---

## 2. Domain Entities & Value Objects

All identifiers, fields, and types in code and database are in **English**. Only visible UI strings remain in Spanish via i18n keys (`composeResources/values-es/strings.xml`).

### 2.1 CartItem (Composite Pattern)

**File:** `commonMain/domain/entities/CartItem.kt`

```kotlin
package com.tpvmovil.domain.entities

import com.tpvmovil.domain.valueobjects.*

sealed interface ICartItem {
    val productId: ProductId
    val productName: String
    val productImageUri: String?
    val quantity: Double
    val discount: Money
    val unitPrice: Money
    val lineSubtotal: Money
    val lineTotal: Money
}

data class SimpleCartItem(
    override val productId: ProductId,
    override val productName: String,
    override val productImageUri: String?,
    override val quantity: Double,
    override val discount: Money,
    override val unitPrice: Money,
    override val lineSubtotal: Money,
    override val lineTotal: Money,
    val product: ProductSummary // subset: id, name, sellingPrice, purchasePrice, currentStock, imageUri
) : ICartItem

sealed class CartType {
    object SALE : CartType()
    object PURCHASE : CartType()
}

interface SimpleCartItemFactory {
    fun createFromProduct(
        product: ProductSummary,
        quantity: Double,
        discount: Money,
        cartType: CartType
    ): SimpleCartItem
}

// DTOs for use cases
data class CreateCartItemInput(
    val productId: ProductId,
    val quantity: Double,
    val discount: Double
)

data class UpdateCartItemInput(
    val quantity: Double,
    val discount: Double
)
```

### 2.2 SaleTransaction (Aggregate Root)

**File:** `commonMain/domain/entities/SaleTransaction.kt`

```kotlin
package com.tpvmovil.domain.entities

import com.tpvmovil.domain.valueobjects.Money

@JvmInline
value class TransactionId(val value: Long) : Comparable<TransactionId> {
    override fun compareTo(other: TransactionId) = value.compareTo(other.value)
}

sealed class TransactionType {
    object SALE : TransactionType()
    object PURCHASE : TransactionType()
    object EXPENSE : TransactionType()
}

data class SaleTransaction(
    val id: TransactionId,
    val type: TransactionType,
    val totalAmount: Money,
    val paymentMethod: PaymentMethod,
    val detail: String?,
    val registeredAt: String,    // ISO 8601
    val deletedAt: String?
)
```

### 2.3 TransactionDetail (Value Object)

**File:** `commonMain/domain/entities/TransactionDetail.kt`

```kotlin
package com.tpvmovil.domain.entities

import com.tpvmovil.domain.valueobjects.Money

data class TransactionDetail(
    val id: Long,
    val transactionId: TransactionId,
    val productId: ProductId,
    val quantity: Double,
    val unitPrice: Money,
    val lineTotal: Money
)
```

### 2.4 PaymentMethod (Value Object)

**File:** `commonMain/domain/entities/PaymentMethod.kt`

```kotlin
package com.tpvmovil.domain.entities

sealed class PaymentMethod(val code: String) {
    object CASH : PaymentMethod("CASH")
    object CARD : PaymentMethod("CARD")
    object TRANSFER : PaymentMethod("TRANSFER")
    object QR : PaymentMethod("QR")

    companion object {
        fun fromCode(code: String): PaymentMethod = when (code.uppercase()) {
            "CASH" -> CASH
            "CARD" -> CARD
            "TRANSFER" -> TRANSFER
            "QR" -> QR
            else -> throw IllegalArgumentException("Invalid payment method: $code")
        }
    }

    fun isCash(): Boolean = this == CASH

    override fun toString(): String = code
}
```

### 2.5 ChangeAmount (Value Object)

**File:** `commonMain/domain/valueobjects/ChangeAmount.kt`

```kotlin
package com.tpvmovil.domain.valueobjects

data class ChangeAmount(val amount: Money) {
    companion object {
        fun zero(currency: Currency): ChangeAmount = ChangeAmount(Money.zero(currency))
    }

    fun isSufficient(): Boolean = amount.amount >= 0

    fun colorClass(): String = if (isSufficient()) "success" else "danger"
}
```

### 2.6 Money / Currency (Shared Kernel)

**Files:** `commonMain/domain/valueobjects/Money.kt`, `Currency.kt` — see `system-settings/spec.md` section 2.1.

---

## 3. Domain Rules & Invariants

| ID | Rule | Description |
|---|---|---|
| RULE-SC-01 | Stock non-negative | `currentStock - quantity >= 0` before any `SALE`. `validateStockAvailable` throws `StockError` if fails. |
| RULE-SC-02 | Discount ≤ subtotal | `applyDiscount`: clamp discount to [0, subtotal]. |
| RULE-SC-03 | Single payment method | Exactly one `PaymentMethod` per transaction. No mixed/split payments (INV-04). |
| RULE-SC-04 | Cash change | `change = received - total` only if `paymentMethod === CASH`. Green if ≥ 0, red if negative. |
| RULE-SC-05 | Rounded totals | `round2(v) = (v * 100).roundToInt() / 100.0` on subtotal, discount, total, change. |
| RULE-SC-06 | Fractional quantities | Quantities and stock captured as decimals (`KeyboardType.Decimal`, step `0.001`), rounded to 3dp on persist. |
| RULE-SC-07 | Atomic transaction | Sale creation: insert transaction + details, decrement product stock, increment `financial_accumulators` — all in SQLDelight transaction block. |
| RULE-SC-08 | Stock alerts post-commit | After successful sale, collect products where `currentStock < minStock` and emit Snackbar. |

Global invariants: **INV-01, INV-03, INV-04, INV-10**.

---

## 4. Database Contract (SQLDelight)

### 4.1 Target Schema (v2.0.0 — English Columns)

**File:** `commonMain/sqldelight/schema.sq` (sales section)

```sql
CREATE TABLE sales_transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT NOT NULL,
    total_amount REAL NOT NULL,
    payment_method TEXT NOT NULL,
    detail TEXT,
    registered_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
    deleted_at INTEGER
);

CREATE TABLE transaction_details (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    transaction_id INTEGER NOT NULL,
    product_id INTEGER NOT NULL,
    quantity REAL NOT NULL,
    unit_price REAL NOT NULL,
    FOREIGN KEY(transaction_id) REFERENCES sales_transactions(id),
    FOREIGN KEY(product_id) REFERENCES products(id)
);

-- financial_accumulators (see financial-accumulators/spec.md)
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

-- Indexes
CREATE INDEX idx_sales_transactions_date ON sales_transactions(registered_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_sales_transactions_type ON sales_transactions(type) WHERE deleted_at IS NULL;
CREATE INDEX idx_sales_transactions_registered_type ON sales_transactions(registered_at, type, deleted_at);
CREATE INDEX idx_transaction_details_transaction ON transaction_details(transaction_id);
CREATE INDEX idx_transaction_details_product ON transaction_details(product_id);
```

### 4.2 Column Mapping: v1.0.0 (Spanish) → v2.0.0 (English)

| v1.0.0 Table | v1.0.0 Column | v2.0.0 Table | v2.0.0 Column |
|---|---|---|---|
| `transacciones` | `id` | `sales_transactions` | `id` |
| `transacciones` | `tipo` | `sales_transactions` | `type` |
| `transacciones` | `monto_total` | `sales_transactions` | `total_amount` |
| `transacciones` | `tipo_pago` | `sales_transactions` | `payment_method` |
| `transacciones` | `detalle` | `sales_transactions` | `detail` |
| `transacciones` | `fecha_registro` | `sales_transactions` | `registered_at` |
| `transacciones` | `fecha_delete` | `sales_transactions` | `deleted_at` |
| `detalle_transacciones` | `id` | `transaction_details` | `id` |
| `detalle_transacciones` | `transaccion_id` | `transaction_details` | `transaction_id` |
| `detalle_transacciones` | `producto_id` | `transaction_details` | `product_id` |
| `detalle_transacciones` | `cantidad` | `transaction_details` | `quantity` |
| `detalle_transacciones` | `precio_unitario` | `transaction_details` | `unit_price` |

### 4.3 Migration DDL (v1.0.0 → v2.0.0)

**File:** `commonMain/sqldelight/migrations/v2_sales_checkout.sqm`

```sql
-- v2_sales_checkout.sqm
-- Atomic migration transaction (single transaction block)

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
CREATE INDEX IF NOT EXISTS idx_sales_transactions_registered_at ON sales_transactions(registered_at) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_sales_transactions_type ON sales_transactions(type) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_sales_transactions_registered_type ON sales_transactions(registered_at, type, deleted_at);
CREATE INDEX IF NOT EXISTS idx_transaction_details_transaction ON transaction_details(transaction_id);
CREATE INDEX IF NOT EXISTS idx_transaction_details_product ON transaction_details(product_id);

-- Step 5: user_version = 2 is set centrally by DatabaseDriverFactory
```

---

## 5. Repository Interfaces

### 5.1 ISaleRepository
**File:** `commonMain/data/repositories/ISaleRepository.kt`

```kotlin
package com.tpvmovil.data.repositories

import com.tpvmovil.domain.entities.*

interface ISaleRepository {
    fun findById(id: TransactionId): SaleTransaction?
    fun findDetails(transactionId: TransactionId): List<TransactionDetail>
    fun list(options: ListOptions): List<SaleTransaction>
    fun listByPeriod(period: Period): List<TypeTotal>
    fun listExpensesThisMonth(limit: Int, offset: Int): List<SaleTransaction>
}

data class ListOptions(
    val type: TransactionType? = null,
    val limit: Int = 15,
    val offset: Int = 0
)

enum class Period { MONTH, YEAR }

data class TypeTotal(val type: TransactionType, val total: Double)
```

---

## 6. Use Cases (Application Layer — `commonMain`)

### 6.1 RegisterSale
**File:** `commonMain/application/sales/RegisterSale.kt`

```kotlin
package com.tpvmovil.application.sales

import com.tpvmovil.domain.entities.*

data class RegisterSaleInput(
    val items: List<ICartItem>,
    val paymentMethod: PaymentMethod,
    val detail: String?
)

data class RegisterSaleResult(
    val transactionId: TransactionId,
    val total: Double,
    val stockAlerts: List<StockAlert>
)

data class StockAlert(
    val id: ProductId,
    val name: String,
    val currentStock: Double,
    val minStock: Double
)

suspend fun registerSale(input: RegisterSaleInput): RegisterSaleResult
```

**Atomic Flow (SQLDelight transaction block):**
1. Validate cart not empty.
2. `calcTotals(lines, currency)`; `total.amount > 0`.
3. `validateStockAvailable` for each item (throws `StockError` if insufficient).
4. `INSERT INTO sales_transactions (type='SALE', total_amount, payment_method, detail)` → `lastInsertRowId`.
3. For each item:
   - `INSERT INTO transaction_details (transaction_id, product_id, quantity, unit_price)`
   - `UPDATE products SET current_stock = current_stock - quantity, updated_at = CURRENT_TIMESTAMP WHERE id = ?`
4. `UPDATE financial_accumulators SET total_sales += total, net_balance += total, updated_at = CURRENT_TIMESTAMP WHERE id = 1`
5. Post-commit: `collectStockAlerts` → Snackbar if `currentStock < minStock`.

### 6.2 RegisterPurchase
**File:** `commonMain/application/purchases/RegisterPurchase.kt` — mirrors `RegisterSale` with type `PURCHASE`, increments stock, decrements balance.

### 6.3 RevertTransaction (Unified)
**File:** `commonMain/application/transactions/RevertTransaction.kt` — handles `SALE`, `PURCHASE`, `EXPENSE` reversal with stock safety checks (see `financial-accumulators/spec.md` section 6.3).

---

## 7. Domain Rules (Cart Math)

**File:** `commonMain/domain/rules/CartRules.kt`

| Function | Signature | Description |
|---|---|---|
| `selectUnitPrice` | `(salePrice: Money, purchasePrice: Money, kind: CartType) -> Money` | `salePrice` if SALE, `purchasePrice` if PURCHASE. |
| `lineSubtotal` | `(line: CartLine) -> Money` | `unitPrice * quantity`, rounded to 2dp. |
| `calcTotals` | `(lines: List<CartLine>, currency: CurrencyCode) -> CartTotals` | `{ subtotal, discount, total }` as `Money`. |
| `calcChange` | `(total: Money, received: Money) -> ChangeAmount` | `received - total`, rounded to 2dp. |
| `applyDiscount` | `(subtotal: Money, discount: Money) -> Money` | Clamp [0, subtotal]. |

`CartLine` is `{ unitPrice: Money; quantity: Double; discount: Money }`.

---

## 8. UI State Machines (Spanish visible text via `stringResource`)

### 8.1 Sale/Purchase Catalog Screen (`CartScreen`)

| State | Trigger | Next State | Visible Text (ES) |
|---|---|---|---|
| `CatalogEmpty` | Search no results | — | `"No se encontraron productos"` (EmptyState) |
| `ProductSelected` | Tap product card | `LineItemDialog` | `"Agregar al carrito"` |

### 8.2 LineItemDialog

| State | Trigger | Next State | Visible Text (ES) |
|---|---|---|---|
| `QuantityInitial` | Open dialog | — | `"Cantidad"` (decimal, step 0.001) |
| `DiscountEntered` | Tap discount | — | `"Descuento (monto fijo)"`, validated ≤ subtotal |
| `Confirmed` | Tap Apply | Cart updated | `"Producto agregado"` |

### 8.3 PaymentDialog

| State | Trigger | Next State | Visible Text (ES) |
|---|---|---|---|
| `SelectPayment` | Open payment | — | `"Tipo de pago"` (EFECTIVO / TARJETA / TRANSFERENCIA / QR) |
| `AmountReceived` | Select CASH | — | `"Monto recibido"` |
| `ChangeCalculation` | Change received amount | — | `cambio = recibido - total` (green ≥ 0, red < 0) |
| `Confirm` | Tap Confirm | Receipt | `"Cobrar"` / `"Registrar"` |

### 8.4 Receipt (Comprobante)

| State | Trigger | Next State | Visible Text (ES) |
|---|---|---|---|
| `Preview` | Payment success | — | `"Recibo"` / `"Comprobante"` |
| `Shared` | Tap Share | — | `ShareSheet` |

---

## 9. Compose UI Components

| Component | File | Type | Memoized | Description |
|---|---|---|---|---|
| `CartScreen` | `composeApp/.../cart/CartScreen.kt` | Screen | N/A | Main cart with `LazyColumn` |
| `CartLine` | `composeApp/.../cart/CartLine.kt` | Composable | `key = item.productId` | Cart row: name, qty × price, line total, remove button |
| `CartSummaryBar` | `composeApp/.../cart/CartSummaryBar.kt` | Composable | No | Bottom bar: item count, total, checkout button, clear button |
| `LineItemDialog` | `composeApp/.../cart/LineItemDialog.kt` | Composable | No | Quantity + discount input, live subtotal |
| `PaymentDialog` | `composeApp/.../cart/PaymentDialog.kt` | Composable | No | Payment method radio, cash received + change calculator |
| `ProductGridCard` | `composeApp/.../catalog/ProductGridCard.kt` | Composable | `key = product.id` | 3-col catalog card (shared with catalog) |
| `SearchBar` | `composeApp/.../filters/SearchBar.kt` | Composable | No | Debounce 200ms |
| `SortMenu` | `composeApp/.../filters/SortMenu.kt` | Composable | No | Dropdown: "Nombre A-Z" / "Más vendidos" |
| `CategoryChipsRow` | `composeApp/.../filters/CategoryChipsRow.kt` | Composable | No | Clean text chips |
| `EmptyState` | `composeApp/.../feedback/EmptyState.kt` | Composable | No | Illustration + action button |
| `ReceiptPreview` | `composeApp/.../receipt/ReceiptPreview.kt` | Composable | No | HTML receipt preview before share |

---

## 10. ViewModels

| ViewModel | File | Responsibility |
|---|---|---|
| `SaleCartViewModel` | `commonMain/presentation/viewmodels/SaleCartViewModel.kt` | SALE cart with granular `StateFlow` selectors |
| `PurchaseCartViewModel` | `commonMain/presentation/viewmodels/PurchaseCartViewModel.kt` | PURCHASE cart with granular `StateFlow` selectors |
| `PaymentViewModel` | `commonMain/presentation/viewmodels/PaymentViewModel.kt` | Payment flow, change calculation, receipt generation |

---

## 11. AdWare Governance

**Rule:** **ZERO ads allowed** on the Checkout screen (`CartScreen`, `PaymentDialog`, `ReceiptPreview`).

**Implementation:**
- `AD_ENABLED` flag is **ignored/disabled** in this view.
- No `AdBanner` rendered in `LazyColumn` footer or any checkout UI.
- Prevents user distraction during payment flow.

---

## 12. Applicable NFRs

- **NFR-02:** `LazyColumn` / `LazyVerticalGrid`, sub-16ms render, stable keys.
- **NFR-03:** 0ms cash calculations — read from `financial_accumulators` accumulator only.
- **NFR-04:** Pagination `LIMIT ? OFFSET ?` with `pageSize` from settings (default 15).
- **NFR-07:** Atomic rollback via SQLDelight transaction blocks on sale/purchase creation.
- **NFR-08:** UI strings in Spanish via `stringResource`; code/DB/identifiers in English.
- **NFR-09:** Strict Kotlin (`explicit-api`, `-Xlint`).
- **NFR-10:** Tests: 100% pass unit + integration + cash/stock integrity.

---

## 13. Extension Points (v2.0.0 Roadmap)

- [ ] **Bundle/Combo products**: Composite `BundleCartItem` implementing `ICartItem`.
- [ ] **Split payments**: Multiple `PaymentMethod` per transaction (currently blocked by INV-04).
- [ ] **Native thermal printing**: Direct ESC/POS to Bluetooth printer (currently HTML → PDF → share).
- [ ] **Offline receipt queue**: Persist unsent receipts for later sync/print.
- [ ] **Customer-facing display**: Second screen mode for payment confirmation.

---

## 14. i18n Keys (Spanish UI Dictionary — `composeResources/values-es/strings.xml`)

```xml
<!-- Cart -->
<string name="checkout_cart_title_sale">Carrito de venta</string>
<string name="checkout_cart_title_purchase">Carrito de compra</string>
<string name="checkout_cart_empty">Carrito vacío</string>
<string name="checkout_cart_item_count">%1$d productos</string>
<string name="checkout_cart_subtotal">Subtotal</string>
<string name="checkout_cart_discount">Descuento</string>
<string name="checkout_cart_total">Total</string>
<string name="checkout_cart_checkout_sale">Cobrar</string>
<string name="checkout_cart_checkout_purchase">Registrar</string>
<string name="checkout_cart_clear">Vaciar carrito</string>
<string name="checkout_cart_clear_confirm">¿Eliminar todos los productos del carrito?</string>

<!-- Line Item -->
<string name="checkout_line_quantity">Cantidad</string>
<string name="checkout_line_discount">Descuento (monto fijo)</string>
<string name="checkout_line_stock_available">Stock disponible: %1$s</string>
<string name="checkout_line_subtotal">Subtotal línea</string>
<string name="checkout_line_apply">Aplicar</string>
<string name="checkout_line_cancel">Cancelar</string>
<string name="checkout_line_error_stock_exceeded">Máximo disponible: %1$s</string>
<string name="checkout_line_error_discount_exceeded">El descuento no puede superar %1$s</string>

<!-- Payment -->
<string name="checkout_payment_title_sale">Cobrar venta</string>
<string name="checkout_payment_title_purchase">Pagar compra</string>
<string name="checkout_payment_total_label">Total a cobrar / Total a pagar</string>
<string name="checkout_payment_method_label">Tipo de pago</string>
<string name="checkout_payment_cash">Efectivo</string>
<string name="checkout_payment_card">Tarjeta</string>
<string name="checkout_payment_transfer">Transferencia</string>
<string name="checkout_payment_qr">QR</string>
<string name="checkout_payment_amount_received">Monto recibido</string>
<string name="checkout_payment_exact">Exacto</string>
<string name="checkout_payment_change">Cambio</string>
<string name="checkout_payment_change_sufficient">Cambio: %1$s</string>
<string name="checkout_payment_change_insufficient">Faltante: %1$s</string>
<string name="checkout_payment_cancel">Cancelar</string>
<string name="checkout_payment_confirm">Cobrar / Registrar</string>

<!-- Receipt -->
<string name="checkout_receipt_title">Recibo / Comprobante</string>
<string name="checkout_receipt_share">Compartir</string>
<string name="checkout_receipt_sale_type">VENTA / COMPRA / GASTO</string>
<string name="checkout_receipt_transaction_id">Transacción #%1$d</string>
<string name="checkout_receipt_date">%1$s</string>
<string name="checkout_receipt_items_header">Productos</string>
<string name="checkout_receipt_payment_method">Pago: %1$s</string>
<string name="checkout_receipt_total">Total: %1$s</string>
<string name="checkout_receipt_change">Cambio: %1$s</string>

<!-- Toasts/Alerts -->
<string name="checkout_toast_sale_created">Venta registrada</string>
<string name="checkout_toast_purchase_created">Compra registrada</string>
<string name="checkout_toast_error">No se pudo registrar.</string>
<string name="checkout_toast_receipt_failed">Comprobante no generado</string>
<string name="checkout_alert_insufficient_stock">Stock insuficiente para %1$s. Disponible: %2$s</string>
<string name="checkout_alert_cart_empty">El carrito está vacío.</string>
```

---

## 15. Cross-References

- `catalog-inventory/spec.md` — Product catalog, stock validation, SKU/Stock VOs
- `financial-accumulators/spec.md` — `financial_accumulators` accumulator, `RevertTransaction` stock math
- `system-settings/spec.md` — Currency/Money VOs, pagination settings, PIN gate for deletion
- `expenses-cashflow/spec.md` — Expense registration (unified in `sales_transactions` type `EXPENSE`)

---

*Last updated: 2026-11-12 — v2.0.0 KMP Specification*