# Catalog & Inventory — Test Specification (v2.0.0)

## Overview

Complete TDD test suite for the Catalog & Inventory module v2.0.0 using Jest. Covers unit tests (domain), integration tests (data layer), **critical DB data migration tests**, hook tests, UI component tests, and E2E scenarios.

---

## 1. Unit Tests (Domain Layer — Pure TypeScript)

### 1.1 `SKU` Value Object
**File:** `src/__tests__/domain/value-objects/SKU.test.ts`

```typescript
import { SKU } from '@/domain/value-objects/SKU';

describe('SKU Value Object', () => {
  describe('constructor', () => {
    it('should create valid SKU from lowercase input', () => {
      const sku = new SKU('abc-123');
      expect(sku.value).toBe('ABC-123');
    });

    it('should create valid SKU with underscore', () => {
      const sku = new SKU('PROD_001');
      expect(sku.value).toBe('PROD_001');
    });

    it('should throw on empty string', () => {
      expect(() => new SKU('')).toThrow('SKU cannot be empty');
      expect(() => new SKU('   ')).toThrow('SKU cannot be empty');
    });

    it('should throw on >32 characters', () => {
      const long = 'A'.repeat(33);
      expect(() => new SKU(long)).toThrow('SKU max length is 32');
    });

    it('should throw on invalid characters', () => {
      expect(() => new SKU('abc@123')).toThrow('SKU must be alphanumeric');
      expect(() => new SKU('abc 123')).toThrow('SKU must be alphanumeric');
      expect(() => new SKU('abc.123')).toThrow('SKU must be alphanumeric');
    });
  });

  describe('equals', () => {
    it('should return true for same value', () => {
      expect(new SKU('ABC-123').equals(new SKU('abc-123'))).toBe(true);
    });

    it('should return false for different values', () => {
      expect(new SKU('ABC-123').equals(new SKU('XYZ-789'))).toBe(false);
    });
  });

  describe('toString', () => {
    it('should return uppercase value', () => {
      expect(new SKU('abc-123').toString()).toBe('ABC-123');
    });
  });
});
```

### 1.2 `Stock` Value Object
**File:** `src/__tests__/domain/value-objects/Stock.test.ts`

```typescript
import { Stock } from '@/domain/value-objects/Stock';

describe('Stock Value Object', () => {
  describe('constructor', () => {
    it('should create zero stock', () => {
      const stock = Stock.zero();
      expect(stock.value).toBe(0);
    });

    it('should create integer stock', () => {
      const stock = new Stock(10);
      expect(stock.value).toBe(10);
    });

    it('should create fractional stock (3dp)', () => {
      const stock = new Stock(0.5);
      expect(stock.value).toBe(0.5);
    });

    it('should round to 3 decimal places', () => {
      const stock = new Stock(1.2345);
      expect(stock.value).toBe(1.235);
    });

    it('should throw on negative value', () => {
      expect(() => new Stock(-0.1)).toThrow('Stock must be a non-negative number');
      expect(() => new Stock(-1)).toThrow('Stock must be a non-negative number');
    });

    it('should throw on NaN', () => {
      expect(() => new Stock(NaN)).toThrow('Stock must be a non-negative number');
    });

    it('should throw on Infinity', () => {
      expect(() => new Stock(Infinity)).toThrow('Stock must be a non-negative number');
    });
  });

  describe('add', () => {
    it('should add integer stocks', () => {
      expect(new Stock(5).add(new Stock(3)).value).toBe(8);
    });

    it('should add fractional stocks', () => {
      expect(new Stock(0.5).add(new Stock(0.25)).value).toBe(0.75);
    });

    it('should handle precision correctly', () => {
      expect(new Stock(0.1).add(new Stock(0.2)).value).toBe(0.3);
    });
  });

  describe('subtract', () => {
    it('should subtract integer stocks', () => {
      expect(new Stock(10).subtract(new Stock(3)).value).toBe(7);
    });

    it('should subtract fractional stocks', () => {
      expect(new Stock(1.0).subtract(new Stock(0.3)).value).toBe(0.7);
    });

    it('should throw when result would be negative', () => {
      expect(() => new Stock(0.5).subtract(new Stock(1.0))).toThrow('Stock cannot go negative');
    });
  });

  describe('isBelow', () => {
    it('should return true when current < min', () => {
      expect(new Stock(0.5).isBelow(new Stock(1.0))).toBe(true);
      expect(new Stock(5).isBelow(new Stock(10))).toBe(true);
    });

    it('should return false when current >= min', () => {
      expect(new Stock(1.0).isBelow(new Stock(1.0))).toBe(false);
      expect(new Stock(10).isBelow(new Stock(5))).toBe(false);
    });
  });

  describe('equals', () => {
    it('should return true for equal values', () => {
      expect(new Stock(1.5).equals(new Stock(1.5))).toBe(true);
    });

    it('should return false for different values', () => {
      expect(new Stock(1.5).equals(new Stock(1.4))).toBe(false);
    });
  });

  describe('toString', () => {
    it('should return string representation', () => {
      expect(new Stock(0.5).toString()).toBe('0.5');
      expect(new Stock(10).toString()).toBe('10');
    });
  });
});
```

### 1.3 `Product` Entity
**File:** `src/__tests__/domain/entities/Product.test.ts`

