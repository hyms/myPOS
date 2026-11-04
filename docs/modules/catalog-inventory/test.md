# Catalog & Inventory — Test Plan

## 1. Unit Tests (TDD)
| Test | File | Coverage |
|---|---|---|
| Producto entity validation | `src/__tests__/domain/entities/Producto.test.ts` | Fields, readonly |
| stockRules.validateStockDisponible | `src/__tests__/domain/rules/stockRules.test.ts` | Non-negative stock |
| SqliteProductoRepository.crear | `src/__tests__/data/repositories/SqliteProductoRepository.test.ts` | Categoria obligatory |

## 2. Integration Tests
| Test | File | Scenario |
|---|---|---|
| Create product with image | `src/__tests__/integration/productos/create-image.test.ts` | Image pipeline end-to-end |
| Soft delete hides from catalog | `src/__tests__/integration/productos/soft-delete.test.ts` | fecha_delete filter |

## 3. Cash / Stock Integrity Tests
| Test | Invariant | File |
|---|---|---|
| Stock below min triggers alert | RULE-02 / useStockAlert | `src/__tests__/presentation/hooks/useStockAlert.test.ts` |

## 4. Gate
All tests pass before module marked complete.
