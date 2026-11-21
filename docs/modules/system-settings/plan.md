# System Settings — Implementation Plan (v2.0.0 KMP)

> **Status (2026-11-14 audit):** Phase 0 Shared Kernel artifacts present in `kmp-app/composeApp` (VOs, rules, SQLDelight schema/migration, unit tests). Gradle compile/codegen/test gates ⏳ PENDING (network/sync/build not verified). Implementation of Phase 5+ items remains unverified/incomplete.

## Overview

This plan covers the implementation of the System Settings module v2.0.0 KMP, including:
- Domain layer: English entities (`UserSettings`, `Currency`, `Money`), Value Objects, rules
- Data layer: Repository interface, SQLite migration from DataStore, singleton pattern
- Application layer: `GetSettings`, `UpdateSettings`, `VerifyPin`, `SetPin`, `ExportBackupUseCase`, `ImportBackupUseCase`
- Presentation layer: ViewModels, Compose UI components, AdWare governance toggle
- Testing: Unit, integration, migration, PIN gate, cash integrity

---

## Phase 1: Database Migration (Critical Path)

### 1.1 Migration Runner Update
**File:** `commonMain/data/database/DatabaseDriverFactory.kt`
- Register the settings step in the version-2 migration list (all v2 modules share one transaction)
- `user_version = 2` is set centrally by `DatabaseDriverFactory` after every v2 step succeeds
- Migration order: catalog-inventory → sales-checkout → financial-accumulators → expenses-cashflow → system-settings

### 1.2 Migration SQL
**File:** `commonMain/sqldelight/migrations/v2_system_settings.sqm` (see spec.md section 4.3)

### 1.3 DataStore Migration Logic
**File:** `commonMain/application/settings/MigrateSettings.kt`
- Read `divisa`, `pageSize`, `biometricEnabled` from `DataStore`/`SharedPreferences`
- Upsert into `settings` table
- Clear migrated keys from `DataStore`
- Called by `GetSettings` on first run (detect empty table)

### 1.4 Migration Checklist
- [ ] Register the settings step in `DatabaseDriverFactory` v2 migration list
- [ ] Write `v2_system_settings.sqm` with atomic transaction
- [ ] Create `settings` table with English columns + CHECK constraint
- [ ] Seed singleton row (id = 1) with defaults
- [ ] Implement DataStore → SQLite migration in `GetSettings`
- [ ] Create `idx_settings_id` index
- [ ] `user_version` = 2 (centralized)
- [ ] Verify row count = 1, columns match English schema
- [ ] Test restore from backup scenario (settings preserved)

---

## Phase 2: Domain Layer (`commonMain/domain/`)

### 2.1 Value Object Files
| New File | Purpose |
|---|---|
| `valueobjects/Currency.kt` | Currency registry, `NumberFormat` per locale |
| `valueobjects/Money.kt` | `Money` data class, `create`, `plus`, `minus`, `format`, `round2` |

### 2.2 Entity Files
| New File | Purpose |
|---|---|
| `entities/UserSettings.kt` | Singleton settings aggregate root, `UpdateSettingsInput`, `DEFAULT_SETTINGS` |
| `entities/SessionState.kt` | `SessionState` with `unlocked: Boolean` |

### 2.3 Domain Rules
**File:** `rules/SettingsRules.kt`
- `validatePageSize(pageSize: Int): Unit` — throws if `< 5 || > 100`
- `validateCurrencyCode(code: String): CurrencyCode` — throws if not `'BS' | 'USD' | 'EUR'`

---

## Phase 3: Data Layer (`commonMain/data/repositories/`)

### 3.1 Repository Interface
**File:** `ISettingsRepository.kt`
- English interface per spec.md section 5.1
- Methods: `get()`, `update(input: UpdateSettingsInput)`

### 3.2 Repository Implementation
**File:** `impl/SqlDelightSettingsRepository.kt`
- Implements `ISettingsRepository`
- Queries `settings` table where `id = 1`
- Maps rows → `UserSettings` entity (snake_case → camelCase)
- Uses transaction block for `update`

### 3.3 Registration
**File:** `commonMain/data/database/DatabaseDriverFactory.kt`
- Register `ISettingsRepository` → `SqlDelightSettingsRepository`

---

## Phase 4: Application Layer (`commonMain/application/`)

### 4.1 GetSettings
**File:** `settings/GetSettings.kt`
- Calls `settingsRepository.get()`
- If table empty → returns `DEFAULT_SETTINGS` + triggers DataStore migration

