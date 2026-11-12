# Catalog & Inventory — Implementation Plan (v2.0.0)

> **Status (2026-11-11):** Phase 2 (atomic migration + repository foundation) is complete and tested. `schema.sql.ts` intentionally remains the v1 legacy baseline; the v2.0.0 schema is produced by `runMigrations`. Domain-entity renaming and presentation work remain pending.

## 1. Component Breakdown

| Component | File | Type | Memoized | Status v2.0.0 |
|---|---|---|---|---|
| `ProductGridCard` | `src/presentation/components/product/ProductGridCard.tsx` | UI | Yes | ✅ Exists |
| `ProductFallbackAvatar` | `src/presentation/components/product/ProductFallbackAvatar.tsx` | UI | Yes | ✅ Exists |
| `ProductFormHeader` | `src/presentation/components/product/ProductFormHeader.tsx` | UI | No | ✅ Exists |
| `CategorySelector` | `src/presentation/components/product/CategorySelector.tsx` | UI | No | ✅ Exists |
| `QuickCategoryModal` | `src/presentation/components/product/QuickCategoryModal.tsx` | UI | No | ✅ Exists |
| `SearchBar` | `src/presentation/components/filters/SearchBar.tsx` | UI | No | ✅ Exists |
| `SortMenu` | `src/presentation/components/filters/SortMenu.tsx` | UI | No | ✅ Exists |
| `CategoryChipsRow` | `src/presentation/components/filters/CategoryChipsRow.tsx` | UI | No | ✅ Exists |
| `EmptyState` | `src/presentation/components/feedback/EmptyState.tsx` | UI | No | ✅ Exists |
| `AnimatedPressable` | `src/presentation/components/ui/AnimatedPressable.tsx` | UI | No | ✅ Exists |
| **SKUInput** | `src/presentation/components/product/SKUInput.tsx` | UI | No | 🆕 New v2.0.0 |
| **AdBanner** | `src/presentation/components/ads/AdBanner.tsx` | UI | No | 🆕 New v2.0.0 |

---

## 2. Value Objects (Domain Layer)

| File | Description | Status |
|---|---|---|
| `src/domain/value-objects/SKU.ts` | Value object for SKU (alphanumeric, max 32, uppercase, unique) | 🆕 Create |
| `src/domain/value-objects/Stock.ts` | Value object for stock (non-negative decimal, 3dp, add/subtract/isBelow) | 🆕 Create |

---

## 3. Entities (Domain Layer)

| File | v2.0.0 Changes | Status |
|---|---|---|
| `src/domain/entities/Product.ts` | English identifiers: `categoryId`, `sku`, `name`, `sellingPrice`, `purchasePrice`, `currentStock`, `minStock`, `imageUri`, `registeredAt`, `updatedAt`, `deletedAt`. Types: `SKU`, `Money`, `Stock`. | 🔄 Update |
| `src/domain/entities/Category.ts` | English identifiers: `id`, `name`, `registeredAt`, `deletedAt`. | 🔄 Update |

---

## 4. Database & Data Migration (v1.0.0 → v2.0.0 Zero Data Loss)

### 4.1 Target Schema (English Columns)

```sql
-- categories table (renamed from categorias)
CREATE TABLE IF NOT EXISTS categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL
);

-- products table (renamed from productos)
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
CREATE INDEX IF NOT EXISTS idx_products_sku ON products(sku) WHERE deleted_at IS NULL AND sku IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_products_deleted_at ON products(deleted_at) WHERE deleted_at IS NULL;
```

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

**Approach:** Atomic transaction using `db.withTransactionSync()` in migration v2. Requires SQLite ≥ 3.25 for `ALTER TABLE ... RENAME COLUMN`. If unsupported, fallback to table copy.

**Files to create/modify:**
- `src/data/database/migrations.ts` — Version-grouped atomic runner (v2 steps share one transaction)
- `src/data/database/migrations/v2_0_0_catalog_inventory_migration.sql.ts` — `CATALOG_INVENTORY_MIGRATION_SQL` (see spec.md section 4.3)
- `src/data/database/schema.sql.ts` — Kept as the v1 legacy baseline; fresh installs run v1 → v2
- `src/data/mappers/rowMappers.ts` — Map English column names to the existing domain entities

### 4.4 Migration Task Checklist (CRITICAL — Execute in Order)

