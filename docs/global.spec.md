# TPV Móvil Express — Technical Architecture & Stack Reference v2.0

## 1. System Architecture Overview

TPV Móvil Express v2.0 is built as an offline-first, high-performance mobile application engineered under **Domain-Driven Design (DDD)**, **Specification-Driven Development (SDD)**, and **Test-Driven Development (TDD)** principles.

```
                    ┌─────────────────────────────────────────┐
                    │       Presentation Layer (React Native) │
                    │ Expo 56 + NativeWind v4 + NativeWindUI  │
                    │            + FlashList                  │
                    └────────────────────┬────────────────────┘
                                         │
                    ┌────────────────────▼────────────────────┐
                    │      Application & Domain Layer         │
                    │   Use Cases + Aggregate Entities (TS)   │
                    └────────────────────┬────────────────────┘
                                         │
                    ┌────────────────────▼────────────────────┐
                    │    Infrastructure Layer (Persistence)   │
                    │  expo-sqlite (JSI) + expo-image (Cache) │
                    └─────────────────────────────────────────┘
```

- **Domain Isolation:** Pure TypeScript logic with zero React Native or UI dependencies inside `src/domain/`.
- **Language Policy:**
  - All code, identifiers, database schema, variables, and architectural documentation must be in **English**.
  - All user interface strings, messages, labels, and receipts are localized in **Spanish (i18n)**.

---

## 2. Technology Stack & Core Tooling

| Category | Technology / Library | Version / Detail |
| :--- | :--- | :--- |
| **Framework** | Expo / React Native | Expo SDK 56 (New Architecture enabled) |
| **Database** | `expo-sqlite` | JSI bindings (`openDatabaseSync`, `withTransactionSync`) |
| **UI Components** | **NativeWindUI** | Production-ready native accessible components built for NativeWind |
| **Styling & Theme** | NativeWind | v4 (Tailwind CSS engine with runtime design tokens & Dark Mode) |
| **List Engine** | `@shopify/flash-list` | Virtualized performance lists for grid & row layouts |
| **Media Pipeline** | `expo-image` + `expo-file-system` | Hardware-accelerated image caching & disk pipeline |
| **Local Sharing** | `expo-sharing` | Native intents for direct WhatsApp/Chat catalog export |
| **Language & Types** | TypeScript | v5+ (Strict mode enabled, `noUncheckedIndexedAccess`) |
| **Testing Suite** | Jest + React Native Testing Library | Unit tests (TDD), use case contracts, DB integrity |

---

## 3. Engineering Principles & Software Design Patterns

### A. Software Engineering Principles

