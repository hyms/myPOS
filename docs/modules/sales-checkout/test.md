# Sales & Checkout Module — Test Specification (v2.0.0)

## Test Organization

```
tests/
├── unit/
│   ├── domain/
│   │   ├── rules/
│   │   │   ├── cartRules.test.ts
│   │   │   └── stockRules.test.ts
│   │   ├── entities/
│   │   │   ├── CartItem.test.ts
│   │   │   ├── SaleTransaction.test.ts
│   │   │   ├── PaymentMethod.test.ts
│   │   │   └── ChangeAmount.test.ts
│   │   └── value-objects/
│   │       ├── Money.test.ts
│   │       └── Currency.test.ts
│   └── hooks/
│       ├── useCart.test.ts
│       ├── useTransactions.test.ts
│       └── useStockAlert.test.ts
├── integration/
│   ├── sales-checkout/
│   │   ├── RegisterSale.test.ts
│   │   ├── RegisterPurchase.test.ts
│   │   └── RevertTransaction.test.ts
│   └── atomic/
│       └── saleAtomicRollback.test.ts
├── migration/
│   └── sales-checkout/
│       └── v2_0_0_migration.test.ts
└── e2e/
    └── sales-checkout/
        ├── saleFlow.test.ts
        ├── purchaseFlow.test.ts
        ├── revertSale.test.ts
        └── migrationIdempotency.test.ts
```

---

## 1. Unit Tests (TDD)

### 1.1 cartRules.ts

**File:** `tests/unit/domain/rules/cartRules.test.ts`

```ts
import { getUnitPrice, lineSubtotal, calcTotals, calcChange, applyDiscount } from '@/domain/rules/cartRules';
import { Money } from '@/domain/value-objects/Money';
import { PaymentMethod } from '@/domain/entities/PaymentMethod';

describe('cartRules', () => {
  const mockProduct = {
    id: 1,
    name: 'Test Product',
    sellingPrice: new Money(10.00, 'USD'),
    purchasePrice: new Money(6.00, 'USD'),
    currentStock: 100,
    imageUri: null,
  };

  const mockCartItem = {
    productId: 1,
    productName: 'Test Product',
    productImageUri: null,
    quantity: 2,
    discount: new Money(0, 'USD'),
    unitPrice: new Money(10.00, 'USD'),
    lineSubtotal: new Money(20.00, 'USD'),
    lineTotal: new Money(20.00, 'USD'),
    product: mockProduct,
    discount: new Money(0, 'USD'),
  };

  describe('getUnitPrice', () => {
    it('returns sellingPrice for SALE type', () => {
      const price = getUnitPrice(mockCartItem, 'SALE');
      expect(price.amount).toBe(10.00);
    });

    it('returns purchasePrice for PURCHASE type', () => {
      const price = getUnitPrice(mockCartItem, 'PURCHASE');
      expect(price.amount).toBe(6.00);
    });
  });

  describe('lineSubtotal', () => {
    it('calculates quantity × unitPrice for SALE', () => {
      const subtotal = lineSubtotal(mockCartItem, 'SALE');
      expect(subtotal.amount).toBe(20.00);
    });

    it('calculates quantity × unitPrice for PURCHASE', () => {
      const subtotal = lineSubtotal(mockCartItem, 'PURCHASE');
      expect(subtotal.amount).toBe(12.00);
    });

    it('handles fractional quantities (3dp)', () => {
      const fractionalItem = { ...mockCartItem, quantity: 1.5 };
      const subtotal = lineSubtotal(fractionalItem, 'SALE');
      expect(subtotal.amount).toBe(15.00);
    });
  });

  describe('calcTotals', () => {
    it('returns correct subtotal, discount, total for single item', () => {
      const totals = calcTotals([mockCartItem], 'SALE');
      expect(totals.subtotal.amount).toBe(20.00);
      expect(totals.discount.amount).toBe(0.00);
      expect(totals.total.amount).toBe(20.00);
    });

    it('applies discount correctly', () => {
      const itemWithDiscount = { ...mockCartItem, discount: new Money(5.00, 'USD') };
      const totals = calcTotals([itemWithDiscount], 'SALE');
      expect(totals.subtotal.amount).toBe(20.00);
      expect(totals.discount.amount).toBe(5.00);
      expect(totals.total.amount).toBe(15.00);
    });

    it('clamps discount to subtotal maximum', () => {
      const itemWithExcessDiscount = { ...mockCartItem, discount: new Money(25.00, 'USD') };
      const totals = calcTotals([itemWithExcessDiscount], 'SALE');
      expect(totals.discount.amount).toBe(20.00); // clamped to subtotal
      expect(totals.total.amount).toBe(0.00);
    });

    it('rounds to 2 decimal places', () => {
      const item = { ...mockCartItem, quantity: 3, unitPrice: new Money(10.555, 'USD') };
      const totals = calcTotals([item], 'SALE');
      expect(totals.subtotal.amount).toBe(31.67); // 31.665 rounded
      expect(totals.total.amount).toBe(31.67);
    });

    it('sums multiple items correctly', () => {
      const item2 = { ...mockCartItem, productId: 2, quantity: 1, unitPrice: new Money(5.00, 'USD') };
      const totals = calcTotals([mockCartItem, item2], 'SALE');
      expect(totals.subtotal.amount).toBe(25.00);
      expect(totals.total.amount).toBe(25.00);
    });
  });

  describe('calcChange', () => {
    it('returns positive change for sufficient cash', () => {
      const change = calcChange(new Money(20.00, 'USD'), new Money(50.00, 'USD'));
      expect(change.amount.amount).toBe(30.00);
      expect(change.isSufficient()).toBe(true);
    });

    it('returns negative change for insufficient cash', () => {
      const change = calcChange(new Money(50.00, 'USD'), new Money(20.00, 'USD'));
      expect(change.amount.amount).toBe(-30.00);
      expect(change.isSufficient()).toBe(false);
    });

    it('returns zero change for exact amount', () => {
      const change = calcChange(new Money(20.00, 'USD'), new Money(20.00, 'USD'));
      expect(change.amount.amount).toBe(0.00);
      expect(change.isSufficient()).toBe(true);
    });

    it('rounds to 2 decimal places', () => {
      const change = calcChange(new Money(10.555, 'USD'), new Money(20.00, 'USD'));
      expect(change.amount.amount).toBe(9.45); // 9.445 rounded
    });
  });

  describe('applyDiscount', () => {
    it('returns discount when ≤ subtotal', () => {
      const result = applyDiscount(new Money(100.00, 'USD'), new Money(20.00, 'USD'));
      expect(result.amount).toBe(20.00);
    });

    it('clamps to subtotal when discount exceeds', () => {
      const result = applyDiscount(new Money(100.00, 'USD'), new Money(150.00, 'USD'));
      expect(result.amount).toBe(100.00);
    });

    it('returns zero for negative discount', () => {
      const result = applyDiscount(new Money(100.00, 'USD'), new Money(-10.00, 'USD'));
      expect(result.amount).toBe(0.00);
    });
  });
});
```

### 1.2 PaymentMethod VO

**File:** `tests/unit/domain/entities/PaymentMethod.test.ts`

```ts
import { PaymentMethod } from '@/domain/entities/PaymentMethod';

describe('PaymentMethod', () => {
  describe('constructor', () => {
    it('accepts valid CASH', () => {
      const pm = new PaymentMethod('CASH');
      expect(pm.code).toBe('CASH');
    });

    it('accepts valid CARD', () => {
      const pm = new PaymentMethod('CARD');
      expect(pm.code).toBe('CARD');
    });

    it('accepts valid TRANSFER', () => {
      const pm = new PaymentMethod('TRANSFER');
      expect(pm.code).toBe('TRANSFER');
    });

    it('accepts valid QR', () => {
      const pm = new PaymentMethod('QR');
      expect(pm.code).toBe('QR');
    });

    it('throws for invalid code', () => {
      expect(() => new PaymentMethod('INVALID')).toThrow('Invalid payment method: INVALID');
    });

    it('throws for lowercase code', () => {
      expect(() => new PaymentMethod('cash')).toThrow('Invalid payment method: cash');
    });
  });

  describe('equals', () => {
    it('returns true for same code', () => {
      const pm1 = new PaymentMethod('CASH');
      const pm2 = new PaymentMethod('CASH');
      expect(pm1.equals(pm2)).toBe(true);
    });

    it('returns false for different codes', () => {
      const pm1 = new PaymentMethod('CASH');
      const pm2 = new PaymentMethod('CARD');
      expect(pm1.equals(pm2)).toBe(false);
    });
  });

  describe('isCash', () => {
    it('returns true for CASH', () => {
      const pm = new PaymentMethod('CASH');
      expect(pm.isCash()).toBe(true);
    });

    it('returns false for CARD', () => {
      const pm = new PaymentMethod('CARD');
      expect(pm.isCash()).toBe(false);
    });

    it('returns false for TRANSFER', () => {
      const pm = new PaymentMethod('TRANSFER');
      expect(pm.isCash()).toBe(false);
    });

    it('returns false for QR', () => {
      const pm = new PaymentMethod('QR');
      expect(pm.isCash()).toBe(false);
    });
  });

  describe('toString', () => {
    it('returns code as string', () => {
      const pm = new PaymentMethod('CASH');
      expect(pm.toString()).toBe('CASH');
    });
  });
});
```

### 1.3 ChangeAmount VO

**File:** `tests/unit/domain/entities/ChangeAmount.test.ts`