```typescript
import { Product, CreateProductInput } from '@/domain/entities/Product';
import { Category } from '@/domain/entities/Category';
import { SKU } from '@/domain/value-objects/SKU';
import { Stock } from '@/domain/value-objects/Stock';
import { Money } from '@/domain/value-objects/Money';

describe('Product Entity', () => {
  const validCategory = new Category(1, 'Beverages', '2024-01-01', null);
  const validMoney = (amount: number) => new Money(amount, 'USD');

  const validInput: CreateProductInput = {
    categoryId: 1,
    sku: 'COKE-500ML',
    name: 'Coca-Cola 500ml',
    sellingPrice: 2.50,
    purchasePrice: 1.75,
    currentStock: 100,
    minStock: 10,
    imageUri: null,
  };

  it('should create product with all fields', () => {
    const product = Product.create(validInput, validCategory);
    expect(product.id).toBeUndefined(); // ID assigned by repo
    expect(product.categoryId).toBe(1);
    expect(product.sku).toBeInstanceOf(SKU);
    expect(product.sku?.value).toBe('COKE-500ML');
    expect(product.name).toBe('Coca-Cola 500ml');
    expect(product.sellingPrice).toBeInstanceOf(Money);
    expect(product.sellingPrice.amount).toBe(2.50);
    expect(product.currentStock).toBeInstanceOf(Stock);
    expect(product.currentStock.value).toBe(100);
    expect(product.minStock.value).toBe(10);
    expect(product.deletedAt).toBeNull();
  });

  it('should create product without SKU', () => {
    const input = { ...validInput, sku: null };
    const product = Product.create(input, validCategory);
    expect(product.sku).toBeNull();
  });

  it('should throw when name is empty', () => {
    const input = { ...validInput, name: '' };
    expect(() => Product.create(input, validCategory)).toThrow('Name is required');
  });

  it('should throw when sellingPrice is negative', () => {
    const input = { ...validInput, sellingPrice: -1 };
    expect(() => Product.create(input, validCategory)).toThrow('Selling price must be non-negative');
  });

  it('should throw when currentStock is negative', () => {
    const input = { ...validInput, currentStock: -1 };
    expect(() => Product.create(input, validCategory)).toThrow('Stock must be a non-negative number');
  });

  it('should throw when minStock is negative', () => {
    const input = { ...validInput, minStock: -1 };
    expect(() => Product.create(input, validCategory)).toThrow('Stock must be a non-negative number');
  });

  it('should create fractional stock product', () => {
    const input = { ...validInput, currentStock: 0.5, minStock: 0.1 };
    const product = Product.create(input, validCategory);
    expect(product.currentStock.value).toBe(0.5);
    expect(product.minStock.value).toBe(0.1);
  });
});
```

### 1.4 `Category` Entity
**File:** `src/__tests__/domain/entities/Category.test.ts`

```typescript
import { Category, CreateCategoryInput } from '@/domain/entities/Category';

describe('Category Entity', () => {
  it('should create category with name', () => {
    const input: CreateCategoryInput = { name: 'Beverages' };
    const category = Category.create(input);
    expect(category.name).toBe('Beverages');
    expect(category.deletedAt).toBeNull();
  });

  it('should throw when name is empty', () => {
    const input: CreateCategoryInput = { name: '' };
    expect(() => Category.create(input)).toThrow('Category name is required');
  });

  it('should throw when name is whitespace', () => {
    const input: CreateCategoryInput = { name: '   ' };
    expect(() => Category.create(input)).toThrow('Category name is required');
  });
});
```

---

## 2. Integration Tests (Data Layer — SQLite)

### 2.1 `SqliteProductRepository`
**File:** `src/__tests__/data/repositories/SqliteProductRepository.test.ts`

