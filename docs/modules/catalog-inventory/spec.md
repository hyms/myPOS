# Catalog & Inventory — Specification (v2.0.0)

## 1. Bounded Context

Responsible for the lifecycle of products, categories, stock levels, low-stock alerts, and local image optimization. This module is the master data source that feeds `sales-checkout`, `expenses-cashflow`, and `financial-accumulators`.

---

## 2. Domain Entities & Value Objects

All identifiers, fields, and types in code and database are in **English**. Only visible UI strings remain in Spanish via i18n keys.

### 2.1 Product (Aggregate Root)

**File:** `src/domain/entities/Product.ts`

```ts
import type { CategoryId } from './Category';
import type { SKU } from '../value-objects/SKU';
import type { Stock } from '../value-objects/Stock';
import type { Money } from '../value-objects/Money';

export type ProductId = number;

export interface Product {
  readonly id: ProductId;
  readonly categoryId: CategoryId;
  readonly sku: SKU | null;
  readonly name: string;
  readonly sellingPrice: Money;
  readonly purchasePrice: Money;
  readonly currentStock: Stock;
  readonly minStock: Stock;
  readonly imageUri: string | null;
  readonly registeredAt: string;   // ISO 8601
  readonly updatedAt: string;      // ISO 8601
  readonly deletedAt: string | null;
}

export type ProductSummary = Pick<
  Product,
  'id' | 'name' | 'sellingPrice' | 'purchasePrice' | 'currentStock' | 'imageUri' | 'categoryId' | 'sku'
>;

// DTOs for use cases (application/infrastructure layers)
export type CreateProductInput = {
  readonly categoryId: CategoryId;
  readonly sku: string | null;
  readonly name: string;
  readonly sellingPrice: number;     // amount in base currency
  readonly purchasePrice: number;
  readonly currentStock: number;     // fractional, >= 0
  readonly minStock: number;         // fractional, >= 0
  readonly imageUri: string | null;
};

export type UpdateProductInput = CreateProductInput;
```

### 2.2 Category (Aggregate Root)

**File:** `src/domain/entities/Category.ts`

```ts
export type CategoryId = number;

export interface Category {
  readonly id: CategoryId;
  readonly name: string;
  readonly registeredAt: string;
  readonly deletedAt: string | null;
}

export type CreateCategoryInput = {
  readonly name: string;
};
```

### 2.3 Value Objects (v2.0.0 additions)

#### 2.3.1 SKU
**File:** `src/domain/value-objects/SKU.ts`

```ts
export class SKU {
  readonly value: string;  // alphanumeric, max 32 chars, unique per product

  constructor(value: string) {
    const trimmed = value.trim().toUpperCase();
    if (trimmed.length === 0) throw new Error('SKU cannot be empty');
    if (trimmed.length > 32) throw new Error('SKU max length is 32');
    if (!/^[A-Z0-9\-_]+$/.test(trimmed)) {
      throw new Error('SKU must be alphanumeric with hyphen/underscore only');
    }
    this.value = trimmed;
  }

  equals(other: SKU): boolean {
    return this.value === other.value;
  }

  toString(): string {
    return this.value;
  }
}
```

#### 2.3.2 Stock
**File:** `src/domain/value-objects/Stock.ts`

```ts
export class Stock {
  readonly value: number;  // non-negative decimal, up to 3 decimal places

  constructor(value: number) {
    const rounded = Math.round(value * 1000) / 1000;
    if (!Number.isFinite(rounded) || rounded < 0) {
      throw new Error('Stock must be a non-negative number with up to 3 decimal places');
    }
    this.value = rounded;
  }

  static zero(): Stock {
    return new Stock(0);
  }

  add(other: Stock): Stock {
    return new Stock(this.value + other.value);
  }

  subtract(other: Stock): Stock {
    const result = this.value - other.value;
    if (result < 0) throw new Error('Stock cannot go negative');
    return new Stock(result);
  }

  isBelow(min: Stock): boolean {
    return this.value < min.value;
  }

  equals(other: Stock): boolean {
    return this.value === other.value;
  }

  toString(): string {
    return String(this.value);
  }
}
```

