# Module Documentation Blueprint — Triad Template

Mandatory location: `/docs/modules/<module-name>/`

Each module must contain exactly three files:

| File | Purpose | Language |
|---|---|---|
| `spec.md` | Business logic, domain rules, DB contracts, UI state machines | English (UI strings marked Spanish) |
| `plan.md` | Implementation steps, component breakdown, migration tasks | English |
| `test.md` | Unit (TDD), integration, cash/stock integrity test suites | English |

## Template: `spec.md`

```markdown
# <Module Name> — Specification

## 1. Bounded Context
<One-line context definition.>

## 2. Domain Entities & Value Objects
| Entity / VO | File | Responsibility |
|---|---|---|
| `<Entity>` | `src/domain/entities/<Entity>.ts` | <Purpose> |

## 3. Domain Rules & Invariants
- **RULE-01:** <Rule statement.>
- **RULE-02:** <Rule statement.>

## 4. Database Contract
```sql
-- Exact DDL / index contract for this module's tables.
```

## 5. Use Cases
| Use Case | File | Atomic? | Preconditions | Postconditions |
|---|---|---|---|---|
| `<UseCase>` | `src/application/<ctx>/<UseCase>.ts` | Yes/No | <List> | <List> |

## 6. UI State Machines (Spanish UI)
| State | Trigger | Next State | Visible String (ES) |
|---|---|---|---|
| `<State>` | `<Action>` | `<State>` | `"Texto en español"` |

## 7. Non-Functional Requirements
- Offline-first SQLite JSI.
- Sub-50ms render (FlashList).
- 0ms cash calculations (accumulator table).
```

## Template: `plan.md`

```markdown
# <Module Name> — Implementation Plan

## 1. Component Breakdown
| Component | File | Type | Memoized? |
|---|---|---|---|
| `<Component>` | `src/presentation/components/<ctx>/<Component>.tsx` | UI | Yes/No |

## 2. Implementation Steps
1. <Step description.>
2. <Step description.>

## 3. Migration Tasks (v1.0 → v2.0)
- [ ] Map existing files to DDD layout.
- [ ] Add module triad under `/docs/modules/<module>/`.
```

## Template: `test.md`

```markdown
# <Module Name> — Test Plan

## 1. Unit Tests (TDD)
| Test | File | Coverage |
|---|---|---|
| `<test>` | `src/__tests__/<ctx>/<test>.test.ts` | <Rule/VO/UseCase> |

## 2. Integration Tests
| Test | File | Scenario |
|---|---|---|
| `<test>` | `src/__tests__/integration/<test>.test.ts` | <Scenario> |

## 3. Cash / Stock Integrity Tests
| Test | Invariant | File |
|---|---|---|
| Revert VENTA restores stock | INV-03 | `src/__tests__/application/ventas/RevertirVenta.test.ts` |
| Revert COMPRA blocks negative stock | INV-02 | `src/__tests__/application/compras/RevertirCompra.test.ts` |
| Revert GASTO adjusts caja | INV-03 | `src/__tests__/application/gastos/RevertirGasto.test.ts` |

## 4. Gate
All tests must pass before module is marked complete.
```
