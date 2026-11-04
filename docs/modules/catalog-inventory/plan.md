# Catalog & Inventory — Implementation Plan

## 1. Component Breakdown
| Component | File | Type | Memoized? |
|---|---|---|---|
| `ProductGridCard` | `src/presentation/components/product/ProductGridCard.tsx` | UI | Yes |
| `ProductFallbackAvatar` | `src/presentation/components/product/ProductFallbackAvatar.tsx` | UI | Yes |
| `ProductFormHeader` | `src/presentation/components/product/ProductFormHeader.tsx` | UI | No |
| `CategorySelector` | `src/presentation/components/product/CategorySelector.tsx` | UI | No |
| `QuickCategoryModal` | `src/presentation/components/product/QuickCategoryModal.tsx` | UI | No |

## 2. Implementation Steps
1. Implement `ImageOptimizer` (400x400 JPEG 70% <40KB) + `ImageStorage`.
2. Build `ProductFormHeader` (camera/galería FIRST, then fields).
3. Wire `CategorySelector` with [+] opening `QuickCategoryModal`; draft state in `productoDraftStore`.
4. Implement `useStockAlert` (Toast if `stock_actual < stock_minimo`).
5. Add `GestionProductos` use case (create/update/soft-delete).

## 3. Migration Tasks (v1.0 → v2.0)
- [ ] Move product use cases under `src/application/productos/GestionProductos.ts`.
- [ ] Add `spec.md`, `plan.md`, `test.md` to `/docs/modules/catalog-inventory/`.
- [ ] Ensure `Categoria` obligatory validation in repository + form.
