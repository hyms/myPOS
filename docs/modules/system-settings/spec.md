# System Settings — Specification (v2.0.0 KMP)

## 1. Bounded Context

Responsible for global application configuration: currency (i18n), pagination defaults, security (PIN/biometric), backup/restore, and the master `AD_ENABLED` flag for AdWare governance. Persists a **singleton settings record** (`id = 1`) in SQLite with English column names, replacing the v1.0.0 `DataStore`/`SharedPreferences`-only approach. All code/DB/identifiers in **English**; only visible UI strings remain in Spanish via i18n keys.

---

## 2. Domain Entities & Value Objects

All identifiers, fields, and types in code and database are in **English**.

### 2.1 Currency (Value Object)

**File:** `commonMain/domain/valueobjects/Currency.kt`

```kotlin
package com.tpvmovil.domain.valueobjects

enum class CurrencyCode { BS, USD, EUR }

data class Currency(
    val code: CurrencyCode,
    val symbol: String,
    val locale: String,           // e.g., 'es-BO', 'en-US', 'es-ES'
    val formatter: java.text.NumberFormat
) {
    companion object {
        private val REGISTRY = mapOf(
            CurrencyCode.BS to Currency(
                CurrencyCode.BS, "Bs", "es-BO",
                java.text.NumberFormat.getCurrencyInstance(java.util.Locale("es", "BO")).apply {
                    currency = java.util.Currency.getInstance("BOB")
                    minimumFractionDigits = 2
                    maximumFractionDigits = 2
                }
            ),
            CurrencyCode.USD to Currency(
                CurrencyCode.USD, "$", "en-US",
                java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "US")).apply {
                    currency = java.util.Currency.getInstance("USD")
                    minimumFractionDigits = 2
                    maximumFractionDigits = 2
                }
            ),
            CurrencyCode.EUR to Currency(
                CurrencyCode.EUR, "€", "es-ES",
                java.text.NumberFormat.getCurrencyInstance(java.util.Locale("es", "ES")).apply {
                    currency = java.util.Currency.getInstance("EUR")
                    minimumFractionDigits = 2
                    maximumFractionDigits = 2
                }
            )
        )

        fun getCurrency(code: CurrencyCode): Currency = REGISTRY[code]
            ?: throw IllegalArgumentException("Unsupported currency: $code")
    }
}
```

### 2.2 Money (Value Object)

**File:** `commonMain/domain/valueobjects/Money.kt`

```kotlin
package com.tpvmovil.domain.valueobjects

data class Money(
    val amount: Double,     // base currency units, 2dp (e.g., 123.45)
    val currency: Currency
) {
    companion object {
        fun create(amount: Double, currency: Currency): Money {
            require(amount.isFinite()) { "Amount must be finite" }
            return Money(round2(amount), currency)
        }

        fun zero(currency: Currency): Money = Money(0.0, currency)
    }

    fun round2(n: Double): Double = (n * 100).roundToInt() / 100.0

    fun plus(other: Money): Money {
        require(this.currency.code == other.currency.code) { "Currency mismatch" }
        return Money(round2(this.amount + other.amount), this.currency)
    }

    fun minus(other: Money): Money {
        require(this.currency.code == other.currency.code) { "Currency mismatch" }
        return Money(round2(this.amount - other.amount), this.currency)
    }

    fun format(): String = currency.formatter.format(amount)
}
```

### 2.3 UserSettings (Aggregate Root — Singleton)

**File:** `commonMain/domain/entities/UserSettings.kt`

```kotlin
package com.tpvmovil.domain.entities

import com.tpvmovil.domain.valueobjects.CurrencyCode

@JvmInline
value class SettingsId(val value: Long) {  // always 1
    companion object { val SINGLETON = SettingsId(1) }
}

data class UserSettings(
    val id: SettingsId = SettingsId.SINGLETON,                    // always 1
    val currencyCode: CurrencyCode = CurrencyCode.BS,            // BS | USD | EUR
    val pageSize: Int = 15,                                      // default 15, min 5, max 100
    val biometricEnabled: Boolean = false,                       // false by default
    val adEnabled: Boolean = false,                              // master AdWare toggle, default false
    val updatedAt: String = java.time.Instant.now().toString()   // ISO 8601
)

data class UpdateSettingsInput(
    val currencyCode: CurrencyCode? = null,
    val pageSize: Int? = null,
    val biometricEnabled: Boolean? = null,
    val adEnabled: Boolean? = null
)
```

### 2.4 Session (UI State)

**File:** `commonMain/domain/entities/SessionState.kt`

```kotlin
package com.tpvmovil.domain.entities

data class SessionState(
    val unlocked: Boolean = false
)
```