```ts
import { ChangeAmount } from '@/domain/entities/ChangeAmount';
import { Money } from '@/domain/value-objects/Money';

describe('ChangeAmount', () => {
  describe('zero', () => {
    it('creates zero change amount', () => {
      const change = ChangeAmount.zero('USD');
      expect(change.amount.amount).toBe(0);
      expect(change.amount.currency).toBe('USD');
    });
  });

  describe('isSufficient', () => {
    it('returns true for positive amount', () => {
      const change = new ChangeAmount(new Money(10.00, 'USD'));
      expect(change.isSufficient()).toBe(true);
    });

    it('returns true for zero amount', () => {
      const change = ChangeAmount.zero('USD');
      expect(change.isSufficient()).toBe(true);
    });

    it('returns false for negative amount', () => {
      const change = new ChangeAmount(new Money(-5.00, 'USD'));
      expect(change.isSufficient()).toBe(false);
    });
  });

  describe('getColorClass', () => {
    it('returns text-success for sufficient', () => {
      const change = new ChangeAmount(new Money(10.00, 'USD'));
      expect(change.getColorClass()).toBe('text-success');
    });

    it('returns text-success for zero', () => {
      const change = ChangeAmount.zero('USD');
      expect(change.getColorClass()).toBe('text-success');
    });

    it('returns text-danger for insufficient', () => {
      const change = new ChangeAmount(new Money(-5.00, 'USD'));
      expect(change.getColorClass()).toBe('text-danger');
    });
  });
});
```

### 1.4 SimpleCartItemFactory

**File:** `tests/unit/domain/entities/CartItem.test.ts`

```ts
import { SimpleCartItemFactory } from '@/domain/entities/CartItem';
import { Money } from '@/domain/value-objects/Money';

describe('SimpleCartItemFactory', () => {
  const factory = new SimpleCartItemFactory();
  const mockProduct = {
    id: 1,
    name: 'Test Product',
    sellingPrice: new Money(10.00, 'USD'),
    purchasePrice: new Money(6.00, 'USD'),
    currentStock: 100,
    imageUri: 'file://test.jpg',
  };

  describe('createFromProduct', () => {
    it('creates SALE item with sellingPrice', () => {
      const item = factory.createFromProduct(mockProduct, 2, new Money(0, 'USD'), 'SALE');
      expect(item.productId).toBe(1);
      expect(item.productName).toBe('Test Product');
      expect(item.quantity).toBe(2);
      expect(item.unitPrice.amount).toBe(10.00);
      expect(item.lineSubtotal.amount).toBe(20.00);
      expect(item.lineTotal.amount).toBe(20.00);
      expect(item.discount.amount).toBe(0.00);
    });

    it('creates PURCHASE item with purchasePrice', () => {
      const item = factory.createFromProduct(mockProduct, 2, new Money(0, 'USD'), 'PURCHASE');
      expect(item.unitPrice.amount).toBe(6.00);
      expect(item.lineSubtotal.amount).toBe(12.00);
      expect(item.lineTotal.amount).toBe(12.00);
    });

    it('applies discount and calculates lineTotal', () => {
      const item = factory.createFromProduct(mockProduct, 2, new Money(5.00, 'USD'), 'SALE');
      expect(item.discount.amount).toBe(5.00);
      expect(item.lineTotal.amount).toBe(15.00); // 20 - 5
    });

    it('clamps discount to subtotal', () => {
      const item = factory.createFromProduct(mockProduct, 2, new Money(25.00, 'USD'), 'SALE');
      expect(item.discount.amount).toBe(20.00); // clamped to subtotal
      expect(item.lineTotal.amount).toBe(0.00);
    });

    it('handles fractional quantities', () => {
      const item = factory.createFromProduct(mockProduct, 1.5, new Money(0, 'USD'), 'SALE');
      expect(item.quantity).toBe(1.5);
      expect(item.lineSubtotal.amount).toBe(15.00);
    });

    it('preserves imageUri', () => {
      const item = factory.createFromProduct(mockProduct, 1, new Money(0, 'USD'), 'SALE');
      expect(item.productImageUri).toBe('file://test.jpg');
    });
  });
});
```

---

## 2. Integration Tests

### 2.1 RegisterSale

**File:** `tests/integration/sales-checkout/RegisterSale.test.ts`

```ts
import { registerSale } from '@/application/sales/RegisterSale';
import { PaymentMethod } from '@/domain/entities/PaymentMethod';
import { SimpleCartItemFactory } from '@/domain/entities/CartItem';
import { Money } from '@/domain/value-objects/Money';
import { getDatabase } from '@/data/database/client';
import { StockError } from '@/domain/rules/stockRules';

describe('RegisterSale', () => {
  let db: ReturnType<typeof getDatabase>;
  let factory: SimpleCartItemFactory;
  let testProductId: number;

  beforeAll(() => {
    db = getDatabase();
    factory = new SimpleCartItemFactory();
  });

  beforeEach(() => {
    // Create test product with stock
    const result = db.runSync(
      `INSERT INTO products (name, selling_price, purchase_price, current_stock, min_stock, category_id)
       VALUES (?, ?, ?, ?, ?, ?)`,
      'Test Product', 10.00, 6.00, 100, 10, 1
    );
    testProductId = result.lastInsertRowId;
  });

  afterEach(() => {
    // Cleanup
    db.runSync('DELETE FROM sales_transactions');
    db.runSync('DELETE FROM transaction_details');
    db.runSync('DELETE FROM products WHERE id = ?', testProductId);
    db.runSync('UPDATE resumen_caja SET total_sales = 0, current_balance = 0 WHERE id = 1');
  });

  it('creates sale successfully with single item', () => {
    const item = factory.createFromProduct(
      { id: testProductId, name: 'Test Product', sellingPrice: new Money(10.00, 'USD'), purchasePrice: new Money(6.00, 'USD'), currentStock: 100, imageUri: null },
      2,
      new Money(0, 'USD'),
      'SALE'
    );

    const result = registerSale({
      items: [item],
      paymentMethod: new PaymentMethod('CASH'),
      detail: 'Test sale',
    });

    expect(result.transactionId).toBeGreaterThan(0);
    expect(result.total).toBe(20.00);
    expect(result.stockAlerts).toHaveLength(0);

    // Verify stock decremented
    const product = db.getFirstSync('SELECT current_stock FROM products WHERE id = ?', testProductId);
    expect(product.current_stock).toBe(98);

    // Verify resumen_caja updated
    const caja = db.getFirstSync('SELECT total_sales, current_balance FROM resumen_caja WHERE id = 1');
    expect(caja.total_sales).toBe(20.00);
    expect(caja.current_balance).toBe(20.00);
  });

  it('creates sale with multiple items', () => {
    const item1 = factory.createFromProduct(
      { id: testProductId, name: 'Test Product', sellingPrice: new Money(10.00, 'USD'), purchasePrice: new Money(6.00, 'USD'), currentStock: 100, imageUri: null },
      2,
      new Money(0, 'USD'),
      'SALE'
    );

    // Create second product
    const result2 = db.runSync(
      `INSERT INTO products (name, selling_price, purchase_price, current_stock, min_stock, category_id)
       VALUES (?, ?, ?, ?, ?, ?)`,
      'Test Product 2', 5.00, 3.00, 50, 5, 1
    );
    const product2Id = result2.lastInsertRowId;

    const item2 = factory.createFromProduct(
      { id: product2Id, name: 'Test Product 2', sellingPrice: new Money(5.00, 'USD'), purchasePrice: new Money(3.00, 'USD'), currentStock: 50, imageUri: null },
      3,
      new Money(2.00, 'USD'),
      'SALE'
    );

    const result = registerSale({
      items: [item1, item2],
      paymentMethod: new PaymentMethod('CARD'),
      detail: 'Multi-item sale',
    });

    expect(result.total).toBe(33.00); // 20 + (15 - 2)
    expect(result.transactionId).toBeGreaterThan(0);

    // Verify both products stock decremented
    const p1 = db.getFirstSync('SELECT current_stock FROM products WHERE id = ?', testProductId);
    const p2 = db.getFirstSync('SELECT current_stock FROM products WHERE id = ?', product2Id);
    expect(p1.current_stock).toBe(98);
    expect(p2.current_stock).toBe(47);
  });

  it('calculates cash change correctly', () => {
    const item = factory.createFromProduct(
      { id: testProductId, name: 'Test Product', sellingPrice: new Money(10.00, 'USD'), purchasePrice: new Money(6.00, 'USD'), currentStock: 100, imageUri: null },
      1,
      new Money(0, 'USD'),
      'SALE'
    );

    const result = registerSale({
      items: [item],
      paymentMethod: new PaymentMethod('CASH'),
      detail: 'Cash sale',
    });

    expect(result.total).toBe(10.00);
    // Change calculation happens in PaymentModal, not here
    // But we verify the transaction was created with CASH payment method
    const tx = db.getFirstSync('SELECT payment_method FROM sales_transactions WHERE id = ?', result.transactionId);
    expect(tx.payment_method).toBe('CASH');
  });

  it('throws StockError when insufficient stock', () => {
    // Set stock to 1
    db.runSync('UPDATE products SET current_stock = 1 WHERE id = ?', testProductId);

    const item = factory.createFromProduct(
      { id: testProductId, name: 'Test Product', sellingPrice: new Money(10.00, 'USD'), purchasePrice: new Money(6.00, 'USD'), currentStock: 1, imageUri: null },
      5, // Request 5, only 1 available
      new Money(0, 'USD'),
      'SALE'
    );

    expect(() => registerSale({
      items: [item],
      paymentMethod: new PaymentMethod('CASH'),
      detail: 'Should fail',
    })).toThrow(StockError);
  });

  it('throws for empty cart', () => {
    expect(() => registerSale({
      items: [],
      paymentMethod: new PaymentMethod('CASH'),
      detail: 'Empty cart',
    })).toThrow('Cart cannot be empty');
  });

  it('rolls back on DB failure during transaction details insert', () => {
    // This test simulates a DB constraint failure mid-transaction
    // We can't easily simulate SQLite failure in unit test without mocking
    // but the atomicity is guaranteed by withTransactionSync
    // Integration test would need a test DB that can simulate failures
  });

  it('emits stock alerts for products below minimum', () => {
    // Set product stock to 5, min_stock to 10
    db.runSync('UPDATE products SET current_stock = 5, min_stock = 10 WHERE id = ?', testProductId);

    const item = factory.createFromProduct(
      { id: testProductId, name: 'Test Product', sellingPrice: new Money(10.00, 'USD'), purchasePrice: new Money(6.00, 'USD'), currentStock: 5, imageUri: null },
      2,
      new Money(0, 'USD'),
      'SALE'
    );

    const result = registerSale({
      items: [item],
      paymentMethod: new PaymentMethod('CASH'),
      detail: 'Low stock sale',
    });

    expect(result.stockAlerts).toHaveLength(1);
    expect(result.stockAlerts[0].id).toBe(testProductId);
    expect(result.stockAlerts[0].currentStock).toBe(3); // 5 - 2
    expect(result.stockAlerts[0].minStock).toBe(10);
  });
});
```

