# TPV Móvil Express — Master Implementation & Migration Plan v2.0

## 1. Executive Summary & Migration Objectives

This document establishes the strategic execution roadmap for transitioning **TPV Móvil Express** from its initial v1.0 release into an enterprise-grade, highly maintainable v2.0 application built upon solid engineering principles (SOLID, DRY, KISS, YAGNI, Low Coupling) and proven design patterns (Singleton, Strategy, Composite, Observer, Repository, Factory).

### Core Objectives:

1. **Domain Isolation & Pattern Alignment (DDD):** Refactor business logic out of UI components into standalone TypeScript entities, value objects, domain factories, and repository interfaces within `src/domain/` and `src/application/`.
2. **Specification-Driven Development (SDD):** Establish and enforce isolated document triads (`spec.md`, `plan.md`, `test.md`) across all core application modules under `/docs/modules/`.
3. **Strict Language Governance:** Ensure 100% of codebase identifiers, database schemas, and documentation are written in English, while UI strings are fully localized in Spanish via an i18n layer.
4. **Performance & Monetization Safeguards:** Enforce strict Dark Mode, sub-50ms render loops with `@shopify/flash-list`, NativeWindUI adoption, and non-intrusive AdMob banner/interstitial governance using Strategy & Singleton patterns.

---

## 2. Master Migration Phasing Roadmap

```
┌────────────────────────────────────────────────────────────────────────┐
│ Phase 1: Global Foundation & Architecture Governance (Completed)       │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│ Phase 2: Core Domain, Design Patterns & Data Layer Refactoring         │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│ Phase 3: Module Triads Rollout (SDD / TDD per Context)                 │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│ Phase 4: UI Refactoring & NativeWindUI Integration                    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│ Phase 5: Verification, Benchmarking & Release Packaging (v2.0 Build)   │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Detailed Phase Breakdown

### Phase 1: Global Foundation & Documentation Governance (Completed)

- [x] Create `/docs/global.spec.md` (Business rules, domain vision, invariants).
- [x] Create `/docs/readme.md` (Technical stack, NFRs, Design Patterns, SOLID Principles, NativeWindUI, AdMob governance).
- [x] Create `/docs/plan.md` (This master execution roadmap).

### Phase 2: Domain Layer, Design Patterns & Data Layer Refactoring (DDD Layering)

- [ ] Establish core Domain Entities & Factories (`src/domain/entities/`, `src/domain/factories/`):
  - `Product.ts`, `Category.ts`, `Transaction.ts`, `CartItem.ts`, `CashSummary.ts`.
  - Implement **Factory Pattern** (`TransactionFactory`) to enforce valid domain creation.
- [ ] Implement Value Objects (`src/domain/value-objects/`):
  - `Money.ts`, `Stock.ts`, `SecurityPIN.ts`, `Currency.ts`.
- [ ] Define Repository Interfaces (**Repository Pattern**) (`src/domain/ports/`):
  - `IProductRepository.ts`, `ITransactionRepository.ts`, `ICashRegisterRepository.ts`.
- [ ] Implement Infrastructure Adapters & Patterns (`src/infrastructure/`):
  - **Singleton Pattern:** `SQLiteDatabaseManager` and `AdMobService`.
  - **Strategy Pattern:** `ISharingStrategy` (`WhatsAppTextSharingStrategy`, `SingleProductImageSharingStrategy`).
  - Refactor SQLite Persistence Layer with soft delete (`fecha_delete`) and atomic wrappers (`db.withTransactionSync`).

### Phase 3: SDD Module Triad Rollout & TDD Execution

Deploy isolated documentation and unit tests for each context prior to UI coupling:

1. **`catalog-inventory` Module:**
   - [ ] Write `docs/modules/catalog-inventory/spec.md`
   - [ ] Write `docs/modules/catalog-inventory/plan.md`
   - [ ] Write `docs/modules/catalog-inventory/test.md` (Jest suite for non-negative stock invariants).

2. **`sales-checkout` Module:**
   - [ ] Write `docs/modules/sales-checkout/spec.md`
   - [ ] Write `docs/modules/sales-checkout/plan.md`
   - [ ] Write `docs/modules/sales-checkout/test.md` (Cart calculations, change math, single-payment validation).

3. **`expenses-cashflow` Module:**
   - [ ] Write `docs/modules/expenses-cashflow/spec.md`
   - [ ] Write `docs/modules/expenses-cashflow/plan.md`
   - [ ] Write `docs/modules/expenses-cashflow/test.md` (Expense registration and deletion rollback).

4. **`accumulators` (Financial Summary) Module:**
   - [ ] Write `docs/modules/accumulators/spec.md`
   - [ ] Write `docs/modules/accumulators/plan.md`
   - [ ] Write `docs/modules/accumulators/test.md` (Singleton `resumen_caja` invariants).

5. **`settings` & `chat-commerce` Modules:**
   - [ ] Write `docs/modules/settings/` triad.
   - [ ] Write `docs/modules/chat-commerce/` triad (Individual/Batch sharing strategy + image generator).

### Phase 4: Presentation Layer Modernization & Performance Optimization

- [ ] Integrate **NativeWindUI** accessible component collection.
- [ ] Enforce Spanish i18n string dictionary (`src/infrastructure/i18n/es.ts`).
- [ ] Apply **Observer Pattern** for reactive Cash Register state updates.
- [ ] Optimize Product Grid using `@shopify/flash-list` with `estimatedItemSize` and `getItemType`.
- [ ] Implement local image processing pipeline (`expo-image` <40KB target).
- [ ] Implement AdMob banner wrappers (`AD_ENABLED` state binding) and pre-loading interstitial flow.

### Phase 5: Testing, Validation & Local Build Pipeline

- [ ] Execute 100% pass on Jest unit test suite (Stock integrity, atomic rollback math, repository mocks).
- [ ] Validate sub-50ms render metrics during heavy continuous catalog scrolling.
- [ ] Run prebuild generation:
  ```bash
  npx expo prebuild --clean
  ```
- [ ] Compile local Android release APK:
  ```bash
  cd android && ./gradlew assembleRelease
  ```

---

## 4. Risk Management & Acceptance Criteria

| Risk Factor | Impact | Mitigation Strategy |
| :--- | :--- | :--- |
| **Data Migration from v1.0 Schema** | High | Write lightweight SQLite migration script (`v1_to_v2_migration.sql`) renaming legacy Spanish columns to English identifiers upon database initialization. |
| **UI Jitter on Heavy Catalogs** | Medium | Mandate `expo-image` GPU caching and explicit `estimatedItemSize` props on all virtualized FlashLists. |
| **Ad Load Failure / UI Shift** | Low | Wrap AdBanner in fixed-height container; unmount gracefully if `AD_ENABLED` is `false`. |

---

## 5. Next Immediate Action

Proceed to Phase 3 by initializing the first module triad under `/docs/modules/catalog-inventory/` (`spec.md`, `plan.md`, and `test.md`).