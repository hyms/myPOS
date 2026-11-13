# AGENTS.md — Master Guidelines for AI Coding Agents (v2.0.0)

This document defines the architectural governance, coding standards, and cross-module contracts that all AI agents must follow when working on the **TPV Móvil Express** codebase.

---

## 0. Repository Layout (Dual-App Workspace)

| Path | Purpose |
|---|---|
| `rn-app/` | Legacy React Native + Expo application (TypeScript, Expo Router, NativeWind, SQLite JSI) |
| `kmp-app/` | New Kotlin Multiplatform + Compose Multiplatform application (SQLDelight) |
| `docs/` | Master architectural specs and plans shared by both apps |
| `scripts/` | Root build, migration, and automation scripts |
| `AGENTS.md`, `README.md` | Workspace-level governance and documentation |

**Rules:**
- All RN/Expo code, configs, tests, and tooling live under `rn-app/`; run `pnpm` commands from that directory.
- All Kotlin/Gradle code lives under `kmp-app/`; run Gradle commands from that directory.
- Root git assets (`.git/`, `.gitignore`, `.github/`) stay at the root.
- Documentation paths in this file and in module specs remain root-relative (e.g. `/docs/...`).

---

## 1. Versioning & Release Governance

| Standard | Value |
|---|---|
| **Semantic Versioning** | 2.0.0 (MAJOR.MINOR.PATCH) |
| **Module Version** | Every module at `v2.0.0` |
| **Breaking Changes** | Require MAJOR bump; coordinated via `migrations.ts` `user_version` |
| **Branch Strategy** | `main` = production-ready; feature branches per module |

---

## 2. Language Governance (STRICT)

| Layer | Language | Enforcement |
|---|---|---|
| **Code identifiers** (variables, functions, classes, types, interfaces) | **English only** | ESLint + TypeScript compile |
| **Database** (tables, columns, indexes, constraints) | **English only** | Migration DDL |
| **API / DTOs / Repository interfaces** | **English only** | TypeScript types |
| **UI strings** (user-visible text) | **Spanish only** via i18n keys | `i18n/es/*.json` namespaces |

> **Zero tolerance:** No Spanish in code/DB. No English in UI strings. Violations fail CI.

---

## 3. Architecture: Clean Architecture + DDD (4 Layers)

```
┌─────────────────────────────────────────────────────────────┐
│  PRESENTATION (React Native / Expo Router)                 │
│  Screens, Components, Hooks, Zustand Stores, i18n          │
├─────────────────────────────────────────────────────────────┤
│  APPLICATION (Use Cases / Orchestration)                   │
│  RegisterSale, RegisterExpense, GetCashSummary, etc.       │
├─────────────────────────────────────────────────────────────┤
│  DOMAIN (Pure TypeScript — Zero External Dependencies)     │
│  Entities, Value Objects (Money, Currency, Stock, SKU),    │
│  Domain Rules, Repository Interfaces (Ports)               │
├─────────────────────────────────────────────────────────────┤
│  INFRASTRUCTURE (Concrete Implementations)                 │
│  SQLite Repositories, SecureStore, FileSystem, AdMob,      │
│  ImageOptimizer, Print, Sharing, Biometric, SettingsStore  │
└─────────────────────────────────────────────────────────────┘
```

### Dependency Rule
- **Presentation** → **Application** → **Domain** ← **Infrastructure**
- Domain has **zero** imports from outer layers
- Infrastructure implements Domain interfaces (Repository Ports)

---

## 4. Core Technical Stack (v2.0.0)

| Category | Technology | Version / Config |
|---|---|---|
| **Framework** | Expo SDK 56 (Managed Workflow) | TypeScript strict |
| **Router** | Expo Router v5 (file-based) | |
| **UI Styling** | NativeWind v4 + Tailwind CSS | Design tokens in `rn-app/src/presentation/theme/` |
| **Lists** | `@shopify/flash-list` v2 | `numColumns={3}` (catalog), `{1}` (expenses) |
| **Database** | `expo-sqlite` (JSI sync API) | `openDatabaseSync`, `withTransactionSync` |
| **State** | Zustand | Granular selectors, manual invalidation |
| **Auth** | `expo-secure-store` (PIN hash), `expo-local-authentication` (biometric) | |
| **Images** | `expo-image-manipulator` + `expo-file-system` + `expo-image` | 400×400, JPEG 70%, <40KB |
| **Print/Share** | `expo-print` + `expo-sharing` | HTML receipts |
| **Backup** | `expo-document-picker` + `expo-file-system` | |
| **Dates** | `dayjs` | |
| **Toasts** | `react-native-toast-message` | |
| **Package Manager** | `pnpm` | |