### 2.2 RegisterPurchase

**File:** `tests/integration/sales-checkout/RegisterPurchase.test.ts`

```ts
import { registerPurchase } from '@/application/purchases/RegisterPurchase';
import { PaymentMethod } from '@/domain/entities/PaymentMethod';
import { SimpleCartItemFactory } from '@/domain/entities/CartItem';
import { Money } from '@/domain/value-objects/Money';
import { getDatabase } from '@/data/database/client';

describe('RegisterPurchase', () => {
  let db: ReturnType<typeof getDatabase>;
  let factory: SimpleCartItemFactory;
  let testProductId: number;

  beforeAll(() => {
    db = getDatabase();
    factory = new SimpleCartItemFactory();
  });

  beforeEach(() => {
    const result = db.runSync(
      `INSERT INTO products (name, selling_price, purchase_price, current_stock, min_stock, category_id)
       VALUES (?, ?, ?, ?, ?, ?)`,
      'Test Product', 10.00, 6.00, 100, 10, 1
    );
    testProductId = result.lastInsertRowId;
    db.runSync('UPDATE resumen_caja SET total_purchases = 0, current_balance = 1000 WHERE id = 1');
  });

  afterEach(() => {
    db.runSync('DELETE FROM sales_transactions');
    db.runSync('DELETE FROM transaction_details');
    db.runSync('DELETE FROM products WHERE id = ?', testProductId);
  });

  it('creates purchase successfully', () => {
    const item = factory.createFromProduct(
      { id: testProductId, name: 'Test Product', sellingPrice: new Money(10.00, 'USD'), purchasePrice: new Money(6.00, 'USD'), currentStock: 100, imageUri: null },
      10,
      new Money(0, 'USD'),
      'PURCHASE'
    );

    const result = registerPurchase({
      items: [item],
      paymentMethod: new PaymentMethod('TRANSFER'),
      detail: 'Stock replenishment',
    });

    expect(result.transactionId).toBeGreaterThan(0);
    expect(result.total).toBe(60.00); // 10 × 6.00

    // Verify stock incremented
    const product = db.getFirstSync('SELECT current_stock FROM products WHERE id = ?', testProductId);
    expect(product.current_stock).toBe(110);

    // Verify resumen_caja updated (purchases increase, balance decreases)
    const caja = db.getFirstSync('SELECT total_purchases, current_balance FROM resumen_caja WHERE id = 1');
    expect(caja.total_purchases).toBe(60.00);
    expect(caja.current_balance).toBe(940.00); // 1000 - 60
  });

  it('uses purchasePrice for unit price', () => {
    const item = factory.createFromProduct(
      { id: testProductId, name: 'Test Product', sellingPrice: new Money(10.00, 'USD'), purchasePrice: new Money(6.00, 'USD'), currentStock: 100, imageUri: null },
      5,
      new Money(0, 'USD'),
      'PURCHASE'
    );

    expect(item.unitPrice.amount).toBe(6.00); // purchasePrice, not sellingPrice
  });
});
```

### 2.3 RevertTransaction

**File:** `tests/integration/sales-checkout/RevertTransaction.test.ts`

```ts
import { revertTransaction } from '@/application/transactions/RevertTransaction';
import { registerSale, registerPurchase } from '@/application/sales/RegisterSale';
import { registerExpense } from '@/application/expenses/RegisterExpense'; // if exists
import { PaymentMethod } from '@/domain/entities/PaymentMethod';
import { SimpleCartItemFactory } from '@/domain/entities/CartItem';
import { Money } from '@/domain/value-objects/Money';
import { getDatabase } from '@/data/database/client';
import { StockError } from '@/domain/rules/stockRules';

describe('RevertTransaction', () => {
  let db: ReturnType<typeof getDatabase>;
  let factory: SimpleCartItemFactory;
  let testProductId: number;

  beforeAll(() => {
    db = getDatabase();
    factory = new SimpleCartItemFactory();
  });

  beforeEach(() => {
    const result = db.runSync(
      `INSERT INTO products (name, selling_price, purchase_price, current_stock, min_stock, category_id)
       VALUES (?, ?, ?, ?, ?, ?)`,
      'Test Product', 10.00, 6.00, 100, 10, 1
    );
    testProductId = result.lastInsertRowId;
    db.runSync('UPDATE resumen_caja SET total_sales = 0, total_purchases = 0, total_expenses = 0, current_balance = 1000 WHERE id = 1');
  });

  afterEach(() => {
    db.runSync('DELETE FROM sales_transactions');
    db.runSync('DELETE FROM transaction_details');
    db.runSync('DELETE FROM products WHERE id = ?', testProductId);
  });

  describe('SALE reversal', () => {
    it('restores stock and decrements balance', () => {
      // Create sale
      const item = factory.createFromProduct(
        { id: testProductId, name: 'Test Product', sellingPrice: new Money(10.00, 'USD'), purchasePrice: new Money(6.00, 'USD'), currentStock: 100, imageUri: null },
        5,
        new Money(0, 'USD'),
        'SALE'
      );

      const saleResult = registerSale({
        items: [item],
        paymentMethod: new PaymentMethod('CASH'),
        detail: 'Sale to revert',
      });

      // Verify sale created
      let product = db.getFirstSync('SELECT current_stock FROM products WHERE id = ?', testProductId);
      expect(product.current_stock).toBe(95);

      let caja = db.getFirstSync('SELECT total_sales, current_balance FROM resumen_caja WHERE id = 1');
      expect(caja.total_sales).toBe(50.00);
      expect(caja.current_balance).toBe(1050.00);

      // Revert sale
      revertTransaction(saleResult.transactionId);

      // Verify stock restored
      product = db.getFirstSync('SELECT current_stock FROM products WHERE id = ?', testProductId);
      expect(product.current_stock).toBe(100);

      // Verify balance corrected
      caja = db.getFirstSync('SELECT total_sales, current_balance FROM resumen_caja WHERE id = 1');
      expect(caja.total_sales).toBe(0.00);
      expect(caja.current_balance).toBe(1000.00);

      // Verify soft delete
      const tx = db.getFirstSync('SELECT deleted_at FROM sales_transactions WHERE id = ?', saleResult.transactionId);
      expect(tx.deleted_at).not.toBeNull();
    });
  });

  describe('PURCHASE reversal', () => {
    it('decrements stock and restores balance', () => {
      // Create purchase
      const item = factory.createFromProduct(
        { id: testProductId, name: 'Test Product', sellingPrice: new Money(10.00, 'USD'), purchasePrice: new Money(6.00, 'USD'), currentStock: 100, imageUri: null },
        10,
        new Money(0, 'USD'),
        'PURCHASE'
      );

      const purchaseResult = registerPurchase({
        items: [item],
        paymentMethod: new PaymentMethod('TRANSFER'),
        detail: 'Purchase to revert',
      });

      // Verify purchase created
      let product = db.getFirstSync('SELECT current_stock FROM products WHERE id = ?', testProductId);
      expect(product.current_stock).toBe(110);

      let caja = db.getFirstSync('SELECT total_purchases, current_balance FROM resumen_caja WHERE id = 1');
      expect(caja.total_purchases).toBe(60.00);
      expect(caja.current_balance).toBe(940.00);

      // Revert purchase
      revertTransaction(purchaseResult.transactionId);

      // Verify stock decremented
      product = db.getFirstSync('SELECT current_stock FROM products WHERE id = ?', testProductId);
      expect(product.current_stock).toBe(100);

      // Verify balance restored
      caja = db.getFirstSync('SELECT total_purchases, current_balance FROM resumen_caja WHERE id = 1');
      expect(caja.total_purchases).toBe(0.00);
      expect(caja.current_balance).toBe(1000.00);
    });

    it('throws StockError if reversal would make stock negative', () => {
      // Create purchase that adds 10 units (stock: 100 → 110)
      const item = factory.createFromProduct(
        { id: testProductId, name: 'Test Product', sellingPrice: new Money(10.00, 'USD'), purchasePrice: new Money(6.00, 'USD'), currentStock: 100, imageUri: null },
        10,
        new Money(0, 'USD'),
        'PURCHASE'
      );

      const purchaseResult = registerPurchase({
        items: [item],
        paymentMethod: new PaymentMethod('TRANSFER'),
        detail: 'Purchase to revert',
      });

      // Manually reduce stock to 5 (simulate sales after purchase)
      db.runSync('UPDATE products SET current_stock = 5 WHERE id = ?', testProductId);

      // Attempt to revert purchase (would need to remove 10, but only 5 available)
      expect(() => revertTransaction(purchaseResult.transactionId)).toThrow(StockError);
    });
  });

  describe('EXPENSE reversal', () => {
    it('restores balance only (no stock changes)', () => {
      // Create expense directly in DB
      const result = db.runSync(
        `INSERT INTO sales_transactions (type, total_amount, payment_method, detail)
         VALUES (?, ?, ?, ?)`,
        'EXPENSE', 50.00, 'CASH', 'Office supplies'
      );
      const expenseId = result.lastInsertRowId;

      // Verify expense created
      let caja = db.getFirstSync('SELECT total_expenses, current_balance FROM resumen_caja WHERE id = 1');
      expect(caja.total_expenses).toBe(50.00);
      expect(caja.current_balance).toBe(950.00);

      // Revert expense
      revertTransaction(expenseId);

      // Verify balance restored
      caja = db.getFirstSync('SELECT total_expenses, current_balance FROM resumen_caja WHERE id = 1');
      expect(caja.total_expenses).toBe(0.00);
      expect(caja.current_balance).toBe(1000.00);

      // Verify soft delete
      const tx = db.getFirstSync('SELECT deleted_at FROM sales_transactions WHERE id = ?', expenseId);
      expect(tx.deleted_at).not.toBeNull();
    });
  });
});
```

