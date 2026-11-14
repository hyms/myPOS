# TPV Móvil Express

> **Point of Sale Mobile Express** — Offline-first, Clean Architecture v2.0.0, Dual-App Workspace

[![Build Status](https://img.shields.io/github/actions/workflow/status/OWNER/REPO/ci.yml?branch=main&logo=github)](https://github.com/OWNER/REPO/actions)
[![Test Coverage](https://img.shields.io/codecov/c/github/OWNER/REPO?logo=codecov)](https://codecov.io/gh/OWNER/REPO)
[![Tech Stack](https://img.shields.io/badge/TypeScript-Strict-3178C6?logo=typescript&logoColor=white)](https://www.typescriptlang.org)
[![Database](https://img.shields.io/badge/SQLite-JSI%20Sync-003B57?logo=sqlite&logoColor=white)](https://docs.expo.dev/versions/latest/sdk/sqlite/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

---

## 🗂️ Workspace Layout (Dual-App)

```
/
├── docs/          # Master specs, plans, and test matrices (shared)
├── scripts/       # Root build, migration, and automation scripts
├── rn-app/        # Legacy React Native + Expo app (TypeScript, Expo Router, NativeWind)
├── kmp-app/       # Kotlin Multiplatform + Compose Multiplatform app (SQLDelight, Koin)
├── AGENTS.md      # Agent governance for the whole workspace
└── README.md      # This file
```

- **`rn-app/`** — current production app. All Expo/Node commands run from this directory.
- **`kmp-app/`** — target v2.0.0 app. Gradle commands run from this directory.
- **`docs/`** and **`scripts/`** — shared across both apps; paths stay root-relative.

---

## 🏗️ Architectural Diagram

```mermaid
graph TD
    %% Presentation Layer (KMP Compose)
    subgraph PRES["🟣 PRESENTATION (Compose Multiplatform)"]
        SCR[Screens: Tabs / Stack]
        CMP[Components: ProductGridScreen, LineItemDialog, PaymentDialog, CashBalanceCard...]
        VMS[ViewModels: SaleViewModel, ExpenseViewModel, CashSummaryViewModel...]
        I18N[i18n: Spanish UI strings via composeResources/values-es/strings.xml]
    end

    %% Application Layer (KMP)
    subgraph APP["🟡 APPLICATION (Use Cases)"]
        UC1[RegisterSale]
        UC2[RegisterPurchase]
        UC3[RegisterExpense]
        UC4[ReverseTransaction]
        UC5[GetCashSummary]
        UC6[GetPeriodSummary]
        UC7[UpdateAccumulator]
        UC8[ProductManagement]
        UC9[GetSettings / UpdateSettings]
        UC10[VerifyPin / SetPin]
        UC11[ExportDatabase / RestoreDatabase]
    end

    %% Domain Layer (KMP - Common Main)
    subgraph DOM["🔵 DOMAIN (Pure Kotlin — Zero Deps)"]
        ENT[Entities: Product, Category, SaleTransaction, Expense, UserSettings, FinancialSummary...]
        VO[Value Objects: Currency, Money, Stock, SKU, PaymentMethod, ChangeAmount]
        RUL[Domain Rules: stockRules, cartRules, expenseRules, settingsRules]
        REP[Repository Interfaces: IProductRepository, ISaleRepository, IExpenseRepository, IAccumulatorRepository, ISettingsRepository]
    end

    %% Infrastructure Layer (KMP)
    subgraph INFRA["🟠 INFRASTRUCTURE (Concrete Implementations)"]
        DB[(SQLite JSI: tpv.db)]
        SQL_REP[Sqlite*Repository Implementations via SQLDelight]
        SEC[PinService, BiometricService]
        IMG[ImageOptimizer, ImageStorage]
        PDF[ComprobanteRenderer, ComprobanteService]
        SET["SettingsStore (SQLite hydration)"]
        AD["AdBanner (AdMob wrapper)"]
        FS[FileSystem, SecureStore, Sharing, DocumentPicker, Print]
    end

    %% Dependency Flow
    PRES -->|calls| APP
    APP -->|uses| DOM
    DOM -.->|implemented by| INFRA
    INFRA -->|persists| DB

    %% Styling
    classDef pres fill:#e0d4ff,stroke:#8b5cf6,color:#1e1b4b;
    classDef app fill:#ffe4d4,stroke:#f97316,color:#7c2d12;
    classDef dom fill:#d4fcd4,stroke:#22c55e,color:#14532d;
    classDef infra fill:#fef3c7,stroke:#f59e0b,color:#78350f;
    class PRES,SCR,CMP,VMS,I18N pres;
    class APP,UC1,UC2,UC3,UC4,UC5,UC6,UC7,UC8,UC9,UC10,UC11 app;
    class DOM,ENT,VO,RUL,REP dom;
    class INFRA,DB,SQL_REP,SEC,IMG,PDF,SET,AD,FS infra;
```

---

## ⚙️ Decisiones Clave de Arquitectura

| Decisión | Justificación |
|---|---|
| **Clean Architecture + DDD** | Separación estricta de responsabilidades; Domain puro = testable, portable, sin dependencias de framework |
| **KMP + SQLDelight** | Migraciones atómicas, esquema versionado, `PRAGMA user_version = 2`, cero inconsistencias |
| **Offline-First SQLite** | SQLDelight genera tipos seguros; `db.withTransactionSync` → ACID nativo, cero latencia |
| **Compose Material3** | UI unificada Android/iOS, Material You, diseño consistente, single source of truth |
| **Compose Material You** | Tema dinámico, colores del sistema, adaptación automática a modo oscuro/claro |
| **Koin** | Inyección de dependencias ligera, `explicitApi()`, nulo-safety estricta |
| **Specification-Driven + SemVer** | Cada módulo tiene `spec.md`/`plan.md`/`test.md` versionados `v2.0.0` → contratos claros, migraciones coordinadas |
| **Value Objects compartidos** | `Currency`, `Money`, `Stock`, `SKU` en Domain → invariantes centralizados, no primitivos sueltos |
| **Atomic Transactions** | Todo write use case usa `db.withTransactionSync` → rollback automático, cero descuadres |
| **Singleton Accumulators** | `financial_accumulators` + `settings` (`id=1`, `CHECK`) → lecturas O(1), 0ms cash calculations |
| **Language Governance** | 100% English code/DB/identifiers; Spanish solo en i18n keys → consistencia global, onboarding fácil |
| **AdWare Governance** | Flag `AD_ENABLED` centralizado en `system-settings` → vistas permitidas (Caja, Gastos, Inventario, Movimientos) vs prohibidas (Checkout, Forms, PIN) |

---

## 📦 Module Boundaries (v2.0.0)

| Module | Bounded Context | Key Tables | AdWare |
|---|---|---|---|
| **catalog-inventory** | Products, categories, stock, images | `products`, `categories` | ✅ Inventory list footer |
| **sales-checkout** | Cart, sales/purchases, payment, receipts | `sales_transactions`, `transaction_details` | ❌ Forbidden (checkout flow) |
| **expenses-cashflow** | Expenses, expense categories | `sales_transactions` (type=EXPENSE), `expense_categories` | ✅ Expenses list footer |
| **financial-accumulators** | Cash accumulator, reports, reversal | `financial_accumulators`, `sales_transactions` | ✅ Caja, Movimientos footers |
| **system-settings** | Currency, pagination, PIN/biometric, backup, AD_ENABLED | `settings` (singleton) | 🎛️ Master toggle (`adEnabled`) |

### Cross-Module Contracts
- `catalog-inventory` → `sales-checkout`: Product data (price, stock, SKU)
- `sales-checkout` → `financial-accumulators`: Atomic `UpdateAccumulator` calls
- `expenses-cashflow` → `financial-accumulators`: Atomic `UpdateAccumulator` calls
- `system-settings` → **All**: `Currency`, `Money`, `pageSize`, `adEnabled`, PIN gate

---

## 🗄️ Database Schema (v2.0.0 — English Columns)

```sql
-- products (catalog-inventory)
CREATE TABLE products (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    category_id INTEGER NOT NULL,
    sku TEXT UNIQUE,
    name TEXT NOT NULL,
    selling_price REAL NOT NULL,
    purchase_price REAL NOT NULL,
    current_stock REAL DEFAULT 0.0,
    min_stock REAL DEFAULT 0.0,
    image_uri TEXT,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL,
    FOREIGN KEY(category_id) REFERENCES categories(id)
);

-- categories (catalog-inventory)
CREATE TABLE categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL
);

-- sales_transactions (sales-checkout + expenses-cashflow)
CREATE TABLE sales_transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT NOT NULL,              -- 'SALE' | 'PURCHASE' | 'EXPENSE'
    total_amount REAL NOT NULL,
    payment_method TEXT NOT NULL,    -- 'CASH' | 'CARD' | 'TRANSFER' | 'QR'
    detail TEXT,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL
);

-- transaction_details (sales-checkout)
CREATE TABLE transaction_details (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    transaction_id INTEGER NOT NULL,
    product_id INTEGER NOT NULL,
    quantity REAL NOT NULL,
    unit_price REAL NOT NULL,
    FOREIGN KEY(transaction_id) REFERENCES sales_transactions(id),
    FOREIGN KEY(product_id) REFERENCES products(id)
);

-- financial_accumulators (financial-accumulators)
CREATE TABLE financial_accumulators (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    total_sales REAL DEFAULT 0.0,
    total_purchases REAL DEFAULT 0.0,
    total_expenses REAL DEFAULT 0.0,
    net_balance REAL DEFAULT 0.0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- expense_categories (expenses-cashflow)
CREATE TABLE expense_categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL
);

-- settings (system-settings)
CREATE TABLE settings (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    currency_code TEXT NOT NULL DEFAULT 'Bs',
    page_size INTEGER NOT NULL DEFAULT 15,
    biometric_enabled INTEGER NOT NULL DEFAULT 0,
    ad_enabled INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 🚀 Quick Start

### Prerequisites
- Node.js ≥ 20
- pnpm ≥ 9
- Gradle (comes with Android Studio)
- Kotlin 1.9+

### Legacy React Native App (reference)

```bash
# Clone
git clone https://github.com/OWNER/REPO.git
cd REPO

# Legacy React Native app
cd rn-app

# Install dependencies
pnpm install

# Start Expo dev server
pnpm expo start

# Open on:
# - iOS Simulator (press 'i')
# - Android Emulator (press 'a')
# - Physical device (scan QR with Expo Go)
```

### New Kotlin Multiplatform App (active development)

```bash
# Clone
git clone https://github.com/OWNER/REPO.git
cd REPO

# Kotlin Multiplatform app (Mobile only: Android + iOS)
cd kmp-app

# Build & run checks
./gradlew check

# Run unit/integration tests (Android)
./gradlew testDebugUnitTest

# Run on Android
./gradlew run

# Run on iOS simulator
./gradlew iosSimulatorArm64Run

# Configure Kotlin Multiplatform (first run / dependency sync)
./gradlew configureKotlinMultiplatform
```

> **Legacy reference app** runs in `rn-app/` with `pnpm expo start`.  
> **Active v2.0.0 development** runs in `kmp-app/` with Gradle commands.

### Environment Variables
Create `.env.local` (optional, for remote config):
```env
EXPO_PUBLIC_AD_ENABLED=false
EXPO_PUBLIC_API_URL=https://api.example.com
```

### Database
- Single file: `tpv.db` (managed by SQLDelight)
- Migrations auto-run via `PRAGMA user_version` on app bootstrap
- v1.0.0 → v2.0.0 migration is zero-data-loss (see `kmp-app/src/data/database/migrations.ts`)

---

## 🧪 Running Tests

> All Gradle commands run from `kmp-app/`.

```bash
# Unit tests (Domain VOs, Rules, Entities)
./gradlew testDebugUnitTest

# Integration tests (Repositories, Use Cases, SQLDelight)
./gradlew testDebugUnitTest --tests "*..integration.*"

# Migration tests (v1→v2 zero-data-loss)
./gradlew migrate

# All tests + type safety + lint (CI gate)
./gradlew check

# Detox / Maestro E2E tests (requires device/emulator)
./gradlew detoxTest
# or
./gradlew maestraTest
```

### Test Structure (Kotlin)

```
src/
├── commonMain/
│   ├── domain/
│   │   ├── entities/
│   │   ├── value-objects/
│   │   └── rules/
│   ├── application/
│   │   ├── products/
│   │   ├── sales/
│   │   ├── expenses/
│   │   ├── transactions/
│   │   ├── accumulators/
│   │   └── settings/
│   └── data/
│       └── database/
│           ├── schema.sq
│           └── migrations/
├── androidTest/
│   └── ... (Android instrumented tests)
└── iosTest/
    └── ... (iOS UI tests via Firebase Test Lab)
```

---

## 📚 Documentation

| File | Description |
|---|---|
| `AGENTS.md` | Master guidelines for AI agents (this file's v2.0.0 version) |
| `docs/modules/catalog-inventory/spec.md` | Catalog & Inventory specification |
| `docs/modules/sales-checkout/spec.md` | Sales & Checkout specification |
| `docs/modules/expenses-cashflow/spec.md` | Expenses & Cash Flow specification |
| `docs/modules/financial-accumulators/spec.md` | Financial Accumulators specification |
| `docs/modules/system-settings/spec.md` | System Settings specification |

Each module has `/spec.md` (contracts), `/plan.md` (implementation), `/test.md` (TDD suite).

---

## 🔧 Tech Stack Summary

| Layer | Technology |
|---|---|
| **Framework** | Kotlin Multiplatform 1.9+ (managed) |
| **Router** | Compose Multiplatform |
| **UI Styling** | Compose Material3 + Material You (design tokens) |
| **Lists** | `LazyVerticalGrid` / `LazyColumn` |
| **Database** | SQLDelight + SQLite JSI |
| **State** | Koin ViewModels + StateFlow |
| **Auth** | `SecureStore` (PIN hash), `Biometric` API |
| **Images** | `ImageManipulator` (400×400, JPEG 70%) + `FileSystem` + `Compose Image` |
| **Print/Share** | `Print` + `Share` APIs |
| **Backup** | `DocumentPicker` + `FileSystem` |
| **Dates** | `kotlinx-datetime` |
| **Toasts** | `compose-toast` |
| **Package Manager** | `gradle` |

---

## 📋 Global Invariants (Non-Negotiable)

| ID | Rule |
|---|---|
| **INV-01** | Non-negative financial math (Money, Stock, totals) |
| **INV-02** | Soft delete only (`deleted_at`), never hard delete |
| **INV-03** | All multi-table writes atomic via `db.withTransactionSync` |
| **INV-04** | Single `PaymentMethod` per transaction (no split payments) |
| **INV-05** | Category required for products (`category_id NOT NULL`) |
| **INV-06** | Singleton tables exactly 1 row (`id=1`, `CHECK`) |
| **INV-07** | Stock alerts fire **post-commit** only |
| **INV-08** | Critical actions require PIN (`usePinGate`) |
| **INV-09** | Image pipeline: 400×400, JPEG 70%, target <40KB |
| **INV-10** | Strict Kotlin: `strictNullSafety = true`, `@JvmInline value class` & `data class` for VOs, `explicitApi()` |

---

## 🤝 Contributing

1. Read `AGENTS.md` and relevant module `spec.md`
2. Create feature branch: `feat/module-name/short-description`
3. Implement following layer boundaries (Domain → Application → Presentation)
4. Write tests (unit + integration) — target 100% coverage
5. Run `./gradlew check` (type safety + lint + tests)
6. Open PR with conventional commit messages (`feat:`, `fix:`, `refactor:`)

---

## 📄 License

MIT License — see [LICENSE](LICENSE) for details.

---

*TPV Móvil Express v2.0.0 — Built with Clean Architecture, Offline-First, and ❤️*
