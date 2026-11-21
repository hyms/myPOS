# TPV Móvil Express — Technical Architecture & Stack Reference v2.0

## 1. System Architecture Overview

TPV Móvil Express v2.0 is an offline-first, high-performance mobile application engineered under **Domain-Driven Design (DDD)**, **Specification-Driven Development (SDD)**, and **Test-Driven Development (TDD)** principles, implemented on **Kotlin Multiplatform (KMP)** with **Compose Multiplatform** as the single UI toolkit for Android and iOS.

```
┌─────────────────────────────────────────────────────────────┐
│  PRESENTATION — Compose Multiplatform                       │
│  Screens, ViewModels (StateFlow), Material3 design tokens   │
│  UI strings: composeResources/values-es/strings.xml         │
├─────────────────────────────────────────────────────────────┤
│  APPLICATION — Use Cases / Orchestration                    │
│  RegisterSale, RegisterPurchase, RegisterExpense,           │
│  ExportBackup, ImportBackup, UpdateAccumulator              │
├─────────────────────────────────────────────────────────────┤
│  DOMAIN — Pure Kotlin, zero external dependencies           │
│  Entities, Value Objects (Money, Currency, Stock, SKU),     │
│  Rules, Repository Ports                                    │
├─────────────────────────────────────────────────────────────┤
│  INFRASTRUCTURE — Platform implementations                  │
│  SQLDelight repositories, SecureStore, FileSystem,          │
│  ImageOptimizer, Print, Sharing, Biometric, SettingsStore   │
└─────────────────────────────────────────────────────────────┘
```

- **Dependency Rule:** Presentation → Application → Domain ← Infrastructure. Domain imports nothing from outer layers.
- **Domain Isolation:** Pure Kotlin with zero Compose, SQLDelight, Koin, or platform dependencies inside `domain/`.
- **Workspace Layout:** `kmp-app/` is the shipping application. `rn-app/` is the legacy React Native implementation retained as a behavioral reference; every RN feature is tracked in `/docs/MIGRATION_CHECKLIST.md`.
- **Language Policy:**
  - All code, identifiers, database schema, variables, and architectural documentation must be in **English**.
  - All user interface strings, messages, labels, and receipts are localized in **Spanish** through `composeResources/values-es/strings.xml`.

---

## 2. Technology Stack & Core Tooling

| Category | Technology / Library | Version / Detail |
| :--- | :--- | :--- |
| **Language** | Kotlin Multiplatform | 2.4.21 · Android + `iosArm64` / `iosSimulatorArm64` targets |
| **Build System** | Gradle + Android Gradle Plugin | Gradle 9.9.1 · AGP 9.5.1 · JVM 21 · `compileSdk` 37 |
| **Module Split** | `androidApp` (`com.android.application`) + `composeApp` (`com.android.kotlin.multiplatform.library`) | AGP 9 New DSL; `com.android.application` is never combined with the KMP plugin |
| **UI Toolkit** | Compose Multiplatform + Material3 (Material You) | 1.12.2 · dark-first design tokens · `MaterialKolor` (`com.materialkolor:material-kolor`) theme driven by `AppConfig` seed color (`Burnt Ember`) + `PaletteStyle` |
| **Navigation** | Compose Multiplatform navigation | Single activity, destination-based back stack |
| **Lists** | `LazyVerticalGrid` / `LazyColumn` | 3-column catalog, single-column history, stable keys |
| **Database** | SQLDelight | 2.4.1 · `schema.sq` + ordered `.sqm` migrations · Android SQLite driver |
| **Dependency Injection** | Koin | 4.2.2 · `koinViewModel` + platform modules |
| **Async / State** | kotlinx-coroutines + `StateFlow` | `viewModelScope`, `collectAsStateWithLifecycle` |
| **Serialization** | kotlinx.serialization | Versioned backup/import JSON contracts |
| **Security** | Platform secure storage (`expect`/`actual`) + SHA-256 PIN hash | PIN gate, biometric unlock |
| **Media Pipeline** | Platform image resizer (`expect`/`actual`) + filesystem | 400×400, JPEG 70%, target <40 KB |
| **Print / Share** | Platform print + share (`expect`/`actual`) | HTML receipts |
| **Backup** | FileSystem `expect`/`actual` + DocumentPicker equivalent | JSON v2 export; legacy `.db` import |
| **Dates** | kotlinx-datetime | ISO-8601 timestamps |
| **Testing** | `kotlin.test` + Koin test + coroutines-test + Compose UI test | `./gradlew :composeApp:testAndroidHostTest` |
| **Distribution** | Single build `com.copitoB.mypos` | Compile-time `AppConfig` singleton in `commonMain` (`ADS_ENABLED: Boolean`, `false` during development); no flavors |