#### 2.3.3 Money (existing)
**File:** `src/domain/value-objects/Money.ts` — see `system-settings/spec.md` section 2.1.

#### 2.3.4 Currency (existing)
**File:** `src/domain/value-objects/Currency.ts` — see `system-settings/spec.md` section 2.1.

---

## 3. Domain Rules & Invariants

| ID | Rule | Description |
|---|---|---|
| RULE-CI-01 | Category required | `category_id` must exist and not be NULL when creating/editing a product. Validated in form, factory, and `SqliteProductRepository.create`/`update`. |
| RULE-CI-02 | Minimum stock | `isBelowMin(product)` returns `true` when `currentStock < minStock`. Fires non-obstructive Toast. |
| RULE-CI-03 | Fractional stock in UI | Quantities and stock captured as decimals (`keyboardType="decimal-pad"`, step `0.001`), rounded to 3dp on persist. Column `REAL` for forward-compat. |
| RULE-CI-04 | Soft delete | Deleted product sets `deleted_at = CURRENT_TIMESTAMP`. Active queries filter `WHERE deleted_at IS NULL`. |
| RULE-CI-05 | Persisted image | Only final URI in `documentDirectory/productos/<uuid>.jpg`. Never temp URI from manipulator. Target size < 40 KB. |
| RULE-CI-06 | Optional unique SKU | If `sku` provided, must be unique among active products (`deleted_at IS NULL`). Validated in repository. |
| RULE-CI-07 | Non-negative prices | `sellingPrice >= 0`, `purchasePrice >= 0`. Validated in factory and repository. |
| RULE-CI-08 | Name required | `name.trim().length > 0`. Validated in form and repository. |

Global invariants applicable: **INV-01, INV-05, INV-07, INV-09, INV-10**.

---

## 4. Database Contract

### 4.1 Target Schema (v2.0.0)

```sql
CREATE TABLE IF NOT EXISTS categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS products (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    category_id INTEGER NOT NULL,
    sku TEXT UNIQUE,
    name TEXT NOT NULL,
    selling_price REAL NOT NULL,
    purchase_price REAL NOT NULL,
    current_stock REAL DEFAULT 0.0,
    min_stock REAL DEFAULT 0.0,
    image_uri TEXT,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL,
    FOREIGN KEY(category_id) REFERENCES categories(id)
);

-- Partial indexes (deleted_at IS NULL)
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_products_name ON products(name) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_products_sku ON products(sku) WHERE deleted_at IS NULL AND sku IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_products_deleted_at ON products(deleted_at) WHERE deleted_at IS NULL;
```

### 4.2 Column Mapping: v1.0.0 (Spanish) → v2.0.0 (English)

| v1.0.0 Column | v2.0.0 Column | Notes |
|---|---|---|
| `categorias.id` | `categories.id` | Table renamed |
| `categorias.nombre` | `categories.name` | |
| `categorias.fecha_registro` | `categories.registered_at` | |
| `categorias.fecha_delete` | `categories.deleted_at` | |
| `productos.id` | `products.id` | Table renamed |
| `productos.categoria_id` | `products.category_id` | Now `NOT NULL` |
| `productos.nombre` | `products.name` | |
| `productos.precio_venta` | `products.selling_price` | |
| `productos.precio_compra` | `products.purchase_price` | |
| `productos.stock_actual` | `products.current_stock` | Fractional support |
| `productos.stock_minimo` | `products.min_stock` | Fractional support |
| `productos.imagen_uri` | `products.image_uri` | |
| `productos.fecha_registro` | `products.registered_at` | |
| `productos.fecha_update` | `products.updated_at` | |
| `productos.fecha_delete` | `products.deleted_at` | |
| *(new)* | `products.sku` | `TEXT UNIQUE`, nullable |

### 4.3 Migration DDL (v1.0.0 → v2.0.0)

**File:** `src/data/database/migrations/v2_0_0_catalog_inventory_migration.sql.ts`
(`CATALOG_INVENTORY_MIGRATION_SQL`, executed by `runMigrations` inside the shared v2 transaction)

