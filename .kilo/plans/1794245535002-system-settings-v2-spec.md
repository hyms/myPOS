# System Settings — Specification (v2.0.0)

## 1. Bounded Context

Responsible for global application configuration: currency (i18n), pagination defaults, security (PIN/biometric), backup/restore, and the master `AD_ENABLED` flag for AdWare governance. Persists a **singleton settings record** (`id = 1`) in SQLite with English column names, replacing the v1.0.0 `AsyncStorage`-only approach. All code/DB/identifiers in **English**; only visible UI strings remain in Spanish via i18n keys.

---

## 2. Domain Entities & Value Objects

All identifiers, fields, and types in code and database are in **English**.

### 2.1 Currency (Value Object)

**File:** `src/domain/value-objects/Currency.ts`

```ts
export type CurrencyCode = 'Bs' | '$' | '€';

export interface Currency {
  readonly code: CurrencyCode;
  readonly symbol: string;
  readonly locale: string;           // e.g., 'es-BO', 'en-US', 'es-ES'
  readonly formatter: Intl.NumberFormat;
}

export const CURRENCY_REGISTRY: ReadonlyMap<CurrencyCode, Currency> = new Map([
  ['Bs', { code: 'Bs', symbol: 'Bs', locale: 'es-BO', formatter: new Intl.NumberFormat('es-BO', { style: 'currency', currency: 'BOB', minimumFractionDigits: 2 }) }],
  ['$',  { code: '$',  symbol: '$',  locale: 'en-US', formatter: new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 2 }) }],
  ['€',  { code: '€',  symbol: '€',  locale: 'es-ES', formatter: new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR', minimumFractionDigits: 2 }) }],
]);

export function getCurrency(code: CurrencyCode): Currency {
  const c = CURRENCY_REGISTRY.get(code);
  if (!c) throw new Error(`Unsupported currency: ${code}`);
  return c;
}
```

### 2.2 Money (Value Object)

**File:** `src/domain/value-objects/Money.ts`

```ts
import type { Currency } from './Currency';

export interface Money {
  readonly amount: number;     // base currency units, 2dp (e.g., 123.45)
  readonly currency: Currency;
}

export function createMoney(amount: number, currency: Currency): Money {
  if (!Number.isFinite(amount)) throw new Error('Amount must be finite');
  return { amount: round2(amount), currency };
}

export function round2(n: number): number {
  return Math.round(n * 100) / 100;
}

export function add(a: Money, b: Money): Money {
  if (a.currency.code !== b.currency.code) throw new Error('Currency mismatch');
  return createMoney(a.amount + b.amount, a.currency);
}

export function subtract(a: Money, b: Money): Money {
  if (a.currency.code !== b.currency.code) throw new Error('Currency mismatch');
  return createMoney(a.amount - b.amount, a.currency);
}

export function format(money: Money): string {
  return money.currency.formatter.format(money.amount);
}
```

### 2.3 UserSettings (Aggregate Root — Singleton)

**File:** `src/domain/entities/UserSettings.ts`

```ts
import type { CurrencyCode } from '../value-objects/Currency';

export type SettingsId = number; // always 1

export interface UserSettings {
  readonly id: SettingsId;                    // always 1
  readonly currencyCode: CurrencyCode;        // 'Bs' | '$' | '€'
  readonly pageSize: number;                  // default 15, min 5, max 100
  readonly biometricEnabled: boolean;         // false by default
  readonly adEnabled: boolean;                // master AdWare toggle, default false
  readonly updatedAt: string;                 // ISO 8601
}

export type UpdateSettingsInput = Partial<{
  readonly currencyCode: CurrencyCode;
  readonly pageSize: number;
  readonly biometricEnabled: boolean;
  readonly adEnabled: boolean;
}>;

export const DEFAULT_SETTINGS: Omit<UserSettings, 'id' | 'updatedAt'> = {
  currencyCode: 'Bs',
  pageSize: 15,
  biometricEnabled: false,
  adEnabled: false,
};
```

### 2.4 Session (UI State)

**File:** `src/domain/entities/Session.ts`

```ts
export interface SessionState {
  readonly unlocked: boolean;
}
```

---

## 3. Domain Rules & Invariants

