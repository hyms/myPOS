# System Settings — Implementation Plan (v2.0.0)

> **Status (2026-11-11):** Phase 2 (atomic migration + repository/use-case SQL foundation) is complete and tested. The `settings` table exists but settings still hydrate from AsyncStorage; AsyncStorage → SQLite migration and presentation work remain pending.

## Overview

This plan covers the implementation of the System Settings module v2.0.0, including:
- Domain layer: English entities (`UserSettings`, `Currency`, `Money`), Value Objects, rules
- Data layer: Repository interface, SQLite migration from AsyncStorage, singleton pattern
- Application layer: `GetSettings`, `UpdateSettings`, `VerifyPin`, `SetPin`, `ExportDatabase`, `RestoreDatabase`
- Presentation layer: Stores, hooks, components, AdWare governance toggle
- Testing: Unit, integration, migration, PIN gate, cash integrity

---

## Phase 1: Database Migration (Critical Path)

### 1.1 Migration Runner Update
**File:** `src/data/database/migrations.ts`
- Register the settings step in the version-2 group (all v2 modules share one `withTransactionSync`)
- `user_version = 2` is set centrally by `runMigrations` after every v2 step succeeds
- Migration order: catalog-inventory → sales-checkout → financial-accumulators → expenses-cashflow → system-settings

### 1.2 Migration SQL
**File:** `src/data/database/migrations/v2_0_0_system_settings_migration.sql.ts` (`SYSTEM_SETTINGS_MIGRATION_SQL`)

```sql
-- Runs inside the shared v2.0.0 transaction (single db.withTransactionSync)

-- Step 1: Create settings table with English columns
CREATE TABLE IF NOT EXISTS settings (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    currency_code TEXT NOT NULL DEFAULT 'Bs',
    page_size INTEGER NOT NULL DEFAULT 15,
    biometric_enabled INTEGER NOT NULL DEFAULT 0,
    ad_enabled INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Step 2: Seed singleton row (id = 1) with defaults
INSERT OR IGNORE INTO settings
    (id, currency_code, page_size, biometric_enabled, ad_enabled, updated_at)
VALUES
    (1, 'Bs', 15, 0, 0, CURRENT_TIMESTAMP);

-- Step 3: Create index (trivial for singleton, but kept for consistency)
CREATE INDEX IF NOT EXISTS idx_settings_id ON settings(id);

-- Step 4: user_version = 2 is set centrally by `runMigrations`
```

> **Note:** AsyncStorage → SQLite migration is application-layer work
> (`GetSettings`), not SQL, and is still pending.

### 1.3 AsyncStorage Migration Logic
**File:** `src/application/settings/MigrateSettings.ts` (pending)
- Read `divisa`, `pageSize`, `biometricEnabled` from `AsyncStorage`
- Upsert into `settings` table
- Clear migrated keys from `AsyncStorage`
- Called by `GetSettings` on first run (detect empty table)

### 1.4 Migration Checklist
- [x] Register the settings step in `migrations.ts` v2 group
- [x] Write `v2_0_0_system_settings_migration.sql.ts` with atomic transaction
- [x] Create `settings` table with English columns + CHECK constraint
- [x] Seed singleton row (id = 1) with defaults
- [ ] Implement AsyncStorage → SQLite migration in `GetSettings`
- [x] Create `idx_settings_id` index
- [x] `user_version` = 2 (centralized)
- [x] Verify row count = 1, columns match English schema
- [ ] Test restore from backup scenario (settings preserved)

---

## Phase 2: Domain Layer

### 2.1 Value Object Files
| New File | Purpose |
|---|---|
| `src/domain/value-objects/Currency.ts` | Currency registry, `Intl.NumberFormat` per locale |
| `src/domain/value-objects/Money.ts` | `Money` interface, `createMoney`, `add`, `subtract`, `format`, `round2` |