```sql
-- Atomic migration transaction (single db.withTransactionSync)
-- Step 1: Rename tables
ALTER TABLE categorias RENAME TO categories;
ALTER TABLE productos RENAME TO products;

-- Step 2: Rename columns in categories
ALTER TABLE categories RENAME COLUMN nombre TO name;
ALTER TABLE categories RENAME COLUMN fecha_registro TO registered_at;
ALTER TABLE categories RENAME COLUMN fecha_delete TO deleted_at;

-- Step 3: Rename columns in products
ALTER TABLE products RENAME COLUMN categoria_id TO category_id;
ALTER TABLE products RENAME COLUMN nombre TO name;
ALTER TABLE products RENAME COLUMN precio_venta TO selling_price;
ALTER TABLE products RENAME COLUMN precio_compra TO purchase_price;
ALTER TABLE products RENAME COLUMN stock_actual TO current_stock;
ALTER TABLE products RENAME COLUMN stock_minimo TO min_stock;
ALTER TABLE products RENAME COLUMN imagen_uri TO image_uri;
ALTER TABLE products RENAME COLUMN fecha_registro TO registered_at;
ALTER TABLE products RENAME COLUMN fecha_update TO updated_at;
ALTER TABLE products RENAME COLUMN fecha_delete TO deleted_at;

-- Step 4: Backfill NULL category_id
-- Assign the oldest existing category; create "General" only when products
-- have a NULL category and no category exists at all.
INSERT INTO categories (name)
SELECT 'General'
 WHERE EXISTS (SELECT 1 FROM products WHERE category_id IS NULL)
   AND NOT EXISTS (SELECT 1 FROM categories);

UPDATE products
   SET category_id = (SELECT MIN(id) FROM categories)
 WHERE category_id IS NULL;

-- Step 5: Enforce NOT NULL on category_id (SQLite requires table rebuild for NOT NULL)
-- We use a temp table approach for safety
CREATE TABLE products_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    category_id INTEGER NOT NULL,
    sku TEXT UNIQUE,
    name TEXT NOT NULL,
    selling_price REAL NOT NULL,
    purchase_price REAL NOT NULL,
    current_stock REAL DEFAULT 0.0,
    min_stock REAL DEFAULT 0.0,
    image_uri TEXT,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL,
    FOREIGN KEY(category_id) REFERENCES categories(id)
);

INSERT INTO products_new (id, category_id, sku, name, selling_price, purchase_price, current_stock, min_stock, image_uri, registered_at, updated_at, deleted_at)
SELECT id, category_id, NULL, name, selling_price, purchase_price, current_stock, min_stock, image_uri, registered_at, updated_at, deleted_at
FROM products;

DROP TABLE products;
ALTER TABLE products_new RENAME TO products;

-- Step 6: Recreate indexes
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_products_name ON products(name) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_products_sku ON products(sku) WHERE deleted_at IS NULL AND sku IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_products_deleted_at ON products(deleted_at) WHERE deleted_at IS NULL;

-- Step 7: user_version = 2 is set centrally by `runMigrations` after every
-- v2.0.0 module step succeeds inside the same transaction.
```

---

## 5. Repository Interfaces

### 5.1 IProductRepository
**File:** `src/data/repositories/IProductRepository.ts`

```ts
import type { CreateProductInput, Product, ProductId, UpdateProductInput } from '@/domain/entities/Product';
import type { CategoryId } from '@/domain/entities/Category';

export type SortOrder = 'NAME_ASC' | 'POPULARITY_DESC';

export interface ProductQuery {
  readonly search?: string;
  readonly categoryId?: CategoryId;
  readonly sort: SortOrder;
  readonly limit: number;
  readonly offset: number;
}

export interface IProductRepository {
  list(query: ProductQuery): ReadonlyArray<Product & { popularity: number }>;
  findById(id: ProductId): Product | null;
  findBySku(sku: string): Product | null;
  create(input: CreateProductInput): Product;
  update(id: ProductId, input: UpdateProductInput): Product;
  delete(id: ProductId): void;                    // soft delete
  findByIds(ids: ReadonlyArray<ProductId>): ReadonlyArray<Product>;
}
```