---

## 5. Database & Persistence Rules

### 5.1 SQLite JSI (Offline-First)
- Single database file: `tpv.db`
- **All writes** wrapped in `db.withTransactionSync(() => { ... })` — atomic, auto-rollback
- **Reads** direct via `db.getAllSync` / `db.getFirstSync` — zero-latency
- `PRAGMA journal_mode = WAL; PRAGMA foreign_keys = ON;`

### 5.2 Schema Conventions
| Convention | Rule |
|---|---|
| Table names | `snake_case`, plural (`products`, `sales_transactions`) |
| Column names | `snake_case` (`current_stock`, `registered_at`) |
| Primary keys | `id INTEGER PRIMARY KEY AUTOINCREMENT` |
| Singletons | `id INTEGER PRIMARY KEY CHECK (id = 1)` |
| Soft delete | `deleted_at TIMESTAMP DEFAULT NULL` + `WHERE deleted_at IS NULL` |
| Timestamps | `registered_at`, `updated_at`, `deleted_at` (ISO 8601 in code) |
| Indexes | Partial (`WHERE deleted_at IS NULL`) on FK and query columns |

### 5.3 Migration Protocol
- Versioned via `PRAGMA user_version`
- **Single source of truth:** `rn-app/src/data/database/migrations.ts` runner
- **Ordering (v2.0.0):** `catalog-inventory` → `sales-checkout` → `financial-accumulators` → `expenses-cashflow` → `system-settings`
- All share `user_version = 2`
- Atomic: each migration runs in `withTransactionSync`

---

## 6. Value Objects (Shared Kernel — Domain Layer)

| VO | File | Purpose |
|---|---|---|
| `Currency` | `rn-app/src/domain/value-objects/Currency.ts` | `Bs` \| `$` \| `€` with `Intl.NumberFormat` per locale |
| `Money` | `rn-app/src/domain/value-objects/Money.ts` | `amount` (2dp) + `Currency`; `add`, `subtract`, `format`, `round2` |
| `Stock` | `rn-app/src/domain/value-objects/Stock.ts` | Non-negative decimal (3dp); `add`, `subtract`, `isBelow` |
| `SKU` | `rn-app/src/domain/value-objects/SKU.ts` | Alphanumeric + hyphen/underscore, max 32, uppercase |
| `PaymentMethod` | `rn-app/src/domain/entities/PaymentMethod.ts` | `CASH` \| `CARD` \| `TRANSFER` \| `QR` |
| `ChangeAmount` | `rn-app/src/domain/value-objects/ChangeAmount.ts` | `received - total`; `isSufficient()`, color class |

> **All modules consume these VOs.** No primitive `number` for money/stock in domain/application layers.

---

## 7. Module Boundaries & Responsibilities

| Module | Bounded Context | Key Tables | AdWare |
|---|---|---|---|
| **catalog-inventory** | Products, categories, stock, images | `products`, `categories` | **Allowed** (Inventory list footer) |
| **sales-checkout** | Cart, sales/purchases, payment, receipts | `sales_transactions`, `transaction_details` | **Forbidden** (checkout flow) |
| **expenses-cashflow** | Expenses, expense categories | `sales_transactions` (type=EXPENSE), `expense_categories` | **Allowed** (Expenses list footer) |
| **financial-accumulators** | Cash accumulator, reports, reversal | `financial_accumulators`, `sales_transactions` | **Allowed** (Caja, Movimientos footers) |
| **system-settings** | Currency, pagination, PIN/biometric, backup, AD_ENABLED | `settings` (singleton) | **Master toggle** (`adEnabled`) |

### Cross-Module Contracts
- `catalog-inventory` → `sales-checkout`: Product data (price, stock, SKU)
- `sales-checkout` → `financial-accumulators`: Atomic `UpdateAccumulator` calls
- `expenses-cashflow` → `financial-accumulators`: Atomic `UpdateAccumulator` calls
- `system-settings` → **All**: `Currency`, `Money`, `pageSize`, `adEnabled`, PIN gate

---

## 8. AdWare Governance (`AD_ENABLED` Flag)