- **SRP (Single Responsibility Principle):** Each module, class, and custom hook has a single, well-defined responsibility. UI components delegate business math exclusively to Use Cases.
- **OCP (Open/Closed Principle):** Systems (such as report generation or catalog formatters) are open for extension through interfaces but closed for direct code modification.
- **DIP (Dependency Inversion Principle):** High-level Application Use Cases depend on abstractions (domain ports/interfaces), never on low-level infrastructure details (SQLite driver, Expo SDK directly).
- **DRY (Don't Repeat Yourself):** Reusable NativeWindUI design primitives and centralized currency/date formatters eliminate code duplication.
- **KISS (Keep It Simple, Stupid):** 100% offline-first architecture without cloud complexity, server synchronization, or unnecessary GraphQL wrappers.
- **YAGNI (You Ain't Gonna Need It):** Strict scope boundaries: zero multi-branch, multi-user roles, or cloud payment gateways in this version.
- **Low Coupling & High Cohesion:** The Presentation layer only invokes Use Cases and remains completely agnostic of the underlying database engine.

### B. Core Design Patterns

- **Singleton Pattern:** Applied to `SQLiteDatabaseManager` (JSI connection instance), `AdMobService` (preventing listener leaks), and the single-row `resumen_caja` accumulator table (`id = 1`).
- **Strategy Pattern:** Implemented via `ISharingStrategy` for catalog export (`WhatsAppTextSharingStrategy` for text batches vs `SingleProductImageSharingStrategy` for rendered image cards) and dynamic ad placement policies.
- **Composite Pattern:** Utilized in the domain cart to treat individual products (`CartItem`) and bundled products under a unified interface (`ICartItem`).
- **Observer Pattern:** React state bindings and domain event listeners update the Cash Register (`resumen_caja`) screen instantly whenever a transaction mutates state without triggering full tree re-renders.
- **Repository Pattern:** Encapsulates persistence logic behind abstract domain contracts (`IProductRepository`, `ITransactionRepository`), enabling instantaneous in-memory mocking for zero-latency TDD execution.
- **Factory Pattern:** Encapsulates entity instantiation (`TransactionFactory.createSale(...)`) to guarantee that created aggregates satisfy domain invariants prior to persistence.

---

## 4. Global Non-Functional Requirements (NFRs)

- **NFR-01 (Offline-First Execution):** Zero cloud runtime dependency. All reads, writes, and image transformations execute locally via native JSI SQLite bindings.
- **NFR-02 (Sub-50ms List Rendering & Performance):**
  - Powered by `@shopify/flash-list` (`getItemType`, explicit `estimatedItemSize`, recycling keys).
  - Strict use of `React.memo`, `useMemo`, and `useCallback` to prevent unnecessary component re-renders.
  - Inputs (search, quantities) use localized states or uncontrolled refs to avoid global UI thread blocking.
  - Images rendered exclusively via `expo-image` to offload decoding and memory caching to native GPU threads.
- **NFR-03 (0ms Cash Aggregation):** Financial balance reads (`resumen_caja`) query a single-row singleton accumulator table (`id = 1`) to eliminate runtime `SUM()` calculation bottlenecks.
- **NFR-04 (Image Storage & Memory Optimization):**
  - Input images processed via local pipeline: resized to maximum **400x400px**, JPEG 70% quality, targeting **<40KB** per image file.
  - Saved in app storage (`documentDirectory/productos/<uuid>.jpg`). Database stores relative paths only.
- **NFR-05 (Strict Dark Mode Default):** Primary UI defaults to "Strict Dark Mode" (obsidian/dark charcoal tones with high-contrast accent colors). System theme integration and component styling accelerated via NativeWindUI and NativeWind tokens.
- **NFR-06 (Transactional Atomicity):** Multi-table mutations (e.g., creating a sale while updating stock and adjusting cash summary) MUST execute within `db.withTransactionSync` blocks. Failure triggers immediate rollback.

---

## 5. Monetization & Adware Governance

- **Global Control Toggle:** Controlled via a central boolean configuration variable (`AD_ENABLED` in settings/config). When disabled, ad components unmount completely to free resources.
- **Allowed Screens (Banners):** Permitted **only** in non-critical auxiliary views:
  - Inventory management (`inventory`)
  - Application settings (`settings`)
  - Financial reports (`reports`)
- **Restricted Screens (Zero Ads):** **Strictly forbidden** in high-focus transactional screens to protect merchant efficiency and avoid user distraction:
  - Shopping Cart & Checkout (`cart`)
  - Purchases & Supplier entries (`purchases`)
- **Catalog Sharing Ad Behavior:** In the catalog sharing screen, ads launch as a full-screen interstitial overlay that automatically transitions into a small banner after 3 to 5 seconds.
- **Performance Protection:** Banners must use fixed-height containers to prevent layout shifts, and listeners must be properly cleaned up on unmount to prevent memory leaks.

---

## 6. Directory & Codebase Structure

```
/
├── docs/
│   ├── global.spec.md           # Business specification & domain vision
│   ├── readme.md                # Technical stack & architecture (This file)
│   ├── plan.md                  # Master roadmap & migration tasks
│   └── modules/                 # Module Triad Specs
│       ├── catalog-inventory/   # (spec.md, plan.md, test.md)
│       ├── sales-checkout/      # (spec.md, plan.md, test.md)
│       ├── expenses-cashflow/   # (spec.md, plan.md, test.md)
│       ├── accumulators/        # (spec.md, plan.md, test.md)
│       └── settings/            # (spec.md, plan.md, test.md)
│
├── src/
│   ├── domain/                  # Entities, Value Objects, Repository Interfaces, Factories
│   ├── application/             # Use Cases & DTOs
│   ├── infrastructure/          # SQLite Repositories, FileSystem, Native Drivers, Strategies
│   └── presentation/           # React Native UI (NativeWindUI, Components, Screens, i18n)
└── __tests__/                   # Unit & Integration Test Suites
```

---

## 7. Development & Build Standards

### TypeScript Strictness
```json
{
  "compilerOptions": {
    "strict": true,
    "noImplicitAny": true,
    "strictNullChecks": true,
    "noUncheckedIndexedAccess": true,
    "exactOptionalPropertyTypes": true
  }
}
```

### Local APK Build Pipeline
1. Execute native prebuild generation:
   ```bash
   npx expo prebuild --clean
   ```
2. Build Android release binary locally:
   ```bash
   cd android && ./gradlew assembleRelease
   ```
   *Output APK location:* `android/app/build/outputs/apk/release/app-release.apk`