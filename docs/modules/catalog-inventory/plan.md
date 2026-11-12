# Catalog & Inventory — Implementation Plan (v2.0.0 KMP)

> **Status (2026-11-12):** KMP migration planning phase. Phase 0 (Shared Kernel definition) complete on paper. Implementation begins with Phase 1.

## 1. Component Breakdown

| Component | File | Type | Memoized | Status v2.0.0 KMP |
|---|---|---|---|---|
| `ProductGridScreen` | `composeApp/.../catalog/ProductGridScreen.kt` | Screen | N/A | 🆕 New KMP |
| `ProductGridCard` | `composeApp/.../catalog/ProductGridCard.kt` | Composable | `key = product.id` | 🆕 New KMP |
| `ProductFallbackAvatar` | `composeApp/.../catalog/ProductFallbackAvatar.kt` | Composable | Yes | 🆕 New KMP |
| `ProductFormScreen` | `composeApp/.../catalog/ProductFormScreen.kt` | Screen | N/A | 🆕 New KMP |
| `ProductFormHeader` | `composeApp/.../catalog/ProductFormHeader.kt` | Composable | No | 🆕 New KMP |
| `CategorySelector` | `composeApp/.../catalog/CategorySelector.kt` | Composable | No | 🆕 New KMP |
| `QuickCategoryDialog` | `composeApp/.../catalog/QuickCategoryDialog.kt` | Composable | No | 🆕 New KMP |
| `SearchBar` | `composeApp/.../filters/SearchBar.kt` | Composable | No | 🆕 New KMP |
| `SortMenu` | `composeApp/.../filters/SortMenu.kt` | Composable | No | 🆕 New KMP |
| `CategoryChipsRow` | `composeApp/.../filters/CategoryChipsRow.kt` | Composable | No | 🆕 New KMP |
| `EmptyState` | `composeApp/.../feedback/EmptyState.kt` | Composable | No | 🆕 New KMP |
| `AnimatedPressable` | `composeApp/.../ui/AnimatedPressable.kt` | Composable | No | 🆕 New KMP |
| **SKUInput** | `composeApp/.../catalog/SKUInput.kt` | Composable | No | 🆕 New KMP |
| **AdBanner** | `composeApp/.../ads/AdBanner.kt` | Composable | No | 🆕 New KMP |

---

## 2. Value Objects (Domain Layer — `commonMain`)

| File | Description | Status |
|---|---|---|
| `commonMain/domain/valueobjects/SKU.kt` | Value class for SKU (alphanumeric, max 32, uppercase, unique) | 🆕 Create |
| `commonMain/domain/valueobjects/Stock.kt` | Value class for stock (non-negative decimal, 3dp, add/subtract/isBelow) | 🆕 Create |

---

## 3. Entities (Domain Layer — `commonMain`)

| File | v2.0.0 KMP Changes | Status |
|---|---|---|
| `commonMain/domain/entities/Product.kt` | English identifiers: `categoryId`, `sku`, `name`, `sellingPrice`, `purchasePrice`, `currentStock`, `minStock`, `imageUri`, `registeredAt`, `updatedAt`, `deletedAt`. Types: `SKU`, `Money`, `Stock`. Immutable `data class`. | 🆕 Create |
| `commonMain/domain/entities/Category.kt` | English identifiers: `id`, `name`, `registeredAt`, `deletedAt`. Immutable `data class`. | 🆕 Create |

---

## 4. Database & Data Migration (v1.0.0 → v2.0.0 Zero Data Loss)

### 4.1 Target Schema (English Columns)

**File:** `commonMain/sqldelight/schema.sq` (see spec.md section 4.1)

### 4.2 Column Mapping