---

## 3. Domain Rules & Invariants

| ID | Rule | Description |
|---|---|---|
| RULE-SS-01 | PIN change requires current PIN | `PinService.setPin(newPin)` demands current PIN hash match, except first-time setup. |
| RULE-SS-02 | Biometric toggle requires auth | Enabling biometric invokes `BiometricManager.authenticate()` first. |
| RULE-SS-03 | PIN gate for critical actions | Change PIN, delete transaction, restore backup → `PinGateViewModel` enforces current PIN. |
| RULE-SS-04 | Restore backup **requires** PIN | v2.0.0 change: restoring backup now requires PIN confirmation (critical data overwrite). `ConfirmDialog`: `"Esto sobrescribirá todos los datos actuales. Ingrese su PIN para confirmar."` |
| RULE-SS-05 | Currency is visual only | `Currency`/`Money` affect formatting only. SQLite stores pure numeric amounts in base currency. |
| RULE-SS-06 | Pagination default | `pageSize` default 15. All historical list ViewModels apply `LIMIT ? OFFSET ?` with this value. |
| RULE-SS-07 | AD_ENABLED master toggle | `adEnabled` in `UserSettings` governs whether banner ads render in allowed views (Expenses list, Catalog, Caja, Movimientos). |
| RULE-SS-08 | Settings singleton | Only one row exists (`id = 1`, `CHECK (id = 1)`). Upsert on update. |

Global invariants: **INV-08** (PIN-protected critical actions), **INV-10** (strict Kotlin).

---

## 4. Database Contract (SQLDelight)

### 4.1 Target Schema (v2.0.0 — English Columns)

**File:** `commonMain/sqldelight/schema.sq` (settings section)

```sql
-- settings (singleton, id = 1)
CREATE TABLE settings (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    currency_code TEXT NOT NULL DEFAULT 'BS',   -- 'BS' | 'USD' | 'EUR'
    page_size INTEGER NOT NULL DEFAULT 15,
    biometric_enabled INTEGER NOT NULL DEFAULT 0,  -- 0/1
    ad_enabled INTEGER NOT NULL DEFAULT 0,         -- 0/1
    updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
);

INSERT OR IGNORE INTO settings (id, currency_code, page_size, biometric_enabled, ad_enabled, updated_at)
VALUES (1, 'BS', 15, 0, 0, strftime('%s', 'now') * 1000);
```

### 4.2 Column Mapping: v1.0.0 (Spanish/DataStore) → v2.0.0 (English/SQLite)

v1.0.0 stored settings in `DataStore`/`SharedPreferences` with Spanish keys. v2.0.0 migrates to SQLite `settings` table.

| v1.0.0 DataStore Key | v2.0.0 Table | v2.0.0 Column | Type | Default |
|---|---|---|---|---|
| `divisa` | `settings` | `currency_code` | TEXT | `'BS'` |
| `pageSize` | `settings` | `page_size` | INTEGER | `15` |
| `biometricEnabled` | `settings` | `biometric_enabled` | INTEGER (0/1) | `0` |
| *(new)* | `settings` | `ad_enabled` | INTEGER (0/1) | `0` |
| *(new)* | `settings` | `updated_at` | INTEGER (epoch millis) | `CURRENT_TIMESTAMP` |

> **Note:** PIN hash remains in platform secure storage (Android Keystore / iOS Keychain), not migrated to SQLite.

### 4.3 Migration DDL (v1.0.0 → v2.0.0)

**File:** `commonMain/sqldelight/migrations/v2_system_settings.sqm`

```sql
-- v2_system_settings.sqm
-- Atomic migration transaction (single transaction block)

-- Step 1: Create settings table with English columns
CREATE TABLE IF NOT EXISTS settings (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    currency_code TEXT NOT NULL DEFAULT 'BS',
    page_size INTEGER NOT NULL DEFAULT 15,
    biometric_enabled INTEGER NOT NULL DEFAULT 0,
    ad_enabled INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
);

-- Step 2: Seed singleton row (id = 1) with defaults
INSERT OR IGNORE INTO settings (id, currency_code, page_size, biometric_enabled, ad_enabled, updated_at)
VALUES (1, 'BS', 15, 0, 0, strftime('%s', 'now') * 1000);

-- Step 3: Migrate DataStore/SharedPreferences values to SQLite (application-layer, not SQL)
-- Handled by GetSettings use case on first run: read DataStore keys,
-- write to settings table, then clear DataStore keys.

-- Step 4: Create index (trivial for singleton, but kept for consistency)
CREATE INDEX IF NOT EXISTS idx_settings_id ON settings(id);

-- Step 5: Update user_version to 2 (coordinated with other modules)
-- user_version = 2 is set centrally by DatabaseDriverFactory after every v2.0.0
-- module step succeeds inside the same transaction.
```