- [x] **Task 1:** Create `v2_0_0_catalog_inventory_migration.sql.ts` with the atomic DDL constant
- [x] **Task 2:** Register the catalog step in `migrations.ts` v2 (target version = 2)
- [x] **Task 3:** Keep `schema.sql.ts` as the v1 baseline; v2 schema is applied by migrations
- [x] **Task 4:** Update `rowMappers.ts` to map English column names
- [x] **Task 5:** Create "General" category when products have a NULL category and none exists
- [x] **Task 6:** Backfill NULL `category_id` to the oldest existing category
- [x] **Task 7:** Add `sku` column (nullable) with a partial unique index over active rows
- [x] **Task 8:** Enforce `category_id NOT NULL` via table rebuild (SQLite limitation)
- [x] **Task 9:** Recreate all partial indexes with English column names
- [x] **Task 10:** `PRAGMA user_version = 2` set centrally by `runMigrations`
- [x] **Task 11:** Verify row counts match pre-migration (products, categories)
- [x] **Task 12:** Verify PKs, timestamps, soft-deletes preserved
- [x] **Task 13:** Atomic rollback test: force a mid-v2 failure → catalog changes roll back
- [x] **Task 14:** Test idempotency: run migration twice → no errors, no data duplication

---

## 5. Repositories (Data Layer)

| File | Changes | Status |
|---|---|---|
| `src/data/repositories/IProductRepository.ts` | English method names, `findBySku`, `SortOrder` = `NAME_ASC` \| `POPULARITY_DESC` | 🔄 Update |
| `src/data/repositories/SqliteProductRepository.ts` | Implement `findBySku`, validate SKU unique, validate `category_id NOT NULL`, English column names | 🔄 Update |
| `src/data/repositories/ICategoryRepository.ts` | English method names | 🔄 Update |
| `src/data/repositories/SqliteCategoryRepository.ts` | English column names | 🔄 Update |
| `src/data/repositories/container.ts` | No changes | ✅ OK |

---

## 6. Use Cases (Application Layer)

| File | Changes | Status |
|---|---|---|
| `src/application/products/ProductManagement.ts` | English identifiers, use `SKU`, `Stock`, `Money` value objects in signatures | 🔄 Update (rename from `GestionProductos`) |

---

## 7. Infrastructure: Image Pipeline

| File | Status | Notes |
|---|---|---|
| `src/infrastructure/image/ImageOptimizer.ts` | ✅ Exists | Verify 400x400, JPEG 70% |
| `src/infrastructure/image/ImageStorage.ts` | ✅ Exists | Verify `productos/` directory, UUID naming |

---

## 8. Presentation: Hooks

| Hook | File | v2.0.0 Changes | Status |
|---|---|---|---|
| `useProducts` | `src/presentation/hooks/useProducts.ts` | Include `sku` in select, English `Product` type, `SortOrder` enum | 🔄 Update |
| `useCategories` | `src/presentation/hooks/useCategories.ts` | English `Category` type | 🔄 Update |
| `useStockAlert` | `src/presentation/hooks/useStockAlert.ts` | Use `Stock.isBelow()` with fractional values | 🔄 Update |
| `useCurrency` | `src/presentation/hooks/useCurrency.ts` | No changes | ✅ OK |

---

## 9. Presentation: Stores

| Store | File | Changes | Status |
|---|---|---|---|
| `productDraftStore` | `src/presentation/stores/productDraftStore.ts` | Add `sku: string` field, English identifiers | 🔄 Update |
| `settingsStore` | `src/presentation/stores/settingsStore.ts` | No changes | ✅ OK |
| `invalidationStore` | `src/presentation/stores/invalidationStore.ts` | No changes | ✅ OK |

---

## 10. Presentation: Screens

| Screen | File | v2.0.0 Changes | Status |
|---|---|---|---|
| Catalog | `app/(tabs)/products.tsx` | Add `AdBanner` in `ListFooterComponent` if `AD_ENABLED`, English identifiers | 🔄 Update |
| New Product | `app/products/new.tsx` | Add `SKUInput` after `ProductFormHeader`, fractional stock inputs (`decimal-pad`), English identifiers | 🔄 Update |
| Edit Product | `app/products/[id].tsx` | Same as new (reuses form component) | 🔄 Update |

---

## 11. i18n Keys (Spanish UI Dictionary)

Create file: `src/presentation/i18n/es/catalog.json` with 45 keys from spec.md section 17.

---

## 12. AdWare Governance

