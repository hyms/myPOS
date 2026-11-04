# Sales & Checkout — Implementation Plan

## 1. Component Breakdown
| Component | File | Type | Memoized? |
|---|---|---|---|
| `LineItemModal` | `src/presentation/components/cart/LineItemModal.tsx` | UI | No |
| `PaymentModal` | `src/presentation/components/cart/PaymentModal.tsx` | UI | No |
| `CartSummaryBar` | `src/presentation/components/cart/CartSummaryBar.tsx` | UI | No |
| `SearchBar` | `src/presentation/components/filters/SearchBar.tsx` | UI | No |
| `CategoryChipsRow` | `src/presentation/components/filters/CategoryChipsRow.tsx` | UI | No |
| `SortMenu` | `src/presentation/components/filters/SortMenu.tsx` | UI | No |

## 2. Implementation Steps
1. Implement `carritoStore` (Zustand, tipo: 'VENTA' | 'COMPRA', granular selectors).
2. Build FlashList grid (`numColumns={3}`, `ProductGridCard` memoized).
3. Implement `LineItemModal` (cantidad numérica, descuento fijo <= subtotal).
4. Implement `PaymentModal` (EFECTIVO → monto recibido + cambio live via useMemo).
5. Implement `ComprobanteRenderer` HTML template + print/share.
6. Wire `RegistrarVenta` use case (atomic transaction, stock alert post-commit).

## 3. Migration Tasks (v1.0 → v2.0)
- [ ] Add `spec.md`, `plan.md`, `test.md` to `/docs/modules/sales-checkout/`.
- [ ] Confirm `RegistroSegmented` sub-tab VENTA/COMPRA routes.
