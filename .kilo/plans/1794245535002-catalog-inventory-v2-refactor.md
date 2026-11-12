# Catalog & Inventory Module v2.0.0 — Refactor Plan

## Goal
Refactor and complete the module triad (`spec.md`, `plan.md`, `test.md`) in `/docs/modules/catalog-inventory/` to v2.0.0 following SemVer, with English identifiers, fractional stock support, and zero-data-loss migration from v1.0.0 Spanish schema.

---

## Context & Constraints

| Aspect | Requirement |
|---|---|
| **Version** | `v2.0.0` (MAJOR.MINOR.PATCH) — breaking changes: column renames, English identifiers |
| **Language** | 100% English identifiers (code, DB, entities, VOs, repos); Spanish UI only via i18n keys |
| **Stock** | Fractional support: `Stock >= 0`, up to 3 decimal places (e.g., 0.5 kg, 1.25 L) |
| **Migration** | Atomic, non-destructive SQLite transaction: rename columns, backfill defaults, add `sku`, preserve PKs/timestamps/soft-deletes |
| **Images** | 400x400 JPEG 70% (<40KB) in `documentDirectory/productos/<uuid>.jpg` |
| **Ads** | Conditional `AdBanner` in `ListFooterComponent` gated by `AD_ENABLED` flag |

---

## Affected Files (Module Triad)

| File | Action |
|---|---|
| `/docs/modules/catalog-inventory/spec.md` | Full rewrite to v2.0.0 |
| `/docs/modules/catalog-inventory/plan.md` | Full rewrite to v2.0.0 with migration checklist |
| `/docs/modules/catalog-inventory/test.md` | New file: complete TDD test suite spec |

---

## Implementation Sequence

### Phase 1: Specification (`spec.md`)

**Tasks:**
1. Update header to `v2.0.0`
2. Define domain entities with English identifiers:
   - `Product` (id, categoryId, sku, name, sellingPrice, purchasePrice, currentStock, minStock, imageUri, registeredAt, updatedAt, deletedAt)
   - `Category` (id, name, registeredAt, deletedAt)
3. Define Value Objects:
   - `SKU` (alphanumeric, max 32, uppercase, unique when provided)
   - `Stock` (non-negative decimal, 3dp precision, add/subtract/isBelow)
   - `Money` (amount, currencyCode — references existing)
   - `Currency` (code: 'BOB' | 'USD' | 'EUR', symbol, locale — references existing)
4. Document Database Schema & Migration DDL:
   - Target schema (English columns, `category_id NOT NULL`, `sku TEXT UNIQUE`)
   - Migration steps: `ALTER TABLE` + `UPDATE` backfill + index creation in single transaction
   - Column mapping table: Spanish → English
5. Update repository interfaces: `IProductRepository`, `ICategoryRepository`
6. List all 45 i18n keys (Spanish UI strings)

### Phase 2: Implementation Plan (`plan.md`)

**Tasks:**
1. Component breakdown table (existing + new: `SKUInput`, `AdBanner`)
2. Step-by-step execution order with dependencies
3. **Database Data Migration Task Checklist** (critical):
   - [ ] Create migration v2 in `migrations.ts` (version 2)
   - [ ] Write `v2_0_0_migration.sql` with atomic transaction
   - [ ] Backfill `category_id = 1` where NULL
   - [ ] Rename columns via `ALTER TABLE ... RENAME COLUMN` (SQLite 3.25+)
   - [ ] Add `sku` column
   - [ ] Create indexes: `idx_products_category`, `idx_products_name`, `idx_products_sku`, `idx_products_deleted_at`
   - [ ] Verify row counts match pre-migration
   - [ ] Test rollback scenario (restore from backup)
4. Update hooks, stores, screens with English identifiers
5. AdWare integration steps
6. i18n file creation: `src/presentation/i18n/es/catalog.json`
7. Risk mitigation table
8. Definition of Done checklist

### Phase 3: Test Specification (`test.md`)

**Tasks:**
1. Unit tests (TDD):
   - `Stock` decimal math: add, subtract, isBelow, precision (3dp), negative rejection
   - `SKU` validation: format, length, uppercase normalization, uniqueness
   - `Product`/`Category` entity factories
   - `Money`/`Currency` formatting (reference existing)