### 4.2 UpdateSettings
**File:** `settings/UpdateSettings.kt`
- Validates `pageSize` ∈ [5, 100] if provided
- Atomic transaction block:
  1. `UPDATE settings SET currency_code = ?, page_size = ?, biometric_enabled = ?, ad_enabled = ?, updated_at = CURRENT_TIMESTAMP WHERE id = 1`
  2. Return updated `UserSettings`

### 4.3 VerifyPin
**File:** `security/VerifyPin.kt`
- Reads `pin_hash` from platform secure storage (Android Keystore / iOS Keychain)
- Computes SHA-256 of input
- Constant-time comparison

### 4.4 SetPin
**File:** `security/SetPin.kt`
- If `currentPin != null`, calls `verifyPin(currentPin)` → throw if false
- Computes SHA-256 of `newPin`
- Stores hash in platform secure storage (`pin_hash`)

### 4.5 ExportBackup / ImportBackup
**Files:** `backup/ExportBackupUseCase.kt`, `backup/ImportBackupUseCase.kt`, `backup/LegacyMapper.kt`, `backup/BackupModels.kt`

- **Export:** read all tables → serialize versioned JSON envelope (`format: "tpv-backup"`, `schemaVersion: 2`) → write `tpv-backup-YYYYMMDD-HHmm.json` → platform share sheet.
- **Import:** PIN gate (`VerifyPin`) → accept `BackupSource.Json` **or** `BackupSource.LegacySqliteFile` (the RN `.db` format) → normalize enums and timestamps via `LegacyMapper` → apply safe defaults (`sku = null`, `categories.updated_at = registered_at`, default expense category, settings fallback `Bs/15/false`) → single SQLDelight `transaction { }` wipe-and-insert → recompute `financial_accumulators` from `sales_transactions` → invalidate all ViewModels → return `ImportReport`.
- **Atomicity:** any failure rolls back the import; the accumulator is never trusted from the file.
- **Restore requires PIN gate** (RULE-SS-04). Contract details: `spec.md` §13.

---

## Phase 5: Presentation Layer

### 5.1 DataStores (`commonMain/infrastructure/settings/`)
| DataStore | File | Description |
|---|---|---|
| `SettingsDataStore` | `SettingsDataStore.kt` | Hydrated from SQLite: `currencyCode`, `pageSize`, `biometricEnabled` |
| `SessionDataStore` | `SessionDataStore.kt` | `unlocked: Boolean` |
| `InvalidationDataStore` | `InvalidationDataStore.kt` | Manual invalidation versions |

### 5.2 ViewModels (`commonMain/presentation/viewmodels/`)
| ViewModel | File | Description |
|---|---|---|
| `SettingsViewModel` | `SettingsViewModel.kt` | Read/write singleton settings |
| `PinViewModel` | `PinViewModel.kt` | Verify/set/change PIN, lock state, failed-attempt throttling |
| `SecurityViewModel` | `SecurityViewModel.kt` | Biometric availability and toggle actions |
| `BackupViewModel` | `BackupViewModel.kt` | Export/Import (JSON + legacy `.db`) with PIN gate |
| `PinGateViewModel` | `PinGateViewModel.kt` | Modal PIN challenge |

### 5.3 Compose UI Components (`composeApp/src/commonMain/compose/`)

| Component | File | Description |
|---|---|---|
| `SettingsScreen.kt` | `settings/SettingsScreen.kt` | Main settings with sections |
| `SectionHeader.kt` | `settings/SectionHeader.kt` | Section title with divider |
| `CurrencySelector.kt` | `settings/CurrencySelector.kt` | **New**: radio group Bs/$/€ |
| `PageSizeInput.kt` | `settings/PageSizeInput.kt` | **New**: numeric input 5–100 |
| `PinForm.kt` | `security/PinForm.kt` | **New**: 4-6 digit masked input |
| `BiometricToggle.kt` | `security/BiometricToggle.kt` | **New**: switch + auth on enable |
| `BackupActions.kt` | `settings/BackupActions.kt` | **New**: export/import buttons |
| `ConfirmDialog.kt` | `feedback/ConfirmDialog.kt` | Existing: reusable |
| `FlavorInfoCard.kt` | `settings/FlavorInfoCard.kt` | **New**: read-only flavor/ads status (no toggle) |
| `LockScreen.kt` | `security/LockScreen.kt` | PIN entry + biometric |
| `PinLockOverlay.kt` | `security/PinLockOverlay.kt` | **New**: app-start and critical-action gate |
| `PinGateDialog.kt` | `security/PinGateDialog.kt` | Modal PIN challenge |