| v1.0.0 (Spanish) | v2.0.0 (English) |
|---|---|
| `categorias` | `categories` |
| `categorias.nombre` | `categories.name` |
| `categorias.fecha_registro` | `categories.registered_at` |
| `categorias.fecha_delete` | `categories.deleted_at` |
| `productos` | `products` |
| `productos.categoria_id` | `products.category_id` (now NOT NULL) |
| `productos.nombre` | `products.name` |
| `productos.precio_venta` | `products.selling_price` |
| `productos.precio_compra` | `products.purchase_price` |
| `productos.stock_actual` | `products.current_stock` (fractional) |
| `productos.stock_minimo` | `products.min_stock` (fractional) |
| `productos.imagen_uri` | `products.image_uri` |
| `productos.fecha_registro` | `products.registered_at` |
| `productos.fecha_update` | `products.updated_at` |
| `productos.fecha_delete` | `products.deleted_at` |
| *(new)* | `products.sku` (TEXT UNIQUE, nullable) |

### 4.3 Migration Strategy

**Approach:** Atomic transaction using SQLDelight `.sqm` migration file. Requires SQLite ≥ 3.25 for `ALTER TABLE ... RENAME COLUMN`. If unsupported, fallback to table copy in migration SQL.

**Files to create/modify:**
- `commonMain/sqldelight/schema.sq` — Target schema for fresh installs
- `commonMain/sqldelight/migrations/v2_catalog_inventory.sqm` — Atomic DDL (see spec.md section 4.3)
- `commonMain/data/database/DatabaseDriverFactory.kt` — Migration runner registration

### 4.4 Migration Task Checklist (CRITICAL — Execute in Order)

- [ ] **Task 1:** Create `v2_catalog_inventory.sqm` with the atomic DDL
- [ ] **Task 2:** Register the catalog step in `DatabaseDriverFactory` v2 migration list
- [ ] **Task 3:** Define target schema in `schema.sq` (used for fresh installs)
- [ ] **Task 4:** Create "General" category when products have a NULL category and none exists
- [ ] **Task 5:** Backfill NULL `category_id` to the oldest existing category
- [ ] **Task 6:** Add `sku` column (nullable) with a partial unique index over active rows
- [ ] **Task 7:** Enforce `category_id NOT NULL` via table rebuild (SQLite limitation)
- [ ] **Task 8:** Recreate all partial indexes with English column names
- [ ] **Task 9:** `PRAGMA user_version = 2` set centrally by `DatabaseDriverFactory`
- [ ] **Task 10:** Verify row counts match pre-migration (products, categories)
- [ ] **Task 11:** Verify PKs, timestamps, soft-deletes preserved
- [ ] **Task 12:** Atomic rollback test: force a mid-v2 failure → catalog changes roll back
- [ ] **Task 13:** Test idempotency: run migration twice → no errors, no data duplication

---

## 5. Repositories (Data Layer — `commonMain`)

| File | Changes | Status |
|---|---|---|
| `commonMain/data/repositories/IProductRepository.kt` | English method names, `findBySku`, `SortOrder` = `NAME_ASC` \| `POPULARITY_DESC` | 🆕 Create |
| `commonMain/data/repositories/impl/SqlDelightProductRepository.kt` | Implement `findBySku`, validate SKU unique, validate `category_id NOT NULL`, English column names via SQLDelight queries | 🆕 Create |
| `commonMain/data/repositories/ICategoryRepository.kt` | English method names | 🆕 Create |
| `commonMain/data/repositories/impl/SqlDelightCategoryRepository.kt` | English column names via SQLDelight queries | 🆕 Create |
| `commonMain/data/database/DatabaseDriverFactory.kt` | Register repositories | 🆕 Create |

---

## 6. Use Cases (Application Layer — `commonMain`)

| File | Changes | Status |
|---|---|---|
| `commonMain/application/products/ProductManagement.kt` | English identifiers, use `SKU`, `Stock`, `Money` value objects in signatures | 🆕 Create |

---

## 7. Infrastructure: Image Pipeline (expect/actual)

