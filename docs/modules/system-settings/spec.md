# System Settings — Specification (v2.0.0 KMP)

## 1. Bounded Context

Responsible for global application configuration: currency (i18n), pagination defaults, security (PIN/biometric), backup/import. Persists a **singleton settings record** (`id = 1`) in SQLite with English column names, replacing the v1.0.0 `DataStore`/`SharedPreferences`-only approach. Ads are **not** part of settings: they are driven at compile time by the `AppConfig` singleton (`ADS_ENABLED: Boolean`, `false` during development) in the single build `com.copitoB.mypos`. All code/DB/identifiers in **English**; only visible UI strings remain in Spanish via i18n keys.

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
    val updatedAt: String = java.time.Instant.now().toString()   // ISO 8601
)

data class UpdateSettingsInput(
    val currencyCode: CurrencyCode? = null,
    val pageSize: Int? = null,
    val biometricEnabled: Boolean? = null
)
```

> **Note:** `adEnabled` was removed from `UserSettings` in Phase 6. Ads are driven by the compile-time `AppConfig.ADS_ENABLED` (`false` during development), not by user settings. The `settings.ad_enabled` column remains in the schema as deprecated/ignored until `v3.0.0`.

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
| RULE-SS-04 | Restore/import **requires** PIN | Restoring or importing a backup requires PIN confirmation (critical data overwrite). `ConfirmDialog`: `"Esto sobrescribirá todos los datos actuales. Ingrese su PIN para confirmar."` |
| RULE-SS-05 | Currency is visual only | `Currency`/`Money` affect formatting only. SQLite stores pure numeric amounts in base currency. |
| RULE-SS-06 | Pagination default | `pageSize` default 15. All historical list ViewModels apply `LIMIT ? OFFSET ?` with this value. |
| RULE-SS-07 | Ads are compile-time | `AppConfig.ADS_ENABLED` (`false` during development) governs ad rendering via `LocalAdsEnabled`. No user setting exists; `settings.ad_enabled` is deprecated and ignored. |
| RULE-SS-08 | Settings singleton | Only one row exists (`id = 1`, `CHECK (id = 1)`). Upsert on update. |
| RULE-SS-09 | Import is atomic | `ImportBackupUseCase` runs in a single SQLDelight transaction and recomputes `financial_accumulators`. |

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
-- ad_enabled is deprecated/ignored; ads are governed by AppConfig.ADS_ENABLED (compile-time), not this column.
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
-- ad_enabled is deprecated/ignored; ads are governed by AppConfig.ADS_ENABLED (compile-time), not this column.

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

### 6.5 ExportBackup / ImportBackup

**Files:** `commonMain/application/backup/ExportBackupUseCase.kt`, `ImportBackupUseCase.kt`, `LegacyMapper.kt`

Backups are **versioned JSON documents**, not raw SQLite copies. The legacy React Native app exported a raw `.db` file, so the importer accepts both formats through a sealed source type:

```kotlin
sealed interface BackupSource {
  data class Json(val bytes: ByteArray) : BackupSource
  data class LegacySqliteFile(val path: String) : BackupSource
}
```

**ExportBackupUseCase**

1. Read all tables through SQLDelight (`settings`, `categories`, `products`, `sales_transactions`, `transaction_details`, `expense_categories`, `financial_accumulators`).
2. Serialize into the versioned envelope (see §13) with `schemaVersion = 2` and `source = "kmp"`.
3. Write `tpv-backup-YYYYMMDD-HHmm.json` to `<documents>/backups/`.
4. Return the absolute path for the platform share sheet.

**ImportBackupUseCase**

1. **PIN gate** — `VerifyPin(pin)` must pass before any write (RULE-SS-04).
2. Decode by source: JSON envelope, or `.db` file read through a read-only SQLite connection and mapped to the same DTOs.
3. Validate `format == "tpv-backup"` and `schemaVersion <= 2`; reject newer versions with a user-facing Spanish error.
4. **Normalize legacy values** via `LegacyMapper`:
   - `type`: `VENTA → SALE`, `COMPRA → PURCHASE`, `GASTO → EXPENSE`
   - `payment_method`: `EFECTIVO → CASH`, `TARJETA → CARD`, `TRANSFERENCIA → TRANSFER`, `QR → QR`
   - Timestamps `YYYY-MM-DD HH:MM:SS` → ISO-8601 `YYYY-MM-DDTHH:MM:SSZ`
   - Insertion order: `categories → expense_categories → products → sales_transactions → transaction_details`
5. **Safe defaults for KMP-only fields:**
   - `products.sku` ← `null` (legacy schema always stores `NULL`; never synthesize)
   - `categories.updated_at` ← `registered_at` (column absent in legacy schema)
   - expenses `category_id` ← seeded default category when legacy detail is empty
   - `settings` ← legacy `tpv.settings.v1` AsyncStorage JSON when supplied, else `Bs / 15 / false`
   - `ad_enabled` ← always `0` (ignored; ads are compile-time)
6. **Single transaction:** wipe-and-insert (or id-preserving upsert) inside one SQLDelight `transaction { }`. Any failure rolls back the entire import.
7. **Recompute `financial_accumulators`** from `sales_transactions` (sum by type where `deleted_at IS NULL`), never trusting the incoming accumulator row.
8. Invalidate all ViewModels and return `ImportReport(inserted, updated, skipped, warnings)`.

**Invariants:** imported row counts must match source counts; the import is idempotent (running it twice yields the same state); the accumulator equals the recomputed sum after every import.

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
| `BackupActions` | `composeApp/.../settings/BackupActions.kt` | Composable | No | Export / Import buttons |
| `ConfirmDialog` | `composeApp/.../feedback/ConfirmDialog.kt` | Composable | No | Reusable, interpolated text |
| `FlavorInfoCard` | `composeApp/.../settings/FlavorInfoCard.kt` | Composable | No | Read-only build flavor + ads status (no toggle) |
| `LockScreen` | `composeApp/.../security/LockScreen.kt` | Screen | N/A | PIN entry + biometric |
| `PinLockOverlay` | `composeApp/.../security/PinLockOverlay.kt` | Composable | No | Full-screen PIN/biometric gate for app start and critical actions |
| `PinGateDialog` | `composeApp/.../security/PinGateDialog.kt` | Composable | No | Modal PIN challenge |

---

## 10. ViewModels

| ViewModel | File | Responsibility |
|---|---|---|
| `SettingsViewModel` | `commonMain/presentation/viewmodels/SettingsViewModel.kt` | Read/write `UserSettings` singleton |
| `PinViewModel` | `commonMain/presentation/viewmodels/PinViewModel.kt` | PIN verification, set/change, lock state, attempt throttling |
| `SecurityViewModel` | `commonMain/presentation/viewmodels/SecurityViewModel.kt` | Biometric availability + enable/disable actions |
| `BackupViewModel` | `commonMain/presentation/viewmodels/BackupViewModel.kt` | Export/Import (JSON + legacy `.db`) with PIN gate and progress state |
| `PinGateViewModel` | `commonMain/presentation/viewmodels/PinGateViewModel.kt` | Modal PIN challenge for critical actions |

---

## 11. Zustand Stores → DataStore / StateFlow

| Old (RN) | New (KMP) | File | Responsibility |
|---|---|---|---|
| `settingsStore` | `SettingsDataStore` | `commonMain/infrastructure/settings/SettingsDataStore.kt` | `currencyCode`, `pageSize`, `biometricEnabled` (hydrated from SQLite) |
| `sessionStore` | `SessionDataStore` | `commonMain/infrastructure/settings/SessionDataStore.kt` | `unlocked: Boolean` |
| `invalidationStore` | `InvalidationDataStore` | `commonMain/infrastructure/settings/InvalidationDataStore.kt` | Manual invalidation versions (TanStack Query style) |

---

## 12. AdWare Governance (Single Build — AppConfig)

**Rule:** Ads are a **compile-time** concern, not a user setting. The runtime `adEnabled` switch is removed from `SettingsScreen`.

- **Build:** Single build `com.copitoB.mypos`; no `free`/`pro` flavors, no `applicationIdSuffix`. `AppConfig` singleton in `commonMain` holds `ADS_ENABLED: Boolean` (`false` during development) plus theme configuration.
- **Propagation:** `AppConfig.ADS_ENABLED` → `expect fun isAdsEnabled(): Boolean` → `LocalAdsEnabled` CompositionLocal at the app root. The `composeApp` library does not reference platform `BuildConfig`.
- **Allowed:** Expenses list, Catalog/Inventory list, Caja, Movimientos — footer banner when `LocalAdsEnabled.current`.
- **Disallowed:** all forms, `ConfirmDialog`, PIN gate, Lock screen, Settings screen. No ad code path anywhere in these.
- **Settings UI:** replaced by a read-only `FlavorInfoCard` showing whether ads are compiled in.
- **Legacy column:** `settings.ad_enabled` is deprecated, never read or written, and dropped in a `v3.0.0` `.sqm`.

---

## 13. Backup Format Contract (JSON v2)

**Envelope**

```json
{
  "format": "tpv-backup",
  "schemaVersion": 2,
  "appVersion": "2.0.0",
  "exportedAt": "2026-11-19T10:00:00Z",
  "source": "kmp",
  "data": {
    "settings": { "currencyCode": "Bs", "pageSize": 15, "biometricEnabled": false },
    "categories": [],
    "expenseCategories": [],
    "products": [],
    "transactions": [],
    "transactionDetails": [],
    "accumulators": { "totalSales": 0, "totalPurchases": 0, "totalExpenses": 0, "netBalance": 0 }
  }
}
```

| Field | Rule |
|---|---|
| `format` | Must equal `"tpv-backup"`; anything else is rejected |
| `schemaVersion` | `2` for KMP exports; imports accept `1`/`2`; newer versions rejected |
| `source` | `"kmp"` or `"rn"` (informational; drives logging only) |
| `exportedAt` | ISO-8601 UTC |
| `data.accumulators` | Written for completeness but **never trusted** on import — recomputed from `transactions` |

**Legacy `.db` import mapping (`rn-app` → KMP)**

| Legacy | KMP | Transformation |
|---|---|---|
| `products.sku` (`NULL`) | `products.sku` | `null` |
| `categories` (no `updated_at`) | `categories.updated_at` | `= registered_at` |
| `type = VENTA / COMPRA / GASTO` | `SALE / PURCHASE / EXPENSE` | enum map |
| `payment_method = EFECTIVO / TARJETA / TRANSFERENCIA / QR` | `CASH / CARD / TRANSFER / QR` | enum map |
| `CURRENT_TIMESTAMP` (`YYYY-MM-DD HH:MM:SS`) | ISO-8601 (`T`…`Z`) | normalizer |
| `tpv.settings.v1` (AsyncStorage JSON) | `settings` row | supplied with the import or defaults `Bs / 15 / false` |
| expense without category | `expense_categories` default row | seeded default category |

**Atomicity:** one SQLDelight `transaction { }`. Failure → full rollback + `ImportReport` error. Success → ViewModel invalidation + `ImportReport(inserted, updated, skipped, warnings)`.

---

## 14. Applicable NFRs

- **NFR-01:** Offline-first SQLDelight (JSI SQLite driver, transaction blocks).
- **NFR-06:** Dark mode strict + design tokens at runtime (`MaterialKolor` driven by `AppConfig` seed color `Burnt Ember` + `PaletteStyle`).
- **NFR-08:** UI strings in Spanish via `stringResource`; code/DB/identifiers in English.
- **NFR-09:** Strict Kotlin (`explicit-api`, `-Xlint`).
- **NFR-10:** Tests: 100% pass unit + integration + migration + PIN gate + cash integrity.

---

## 15. Extension Points (v2.0.0 Roadmap)

- [ ] **Auto-lock after inactivity**: Configurable timeout (1/5/15/30 min).
- [ ] **PIN recovery**: Security question fallback.
- [ ] **Scheduled backup**: Daily/weekly auto-export to cloud (iCloud/Drive).
- [ ] **Multi-currency support**: Per-transaction currency with FX rates.
- [ ] **Audit log**: Immutable log of settings changes.

---

## 16. i18n Keys (Spanish UI Dictionary — `composeResources/values-es/strings.xml`)

```xml
<!-- Settings Screen -->
<string name="settings_title">Configuración</string>
<string name="settings_section_general">General</string>
<string name="settings_section_security">Seguridad</string>
<string name="settings_section_backup">Copia de seguridad</string>
<string name="settings_section_about">Aplicación</string>
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
<string name="settings_build_id">Compilación única: com.copitoB.mypos</string>
<string name="settings_ads_mode">Anuncios: AppConfig.ADS_ENABLED (tiempo de compilación)</string>
<string name="settings_ads_note">Sin interruptor en tiempo de ejecución.</string>

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

## 17. Cross-References

- `expenses-cashflow/spec.md` — Section 11 (AdWare Governance), `isAdsEnabled()` / `LocalAdsEnabled`.
- `sales-checkout/spec.md` — Section 4 (Database Contract), uses `pageSize` from settings.
- `financial-accumulators/spec.md` — Section 4 (financial_accumulators), uses `Currency` for formatting.
- `catalog-inventory/spec.md` — Section 6 (RULE-CI-03), uses `Money` for fractional amounts.
- `system-settings/spec.md` — This document (source of truth for `Currency`, `Money`, `UserSettings`, backup format).
- `/docs/MIGRATION_CHECKLIST.md` — Import parity and safe-default rules.

---

*Last updated: 2026-11-19 — v2.0.0 KMP Specification (Phase 6)*