2. Integration tests:
   - `SqliteProductRepository`: CRUD, soft-delete filter (`deleted_at IS NULL`), SKU uniqueness, pagination, popularity sort
   - `SqliteCategoryRepository`: CRUD, soft-delete filter
3. **DB Data Migration Tests** (critical):
   - Seed v1.0.0 schema with Spanish columns + sample data (including NULL `categoria_id`, soft-deleted rows)
   - Run migration v2
   - Assert: all rows migrated, English columns populated, `category_id` backfilled to 1, PKs preserved, timestamps preserved, soft-deletes preserved, row count unchanged
   - Assert: new `sku` column exists and nullable
   - Assert: indexes created
4. Hook tests:
   - `useProducts`: search, category filter, sort, pagination, infinite scroll
   - `useStockAlert`: triggers when `currentStock < minStock` (fractional)
5. UI component tests (React Native Testing Library):
   - `ProductGridCard`: renders image/avatar, price, stock badge
   - `SKUInput`: validation, formatting
   - `AdBanner`: conditional render based on `AD_ENABLED`
6. E2E scenarios:
   - Create product with fractional stock (0.5) → verify DB persistence
   - Soft delete → verify hidden from catalog, visible in forensic query
   - Migration idempotency: run twice → no errors, no data duplication

---

## Key Design Decisions (Resolved)

| Decision | Resolution |
|---|---|
| **Stock precision** | 3 decimal places (milligram/milliliter precision) |
| **SKU format** | Uppercase alphanumeric + hyphen/underscore, max 32 chars |
| **Migration strategy** | SQLite `ALTER TABLE ... RENAME COLUMN` (requires 3.25+); single transaction |
| **Default category** | ID 1, name "General" — seeded in migration |
| **Currency codes** | ISO 4217: `BOB`, `USD`, `EUR` (replacing `Bs`, `$`, `€` in code) |
| **Soft delete column** | `deleted_at` (replacing `fecha_delete`) |
| **i18n namespace** | `catalog` prefix for all 45 keys |

---

## Open Questions (Need User Input)

1. **SQLite version**: Confirm target devices run SQLite ≥ 3.25 (supports `RENAME COLUMN`). If not, migration requires table copy approach.
2. **Currency display**: Keep `Bs.`, `$`, `€` symbols in UI? (i18n handles this; code uses ISO codes)
3. **AdMob App ID**: Will be provided at build time or via env var?
4. **Existing v1.0.0 data volume**: Approximate row counts for `productos`/`categorias` to size migration test fixtures?
5. **Rollback requirement**: Is migration rollback needed, or is forward-only with backup restore acceptable?

---

## Validation Plan

| Check | Method |
|---|---|
| TypeScript strict | `tsc --noEmit` passes |
| Lint | `eslint .` passes |
| Unit tests | `jest --testPathPattern=domain` 100% pass |
| Integration tests | `jest --testPathPattern=integration` 100% pass |
| Migration test | `jest --testPathPattern=migration` passes with seeded v1 data |
| FlashList perf | Manual profile: <50ms/frame on device |
| Image size | Automated check: optimized < 40KB for 10 test images |
| i18n coverage | Script: 0 hardcoded Spanish strings in catalog components |

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| `RENAME COLUMN` unsupported | Migration fails on old devices | Detect SQLite version; fallback to table copy if < 3.25 |
| Fractional stock breaks existing UI | Integer-only inputs reject decimals | Update `keyboardType` to `decimal-pad`, `step={0.001}` |
| SKU uniqueness conflicts | Duplicate SKU on create/update | Pre-validate in UI + DB constraint; clear error message |
| Migration long-running | App freeze on startup | Run in background with progress indicator; timeout guard |
| i18n keys missing | Hardcoded strings slip through | CI check: `grep -r "catalog\." --include="*.tsx" src/presentation/components/product` |

---

## Next Steps

1. **User confirms** open questions above (especially SQLite version)
2. **Write `spec.md`** with full v2.0.0 content
3. **Write `plan.md`** with migration checklist
4. **Write `test.md`** with complete test matrix
5. **Call `plan_exit`** with plan file path

---

**Recommended next question:** What is the minimum SQLite version on target devices? (Determines migration approach: `RENAME COLUMN` vs table copy)