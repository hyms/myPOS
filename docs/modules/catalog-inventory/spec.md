# Catalog & Inventory — Specification (v2.0.0 KMP)

## 1. Bounded Context

Responsible for the lifecycle of products, categories, stock levels, low-stock alerts, and local image optimization. This module is the master data source that feeds `sales-checkout`, `expenses-cashflow`, and `financial-accumulators`.

---

## 2. Domain Entities & Value Objects

All identifiers, fields, and types in code and database are in **English**. Only visible UI strings remain in Spanish via i18n keys (`composeResources/values-es/strings.xml`).

### 2.1 Product (Aggregate Root)

**File:** `commonMain/domain/entities/Product.kt`

```kotlin
package com.tpvmovil.domain.entities

import com.tpvmovil.domain.valueobjects.*

@JvmInline
value class ProductId(val value: Long) : Comparable<ProductId> {
    override fun compareTo(other: ProductId) = value.compareTo(other.value)
}

data class Product(
    val id: ProductId,
    val categoryId: CategoryId,
    val sku: SKU?,
    val name: String,
    val sellingPrice: Money,
    val purchasePrice: Money,
    val currentStock: Stock,
    val minStock: Stock,
    val imageUri: String?,
    val registeredAt: String,   // ISO 8601
    val updatedAt: String,      // ISO 8601
    val deletedAt: String?
) {
    companion object {
        fun create(input: CreateProductInput): Product = TODO()
    }
}

data class ProductSummary(
    val id: ProductId,
    val name: String,
    val sellingPrice: Money,
    val purchasePrice: Money,
    val currentStock: Stock,
    val imageUri: String?,
    val categoryId: CategoryId,
    val sku: SKU?
)

// DTOs for use cases (application layer)
data class CreateProductInput(
    val categoryId: CategoryId,
    val sku: String?,           // validated to SKU in factory
    val name: String,
    val sellingPrice: Double,   // amount in base currency
    val purchasePrice: Double,
    val currentStock: Double,   // fractional, >= 0
    val minStock: Double,       // fractional, >= 0
    val imageUri: String?
)

typealias UpdateProductInput = CreateProductInput
```

### 2.2 Category (Aggregate Root)

**File:** `commonMain/domain/entities/Category.kt`

```kotlin
package com.tpvmovil.domain.entities

@JvmInline
value class CategoryId(val value: Long) : Comparable<CategoryId> {
    override fun compareTo(other: CategoryId) = value.compareTo(other.value)
}

data class Category(
    val id: CategoryId,
    val name: String,
    val registeredAt: String,
    val deletedAt: String?
) {
    companion object {
        fun create(input: CreateCategoryInput): Category = TODO()
    }
}

data class CreateCategoryInput(val name: String)
```

### 2.3 Value Objects (v2.0.0 additions)

#### 2.3.1 SKU
**File:** `commonMain/domain/valueobjects/SKU.kt`

```kotlin
package com.tpvmovil.domain.valueobjects

@JvmInline
value class SKU private constructor(val value: String) {
    companion object {
        fun create(value: String): SKU {
            val trimmed = value.trim().uppercase()
            require(trimmed.isNotBlank()) { "SKU cannot be empty" }
            require(trimmed.length <= 32) { "SKU max length is 32" }
            require(trimmed.matches(Regex("^[A-Z0-9\\-_]+$"))) { 
                "SKU must be alphanumeric with hyphen/underscore only" 
            }
            return SKU(trimmed)
        }
    }

    override fun toString(): String = value
}
```

#### 2.3.2 Stock
**File:** `commonMain/domain/valueobjects/Stock.kt`

```kotlin
package com.tpvmovil.domain.valueobjects

@JvmInline
value class Stock private constructor(val value: Double) {
    companion object {
        fun create(value: Double): Stock {
            val rounded = (value * 1000).roundToInt() / 1000.0
            require(rounded.isFinite() && rounded >= 0) { 
                "Stock must be a non-negative number with up to 3 decimal places" 
            }
            return Stock(rounded)
        }

        fun zero(): Stock = Stock(0.0)
    }

    fun add(other: Stock): Stock = Stock.create(this.value + other.value)
    fun subtract(other: Stock): Stock {
        val result = this.value - other.value
        require(result >= 0) { "Stock cannot go negative" }
        return Stock.create(result)
    }

    fun isBelow(min: Stock): Boolean = this.value < min.value

    override fun toString(): String = value.toString()
}
```