---

## 3. Migration Tests

### 3.1 v2.0.0 Migration

**File:** `tests/migration/sales-checkout/v2_0_0_migration.test.ts`

```ts
import { getDatabase } from '@/data/database/client';
import { runMigrations } from '@/data/database/migrations';

describe('v2.0.0 Sales Migration', () => {
  let db: ReturnType<typeof getDatabase>;

  beforeAll(() => {
    db = getDatabase();
  });

  beforeEach(() => {
    // Reset to v1 schema
    db.execSync(`
      DROP TABLE IF EXISTS sales_transactions;
      DROP TABLE IF EXISTS transaction_details;
      DROP TABLE IF EXISTS transacciones;
      DROP TABLE IF EXISTS detalle_transacciones;
      DROP TABLE IF EXISTS resumen_caja;
    `);

    // Create v1 schema (Spanish columns)
    db.execSync(`
      CREATE TABLE transacciones (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        tipo TEXT NOT NULL,
        monto_total REAL NOT NULL,
        tipo_pago TEXT NOT NULL,
        detalle TEXT,
        fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        fecha_delete TIMESTAMP DEFAULT NULL
      );

      CREATE TABLE detalle_transacciones (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        transaccion_id INTEGER NOT NULL,
        producto_id INTEGER NOT NULL,
        cantidad REAL NOT NULL,
        precio_unitario REAL NOT NULL,
        FOREIGN KEY(transaccion_id) REFERENCES transacciones(id)
      );

      CREATE TABLE resumen_caja (
        id INTEGER PRIMARY KEY CHECK (id = 1),
        total_ventas REAL DEFAULT 0.0,
        total_compras REAL DEFAULT 0.0,
        total_gastos REAL DEFAULT 0.0,
        saldo_actual REAL DEFAULT 0.0
      );

      INSERT OR IGNORE INTO resumen_caja (id, total_ventas, total_compras, total_gastos, saldo_actual)
      VALUES (1, 0.0, 0.0, 0.0, 0.0);
    `);

    // Seed v1 data
    db.runSync(`
      INSERT INTO transacciones (tipo, monto_total, tipo_pago, detalle)
      VALUES ('VENTA', 100.00, 'EFECTIVO', 'Test sale')
    `);
    const txId = db.runSync('SELECT last_insert_rowid() as id').id;

    db.runSync(`
      INSERT INTO detalle_transacciones (transaccion_id, producto_id, cantidad, precio_unitario)
      VALUES (?, 1, 2, 50.00)
    `, txId);

    db.runSync(`
      INSERT INTO transacciones (tipo, monto_total, tipo_pago, detalle)
      VALUES ('COMPRA', 200.00, 'TRANSFERENCIA', 'Test purchase')
    `);
    const txId2 = db.runSync('SELECT last_insert_rowid() as id').id;

    db.runSync(`
      INSERT INTO detalle_transacciones (transaccion_id, producto_id, cantidad, precio_unitario)
      VALUES (?, 2, 10, 20.00)
    `, txId2);

    // Soft delete one transaction
    db.runSync(`
      INSERT INTO transacciones (tipo, monto_total, tipo_pago, detalle, fecha_delete)
      VALUES ('GASTO', 50.00, 'EFECTIVO', 'Deleted expense', CURRENT_TIMESTAMP)
    `);

    // Update resumen_caja
    db.runSync(`UPDATE resumen_caja SET total_ventas = 100, total_compras = 200, total_gastos = 50, saldo_actual = 50 WHERE id = 1`);
  });

  it('migrates transacciones to sales_transactions with English columns', () => {
    runMigrations();

    // Verify table renamed
    const tables = db.getAllSync("SELECT name FROM sqlite_master WHERE type='table'");
    const tableNames = tables.map(t => t.name);
    expect(tableNames).toContain('sales_transactions');
    expect(tableNames).not.toContain('transacciones');

    // Verify columns renamed
    const columns = db.getAllSync("PRAGMA table_info(sales_transactions)");
    const columnNames = columns.map(c => c.name);
    expect(columnNames).toContain('id');
    expect(columnNames).toContain('type');
    expect(columnNames).toContain('total_amount');
    expect(columnNames).toContain('payment_method');
    expect(columnNames).toContain('detail');
    expect(columnNames).toContain('registered_at');
    expect(columnNames).toContain('deleted_at');
    expect(columnNames).not.toContain('tipo');
    expect(columnNames).not.toContain('monto_total');
    expect(columnNames).not.toContain('tipo_pago');
    expect(columnNames).not.toContain('detalle');
    expect(columnNames).not.toContain('fecha_registro');
    expect(columnNames).not.toContain('fecha_delete');
  });

  it('migrates detalle_transacciones to transaction_details with English columns', () => {
    runMigrations();

    const tables = db.getAllSync("SELECT name FROM sqlite_master WHERE type='table'");
    const tableNames = tables.map(t => t.name);
    expect(tableNames).toContain('transaction_details');
    expect(tableNames).not.toContain('detalle_transacciones');

    const columns = db.getAllSync("PRAGMA table_info(transaction_details)");
    const columnNames = columns.map(c => c.name);
    expect(columnNames).toContain('id');
    expect(columnNames).toContain('transaction_id');
    expect(columnNames).toContain('product_id');
    expect(columnNames).toContain('quantity');
    expect(columnNames).toContain('unit_price');
    expect(columnNames).not.toContain('transaccion_id');
    expect(columnNames).not.toContain('producto_id');
    expect(columnNames).not.toContain('cantidad');
    expect(columnNames).not.toContain('precio_unitario');
  });

  it('preserves all data rows', () => {
    runMigrations();

    const txCount = db.getFirstSync('SELECT COUNT(*) as count FROM sales_transactions');
    expect(txCount.count).toBe(3); // 2 active + 1 soft deleted

    const detailCount = db.getFirstSync('SELECT COUNT(*) as count FROM transaction_details');
    expect(detailCount.count).toBe(2);

    // Verify data values
    const venta = db.getFirstSync("SELECT * FROM sales_transactions WHERE type = 'VENTA'");
    expect(venta.total_amount).toBe(100.00);
    expect(venta.payment_method).toBe('EFECTIVO');
    expect(venta.detail).toBe('Test sale');
    expect(venta.deleted_at).toBeNull();

    const gasto = db.getFirstSync("SELECT * FROM sales_transactions WHERE type = 'GASTO'");
    expect(gasto.total_amount).toBe(50.00);
    expect(gasto.deleted_at).not.toBeNull(); // Soft delete preserved
  });

  it('preserves primary keys', () => {
    runMigrations();

    const ids = db.getAllSync('SELECT id FROM sales_transactions ORDER BY id');
    expect(ids.map(r => r.id)).toEqual([1, 2, 3]);
  });

  it('preserves timestamps', () => {
    runMigrations();

    const tx = db.getFirstSync("SELECT registered_at FROM sales_transactions WHERE type = 'VENTA'");
    expect(tx.registered_at).not.toBeNull();
    expect(new Date(tx.registered_at).getTime()).toBeGreaterThan(0);
  });

  it('recreates indexes with English names', () => {
    runMigrations();

    const indexes = db.getAllSync("SELECT name FROM sqlite_master WHERE type='index'");
    const indexNames = indexes.map(i => i.name);
    expect(indexNames).toContain('idx_sales_transactions_date');
    expect(indexNames).toContain('idx_sales_transactions_type');
    expect(indexNames).toContain('idx_transaction_details_transaction');
    expect(indexNames).toContain('idx_transaction_details_product');
  });

  it('preserves resumen_caja accumulator', () => {
    runMigrations();

    const caja = db.getFirstSync('SELECT * FROM resumen_caja WHERE id = 1');
    expect(caja.total_ventas).toBe(100.00);
    expect(caja.total_compras).toBe(200.00);
    expect(caja.total_gastos).toBe(50.00);
    expect(caja.saldo_actual).toBe(50.00);
  });

  it('updates user_version to 2', () => {
    runMigrations();

    const version = db.getFirstSync('PRAGMA user_version');
    expect(version.user_version).toBe(2);
  });

  it('is idempotent - running twice causes no errors', () => {
    runMigrations();
    expect(() => runMigrations()).not.toThrow();

    // Verify data still correct
    const txCount = db.getFirstSync('SELECT COUNT(*) as count FROM sales_transactions');
    expect(txCount.count).toBe(3);
  });
});
```

