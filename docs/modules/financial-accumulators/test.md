# Financial Accumulators — Test Plan

## 1. Unit Tests (TDD)
| Test | File | Coverage |
|---|---|---|
| ResumenPeriodo.buildResumen | `src/__tests__/application/reportes/ResumenPeriodo.test.ts` | neto = ventas - compras - gastos |
| SqliteResumenCajaRepository.obtener | `src/__tests__/data/repositories/SqliteResumenCajaRepository.test.ts` | Single row read |

## 2. Integration Tests
| Test | File | Scenario |
|---|---|---|
| Revert VENTA restores stock+caja | `src/__tests__/application/ventas/RevertirTransaccion.test.ts` | Stock +, total_ventas -, saldo - |
| Revert COMPRA blocks negative stock | `src/__tests__/application/compras/RegistrarCompra.test.ts` | StockError if stock_actual - cantidad < 0 |
| Revert GASTO adjusts caja | `src/__tests__/application/gastos/RegistrarGasto.test.ts` | total_gastos -, saldo + |

## 3. Cash / Stock Integrity Tests
| Test | Invariant | File |
|---|---|---|
| Atomic rollback on write failure | INV-03 | `src/__tests__/application/transacciones/RevertirTransaccion.test.ts` |
| resumen_caja singleton id=1 | INV-06 | `src/__tests__/data/repositories/SqliteResumenCajaRepository.test.ts` |

## 4. Gate
All tests pass before module marked complete.