#### 2.3.3 Money / Currency (Shared Kernel)
**Files:** `commonMain/domain/valueobjects/Money.kt`, `Currency.kt` — see `system-settings/spec.md` section 2.1.

---

## 3. Domain Rules & Invariants

| ID | Rule | Description |
|---|---|---|
| RULE-CI-01 | Category required | `categoryId` must exist and not be NULL when creating/editing a product. Validated in form, factory, and `SqlDelightProductRepository.create`/`update`. |
| RULE-CI-02 | Minimum stock | `isBelowMin(product)` returns `true` when `currentStock < minStock`. Fires non-obstructive Toast/Snackbar. |
| RULE-CI-03 | Fractional stock in UI | Quantities and stock captured as decimals (`KeyboardType.Decimal`, step `0.001`), rounded to 3dp on persist. Column `REAL` for forward-compat. |
| RULE-CI-04 | Soft delete | Deleted product sets `deleted_at = CURRENT_TIMESTAMP`. Active queries filter `WHERE deleted_at IS NULL`. |
| RULE-CI-05 | Persisted image | Only final URI in `documentDirectory/productos/<uuid>.jpg`. Never temp URI from manipulator. Target size < 40 KB. |
| RULE-CI-06 | Optional unique SKU | If `sku` provided, must be unique among active products (`deleted_at IS NULL`). Validated in repository. |
| RULE-CI-07 | Non-negative prices | `sellingPrice >= 0`, `purchasePrice >= 0`. Validated in factory and repository. |
| RULE-CI-08 | Name required | `name.trim().isNotEmpty()`. Validated in form and repository. |

Global invariants applicable: **INV-01, INV-05, INV-07, INV-09, INV-10**.

---

## 4. Database Contract (SQLDelight)

### 4.1 Target Schema (v2.0.0)

**File:** `commonMain/sqldelight/schema.sq` (catalog section)

```sql
CREATE TABLE categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    registered_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
    deleted_at INTEGER
);

CREATE TABLE products (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    category_id INTEGER NOT NULL,
    sku TEXT UNIQUE,
    name TEXT NOT NULL,
    selling_price REAL NOT NULL,
    purchase_price REAL NOT NULL,
    current_stock REAL DEFAULT 0.0,
    min_stock REAL DEFAULT 0.0,
    image_uri TEXT,
    registered_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
    updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
    deleted_at INTEGER,
    FOREIGN KEY(category_id) REFERENCES categories(id)
);

-- Partial indexes (deleted_at IS NULL)
CREATE INDEX idx_products_category ON products(category_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_products_name ON products(name) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX idx_products_sku ON products(sku) WHERE deleted_at IS NULL AND sku IS NOT NULL;
CREATE INDEX idx_products_deleted_at ON products(deleted_at) WHERE deleted_at IS NULL;
```

### 4.2 Column Mapping: v1.0.0 (Spanish) → v2.0.0 (English)

| v1.0.0 Column | v2.0.0 Column | Notes |
|---|---|---|
| `categorias.id` | `categories.id` | Table renamed |
| `categorias.nombre` | `categories.name` | |
| `categorias.fecha_registro` | `categories.registered_at` | Stored as epoch millis |
| `categorias.fecha_delete` | `categories.deleted_at` | Stored as epoch millis |
| `productos.id` | `products.id` | Table renamed |
| `productos.categoria_id` | `products.category_id` | Now `NOT NULL` |
| `productos.nombre` | `products.name` | |
| `productos.precio_venta` | `products.selling_price` | |
| `productos.precio_compra` | `products.purchase_price` | |
| `productos.stock_actual` | `products.current_stock` | Fractional support |
| `productos.stock_minimo` | `products.min_stock` | Fractional support |
| `productos.imagen_uri` | `products.image_uri` | |
| `productos.fecha_registro` | `products.registered_at` | Stored as epoch millis |
| `productos.fecha_update` | `products.updated_at` | Stored as epoch millis |
| `productos.fecha_delete` | `products.deleted_at` | Stored as epoch millis |
| *(new)* | `products.sku` | `TEXT UNIQUE`, nullable |

