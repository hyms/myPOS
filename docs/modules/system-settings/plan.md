# System Settings — Implementation Plan (v2.0.0 KMP)

> **Status (2026-11-14 audit):** Phase 0 Shared Kernel artifacts present in `kmp-app/composeApp` (VOs, rules, SQLDelight schema/migration, unit tests). Gradle compile/codegen/test gates ⏳ PENDING (network/sync/build not verified). Implementation of Phase 5+ items remains unverified/incomplete.

## Overview

This plan covers the implementation of the System Settings module v2.0.0 KMP, including:
- Domain layer: English entities (`UserSettings`, `Currency`, `Money`), Value Objects, rules
- Data layer: Repository interface, SQLite migration from DataStore, singleton pattern
- Application layer: `GetSettings`, `UpdateSettings`, `VerifyPin`, `SetPin`, `ExportDatabase`, `RestoreDatabase`
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

### 4.5 ExportDatabase / RestoreDatabase
**Files:** `backup/ExportDatabase.kt`, `backup/RestoreDatabase.kt`
- Export: `FileSystem.copy` → `ShareSheet`
- Restore: `FilePicker` → close DB → copy → reopen → invalidate DataStores → reload app
- **Restore requires PIN gate** (RULE-SS-04)

---

## Phase 5: Presentation Layer

### 5.1 DataStores (`commonMain/infrastructure/settings/`)
| DataStore | File | Description |
|---|---|---|
| `SettingsDataStore` | `SettingsDataStore.kt` | Hydrated from SQLite: `currencyCode`, `pageSize`, `biometricEnabled`, `adEnabled` |
| `SessionDataStore` | `SessionDataStore.kt` | `unlocked: Boolean` |
| `InvalidationDataStore` | `InvalidationDataStore.kt` | Manual invalidation versions |

### 5.2 ViewModels (`commonMain/presentation/viewmodels/`)
| ViewModel | File | Description |
|---|---|---|
| `SettingsViewModel` | `SettingsViewModel.kt` | Read/write singleton settings |
| `SecurityViewModel` | `SecurityViewModel.kt` | PIN/biometric actions |
| `BackupViewModel` | `BackupViewModel.kt` | Export/Restore with PIN gate |
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
| `BackupActions.kt` | `settings/BackupActions.kt` | **New**: export/restore buttons |
| `ConfirmDialog.kt` | `feedback/ConfirmDialog.kt` | Existing: reusable |
| `AdEnabledToggle.kt` | `settings/AdEnabledToggle.kt` | **New**: master AdWare switch |
| `LockScreen.kt` | `security/LockScreen.kt` | PIN entry + biometric |
| `PinGateDialog.kt` | `security/PinGateDialog.kt` | Modal PIN challenge |

### 5.4 AdWare Integration
**Files to modify:**
- `ExpenseListScreen.kt` — Check `AdEnabledProvider` before rendering `AdBanner`
- `ProductGridScreen.kt` — Check `AdEnabledProvider` before rendering `AdBanner`
- `CashSummaryScreen.kt` — Check `AdEnabledProvider` before rendering `AdBanner`
- `MovementsListScreen.kt` — Check `AdEnabledProvider` before rendering `AdBanner`
- `composeApp/.../ads/AdBanner.kt` — Only renders when enabled

**Rule:** `adEnabled` respected in Expenses list, Catalog, Caja, Movimientos; ignored in forms, ConfirmDialog, PIN gate, Lock, Settings.

### 5.5 i18n
**File:** `composeApp/src/commonMain/composeResources/values-es/strings.xml`
- All 60+ keys from spec.md section 15
- Namespace: `settings_`
- Additional `lock_`, `pin_gate_`, `pin_form_`, `biometric_`, `toast_` namespaces

---

## Phase 6: Migration Verification

### 6.1 Data Integrity Checks
- Row count: `settings` table has exactly 1 row (`id = 1`)
- Columns: `currency_code`, `page_size`, `biometric_enabled`, `ad_enabled`, `updated_at`
- Values migrated from DataStore match (currency, pageSize, biometricEnabled)
- PIN hash unchanged in platform secure storage
- `user_version = 2`

### 6.2 Functional Verification
- Change currency → `Currency` formatter updates globally
- Change pageSize → list ViewModels use new `LIMIT` value
- Toggle biometric → requires auth, persists
- Toggle adEnabled → allowed views show/hide banner
- Export DB → valid `.db` file shared
- Restore DB → requires PIN, reloads app, data intact

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
- `ExportDatabaseTest.kt` — File copied, shared via ShareSheet
- `RestoreDatabaseTest.kt` — Requires PIN, copies file, invalidates DataStores

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
- `SecurityViewModelTest.kt` — PIN/biometric actions
- `BackupViewModelTest.kt` — Export/Restore flow
- `PinGateViewModelTest.kt` — Modal PIN challenge

### 7.6 Compose UI Tests (Robolectric)
**Directory:** `androidTest/compose/settings/`
- `CurrencySelectorTest.kt` — Renders 3 options, selection updates DataStore
- `PageSizeInputTest.kt` — Validates 5-100, rejects out of bounds
- `PinFormTest.kt` — Masked input, length validation, mismatch error
- `BiometricToggleTest.kt` — Toggle triggers auth, updates DataStore on success
- `AdEnabledToggleTest.kt` — Toggle updates `adEnabled`, persists

### 7.7 E2E Tests (Maestro)
**Directory:** `maestro/system-settings/`
- `settingsFlow.yaml` — Open settings → change currency → verify global format
- `pinChangeFlow.yaml` — Change PIN → verify old PIN rejected, new PIN works
- `biometricFlow.yaml` — Enable biometric → auth → toggle persists
- `backupFlow.yaml` — Export → verify file → Restore → verify data intact
- `adEnabledFlow.yaml` — Toggle adEnabled → verify allowed views banner visibility
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
| AdBanner in disallowed views | CI grep check: `grep -r "AdBanner" --include="*.kt" composeApp/.../settings composeApp/.../security composeApp/.../feedback` |
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
- [ ] Restore backup requires PIN confirmation
- [ ] `adEnabled` toggle controls allowed views banner only
- [ ] All 60+ i18n keys implemented
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