### 5.2 ICategoryRepository
**File:** `src/data/repositories/ICategoryRepository.ts`

```ts
import type { Category, CategoryId, CreateCategoryInput } from '@/domain/entities/Category';

export interface ICategoryRepository {
  list(): ReadonlyArray<Category>;
  findById(id: CategoryId): Category | null;
  create(input: CreateCategoryInput): Category;
}
```

---

## 6. Popularity Query (Sales)

```sql
SELECT p.*, COALESCE(SUM(d.quantity), 0) AS popularity
FROM products p
LEFT JOIN transaction_items d ON d.product_id = p.id
LEFT JOIN transactions t
  ON t.id = d.transaction_id
 AND t.type = 'SALE'
 AND t.deleted_at IS NULL
WHERE p.deleted_at IS NULL
  AND (? IS NULL OR p.category_id = ?)
  AND p.name LIKE ?
GROUP BY p.id
ORDER BY popularity DESC, p.name ASC
LIMIT ? OFFSET ?;
```

---

## 7. Use Cases (`ProductManagement`)

**File:** `src/application/products/ProductManagement.ts`

| Use Case | Signature | Atomic | Preconditions | Postconditions |
|---|---|---|---|---|
| `createProduct` | `(input: CreateProductInput) => Product` | Yes (repo txn) | category exists, SKU unique if provided | Product inserted, `registered_at` = now |
| `updateProduct` | `(id, input: UpdateProductInput) => Product` | Yes (repo txn) | product exists, SKU unique if changed | Product updated, `updated_at` = now |
| `deleteProduct` | `(id: ProductId) => void` | Yes (soft delete) | product exists | `deleted_at` = now, `updated_at` = now |

---

## 8. Image Compression Pipeline

**Files:**
- `src/infrastructure/image/ImageOptimizer.ts`
- `src/infrastructure/image/ImageStorage.ts`

### Technical Specification

| Parameter | Value |
|---|---|
| Target dimension | 400 × 400 px (fit: contain, maintains aspect ratio) |
| Format | JPEG |
| Quality | 70% (`compress: 0.7`) |
| Target size | < 40 KB |
| Destination directory | `${FileSystem.documentDirectory}productos/` |
| Filename | `<uuid>.jpg` (timestamp base36 + random) |
| URI stored in SQLite | Relative to `productos/` directory (e.g., `productos/abc123.jpg`) |

### Flow

```
User selects image (camera/gallery)
       │
       ▼
ImageManipulator.manipulateAsync(uri, [{ resize: { width: 400, height: 400 } }], { compress: 0.7, format: SaveFormat.JPEG })
       │
       ▼
ImageStorage.saveProductImage(optimizedUri)
       ├── Creates directory if not exists
       ├── Copies to documentDirectory/productos/<uuid>.jpg
       ├── Gets actual size and dimensions
       └── Returns { uri, width, height, size }
       │
       ▼
UI updates draft with final URI → Persists to SQLite on product save
```

### Post-Compression Validation

- If `size > 40_000` bytes → log warning (non-blocking, dev only).
- `expo-image` renders with `cachePolicy="memory-disk"`, `contentFit="cover"`.

---

## 9. Stock Alert Logic

**File:** `src/presentation/hooks/useStockAlert.ts`

### Trigger

Evaluated **after each successful commit** in:
- `RegisterSale` (sales-checkout)
- `RegisterPurchase` (sales-checkout)
- `updateProduct` (catalog-inventory)

### Rule

```
ALERT IF: currentStock < minStock
```

### UI Behavior

| Affected products | Toast |
|---|---|
| 1 | `"Stock bajo: {name} — {currentStock} / mínimo {minStock}"` |
| >1 | `"{N} productos por debajo del mínimo ({firstName} y otros)"` |

- Variant: `warning`
- Non-obstructive (auto-dismiss after 3.5s, configurable `STOCK_ALERT_TOAST_TIME`)
- Visual "Bajo" badge on `ProductGridCard` (red, top-left corner)

