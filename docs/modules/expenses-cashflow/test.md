# Expenses & Cash Flow — Test Plan

> **Status: legacy Jest suite (RN reference).** This document describes the original React Native test matrix. The KMP target suite lives in `kmp-app/composeApp/src/commonTest` and runs via `./gradlew :composeApp:testAndroidHostTest`. Porting these cases to Kotlin is tracked in `/docs/MIGRATION_CHECKLIST.md` §7.3.7.

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
