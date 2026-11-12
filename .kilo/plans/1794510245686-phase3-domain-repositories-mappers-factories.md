# Phase 3 Plan — Domain Repositories, Mappers, and Factories (v2.0.0)

## 1. Goal
Deliver Phase 3 for TPV Móvil Express v2.0.0: implement/update **mappers**, **SQLite repositories**, **domain factories**, and **repository integration tests** so persistence translates cleanly between v2 English SQLite rows and domain models using Value Objects (`Money`, `Stock`, `SKU`, `PaymentMethod`), with strict TypeScript and atomic transaction boundaries where required.

## 2. Current state (verified)
- **DB/schema/migrations:** v2.0.0 English schema is produced by `runMigrations` (v1 baseline in `src/data/database/schema.sql.ts`, module migration SQL in `src/data/database/migrations/v2_0_0_*`). `client.ts` enables WAL/foreign_keys and runs migrations synchronously.
- **VOs present:** `Money` (`src/domain/value-objects/Money.ts`), `Stock` (`src/domain/value-objects/Stock.ts`), `SKU` (`src/domain/value-objects/SKU.ts`), `PaymentMethod` (`src/domain/entities/PaymentMethod.ts`) exist; `ChangeAmount`/`Currency` also available.
- **Mappers:** `src/data/mappers/rowMappers.ts` maps English columns → legacy Spanish-named domain entities (`Producto`, `Categoria`, `Transaccion`, `DetalleTransaccion`, `ResumenCaja`) using primitives (no VO hydration, no `sku`).
- **Repositories:** `SqliteProductoRepository`, `SqliteCategoriaRepository`, `SqliteTransaccionRepository`, `SqliteResumenCajaRepository` implement legacy Spanish interfaces/DTOs; product repo lacks `sku`/`findBySku`, no `withTransactionSync` on write ops; accumulator repo is read-only.
- **Factories:** no domain factories found under `src/domain`.
- **Tests:** repository integration tests exist in `src/__tests__/data/repositories/*` (Spanish API) using `createMigratedDatabase()` (`better-sqlite3`/`expo-sqlite` mock). No mapper/factory test suites found.
- **Specs/plans:** `docs/modules/*/spec.md|plan.md` define English target entities/repos/VO usage (e.g. `Product`, `IProductRepository.list/findBySku`, `FinancialSummary`, `UpdateAccumulator`), but full rename is cross-cutting (hooks/stores/screens/use cases). Phase 3 must be scoped to avoid breaking `pnpm test`/`tsc`.

## 3. Scope and constraints
- **In scope:** `src/data/mappers/**`, `src/data/repositories/**` (interfaces + Sqlite implementations + `container.ts` as needed), domain factory modules for aggregates/entities covered by Phase 3, `src/__tests__/data/repositories/**` and any new `src/__tests__/data/mappers/**` / `src/__tests__/domain/factories/**` required to verify mapping/persistence/atomic rollback.
- **Out of scope (unless required by compile/tests):** presentation/hooks/stores/screens, application use-case rewrites beyond repository contract compatibility, i18n, migration SQL changes (already complete), AdWare/UI.
- **Non-negotiable:** English identifiers in code/DB; strict TS (`strict`, `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes`); parameterized SQL only; soft-delete filtering (`deleted_at IS NULL`) on active reads; no hard deletes; no secrets.
- **Compatibility strategy (recommended):** preserve existing public repository/DTO names consumed by current tests/app where possible, while adding v2 capabilities (SKU, VO-aware construction, transactional integrity) behind compatible signatures. Introduce English-named v2 types/interfaces/factories alongside legacy adapters if specs require English APIs, but do not break existing callers in this phase.

## 4. Affected boundaries and data flow
1. **SQLite row (English)** → **row mappers** (`toProduct`/`toCategory`/`toSaleTransaction`/`toTransactionDetail`/`toFinancialSummary`, plus legacy aliases as needed) → **domain factories** (validate + construct VOs: `Money(amount,currency)`, `Stock(value)`, `SKU(value|null)`, `PaymentMethod(code)`) → **domain entities/aggregates**.
2. **Application/input DTOs** → **repositories** (validate, normalize with VOs/rules, enforce uniqueness/category/stock constraints) → **SQL writes in `db.withTransactionSync` for multi-statement atomic operations** → **read-back/mapped entity result**.
3. **Accumulator/transaction integrity:** repositories that mutate `sales_transactions`, `transaction_details`, `products.current_stock`, or `financial_accumulators` together must execute as a single transaction; on thrown error SQLite/expo-sqlite must roll back all statements.

## 5. Implementation plan (ordered tasks)

### 5.1 Domain factories
- [ ] Add factory module(s) under `src/domain/factories/` (or module-specific files per architecture conventions), e.g. `ProductFactory`, `CategoryFactory`, `TransactionFactory`/`SaleTransactionFactory`, `ExpenseFactory` (if represented), `FinancialSummaryFactory`.
- [ ] Factories accept raw/DTO inputs + required context (e.g. currency for `Money`) and return fully valid entities/value objects; centralize invariants: non-empty name, `categoryId` required for product, non-negative money/stock, SKU normalization/validation via `new SKU()`, 2dp money / 3dp stock handled by VOs, `deletedAt` nullability preserved.
- [ ] Export factories via a small barrel/index if useful; keep Domain pure (no infra imports).