---

## 10. UI State Machines (Spanish visible text)

### 10.1 Catalog Screen (`app/(tabs)/products.tsx`)

| State | Trigger | Next State | Visible Text (ES) |
|---|---|---|---|
| `CatalogLoaded` | Pull-to-refresh | `Refreshing` | (spinner) |
| `CatalogLoaded` | Scroll to end + `hasMore` | `LoadingMore` | (footer loader) |
| `CatalogLoaded` | Search with results | `Filtered` | (grid updated) |
| `CatalogLoaded` | Search no results | `Empty` | `"No se encontraron productos"` + "Crear primer producto" button |
| `CatalogLoaded` | Tap FAB (+) | `NewProductForm` | Navigates to `/products/new` |
| `CatalogLoaded` | Tap product card | `EditProductForm` | Navigates to `/products/{id}` |

### 10.2 Product Form (`app/products/new.tsx`, `app/products/[id].tsx`)

| State | Trigger | Next State | Visible Text (ES) |
|---|---|---|---|
| `FormEmpty` | User opens form | `Editing` | `"Nuevo producto"` / `"Editar producto"` (nav title) |
| `Editing` | User taps [+] category | `QuickCategoryModal` | `"Crear categoría"` (modal sheet) |
| `QuickCategoryModal` | Category saved | `Editing` | Category selected in `CategorySelector`; draft preserved |
| `Editing` | User taps Camera/Gallery | `ImagePicker` | Native selection system |
| `ImagePicker` | Image optimized | `Editing` | Preview 400×400 in `ProductFormHeader` |
| `Editing` | User saves (valid) | `Catalog` | `"Producto guardado"` (Toast success) + navigate back |
| `Editing` | User saves (invalid) | `Editing` | Toast error + focus invalid field |
| `Editing` | User deletes (confirmed) | `Catalog` | `"Producto eliminado"` (Toast success) + navigate back |

### 10.3 Stock Alert (cross-cutting)

| State | Trigger | Next State | Visible Text (ES) |
|---|---|---|---|
| `Normal` | Post-sale/purchase/edit → `currentStock < minStock` | `StockAlert` | Toast warning (see section 9) |

---

## 11. UI Components

| Component | File | Type | Memoized | Description |
|---|---|---|---|---|
| `ProductGridCard` | `src/presentation/components/product/ProductGridCard.tsx` | UI | Yes (`React.memo`) | 3-col card: image/avatar, name, price, stock, "Bajo" badge |
| `ProductFallbackAvatar` | `src/presentation/components/product/ProductFallbackAvatar.tsx` | UI | Yes | Name initial on deterministic color (`colorForId`) |
| `ProductFormHeader` | `src/presentation/components/product/ProductFormHeader.tsx` | UI | No | **FIRST** in form: image picker (camera/gallery) + preview + remove button |
| `CategorySelector` | `src/presentation/components/product/CategorySelector.tsx` | UI | No | Horizontal chips + `[+]` button for `QuickCategoryModal` |
| `QuickCategoryModal` | `src/presentation/components/product/QuickCategoryModal.tsx` | UI | No | Native sheet: name input → creates category → invalidates cache → auto-selects |
| `SearchBar` | `src/presentation/components/filters/SearchBar.tsx` | UI | No | Debounce 200ms, placeholder "Buscar productos…" |
| `SortMenu` | `src/presentation/components/filters/SortMenu.tsx` | UI | No | Dropdown: "Nombre A-Z" / "Más vendidos" |
| `CategoryChipsRow` | `src/presentation/components/filters/CategoryChipsRow.tsx` | UI | No | Clean text chips (no colors), "Todas" + categories |
| `EmptyState` | `src/presentation/components/feedback/EmptyState.tsx` | UI | No | Illustration + title + description + action button |
| `AnimatedPressable` | `src/presentation/components/ui/AnimatedPressable.tsx` | UI | No | Reanimated scale/opacity wrapper for tactile feedback |
| `SKUInput` | `src/presentation/components/product/SKUInput.tsx` | UI | No | SKU input with format validation (uppercase, alphanumeric, hyphen/underscore) |
| `AdBanner` | `src/presentation/components/ads/AdBanner.tsx` | UI | No | Conditional AdMob banner governed by `AD_ENABLED` |