### 4.3 Migration DDL (v1.0.0 → v2.0.0)

**File:** `commonMain/sqldelight/migrations/v2_catalog_inventory.sqm`

```sql
-- v2_catalog_inventory.sqm
-- Atomic migration transaction (single transaction block)

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
INSERT INTO categories (name, registered_at)
SELECT 'General', strftime('%s', 'now') * 1000
WHERE EXISTS (SELECT 1 FROM products WHERE category_id IS NULL)
  AND NOT EXISTS (SELECT 1 FROM categories);

UPDATE products
   SET category_id = (SELECT MIN(id) FROM categories)
 WHERE category_id IS NULL;

-- Step 5: Enforce NOT NULL on category_id (SQLite requires table rebuild)
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
    registered_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
    updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
    deleted_at INTEGER,
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

-- Step 7: user_version = 2 is set centrally by DatabaseDriverFactory after every
-- v2.0.0 module step succeeds inside the same transaction.
```

---

## 5. Repository Interfaces (Ports)

### 5.1 IProductRepository
**File:** `commonMain/data/repositories/IProductRepository.kt`

```kotlin
package com.tpvmovil.data.repositories

import com.tpvmovil.domain.entities.*

interface IProductRepository {
    fun list(query: ProductQuery): List<ProductWithPopularity>
    fun findById(id: ProductId): Product?
    fun findBySku(sku: String): Product?
    fun create(input: CreateProductInput): Product
    fun update(id: ProductId, input: UpdateProductInput): Product
    fun delete(id: ProductId)  // soft delete
    fun findByIds(ids: List<ProductId>): List<Product>
}

data class ProductQuery(
    val search: String? = null,
    val categoryId: CategoryId? = null,
    val sort: SortOrder = SortOrder.NAME_ASC,
    val limit: Int = 15,
    val offset: Int = 0
)

enum class SortOrder { NAME_ASC, POPULARITY_DESC }

data class ProductWithPopularity(
    val product: Product,
    val popularity: Int
)
```

### 5.2 ICategoryRepository
**File:** `commonMain/data/repositories/ICategoryRepository.kt`

```kotlin
package com.tpvmovil.data.repositories

import com.tpvmovil.domain.entities.*

interface ICategoryRepository {
    fun list(): List<Category>
    fun findById(id: CategoryId): Category?
    fun create(input: CreateCategoryInput): Category
}
```

---

## 6. Popularity Query (Sales)

```sql
-- In SqlDelight .sq file
selectProductsWithPopularity:
SELECT p.*, COALESCE(SUM(d.quantity), 0) AS popularity
FROM products p
LEFT JOIN transaction_details d ON d.product_id = p.id
LEFT JOIN sales_transactions t
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

**File:** `commonMain/application/products/ProductManagement.kt`

| Use Case | Signature | Atomic | Preconditions | Postconditions |
|---|---|---|---|---|
| `createProduct` | `(input: CreateProductInput) -> Product` | Yes (SQLDelight tx) | category exists, SKU unique if provided | Product inserted, `registered_at` = now |
| `updateProduct` | `(id, input: UpdateProductInput) -> Product` | Yes (SQLDelight tx) | product exists, SKU unique if changed | Product updated, `updated_at` = now |
| `deleteProduct` | `(id: ProductId) -> Unit` | Yes (soft delete) | product exists | `deleted_at` = now, `updated_at` = now |

---

## 8. Image Compression Pipeline

**Files:**
- `commonMain/infrastructure/image/ImageOptimizer.kt` (expect)
- `androidMain/infrastructure/image/ImageOptimizer.kt` (actual)
- `iosMain/infrastructure/image/ImageOptimizer.kt` (actual)
- `commonMain/infrastructure/image/ImageStorage.kt` (expect/actual)

### Technical Specification

| Parameter | Value |
|---|---|
| Target dimension | 400 × 400 px (fit: contain, maintains aspect ratio) |
| Format | JPEG |
| Quality | 70% |
| Target size | < 40 KB |
| Destination directory | `${FileSystem.documentDirectory}productos/` (platform actual) |
| Filename | `<uuid>.jpg` (timestamp base36 + random) |
| URI stored in SQLite | Relative to `productos/` directory (e.g., `productos/abc123.jpg`) |

### Flow
```
User selects image (camera/gallery)
        │
        ▼
