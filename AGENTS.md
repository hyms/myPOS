# AGENTS.md — Master Guidelines for AI Coding Agents (v2.0.0)

This document defines the architectural governance, coding standards, and cross-module contracts that all AI agents must follow when working on the **TPV Móvil Express** codebase.

---

## 0. Repository Layout (Dual-App Workspace)

| Path | Purpose |
|---|---|
| `rn-app/` | Legacy React Native + Expo application (TypeScript, Expo Router, NativeWind, SQLite JSI) |
| `kmp-app/` | New Kotlin Multiplatform + Compose Multiplatform application (SQLDelight, Koin) |
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
- **Semantic Versioning**: 2.0.0 (MAJOR.MINOR.PATCH)
- **Module Version**: Every module at `v2.0.0`
- **Breaking Changes**: Require MAJOR bump; coordinated via `kmp-app/src/data/database/migrations.ts` `user_version`
- **Branch Strategy**: `main` = production-ready; feature branches per module

---

## 2. Language Governance (STRICT)
| Layer | Language | Enforcement |
|---|---|---|
| **Code identifiers** (variables, functions, classes, types, interfaces) | **English only** | Koin + Kotlin compile |
| **Database** (tables, columns, indexes, constraints) | **English only** | SQLDelight DDL |
| **API / DTOs / Repository interfaces** | **English only** | Kotlin types |
| **UI strings** (user-visible text) | **Spanish only** via i18n keys | `composeResources/values-es/strings.xml` |

> **Zero tolerance:** No Spanish in code/DB. No English in UI strings. Violations fail CI.

---

## 3. Architecture: Clean Architecture + DDD (4 Layers)