| File | Status | Notes |
|---|---|---|
| `commonMain/infrastructure/image/ImageOptimizer.kt` | 🆕 Create (expect) | 400x400, JPEG 70% |
| `androidMain/infrastructure/image/ImageOptimizer.kt` | 🆕 Create (actual) | CameraX + ImageManipulator |
| `iosMain/infrastructure/image/ImageOptimizer.kt` | 🆕 Create (actual) | PhotosUI + CoreImage |
| `commonMain/infrastructure/image/ImageStorage.kt` | 🆕 Create (expect) | `productos/` directory, UUID naming |
| `androidMain/infrastructure/image/ImageStorage.kt` | 🆕 Create (actual) | `FileSystem.documentDirectory` |
| `iosMain/infrastructure/image/ImageStorage.kt` | 🆕 Create (actual) | `FileManager.default.urls(.documentDirectory)` |

---

## 8. Presentation: ViewModels (`commonMain/presentation/viewmodels/`)

| ViewModel | File | v2.0.0 Changes | Status |
|---|---|---|---|
| `ProductsViewModel` | `ProductsViewModel.kt` | Include `sku` in select, English `Product` type, `SortOrder` enum, `StateFlow` UI state | 🆕 Create |
| `ProductFormViewModel` | `ProductFormViewModel.kt` | Form state, validation, image handling, category management, `MutableStateFlow` | 🆕 Create |
| `CategoriesViewModel` | `CategoriesViewModel.kt` | English `Category` type, `StateFlow` | 🆕 Create |

---

## 9. Presentation: Compose UI Screens (`composeApp/src/commonMain/compose/`)

| Screen | File | v2.0.0 Changes | Status |
|---|---|---|---|
| Catalog | `catalog/ProductGridScreen.kt` | Add `AdBanner` in footer if `AD_ENABLED`, English identifiers, `LazyVerticalGrid` | 🆕 Create |
| New Product | `catalog/ProductFormScreen.kt` | Add `SKUInput` after `ProductFormHeader`, fractional stock inputs (`KeyboardType.Decimal`), English identifiers | 🆕 Create |
| Edit Product | `catalog/ProductFormScreen.kt` | Same as new (reuses screen with ViewModel mode) | 🆕 Create |

---

## 10. i18n Keys (Spanish UI Dictionary)

Create file: `composeApp/src/commonMain/composeResources/values-es/strings.xml` with 45 keys from spec.md section 16.

Additional locales:
- `values/` (English fallback)
- `values-es-rUS/` (US Spanish variant if needed)

---

## 11. AdWare Governance

- [ ] Create `composeApp/.../ads/AdBanner.kt` (AdMob wrapper Composable)
- [ ] In `ProductGridScreen`: conditional `AD_ENABLED` → render `AdBanner` in footer item
- [ ] Respect `prefersReducedMotion` for banner animations
- [ ] Default `AD_ENABLED = false` (opt-in at build time via `BuildConfig`)

---

## 12. Testing (TDD) — Complete Test Suite Specification

### 12.1 Unit Tests (Domain Layer)
**Directory:** `commonTest/unit/catalog-inventory/`

| Test | File | Coverage |
|---|---|---|
| `SKU` validation | `SKUTest.kt` | Constructor, equals, format, uppercase normalization, length, invalid chars |
| `Stock` decimal math | `StockTest.kt` | Constructor, add, subtract, isBelow, 3dp precision, negative rejection, zero |
| `Product` entity factory | `ProductTest.kt` | Factory with SKU/Stock/Money, required fields, optional SKU |
| `Category` entity factory | `CategoryTest.kt` | Factory, required name |

### 12.2 Integration Tests (Data Layer)
**Directory:** `commonTest/integration/catalog-inventory/`

