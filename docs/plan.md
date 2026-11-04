# TPV Móvil Express — Migration Plan v1.0 → v2.0

## 1. Objective

Transition the codebase to a DDD-aligned, modular documentation architecture where every domain module has an isolated triad (`plan.md`, `spec.md`, `test.md`) under `/docs/modules/<module-name>/`. Code remains in `src/`; documentation migrates to `/docs/`.

## 2. Target DDD Folder Structure (src/)

```
src/
├── domain/                    # Pure domain, zero external deps
│   ├── entities/              # Producto, Categoria, Transaccion, DetalleTransaccion, ResumenCaja, CarritoItem
│   ├── value-objects/         # Money, Currency
│   └── rules/                 # stockRules, cartRules, domain exceptions (StockError)
├── application/               # Use cases (orchestrate repositories)
│   ├── ventas/
│   ├── compras/
│   ├── gastos/
│   ├── productos/
│   ├── transacciones/         # RevertirTransaccion
│   ├── reportes/              # ResumenPeriodo
│   └── backup/
├── infrastructure/            # Concrete services (SQLite, SecureStore, expo-print, etc.)
│   ├── database/
│   ├── repositories/          # Sqlite*Repository + container.ts
│   ├── image/
│   ├── pdf/
│   ├── security/
│   ├── settings/
│   └── toast/
└── presentation/              # UI (Expo Router, NativeWind, Zustand)
    ├── app/                   # Expo Router file-based routes
    ├── components/
    ├── hooks/
    ├── stores/
    └── theme/
```

## 3. Documentation Folder Structure (/docs/)

```
/docs/
├── global.spec.md             # High-level spec (bounded contexts, NFRs, invariants)
├── plan.md                    # This migration roadmap
└── modules/
    ├── catalog-inventory/
    │   ├── spec.md
    │   ├── plan.md
    │   └── test.md
    ├── sales-checkout/
    │   ├── spec.md
    │   ├── plan.md
    │   └── test.md
    ├── expenses-cashflow/
    │   ├── spec.md
    │   ├── plan.md
    │   └── test.md
    ├── financial-accumulators/
    │   ├── spec.md
    │   ├── plan.md
    │   └── test.md
    └── system-settings/
        ├── spec.md
        ├── plan.md
        └── test.md
```

## 4. Milestones

| Milestone | Deliverable | Gate |
|---|---|---|
| M1 | `/docs/global.spec.md` + module triad templates | Reviewed |
| M2 | `catalog-inventory` triad (`spec/plan/test`) | Tests pass |
| M3 | `sales-checkout` triad | Cash/stock integrity tests pass |
| M4 | `expenses-cashflow` triad | ConfirmDialog + gasto tests pass |
| M5 | `financial-accumulators` triad | `resumen_caja` atomic read tests pass |
| M6 | `system-settings` triad | PIN/biometric/divisa/backup tests pass |
| M7 | AGENTS.md alignment: remove obsolete paths, add module triad references | All docs consistent |
| M8 | v2.0 baseline frozen | All module tests green |

## 5. Migration Tasks (Code)

- [ ] Verify `src/domain/entities/*` are pure (no Expo/React imports).
- [ ] Confirm `src/application/*` use cases depend only on repository interfaces.
- [ ] Confirm `src/infrastructure/repositories/container.ts` is the single injection point.
- [ ] Map every existing use case to its bounded context module triad.
- [ ] Add missing `test.md` cash/stock integrity suites for revert paths (`VENTA`/`COMPRA`/`GASTO`).

## 6. Success Criteria

- All 142 existing Jest tests continue to pass.
- Every module has exactly `spec.md`, `plan.md`, `test.md` under `/docs/modules/<module>/`.
- AGENTS.md references `/docs/modules/*` triads instead of inline architectural decisions.
- UI strings remain Spanish; all code/DB identifiers remain English.