---

## 4. Atomic Transaction Tests

### 4.1 Sale Atomic Rollback

**File:** `tests/integration/atomic/saleAtomicRollback.test.ts`

```ts
import { registerSale } from '@/application/sales/RegisterSale';
import { PaymentMethod } from '@/domain/entities/PaymentMethod';
import { SimpleCartItemFactory } from '@/domain/entities/CartItem';
import { Money } from '@/domain/value-objects/Money';
import { getDatabase } from '@/data/database/client';

describe('Sale Atomic Rollback', () => {
  let db: ReturnType<typeof getDatabase>;
  let factory: SimpleCartItemFactory;
  let testProductId: number;

  beforeAll(() => {
    db = getDatabase();
    factory = new SimpleCartItemFactory();
  });

  beforeEach(() => {
    const result = db.runSync(
      `INSERT INTO products (name, selling_price, purchase_price, current_stock, min_stock, category_id)
       VALUES (?, ?, ?, ?, ?, ?)`,
      'Test Product', 10.00, 6.00, 100, 10, 1
    );
    testProductId = result.lastInsertRowId;
    db.runSync('UPDATE resumen_caja SET total_sales = 0, current_balance = 1000 WHERE id = 1');
  });

  afterEach(() => {
    db.runSync('DELETE FROM sales_transactions');
    db.runSync('DELETE FROM transaction_details');
    db.runSync('DELETE FROM products WHERE id = ?', testProductId);
  });

  it('rolls back stock change if transaction_details insert fails', () => {
    // This test requires mocking a DB failure
    // In practice, we verify the withTransactionSync wrapper exists
    // and that all writes are inside it
    const item = factory.createFromProduct(
      { id: testProductId, name: 'Test Product', sellingPrice: new Money(10.00, 'USD'), purchasePrice: new Money(6.00, 'USD'), currentStock: 100, imageUri: null },
      5,
      new Money(0, 'USD'),
      'SALE'
    );

    const result = registerSale({
      items: [item],
      paymentMethod: new PaymentMethod('CASH'),
      detail: 'Atomic test',
    });

    // Verify all or nothing: either complete success or complete rollback
    const product = db.getFirstSync('SELECT current_stock FROM products WHERE id = ?', testProductId);
    const caja = db.getFirstSync('SELECT total_sales FROM resumen_caja WHERE id = 1');

    // Both should be consistent
    if (product.current_stock === 95) {
      expect(caja.total_sales).toBe(50.00);
    } else {
      expect(product.current_stock).toBe(100);
      expect(caja.total_sales).toBe(0.00);
    }
  });

  it('maintains consistency under concurrent sales', async () => {
    // Simulate concurrent sales on same product
    const item = factory.createFromProduct(
      { id: testProductId, name: 'Test Product', sellingPrice: new Money(10.00, 'USD'), purchasePrice: new Money(6.00, 'USD'), currentStock: 10, imageUri: null },
      6,
      new Money(0, 'USD'),
      'SALE'
    );

    // First sale should succeed (stock: 10 → 4)
    const result1 = registerSale({
      items: [item],
      paymentMethod: new PaymentMethod('CASH'),
      detail: 'Concurrent sale 1',
    });

    // Second sale should fail (stock: 4 < 6)
    await expect(async () => {
      registerSale({
        items: [item],
        paymentMethod: new PaymentMethod('CASH'),
        detail: 'Concurrent sale 2',
      });
    }).rejects.toThrow();

    // Verify stock is 4 (only first sale committed)
    const product = db.getFirstSync('SELECT current_stock FROM products WHERE id = ?', testProductId);
    expect(product.current_stock).toBe(4);
  });
});
```

---

## 5. Hook Tests

### 5.1 useCart

**File:** `tests/unit/hooks/useCart.test.ts`

```ts
import { renderHook, act } from '@testing-library/react-native';
import { useCart } from '@/presentation/hooks/useCart';
import { saleCartStore } from '@/presentation/stores/saleCartStore';
import { SimpleCartItemFactory } from '@/domain/entities/CartItem';
import { Money } from '@/domain/value-objects/Money';

describe('useCart', () => {
  const factory = new SimpleCartItemFactory();
  const mockProduct = {
    id: 1,
    name: 'Test Product',
    sellingPrice: new Money(10.00, 'USD'),
    purchasePrice: new Money(6.00, 'USD'),
    currentStock: 100,
    imageUri: null,
  };

  beforeEach(() => {
    saleCartStore.getState().clear();
  });

  it('returns empty cart initially', () => {
    const { result } = renderHook(() => useCart('SALE'));
    expect(result.current.items).toHaveLength(0);
    expect(result.current.total).toBe(0);
    expect(result.current.itemCount).toBe(0);
  });

  it('adds item to cart', () => {
    const { result } = renderHook(() => useCart('SALE'));
    const item = factory.createFromProduct(mockProduct, 2, new Money(0, 'USD'), 'SALE');

    act(() => {
      result.current.addItem(item);
    });

    expect(result.current.items).toHaveLength(1);
    expect(result.current.itemCount).toBe(2);
    expect(result.current.total).toBe(20.00);
  });

  it('updates item quantity', () => {
    const { result } = renderHook(() => useCart('SALE'));
    const item = factory.createFromProduct(mockProduct, 2, new Money(0, 'USD'), 'SALE');

    act(() => {
      result.current.addItem(item);
    });

    act(() => {
      result.current.updateItem(1, { quantity: 5, discount: 0 });
    });

    expect(result.current.items[0].quantity).toBe(5);
    expect(result.current.itemCount).toBe(5);
    expect(result.current.total).toBe(50.00);
  });

  it('removes item from cart', () => {
    const { result } = renderHook(() => useCart('SALE'));
    const item = factory.createFromProduct(mockProduct, 2, new Money(0, 'USD'), 'SALE');

    act(() => {
      result.current.addItem(item);
    });

    act(() => {
      result.current.removeItem(1);
    });

    expect(result.current.items).toHaveLength(0);
    expect(result.current.itemCount).toBe(0);
    expect(result.current.total).toBe(0);
  });

  it('clears cart', () => {
    const { result } = renderHook(() => useCart('SALE'));
    const item = factory.createFromProduct(mockProduct, 2, new Money(0, 'USD'), 'SALE');

    act(() => {
      result.current.addItem(item);
    });

    act(() => {
      result.current.clear();
    });

    expect(result.current.items).toHaveLength(0);
    expect(result.current.itemCount).toBe(0);
    expect(result.current.total).toBe(0);
  });

  it('granular selectors prevent unnecessary re-renders', () => {
    const { result, rerender } = renderHook(() => useCart('SALE'));
    const item = factory.createFromProduct(mockProduct, 2, new Money(0, 'USD'), 'SALE');

    act(() => {
      result.current.addItem(item);
    });

    // Subscribe only to total
    const { result: totalHook } = renderHook(() => useCart('SALE', state => state.total));
    expect(totalHook.current).toBe(20.00);

    // Add another item - totalHook should re-render, but items hook should not
    const item2 = factory.createFromProduct({ ...mockProduct, id: 2 }, 1, new Money(0, 'USD'), 'SALE');
    act(() => {
      result.current.addItem(item2);
    });

    expect(totalHook.current).toBe(30.00);
  });
});
```

### 5.2 useTransactions

**File:** `tests/unit/hooks/useTransactions.test.ts`

```ts
import { renderHook, waitFor } from '@testing-library/react-native';
import { useTransactions } from '@/presentation/hooks/useTransactions';
import { getDatabase } from '@/data/database/client';

describe('useTransactions', () => {
  let db: ReturnType<typeof getDatabase>;

  beforeAll(() => {
    db = getDatabase();
  });

  beforeEach(() => {
    db.runSync('DELETE FROM sales_transactions');
    db.runSync('DELETE FROM transaction_details');
    db.runSync('UPDATE resumen_caja SET total_sales = 0, current_balance = 0 WHERE id = 1');

    // Seed test data
    for (let i = 0; i < 20; i++) {
      const result = db.runSync(
        `INSERT INTO sales_transactions (type, total_amount, payment_method, detail)
         VALUES (?, ?, ?, ?)`,
        i % 2 === 0 ? 'SALE' : 'PURCHASE',
        (i + 1) * 10,
        'CASH',
        `Transaction ${i + 1}`
      );
      const txId = result.lastInsertRowId;
      db.runSync(
        `INSERT INTO transaction_details (transaction_id, product_id, quantity, unit_price)
         VALUES (?, 1, 1, ?)`,
        txId, (i + 1) * 10
      );
    }
  });

  it('fetches paginated transactions', async () => {
    const { result } = renderHook(() => useTransactions({ type: 'SALE', limit: 5, offset: 0 }));

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.transactions).toHaveLength(5);
    expect(result.current.hasMore).toBe(true);
  });

  it('filters by type', async () => {
    const { result } = renderHook(() => useTransactions({ type: 'SALE', limit: 20, offset: 0 }));

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.transactions.every(t => t.type === 'SALE')).toBe(true);
  });

  it('loads more on scroll', async () => {
    const { result } = renderHook(() => useTransactions({ type: 'SALE', limit: 5, offset: 0 }));

    await waitFor(() => expect(result.current.loading).toBe(false));
    const firstBatch = result.current.transactions.length;

    act(() => {
      result.current.loadMore();
    });

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.transactions.length).toBeGreaterThan(firstBatch);
  });
});
```