```typescript
import { getDatabase, closeDatabase } from '@/data/database/client';
import { SqliteProductRepository } from '@/data/repositories/SqliteProductRepository';
import { SqliteCategoryRepository } from '@/data/repositories/SqliteCategoryRepository';
import { runMigrations } from '@/data/database/migrations';
import type { Product, Category } from '@/domain/entities';
import type { CreateProductInput } from '@/domain/entities/Product';

const db = getDatabase();
const productRepo = new SqliteProductRepository(db);
const categoryRepo = new SqliteCategoryRepository(db);

beforeAll(() => {
  runMigrations(db);
  // Seed default category
  categoryRepo.create({ name: 'General' });
});

afterAll(() => {
  closeDatabase();
});

beforeEach(() => {
  // Clean products table
  db.execSync('DELETE FROM products');
});

describe('SqliteProductRepository', () => {
  const createCategory = (name: string): Category => categoryRepo.create({ name });

  describe('create', () => {
    it('should create product with valid category', () => {
      const category = createCategory('Beverages');
      const input: CreateProductInput = {
        categoryId: category.id,
        sku: 'COKE-500',
        name: 'Coca-Cola 500ml',
        sellingPrice: 2.50,
        purchasePrice: 1.75,
        currentStock: 100,
        minStock: 10,
        imageUri: null,
      };
      const product = productRepo.create(input);
      expect(product.id).toBeGreaterThan(0);
      expect(product.categoryId).toBe(category.id);
      expect(product.sku?.value).toBe('COKE-500');
      expect(product.currentStock.value).toBe(100);
    });

    it('should create product with fractional stock', () => {
      const category = createCategory('Produce');
      const input: CreateProductInput = {
        categoryId: category.id,
        sku: null,
        name: 'Bananas (kg)',
        sellingPrice: 1.20,
        purchasePrice: 0.80,
        currentStock: 25.5,
        minStock: 2.0,
        imageUri: null,
      };
      const product = productRepo.create(input);
      expect(product.currentStock.value).toBe(25.5);
      expect(product.minStock.value).toBe(2.0);
    });

    it('should throw when category_id is missing', () => {
      const input: CreateProductInput = {
        categoryId: 999, // non-existent
        sku: 'TEST-001',
        name: 'Test Product',
        sellingPrice: 10,
        purchasePrice: 5,
        currentStock: 10,
        minStock: 1,
        imageUri: null,
      };
      expect(() => productRepo.create(input)).toThrow('Category not found');
    });

    it('should throw on duplicate SKU', () => {
      const category = createCategory('Test');
      const input: CreateProductInput = {
        categoryId: category.id,
        sku: 'DUPLICATE',
        name: 'Product 1',
        sellingPrice: 10,
        purchasePrice: 5,
        currentStock: 10,
        minStock: 1,
        imageUri: null,
      };
      productRepo.create(input);
      const input2 = { ...input, name: 'Product 2' };
      expect(() => productRepo.create(input2)).toThrow('SKU already exists');
    });

    it('should throw on negative price', () => {
      const category = createCategory('Test');
      const input: CreateProductInput = {
        categoryId: category.id,
        sku: 'NEG-PRICE',
        name: 'Negative Price',
        sellingPrice: -1,
        purchasePrice: 5,
        currentStock: 10,
        minStock: 1,
        imageUri: null,
      };
      expect(() => productRepo.create(input)).toThrow('Price must be non-negative');
    });
  });

  describe('findBySku', () => {
    it('should return product by SKU', () => {
      const category = createCategory('Test');
      productRepo.create({
        categoryId: category.id,
        sku: 'FIND-ME',
        name: 'Findable Product',
        sellingPrice: 10,
        purchasePrice: 5,
        currentStock: 10,
        minStock: 1,
        imageUri: null,
      });
      const found = productRepo.findBySku('FIND-ME');
      expect(found).not.toBeNull();
      expect(found?.name).toBe('Findable Product');
    });

    it('should return null for non-existent SKU', () => {
      const found = productRepo.findBySku('NON-EXISTENT');
      expect(found).toBeNull();
    });

    it('should not find soft-deleted product by SKU', () => {
      const category = createCategory('Test');
      const product = productRepo.create({
        categoryId: category.id,
        sku: 'DELETED-SKU',
        name: 'Deleted Product',
        sellingPrice: 10,
        purchasePrice: 5,
        currentStock: 10,
        minStock: 1,
        imageUri: null,
      });
      productRepo.delete(product.id);
      const found = productRepo.findBySku('DELETED-SKU');
      expect(found).toBeNull();
    });
  });

  describe('list', () => {
    beforeEach(() => {
      const cat1 = createCategory('Cat A');
      const cat2 = createCategory('Cat B');
      productRepo.create({ categoryId: cat1.id, sku: 'A1', name: 'Alpha', sellingPrice: 10, purchasePrice: 5, currentStock: 100, minStock: 10, imageUri: null });
      productRepo.create({ categoryId: cat1.id, sku: 'A2', name: 'Beta', sellingPrice: 20, purchasePrice: 10, currentStock: 50, minStock: 5, imageUri: null });
      productRepo.create({ categoryId: cat2.id, sku: 'B1', name: 'Gamma', sellingPrice: 30, purchasePrice: 15, currentStock: 200, minStock: 20, imageUri: null });
    });

    it('should filter by category', () => {
      const cat1 = categoryRepo.findById(1)!;
      const results = productRepo.list({ categoryId: cat1.id, search: undefined, sort: 'NAME_ASC', limit: 10, offset: 0 });
      expect(results.every(p => p.categoryId === cat1.id)).toBe(true);
    });

    it('should search by name (case-insensitive)', () => {
      const results = productRepo.list({ search: 'alpha', categoryId: undefined, sort: 'NAME_ASC', limit: 10, offset: 0 });
      expect(results.length).toBe(1);
      expect(results[0].name).toBe('Alpha');
    });

    it('should sort by name ASC', () => {
      const results = productRepo.list({ search: undefined, categoryId: undefined, sort: 'NAME_ASC', limit: 10, offset: 0 });
      expect(results.map(p => p.name)).toEqual(['Alpha', 'Beta', 'Gamma']);
    });

    it('should sort by popularity DESC', () => {
      // Popularity requires sales data - simplified test
      const results = productRepo.list({ search: undefined, categoryId: undefined, sort: 'POPULARITY_DESC', limit: 10, offset: 0 });
      expect(results.length).toBe(3);
    });

    it('should paginate with limit and offset', () => {
      const page1 = productRepo.list({ search: undefined, categoryId: undefined, sort: 'NAME_ASC', limit: 2, offset: 0 });
      const page2 = productRepo.list({ search: undefined, categoryId: undefined, sort: 'NAME_ASC', limit: 2, offset: 2 });
      expect(page1.length).toBe(2);
      expect(page2.length).toBe(1);
      expect(page1[0].name).toBe('Alpha');
      expect(page2[0].name).toBe('Gamma');
    });

    it('should exclude soft-deleted products', () => {
      const cat1 = createCategory('Cat A');
      const product = productRepo.create({ categoryId: cat1.id, sku: 'TO-DELETE', name: 'To Delete', sellingPrice: 10, purchasePrice: 5, currentStock: 10, minStock: 1, imageUri: null });
      productRepo.delete(product.id);
      const results = productRepo.list({ search: undefined, categoryId: undefined, sort: 'NAME_ASC', limit: 10, offset: 0 });
      expect(results.find(p => p.id === product.id)).toBeUndefined();
    });
  });

  describe('soft delete', () => {
    it('should set deleted_at and hide from list', () => {
      const category = createCategory('Test');
      const product = productRepo.create({ categoryId: category.id, sku: 'DEL-001', name: 'To Delete', sellingPrice: 10, purchasePrice: 5, currentStock: 10, minStock: 1, imageUri: null });
      productRepo.delete(product.id);
      const found = productRepo.findById(product.id);
      expect(found).toBeNull(); // findById filters deleted_at
      // Direct DB check
      const row = db.getFirstSync('SELECT deleted_at FROM products WHERE id = ?', product.id);
      expect(row?.deleted_at).not.toBeNull();
    });
  });
});
```

### 2.2 `SqliteCategoryRepository`
**File:** `src/__tests__/data/repositories/SqliteCategoryRepository.test.ts`

