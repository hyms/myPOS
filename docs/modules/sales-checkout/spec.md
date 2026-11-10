# Sales & Checkout — Specification (v2.0.0)

## 1. Bounded Context

Responsible for the shopping cart, sales/purchase registration, payment methods, change calculation, and receipt generation. This module consumes product data from `catalog-inventory` and updates `financial-accumulators` atomically.

---

## 2. Domain Entities & Value Objects

All identifiers, fields, and types in code and database are in **English**. Only visible UI strings remain in Spanish via i18n keys.

### 2.1 CartItem (Composite Pattern)

**File:** `src/domain/entities/CartItem.ts`

```ts
import type { Product } from './Product';
import type { Money } from '../value-objects/Money';
import type { Stock } from '../value-objects/Stock';

export type CartType = 'SALE' | 'PURCHASE';

/**
 * ICartItem — Composite interface unifying single product lines
 * and future bundle/combo lines.
 * Currently only SimpleCartItem is implemented.
 */
export interface ICartItem {
  readonly productId: number;
  readonly productName: string;
  readonly productImageUri: string | null;
  readonly quantity: number;
  readonly discount: Money;
  readonly unitPrice: Money;
  readonly lineSubtotal: Money;
  readonly lineTotal: Money;
}

/**
 * Simple product line item — leaf in the Composite pattern.
 */
export interface SimpleCartItem extends ICartItem {
  readonly product: Pick<
    Product,
    'id' | 'name' | 'sellingPrice' | 'purchasePrice' | 'currentStock' | 'imageUri'
  >;
  readonly discount: Money;
}

/**
 * Factory for creating simple cart items.
 */
export interface SimpleCartItemFactory {
  createFromProduct(
    product: Pick<Product, 'id' | 'name' | 'sellingPrice' | 'purchasePrice' | 'currentStock' | 'imageUri'>,
    quantity: number,
    discount: Money,
    cartType: CartType
  ): SimpleCartItem;
}

// DTOs for use cases
export type CreateCartItemInput = {
  readonly productId: number;
  readonly quantity: number;
  readonly discount: number;
};

export type UpdateCartItemInput = {
  readonly quantity: number;
  readonly discount: number;
};
```

### 2.2 SaleTransaction (Aggregate Root)

**File:** `src/domain/entities/SaleTransaction.ts`

```ts
import type { PaymentMethod } from './PaymentMethod';
import type { Money } from '../value-objects/Money';

export type TransactionId = number;
export type TransactionType = 'SALE' | 'PURCHASE' | 'EXPENSE';

export interface SaleTransaction {
  readonly id: TransactionId;
  readonly type: TransactionType;
  readonly totalAmount: Money;
  readonly paymentMethod: PaymentMethod;
  readonly detail: string | null;
  readonly registeredAt: string;
  readonly deletedAt: string | null;
}
```

### 2.3 TransactionDetail (Value Object)

**File:** `src/domain/entities/TransactionDetail.ts`

```ts
import type { Money } from '../value-objects/Money';

export interface TransactionDetail {
  readonly id: number;
  readonly transactionId: TransactionId;
  readonly productId: number;
  readonly quantity: number;
  readonly unitPrice: Money;
  readonly lineTotal: Money;
}
```

### 2.4 PaymentMethod (Value Object)

**File:** `src/domain/entities/PaymentMethod.ts`

```ts
export type PaymentMethodCode = 'CASH' | 'CARD' | 'TRANSFER' | 'QR';

export class PaymentMethod {
  readonly code: PaymentMethodCode;

  constructor(code: PaymentMethodCode) {
    if (!['CASH', 'CARD', 'TRANSFER', 'QR'].includes(code)) {
      throw new Error(`Invalid payment method: ${code}`);
    }
    this.code = code;
  }

  equals(other: PaymentMethod): boolean {
    return this.code === other.code;
  }

  isCash(): boolean {
    return this.code === 'CASH';
  }

  toString(): string {
    return this.code;
  }
}
```

### 2.5 ChangeAmount (Value Object)

**File:** `src/domain/value-objects/ChangeAmount.ts`