- [ ] Create `src/presentation/components/ads/AdBanner.tsx` (AdMob wrapper)
- [ ] In `app/(tabs)/products.tsx`: conditional `AD_ENABLED` → render `AdBanner` in `ListFooterComponent`
- [ ] Respect `useReducedMotion` for banner animations
- [ ] Default `AD_ENABLED = false` (opt-in at build time)

---

## 13. Testing (TDD) — Complete Test Suite Specification

### 13.1 Unit Tests (Domain Layer)

| Test | File | Coverage |
|---|---|---|
| `SKU` validation | `src/__tests__/domain/value-objects/SKU.test.ts` | Constructor, equals, format, uppercase normalization, length, invalid chars |
| `Stock` decimal math | `src/__tests__/domain/value-objects/Stock.test.ts` | Constructor, add, subtract, isBelow, 3dp precision, negative rejection, zero |
| `Product` entity factory | `src/__tests__/domain/entities/Product.test.ts` | Factory with SKU/Stock/Money, required fields, optional SKU |
| `Category` entity factory | `src/__tests__/domain/entities/Category.test.ts` | Factory, required name |

### 13.2 Integration Tests (Data Layer)

| Test | File | Scenario |
|---|---|---|
| `SqliteProductRepository.create` | `src/__tests__/data/repositories/SqliteProductRepository.test.ts` | Category required, SKU unique, fractional stock persisted |
| `SqliteProductRepository.findBySku` | `src/__tests__/data/repositories/SqliteProductRepository.test.ts` | Returns product, null if not found |
| `SqliteProductRepository.list` | `src/__tests__/data/repositories/SqliteProductRepository.test.ts` | Search, category filter, sort (name_asc, popularity_desc), pagination |
| `SqliteProductRepository.softDelete` | `src/__tests__/data/repositories/SqliteProductRepository.test.ts` | Sets `deleted_at`, hidden from `list()`, visible in forensic query |
| `SqliteCategoryRepository` CRUD | `src/__tests__/data/repositories/SqliteCategoryRepository.test.ts` | Create, list, findById, soft delete filter |

### 13.3 DB Data Migration Tests (CRITICAL)

| Test | File | Verification |
|---|---|---|
| Migration v2 transforms v1 schema | `src/__tests__/integration/migration/v2_0_0_migration.test.ts` | Tables renamed, columns renamed, `sku` added, indexes recreated |
| Legacy data preserved | `src/__tests__/integration/migration/v2_0_0_migration.test.ts` | Row counts match, PKs unchanged, timestamps preserved, soft-deletes preserved |
| NULL `category_id` backfilled | `src/__tests__/integration/migration/v2_0_0_migration.test.ts` | All products have `category_id = 1` (default "General") |
| `NOT NULL` enforced | `src/__tests__/integration/migration/v2_0_0_migration.test.ts` | Insert without `category_id` throws |
| `sku` unique constraint | `src/__tests__/integration/migration/v2_0_0_migration.test.ts` | Duplicate SKU throws |
| Migration idempotency | `src/__tests__/integration/migration/v2_0_0_migration.test.ts` | Running twice = no errors, no data duplication |
| Rollback via backup restore | `src/__tests__/integration/migration/v2_0_0_migration.test.ts` | Restore backup → v1.0.0 schema and data intact |

**Migration Test Fixture Strategy:**
```typescript
// Seed v1.0.0 schema with Spanish columns
await db.execSync(`
  CREATE TABLE categorias (id INTEGER PRIMARY KEY, nombre TEXT, fecha_registro TEXT, fecha_delete TEXT);
  CREATE TABLE productos (id INTEGER PRIMARY KEY, categoria_id INTEGER, nombre TEXT, precio_venta REAL, precio_compra REAL, stock_actual REAL, stock_minimo REAL, imagen_uri TEXT, fecha_registro TEXT, fecha_update TEXT, fecha_delete TEXT);
  INSERT INTO categorias VALUES (1, 'Bebidas', '2024-01-01', NULL);
  INSERT INTO productos VALUES (1, 1, 'Coca-Cola', 10.5, 7.0, 100, 10, NULL, '2024-01-01', '2024-01-01', NULL);
  INSERT INTO productos VALUES (2, NULL, 'Pan', 5.0, 3.0, 50, 5, NULL, '2024-01-01', '2024-01-01', '2024-06-01'); -- soft-deleted, NULL category
`);
```

### 13.4 Hook Tests