### 5.4 AdWare Integration (AppConfig)
**Files to configure:**
- `commonMain` — `AppConfig` singleton (`ADS_ENABLED: Boolean` `false` during development + theme config) + `expect fun isAdsEnabled(): Boolean` + `LocalAdsEnabled` CompositionLocal provided at `App()`
- `androidMain` — actual reads `AppConfig.ADS_ENABLED`
- `ExpenseListScreen.kt`, `ProductListScreen.kt`, `CashSummaryScreen.kt`, `MovementsScreen.kt` — footer `AdBanner` only when `LocalAdsEnabled.current`
- `SettingsScreen.kt` — replace toggle with `FlavorInfoCard`

**Rule:** ads exist only when `AppConfig.ADS_ENABLED`; `false` during development. No runtime toggle, no `settings.ad_enabled` reads.

### 5.5 i18n
**File:** `composeApp/src/commonMain/composeResources/values-es/strings.xml`
- All keys from `spec.md` §16
- Namespaces: `settings_`, `lock_`, `pin_gate_`, `pin_form_`, `biometric_`, `toast_`

---

## Phase 6: Migration Verification

### 6.1 Data Integrity Checks
- Row count: `settings` table has exactly 1 row (`id = 1`)
- Columns: `currency_code`, `page_size`, `biometric_enabled`, `updated_at` (`ad_enabled` retained but deprecated/ignored)
- Values migrated from DataStore match (currency, pageSize, biometricEnabled)
- PIN hash unchanged in platform secure storage
- `user_version = 2`

### 6.2 Functional Verification
- Change currency → `Currency` formatter updates globally
- Change pageSize → list ViewModels use new `LIMIT` value
- Toggle biometric → requires auth, persists
- Banners show in allowed views only when `AppConfig.ADS_ENABLED`; no ad code in forbidden views
- Export backup → valid JSON v2 file shared
- Import backup → requires PIN, atomic, accumulators recomputed
- Import legacy `.db` → enums translated, row counts match, app state invalidated

---

## Phase 7: Testing

### 7.1 Unit Tests
**Directory:** `commonTest/unit/system-settings/`
- `CurrencyTest.kt` — Registry, formatter per locale, unsupported code throws
- `MoneyTest.kt` — `create`, `round2`, `plus`, `minus`, `format`, currency mismatch throws
- `UserSettingsTest.kt` — Entity creation, `DEFAULT_SETTINGS`, `UpdateSettingsInput` partial
- `SettingsRulesTest.kt` — `validatePageSize` (boundaries), `validateCurrencyCode` (valid/invalid)

### 7.2 Integration Tests
**Directory:** `commonTest/integration/system-settings/`
- `GetSettingsTest.kt` — Reads singleton, triggers DataStore migration on first run
- `UpdateSettingsTest.kt` — Atomic update, validation, returns updated entity
- `VerifyPinTest.kt` — Correct PIN true, incorrect PIN false, constant-time
- `SetPinTest.kt` — First-time set, change with current PIN, rejects wrong current PIN
- `ExportBackupTest.kt` — Serializes all tables into the JSON v2 envelope
- `ImportBackupTest.kt` — JSON round-trip is atomic, idempotent, accumulators recomputed
- `LegacyDbImportTest.kt` — `rn-app` `.db` fixture: enum translation, `sku = null`, timestamp normalization

### 7.3 Migration Tests
**Directory:** `commonTest/migration/system-settings/`
- `V2SystemSettingsMigrationTest.kt` — Seed v1 DataStore, run migration, assert SQLite row
- `DataStoreMigrationTest.kt` — `GetSettings` migrates keys, clears DataStore

### 7.4 PIN Gate Tests
**Directory:** `commonTest/integration/security/`
- `PinGateTest.kt` — Blocks critical actions without PIN, allows with correct PIN
- `PinFormTest.kt` — 4-6 digit validation, mismatch detection, masked input

### 7.5 ViewModel Tests (Turbine)
**Directory:** `commonTest/viewmodels/system-settings/`
- `SettingsViewModelTest.kt` — Read/write singleton, triggers re-render on change
- `PinViewModelTest.kt` — Verify/set/change, wrong-current rejection, lock state
- `SecurityViewModelTest.kt` — Biometric availability and toggle actions
- `BackupViewModelTest.kt` — Export/Import flow, PIN gate, failure rollback
- `PinGateViewModelTest.kt` — Modal PIN challenge