### 2.2 Entity Files
| New File | Purpose |
|---|---|
| `src/domain/entities/UserSettings.ts` | Singleton settings aggregate root, `UpdateSettingsInput`, `DEFAULT_SETTINGS` |
| `src/domain/entities/Session.ts` | `SessionState` with `unlocked: boolean` |

### 2.3 Domain Rules
**File:** `src/domain/rules/settingsRules.ts`
- `validatePageSize(pageSize: number): void` — throws if `< 5 || > 100 || !isInteger`
- `validateCurrencyCode(code: string): asserts code is CurrencyCode` — throws if not `'Bs' | '$' | '€'`

---

## Phase 3: Data Layer

### 3.1 Repository Interface
**File:** `src/data/repositories/ISettingsRepository.ts`
- English interface per spec.md section 5.1
- Methods: `get()`, `update(input: UpdateSettingsInput)`

### 3.2 Repository Implementation
**File:** `src/data/repositories/SqliteSettingsRepository.ts`
- Implements `ISettingsRepository`
- Queries `settings` table where `id = 1`
- Maps rows → `UserSettings` entity (snake_case → camelCase)
- Uses `withTransactionSync` for `update`

### 3.3 Container Registration
**File:** `src/data/repositories/container.ts`
- Register `ISettingsRepository` → `SqliteSettingsRepository`

---

## Phase 4: Application Layer

### 4.1 GetSettings
**File:** `src/application/settings/GetSettings.ts`
- Calls `settingsRepository.get()`
- If table empty → returns `DEFAULT_SETTINGS` + triggers AsyncStorage migration

### 4.2 UpdateSettings
**File:** `src/application/settings/UpdateSettings.ts`
- Validates `pageSize` ∈ [5, 100] if provided
- Atomic `withTransactionSync`:
  1. `UPDATE settings SET currency_code = ?, page_size = ?, biometric_enabled = ?, ad_enabled = ?, updated_at = CURRENT_TIMESTAMP WHERE id = 1`
  2. Return updated `UserSettings`

### 4.3 VerifyPin
**File:** `src/application/security/VerifyPin.ts`
- Reads `pin_hash` from `expo-secure-store`
- Computes SHA-256 of input
- Constant-time comparison

### 4.4 SetPin
**File:** `src/application/security/SetPin.ts`
- If `currentPin !== null`, calls `verifyPin(currentPin)` → throw if false
- Computes SHA-256 of `newPin`
- Stores hash in `expo-secure-store` (`pin_hash`)

### 4.5 ExportDatabase / RestoreDatabase
**Files:** `src/application/backup/ExportDatabase.ts`, `src/application/backup/RestoreDatabase.ts`
- Export: `FileSystem.copyAsync` → `Sharing.shareAsync`
- Restore: `DocumentPicker` → close DB → copy → reopen → invalidate stores → reload app
- **Restore requires PIN gate** (RULE-SS-04)

---

## Phase 5: Presentation Layer

### 5.1 Zustand Stores
| Store | File | Changes |
|---|---|---|
| `settingsStore.ts` | `src/presentation/stores/settingsStore.ts` | Hydrated from SQLite: `currencyCode`, `pageSize`, `biometricEnabled`, `adEnabled` |
| `sessionStore.ts` | `src/presentation/stores/sessionStore.ts` | Existing: `unlocked: boolean` |
| `invalidationStore.ts` | `src/presentation/stores/invalidationStore.ts` | Existing: manual invalidation |

### 5.2 Hooks
| Hook | File | Changes |
|---|---|---|
| `useSettings` | `src/presentation/hooks/useSettings.ts` | New: read/write singleton |
| `useSettingsCurrency` | `src/presentation/hooks/useSettingsCurrency.ts` | Existing: returns `Currency` object |
| `useSettingsPageSize` | `src/presentation/hooks/useSettingsPageSize.ts` | Existing: returns `pageSize` |
| `useSettingsSecurity` | `src/presentation/hooks/useSettingsSecurity.ts` | Existing: PIN/biometric actions |
| `useSettingsBackup` | `src/presentation/hooks/useSettingsBackup.ts` | Existing: export/restore with PIN gate |
| `usePinGate` | `src/presentation/hooks/usePinGate.ts` | Existing: modal PIN challenge |
| `useCurrency` | `src/presentation/hooks/useCurrency.ts` | Existing: `Money` formatter |
| `useReducedMotion` | `src/presentation/hooks/useReducedMotion.ts` | Existing: media query |
| `useAdEnabled` | `src/presentation/hooks/useAdEnabled.ts` | **New**: reads `adEnabled` from settings |

