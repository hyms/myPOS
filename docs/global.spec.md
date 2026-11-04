# TPV Móvil Express — Global Specification v2.0

## 1. Business Goals & Value Proposition

**Product:** TPV Móvil Express — single-user, single-branch mobile Point of Sale (mPOS).

**Core value proposition:** Enable small merchants to record sales, purchases, and expenses entirely offline on a mobile device, with atomic cash/stock integrity and instant receipt printing/sharing.

**Target user persona:** Single-owner microbusiness (retail, food, services) operating one branch, no back-office, no cloud dependency, Spanish UI.

**Non-goals:** Multi-user, multi-branch, online sync, e-commerce, mixed-payment transactions.

---

## 2. Bounded Contexts (Domain Map)

| Bounded Context | Responsibility | Core Aggregates |
|---|---|---|
| `catalog-inventory` | Product lifecycle, categories, stock levels, stock alerts | Producto, Categoria |
| `sales-checkout` | Shopping cart, sales registration, payment methods, change calculation | CarritoItem, Venta |
| `expenses-cashflow` | Expense registration, confirmation dialog | Gasto |
| `financial-accumulators` | Cash summary accumulator, transaction history, period reports | ResumenCaja, Transaccion, DetalleTransaccion |
| `system-settings` | PIN/biometric security, currency (i18n), pagination, backup/restore | Settings, Session |

**Shared Kernel:** Value objects `Money`, `Currency`; strict TypeScript entities; `fecha_delete` soft-delete convention; SQLite `resumen_caja(id=1)` accumulator table.

---

## 3. Global Non-Functional Requirements

| ID | Requirement | Target |
|---|---|---|
| NFR-01 | Offline-first SQLite via `expo-sqlite` JSI (`openDatabaseSync`, `withTransactionSync`) | Local-first, zero network dependency |
| NFR-02 | Sub-50ms list render for catalog grid (`@shopify/flash-list`, `numColumns={3}`) | <50ms per frame |
| NFR-03 | 0ms cash calculations — read from `resumen_caja` accumulator table only | No runtime aggregations for caja |
| NFR-04 | Paginated FlashList `LIMIT ? OFFSET ?` with settings-driven `pageSize` | Default 15 rows |
| NFR-05 | Local image optimization — 400x400 JPEG 70% target <40KB, persisted in `documentDirectory/productos/` | <40KB per product image |
| NFR-06 | Strict dark mode default with runtime design token support for future themes | NativeWind v4 + tokens |
| NFR-07 | Atomic rollback — `withTransactionSync` guarantees zero cash/stock drift on any write failure | All-or-nothing |
| NFR-08 | Spanish UI strings via i18n; all code/entity/DB identifiers in English | i18n for visible strings |
| NFR-09 | Strict TypeScript (`strict`, `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes`) | Zero `any` |
| NFR-10 | Tests: 100% pass on unit + integration + cash/stock integrity suites | Jest |

---

## 4. System Invariants

- **INV-01 (Stock non-negative):** `stock_actual - cantidad >= 0` enforced before any `VENTA` write; rejected otherwise.
- **INV-02 (Revert stock safety):** Reverting `COMPRA` that would drive `stock_actual < 0` throws `StockError` before transaction opens.
- **INV-03 (Atomic rollback math):** Deleted `VENTA` restores `stock_actual += cantidad` and decrements `total_ventas`/`saldo_actual`; deleted `COMPRA` decrements `stock_actual` and increments `total_compras`/`saldo_actual`; deleted `GASTO` increments `total_gastos`/`saldo_actual`. All inside `db.withTransactionSync`.
- **INV-04 (Single payment per transaction):** Exactly one `tipo_pago` per `transaccion`. No mixed payments.
- **INV-05 (Soft delete only):** `fecha_delete` marks deletion; all active queries filter `WHERE fecha_delete IS NULL`.
- **INV-06 (ResumenCaja singleton):** `resumen_caja` has exactly one row (`id=1`, `CHECK (id=1)`); never deleted.
- **INV-07 (Categoría obligatoria):** `categoria_id` NOT NULL on create/update; validated in form and repository.
- **INV-08 (PIN gate):** PIN change and transaction deletion require current PIN. Backup restore does NOT require PIN.
- **INV-09 (Image persistence):** Only final optimized URI stored in `documentDirectory/productos/<uuid>.jpg`; relative URI saved in SQLite.
- **INV-10 (Divisa visual-only):** Currency affects display formatting (`Intl.NumberFormat`); SQLite stores raw numeric amounts in base currency.

---

## 5. Module Triad Convention

Every domain module lives at `/docs/modules/<module-name>/` with exactly three files:

- `spec.md` — Business logic, domain rules, DB contracts, UI state machines (Spanish UI strings noted).
- `plan.md` — Implementation steps, component breakdown, migration tasks.
- `test.md` — Unit (TDD), integration, cash/stock integrity test suites.