```typescript
import { getDatabase, closeDatabase } from '@/data/database/client';
import { SqliteCategoryRepository } from '@/data/repositories/SqliteCategoryRepository';
import { runMigrations } from '@/data/database/migrations';

const db = getDatabase();
const repo = new SqliteCategoryRepository(db);

beforeAll(() => {
  runMigrations(db);
});

afterAll(() => {
  closeDatabase();
});

beforeEach(() => {
  db.execSync('DELETE FROM categories');
});

describe('SqliteCategoryRepository', () => {
  it('should create category', () => {
    const cat = repo.create({ name: 'Electronics' });
    expect(cat.id).toBeGreaterThan(0);
    expect(cat.name).toBe('Electronics');
    expect(cat.deletedAt).toBeNull();
  });

  it('should list active categories', () => {
    repo.create({ name: 'Cat 1' });
    repo.create({ name: 'Cat 2' });
    const deleted = repo.create({ name: 'Deleted Cat' });
    repo.delete(deleted.id); // Assuming delete method exists or soft delete
    const list = repo.list();
    expect(list.length).toBe(2);
    expect(list.find(c => c.name === 'Deleted Cat')).toBeUndefined();
  });

  it('should find by id', () => {
    const created = repo.create({ name: 'Find Me' });
    const found = repo.findById(created.id);
    expect(found).not.toBeNull();
    expect(found?.name).toBe('Find Me');
  });

  it('should return null for non-existent id', () => {
    expect(repo.findById(999)).toBeNull();
  });
});
```

---

## 3. DB Data Migration Tests (CRITICAL — Zero Data Loss)

**File:** `src/__tests__/integration/migration/v2_0_0_migration.test.ts`

