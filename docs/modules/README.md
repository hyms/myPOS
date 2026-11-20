# Module Triads — v2.0

Each directory contains `spec.md`, `plan.md`, and `test.md` per the blueprint at `/docs/modules/_blueprint/README.md`.

| Module | Bounded Context | Implementation Status | Parity Status |
|---|---|---|---|
| `catalog-inventory` | Catalog & Inventory | Phase 0–1 complete (create-only UI) | Edit/soft-delete, grid, stock adjust, images pending → Phase 6 |
| `sales-checkout` | Sales & Checkout | Phase 2 complete (sale path) | Purchase UI, payment dialog, receipts pending → Phase 6 |
| `expenses-cashflow` | Expenses & Cash Flow | Phase 4 complete (basic list/form) | Category selector, payment method, month filter pending → Phase 6 |
| `financial-accumulators` | Cuentas / Accumulators | Phase 3 complete (summary + revert use case) | Reports, movements list, detail, PM breakdown pending → Phase 6 |
| `system-settings` | System / Settings | Phase 5 complete (settings + PIN services) | PIN UI, biometric, backup/import, i18n pending → Phase 6 |

Authoritative per-item tracking: `/docs/MIGRATION_CHECKLIST.md`.