---

## 5. Repository Interfaces

### 5.1 ISettingsRepository

**File:** `commonMain/data/repositories/ISettingsRepository.kt`

```kotlin
package com.tpvmovil.data.repositories

import com.tpvmovil.domain.entities.*

interface ISettingsRepository {
    fun get(): UserSettings                           // singleton read (id = 1)
    fun update(input: UpdateSettingsInput): UserSettings  // upsert singleton
}
```

---

## 6. Use Cases (Application Layer — `commonMain`)

### 6.1 GetSettings

**File:** `commonMain/application/settings/GetSettings.kt`

```kotlin
package com.tpvmovil.application.settings

import com.tpvmovil.domain.entities.UserSettings

suspend fun getSettings(): UserSettings
```

**Flow:**
1. `settingsRepository.get()` → returns `UserSettings`.
2. If table empty (first run after migration), returns `DEFAULT_SETTINGS` and triggers migration from DataStore.

### 6.2 UpdateSettings

**File:** `commonMain/application/settings/UpdateSettings.kt`

```kotlin
package com.tpvmovil.application.settings

import com.tpvmovil.domain.entities.*

suspend fun updateSettings(input: UpdateSettingsInput): UserSettings
```

**Atomic Flow (SQLDelight transaction block):**
1. Validate `pageSize` ∈ [5, 100] if provided.
2. `UPDATE settings SET currency_code = ?, page_size = ?, biometric_enabled = ?, ad_enabled = ?, updated_at = CURRENT_TIMESTAMP WHERE id = 1`.
3. Return updated `UserSettings`.

### 6.3 VerifyPin (PIN Gate)

**File:** `commonMain/application/security/VerifyPin.kt`

```kotlin
package com.tpvmovil.application.security

suspend fun verifyPin(inputPin: String): Boolean
```

**Flow:**
1. Read PIN hash from platform secure storage (`pin_hash`).
2. Compute SHA-256 of `inputPin`.
3. Compare constant-time. Return `true`/`false`.

### 6.4 SetPin

**File:** `commonMain/application/security/SetPin.kt`

```kotlin
package com.tpvmovil.application.security

suspend fun setPin(currentPin: String?, newPin: String)
```

**Flow:**
1. If `currentPin != null`, call `verifyPin(currentPin)` → throw if false.
2. Compute SHA-256 of `newPin`.
3. Store hash in platform secure storage (`pin_hash`).

### 6.5 ExportDatabase / RestoreDatabase

**Files:** `commonMain/application/backup/ExportDatabase.kt`, `RestoreDatabase.kt`

- `ExportDatabase`: Copy SQLite file to `documentDirectory/backups/tpv-YYYYMMDD-HHmm.db` → `ShareSheet`.
- `RestoreDatabase`: `FilePicker` → close DB → copy over main DB → reopen DB → invalidate all ViewModels → reload app (via `ProcessPhoenix` or in-memory store reset). **Requires PIN gate (RULE-SS-04).**

---

## 7. Domain Rules (Settings Math)

**File:** `commonMain/domain/rules/SettingsRules.kt`

| Function | Signature | Description |
|---|---|---|
| `validatePageSize` | `(pageSize: Int) -> Unit` | Throws if `pageSize < 5 || pageSize > 100`. |
| `validateCurrencyCode` | `(code: String) -> CurrencyCode` | Throws if not `'BS' \| 'USD' \| 'EUR'`. |

---

## 8. UI State Machines (Spanish visible text via `stringResource`)

### 8.1 Lock Screen (`LockScreen`)

| State | Trigger | Next State | Visible Text (ES) |
|---|---|---|---|
| `Locked` | App start, PIN configured | `Authenticating` | `"Ingrese su PIN"` |
| `Authenticating` | Biometric success | `Unlocked` | (silent) |
| `Authenticating` | PIN correct | `Unlocked` | (silent) |
| `Authenticating` | PIN incorrect | `Error` | `"PIN incorrecto"` |
| `Authenticating` | Biometric fail / cancel | `Locked` | `"Ingrese su PIN"` |

### 8.2 Settings Screen (`SettingsScreen`)