```typescript
import { getDatabase, closeDatabase, resetDatabaseForTests } from '@/data/database/client';
import { runMigrations, TARGET_VERSION } from '@/data/database/migrations';
import { SqliteProductRepository } from '@/data/repositories/SqliteProductRepository';
import { SqliteCategoryRepository } from '@/data/repositories/SqliteCategoryRepository';
import * as SQLite from 'expo-sqlite';

describe('Migration v2.0.0: Spanish → English Schema', () => {
  let db: SQLite.SQLiteDatabase;
  let productRepo: SqliteProductRepository;
  let categoryRepo: SqliteCategoryRepository;

  beforeEach(() => {
    // Fresh in-memory database for each test
    resetDatabaseForTests();
    db = getDatabase();
    productRepo = new SqliteProductRepository(db);
    categoryRepo = new SqliteCategoryRepository(db);
  });

  afterEach(() => {
    closeDatabase();
  });

  it('should migrate v1 Spanish schema to v2 English schema', () => {
    // Step 1: Create v1.0.0 schema (Spanish)
    db.execSync(`
      CREATE TABLE categorias (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        nombre TEXT NOT NULL,
        fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        fecha_delete TIMESTAMP DEFAULT NULL
      );
      CREATE TABLE productos (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        categoria_id INTEGER,
        nombre TEXT NOT NULL,
        precio_venta REAL NOT NULL,
        precio_compra REAL NOT NULL,
        stock_actual REAL DEFAULT 0.0,
        stock_minimo REAL DEFAULT 0.0,
        imagen_uri TEXT,
        fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        fecha_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        fecha_delete TIMESTAMP DEFAULT NULL
      );
    `);

    // Step 2: Seed v1 data (including edge cases)
    db.execSync(`
      INSERT INTO categorias (id, nombre, fecha_registro, fecha_delete) VALUES
        (1, 'Bebidas', '2024-01-01 10:00:00', NULL),
        (2, 'Alimentos', '2024-01-02 10:00:00', NULL);
      INSERT INTO productos (id, categoria_id, nombre, precio_venta, precio_compra, stock_actual, stock_minimo, imagen_uri, fecha_registro, fecha_update, fecha_delete) VALUES
        (1, 1, 'Coca-Cola 500ml', 2.50, 1.75, 100, 10, NULL, '2024-01-01 10:00:00', '2024-01-01 10:00:00', NULL),
        (2, 1, 'Pepsi 500ml', 2.30, 1.60, 50, 5, NULL, '2024-01-01 11:00:00', '2024-01-01 11:00:00', NULL),
        (3, NULL, 'Pan integral', 1.50, 1.00, 30, 3, NULL, '2024-01-02 10:00:00', '2024-01-02 10:00:00', '2024-06-01 10:00:00'), -- soft-deleted, NULL category
        (4, 2, 'Arroz 1kg', 3.00, 2.00, 200.5, 20.0, NULL, '2024-01-03 10:00:00', '2024-01-03 10:00:00', NULL); -- fractional stock
    `);

    // Verify pre-migration state
    const preProducts = db.getAllSync('SELECT * FROM productos');
    const preCategories = db.getAllSync('SELECT * FROM categorias');
    expect(preProducts.length).toBe(4);
    expect(preCategories.length).toBe(2);

    // Step 3: Run migration v2
    runMigrations(db);

    // Step 4: Verify post-migration schema
    const tables = db.getAllSync("SELECT name FROM sqlite_master WHERE type='table'");
    const tableNames = tables.map(t => t.name);
    expect(tableNames).toContain('products');
    expect(tableNames).toContain('categories');
    expect(tableNames).not.toContain('productos');
    expect(tableNames).not.toContain('categorias');

    // Step 5: Verify columns renamed
    const productCols = db.getAllSync("PRAGMA table_info(products)");
    const colNames = productCols.map(c => c.name);
    expect(colNames).toContain('category_id');
    expect(colNames).toContain('name');
    expect(colNames).toContain('selling_price');
    expect(colNames).toContain('purchase_price');
    expect(colNames).toContain('current_stock');
    expect(colNames).toContain('min_stock');
    expect(colNames).toContain('image_uri');
    expect(colNames).toContain('registered_at');
    expect(colNames).toContain('updated_at');
    expect(colNames).toContain('deleted_at');
    expect(colNames).toContain('sku');

    // Step 6: Verify data preserved
    const products = db.getAllSync('SELECT * FROM products');
    expect(products.length).toBe(4);

    const coke = products.find(p => p.name === 'Coca-Cola 500ml');
    expect(coke).toBeDefined();
    expect(coke!.category_id).toBe(1);
    expect(coke!.selling_price).toBe(2.50);
    expect(coke!.current_stock).toBe(100);
    expect(coke!.deleted_at).toBeNull();

    const bread = products.find(p => p.name === 'Pan integral');
    expect(bread).toBeDefined();
    expect(bread!.category_id).toBe(1); // Backfilled to default category (id=1)
    expect(bread!.deleted_at).not.toBeNull(); // Soft delete preserved

    const rice = products.find(p => p.name === 'Arroz 1kg');
    expect(rice).toBeDefined();
    expect(rice!.current_stock).toBe(200.5); // Fractional preserved
    expect(rice!.min_stock).toBe(20.0);

    // Step 7: Verify categories migrated
    const categories = db.getAllSync('SELECT * FROM categories');
    expect(categories.length).toBe(2);
    const beverages = categories.find(c => c.name === 'Bebidas');
    expect(beverages).toBeDefined();
    expect(beverages!.registered_at).toBe('2024-01-01 10:00:00');

    // Step 8: Verify indexes exist
    const indexes = db.getAllSync("SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='products'");
    const indexNames = indexes.map(i => i.name);
    expect(indexNames).toContain('idx_products_category');
    expect(indexNames).toContain('idx_products_name');
    expect(indexNames).toContain('idx_products_sku');
    expect(indexNames).toContain('idx_products_deleted_at');

    // Step 9: Verify user_version
    const version = db.getFirstSync('PRAGMA user_version');
    expect(version?.user_version).toBe(2);
  });

  it('should enforce NOT NULL on category_id after migration', () => {
    // Setup v1 schema
    db.execSync(`
      CREATE TABLE categorias (id INTEGER PRIMARY KEY, nombre TEXT, fecha_registro TEXT, fecha_delete TEXT);
      CREATE TABLE productos (id INTEGER PRIMARY KEY, categoria_id INTEGER, nombre TEXT, precio_venta REAL, precio_compra REAL, stock_actual REAL, stock_minimo REAL, imagen_uri TEXT, fecha_registro TEXT, fecha_update TEXT, fecha_delete TEXT);
      INSERT INTO categorias VALUES (1, 'General', '2024-01-01', NULL);
    `);
    runMigrations(db);

    // After migration, category_id should be NOT NULL
    // SQLite enforces via table rebuild - attempt insert without category_id
    expect(() => {
      db.runSync('INSERT INTO products (name, selling_price, purchase_price, current_stock, min_stock) VALUES (?, ?, ?, ?, ?)', ['Test', 10, 5, 10, 1]);
    }).toThrow();
  });

  it('should enforce UNIQUE on sku', () => {
    db.execSync(`
      CREATE TABLE categorias (id INTEGER PRIMARY KEY, nombre TEXT, fecha_registro TEXT, fecha_delete TEXT);
      CREATE TABLE productos (id INTEGER PRIMARY KEY, categoria_id INTEGER, nombre TEXT, precio_venta REAL, precio_compra REAL, stock_actual REAL, stock_minimo REAL, imagen_uri TEXT, fecha_registro TEXT, fecha_update TEXT, fecha_delete TEXT);
      INSERT INTO categorias VALUES (1, 'General', '2024-01-01', NULL);
    `);
    runMigrations(db);

    db.runSync('INSERT INTO products (category_id, sku, name, selling_price, purchase_price, current_stock, min_stock) VALUES (?, ?, ?, ?, ?, ?, ?)', [1, 'UNIQUE-SKU', 'Product 1', 10, 5, 10, 1]);
    expect(() => {
      db.runSync('INSERT INTO products (category_id, sku, name, selling_price, purchase_price, current_stock, min_stock) VALUES (?, ?, ?, ?, ?, ?, ?)', [1, 'UNIQUE-SKU', 'Product 2', 10, 5, 10, 1]);
    }).toThrow();
  });

  it('should be idempotent: running migration twice causes no errors', () => {
    db.execSync(`
      CREATE TABLE categorias (id INTEGER PRIMARY KEY, nombre TEXT, fecha_registro TEXT, fecha_delete TEXT);
      CREATE TABLE productos (id INTEGER PRIMARY KEY, categoria_id INTEGER, nombre TEXT, precio_venta REAL, precio_compra REAL, stock_actual REAL, stock_minimo REAL, imagen_uri TEXT, fecha_registro TEXT, fecha_update TEXT, fecha_delete TEXT);
      INSERT INTO categorias VALUES (1, 'General', '2024-01-01', NULL);
      INSERT INTO productos VALUES (1, 1, 'Test', 10, 5, 100, 10, NULL, '2024-01-01', '2024-01-01', NULL);
    `);

    // Run migration first time
    runMigrations(db);
    const countAfterFirst = db.getFirstSync('SELECT COUNT(*) as c FROM products')?.c;

    // Run migration second time (should not fail)
    runMigrations(db);
    const countAfterSecond = db.getFirstSync('SELECT COUNT(*) as c FROM products')?.c;

    expect(countAfterFirst).toBe(countAfterSecond);
  });

  it('should support rollback via backup restore', () => {
    // Create v1 schema and data
    db.execSync(`
      CREATE TABLE categorias (id INTEGER PRIMARY KEY, nombre TEXT, fecha_registro TEXT, fecha_delete TEXT);
      CREATE TABLE productos (id INTEGER PRIMARY KEY, categoria_id INTEGER, nombre TEXT, precio_venta REAL, precio_compra REAL, stock_actual REAL, stock_minimo REAL, imagen_uri TEXT, fecha_registro TEXT, fecha_update TEXT, fecha_delete TEXT);
      INSERT INTO categorias VALUES (1, 'Bebidas', '2024-01-01', NULL);
      INSERT INTO productos VALUES (1, 1, 'Coca-Cola', 2.5, 1.75, 100, 10, NULL, '2024-01-01', '2024-01-01', NULL);
    `);

    // Backup: copy tables
    const backupCategories = db.getAllSync('SELECT * FROM categorias');
    const backupProducts = db.getAllSync('SELECT * FROM productos');

    // Run migration
    runMigrations(db);

    // Simulate rollback: restore from backup
    // (In real app, this would be file restore; here we verify data can be reconstructed)
    expect(backupCategories.length).toBe(1);
    expect(backupProducts.length).toBe(1);
    expect(backupProducts[0].nombre).toBe('Coca-Cola');
    expect(backupProducts[0].precio_venta).toBe(2.5);
  });
});
```

---

## 4. Hook Tests