### 7.6 Compose UI Tests (Robolectric)
**Directory:** `androidTest/compose/settings/`
- `CurrencySelectorTest.kt` — Renders 3 options, selection updates DataStore
- `PageSizeInputTest.kt` — Validates 5-100, rejects out of bounds
- `PinFormTest.kt` — Masked input, length validation, mismatch error
- `BiometricToggleTest.kt` — Toggle triggers auth, updates DataStore on success
- `FlavorInfoCardTest.kt` — Renders read-only flavor label; no interactive ad control exists

### 7.7 E2E Tests (Maestro)
**Directory:** `maestro/system-settings/`
- `settingsFlow.yaml` — Open settings → change currency → verify global format
- `pinChangeFlow.yaml` — Change PIN → verify old PIN rejected, new PIN works
- `biometricFlow.yaml` — Enable biometric → auth → toggle persists
- `backupFlow.yaml` — Export → verify file → Import → verify data intact
- `legacyImportFlow.yaml` — Import `rn-app` `.db` → verify translated rows
- `singleBuildInstallFlow.yaml` — Install `com.copitoB.mypos` → local data preserved across reinstalls
- `migrationIdempotency.yaml` — Run migration twice → no errors, single row

---

## Dependencies & Ordering

```
Phase 1 (Migration) → Phase 2 (Domain) → Phase 3 (Data) → Phase 4 (Application) → Phase 5 (Presentation) → Phase 6 (Verification) → Phase 7 (Testing)
```

**Parallelizable:**
- Phase 2 & 3 can run in parallel after Phase 1
- Phase 7 tests can be written alongside implementation

**Cross-module coordination:**
- `catalog-inventory` migration runs first
- `sales-checkout` migration runs second
- `financial-accumulators` migration runs third
- `expenses-cashflow` migration runs fourth
- `system-settings` migration runs fifth
- All share `user_version = 2`

---

## Risk Mitigation

| Risk | Mitigation |
|---|---|
| Migration order conflict | Single `DatabaseDriverFactory` with explicit ordering; `PRAGMA user_version` checked |
| DataStore migration race | `GetSettings` checks table empty → migrates → upserts; idempotent |
| `CHECK (id = 1)` insert conflict | `INSERT OR IGNORE` + `UPDATE` pattern in repository |
| PIN hash timing attack | Constant-time comparison in `VerifyPin` |
| AdBanner in disallowed views | CI grep check over `settings/`, `security/`, `feedback/`, `cart/`, `purchases/`; no ad code path in single build |
| Flavor upgrade wipes data | Single build `com.copitoB.mypos`; reinstalls preserve local data |
| Backup import corrupts data | Single transaction + row-count assertions + rollback test |
| Legacy `.db` mis-mapped | Fixture-based test asserting enums, timestamps, `sku = null` |
| i18n keys missing | CI check: `grep -r "settings_" --include="*.kt" composeApp/.../settings` |
| Currency formatter mismatch | Unit test asserts `format()` matches `NumberFormat` for each locale |

---

## Definition of Done

- [ ] Migration runs atomically, preserves all data, coordinates with other modules
- [ ] All entities/repos/use cases use English identifiers
- [ ] `UserSettings` singleton in SQLite with English columns
- [ ] `Currency`/`Money` value objects used globally across modules
- [ ] `GetSettings` migrates DataStore on first run
- [ ] `UpdateSettings` works atomically with validation
- [ ] PIN change requires current PIN (except first time)
- [ ] Biometric toggle requires auth before enabling
- [ ] Restore/import requires PIN confirmation and is fully atomic
- [ ] Ads are compile-time only via `AppConfig.ADS_ENABLED` (`false` during development), zero runtime UI toggle, read-only flavor card in Settings
- [ ] Export produces a JSON v2 envelope; import accepts JSON and legacy `rn-app` `.db`
- [ ] Import translates enums/timestamps and recomputes `financial_accumulators`
- [ ] All i18n keys implemented
- [ ] Unit tests: 100% pass
- [ ] Integration tests: 100% pass
- [ ] Migration tests: 100% pass
- [ ] PIN gate tests: 100% pass
- [ ] ViewModel tests: 100% pass
- [ ] Compose UI tests: 100% pass
- [ ] Kotlin strict: no errors
- [ ] Detekt: no errors

---

*Plan version: 2.0 — 2026-11-12 — KMP Migration*