```
┌─────────────────────────────────────────────────────────────┐
│  PRESENTATION (Compose Multiplatform)                      │
│  Screens, Compose Components, ViewModels, StateFlow, i18n  │
├─────────────────────────────────────────────────────────────┤
│  APPLICATION (Use Cases / Orchestration)                   │
│  RegisterSale, RegisterExpense, GetCashSummary, etc.       │
├─────────────────────────────────────────────────────────────┤
│  DOMAIN (Pure Kotlin — Zero External Dependencies)         │
│  Entities, Value Objects (Money, Currency, Stock, SKU),    │
│  Domain Rules, Repository Interfaces (Ports)               │
├─────────────────────────────────────────────────────────────┤
│  INFRASTRUCTURE (Concrete Implementations)                 │
│  SQLDelight Repositories, SecureStore, FileSystem, AdMob,  │
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
| **Framework** | Kotlin Multiplatform 2.4+ | Gradle 9.9.1 · AGP 9.5.1 · JVM 21 · Kotlin strict |
| **Router** | Compose Multiplatform navigation | Single activity, destination back stack |
| **UI Styling** | Compose Material3 + Material You | Design tokens in `kmp-app/composeApp/src/commonMain/kotlin/.../presentation/theme/` |
| **Lists** | `LazyVerticalGrid` | `columns = 3` (catalog), `columns = 1` (history lists) |
| **Database** | SQLDelight 2.4.1 | `.sq` schema + ordered `.sqm` migrations, `transaction { }` |
| **State** | Koin 4.2.2 + `StateFlow` | `koinViewModel`, granular ViewModels |
| **Auth** | Platform secure storage (PIN hash) + Biometric | `expect`/`actual` in `androidMain` / `iosMain` |
| **Images** | Platform image optimizer (`expect`/`actual`) | 400×400, JPEG 70%, <40 KB |
| **Print/Share** | Platform print + share (`expect`/`actual`) | HTML receipts |
| **Backup** | `ExportBackupUseCase` / `ImportBackupUseCase` | Versioned JSON v2 + legacy `rn-app` `.db` import |
| **Dates** | `kotlinx-datetime` | ISO-8601 |
| **Toasts** | Compose Snackbar | Material3 |
| **Distribution** | Single build `com.copitoB.mypos` | Compile-time `AppConfig` singleton (`ADS_ENABLED: Boolean`, `false` during development) |
| **Package Manager** | Gradle | `gradlew` from `kmp-app/` |

---

## 5. Database & Persistence Rules
- Single database file: `tpv.db` (managed by SQLDelight)
- **All writes** wrapped in a SQLDelight `transaction { ... }` block — atomic, auto-rollback
- **Reads** direct through generated SQLDelight queries — zero-latency
- `PRAGMA journal_mode = WAL; PRAGMA foreign_keys = ON;`

### Schema Conventions
| Convention | Rule |
|---|---|
| Table names | `snake_case`, plural (`products`, `sales_transactions`) |
| Column names | `snake_case` (`current_stock`, `registered_at`) |
| Primary keys | `id INTEGER PRIMARY KEY AUTOINCREMENT` |
| Singletons | `id INTEGER PRIMARY KEY CHECK (id = 1)` |
| Soft delete | `deleted_at TIMESTAMP DEFAULT NULL` + `WHERE deleted_at IS NULL` |
| Timestamps | `registered_at`, `updated_at`, `deleted_at` (ISO 8601 in code) |
| Indexes | Partial (`WHERE deleted_at IS NULL`) on FK and query columns |

### Migration Protocol
- Versioned via `PRAGMA user_version`
- **Single source of truth:** `kmp-app/composeApp/src/commonMain/sqldelight/schema.sq` + ordered `.sqm` files under `migrations/`
- **Ordering (v2.0.0):** `catalog-inventory` → `sales-checkout` → `financial-accumulators` → `expenses-cashflow` → `system-settings`
- All share `user_version = 2`
- Atomic: each migration runs inside its own `transaction { }`

---

## 6. Value Objects (Shared Kernel — Domain Layer)

| VO | File | Purpose |
|---|---|---|
| `Currency` | `kmp-app/src/commonMain/kotlin/domain/value-objects/Currency.kt` | `Bs` \| `$` \| `€` with `Intl.NumberFormat` per locale |
| `Money` | `kmp-app/src/commonMain/kotlin/domain/value-objects/Money.kt` | `amount` (2dp) + `Currency`; `add`, `subtract`, `format`, `round2` |
| `Stock` | `kmp-app/src/commonMain/kotlin/domain/value-objects/Stock.kt` | Non-negative decimal (3dp); `add`, `subtract`, `isBelow` |
| `SKU` | `kmp-app/src/commonMain/kotlin/domain/value-objects/SKU.kt` | Alphanumeric + hyphen/underscore, max 32, uppercase |
| `PaymentMethod` | `kmp-app/src/commonMain/kotlin/domain/entities/PaymentMethod.kt` | `CASH` \| `CARD` \| `TRANSFER` \| `QR` |
| `ChangeAmount` | `kmp-app/src/commonMain/kotlin/domain/value-objects/ChangeAmount.kt` | `received - total`; `isSufficient()`, color class |

> **All modules consume these VOs.** No primitive `number` for money/stock in domain/application layers.

---

## 7. Module Boundaries & Responsibilities

| Module | Bounded Context | Key Tables | Ad Surface |
|---|---|---|---|
| **catalog-inventory** | Products, categories, stock, images | `products`, `categories` | `AppConfig.ADS_ENABLED`: Inventory list footer |
| **sales-checkout** | Cart, sales/purchases, payment, receipts | `sales_transactions`, `transaction_details` | **None** (checkout + purchases) |
| **expenses-cashflow** | Expenses, expense categories | `sales_transactions` (type=EXPENSE), `expense_categories` | `AppConfig.ADS_ENABLED`: Expenses list footer |
| **financial-accumulators** | Cash accumulator, reports, reversal | `financial_accumulators`, `sales_transactions` | `AppConfig.ADS_ENABLED`: Caja + Movimientos footers |
| **system-settings** | Currency, pagination, PIN/biometric, backup/import, AppConfig | `settings` (singleton) | Read-only `FlavorInfoCard` |

### Cross-Module Contracts
- `catalog-inventory` → `sales-checkout`: Product data (price, stock, SKU)
- `sales-checkout` → `financial-accumulators`: Atomic `UpdateAccumulator` calls
- `expenses-cashflow` → `financial-accumulators`: Atomic `UpdateAccumulator` calls
- `system-settings` → **All**: `Currency`, `Money`, `pageSize`, PIN gate, `LocalAdsEnabled`

---

## 8. AdWare Governance (Single Build — AppConfig)

**Ads are a compile-time property, never a runtime setting.** There is no ads toggle in the Settings screen and no `ad_enabled` read path. The app is built as a single build (`com.copitoB.mypos`) driven by a compile-time `AppConfig` singleton in `commonMain` holding `ADS_ENABLED: Boolean` (`false` during development) and theme configuration.

| View | Ads when `AppConfig.ADS_ENABLED` | Implementation |
|---|---|---|
| Catalog / Inventory list | ✅ Banner | Footer item in `LazyVerticalGrid` when `LocalAdsEnabled.current` |
| Expenses list | ✅ Banner | Footer item in `LazyColumn` when `LocalAdsEnabled.current` |
| Caja (Cash Summary) | ✅ Banner | Footer item when `LocalAdsEnabled.current` |
| Movimientos (Transactions) | ✅ Banner | Footer item when `LocalAdsEnabled.current` |
| **Checkout / Payment / Receipt** | ❌ No | No ad code path |
| **Purchases flow** | ❌ No | No ad code path |
| **Forms (create/edit)** | ❌ No | No ad code path |
| **ConfirmDialog / PIN Gate / Lock** | ❌ No | No ad code path |
| **Settings screen** | ❌ No | Read-only `FlavorInfoCard` |

**Ad config contract:**
- Single build `com.copitoB.mypos`; no `free`/`pro` flavors, no `applicationIdSuffix`.
- `AppConfig` singleton in `commonMain` exposes `ADS_ENABLED: Boolean` (default `false` during development) plus theme configuration (seed color, `PaletteStyle`).
- Flag chain: `AppConfig.ADS_ENABLED` → `expect fun isAdsEnabled(): Boolean` → `LocalAdsEnabled` CompositionLocal at the app root.
- `settings.ad_enabled` is deprecated: never read or written, retained until `v3.0.0` to avoid a mid-phase migration, then dropped by a dedicated `.sqm`.

---

## 9. Global Invariants (INV-*)

| ID | Invariant | Enforcement |
|---|---|---|
| **INV-01** | Non-negative financial math (totals, stock, money) | Domain VO constructors, `validatePageSize`, `Stock.subtract` |
| **INV-02** | Deleted entities never hard-deleted | Soft delete only (`deleted_at`), queries filter `IS NULL` |
| **INV-03** | All multi-table writes atomic | SQLDelight `transaction { }` in every write use case |
| **INV-04** | Single payment method per transaction | `PaymentMethod` VO, no split payments |
| **INV-05** | Category required for products | `category_id NOT NULL`, validated in repo + form |
| **INV-06** | Singleton tables have exactly 1 row (`id=1`) | `CHECK (id = 1)` + `INSERT OR IGNORE` |
| **INV-07** | Stock alerts fire post-commit only | Snackbar triggered after the transaction returns successfully |
| **INV-08** | Critical actions require PIN | `PinViewModel` / `PinLockOverlay`: change PIN, delete transaction, restore/import backup |
| **INV-09** | Image pipeline: 400×400, JPEG 70%, <40KB | `expect`/`actual` `ImageOptimizer` + `ImageStorage` |
| **INV-10** | Strict Kotlin everywhere | `kmp-app/composeApp/build.gradle.kts`: explicit API, `@JvmInline value class` & `data class` for VOs |
| **INV-11** | Backup import is atomic and idempotent | `ImportBackupUseCase` runs in one `transaction { }`; re-running yields identical state; `financial_accumulators` is always recomputed, never trusted |
| **INV-12** | Ads are build-time only | Single build `com.copitoB.mypos`; `AppConfig.ADS_ENABLED` + `LocalAdsEnabled`; zero runtime UI toggle, no `ad_enabled` read path |

---

## 10. Coding Standards

### 10.1 Kotlin
```kotlin
// ✅ Required: explicit types on public APIs
fun registerSale(input: RegisterSaleInput): RegisterSaleResult