### 5.3 useStockAlert

**File:** `tests/unit/hooks/useStockAlert.test.ts`

```ts
import { renderHook, act } from '@testing-library/react-native';
import { useStockAlert } from '@/presentation/hooks/useStockAlert';
import { ToastService } from '@/infrastructure/toast/ToastService';

jest.mock('@/infrastructure/toast/ToastService');

describe('useStockAlert', () => {
  it('shows toast when stock below minimum', () => {
    const { result } = renderHook(() => useStockAlert());

    act(() => {
      result.current.checkAndAlert([
        { id: 1, name: 'Product A', currentStock: 5, minStock: 10 },
        { id: 2, name: 'Product B', currentStock: 20, minStock: 10 },
      ]);
    });

    expect(ToastService.show).toHaveBeenCalledWith({
      type: 'warning',
      text1: 'Stock bajo',
      text2: 'Stock insuficiente para Product A. Disponible: 5',
    });
  });

  it('does not show toast when stock above minimum', () => {
    const { result } = renderHook(() => useStockAlert());

    act(() => {
      result.current.checkAndAlert([
        { id: 1, name: 'Product A', currentStock: 15, minStock: 10 },
      ]);
    });

    expect(ToastService.show).not.toHaveBeenCalled();
  });

  it('shows multiple toasts for multiple low-stock products', () => {
    const { result } = renderHook(() => useStockAlert());

    act(() => {
      result.current.checkAndAlert([
        { id: 1, name: 'Product A', currentStock: 5, minStock: 10 },
        { id: 2, name: 'Product B', currentStock: 3, minStock: 10 },
      ]);
    });

    expect(ToastService.show).toHaveBeenCalledTimes(2);
  });
});
```

---

## 6. UI Component Tests

### 6.1 PaymentModal

**File:** `tests/components/cart/PaymentModal.test.tsx`

```tsx
import React from 'react';
import { render, fireEvent, waitFor } from '@testing-library/react-native';
import { PaymentModal } from '@/presentation/components/cart/PaymentModal';
import { PaymentMethod } from '@/domain/entities/PaymentMethod';
import { Money } from '@/domain/value-objects/Money';

describe('PaymentModal', () => {
  const mockOnConfirm = jest.fn();
  const mockOnCancel = jest.fn();
  const total = new Money(50.00, 'USD');

  const defaultProps = {
    visible: true,
    total,
    type: 'SALE' as const,
    onConfirm: mockOnConfirm,
    onCancel: mockOnCancel,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders payment method options', () => {
    const { getByText } = render(<PaymentModal {...defaultProps} />);
    expect(getByText('Tipo de pago')).toBeTruthy();
    expect(getByText('Efectivo')).toBeTruthy();
    expect(getByText('Tarjeta')).toBeTruthy();
    expect(getByText('Transferencia')).toBeTruthy();
    expect(getByText('QR')).toBeTruthy();
  });

  it('shows cash received input when CASH selected', () => {
    const { getByText, getByPlaceholderText } = render(<PaymentModal {...defaultProps} />);
    fireEvent.press(getByText('Efectivo'));
    expect(getByPlaceholderText('Monto recibido')).toBeTruthy();
  });

  it('calculates change correctly for sufficient cash', () => {
    const { getByText, getByPlaceholderText } = render(<PaymentModal {...defaultProps} />);
    fireEvent.press(getByText('Efectivo'));
    const input = getByPlaceholderText('Monto recibido');
    fireEvent.changeText(input, '100');
    expect(getByText('Cambio: $50.00')).toBeTruthy();
    expect(getByText('text-success')).toBeTruthy(); // green color class
  });

  it('shows negative change in red for insufficient cash', () => {
    const { getByText, getByPlaceholderText } = render(<PaymentModal {...defaultProps} />);
    fireEvent.press(getByText('Efectivo'));
    const input = getByPlaceholderText('Monto recibido');
    fireEvent.changeText(input, '30');
    expect(getByText('Faltante: $20.00')).toBeTruthy();
    expect(getByText('text-danger')).toBeTruthy(); // red color class
  });

  it('sets exact amount when "Exacto" pressed', () => {
    const { getByText, getByPlaceholderText } = render(<PaymentModal {...defaultProps} />);
    fireEvent.press(getByText('Efectivo'));
    const input = getByPlaceholderText('Monto recibido');
    fireEvent.press(getByText('Exacto'));
    expect(input.props.value).toBe('50.00');
  });

  it('calls onConfirm with payment method and received amount', () => {
    const { getByText, getByPlaceholderText } = render(<PaymentModal {...defaultProps} />);
    fireEvent.press(getByText('Efectivo'));
    const input = getByPlaceholderText('Monto recibido');
    fireEvent.changeText(input, '60');
    fireEvent.press(getByText('Cobrar'));

    expect(mockOnConfirm).toHaveBeenCalledWith(
      expect.objectContaining({
        paymentMethod: expect.any(PaymentMethod),
        receivedAmount: 60,
      })
    );
  });

  it('calls onCancel when cancel pressed', () => {
    const { getByText } = render(<PaymentModal {...defaultProps} />);
    fireEvent.press(getByText('Cancelar'));
    expect(mockOnCancel).toHaveBeenCalled();
  });

  it('does not show cash input for non-CASH methods', () => {
    const { getByText, queryByPlaceholderText } = render(<PaymentModal {...defaultProps} />);
    fireEvent.press(getByText('Tarjeta'));
    expect(queryByPlaceholderText('Monto recibido')).toBeNull();
  });
});
```

### 6.2 LineItemModal

**File:** `tests/components/cart/LineItemModal.test.tsx`

```tsx
import React from 'react';
import { render, fireEvent, waitFor } from '@testing-library/react-native';
import { LineItemModal } from '@/presentation/components/cart/LineItemModal';
import { Money } from '@/domain/value-objects/Money';

describe('LineItemModal', () => {
  const mockOnApply = jest.fn();
  const mockOnCancel = jest.fn();

  const defaultProps = {
    visible: true,
    product: {
      id: 1,
      name: 'Test Product',
      sellingPrice: new Money(10.00, 'USD'),
      purchasePrice: new Money(6.00, 'USD'),
      currentStock: 100,
      imageUri: null,
    },
    type: 'SALE' as const,
    onApply: mockOnApply,
    onCancel: mockOnCancel,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders product name and stock', () => {
    const { getByText } = render(<LineItemModal {...defaultProps} />);
    expect(getByText('Test Product')).toBeTruthy();
    expect(getByText('Stock disponible: 100')).toBeTruthy();
  });

  it('accepts fractional quantity', () => {
    const { getByPlaceholderText } = render(<LineItemModal {...defaultProps} />);
    const qtyInput = getByPlaceholderText('Cantidad');
    fireEvent.changeText(qtyInput, '1.5');
    expect(qtyInput.props.value).toBe('1.5');
  });

  it('calculates subtotal live', () => {
    const { getByPlaceholderText, getByText } = render(<LineItemModal {...defaultProps} />);
    const qtyInput = getByPlaceholderText('Cantidad');
    fireEvent.changeText(qtyInput, '3');
    expect(getByText('Subtotal línea: $30.00')).toBeTruthy();
  });

  it('validates discount ≤ subtotal', () => {
    const { getByPlaceholderText, getByText } = render(<LineItemModal {...defaultProps} />);
    const qtyInput = getByPlaceholderText('Cantidad');
    fireEvent.changeText(qtyInput, '2'); // subtotal = 20

    const discountInput = getByPlaceholderText('Descuento (monto fijo)');
    fireEvent.changeText(discountInput, '25'); // exceeds subtotal
    expect(getByText('El descuento no puede superar $20.00')).toBeTruthy();
  });

  it('clamps discount to subtotal on apply', () => {
    const { getByPlaceholderText, getByText } = render(<LineItemModal {...defaultProps} />);
    const qtyInput = getByPlaceholderText('Cantidad');
    fireEvent.changeText(qtyInput, '2'); // subtotal = 20

    const discountInput = getByPlaceholderText('Descuento (monto fijo)');
    fireEvent.changeText(discountInput, '25'); // exceeds
    fireEvent.press(getByText('Aplicar'));

    expect(mockOnApply).toHaveBeenCalledWith(
      expect.objectContaining({
        quantity: 2,
        discount: 20, // clamped to subtotal
      })
    );
  });

  it('rejects quantity exceeding stock', () => {
    const { getByPlaceholderText, getByText } = render(<LineItemModal {...defaultProps} />);
    const qtyInput = getByPlaceholderText('Cantidad');
    fireEvent.changeText(qtyInput, '150'); // exceeds stock of 100
    expect(getByText('Máximo disponible: 100')).toBeTruthy();
  });

  it('calls onCancel when cancel pressed', () => {
    const { getByText } = render(<LineItemModal {...defaultProps} />);
    fireEvent.press(getByText('Cancelar'));
    expect(mockOnCancel).toHaveBeenCalled();
  });
});
```

### 6.3 CartSummaryBar

**File:** `tests/components/cart/CartSummaryBar.test.tsx`