| View | Allowed? | Implementation |
|---|---|---|
| Catalog / Inventory list | ✅ Yes | `ListFooterComponent` in FlashList |
| Expenses list | ✅ Yes | `ListFooterComponent` in FlashList |
| Caja (Cash Summary) | ✅ Yes | `ListFooterComponent` |
| Movimientos (Transactions) | ✅ Yes | `ListFooterComponent` |
| **Checkout / Payment / Receipt** | ❌ No | `AD_ENABLED` ignored |
| **Forms (create/edit)** | ❌ No | `AD_ENABLED` ignored |
| **ConfirmDialog / PIN Gate / Lock** | ❌ No | `AD_ENABLED` ignored |
| **Settings screen** | ❌ No | `AD_ENABLED` ignored |

**Master toggle:** `settings.ad_enabled` (SQLite) → `settingsStore.adEnabled` (Zustand) → `useAdEnabled()` hook

---

## 9. Global Invariants (INV-*)

| ID | Invariant | Enforcement |
|---|---|---|
| **INV-01** | Non-negative financial math (totals, stock, money) | Domain VO constructors, `validatePageSize`, `Stock.subtract` |
| **INV-02** | Deleted entities never hard-deleted | Soft delete only (`deleted_at`), queries filter `IS NULL` |
| **INV-03** | All multi-table writes atomic | `db.withTransactionSync` in every use case |
| **INV-04** | Single payment method per transaction | `PaymentMethod` VO, no split payments |
| **INV-05** | Category required for products | `category_id NOT NULL`, validated in repo + form |
| **INV-06** | Singleton tables have exactly 1 row (`id=1`) | `CHECK (id = 1)` + `INSERT OR IGNORE` |
| **INV-07** | Stock alerts fire post-commit only | `useStockAlert` hook after successful transaction |
| **INV-08** | Critical actions require PIN | `usePinGate` hook: change PIN, delete transaction, restore backup |
| **INV-09** | Image pipeline: 400×400, JPEG 70%, <40KB | `ImageOptimizer` + `ImageStorage` |
| **INV-10** | Strict TypeScript everywhere | `rn-app/tsconfig.json`: `strict`, `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes` |

---

## 10. Coding Standards

### 10.1 TypeScript
```ts
// ✅ Required: explicit types on public APIs
export function registerSale(input: RegisterSaleInput): RegisterSaleResult;

// ✅ Required: readonly on entities/DTOs
export interface Product {
  readonly id: ProductId;
  readonly name: string;
  // ...
}

// ✅ Required: branded types for IDs
export type ProductId = number & { readonly __brand: 'ProductId' };
```

### 10.2 React / NativeWind
- Components memoized by default (`React.memo`, `useCallback`, `useMemo`)
- FlashList: `keyExtractor`, `renderItem` stable, no `estimatedItemSize`
- Styles: NativeWind classes + design tokens (`theme/tokens.ts`)

### 10.3 SQL
- **Always parameterized** — never string concatenation
- Column aliases match entity fields (`current_stock` → `currentStock` via mapper)

### 10.4 Imports Order
```ts
// 1. External
import { useState } from 'react';
// 2. Internal absolute (@/ alias)
import { useCurrency } from '@/presentation/hooks/useCurrency';
// 3. Relative
import './Component.css';
```

### 10.5 No Comments (unless requested)
- Self-documenting code via types, names, structure
- Only document "why", not "what"

---

## 11. Testing Requirements (NFR-10)

| Layer | Framework | Target |
|---|---|---|
| **Unit (Domain)** | Jest | 100% functions/branches on VOs, Rules, Entities |
| **Integration (Data)** | Jest + SQLite | 100% repo methods, use cases, atomic flows |
| **Migration** | Jest + SQLite | v1→v2 zero-data-loss, idempotent |
| **Hooks** | Jest + RTL | 100% hook logic |
| **Components** | Jest + RTL | 100% render/interaction |
| **E2E** | Detox / Maestro | Critical user flows (checkout, PIN, backup) |
| **CI Gate** | All suites pass + `tsc --noEmit` + `eslint` | Required for merge |

---

## 12. Agent Workflow Rules

1. **Read specs first** — `/docs/modules/*/spec.md` before any implementation
2. **Follow layer boundaries** — never import Infrastructure in Domain
3. **Use VOs** — `Money`, `Stock`, `SKU`, `Currency` in domain/application
4. **Atomic transactions** — every write use case uses `withTransactionSync`
5. **English identifiers** — rename if you see Spanish in code/DB
6. **i18n keys** — add to `rn-app/src/presentation/i18n/es/*.json` for new UI text
7. **Run tests** — `cd rn-app && pnpm test` (unit + integration) before marking complete
8. **Update docs** — if schema/contracts change, update `spec.md` + `plan.md` + `test.md`