```ts
import type { Money } from './Money';

export class ChangeAmount {
  readonly amount: Money;

  constructor(amount: Money) {
    this.amount = amount;
  }

  static zero(currency: Money['currency']): ChangeAmount {
    return new ChangeAmount(new Money(0, currency));
  }

  isSufficient(): boolean {
    return this.amount.amount >= 0;
  }

  getColorClass(): 'text-success' | 'text-danger' {
    return this.isSufficient() ? 'text-success' : 'text-danger';
  }
}
```

### 2.6 Money / Currency (Shared Kernel)

**Files:** `src/domain/value-objects/Money.ts`, `src/domain/value-objects/Currency.ts` — see `system-settings/spec.md` section 2.1.

---

## 3. Domain Rules & Invariants

| ID | Rule | Description |
|---|---|---|
| RULE-SC-01 | Stock non-negative | `currentStock - quantity >= 0` before any `SALE`. `validateStockAvailable` throws `StockError` if fails. |
| RULE-SC-02 | Discount ≤ subtotal | `applyDiscount`: clamp discount to [0, subtotal]. |
| RULE-SC-03 | Single payment method | Exactly one `PaymentMethod` per transaction. No mixed/split payments (INV-04). |
| RULE-SC-04 | Cash change | `change = received - total` only if `paymentMethod === 'CASH'`. Green if ≥ 0, red if negative. |
| RULE-SC-05 | Rounded totals | `round2(v) = Math.round(v * 100) / 100` on subtotal, discount, total, change. |
| RULE-SC-06 | Fractional quantities | Quantities and stock captured as decimals (`keyboardType="decimal-pad"`, step `0.001`), rounded to 3dp on persist. |
| RULE-SC-07 | Atomic transaction | Sale creation: insert transaction + details, decrement product stock, increment `resumen_caja` — all in `db.withTransactionSync`. |
| RULE-SC-08 | Stock alerts post-commit | After successful sale, collect products where `currentStock < minStock` and emit Toast. |

Global invariants: **INV-01, INV-03, INV-04, INV-10**.

---

## 4. Database Contract

### 4.1 Target Schema (v2.0.0 — English Columns)

```sql
-- sales_transactions (renamed from transacciones)
CREATE TABLE IF NOT EXISTS sales_transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT NOT NULL,
    total_amount REAL NOT NULL,
    payment_method TEXT NOT NULL,
    detail TEXT,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL
);

-- transaction_details (renamed from detalle_transacciones)
CREATE TABLE IF NOT EXISTS transaction_details (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    transaction_id INTEGER NOT NULL,
    product_id INTEGER NOT NULL,
    quantity REAL NOT NULL,
    unit_price REAL NOT NULL,
    FOREIGN KEY(transaction_id) REFERENCES sales_transactions(id),
    FOREIGN KEY(product_id) REFERENCES products(id)
);

-- resumen_caja (accumulator — unchanged structure, English column names in code)
CREATE TABLE IF NOT EXISTS resumen_caja (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    total_sales REAL DEFAULT 0.0,
    total_purchases REAL DEFAULT 0.0,
    total_expenses REAL DEFAULT 0.0,
    current_balance REAL DEFAULT 0.0
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_sales_transactions_date ON sales_transactions(registered_at) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_sales_transactions_type ON sales_transactions(type) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_transaction_details_transaction ON transaction_details(transaction_id);
CREATE INDEX IF NOT EXISTS idx_transaction_details_product ON transaction_details(product_id);
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

---

## 5. Repository Interfaces

### 5.1 ISaleRepository
**File:** `src/data/repositories/ISaleRepository.ts`

```ts
import type { SaleTransaction, TransactionId } from '@/domain/entities/SaleTransaction';
import type { TransactionDetail } from '@/domain/entities/TransactionDetail';
import type { TransactionType } from '@/domain/entities/SaleTransaction';
import type { PaymentMethod } from '@/domain/entities/PaymentMethod';

export interface CreateSaleTransactionInput {
  readonly type: TransactionType;
  readonly totalAmount: number;
  readonly paymentMethod: PaymentMethod;
  readonly detail: string | null;
}

export interface CreateTransactionDetailInput {
  readonly productId: number;
  readonly quantity: number;
  readonly unitPrice: number;
}