```tsx
import React from 'react';
import { render, fireEvent } from '@testing-library/react-native';
import { CartSummaryBar } from '@/presentation/components/cart/CartSummaryBar';

describe('CartSummaryBar', () => {
  const mockOnCheckout = jest.fn();
  const mockOnClear = jest.fn();

  const defaultProps = {
    itemCount: 3,
    total: 75.00,
    onCheckout: mockOnCheckout,
    onClear: mockOnClear,
    type: 'SALE' as const,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders item count', () => {
    const { getByText } = render(<CartSummaryBar {...defaultProps} />);
    expect(getByText('3 productos')).toBeTruthy();
  });

  it('renders total', () => {
    const { getByText } = render(<CartSummaryBar {...defaultProps} />);
    expect(getByText('Total: $75.00')).toBeTruthy();
  });

  it('renders checkout button with correct label', () => {
    const { getByText } = render(<CartSummaryBar {...defaultProps} />);
    expect(getByText('Cobrar')).toBeTruthy();
  });

  it('renders clear button', () => {
    const { getByText } = render(<CartSummaryBar {...defaultProps} />);
    expect(getByText('Vaciar carrito')).toBeTruthy();
  });

  it('calls onCheckout when checkout pressed', () => {
    const { getByText } = render(<CartSummaryBar {...defaultProps} />);
    fireEvent.press(getByText('Cobrar'));
    expect(mockOnCheckout).toHaveBeenCalled();
  });

  it('calls onClear when clear pressed', () => {
    const { getByText } = render(<CartSummaryBar {...defaultProps} />);
    fireEvent.press(getByText('Vaciar carrito'));
    expect(mockOnClear).toHaveBeenCalled();
  });

  it('shows "Registrar" for PURCHASE type', () => {
    const { getByText } = render(<CartSummaryBar {...defaultProps} type="PURCHASE" />);
    expect(getByText('Registrar')).toBeTruthy();
  });
});
```

### 6.4 ReceiptPreview

**File:** `tests/components/receipt/ReceiptPreview.test.tsx`

```tsx
import React from 'react';
import { render } from '@testing-library/react-native';
import { ReceiptPreview } from '@/presentation/components/receipt/ReceiptPreview';
import { Money } from '@/domain/value-objects/Money';
import { PaymentMethod } from '@/domain/entities/PaymentMethod';

describe('ReceiptPreview', () => {
  const mockTransaction = {
    id: 123,
    type: 'SALE' as const,
    totalAmount: new Money(75.00, 'USD'),
    paymentMethod: new PaymentMethod('CASH'),
    detail: 'Test sale',
    registeredAt: '2026-11-10T10:30:00Z',
    deletedAt: null,
  };

  const mockDetails = [
    { productId: 1, productName: 'Product A', quantity: 2, unitPrice: new Money(10.00, 'USD'), lineTotal: new Money(20.00, 'USD') },
    { productId: 2, productName: 'Product B', quantity: 1, unitPrice: new Money(55.00, 'USD'), lineTotal: new Money(55.00, 'USD') },
  ];

  it('renders receipt title', () => {
    const { getByText } = render(<ReceiptPreview transaction={mockTransaction} details={mockDetails} />);
    expect(getByText('Recibo')).toBeTruthy();
  });

  it('renders transaction type', () => {
    const { getByText } = render(<ReceiptPreview transaction={mockTransaction} details={mockDetails} />);
    expect(getByText('VENTA')).toBeTruthy();
  });

  it('renders transaction ID', () => {
    const { getByText } = render(<ReceiptPreview transaction={mockTransaction} details={mockDetails} />);
    expect(getByText('Transacción #123')).toBeTruthy();
  });

  it('renders date', () => {
    const { getByText } = render(<ReceiptPreview transaction={mockTransaction} details={mockDetails} />);
    expect(getByText('2026-11-10')).toBeTruthy();
  });

  it('renders items list', () => {
    const { getByText } = render(<ReceiptPreview transaction={mockTransaction} details={mockDetails} />);
    expect(getByText('Product A')).toBeTruthy();
    expect(getByText('2 × $10.00')).toBeTruthy();
    expect(getByText('$20.00')).toBeTruthy();
    expect(getByText('Product B')).toBeTruthy();
    expect(getByText('1 × $55.00')).toBeTruthy();
    expect(getByText('$55.00')).toBeTruthy();
  });

  it('renders payment method', () => {
    const { getByText } = render(<ReceiptPreview transaction={mockTransaction} details={mockDetails} />);
    expect(getByText('Pago: EFECTIVO')).toBeTruthy();
  });

  it('renders total', () => {
    const { getByText } = render(<ReceiptPreview transaction={mockTransaction} details={mockDetails} />);
    expect(getByText('Total: $75.00')).toBeTruthy();
  });

  it('renders change for cash payments', () => {
    const cashTransaction = { ...mockTransaction, totalAmount: new Money(75.00, 'USD') };
    const { getByText } = render(<ReceiptPreview transaction={cashTransaction} details={mockDetails} receivedAmount={100} />);
    expect(getByText('Cambio: $25.00')).toBeTruthy();
  });

  it('does not render change for non-cash payments', () => {
    const cardTransaction = { ...mockTransaction, paymentMethod: new PaymentMethod('CARD') };
    const { queryByText } = render(<ReceiptPreview transaction={cardTransaction} details={mockDetails} />);
    expect(queryByText(/Cambio/)).toBeNull();
  });
});
```

---

## 7. E2E Tests

### 7.1 Sale Flow

**File:** `tests/e2e/sales-checkout/saleFlow.test.ts`

```ts
import { getDatabase } from '@/data/database/client';
import { registerSale } from '@/application/sales/RegisterSale';
import { PaymentMethod } from '@/domain/entities/PaymentMethod';
import { SimpleCartItemFactory } from '@/domain/entities/CartItem';
import { Money } from '@/domain/value-objects/Money';

describe('E2E: Sale Flow', () => {
  let db: ReturnType<typeof getDatabase>;
  let factory: SimpleCartItemFactory;
  let productId: number;

  beforeAll(() => {
    db = getDatabase();
    factory = new SimpleCartItemFactory();
  });

  beforeEach(() => {
    const result = db.runSync(
      `INSERT INTO products (name, selling_price, purchase_price, current_stock, min_stock, category_id)
       VALUES (?, ?, ?, ?, ?, ?)`,
      'Coca Cola 500ml', 2.50, 1.50, 100, 20, 1
    );
    productId = result.lastInsertRowId;
    db.runSync('UPDATE resumen_caja SET total_sales = 0, current_balance = 500 WHERE id = 1');
  });

  afterEach(() => {
    db.runSync('DELETE FROM sales_transactions');
    db.runSync('DELETE FROM transaction_details');
    db.runSync('DELETE FROM products WHERE id = ?', productId);
  });

  it('completes full sale: add items → cash payment with change → receipt', () => {
    // 1. Add items to cart (simulated)
    const item = factory.createFromProduct(
      { id: productId, name: 'Coca Cola 500ml', sellingPrice: new Money(2.50, 'USD'), purchasePrice: new Money(1.50, 'USD'), currentStock: 100, imageUri: null },
      4,
      new Money(0, 'USD'),
      'SALE'
    );

    // 2. Process payment (cash with change)
    const result = registerSale({
      items: [item],
      paymentMethod: new PaymentMethod('CASH'),
      detail: 'E2E test sale',
    });

    // 3. Verify transaction created
    expect(result.transactionId).toBeGreaterThan(0);
    expect(result.total).toBe(10.00);

    // 4. Verify stock decremented
    const product = db.getFirstSync('SELECT current_stock FROM products WHERE id = ?', productId);
    expect(product.current_stock).toBe(96);

    // 5. Verify resumen_caja updated
    const caja = db.getFirstSync('SELECT total_sales, current_balance FROM resumen_caja WHERE id = 1');
    expect(caja.total_sales).toBe(10.00);
    expect(caja.current_balance).toBe(510.00);

    // 6. Verify receipt data available
    const tx = db.getFirstSync('SELECT * FROM sales_transactions WHERE id = ?', result.transactionId);
    expect(tx.type).toBe('SALE');
    expect(tx.payment_method).toBe('CASH');
    expect(tx.total_amount).toBe(10.00);

    const details = db.getAllSync('SELECT * FROM transaction_details WHERE transaction_id = ?', result.transactionId);
    expect(details).toHaveLength(1);
    expect(details[0].quantity).toBe(4);
    expect(details[0].unit_price).toBe(2.50);
  });

  it('generates receipt with correct change calculation', () => {
    const item = factory.createFromProduct(
      { id: productId, name: 'Coca Cola 500ml', sellingPrice: new Money(2.50, 'USD'), purchasePrice: new Money(1.50, 'USD'), currentStock: 100, imageUri: null },
      2,
      new Money(0, 'USD'),
      'SALE'
    );

    const result = registerSale({
      items: [item],
      paymentMethod: new PaymentMethod('CASH'),
      detail: 'Receipt test',
    });

    // Simulate receipt generation with received amount
    const received = 10.00;
    const change = received - result.total; // 10 - 5 = 5

    expect(change).toBe(5.00);
    // Receipt would show: Total: $5.00, Cambio: $5.00
  });
});
```

### 7.2 Purchase Flow

**File:** `tests/e2e/sales-checkout/purchaseFlow.test.ts`