| Test | File | Scenario |
|---|---|---|
| `SqlDelightProductRepository.create` | `SqlDelightProductRepositoryTest.kt` | Category required, SKU unique, fractional stock persisted |
| `SqlDelightProductRepository.findBySku` | `SqlDelightProductRepositoryTest.kt` | Returns product, null if not found |
| `SqlDelightProductRepository.list` | `SqlDelightProductRepositoryTest.kt` | Search, category filter, sort (name_asc, popularity_desc), pagination |
| `SqlDelightProductRepository.softDelete` | `SqlDelightProductRepositoryTest.kt` | Sets `deleted_at`, hidden from `list()`, visible in forensic query |
| `SqlDelightCategoryRepository` CRUD | `SqlDelightCategoryRepositoryTest.kt` | Create, list, findById, soft delete filter |

### 12.3 DB Data Migration Tests (CRITICAL)
**Directory:** `commonTest/migration/catalog-inventory/`

| Test | File | Verification |
|---|---|---|
| Migration v2 transforms v1 schema | `V2CatalogInventoryMigrationTest.kt` | Tables renamed, columns renamed, `sku` added, indexes recreated |
| Legacy data preserved | `V2CatalogInventoryMigrationTest.kt` | Row counts match, PKs unchanged, timestamps preserved, soft-deletes preserved |
| NULL `category_id` backfilled | `V2CatalogInventoryMigrationTest.kt` | All products have `category_id = 1` (default "General") |
| `NOT NULL` enforced | `V2CatalogInventoryMigrationTest.kt` | Insert without `category_id` throws |
| `sku` unique constraint | `V2CatalogInventoryMigrationTest.kt` | Duplicate SKU throws |
| Migration idempotency | `V2CatalogInventoryMigrationTest.kt` | Running twice = no errors, no data duplication |
| Rollback via backup restore | `V2CatalogInventoryMigrationTest.kt` | Restore backup → v1.0.0 schema and data intact |

**Migration Test Fixture Strategy:**
```kotlin
// Seed v1.0.0 schema with Spanish columns
val db = SqliteDriverFactory.createInMemory()
db.execute("""
    CREATE TABLE categorias (id INTEGER PRIMARY KEY, nombre TEXT, fecha_registro INTEGER, fecha_delete INTEGER);
    CREATE TABLE productos (id INTEGER PRIMARY KEY, categoria_id INTEGER, nombre TEXT, precio_venta REAL, precio_compra REAL, stock_actual REAL, stock_minimo REAL, imagen_uri TEXT, fecha_registro INTEGER, fecha_update INTEGER, fecha_delete INTEGER);
    INSERT INTO categorias VALUES (1, 'Bebidas', 1704067200000, NULL);
    INSERT INTO productos VALUES (1, 1, 'Coca-Cola', 10.5, 7.0, 100, 10, NULL, 1704067200000, 1704067200000, NULL);
    INSERT INTO productos VALUES (2, NULL, 'Pan', 5.0, 3.0, 50, 5, NULL, 1704067200000, 1704067200000, 1717200000000); -- soft-deleted, NULL category
""")
```

### 12.4 ViewModel Tests (Turbine)
**Directory:** `commonTest/viewmodels/catalog-inventory/`

| Test | File | Coverage |
|---|---|---|
| `ProductsViewModel` | `ProductsViewModelTest.kt` | Search, category filter, sort, pagination, infinite scroll, invalidation |
| `ProductFormViewModel` | `ProductFormViewModelTest.kt` | Form validation, image handling, category selection, draft preservation |
| `StockAlertViewModel` | `StockAlertViewModelTest.kt` | Triggers when `currentStock < minStock` (fractional: 0.5 < 1.0) |

### 12.5 Compose UI Tests (Robolectric)
**Directory:** `androidTest/compose/catalog/`

| Test | File | Coverage |
|---|---|---|
| `ProductGridScreen` | `ProductGridScreenTest.kt` | Renders grid, search, category filter, FAB navigation |
| `ProductGridCard` | `ProductGridCardTest.kt` | Renders image/avatar, name, price, stock, "Bajo" badge |
| `SKUInput` | `SKUInputTest.kt` | Validation, uppercase formatting, max length |
| `AdBanner` | `AdBannerTest.kt` | Conditional render based on `AD_ENABLED` |