| Test | File | Coverage |
|---|---|---|
| `useProducts` | `src/__tests__/presentation/hooks/useProducts.test.ts` | Search, category filter, sort, pagination, infinite scroll, invalidation |
| `useStockAlert` | `src/__tests__/presentation/hooks/useStockAlert.test.ts` | Triggers when `currentStock < minStock` (fractional: 0.5 < 1.0) |

### 13.5 UI Component Tests (React Native Testing Library)

| Test | File | Coverage |
|---|---|---|
| `ProductGridCard` | `src/__tests__/presentation/components/product/ProductGridCard.test.tsx` | Renders image/avatar, name, price, stock, "Bajo" badge |
| `SKUInput` | `src/__tests__/presentation/components/product/SKUInput.test.tsx` | Validation, uppercase formatting, max length |
| `AdBanner` | `src/__tests__/presentation/components/ads/AdBanner.test.tsx` | Conditional render based on `AD_ENABLED` |

### 13.6 E2E Scenarios

| Scenario | Verification |
|---|---|
| Create product with fractional stock (0.5 kg) | DB persists `current_stock = 0.5`, UI shows 0.5 |
| Soft delete product | Hidden from catalog, `deleted_at` set, forensic query shows it |
| Migration v1→v2 on seeded data | All assertions in 13.3 pass |

---

## 14. Implementation Order (Dependency Graph)

```
1. Domain Value Objects: SKU.ts, Stock.ts
2. Domain Entities: Product.ts, Category.ts (English identifiers)
3. Database Migration:
   3a. v2_0_0_migration.sql (atomic DDL)
   3b. migrations.ts (register v2)
   3c. schema.sql.ts (target schema)
   3d. rowMappers.ts (English column mapping)
4. Repositories: IProductRepository, SqliteProductRepository, ICategoryRepository, SqliteCategoryRepository
5. Use Cases: ProductManagement.ts
6. Image Pipeline: Verify ImageOptimizer.ts, ImageStorage.ts
7. Hooks: useProducts.ts, useCategories.ts, useStockAlert.ts
8. Stores: productDraftStore.ts (add sku)
9. UI Components: SKUInput.tsx, AdBanner.tsx
10. i18n: es/catalog.json (45 keys)
11. Screens: app/(tabs)/products.tsx, app/products/new.tsx
12. Tests: All test files (unit → integration → migration → hooks → UI → E2E)
```

---

## 15. Risks & Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| `RENAME COLUMN` unsupported (SQLite < 3.25) | Migration fails | Detect version; fallback to table copy approach |
| Fractional stock breaks integer-only UI | User cannot enter decimals | `keyboardType="decimal-pad"`, `step={0.001}`, `min={0}` |
| SKU uniqueness conflicts on import | Duplicate SKU errors | Pre-validate in UI + DB constraint; clear error message |
| Migration long-running on large DB | App freeze on startup | Background execution with progress; timeout guard |
| i18n keys missing in components | Hardcoded Spanish strings | CI check: `grep -r "catalog\." --include="*.tsx" src/presentation/components/product` |
| AdMob not approved in store | Build rejection | `AD_ENABLED` flag default `false`, opt-in only |

---

## 16. Definition of Done (Acceptance Criteria)

- [ ] All tests pass: unit + integration + migration + hooks + UI + E2E
- [x] TypeScript strict build passes (`tsc --noEmit`)
- [ ] Lint passes (`eslint .`)
- [ ] FlashList render <50ms/frame on physical device
- [ ] Optimized image <40KB verified on 10 test images
- [ ] Soft delete hides from catalog, preserves history
- [ ] Stock alert Toast fires when `currentStock < minStock` (fractional)
- [ ] SKU unique validated on create/update
- [ ] Category required blocks save if not selected
- [ ] AdBanner renders only when `AD_ENABLED=true`
- [ ] i18n: zero hardcoded Spanish strings in catalog components
- [x] Migration v1→v2 tested with seeded v1.0.0 data (zero data loss)
- [x] Atomic rollback verified (forced mid-v2 failure rolls back catalog changes)
- [ ] Migration rollback via backup restore verified
- [x] Migration idempotency verified (run twice = no errors)

---

## 17. Cross-References

- `sales-checkout/plan.md` — `RegisterSale` use case integration
- `financial-accumulators/plan.md` — `RevertTransaction` stock math
- `system-settings/plan.md` — Currency, pagination, backup settings
- `expenses-cashflow/plan.md` — Expense registration

---

*Last updated: 2026-11-09 — v2.0.0 Plan*