| State | Trigger | Next State | Visible Text (ES) |
|---|---|---|---|
| `Settings` | Open | — | `"Configuración"` |
| `Currency` | Tap currency row | `CurrencySelector` | `"Bs" / "$" / "€"` |
| `Pagination` | Tap pageSize row | `PageSizeInput` | `"Filas por consulta"` |
| `Security` | Tap change PIN | `PinGate` → `PinForm` | `"Nuevo PIN"` / `"PIN actual"` |
| `Security` | Toggle biometric | `BiometricAuth` | `"Activar huella/dactilar"` |
| `Backup` | Tap Export | `Exporting` | `"Exportar copia de seguridad"` |
| `Backup` | Tap Restore | `PinGate` → `ConfirmDialog` | `"Esto sobrescribirá todos los datos actuales. Ingrese su PIN para confirmar."` |

---

## 9. Compose UI Components

| Component | File | Type | Memoized | Description |
|---|---|---|---|---|
| `SettingsScreen` | `composeApp/.../settings/SettingsScreen.kt` | Screen | N/A | Main settings with sections |
| `SectionHeader` | `composeApp/.../settings/SectionHeader.kt` | Composable | No | Section title with divider |
| `CurrencySelector` | `composeApp/.../settings/CurrencySelector.kt` | Composable | No | Radio group: Bs / $ / € |
| `PageSizeInput` | `composeApp/.../settings/PageSizeInput.kt` | Composable | No | Numeric input (5–100) |
| `PinForm` | `composeApp/.../security/PinForm.kt` | Composable | No | 4–6 digit PIN input, masked |
| `BiometricToggle` | `composeApp/.../security/BiometricToggle.kt` | Composable | No | Switch + auth on enable |
| `BackupActions` | `composeApp/.../settings/BackupActions.kt` | Composable | No | Export / Restore buttons |
| `ConfirmDialog` | `composeApp/.../feedback/ConfirmDialog.kt` | Composable | No | Reusable, interpolated text |
| `AdEnabledToggle` | `composeApp/.../settings/AdEnabledToggle.kt` | Composable | No | Master AdWare switch |
| `LockScreen` | `composeApp/.../security/LockScreen.kt` | Screen | N/A | PIN entry + biometric |
| `PinGateDialog` | `composeApp/.../security/PinGateDialog.kt` | Composable | No | Modal PIN challenge |

---

## 10. ViewModels

| ViewModel | File | Responsibility |
|---|---|---|
| `SettingsViewModel` | `commonMain/presentation/viewmodels/SettingsViewModel.kt` | Read/write `UserSettings` singleton |
| `SecurityViewModel` | `commonMain/presentation/viewmodels/SecurityViewModel.kt` | PIN/biometric state + actions |
| `BackupViewModel` | `commonMain/presentation/viewmodels/BackupViewModel.kt` | Export/Restore with PIN gate |
| `PinGateViewModel` | `commonMain/presentation/viewmodels/PinGateViewModel.kt` | Modal PIN challenge for critical actions |

---

## 11. Zustand Stores → DataStore / StateFlow

| Old (RN) | New (KMP) | File | Responsibility |
|---|---|---|---|
| `settingsStore` | `SettingsDataStore` | `commonMain/infrastructure/settings/SettingsDataStore.kt` | `currencyCode`, `pageSize`, `biometricEnabled`, `adEnabled` (hydrated from SQLite via `DataStore`) |
| `sessionStore` | `SessionDataStore` | `commonMain/infrastructure/settings/SessionDataStore.kt` | `unlocked: Boolean` |
| `invalidationStore` | `InvalidationDataStore` | `commonMain/infrastructure/settings/InvalidationDataStore.kt` | Manual invalidation versions (TanStack Query style) |

---

## 12. AdWare Governance

**Rule:** The master toggle `adEnabled` (in `UserSettings` → `settings.ad_enabled`) controls whether banner ads render in **allowed views only**.

- **Allowed:** Expenses list, Catalog/Inventory list, Caja (Cash Summary), Movimientos (Transactions) — footer banner.
- **Disallowed:** All forms, ConfirmDialogs, PIN gate, Lock screen, Settings screen.
- **Implementation:** `AdEnabledProvider` CompositionLocal reads `SettingsDataStore.adEnabled`. Components check this flag before rendering `AdBanner`.
- **Default:** `false` (opt-in).

---

## 13. Applicable NFRs

- **NFR-01:** Offline-first SQLDelight (JSI SQLite driver, transaction blocks).
- **NFR-06:** Dark mode strict + design tokens at runtime (`MaterialTheme` + custom tokens).
- **NFR-08:** UI strings in Spanish via `stringResource`; code/DB/identifiers in English.
- **NFR-09:** Strict Kotlin (`explicit-api`, `-Xlint`).
- **NFR-10:** Tests: 100% pass unit + integration + migration + PIN gate + cash integrity.

---