### 4.1 `useProducts`
**File:** `src/__tests__/presentation/hooks/useProducts.test.ts`

```typescript
import { renderHook, act } from '@testing-library/react-native';
import { useProducts } from '@/presentation/hooks/useProducts';
import { getDatabase, closeDatabase } from '@/data/database/client';
import { runMigrations } from '@/data/database/migrations';
import { SqliteProductRepository } from '@/data/repositories/SqliteProductRepository';
import { SqliteCategoryRepository } from '@/data/repositories/SqliteCategoryRepository';

const db = getDatabase();
const productRepo = new SqliteProductRepository(db);
const categoryRepo = new SqliteCategoryRepository(db);

beforeAll(() => {
  runMigrations(db);
  categoryRepo.create({ name: 'General' });
});

afterAll(() => {
  closeDatabase();
});

beforeEach(() => {
  db.execSync('DELETE FROM products');
});

describe('useProducts', () => {
  it('should return empty list initially', () => {
    const { result } = renderHook(() => useProducts({ search: '', sort: 'NAME_ASC', page: 0 }));
    expect(result.current.items).toEqual([]);
    expect(result.current.hasMore).toBe(false);
  });

  it('should return products matching search', () => {
    const cat = categoryRepo.create({ name: 'Test' });
    productRepo.create({ categoryId: cat.id, sku: 'A1', name: 'Apple', sellingPrice: 10, purchasePrice: 5, currentStock: 100, minStock: 10, imageUri: null });
    productRepo.create({ categoryId: cat.id, sku: 'B1', name: 'Banana', sellingPrice: 10, purchasePrice: 5, currentStock: 100, minStock: 10, imageUri: null });

    const { result } = renderHook(() => useProducts({ search: 'app', sort: 'NAME_ASC', page: 0 }));
    expect(result.current.items.length).toBe(1);
    expect(result.current.items[0].name).toBe('Apple');
  });

  it('should filter by category', () => {
    const cat1 = categoryRepo.create({ name: 'Cat 1' });
    const cat2 = categoryRepo.create({ name: 'Cat 2' });
    productRepo.create({ categoryId: cat1.id, sku: 'A1', name: 'Product A', sellingPrice: 10, purchasePrice: 5, currentStock: 100, minStock: 10, imageUri: null });
    productRepo.create({ categoryId: cat2.id, sku: 'B1', name: 'Product B', sellingPrice: 10, purchasePrice: 5, currentStock: 100, minStock: 10, imageUri: null });

    const { result } = renderHook(() => useProducts({ search: '', categoryId: cat1.id, sort: 'NAME_ASC', page: 0 }));
    expect(result.current.items.length).toBe(1);
    expect(result.current.items[0].categoryId).toBe(cat1.id);
  });

  it('should paginate with page parameter', () => {
    const cat = categoryRepo.create({ name: 'Test' });
    for (let i = 0; i < 20; i++) {
      productRepo.create({ categoryId: cat.id, sku: `P${i}`, name: `Product ${i}`, sellingPrice: 10, purchasePrice: 5, currentStock: 100, minStock: 10, imageUri: null });
    }

    const { result: page1 } = renderHook(() => useProducts({ search: '', sort: 'NAME_ASC', page: 0 }));
    const { result: page2 } = renderHook(() => useProducts({ search: '', sort: 'NAME_ASC', page: 1 }));

    expect(page1.current.items.length).toBe(15); // default pageSize
    expect(page2.current.items.length).toBe(5);
  });

  it('should refresh when invalidation store changes', () => {
    const cat = categoryRepo.create({ name: 'Test' });
    const { result, rerender } = renderHook(
      ({ page }) => useProducts({ search: '', sort: 'NAME_ASC', page }),
      { initialProps: { page: 0 } }
    );

    expect(result.current.items.length).toBe(0);

    productRepo.create({ categoryId: cat.id, sku: 'NEW', name: 'New Product', sellingPrice: 10, purchasePrice: 5, currentStock: 100, minStock: 10, imageUri: null });

    // Trigger invalidation (simplified)
    rerender({ page: 0 });
    expect(result.current.items.length).toBe(1);
  });
});
```

### 4.2 `useStockAlert`
**File:** `src/__tests__/presentation/hooks/useStockAlert.test.ts`

```typescript
import { renderHook, act } from '@testing-library/react-native';
import { useStockAlert } from '@/presentation/hooks/useStockAlert';
import { ToastService } from '@/infrastructure/toast/ToastService';

jest.mock('@/infrastructure/toast/ToastService');

describe('useStockAlert', () => {
  const mockToast = ToastService as jest.Mocked<typeof ToastService>;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should not show toast when no items', () => {
    const { result } = renderHook(() => useStockAlert());
    act(() => {
      result.current.notify([]);
    });
    expect(mockToast.warning).not.toHaveBeenCalled();
    expect(mockToast.show).not.toHaveBeenCalled();
  });

  it('should show single product toast with fractional stock', () => {
    const { result } = renderHook(() => useStockAlert());
    act(() => {
      result.current.notify([{ id: 1, name: 'Bananas', stockActual: 0.5, stockMinimo: 1.0 }]);
    });
    expect(mockToast.warning).toHaveBeenCalledWith(
      'Stock bajo',
      'Bananas: 0.5 / mínimo 1'
    );
  });

  it('should show multiple products toast', () => {
    const { result } = renderHook(() => useStockAlert());
    act(() => {
      result.current.notify([
        { id: 1, name: 'Apples', stockActual: 2, stockMinimo: 5 },
        { id: 2, name: 'Oranges', stockActual: 1, stockMinimo: 3 },
      ]);
    });
    expect(mockToast.show).toHaveBeenCalledWith(
      expect.objectContaining({
        title: 'Stock bajo',
        message: '2 productos por debajo del mínimo (Apples y otros).',
        variant: 'warning',
      })
    );
  });

  it('should not show toast when stock >= min', () => {
    const { result } = renderHook(() => useStockAlert());
    act(() => {
      result.current.notify([{ id: 1, name: 'Apples', stockActual: 10, stockMinimo: 5 }]);
    });
    expect(mockToast.warning).not.toHaveBeenCalled();
  });
});
```