// ✅ Required: immutable data classes
data class Product(
    val id: ProductId,
    val name: String,
    // ...
)

// ✅ Required: branded types for IDs
@JvmInline
value class ProductId(val id: Int) { val __brand: 'ProductId' }

// ✅ Required: strict null-safety
@JvmInline
fun <T> T?.let { /* ... */ }

// ✅ Required: explicitApi() for public APIs
explicitApi<RegisterSaleResult>(RegisterSaleResult)
```

### 10.2 Compose Multiplatform
- Components memoized by default (`@Composable` with `remember`)
- `LazyVerticalGrid` / `LazyColumn` with `key` and `itemContent`
- Theming: Material3 with Material You (colors, typography, shapes)
- State: `StateFlow` with `collectAsState()` or `remember { mutableStateOf() }`

### 10.3 SQLDelight
- **Always parameterized** — never string concatenation
- Column aliases match entity fields (`current_stock` → `currentStock` via mapper)
- Migrations: `.sqm` files, atomic, ordered

### 10.4 Imports Order
```kotlin
// 1. External
import com.google.kotlinx.coroutines.*

// 2. Internal absolute (package) 
import com.mypos.shared.domain.value.objects.Currency

// 3. Relative
import com.mypos.kmp.app.ui.screens.CartScreen
```

### 10.5 No Comments (unless requested)
- Self-documenting code via types, names, structure
- Only document "why", not "what"

---

## 11. Testing Requirements (NFR-10)

| Layer | Framework | Target |
|---|---|---|
| **Unit (Domain)** | Kotlin Test | 100% functions/branches on VOs, Rules, Entities |
| **Integration (Data)** | Kotlin Test + SQLDelight | 100% repo methods, use cases, atomic flows |
| **Migration** | Kotlin Test + SQLDelight | v1→v2 zero-data-loss, idempotent, backup round-trip |
| **Composables** | Compose Testing Library | 100% composable logic |
| **E2E** | Maestro | Critical user flows (checkout, purchase, PIN, backup) |
| **CI Gate** | `./gradlew :composeApp:testAndroidHostTest` + `./gradlew detekt` + `./gradlew :androidApp:assembleDebug` | Required for merge |

---

## 12. Agent Workflow Rules
1. **Read specs first** — `/docs/modules/*/spec.md` before any implementation
2. **Follow layer boundaries** — never import Infrastructure in Domain
3. **Use VOs** — `Money`, `Stock`, `SKU`, `Currency` in domain/application
4. **Atomic transactions** — every write use case uses a SQLDelight `transaction { }` block
5. **English identifiers** — rename if you see Spanish in code/DB
6. **i18n keys** — add to `kmp-app/composeApp/src/commonMain/composeResources/values-es/strings.xml` for new UI text
7. **Run tests** — `cd kmp-app && ./gradlew :composeApp:testAndroidHostTest` (unit + integration) before marking complete
8. **Update docs** — if schema/contracts change, update `spec.md` + `plan.md` + `test.md`, and tick `/docs/MIGRATION_CHECKLIST.md` when closing a parity gap
9. **Token & Context Protocol** — Query `codebaseMemory` first for symbol/doc lookups; treat `docs/MIGRATION_CHECKLIST.md` as the single source of truth for `rn-app/` parity; keep responses under `/caverman` density.

---

## 13. File Structure Reference

```
kmp-app/
├── shared/
│   └── src/
│       └── commonMain/
│           └── kotlin/
│               ├── domain/
│               │   ├── entities/
│               │   ├── value-objects/
│               │   └── rules/
│               ├── application/
│               │   ├── products/
│               │   ├── sales/
│               │   ├── expenses/
│               │   ├── transactions/
│               │   ├── accumulators/
│               │   └── settings/
│               └── data/
│                   └── database/
│                       ├── schema.sq
│                       └── migrations/
├── composeApp/
│   └── src/
│       └── commonMain/
│           ├── kotlin/
│           │   └── ui/
│           │   └── resources/
│           └── resources/
│               ├── values-es/
│               │   └── strings.xml
│               └── common/
├── composeApp/
│   └── src/
│       ├── androidMain/
│       │   └── kotlin/
│       └── iosMain/
│           └── kotlin/
└── settings.gradle.kts
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

*Last updated: 2026-11-12 — v2.0.0 Architecture Governance*