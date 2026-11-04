# Expenses & Cash Flow — Implementation Plan

## 1. Component Breakdown
| Component | File | Type | Memoized? |
|---|---|---|---|
| `ConfirmDialog` | `src/presentation/components/feedback/ConfirmDialog.tsx` | UI | No |

## 2. Implementation Steps
1. Implement `RegistrarGasto` use case (atomic: GASTO insert + caja update).
2. Build gasto form (monto + descripcion).
3. Wire `ConfirmDialog` with interpolated Spanish text.
4. Implement `useGastos` (mes/año filter, pagination).

## 3. Migration Tasks (v1.0 → v2.0)
- [ ] Add `spec.md`, `plan.md`, `test.md` to `/docs/modules/expenses-cashflow/`.
- [ ] Confirm `app/gastos/index.tsx` and `app/gastos/nuevo.tsx` routes.