### 12.6 E2E Scenarios (Maestro)
**Directory:** `maestro/catalog-inventory/`

| Scenario | Verification |
|---|---|
| Create product with fractional stock (0.5 kg) | DB persists `current_stock = 0.5`, UI shows 0.5 |
| Soft delete product | Hidden from catalog, `deleted_at` set, forensic query shows it |
| Migration v1→v2 on seeded data | All assertions in 12.3 pass |

---

## 13. Implementation Order (Dependency Graph)

```
1. Domain Value Objects: SKU.kt, Stock.kt
2. Domain Entities: Product.kt, Category.kt (English identifiers)
3. Database Migration:
   3a. v2_catalog_inventory.sqm (atomic DDL)
   3b. DatabaseDriverFactory.kt (register v2)
   3c. schema.sq (target schema)
4. Repositories: IProductRepository, SqlDelightProductRepository, ICategoryRepository, SqlDelightCategoryRepository
5. Use Cases: ProductManagement.kt
6. Image Pipeline: Verify ImageOptimizer.kt (expect/actual), ImageStorage.kt (expect/actual)
7. ViewModels: ProductsViewModel.kt, ProductFormViewModel.kt, CategoriesViewModel.kt
8. Compose UI Components: SKUInput.kt, AdBanner.kt, ProductGridCard.kt, etc.
9. Compose Screens: ProductGridScreen.kt, ProductFormScreen.kt
10. i18n: values-es/strings.xml (45 keys)
11. Tests: All test files (unit → integration → migration → ViewModel → UI → E2E)
```

---

## 14. Risks & Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| `RENAME COLUMN` unsupported (SQLite < 3.25) | Migration fails | Detect version; fallback to table copy approach in `.sqm` |
| Fractional stock breaks integer-only UI | User cannot enter decimals | `KeyboardType.Decimal`, `step = 0.001`, `min = 0` in `TextField` |
| SKU uniqueness conflicts on import | Duplicate SKU errors | Pre-validate in UI + DB constraint; clear error message |
| Migration long-running on large DB | App freeze on startup | Background execution with progress; timeout guard |
| i18n keys missing in components | Hardcoded Spanish strings | CI check: `grep -r "stringResource" --include="*.kt" composeApp/.../catalog` |
| AdMob not approved in store | Build rejection | `AD_ENABLED` flag default `false`, opt-in only |
| Compose iOS rendering differences | Visual inconsistencies | Test on both platforms; use `expect/actual` for platform specifics |

---

## 15. Definition of Done (Acceptance Criteria)

- [ ] All tests pass: unit + integration + migration + ViewModel + UI + E2E
- [ ] Kotlin strict build passes (`./gradlew compileKotlinAndroid compileKotlinIos`)
- [ ] Detekt passes (`./gradlew detekt`)
- [ ] LazyVerticalGrid render <16ms/frame on physical device (Android/iOS)
- [ ] Optimized image <40KB verified on 10 test images
- [ ] Soft delete hides from catalog, preserves history
- [ ] Stock alert Snackbar fires when `currentStock < minStock` (fractional)
- [ ] SKU unique validated on create/update
- [ ] Category required blocks save if not selected
- [ ] AdBanner renders only when `AD_ENABLED=true`
- [ ] i18n: zero hardcoded Spanish strings in catalog components
- [ ] Migration v1→v2 tested with seeded v1.0.0 data (zero data loss)
- [ ] Atomic rollback verified (forced mid-v2 failure rolls back catalog changes)
- [ ] Migration rollback via backup restore verified
- [ ] Migration idempotency verified (run twice = no errors)

---

## 16. Cross-References

- `sales-checkout/plan.md` — `RegisterSale` use case integration
- `financial-accumulators/plan.md` — `RevertTransaction` stock math
- `system-settings/plan.md` — Currency, pagination, backup settings
- `expenses-cashflow/plan.md` — Expense registration

---

*Plan version: 2.0 — 2026-11-12 — KMP Migration*