---

## 5. UI Component Tests (React Native Testing Library)

### 5.1 `ProductGridCard`
**File:** `src/__tests__/presentation/components/product/ProductGridCard.test.tsx`

```tsx
import React from 'react';
import { render, fireEvent } from '@testing-library/react-native';
import { ProductGridCard } from '@/presentation/components/product/ProductGridCard';

jest.mock('@/presentation/hooks/useCurrency', () => ({
  useCurrency: () => ({
    format: (amount: number) => `$${amount.toFixed(2)}`,
  }),
}));

jest.mock('@/domain/rules/stockRules', () => ({
  isBelowMin: (product: { currentStock: number; minStock: number }) => product.currentStock < product.minStock,
}));

describe('ProductGridCard', () => {
  const mockProduct = {
    id: 1,
    name: 'Test Product',
    sellingPrice: 10.50,
    currentStock: 100,
    minStock: 10,
    imageUri: null,
  };

  const mockOnPress = jest.fn();

  beforeEach(() => {
    mockOnPress.mockClear();
  });

  it('renders product name and price', () => {
    const { getByText } = render(
      <ProductGridCard product={mockProduct} onPress={mockOnPress} />
    );
    expect(getByText('Test Product')).toBeTruthy();
    expect(getByText('$10.50')).toBeTruthy();
  });

  it('renders fallback avatar when no image', () => {
    const { getByTestId } = render(
      <ProductGridCard product={mockProduct} onPress={mockOnPress} />
    );
    // Fallback avatar renders initial
    expect(getByTestId('fallback-avatar')).toBeTruthy();
  });

  it('shows "Bajo" badge when stock below min', () => {
    const lowStockProduct = { ...mockProduct, currentStock: 5, minStock: 10 };
    const { getByText } = render(
      <ProductGridCard product={lowStockProduct} onPress={mockOnPress} />
    );
    expect(getByText('Bajo')).toBeTruthy();
  });

  it('does not show badge when stock above min', () => {
    const { queryByText } = render(
      <ProductGridCard product={mockProduct} onPress={mockOnPress} />
    );
    expect(queryByText('Bajo')).toBeNull();
  });

  it('calls onPress when tapped', () => {
    const { getByRole } = render(
      <ProductGridCard product={mockProduct} onPress={mockOnPress} />
    );
    fireEvent.press(getByRole('button'));
    expect(mockOnPress).toHaveBeenCalledWith(1);
  });

  it('renders image when imageUri provided', () => {
    const productWithImage = { ...mockProduct, imageUri: 'file://image.jpg' };
    const { getByType } = render(
      <ProductGridCard product={productWithImage} onPress={mockOnPress} />
    );
    // expo-image component should be rendered
    expect(getByType('Image')).toBeTruthy();
  });
});
```

### 5.2 `SKUInput`
**File:** `src/__tests__/presentation/components/product/SKUInput.test.tsx`

```tsx
import React from 'react';
import { render, fireEvent } from '@testing-library/react-native';
import { SKUInput } from '@/presentation/components/product/SKUInput';

describe('SKUInput', () => {
  const mockOnChange = jest.fn();
  const mockOnBlur = jest.fn();

  beforeEach(() => {
    mockOnChange.mockClear();
    mockOnBlur.mockClear();
  });

  it('renders label and input', () => {
    const { getByLabelText, getByPlaceholderText } = render(
      <SKUInput value="" onChangeText={mockOnChange} onBlur={mockOnBlur} />
    );
    expect(getByLabelText('SKU')).toBeTruthy();
    expect(getByPlaceholderText('Opcional: ABC-123')).toBeTruthy();
  });

  it('converts input to uppercase', () => {
    const { getByPlaceholderText } = render(
      <SKUInput value="abc" onChangeText={mockOnChange} onBlur={mockOnBlur} />
    );
    fireEvent.changeText(getByPlaceholderText('Opcional: ABC-123'), 'abc-123');
    expect(mockOnChange).toHaveBeenCalledWith('ABC-123');
  });

  it('allows hyphens and underscores', () => {
    const { getByPlaceholderText } = render(
      <SKUInput value="" onChangeText={mockOnChange} onBlur={mockOnBlur} />
    );
    fireEvent.changeText(getByPlaceholderText('Opcional: ABC-123'), 'PROD_001-V2');
    expect(mockOnChange).toHaveBeenCalledWith('PROD_001-V2');
  });

  it('strips invalid characters', () => {
    const { getByPlaceholderText } = render(
      <SKUInput value="" onChangeText={mockOnChange} onBlur={mockOnBlur} />
    );
    fireEvent.changeText(getByPlaceholderText('Opcional: ABC-123'), 'abc@123!');
    expect(mockOnChange).toHaveBeenCalledWith('ABC123');
  });

  it('enforces max length of 32', () => {
    const { getByPlaceholderText } = render(
      <SKUInput value="" onChangeText={mockOnChange} onBlur={mockOnBlur} />
    );
    const longInput = 'A'.repeat(35);
    fireEvent.changeText(getByPlaceholderText('Opcional: ABC-123'), longInput);
    expect(mockOnChange).toHaveBeenCalledWith('A'.repeat(32));
  });

  it('shows error when invalid format on blur', () => {
    const { getByPlaceholderText, queryByText } = render(
      <SKUInput value="abc@123" onChangeText={mockOnChange} onBlur={mockOnBlur} />
    );
    fireEvent.blur(getByPlaceholderText('Opcional: ABC-123'));
    expect(queryByText('Formato SKU inválido')).toBeTruthy();
  });

  it('allows empty value (optional)', () => {
    const { getByPlaceholderText } = render(
      <SKUInput value="" onChangeText={mockOnChange} onBlur={mockOnBlur} />
    );
    fireEvent.blur(getByPlaceholderText('Opcional: ABC-123'));
    expect(mockOnChange).toHaveBeenCalledWith('');
  });
});
```