ImageManipulator.resize(400, 400) + compress(0.7, JPEG)
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
- `AsyncImage` (Coil) renders with `contentScale = ContentScale.Crop`.

---

## 9. Stock Alert Logic

**File:** `commonMain/presentation/viewmodels/StockAlertViewModel.kt` (shared logic)

### Trigger
Evaluated **after each successful commit** in:
- `RegisterSale` (sales-checkout)
- `RegisterPurchase` (sales-checkout)
- `updateProduct` (catalog-inventory)

### Rule
```
ALERT IF: currentStock < minStock
```

### UI Behavior (Compose)

| Affected products | Snackbar/Toast |
|---|---|
| 1 | `"Stock bajo: {name} — {currentStock} / mínimo {minStock}"` |
| >1 | `"{N} productos por debajo del mínimo ({firstName} y otros)"` |

- Variant: Warning (yellow/orange)
- Non-obstructive (auto-dismiss after 3.5s, configurable)
- Visual "Bajo" badge on `ProductGridCard` (red, top-left corner)

---

## 10. UI State Machines (Spanish visible text via `stringResource`)

### 10.1 Catalog Screen (`ProductGridScreen`)

| State | Trigger | Next State | Visible Text (ES) |
|---|---|---|---|
| `CatalogLoaded` | Pull-to-refresh | `Refreshing` | (spinner) |
| `CatalogLoaded` | Scroll to end + `hasMore` | `LoadingMore` | (footer loader) |
| `CatalogLoaded` | Search with results | `Filtered` | (grid updated) |
| `CatalogLoaded` | Search no results | `Empty` | `"No se encontraron productos"` + "Crear primer producto" button |
| `CatalogLoaded` | Tap FAB (+) | `NewProductForm` | Navigates to `ProductFormScreen` (create) |
| `CatalogLoaded` | Tap product card | `EditProductForm` | Navigates to `ProductFormScreen` (edit) |

### 10.2 Product Form (`ProductFormScreen`)

| State | Trigger | Next State | Visible Text (ES) |
|---|---|---|---|
| `FormEmpty` | User opens form | `Editing` | `"Nuevo producto"` / `"Editar producto"` (top bar title) |
| `Editing` | User taps [+] category | `QuickCategoryDialog` | `"Crear categoría"` (bottom sheet) |
| `QuickCategoryDialog` | Category saved | `Editing` | Category selected in `CategorySelector`; draft preserved |
| `Editing` | User taps Camera/Gallery | `ImagePicker` | Native selection system |
| `ImagePicker` | Image optimized | `Editing` | Preview 400×400 in `ProductFormHeader` |
| `Editing` | User saves (valid) | `Catalog` | `"Producto guardado"` (Snackbar success) + navigate back |
| `Editing` | User saves (invalid) | `Editing` | Snackbar error + focus invalid field |
| `Editing` | User deletes (confirmed) | `Catalog` | `"Producto eliminado"` (Snackbar success) + navigate back |

### 10.3 Stock Alert (cross-cutting)

| State | Trigger | Next State | Visible Text (ES) |
|---|---|---|---|
| `Normal` | Post-sale/purchase/edit → `currentStock < minStock` | `StockAlert` | Snackbar warning (see section 9) |

---

## 11. Compose UI Components