---

## 3. Engineering Principles & Software Design Patterns

### A. Software Engineering Principles

- **SRP (Single Responsibility Principle):** Each module, class, and custom hook has a single, well-defined responsibility. UI components delegate business math exclusively to Use Cases.
- **OCP (Open/Closed Principle):** Systems (such as report generation or catalog formatters) are open for extension through interfaces but closed for direct code modification.
- **DIP (Dependency Inversion Principle):** High-level Application Use Cases depend on abstractions (domain ports/interfaces), never on low-level infrastructure details (SQLite driver, Expo SDK directly).
- **DRY (Don't Repeat Yourself):** Reusable Compose design primitives and centralized currency/date formatters eliminate duplication.
- **KISS (Keep It Simple, Stupid):** 100% offline-first architecture without cloud complexity, server synchronization, or unnecessary GraphQL wrappers.
- **YAGNI (You Ain't Gonna Need It):** Strict scope boundaries: zero multi-branch, multi-user roles, or cloud payment gateways in this version.
- **Low Coupling & High Cohesion:** The Presentation layer only invokes Use Cases and remains completely agnostic of the underlying database engine.

### B. Core Design Patterns

- **Singleton Pattern:** Applied to the SQLDelight `SqlDriver` instance, the platform secure-storage adapter, and the single-row `financial_accumulators` table (`id = 1`).
- **Strategy Pattern:** Implemented via `expect`/`actual` platform ports for printing, sharing, image optimization, and biometric authentication, so `commonMain` stays platform-free.
- **Composite Pattern:** Utilized in the domain cart to treat individual products (`SimpleCartItem`) and bundled products under a unified interface (`ICartItem`).
- **Observer Pattern:** `StateFlow` in ViewModels drives Compose recomposition, so the Caja screen refreshes when a transaction mutates the `financial_accumulators` singleton.
- **Repository Pattern:** Encapsulates persistence behind domain ports (`ProductRepository`, `SaleRepository`, `ExpenseRepository`, `AccumulatorRepository`, `SettingsRepository`), enabling in-memory fakes for zero-latency TDD.
- **Factory Pattern:** Encapsulates aggregate creation (`ICartItem` factory) so entities satisfy invariants before persistence.

---

## 4. Global Non-Functional Requirements (NFRs)

- **NFR-01 (Offline-First Execution):** Zero cloud runtime dependency. All reads, writes, and image transformations execute locally through SQLDelight and platform `expect`/`actual` implementations.
- **NFR-02 (Sub-50ms List Rendering & Performance):**
  - Powered by `LazyVerticalGrid` / `LazyColumn` with stable keys and explicit item content types.
  - Immutable UI models (`@Immutable`) and `StateFlow` with `collectAsStateWithLifecycle` avoid unnecessary recomposition.
  - Search input is debounced locally; quantities use local `mutableStateOf` to keep the UI thread responsive.
  - Images are decoded off the UI thread and cached by the platform pipeline.
- **NFR-03 (0ms Cash Aggregation):** Financial balance reads query the single-row `financial_accumulators` singleton (`id = 1`) instead of running runtime `SUM()` scans.
- **NFR-04 (Image Storage & Memory Optimization):**
  - Input images processed locally: resized to a maximum of **400×400 px**, JPEG 70% quality, targeting **<40 KB** per file.
  - Stored in app storage (`<documents>/products/<uuid>.jpg`). The database stores relative paths only.
- **NFR-05 (Strict Dark Mode Default):** Primary UI defaults to "Strict Dark Mode" (obsidian/charcoal tones with high-contrast accents), driven by Material3 design tokens.
- **NFR-06 (Transactional Atomicity):** Multi-table mutations (sale/purchase creation, stock updates, accumulator adjustments, backup import) MUST execute inside a single SQLDelight `transaction { }` block. Any failure rolls back the whole operation.

---

## 5. Monetization & Adware Governance

**Ads are a compile-time property, never a runtime setting.** There is no runtime ads switch and no `ad_enabled` UI control. The app is built as a single build (`com.copitoB.mypos`) driven by a compile-time `AppConfig` singleton in `commonMain` holding `ADS_ENABLED: Boolean` (`false` during development) and theme configuration.

**Build rules:**
- Single build declares `applicationId` `com.copitoB.mypos`, signed with the shared `.jks` keystore. `applicationIdSuffix` is forbidden.
- `ADS_ENABLED` defaults to `false` during development.
- The flag is consumed through `expect fun isAdsEnabled(): Boolean` + `LocalAdsEnabled` CompositionLocal at the app root; iOS resolves the same `expect` to `false`.
- Ad code is isolated in platform source sets; `commonMain` only sees the boolean.
- The legacy `settings.ad_enabled` column is **deprecated and ignored**. It is retained until `v3.0.0` so the Phase 6 upgrade needs no mid-phase schema migration, and is removed by a dedicated `.sqm` afterwards.

**Allowed ad surfaces (footer of the browse list when `LocalAdsEnabled.current`):**
- Catalog / Inventory list
- Expenses list
- Caja (Cash Summary)
- Movimientos (Transactions)

**Forbidden surfaces (no ad code path at all):**
- Checkout, payment, and receipt screens
- Purchases flow
- All create/edit forms
- `ConfirmDialog`, PIN gate, lock screen
- Settings screen

**Performance protection:** banners use fixed-height containers to prevent layout shifts, and listeners are released on disposal. Ads never block scroll or pre-commit calculations.

---

## 6. Directory & Codebase Structure

```
/
├── docs/
│   ├── global.spec.md            # Architecture, stack, NFRs, invariants
│   ├── plan.md                   # Master roadmap (phases 0-6)
│   ├── MIGRATION_CHECKLIST.md    # RN → KMP feature parity tracker
│   ├── README.md                 # Documentation index
│   └── modules/
│       ├── _blueprint/           # Triad template
│       ├── catalog-inventory/    # spec.md, plan.md, test.md
│       ├── sales-checkout/       # spec.md, plan.md, test.md
│       ├── expenses-cashflow/    # spec.md, plan.md, test.md
│       ├── financial-accumulators/  # spec.md, plan.md, test.md
│       └── system-settings/      # spec.md, plan.md, test.md
│
├── kmp-app/
│   ├── androidApp/               # com.android.application: flavors, signing, manifest
│   │   ├── build.gradle.kts
│   │   └── src/main/
│   ├── composeApp/               # KMP shared module
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── commonMain/
│   │       │   ├── kotlin/com/copitoB/mypos/{domain,application,data,presentation}
│   │       │   ├── sqldelight/com/copitoB/mypos/data/repository/
│   │       │   └── composeResources/values-es/strings.xml
│   │       ├── commonTest/
│   │       ├── androidMain/
│   │       └── iosMain/
│   ├── gradle/libs.versions.toml
│   └── settings.gradle.kts
│
├── rn-app/                       # Legacy React Native reference (not shipped)
└── scripts/                      # Build, migration, and automation scripts
```

---

## 7. Development & Build Standards

### Kotlin Strictness
- `explicitApi()` on public declarations; immutable data classes; `@JvmInline value class` for IDs and value objects.
- No `java.math.BigDecimal` / `java.text.*` in `commonMain` — those are JVM-only and break the `iosArm64` target.
- Detekt clean; zero compiler warnings treated as errors where configured.

### Verification Commands (run from `kmp-app/`)

```bash
./gradlew :composeApp:generateCommonMainTpvDatabaseInterface   # SQLDelight codegen
./gradlew :composeApp:compileAndroidMain                        # Android compile
./gradlew :composeApp:testAndroidHostTest                       # Unit + integration tests
./gradlew :androidApp:assembleDebug                             # Single build debug APK
./gradlew detekt                                                # Static analysis
```

### Release Pipeline
1. Release keystore lives **outside version control** (`keystore/mypos-release.jks`); credentials are supplied through the gitignored `keystore.properties` or CI environment variables.
2. `signingConfig` uses the shared `.jks`; no `applicationIdSuffix`.
3. Build the single release artifact:
   ```bash
   ./gradlew :androidApp:bundleRelease
   ```
   *Output:* `androidApp/build/outputs/bundle/release/androidApp-release.aab`