## 14. Extension Points (v2.0.0 Roadmap)

- [ ] **Auto-lock after inactivity**: Configurable timeout (1/5/15/30 min).
- [ ] **PIN recovery**: Security question fallback.
- [ ] **Scheduled backup**: Daily/weekly auto-export to cloud (iCloud/Drive).
- [ ] **Multi-currency support**: Per-transaction currency with FX rates.
- [ ] **Audit log**: Immutable log of settings changes.

---

## 15. i18n Keys (Spanish UI Dictionary — `composeResources/values-es/strings.xml`)

```xml
<!-- Settings Screen -->
<string name="settings_title">Configuración</string>
<string name="settings_section_general">General</string>
<string name="settings_section_security">Seguridad</string>
<string name="settings_section_backup">Copia de seguridad</string>
<string name="settings_section_ads">Publicidad</string>
<string name="settings_currency_label">Moneda</string>
<string name="settings_currency_option_bs">Bolivianos (Bs)</string>
<string name="settings_currency_option_usd">Dólares ($)</string>
<string name="settings_currency_option_eur">Euros (€)</string>
<string name="settings_pagination_label">Filas por consulta</string>
<string name="settings_pagination_placeholder">15</string>
<string name="settings_pagination_helper">Entre 5 y 100</string>
<string name="settings_security_pin_title">Cambiar PIN</string>
<string name="settings_security_pin_current">PIN actual</string>
<string name="settings_security_pin_new">Nuevo PIN</string>
<string name="settings_security_pin_confirm">Confirmar PIN</string>
<string name="settings_security_biometric_label">Autenticación biométrica</string>
<string name="settings_security_biometric_helper">Requiere autenticación para activar</string>
<string name="settings_backup_export_label">Exportar copia de seguridad</string>
<string name="settings_backup_export_success">Copia exportada correctamente</string>
<string name="settings_backup_export_error">Error al exportar</string>
<string name="settings_backup_restore_label">Restaurar copia de seguridad</string>
<string name="settings_backup_restore_confirm_title">¿Restaurar copia de seguridad?</string>
<string name="settings_backup_restore_confirm_message">Esto sobrescribirá todos los datos actuales. Ingrese su PIN para confirmar.</string>
<string name="settings_backup_restore_success">Copia restaurada. La app se reiniciará.</string>
<string name="settings_backup_restore_error">Error al restaurar</string>
<string name="settings_ads_enabled_label">Permitir anuncios</string>
<string name="settings_ads_enabled_helper">Muestra banners solo en la lista de gastos</string>

<!-- Lock Screen -->
<string name="lock_title">Ingrese su PIN</string>
<string name="lock_pin_placeholder">••••</string>
<string name="lock_biometric_hint">O use huella / Face ID</string>
<string name="lock_error_invalid">PIN incorrecto</string>

<!-- PIN Gate -->
<string name="pin_gate_title">Confirmar PIN</string>
<string name="pin_gate_message">Ingrese su PIN para continuar</string>
<string name="pin_gate_cancel">Cancelar</string>
<string name="pin_gate_confirm">Continuar</string>

<!-- PIN Form -->
<string name="pin_form_title_new">Establecer PIN</string>
<string name="pin_form_title_change">Cambiar PIN</string>
<string name="pin_form_label_current">PIN actual</string>
<string name="pin_form_label_new">Nuevo PIN</string>
<string name="pin_form_label_confirm">Confirmar nuevo PIN</string>
<string name="pin_form_error_mismatch">Los PINs no coinciden</string>
<string name="pin_form_error_length">El PIN debe tener 4-6 dígitos</string>
<string name="pin_form_success">PIN actualizado</string>

<!-- Biometric -->
<string name="biometric_prompt">Autentíquese para activar biométrica</string>
<string name="biometric_unavailable">Biometría no disponible en este dispositivo</string>

<!-- Toasts -->
<string name="toast_settings_saved">Configuración guardada</string>
<string name="toast_settings_error">Error al guardar</string>
```

---

## 16. Cross-References

- `expenses-cashflow/spec.md` — Section 12 (AdWare Governance), `AdEnabledProvider` CompositionLocal.
- `sales-checkout/spec.md` — Section 4 (Database Contract), uses `pageSize` from settings.
- `financial-accumulators/spec.md` — Section 4 (financial_accumulators), uses `Currency` for formatting.
- `catalog-inventory/spec.md` — Section 6 (RULE-CI-03), uses `Money` for fractional amounts.
- `system-settings/spec.md` — This document (source of truth for `Currency`, `Money`, `UserSettings`).

---

*Last updated: 2026-11-12 — v2.0.0 KMP Specification*