### 5.3 Components
| Component | File | Changes |
|---|---|---|
| `SectionHeader` | `src/presentation/components/settings/SectionHeader.tsx` | Existing |
| `CurrencySelector` | `src/presentation/components/settings/CurrencySelector.tsx` | **New**: radio group Bs/$/€ |
| `PageSizeInput` | `src/presentation/components/settings/PageSizeInput.tsx` | **New**: numeric input 5–100 |
| `PinForm` | `src/presentation/components/security/PinForm.tsx` | **New**: 4-6 digit masked input |
| `BiometricToggle` | `src/presentation/components/security/BiometricToggle.tsx` | **New**: switch + auth on enable |
| `BackupActions` | `src/presentation/components/settings/BackupActions.tsx` | **New**: export/restore buttons |
| `ConfirmDialog` | `src/presentation/components/feedback/ConfirmDialog.tsx` | Existing: reusable |
| `AdEnabledToggle` | `src/presentation/components/settings/AdEnabledToggle.tsx` | **New**: master AdWare switch |

### 5.4 AdWare Integration
**Files to modify:**
- `app/gastos/index.tsx` — Check `useAdEnabled()` before rendering `AdBanner`
- `src/presentation/components/ads/AdBanner.tsx` — Only renders when enabled

**Rule:** `adEnabled` respected in Expenses list; ignored in forms, ConfirmDialog, PIN gate, Lock, Settings.

### 5.5 i18n
**File:** `src/presentation/i18n/es/settings.json`
- All 60+ keys from spec.md section 15
- Namespace: `settings`
- Additional `lock`, `pin_gate`, `pin_form`, `biometric`, `toast` namespaces

---

## Phase 6: Migration Verification

### 6.1 Data Integrity Checks
- Row count: `settings` table has exactly 1 row (`id = 1`)
- Columns: `currency_code`, `page_size`, `biometric_enabled`, `ad_enabled`, `updated_at`
- Values migrated from AsyncStorage match (currency, pageSize, biometricEnabled)
- PIN hash unchanged in `expo-secure-store`
- `user_version = 2`

### 6.2 Functional Verification
- Change currency → `useCurrency` formatter updates globally
- Change pageSize → list hooks use new `LIMIT` value
- Toggle biometric → requires auth, persists
- Toggle adEnabled → Expenses list shows/hides banner
- Export DB → valid `.db` file shared
- Restore DB → requires PIN, reloads app, data intact

---

## Phase 7: Testing

### 7.1 Unit Tests
**Directory:** `tests/unit/system-settings/`
- `Currency.test.ts` — Registry, formatter per locale, unsupported code throws
- `Money.test.ts` — `createMoney`, `round2`, `add`, `subtract`, `format`, currency mismatch throws
- `UserSettings.test.ts` — Entity creation, `DEFAULT_SETTINGS`, `UpdateSettingsInput` partial
- `settingsRules.test.ts` — `validatePageSize` (boundaries), `validateCurrencyCode` (valid/invalid)

### 7.2 Integration Tests
**Directory:** `tests/integration/system-settings/`
- `GetSettings.test.ts` — Reads singleton, triggers AsyncStorage migration on first run
- `UpdateSettings.test.ts` — Atomic update, validation, returns updated entity
- `VerifyPin.test.ts` — Correct PIN true, incorrect PIN false, constant-time
- `SetPin.test.ts` — First-time set, change with current PIN, rejects wrong current PIN
- `ExportDatabase.test.ts` — File copied, shared via Sharing
- `RestoreDatabase.test.ts` — Requires PIN, copies file, invalidates stores

