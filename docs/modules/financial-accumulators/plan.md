# Financial Accumulators — Implementation Plan

## 1. Component Breakdown
| Component | File | Type | Memoized? |
|---|---|---|---|
| `Stat` | `src/presentation/components/reports/Stat.tsx` | UI | No |
| `ReportCard` | `src/presentation/components/reports/ReportCard.tsx` | UI | No |
| `ReportRow` | `src/presentation/components/reports/ReportRow.tsx` | UI | No |
| `BreakdownBar` | `src/presentation/components/reports/BreakdownBar.tsx` | UI | No |

## 2. Implementation Steps
1. Implement `useResumenCaja` (single row read, no aggregation).
2. Implement `ResumenPeriodo` use case (mes/año reports via repository).
3. Implement `RevertirTransaccion` use case (atomic revert for VENTA/COMPRA/GASTO).
4. Build caja screen with Stat cards + periodo breakdown.
5. Wire `usePinGate` for transaction deletion.

## 3. Migration Tasks (v1.0 → v2.0)
- [ ] Add `spec.md`, `plan.md`, `test.md` to `/docs/modules/financial-accumulators/`.
- [ ] Consolidate `RevertirVenta`/`RevertirCompra`/`RevertirGasto` into `RevertirTransaccion`.
- [ ] Rename `ResumenMesAnio` → `ResumenPeriodo`.