| ID | Rule | Description |
|---|---|---|
| RULE-SS-01 | PIN change requires current PIN | `PinService.setPin(newPin)` demands current PIN hash match, except first-time setup. |
| RULE-SS-02 | Biometric toggle requires auth | Enabling biometric invokes `LocalAuthentication.authenticateAsync` first. |
| RULE-SS-03 | PIN gate for critical actions | Change PIN, delete transaction, restore backup → `usePinGate()` enforces current PIN. |
| RULE-SS-04 | Restore backup **requires** PIN | v2.0.0 change: restoring backup now requires PIN confirmation (critical data overwrite). `ConfirmDialog`: `"Esto sobrescribirá todos los datos actuales. Ingrese su PIN para confirmar."` |
| RULE-SS-05 | Currency is visual only | `Currency`/`Money` affect formatting only. SQLite stores pure numeric amounts in base currency. |
| RULE-SS-06 | Pagination default | `pageSize` default 15. All historical list hooks apply `LIMIT ? OFFSET ?` with this value. |
| RULE-SS-07 | AD_ENABLED master toggle | `adEnabled` in `UserSettings` governs whether banner ads render in allowed views (Expenses list). |
| RULE-SS-08 | Settings singleton | Only one row exists (`id = 1`, `CHECK (id = 1)`). Upsert on update. |

Global invariants: **INV-08** (PIN-protected critical actions), **INV-10** (strict TypeScript).

---

## 4. Database Contract

### 4.1 Target Schema (v2.0.0 — English Columns)

```sql
-- settings (singleton, id = 1)
CREATE TABLE IF NOT EXISTS settings (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    currency_code TEXT NOT NULL DEFAULT 'Bs',   -- 'Bs' | '$' | '€'
    page_size INTEGER NOT NULL DEFAULT 15,
    biometric_enabled INTEGER NOT NULL DEFAULT 0,  -- 0/1
    ad_enabled INTEGER NOT NULL DEFAULT 0,         -- 0/1
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT OR IGNORE INTO settings (id, currency_code, page_size, biometric_enabled, ad_enabled, updated_at)
VALUES (1, 'Bs', 15, 0, 0, CURRENT_TIMESTAMP);
```

### 4.2 Column Mapping: v1.0.0 (Spanish/AsyncStorage) → v2.0.0 (English/SQLite)

v1.0.0 stored settings in `AsyncStorage` with Spanish keys. v2.0.0 migrates to SQLite `settings` table.

| v1.0.0 AsyncStorage Key | v2.0.0 Table | v2.0.0 Column | Type | Default |
|---|---|---|---|---|
| `divisa` | `settings` | `currency_code` | TEXT | `'Bs'` |
| `pageSize` | `settings` | `page_size` | INTEGER | `15` |
| `biometricEnabled` | `settings` | `biometric_enabled` | INTEGER (0/1) | `0` |
| *(new)* | `settings` | `ad_enabled` | INTEGER (0/1) | `0` |
| *(new)* | `settings` | `updated_at` | TIMESTAMP | `CURRENT_TIMESTAMP` |

> **Note:** PIN hash remains in `expo-secure-store` (key: `pin_hash`), not migrated to SQLite.

### 4.3 Migration DDL (v1.0.0 → v2.0.0)

**File:** `src/data/database/migrations/v2_0_0_settings_migration.sql`

```sql
-- Atomic migration transaction (single db.withTransactionSync)

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
INSERT OR IGNORE INTO settings (id, currency_code, page_size, biometric_enabled, ad_enabled, updated_at)
VALUES (1, 'Bs', 15, 0, 0, CURRENT_TIMESTAMP);

-- Step 3: Migrate AsyncStorage values to SQLite (application-layer, not SQL)
-- Handled by GetSettings use case on first run: read AsyncStorage keys,
-- write to settings table, then clear AsyncStorage keys.

-- Step 4: Create index (trivial for singleton, but kept for consistency)
CREATE INDEX IF NOT EXISTS idx_settings_id ON settings(id);

-- Step 5: Update user_version to 2 (coordinated with other modules)
PRAGMA user_version = 2;
```

---

## 5. Repository Interfaces

### 5.1 ISettingsRepository