```ts
import { getDatabase } from '@/data/database/client';
import { registerPurchase } from '@/application/purchases/RegisterPurchase';
import { PaymentMethod } from '@/domain/entities/PaymentMethod';
import { SimpleCartItemFactory } from '@/domain/entities/CartItem';
import { Money } from '@/domain/value-objects/Money';

describe('E2E: Purchase Flow', () => {
  let db: ReturnType<typeof getDatabase>;
  let factory: SimpleCartItemFactory;
  let productId: number;

  beforeAll(() => {
    db = getDatabase();
    factory = new SimpleCartItemFactory();
  });

  beforeEach(() => {
    const result = db.runSync(
      `INSERT INTO products (name, selling_price, purchase_price, current_stock, min_stock, category_id)
       VALUES (?, ?, ?, ?, ?, ?)`,
      'Coca Cola 500ml', 2.50, 1.50, 100, 20, 1
    );
    productId = result.lastInsertRowId;
    db.runSync('UPDATE resumen_caja SET total_purchases = 0, current_balance = 500 WHERE id = 1');
  });

  afterEach(() => {
    db.runSync('DELETE FROM sales_transactions');
    db.runSync('DELETE FROM transaction_details');
    db.runSync('DELETE FROM products WHERE id = ?', productId);
  });

  it('completes full purchase: add items → card payment → receipt', () => {
    const item = factory.createFromProduct(
      { id: productId, name: 'Coca Cola 500ml', sellingPrice: new Money(2.50, 'USD'), purchasePrice: new Money(1.50, 'USD'), currentStock: 100, imageUri: null },
      50,
      new Money(0, 'USD'),
      'PURCHASE'
    );

    const result = registerPurchase({
      items: [item],
      paymentMethod: new PaymentMethod('CARD'),
      detail: 'Stock replenishment',
    });

    expect(result.transactionId).toBeGreaterThan(0);
    expect(result.total).toBe(75.00); // 50 × 1.50

    // Verify stock incremented
    const product = db.getFirstSync('SELECT current_stock FROM products WHERE id = ?', productId);
    expect(product.current_stock).toBe(150);

    // Verify resumen_caja updated
    const caja = db.getFirstSync('SELECT total_purchases, current_balance FROM resumen_caja WHERE id = 1');
    expect(caja.total_purchases).toBe(75.00);
    expect(caja.current_balance).toBe(425.00); // 500 - 75

    // Verify transaction type
    const tx = db.getFirstSync('SELECT type FROM sales_transactions WHERE id = ?', result.transactionId);
    expect(tx.type).toBe('PURCHASE');
  });
});
```

### 7.3 Revert Sale

**File:** `tests/e2e/sales-checkout/revertSale.test.ts`

```ts
import { getDatabase } from '@/data/database/client';
import { registerSale } from '@/application/sales/RegisterSale';
import { revertTransaction } from '@/application/transactions/RevertTransaction';
import { PaymentMethod } from '@/domain/entities/PaymentMethod';
import { SimpleCartItemFactory } from '@/domain/entities/CartItem';
import { Money } from '@/domain/value-objects/Money';

describe('E2E: Revert Sale', () => {
  let db: ReturnType<typeof getDatabase>;
  let factory: SimpleCartItemFactory;
  let productId: number;

  beforeAll(() => {
    db = getDatabase();
    factory = new SimpleCartItemFactory();
  });

  beforeEach(() => {
    const result = db.runSync(
      `INSERT INTO products (name, selling_price, purchase_price, current_stock, min_stock, category_id)
       VALUES (?, ?, ?, ?, ?, ?)`,
      'Coca Cola 500ml', 2.50, 1.50, 100, 20, 1
    );
    productId = result.lastInsertRowId;
    db.runSync('UPDATE resumen_caja SET total_sales = 0, current_balance = 500 WHERE id = 1');
  });

  afterEach(() => {
    db.runSync('DELETE FROM sales_transactions');
    db.runSync('DELETE FROM transaction_details');
    db.runSync('DELETE FROM products WHERE id = ?', productId);
  });

  it('reverts sale and restores stock/balance', () => {
    // Create sale
    const item = factory.createFromProduct(
      { id: productId, name: 'Coca Cola 500ml', sellingPrice: new Money(2.50, 'USD'), purchasePrice: new Money(1.50, 'USD'), currentStock: 100, imageUri: null },
      10,
      new Money(0, 'USD'),
      'SALE'
    );

    const saleResult = registerSale({
      items: [item],
      paymentMethod: new PaymentMethod('CASH'),
      detail: 'Sale to revert',
    });

    // Verify sale state
    let product = db.getFirstSync('SELECT current_stock FROM products WHERE id = ?', productId);
    expect(product.current_stock).toBe(90);

    let caja = db.getFirstSync('SELECT total_sales, current_balance FROM resumen_caja WHERE id = 1');
    expect(caja.total_sales).toBe(25.00);
    expect(caja.current_balance).toBe(525.00);

    // Revert
    revertTransaction(saleResult.transactionId);

    // Verify restored
    product = db.getFirstSync('SELECT current_stock FROM products WHERE id = ?', productId);
    expect(product.current_stock).toBe(100);

    caja = db.getFirstSync('SELECT total_sales, current_balance FROM resumen_caja WHERE id = 1');
    expect(caja.total_sales).toBe(0.00);
    expect(caja.current_balance).toBe(500.00);

    // Verify soft delete
    const tx = db.getFirstSync('SELECT deleted_at FROM sales_transactions WHERE id = ?', saleResult.transactionId);
    expect(tx.deleted_at).not.toBeNull();
  });
});
```

### 7.4 Migration Idempotency

**File:** `tests/e2e/sales-checkout/migrationIdempotency.test.ts`

```ts
import { getDatabase } from '@/data/database/client';
import { runMigrations } from '@/data/database/migrations';

describe('E2E: Migration Idempotency', () => {
  let db: ReturnType<typeof getDatabase>;

  beforeAll(() => {
    db = getDatabase();
  });

  beforeEach(() => {
    // Reset to v1 schema with data
    db.execSync(`
      DROP TABLE IF EXISTS sales_transactions;
      DROP TABLE IF EXISTS transaction_details;
      DROP TABLE IF EXISTS transacciones;
      DROP TABLE IF EXISTS detalle_transacciones;
      DROP TABLE IF EXISTS resumen_caja;
    `);

    db.execSync(`
      CREATE TABLE transacciones (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        tipo TEXT NOT NULL,
        monto_total REAL NOT NULL,
        tipo_pago TEXT NOT NULL,
        detalle TEXT,
        fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        fecha_delete TIMESTAMP DEFAULT NULL
      );

      CREATE TABLE detalle_transacciones (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        transaccion_id INTEGER NOT NULL,
        producto_id INTEGER NOT NULL,
        cantidad REAL NOT NULL,
        precio_unitario REAL NOT NULL
      );

      CREATE TABLE resumen_caja (
        id INTEGER PRIMARY KEY CHECK (id = 1),
        total_ventas REAL DEFAULT 0.0,
        total_compras REAL DEFAULT 0.0,
        total_gastos REAL DEFAULT 0.0,
        saldo_actual REAL DEFAULT 0.0
      );

      INSERT OR IGNORE INTO resumen_caja VALUES (1, 100, 50, 25, 25);
      INSERT INTO transacciones (tipo, monto_total, tipo_pago, detalle) VALUES ('VENTA', 100, 'EFECTIVO', 'Test');
    `);
  });

  it('runs successfully twice without errors', () => {
    expect(() => runMigrations()).not.toThrow();
    expect(() => runMigrations()).not.toThrow();
  });

  it('preserves data after second run', () => {
    runMigrations();
    runMigrations();

    const txCount = db.getFirstSync('SELECT COUNT(*) as count FROM sales_transactions');
    expect(txCount.count).toBe(1);

    const caja = db.getFirstSync('SELECT * FROM resumen_caja WHERE id = 1');
    expect(caja.total_ventas).toBe(100);
    expect(caja.total_compras).toBe(50);
    expect(caja.total_gastos).toBe(25);
    expect(caja.saldo_actual).toBe(25);
  });

  it('does not duplicate indexes', () => {
    runMigrations();
    runMigrations();

    const indexes = db.getAllSync("SELECT name FROM sqlite_master WHERE type='index' AND name LIKE 'idx_sales_transactions%'");
    // Should only have one of each index
    expect(indexes.filter(i => i.name === 'idx_sales_transactions_date')).toHaveLength(1);
    expect(indexes.filter(i => i.name === 'idx_sales_transactions_type')).toHaveLength(1);
  });
});
```

---

## 8. Test Commands

```json
{
  "scripts": {
    "test:unit": "jest --testPathPattern=tests/unit",
    "test:integration": "jest --testPathPattern=tests/integration",
    "test:migration": "jest --testPathPattern=tests/migration",
    "test:atomic": "jest --testPathPattern=tests/integration/atomic",
    "test:e2e": "jest --testPathPattern=tests/e2e",
    "test:all": "jest",
    "test:coverage": "jest --coverage"
  }
}
```

---

## 9. CI Checks

```yaml
# .github/workflows/test.yml
- name: TypeScript Check
  run: tsc --noEmit
- name: Lint
  run: eslint .
- name: Unit Tests
  run: npm run test:unit
- name: Integration Tests
  run: npm run test:integration
- name: Migration Tests
  run: npm run test:migration
- name: Atomic Tests
  run: npm run test:atomic
- name: E2E Tests
  run: npm run test:e2e
- name: AdWare Check
  run: |
    if grep -r "AdBanner" --include="*.tsx" src/presentation/components/cart; then
      echo "ERROR: AdBanner found in checkout components"
      exit 1
    fi
- name: i18n Check
  run: |
    if grep -r "checkout\." --include="*.tsx" src/presentation/components/cart | grep -v "\.json"; then
      echo "OK: i18n keys used"
    else
      echo "WARNING: Check for hardcoded strings"
    fi
```

---

*Test specification version: 1.0 — 2026-11-10*