export interface ISaleRepository {
  findById(id: TransactionId): SaleTransaction | null;
  findDetails(transactionId: TransactionId): ReadonlyArray<TransactionDetail>;
  list(options: {
    readonly type?: TransactionType;
    readonly limit: number;
    readonly offset: number;
  }): ReadonlyArray<SaleTransaction>;
  listByPeriod(options: {
    readonly period: 'MONTH' | 'YEAR';
  }): ReadonlyArray<{ type: TransactionType; total: number }>;
  listExpensesThisMonth(limit: number, offset: number): ReadonlyArray<SaleTransaction>;
}
```

---

## 6. Use Cases (Application Layer)

### 6.1 RegisterSale (Factory + Repository Pattern)
**File:** `src/application/sales/RegisterSale.ts`

```ts
import type { CartItem } from '@/domain/entities/CartItem';
import type { PaymentMethod } from '@/domain/entities/PaymentMethod';

export interface RegisterSaleInput {
  readonly items: ReadonlyArray<CartItem>;
  readonly paymentMethod: PaymentMethod;
  readonly detail: string | null;
}

export interface RegisterSaleResult {
  readonly transactionId: TransactionId;
  readonly total: number;
  readonly stockAlerts: ReadonlyArray<{
    readonly id: number;
    readonly name: string;
    readonly currentStock: number;
    readonly minStock: number;
  }>;
}