### 7.3 Migration Tests
**Directory:** `tests/migration/system-settings/`
- `v2_0_0_migration.test.ts` — Seed v1 AsyncStorage, run migration, assert SQLite row
- `asyncStorageMigration.test.ts` — `GetSettings` migrates keys, clears AsyncStorage

### 7.4 PIN Gate Tests
**Directory:** `tests/integration/security/`
- `pinGate.test.ts` — Blocks critical actions without PIN, allows with correct PIN
- `pinForm.test.tsx` — 4-6 digit validation, mismatch detection, masked input

### 7.5 Hook Tests
**Directory:** `tests/unit/hooks/`
- `useSettings.test.ts` — Read/write singleton, triggers re-render on change
- `useSettingsCurrency.test.ts` — Returns correct `Currency` per `currencyCode`
- `useSettingsPageSize.test.ts` — Returns `pageSize` for pagination hooks
- `useAdEnabled.test.ts` — Returns `adEnabled` boolean

### 7.6 UI Component Tests
**Directory:** `tests/components/settings/`
- `CurrencySelector.test.tsx` — Renders 3 options, selection updates store
- `PageSizeInput.test.tsx` — Validates 5-100, rejects out of bounds
- `PinForm.test.tsx` — Masked input, length validation, mismatch error
- `BiometricToggle.test.tsx` — Toggle triggers auth, updates store on success
- `AdEnabledToggle.test.tsx` — Toggle updates `adEnabled`, persists

### 7.7 E2E Tests
**Directory:** `tests/e2e/system-settings/`
- `settingsFlow.test.ts` — Open settings → change currency → verify global format
- `pinChangeFlow.test.ts` — Change PIN → verify old PIN rejected, new PIN works
- `biometricFlow.test.ts` — Enable biometric → auth → toggle persists
- `backupFlow.test.ts` — Export → verify file → Restore → verify data intact
- `adEnabledFlow.test.ts` — Toggle adEnabled → verify Expenses list banner visibility
- `migrationIdempotency.test.ts` — Run migration twice → no errors, single row

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
| Migration order conflict | Single `migrations.ts` runner with explicit ordering; `PRAGMA user_version` checked |
| AsyncStorage migration race | `GetSettings` checks table empty → migrates → upserts; idempotent |
| `CHECK (id = 1)` insert conflict | `INSERT OR IGNORE` + `UPDATE` pattern in repository |
| PIN hash timing attack | Constant-time comparison in `VerifyPin` |
| AdBanner in disallowed views | CI grep check: `grep -r "AdBanner" --include="*.tsx" app/gastos/nuevo.tsx src/presentation/components/feedback/ConfirmDialog.tsx app/lock.tsx app/settings/` |
| i18n keys missing | CI check: `grep -r "settings\." --include="*.tsx" src/presentation/components/settings` |
| Currency formatter mismatch | Unit test asserts `format()` matches `Intl.NumberFormat` for each locale |

---

## Definition of Done

- [x] Migration runs atomically, preserves all data, coordinates with other modules
- [ ] All entities/repos/use cases use English identifiers
- [x] `UserSettings` singleton in SQLite with English columns
- [ ] `Currency`/`Money` value objects used globally across modules
- [ ] `GetSettings` migrates AsyncStorage on first run
- [ ] `UpdateSettings` works atomically with validation
- [ ] PIN change requires current PIN (except first time)
- [ ] Biometric toggle requires auth before enabling
- [ ] Restore backup requires PIN confirmation
- [ ] `adEnabled` toggle controls Expenses list banner only
- [ ] All 60+ i18n keys implemented
- [x] Unit tests: 100% pass
- [x] Integration tests: 100% pass
- [x] Migration tests: 100% pass
- [ ] PIN gate tests: 100% pass
- [x] TypeScript strict: no errors
- [ ] Lint: no errors

---

*Plan version: 1.0 — 2026-11-10*