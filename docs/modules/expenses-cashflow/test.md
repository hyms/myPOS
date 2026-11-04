# Expenses & Cash Flow — Test Plan

## 1. Unit Tests (TDD)
| Test | File | Coverage |
|---|---|---|
| registrarGasto validation | `src/__tests__/application/gastos/RegistrarGasto.test.ts` | monto > 0, descripcion required |

## 2. Integration Tests
| Test | File | Scenario |
|---|---|---|
| GASTO caja update | `src/__tests__/integration/gastos/caja.test.ts` | total_gastos +, saldo_actual - |

## 3. Cash / Stock Integrity Tests
| Test | Invariant | File |
|---|---|---|
| GASTO adjusts resumen_caja | INV-03 | `src/__tests__/application/gastos/RegistrarGasto.test.ts` |

## 4. Gate
All tests pass before module marked complete.
