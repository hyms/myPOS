# Documentation Index — TPV Móvil Express v2.0

| File | Purpose |
|---|---|
| `/docs/global.spec.md` | High-level spec: bounded contexts, NFRs, invariants |
| `/docs/plan.md` | Migration roadmap v1.0 → v2.0 |
| `/docs/modules/_blueprint/README.md` | Triad template (spec/plan/test) |
| `/docs/modules/README.md` | Module inventory |

## Module Triads
- `catalog-inventory/` — spec, plan, test
- `sales-checkout/` — spec, plan, test
- `expenses-cashflow/` — spec, plan, test
- `financial-accumulators/` — spec, plan, test
- `system-settings/` — spec, plan, test

## Conventions
- Code/architecture language: English.
- UI visible strings: Spanish (i18n).
- Default theme: Strict Dark Mode with runtime design token support.
- All tests: Jest, must pass before module is complete.