| Component | File | Type | Memoized | Description |
|---|---|---|---|---|
| `ProductGridScreen` | `composeApp/.../catalog/ProductGridScreen.kt` | Screen | N/A | Main catalog with `LazyVerticalGrid(columns = 3)` |
| `ProductGridCard` | `composeApp/.../catalog/ProductGridCard.kt` | Composable | `key = product.id` | 3-col card: image/avatar, name, price, stock, "Bajo" badge |
| `ProductFallbackAvatar` | `composeApp/.../catalog/ProductFallbackAvatar.kt` | Composable | Yes | Name initial on deterministic color (`colorForId`) |
| `ProductFormScreen` | `composeApp/.../catalog/ProductFormScreen.kt` | Screen | N/A | Form with header, inputs, category selector, SKU input |
| `ProductFormHeader` | `composeApp/.../catalog/ProductFormHeader.kt` | Composable | No | **FIRST** in form: image picker (camera/gallery) + preview + remove |
| `CategorySelector` | `composeApp/.../catalog/CategorySelector.kt` | Composable | No | Horizontal chips + `[+]` button for `QuickCategoryDialog` |
| `QuickCategoryDialog` | `composeApp/.../catalog/QuickCategoryDialog.kt` | Composable | No | Bottom sheet: name input → creates category → invalidates cache → auto-selects |
| `SearchBar` | `composeApp/.../filters/SearchBar.kt` | Composable | No | Debounce 200ms, placeholder `stringResource(R.string.catalog_search_placeholder)` |
| `SortMenu` | `composeApp/.../filters/SortMenu.kt` | Composable | No | Dropdown: "Nombre A-Z" / "Más vendidos" |
| `CategoryChipsRow` | `composeApp/.../filters/CategoryChipsRow.kt` | Composable | No | Clean text chips (no colors), "Todas" + categories |
| `EmptyState` | `composeApp/.../feedback/EmptyState.kt` | Composable | No | Illustration + title + description + action button |
| `AnimatedPressable` | `composeApp/.../ui/AnimatedPressable.kt` | Composable | No | Scale/opacity wrapper for tactile feedback (`pointerInput`) |
| `SKUInput` | `composeApp/.../catalog/SKUInput.kt` | Composable | No | SKU input with format validation (uppercase, alphanumeric, hyphen/underscore) |
| `AdBanner` | `composeApp/.../ads/AdBanner.kt` | Composable | No | Conditional AdMob banner governed by `AD_ENABLED` |

---

## 12. ViewModels

| ViewModel | File | Responsibility |
|---|---|---|
| `ProductsViewModel` | `commonMain/presentation/viewmodels/ProductsViewModel.kt` | Catalog with search, category, sort, pagination (`LazyVerticalGrid`) |
| `ProductFormViewModel` | `commonMain/presentation/viewmodels/ProductFormViewModel.kt` | Form state, validation, image handling, category management |
| `CategoriesViewModel` | `commonMain/presentation/viewmodels/CategoriesViewModel.kt` | Active categories list + manual refresh |
| `StockAlertViewModel` | `commonMain/presentation/viewmodels/StockAlertViewModel.kt` | Evaluates and shows Snackbar if `currentStock < minStock` |

---

## 13. AdWare Governance

**Rule:** Banner ads **ARE allowed** on the Inventory screen (`ProductGridScreen`).

**Implementation:**
- Check `AD_ENABLED` flag via `AdEnabledProvider` (CompositionLocal).
- If `true`, render `AdBanner` in `LazyVerticalGrid` footer item, **below** products and **above** FAB.
- Do not interrupt scroll or card tactile feedback.
- Respect `prefersReducedMotion` for banner animations.

---

## 14. Applicable NFRs

- **NFR-01:** Offline-first SQLDelight (JSI SQLite driver, transaction blocks).
- **NFR-02:** `LazyVerticalGrid` `columns = 3`, sub-16ms render, stable keys.
- **NFR-04:** Pagination `LIMIT ? OFFSET ?` with `pageSize` from settings (default 15).
- **NFR-05:** Image 400×400, JPEG 70%, target < 40 KB.
- **NFR-06:** Dark mode + runtime design tokens (`MaterialTheme` + custom tokens).
- **NFR-07:** Atomic rollback via SQLDelight transaction blocks on write use cases.
- **NFR-08:** UI strings in Spanish via `stringResource`; code/DB/identifiers in English.
- **NFR-09:** Strict Kotlin (`explicit-api`, `freeCompilerArgs = ["-Xlint"]`).
- **NFR-10:** Tests: 100% pass unit + integration + stock/cash integrity.

---

## 15. Extension Points (v2.0.0 Roadmap)