---

## 12. Hooks

| Hook | File | Responsibility |
|---|---|---|
| `useProducts` | `src/presentation/hooks/useProducts.ts` | Catalog with search, category, sort, pagination (FlashList) |
| `useCategories` | `src/presentation/hooks/useCategories.ts` | Active categories list + manual refresh |
| `useStockAlert` | `src/presentation/hooks/useStockAlert.ts` | Evaluates and shows Toast if `currentStock < minStock` |
| `useCurrency` | `src/presentation/hooks/useCurrency.ts` | `Intl.NumberFormat` formatter per `settingsStore.currency` |
| `useInfiniteScroll` | `src/presentation/hooks/useInfiniteScroll.ts` | `onEndReached` with threshold 0.5 for FlashList |
| `useDebouncedValue` | `src/presentation/hooks/useInfiniteScroll.ts` | Generic debounce (200ms for search) |

---

## 13. Zustand Stores

| Store | File | Responsibility |
|---|---|---|
| `productDraftStore` | `src/presentation/stores/productDraftStore.ts` | Product form draft (survives `QuickCategoryModal`); includes cached `categories` |
| `settingsStore` | `src/presentation/stores/settingsStore.ts` | `currency`, `pageSize` (default 15), `biometricEnabled` |
| `invalidationStore` | `src/presentation/stores/invalidationStore.ts` | Manual invalidation TanStack Query style (`products`, `categories`, etc.) |

---

## 14. AdWare Governance

**Rule:** Banner ads **ARE allowed** on the Inventory screen (`app/(tabs)/products.tsx`).

**Implementation:**
- Check `AD_ENABLED` flag (configurable via remote config or build-time).
- If `true`, render `AdBanner` (AdMob wrapper) in FlashList `ListFooterComponent`, **below** products and **above** FAB.
- Do not interrupt scroll or card tactile feedback.
- Respect `useReducedMotion` for banner animations.

---

## 15. Applicable NFRs

- **NFR-01:** Offline-first SQLite JSI (`expo-sqlite`, `openDatabaseSync`, `withTransactionSync`).
- **NFR-02:** FlashList `numColumns={3}`, sub-50ms render, no `estimatedItemSize`.
- **NFR-04:** Pagination `LIMIT ? OFFSET ?` with `pageSize` from settings (default 15).
- **NFR-05:** Image 400×400, JPEG 70%, target < 40 KB.
- **NFR-06:** Strict dark mode + runtime design tokens (NativeWind v4).
- **NFR-07:** Atomic rollback via `withTransactionSync` on write use cases.
- **NFR-08:** UI strings in Spanish; code/DB/identifiers in English.
- **NFR-09:** Strict TypeScript (`strict`, `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes`).
- **NFR-10:** Tests: 100% pass unit + integration + stock/cash integrity.

---

## 16. Extension Points (v2.0.0 Roadmap)

- [ ] **SKU/Barcode scanning**: `expo-camera` + `expo-barcode-scanner` integration for quick capture in `ProductFormHeader`.
- [ ] **Restock via purchase**: Guided "Buy low stock" flow pre-populating purchase cart with alerted products.
- [ ] **Stock history**: `stock_movements` table (type: `IN`/`OUT`/`ADJUSTMENT`, `quantity`, `source_transaction_id`).
- [ ] **Product variants**: Size/color/flavor as sub-entities under `Product`.
- [ ] **Bulk import**: CSV/Excel → validation → bulk `createProduct` transaction.
- [ ] **Catalog export**: CSV/PDF with current filters (search, category, sort).
- [ ] **Full-text search index**: FTS5 on `products(name)` for richer searches.
- [ ] **AdMob integration**: Implement conditional `AD_ENABLED` banner in `ListFooterComponent`.
- [ ] **DB migration v2**: Verified zero-data-loss migration from v1.0.0 Spanish schema.