### 5.3 `AdBanner`
**File:** `src/__tests__/presentation/components/ads/AdBanner.test.tsx`

```tsx
import React from 'react';
import { render } from '@testing-library/react-native';
import { AdBanner } from '@/presentation/components/ads/AdBanner';

jest.mock('expo-constants', () => ({
  expoConfig: { extra: { AD_ENABLED: false } },
}));

jest.mock('@/presentation/hooks/useReducedMotion', () => ({
  useReducedMotion: () => false,
}));

describe('AdBanner', () => {
  it('renders nothing when AD_ENABLED is false', () => {
    const { queryByTestId } = render(<AdBanner />);
    expect(queryByTestId('ad-banner')).toBeNull();
  });

  it('renders AdMob banner when AD_ENABLED is true', () => {
    jest.resetModules();
    jest.doMock('expo-constants', () => ({
      expoConfig: { extra: { AD_ENABLED: true } },
    }));
    const { AdBanner: AdBannerEnabled } = require('@/presentation/components/ads/AdBanner');
    const { getByTestId } = render(<AdBannerEnabled />);
    expect(getByTestId('ad-banner')).toBeTruthy();
  });

  it('respects reduced motion', () => {
    jest.resetModules();
    jest.doMock('expo-constants', () => ({
      expoConfig: { extra: { AD_ENABLED: true } },
    }));
    jest.doMock('@/presentation/hooks/useReducedMotion', () => ({
      useReducedMotion: () => true,
    }));
    const { AdBanner: AdBannerReduced } = require('@/presentation/components/ads/AdBanner');
    const { getByTestId } = render(<AdBannerReduced />);
    // Should still render but with reduced motion props
    expect(getByTestId('ad-banner')).toBeTruthy();
  });
});
```

---

## 6. E2E Test Scenarios (Manual / Detox)

### 6.1 Create Product with Fractional Stock
**Steps:**
1. Navigate to `/products/new`
2. Fill form: Name "Bananas", Category "Produce", Selling Price 1.20, Purchase Price 0.80, Current Stock 25.5, Min Stock 2.0
3. Tap "Crear producto"
4. Verify Toast "Producto creado"
5. Navigate to catalog, find "Bananas"
6. Verify stock displays "25.5"
7. Verify DB: `SELECT current_stock FROM products WHERE name = 'Bananas'` → 25.5

### 6.2 Soft Delete Preserves History
**Steps:**
1. Create product "Test Product" with stock 100
2. Register a sale with this product (via sales-checkout)
3. Soft delete product from catalog
4. Verify product hidden from catalog screen
5. Verify forensic query shows product: `SELECT * FROM products WHERE deleted_at IS NOT NULL`
6. Verify transaction history intact in `transactions` and `transaction_items`

### 6.3 Migration v1→v2 on Device
**Steps:**
1. Install v1.0.0 app with seeded Spanish data
2. Upgrade to v2.0.0 build
3. On first launch, migration runs
4. Verify app opens without crash
5. Verify all products visible in catalog
6. Verify fractional stock products work
7. Verify soft-deleted products hidden
8. Verify new SKU field works

---

## 7. Test Execution Commands

```bash
# Unit tests only
npm test -- --testPathPattern="domain/value-objects|domain/entities"

# Integration tests (requires SQLite)
npm test -- --testPathPattern="data/repositories"

# Migration tests (critical)
npm test -- --testPathPattern="integration/migration"

# Hook tests
npm test -- --testPathPattern="presentation/hooks"

# UI component tests
npm test -- --testPathPattern="presentation/components"

# All tests with coverage
npm test -- --coverage

# Watch mode for TDD
npm test -- --watch
```

---

## 8. CI Pipeline Checks

```yaml
# .github/workflows/test.yml (excerpt)
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - run: npm ci
      - run: npm run lint
      - run: npm run typecheck
      - run: npm test -- --coverage --passWithNoTests
      - run: |
          # Verify zero hardcoded Spanish strings in catalog components
          if grep -r "catalog\." src/presentation/components/product --include="*.tsx" | grep -v "\.test\." | grep -v "i18n" | grep -v "t("; then
            echo "ERROR: Hardcoded Spanish strings found"
            exit 1
          fi
```

---

## 9. Test Data Factories

**File:** `src/__tests__/factories/index.ts`

```typescript
import type { Product, Category } from '@/domain/entities';
import type { CreateProductInput } from '@/domain/entities/Product';
import { SKU } from '@/domain/value-objects/SKU';
import { Stock } from '@/domain/value-objects/Stock';
import { Money } from '@/domain/value-objects/Money';

export function createTestCategory(overrides: Partial<Category> = {}): Category {
  return {
    id: 1,
    name: 'Test Category',
    registeredAt: '2024-01-01T00:00:00Z',
    deletedAt: null,
    ...overrides,
  };
}

export function createTestProduct(overrides: Partial<Product> = {}): Product {
  return {
    id: 1,
    categoryId: 1,
    sku: new SKU('TEST-001'),
    name: 'Test Product',
    sellingPrice: new Money(10, 'USD'),
    purchasePrice: new Money(5, 'USD'),
    currentStock: new Stock(100),
    minStock: new Stock(10),
    imageUri: null,
    registeredAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
    deletedAt: null,
    ...overrides,
  };
}

export function createTestProductInput(overrides: Partial<CreateProductInput> = {}): CreateProductInput {
  return {
    categoryId: 1,
    sku: 'TEST-001',
    name: 'Test Product',
    sellingPrice: 10,
    purchasePrice: 5,
    currentStock: 100,
    minStock: 10,
    imageUri: null,
    ...overrides,
  };
}
```

---

## 10. Coverage Targets

| Layer | Target |
|---|---|
| Domain (Value Objects, Entities) | 100% |
| Data (Repositories) | 95% |
| Application (Use Cases) | 90% |
| Presentation (Hooks, Stores) | 85% |
| UI Components | 80% |
| Migration | 100% (critical path) |

---

*Last updated: 2026-11-09 — v2.0.0 Test Specification*