- [ ] **SKU/Barcode scanning**: `CameraX` + `ML Kit Barcode Scanning` integration for quick capture in `ProductFormHeader`.
- [ ] **Restock via purchase**: Guided "Buy low stock" flow pre-populating purchase cart with alerted products.
- [ ] **Stock history**: `stock_movements` table (type: `IN`/`OUT`/`ADJUSTMENT`, `quantity`, `source_transaction_id`).
- [ ] **Product variants**: Size/color/flavor as sub-entities under `Product`.
- [ ] **Bulk import**: CSV/Excel → validation → bulk `createProduct` transaction.
- [ ] **Catalog export**: CSV/PDF with current filters (search, category, sort).
- [ ] **Full-text search index**: FTS5 on `products(name)` for richer searches.
- [ ] **AdMob integration**: Implement conditional `AD_ENABLED` banner in grid footer.
- [ ] **DB migration v2**: Verified zero-data-loss migration from v1.0.0 Spanish schema.

---

## 16. i18n Keys (Spanish UI Dictionary — `composeResources/values-es/strings.xml`)

```xml
<!-- Catalog Screen -->
<string name="catalog_title">Productos</string>
<string name="catalog_empty_title">Sin productos</string>
<string name="catalog_empty_desc">Crea tu primer producto para empezar a vender.</string>
<string name="catalog_empty_action">Crear primer producto</string>
<string name="catalog_fab_label">Crear producto</string>
<string name="catalog_search_placeholder">Buscar productos…</string>
<string name="catalog_sort_label">Ordenar</string>
<string name="catalog_sort_name_asc">Nombre A-Z</string>
<string name="catalog_sort_popularity_desc">Más vendidos</string>
<string name="catalog_category_all">Todas</string>
<string name="catalog_category_new">Nueva</string>

<!-- Product Form -->
<string name="catalog_form_title_new">Nuevo producto</string>
<string name="catalog_form_title_edit">Editar producto</string>
<string name="catalog_form_header_camera">Cámara</string>
<string name="catalog_form_header_gallery">Galería</string>
<string name="catalog_form_header_remove">Quitar</string>
<string name="catalog_form_label_name">Nombre</string>
<string name="catalog_form_label_selling_price">Precio de venta</string>
<string name="catalog_form_label_purchase_price">Precio de compra</string>
<string name="catalog_form_label_current_stock">Stock actual</string>
<string name="catalog_form_label_min_stock">Stock mínimo</string>
<string name="catalog_form_helper_min_stock">Alerta no obstructiva al pasar este umbral.</string>
<string name="catalog_form_category_label">Categoría</string>
<string name="catalog_form_category_required">Categoría obligatoria</string>
<string name="catalog_form_category_select_hint">Selecciona una categoría obligatoria.</string>
<string name="catalog_modal_category_title">Nueva categoría</string>
<string name="catalog_modal_category_label">Nombre</string>
<string name="catalog_modal_category_placeholder">Ej. Bebidas</string>
<string name="catalog_btn_create">Crear producto</string>
<string name="catalog_btn_save">Guardar cambios</string>
<string name="catalog_btn_delete">Eliminar producto</string>
<string name="catalog_confirm_delete_title">Eliminar producto</string>
<string name="catalog_confirm_delete_msg">¿Eliminar este producto? Se ocultará del catálogo pero se conservarán las ventas y compras históricas.</string>
<string name="catalog_toast_created">Producto creado</string>
<string name="catalog_toast_updated">Producto actualizado</string>
<string name="catalog_toast_deleted">Producto eliminado</string>
<string name="catalog_toast_error">No se pudo guardar.</string>
<string name="catalog_alert_low_stock_single">Stock bajo: %1$s — %2$s / mínimo %3$s</string>
<string name="catalog_alert_low_stock_multiple">%1$d productos por debajo del mínimo (%2$s y otros)</string>
<string name="catalog_image_perm_denied">No se puede acceder a la fuente de imagen.</string>
<string name="catalog_image_process_error">No se pudo procesar la imagen.</string>
```

---

## 17. Cross-References

- `sales-checkout/spec.md` — Section 7 (RegisterSale), 8 (UI State Machines), 12 (NFR-02, NFR-07)
- `financial-accumulators/spec.md` — Section 6.3 (RevertTransaction stock math), 13 (NFR-03, NFR-07)
- `system-settings/spec.md` — Section 2.1 (Currency/Money), 3 (RULE-SS-05, RULE-SS-06), 10 (useCurrency, useSettingsPageSize)
- `expenses-cashflow/spec.md` — Section 3 (RULE-EC-03: EXPENSE without product items)

---

*Last updated: 2026-11-12 — v2.0.0 KMP Specification*