---

## 17. i18n Keys (Spanish UI Dictionary)

| Key | Text (ES) | Context |
|---|---|---|
| `catalog.title` | Productos | Main screen title |
| `catalog.empty.title` | Sin productos | EmptyState |
| `catalog.empty.desc` | Crea tu primer producto para empezar a vender. | EmptyState |
| `catalog.empty.action` | Crear primer producto | EmptyState button |
| `catalog.fab.label` | Crear producto | FAB accessibility |
| `catalog.search.placeholder` | Buscar productos… | SearchBar |
| `catalog.sort.label` | Ordenar | SortMenu |
| `catalog.sort.name_asc` | Nombre A-Z | Sort option |
| `catalog.sort.popularity_desc` | Más vendidos | Sort option |
| `catalog.category.all` | Todas | CategoryChipsRow |
| `catalog.category.new` | Nueva | [+] button CategorySelector |
| `catalog.form.title.new` | Nuevo producto | Nav title create |
| `catalog.form.title.edit` | Editar producto | Nav title edit |
| `catalog.form.header.camera` | Cámara | ProductFormHeader button |
| `catalog.form.header.gallery` | Galería | ProductFormHeader button |
| `catalog.form.header.remove` | Quitar | ProductFormHeader button |
| `catalog.form.label.name` | Nombre | Input |
| `catalog.form.label.selling_price` | Precio de venta | Input |
| `catalog.form.label.purchase_price` | Precio de compra | Input |
| `catalog.form.label.current_stock` | Stock actual | Input |
| `catalog.form.label.min_stock` | Stock mínimo | Input |
| `catalog.form.helper.min_stock` | Alerta no obstructiva al pasar este umbral. | Helper min stock |
| `catalog.form.category.label` | Categoría | CategorySelector |
| `catalog.form.category.required` | Categoría obligatoria | Validation error |
| `catalog.form.category.select_hint` | Selecciona una categoría obligatoria. | Hint CategorySelector |
| `catalog.modal.category.title` | Nueva categoría | QuickCategoryModal |
| `catalog.modal.category.label` | Nombre | Modal input |
| `catalog.modal.category.placeholder` | Ej. Bebidas | Modal placeholder |
| `catalog.btn.create` | Crear producto | Submit button new |
| `catalog.btn.save` | Guardar cambios | Submit button edit |
| `catalog.btn.delete` | Eliminar producto | Danger button |
| `catalog.confirm.delete.title` | Eliminar producto | ConfirmDialog |
| `catalog.confirm.delete.msg` | ¿Eliminar este producto? Se ocultará del catálogo pero se conservarán las ventas y compras históricas. | ConfirmDialog |
| `catalog.toast.created` | Producto creado | Toast success |
| `catalog.toast.updated` | Producto actualizado | Toast success |
| `catalog.toast.deleted` | Producto eliminado | Toast success |
| `catalog.toast.error` | No se pudo guardar. | Toast generic error |
| `catalog.alert.low_stock.single` | Stock bajo: {name} — {actual} / mínimo {min} | Toast warning 1 product |
| `catalog.alert.low_stock.multiple` | {count} productos por debajo del mínimo ({first} y otros) | Toast warning multiple |
| `catalog.image.perm_denied` | No se puede acceder a la fuente de imagen. | Toast permission error |
| `catalog.image.process_error` | No se pudo procesar la imagen. | Toast image error |

---

## 18. Cross-References

- `sales-checkout/spec.md` — Section 7 (RegisterSale), 8 (UI State Machines), 12 (NFR-02, NFR-07)
- `financial-accumulators/spec.md` — Section 6.3 (RevertTransaction stock math), 13 (NFR-03, NFR-07)
- `system-settings/spec.md` — Section 2.1 (Currency/Money), 3 (RULE-SS-05, RULE-SS-06), 10 (useCurrency, useSettingsPageSize)
- `expenses-cashflow/spec.md` — Section 3 (RULE-EC-03: EXPENSE without product items)

---

*Last updated: 2026-11-09 — v2.0.0 Specification*