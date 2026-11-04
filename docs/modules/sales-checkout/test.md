# Sales & Checkout — Test Plan

## 1. Unit Tests (TDD)
| Test | File | Coverage |
|---|---|---|
| cartRules.calcTotals | `src/__tests__/domain/rules/cartRules.test.ts` | Total, discount |
| carritoStore selectors | `src/__tests__/stores/carritoStore.test.ts` | Granular selectors |

## 2. Integration Tests
| Test | File | Scenario |
|---|---|---|
| RegistrarVenta atomic | `src/__tests__/application/ventas/RegistrarVenta.test.ts` | Stock -, caja + |
| Payment change calculation | `src/__tests__/integration/cart/change.test.ts` | EFECTIVO cambio >= 0 / < 0 |

## 3. Cash / Stock Integrity Tests
| Test | Invariant | File |
|---|---|---|
| Venta blocks negative stock | INV-01 | `src/__tests__/application/ventas/RegistrarVenta.test.ts` |
| Venta updates resumen_caja | INV-03 | `src/__tests__/application/ventas/RegistrarVenta.test.ts` |

## 4. Gate
All tests pass before module marked complete.