### 5.2 Mappers (row ↔ domain/VO)
- [ ] Update `src/data/mappers/rowMappers.ts`:
  - Extend row types to include `sku` on product rows and any missing columns; align with migrated schema (`products.sku`, English table/column names already used in SQL).
  - Add v2 mappers that hydrate VOs (e.g. `toProductRow`/`toProduct` producing `Product` with `SKU|null`, `Money`, `Stock` per spec) and/or adapt to current entity shape without primitive leakage in mapping logic.
  - Keep/introduce compatibility mappers (`toProducto`, etc.) if legacy entities remain, implemented via factories/VOs internally to avoid duplicated validation.
  - Handle nullable `image_uri`, `deleted_at`, `detail`; ensure `noUncheckedIndexedAccess`/`exactOptionalPropertyTypes` safe access.
- [ ] Ensure transaction/payment mapping uses `PaymentMethod` VO or validated enum normalization; map stored type/payment codes deterministically.

### 5.3 Repository interfaces and implementations
- [ ] **Product/Catalog** (`IProductoRepository`/`SqliteProductoRepository` or v2 English counterparts):
  - Add SKU support: persist/read `sku`, validate format (VO) and uniqueness among active products (`WHERE deleted_at IS NULL AND sku = ?` excluding current id on update), `findBySku`/equivalent.
  - Validate inputs (category required/exists as applicable, trimmed name, non-negative prices/stock, image URI nullable) and throw clear errors.
  - Use `db.withTransactionSync` for create/update/delete sequences that must be atomic (e.g. uniqueness check + insert/update, or multi-statement writes); ensure read-after-write returns mapped entity.
  - Keep query behavior correct: `deleted_at IS NULL` filters, parameterized LIKE/search, pagination, popularity join respects sale transactions.
- [ ] **Category/Expense categories**: maintain soft-delete semantics and English schema mapping; add factories/mappers consistency.
- [ ] **Sales/Transactions** (`ITransaccionRepository`/`SqliteTransaccionRepository`): align with v2 schema and detail rows; provide methods needed by checkout/expense/accumulator flows if missing (create transaction+details) using `withTransactionSync`; map to entities with Money/PaymentMethod VOs where introduced.
- [ ] **Financial accumulators** (`IResumenCajaRepository`/`SqliteResumenCajaRepository` or `IAccumulatorRepository`): implement `getSummary`/`updateAccumulator` semantics over `financial_accumulators` singleton; updates must be atomic and preserve `net_balance` invariant (`sales - purchases - expenses` deltas) and `updated_at`.
- [ ] **Container** (`src/data/repositories/container.ts`): wire any new repositories/factories/dependencies while preserving existing exports used by app/tests.
- [ ] Prefer repository-level composition/injection of factories/mappers only where it does not violate layer boundaries.

### 5.4 Atomicity and error/rollback behavior
- [ ] Wrap every multi-table/multi-statement write path in `db.withTransactionSync(() => { ... })` (sale/purchase/expense registration, reversal, accumulator delta updates, product stock adjustments coupled to transactions).
- [ ] On validation/constraint/business-rule failure, throw before partial side effects where possible; inside a transaction, thrown errors must result in rollback (verify with integration tests using intentional mid-flow failure).
- [ ] Ensure FK/PRAGMA assumptions remain compatible with migrations/client setup.

### 5.5 Integration tests (`src/__tests__/data/repositories/`)
- [ ] Update existing repository tests to match finalized Phase 3 API/behavior (including SKU column handling, VO-mapped fields if exposed, error messages/codes as applicable) while keeping them green.
- [ ] Add/extend tests to cover:
  - mapper correctness: English row → domain entity/VO hydration, nullable fields (`deleted_at`, `image_uri`, `detail`, `sku`).
  - product create/update with SKU (valid/duplicate/invalid), fractional stock, category required, soft delete visibility.
  - transaction repository read/write paths and detail mapping (as applicable to implemented API).
  - accumulator read and atomic update behavior.
  - **atomic rollback:** force failure inside `withTransactionSync` (e.g. FK violation/duplicate/explicit throw after first write) and assert no persisted partial changes across involved tables.
- [ ] Use `createMigratedDatabase()` fixture; keep tests deterministic and isolated (`beforeEach` fresh DB).

### 5.6 Type safety and cleanup
- [ ] Resolve `exactOptionalPropertyTypes` issues (avoid assigning `undefined` to optional props; use conditional spreads/omit).
- [ ] Add explicit types/readonly where required by public APIs; no `any`.
- [ ] Remove dead imports/legacy mapping paths only if unused and safe.

## 6. Validation plan
- [ ] Run focused tests during development (scoped to changed suites) to keep feedback fast.
- [ ] `pnpm test` — must remain fully green.
- [ ] `pnpm exec tsc --noEmit` (or project typecheck script) — must exit 0.
- [ ] `pnpm lint` if available/time permits in environment (non-blocking in this phase unless requested, but preferred).

## 7. Risks and mitigations
- **Cross-cutting rename risk (spec English APIs vs current Spanish APIs):** can break app/tests. *Mitigation:* compatibility layer/adapters and incremental typing; prioritize green tests/tsc over speculative full rename.
- **Atomic rollback semantics differ between expo-sqlite/better-sqlite3 mocks:** *Mitigation:* write tests against project’s DB test harness and assert observable rollback.
- **SKU/uniqueness edge cases (nulls, soft-deleted rows):** *Mitigation:* partial unique index-aware queries and targeted tests.
- **VO hydration may change runtime shapes:** *Mitigation:* factories/mappers own conversion; keep serialization boundaries explicit.

## 8. Out of scope / follow-ups (post-Phase 3)
- Full domain/entity/interface rename across application/presentation layers per module specs.
- `findBySku`/SKU UI plumbing, expense/accumulator use-case orchestration beyond repo support, E2E flows.
- Additional migrations or schema changes.

## 9. Open questions (none blocking for plan)
- None. If implementation reveals that existing consumers require strict English-only repository APIs immediately, introduce parallel v2 interfaces/classes with adapters in the same phase and document the chosen approach in code/tests.