export function registerSale(input: RegisterSaleInput): RegisterSaleResult;
```

**Atomic Flow (`db.withTransactionSync`):**
1. Validate cart not empty.
2. `calcTotals(items, 'SALE')`; `total > 0`.
3. `validateStockAvailable` for each item (throws `StockError` if insufficient).
4. `INSERT INTO sales_transactions (type='SALE', total_amount, payment_method, detail)` → `lastInsertRowId`.
5. For each item:
   - `INSERT INTO transaction_details (transaction_id, product_id, quantity, unit_price)`
   - `UPDATE products SET current_stock = current_stock - quantity, updated_at = CURRENT_TIMESTAMP WHERE id = ?`
6. `UPDATE resumen_caja SET total_sales += total, current_balance += total WHERE id = 1`
7. Post-commit: `collectStockAlerts` → Toast if `currentStock < minStock`.

### 6.2 RegisterPurchase
**File:** `src/application/purchases/RegisterPurchase.ts` — mirrors `RegisterSale` with type `PURCHASE`, increments stock, decrements balance.

### 6.3 RevertTransaction (Unified)
**File:** `src/application/transactions/RevertTransaction.ts` — handles `SALE`, `PURCHASE`, `EXPENSE` reversal with stock safety checks (see `financial-accumulators/spec.md` section 6.3).

---

## 7. Domain Rules (Cart Math)

**File:** `src/domain/rules/cartRules.ts`

| Function | Signature | Description |
|---|---|---|
| `getUnitPrice` | `(item: ICartItem, type: CartType) => Money` | `sellingPrice` if SALE, `purchasePrice` if PURCHASE. |
| `lineSubtotal` | `(item: ICartItem, type: CartType) => Money` | `unitPrice * quantity`. |
| `calcTotals` | `(items: ICartItem[], type: CartType) => CartTotals` | `{ subtotal, discount, total }` rounded. |
| `calcChange` | `(total: Money, received: Money) => ChangeAmount` | `received - total` rounded. |
| `applyDiscount` | `(subtotal: Money, discount: Money) => Money` | Clamp [0, subtotal]. |

---

## 8. UI State Machines (Spanish visible text)

### 8.1 Sale/Purchase Catalog Screen (`app/(tabs)/registro.tsx` → `CarritoScreen`)

| State | Trigger | Next State | Visible Text (ES) |
|---|---|---|---|
| `CatalogEmpty` | Search no results | — | `"No se encontraron productos"` (EmptyState) |
| `ProductSelected` | Tap product card | `LineItemModal` | `"Agregar al carrito"` |

### 8.2 LineItemModal

| State | Trigger | Next State | Visible Text (ES) |
|---|---|---|---|
| `QuantityInitial` | Open modal | — | `"Cantidad"` (decimal, step 0.001) |
| `DiscountEntered` | Tap discount | — | `"Descuento (monto fijo)"`, validated ≤ subtotal |
| `Confirmed` | Tap Apply | Cart updated | `"Producto agregado"` |

### 8.3 PaymentModal

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
| `Shared` | Tap Share | — | `Sharing.shareAsync(uri)` |

---

## 9. UI Components

| Component | File | Type | Memoized | Description |
|---|---|---|---|---|
| `CartLine` | `src/presentation/components/cart/CartLine.tsx` | UI | Yes | Cart row: name, qty × price, line total, remove button |
| `CartSummaryBar` | `src/presentation/components/cart/CartSummaryBar.tsx` | UI | No | Bottom bar: item count, total, checkout button, clear button |
| `LineItemModal` | `src/presentation/components/cart/LineItemModal.tsx` | UI | No | Quantity + discount input, live subtotal |
| `PaymentModal` | `src/presentation/components/cart/PaymentModal.tsx` | UI | No | Payment method radio, cash received + change calculator |
| `ProductGridCard` | `src/presentation/components/product/ProductGridCard.tsx` | UI | Yes | 3-col catalog card (shared with catalog) |
| `SearchBar` | `src/presentation/components/filters/SearchBar.tsx` | UI | No | Debounce 200ms |
| `SortMenu` | `src/presentation/components/filters/SortMenu.tsx` | UI | No | Dropdown: "Nombre A-Z" / "Más vendidos" |
| `CategoryChipsRow` | `src/presentation/components/filters/CategoryChipsRow.tsx` | UI | No | Clean text chips |
| `EmptyState` | `src/presentation/components/feedback/EmptyState.tsx` | UI | No | Illustration + action button |
| `ReceiptPreview` | `src/presentation/components/receipt/ReceiptPreview.tsx` | UI | No | HTML receipt preview before share |

---

## 10. Hooks

| Hook | File | Responsibility |
|---|---|---|
| `useCart` | `src/presentation/hooks/useCart.ts` | Granular Zustand selectors for cart items, totals |
| `useTransactions` | `src/presentation/hooks/useTransactions.ts` | Paginated transaction list |
| `useStockAlert` | `src/presentation/hooks/useStockAlert.ts` | Toast if `currentStock < minStock` post-sale |

---

## 11. Zustand Stores

| Store | File | Responsibility |
|---|---|---|
| `saleCartStore` | `src/presentation/stores/saleCartStore.ts` | SALE cart with granular selectors |
| `purchaseCartStore` | `src/presentation/stores/purchaseCartStore.ts` | PURCHASE cart with granular selectors |

---

## 12. AdWare Governance

**Rule:** **ZERO ads allowed** on the Checkout screen (`CarritoScreen`, `PaymentModal`, `ReceiptPreview`).

**Implementation:**
- `AD_ENABLED` flag is **ignored/disabled** in this view.
- No `AdBanner` rendered in `ListFooterComponent` or any checkout UI.
- Prevents user distraction during payment flow.

---

## 13. Applicable NFRs

- **NFR-02:** FlashList `numColumns={3}`, sub-50ms render, no `estimatedItemSize`.
- **NFR-03:** 0ms cash calculations — read from `resumen_caja` accumulator only.
- **NFR-04:** Pagination `LIMIT ? OFFSET ?` with `pageSize` from settings (default 15).
- **NFR-07:** Atomic rollback via `withTransactionSync` on sale/purchase creation.
- **NFR-08:** UI strings in Spanish; code/DB/identifiers in English.
- **NFR-09:** Strict TypeScript (`strict`, `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes`).
- **NFR-10:** Tests: 100% pass unit + integration + cash/stock integrity.

---

## 14. Extension Points (v2.0.0 Roadmap)

- [ ] **Bundle/Combo products**: Composite `BundleCartItem` implementing `ICartItem`.
- [ ] **Split payments**: Multiple `PaymentMethod` per transaction (currently blocked by INV-04).
- [ ] **Native thermal printing**: Direct ESC/POS to Bluetooth printer (currently `expo-print` HTML → PDF → share).
- [ ] **Offline receipt queue**: Persist unsent receipts for later sync/print.
- [ ] **Customer-facing display**: Second screen mode for payment confirmation.

---

## 15. i18n Keys (Spanish UI Dictionary)

| Key | Text (ES) | Context |
|---|---|---|
| `checkout.cart.title` | Carrito de venta / Carrito de compra | Cart header |
| `checkout.cart.empty` | Carrito vacío | Empty cart state |
| `checkout.cart.item_count` | {count} productos | Item count badge |
| `checkout.cart.subtotal` | Subtotal | Subtotal label |
| `checkout.cart.discount` | Descuento | Discount label |
| `checkout.cart.total` | Total | Total label |
| `checkout.cart.checkout` | Cobrar / Registrar | Checkout button |
| `checkout.cart.clear` | Vaciar carrito | Clear cart button |
| `checkout.cart.clear_confirm` | ¿Eliminar todos los productos del carrito? | Clear confirmation |
| `checkout.line.quantity` | Cantidad | LineItemModal label |
| `checkout.line.discount` | Descuento (monto fijo) | LineItemModal label |
| `checkout.line.stock_available` | Stock disponible: {stock} | LineItemModal helper |
| `checkout.line.subtotal` | Subtotal línea | LineItemModal label |
| `checkout.line.apply` | Aplicar | LineItemModal button |
| `checkout.line.cancel` | Cancelar | LineItemModal button |
| `checkout.line.error.stock_exceeded` | Máximo disponible: {stock} | Stock validation error |
| `checkout.line.error.discount_exceeded` | El descuento no puede superar {subtotal} | Discount validation error |
| `checkout.payment.title` | Cobrar venta / Pagar compra | PaymentModal title |
| `checkout.payment.total_label` | Total a cobrar / Total a pagar | PaymentModal label |
| `checkout.payment.method_label` | Tipo de pago | Payment method header |
| `checkout.payment.cash` | Efectivo | Cash option |
| `checkout.payment.card` | Tarjeta | Card option |
| `checkout.payment.transfer` | Transferencia | Transfer option |
| `checkout.payment.qr` | QR | QR option |
| `checkout.payment.amount_received` | Monto recibido | Cash input label |
| `checkout.payment.exact` | Exacto | Set exact amount button |
| `checkout.payment.change` | Cambio | Change label |
| `checkout.payment.change_sufficient` | Cambio: {amount} | Positive change |
| `checkout.payment.change_insufficient` | Faltante: {amount} | Negative change |
| `checkout.payment.cancel` | Cancelar | PaymentModal button |
| `checkout.payment.confirm` | Cobrar / Registrar | PaymentModal confirm |
| `checkout.receipt.title` | Recibo / Comprobante | Receipt title |
| `checkout.receipt.share` | Compartir | Share button |
| `checkout.receipt.sale_type` | VENTA / COMPRA / GASTO | Receipt type |
| `checkout.receipt.transaction_id` | Transacción #{id} | Receipt transaction ID |
| `checkout.receipt.date` | {date} | Receipt date |
| `checkout.receipt.items_header` | Productos | Items section header |
| `checkout.receipt.payment_method` | Pago: {method} | Receipt payment method |
| `checkout.receipt.total` | Total: {amount} | Receipt total |
| `checkout.receipt.change` | Cambio: {amount} | Receipt change (cash only) |
| `checkout.toast.sale_created` | Venta registrada | Toast success |
| `checkout.toast.purchase_created` | Compra registrada | Toast success |
| `checkout.toast.error` | No se pudo registrar. | Toast error |
| `checkout.toast.receipt_failed` | Comprobante no generado | Toast warning |
| `checkout.alert.insufficient_stock` | Stock insuficiente para {name}. Disponible: {stock} | StockError toast |
| `checkout.alert.cart_empty` | El carrito está vacío. | Validation error |

---

## 16. Cross-References

- `catalog-inventory/spec.md` — Product catalog, stock validation, SKU/Stock VOs
- `financial-accumulators/spec.md` — `resumen_caja` accumulator, `RevertTransaction` stock math
- `system-settings/spec.md` — Currency/Money VOs, pagination settings, PIN gate for deletion
- `expenses-cashflow/spec.md` — Expense registration (unified in `sales_transactions` type `EXPENSE`)

---

*Last updated: 2026-11-10 — v2.0.0 Specification*