**File:** `src/data/repositories/ISettingsRepository.ts`

```ts
import type { UserSettings, SettingsId, UpdateSettingsInput } from '@/domain/entities/UserSettings';

export interface ISettingsRepository {
  get(): UserSettings;                           // singleton read (id = 1)
  update(input: UpdateSettingsInput): UserSettings;  // upsert singleton
}
```

---

## 6. Use Cases (Application Layer)

### 6.1 GetSettings

**File:** `src/application/settings/GetSettings.ts`

```ts
import type { UserSettings } from '@/domain/entities/UserSettings';

export function getSettings(): UserSettings;
```

**Flow:**
1. `settingsRepository.get()` → returns `UserSettings`.
2. If table empty (first run after migration), returns `DEFAULT_SETTINGS` and triggers migration from AsyncStorage.

### 6.2 UpdateSettings

**File:** `src/application/settings/UpdateSettings.ts`

```ts
import type { UserSettings, UpdateSettingsInput } from '@/domain/entities/UserSettings';

export function updateSettings(input: UpdateSettingsInput): UserSettings;
```

**Atomic Flow (`db.withTransactionSync`):**
1. Validate `pageSize` ∈ [5, 100] if provided.
2. `UPDATE settings SET currency_code = ?, page_size = ?, biometric_enabled = ?, ad_enabled = ?, updated_at = CURRENT_TIMESTAMP WHERE id = 1`.
3. Return updated `UserSettings`.

### 6.3 VerifyPin (PIN Gate)

**File:** `src/application/security/VerifyPin.ts`

```ts
export function verifyPin(inputPin: string): boolean;
```

**Flow:**
1. Read PIN hash from `expo-secure-store` (`pin_hash`).
2. Compute SHA-256 of `inputPin`.
3. Compare constant-time. Return `true`/`false`.

### 6.4 SetPin

**File:** `src/application/security/SetPin.ts`

```ts
export function setPin(currentPin: string | null, newPin: string): void;
```

**Flow:**
1. If `currentPin !== null`, call `verifyPin(currentPin)` → throw if false.
2. Compute SHA-256 of `newPin`.
3. Store hash in `expo-secure-store` (`pin_hash`).

### 6.5 ExportDatabase / RestoreDatabase

**Files:** `src/application/backup/ExportDatabase.ts`, `src/application/backup/RestoreDatabase.ts`

- `ExportDatabase`: Copy SQLite file to `documentDirectory/backups/tpv-YYYYMMDD-HHmm.db` → `Sharing.shareAsync`.
- `RestoreDatabase`: `DocumentPicker.getDocumentAsync` → close DB → copy over main DB → reopen DB → invalidate all stores → reload app (via `expo-updates` or in-memory store reset). **Requires PIN gate (RULE-SS-04).**

---

## 7. Domain Rules (Settings Math)

**File:** `src/domain/rules/settingsRules.ts`

| Function | Signature | Description |
|---|---|---|
| `validatePageSize` | `(pageSize: number) => void` | Throws if `pageSize < 5 || pageSize > 100 || !Number.isInteger(pageSize)`. |
| `validateCurrencyCode` | `(code: string) => asserts code is CurrencyCode` | Throws if not `'Bs' \| '$' \| '€'`. |

---

## 8. UI State Machines (Spanish visible text)

### 8.1 Lock Screen (`app/lock.tsx`)

| State | Trigger | Next State | Visible Text (ES) |
|---|---|---|---|
| `Locked` | App start, PIN configured | `Authenticating` | `"Ingrese su PIN"` |
| `Authenticating` | Biometric success | `Unlocked` | (silent) |
| `Authenticating` | PIN correct | `Unlocked` | (silent) |
| `Authenticating` | PIN incorrect | `Error` | `"PIN incorrecto"` |
| `Authenticating` | Biometric fail / cancel | `Locked` | `"Ingrese su PIN"` |

### 8.2 Settings Screen (`app/settings/index.tsx`)

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

## 9. UI Components