---

## 13. File Structure Reference

```
rn-app/src/
├── domain/                    # 🔵 Pure Domain (no deps)
│   ├── entities/              # Product, Category, SaleTransaction, Expense, UserSettings, FinancialSummary...
│   ├── value-objects/         # Currency, Money, Stock, SKU, PaymentMethod, ChangeAmount
│   └── rules/                 # stockRules, cartRules, expenseRules, settingsRules
├── data/                      # 🟢 Data Layer
│   ├── database/              # client.ts, migrations.ts, schema.sql.ts, seed.sql.ts
│   ├── repositories/          # Interfaces (Ports) + Sqlite* Implementations
│   └── mappers/               # rowMappers.ts (snake_case → camelCase)
├── application/               # 🟡 Use Cases
│   ├── products/              # ProductManagement
│   ├── sales/                 # RegisterSale
│   ├── purchases/             # RegisterPurchase
│   ├── expenses/              # RegisterExpense
│   ├── transactions/          # ReverseTransaction (shared)
│   ├── accumulators/          # GetCashSummary, GetPeriodSummary, UpdateAccumulator
│   ├── settings/              # GetSettings, UpdateSettings
│   ├── security/              # VerifyPin, SetPin
│   └── backup/                # ExportDatabase, RestoreDatabase
├── infrastructure/            # 🟠 Concrete Services
│   ├── image/                 # ImageOptimizer, ImageStorage
│   ├── pdf/                   # ComprobanteRenderer, ComprobanteService
│   ├── security/              # PinService, BiometricService
│   ├── settings/              # SettingsStore (SQLite hydration)
│   └── toast/                 # ToastService
└── presentation/              # 🟣 UI Layer
    ├── components/
    │   ├── ui/                # Button, Input, Modal, Card, Badge, AnimatedPressable
    │   ├── product/           # ProductGridCard, CategorySelector, QuickCategoryModal...
    │   ├── cart/              # CartLine, CartSummaryBar, LineItemModal, PaymentModal
    │   ├── expenses/          # ExpenseListItem, CategorySelector, QuickCategoryModal
    │   ├── filters/           # SearchBar, CategoryChipsRow, SortMenu
    │   ├── feedback/          # EmptyState, Toast, ConfirmDialog
    │   ├── settings/          # SectionHeader, CurrencySelector, PageSizeInput, PinForm...
    │   ├── accumulators/      # CashBalanceCard, StatCard, BreakdownBar
    │   ├── movimientos/       # MovimientoListItem, TipoFilterChips
    │   └── ads/               # AdBanner
    ├── hooks/                 # useProducts, useCart, useExpenses, useCashSummary...
    ├── stores/                # Zustand: *Store.ts
    └── theme/                 # tokens.ts, palette.json, index.ts
```

**KMP workspace (`kmp-app/`):**

```
kmp-app/
├── shared/src/commonMain/kotlin/   # Domain, application, data (SQLDelight), shared UI
├── composeApp/src/commonMain/      # Compose Multiplatform UI + resources
├── composeApp/src/androidMain/     # Android platform implementations
├── gradle/libs.versions.toml       # Version catalog
└── settings.gradle.kts             # Gradle modules: shared, composeApp
```

---

## 14. Quick Reference: Module Specs Location

| Module | Spec | Plan | Tests |
|---|---|---|---|
| catalog-inventory | `/docs/modules/catalog-inventory/spec.md` | `/docs/modules/catalog-inventory/plan.md` | `/docs/modules/catalog-inventory/test.md` |
| sales-checkout | `/docs/modules/sales-checkout/spec.md` | `/docs/modules/sales-checkout/plan.md` | `/docs/modules/sales-checkout/test.md` |
| expenses-cashflow | `/docs/modules/expenses-cashflow/spec.md` | `/docs/modules/expenses-cashflow/plan.md` | `/docs/modules/expenses-cashflow/test.md` |
| financial-accumulators | `/docs/modules/financial-accumulators/spec.md` | `/docs/modules/financial-accumulators/plan.md` | `/docs/modules/financial-accumulators/test.md` |
| system-settings | `/docs/modules/system-settings/spec.md` | `/docs/modules/system-settings/plan.md` | `/docs/modules/system-settings/test.md` |

---

*Last updated: 2026-11-10 — v2.0.0 Architecture Governance*