| Component | File | Type | Memoized | Description |
|---|---|---|---|---|
| `SectionHeader` | `src/presentation/components/settings/SectionHeader.tsx` | UI | No | Section title with divider |
| `CurrencySelector` | `src/presentation/components/settings/CurrencySelector.tsx` | UI | No | Radio group: Bs / $ / € |
| `PageSizeInput` | `src/presentation/components/settings/PageSizeInput.tsx` | UI | No | Numeric input (5–100) |
| `PinForm` | `src/presentation/components/security/PinForm.tsx` | UI | No | 4–6 digit PIN input, masked |
| `BiometricToggle` | `src/presentation/components/security/BiometricToggle.tsx` | UI | No | Switch + auth on enable |
| `BackupActions` | `src/presentation/components/settings/BackupActions.tsx` | UI | No | Export / Restore buttons |
| `ConfirmDialog` | `src/presentation/components/feedback/ConfirmDialog.tsx` | UI | No | Reusable, interpolated text |
| `AdEnabledToggle` | `src/presentation/components/settings/AdEnabledToggle.tsx` | UI | No | Master AdWare switch |

---

## 10. Hooks

| Hook | File | Responsibility |
|---|---|---|
| `useSettings` | `src/presentation/hooks/useSettings.ts` | Read/write `UserSettings` singleton |
| `useSettingsCurrency` | `src/presentation/hooks/useSettingsCurrency.ts` | Current `Currency` object |
| `useSettingsPageSize` | `src/presentation/hooks/useSettingsPageSize.ts` | Current `pageSize` for pagination |
| `useSettingsSecurity` | `src/presentation/hooks/useSettingsSecurity.ts` | PIN/biometric state + actions |
| `useSettingsBackup` | `src/presentation/hooks/useSettingsBackup.ts` | Export/Restore with PIN gate |
| `usePinGate` | `src/presentation/hooks/usePinGate.ts` | Modal PIN challenge for critical actions |
| `useCurrency` | `src/presentation/hooks/useCurrency.ts` | `Money` formatter via `Currency` |
| `useReducedMotion` | `src/presentation/hooks/useReducedMotion.ts` | `prefers-reduced-motion` media query |
| `useAdEnabled` | `src/presentation/hooks/useAdEnabled.ts` | Reads `adEnabled` from settings |

---

## 11. Zustand Stores

| Store | File | Responsibility |
|---|---|---|
| `settingsStore` | `src/presentation/stores/settingsStore.ts` | `currencyCode`, `pageSize`, `biometricEnabled`, `adEnabled` (hydrated from SQLite) |
| `sessionStore` | `src/presentation/stores/sessionStore.ts` | `unlocked: boolean` |
| `invalidationStore` | `src/presentation/stores/invalidationStore.ts` | Manual invalidation (TanStack Query style) |

---

## 12. AdWare Governance

**Rule:** The master toggle `adEnabled` (in `UserSettings` → `settings.ad_enabled`) controls whether banner ads render in **allowed views only**.

- **Allowed:** Expenses list (`app/gastos/index.tsx`) — footer banner.
- **Disallowed:** All forms, ConfirmDialogs, PIN gate, Lock screen, Settings screen.
- **Implementation:** `useAdEnabled()` hook reads `settingsStore.adEnabled`. Components check this flag before rendering `AdBanner`.
- **Default:** `false` (opt-in).

---

## 13. Applicable NFRs

- **NFR-01:** Offline-first SQLite JSI (`expo-sqlite`, `openDatabaseSync`, `withTransactionSync`).
- **NFR-06:** Dark mode strict + design tokens at runtime (NativeWind v4).
- **NFR-08:** UI strings in Spanish; code/DB/identifiers in English.
- **NFR-09:** Strict TypeScript (`strict`, `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes`).
- **NFR-10:** Tests: 100% pass unit + integration + migration + PIN gate + cash integrity.

---

## 14. Extension Points (v2.0.0 Roadmap)

- [ ] **Auto-lock after inactivity**: Configurable timeout (1/5/15/30 min).
- [ ] **PIN recovery**: Security question fallback.
- [ ] **Scheduled backup**: Daily/weekly auto-export to cloud (iCloud/Drive).
- [ ] **Multi-currency support**: Per-transaction currency with FX rates.
- [ ] **Audit log**: Immutable log of settings changes.

---

## 15. i18n Keys (Spanish UI Dictionary)

| Key | Text (ES) | Context |
|---|---|---|
| `settings.title` | Configuración | Screen title |
| `settings.section.general` | General | Section header |
| `settings.section.security` | Seguridad | Section header |
| `settings.section.backup` | Copia de seguridad | Section header |
| `settings.section.ads` | Publicidad | Section header |
| `settings.currency.label` | Moneda | CurrencySelector label |
| `settings.currency.option.bs` | Bolivianos (Bs) | Radio option |
| `settings.currency.option.usd` | Dólares ($) | Radio option |
| `settings.currency.option.eur` | Euros (€) | Radio option |
| `settings.pagination.label` | Filas por consulta | PageSizeInput label |
| `settings.pagination.placeholder` | 15 | Input placeholder |
| `settings.pagination.helper` | Entre 5 y 100 | Helper text |
| `settings.security.pin.title` | Cambiar PIN | Section title |
| `settings.security.pin.current` | PIN actual | Input label |
| `settings.security.pin.new` | Nuevo PIN | Input label |
| `settings.security.pin.confirm` | Confirmar PIN | Input label |
| `settings.security.biometric.label` | Autenticación biométrica | Toggle label |
| `settings.security.biometric.helper` | Requiere autenticación para activar | Helper text |
| `settings.backup.export.label` | Exportar copia de seguridad | Button |
| `settings.backup.export.success` | Copia exportada correctamente | Toast |
| `settings.backup.export.error` | Error al exportar | Toast |
| `settings.backup.restore.label` | Restaurar copia de seguridad | Button |
| `settings.backup.restore.confirm_title` | ¿Restaurar copia de seguridad? | ConfirmDialog title |
| `settings.backup.restore.confirm_message` | Esto sobrescribirá todos los datos actuales. Ingrese su PIN para confirmar. | ConfirmDialog message |
| `settings.backup.restore.success` | Copia restaurada. La app se reiniciará. | Toast |
| `settings.backup.restore.error` | Error al restaurar | Toast |
| `settings.ads.enabled.label` | Permitir anuncios | Toggle label |
| `settings.ads.enabled.helper` | Muestra banners solo en la lista de gastos | Helper text |
| `lock.title` | Ingrese su PIN | Lock screen title |
| `lock.pin.placeholder` | •••• | PIN input placeholder |
| `lock.biometric.hint` | O use huella / Face ID | Biometric hint |
| `lock.error.invalid` | PIN incorrecto | Error toast |
| `pin_gate.title` | Confirmar PIN | PIN gate modal title |
| `pin_gate.message` | Ingrese su PIN para continuar | PIN gate message |
| `pin_gate.cancel` | Cancelar | Button |
| `pin_gate.confirm` | Continuar | Button |
| `pin_form.title.new` | Establecer PIN | First-time setup |
| `pin_form.title.change` | Cambiar PIN | Change PIN |
| `pin_form.label.current` | PIN actual | Current PIN label |
| `pin_form.label.new` | Nuevo PIN | New PIN label |
| `pin_form.label.confirm` | Confirmar nuevo PIN | Confirm label |
| `pin_form.error.mismatch` | Los PINs no coinciden | Validation error |
| `pin_form.error.length` | El PIN debe tener 4-6 dígitos | Validation error |
| `pin_form.success` | PIN actualizado | Toast |
| `biometric.prompt` | Autentíquese para activar biométrica | Biometric auth prompt |
| `biometric.unavailable` | Biometría no disponible en este dispositivo | Toast |
| `toast.settings_saved` | Configuración guardada | Toast |
| `toast.settings_error` | Error al guardar | Toast |

---

## 16. Cross-References

- `expenses-cashflow/spec.md` — Section 12 (AdWare Governance), `useAdEnabled` hook.
- `sales-checkout/spec.md` — Section 4 (Database Contract), uses `pageSize` from settings.
- `financial-accumulators/spec.md` — Section 4 (financial_accumulators), uses `Currency` for formatting.
- `catalog-inventory/spec.md` — Section 6 (RULE-CI-03), uses `Money` for fractional amounts.
- `system-settings/spec.md` — This document (source of truth for `Currency`, `Money`, `UserSettings`).

---

*Last updated: 2026-11-10 — v2.0.0 Specification*