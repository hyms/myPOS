# System Settings — Test Specification (v2.0.0)

## Test Organization

| Layer | Directory | Framework | Target Coverage |
|---|---|---|---|
| Unit (Domain) | `tests/unit/system-settings/` | Jest | 100% functions/branches |
| Unit (Hooks) | `tests/unit/hooks/` | Jest + RTL | 100% hooks |
| Integration (Data) | `tests/integration/system-settings/` | Jest + SQLite | 100% repo/use cases |
| Integration (Security) | `tests/integration/security/` | Jest | 100% PIN/biometric flows |
| Migration | `tests/migration/system-settings/` | Jest + SQLite | 100% v1→v2 scenarios |
| Component | `tests/components/settings/` | Jest + RTL | 100% components |
| E2E | `tests/e2e/system-settings/` | Detox / Maestro | Critical user flows |

---

## 1. Unit Tests — Domain Value Objects

### 1.1 `Currency.test.ts`

**File:** `tests/unit/system-settings/Currency.test.ts`

| Test Case | Description | Expected |
|---|---|---|
| `getCurrency('Bs')` | Returns Bolivianos currency | `code: 'Bs'`, `symbol: 'Bs'`, `locale: 'es-BO'`, formatter for BOB |
| `getCurrency('$')` | Returns USD currency | `code: '$'`, `symbol: '$'`, `locale: 'en-US'`, formatter for USD |
| `getCurrency('€')` | Returns EUR currency | `code: '€'`, `symbol: '€'`, `locale: 'es-ES'`, formatter for EUR |
| `getCurrency('XXX')` | Throws on unsupported code | `Error: Unsupported currency: XXX` |
| `formatter.format(1234.5)` | BOB formatting | `"Bs 1.234,50"` (es-BO locale) |
| `formatter.format(1234.5)` | USD formatting | `"$1,234.50"` (en-US locale) |
| `formatter.format(1234.5)` | EUR formatting | `"1.234,50 €"` (es-ES locale) |
| `CURRENCY_REGISTRY.size` | Registry has exactly 3 entries | `3` |

### 1.2 `Money.test.ts`

**File:** `tests/unit/system-settings/Money.test.ts`

| Test Case | Description | Expected |
|---|---|---|
| `createMoney(100, currency)` | Creates Money with 2dp rounding | `{ amount: 100, currency }` |
| `createMoney(100.123, currency)` | Rounds to 2dp | `{ amount: 100.12, currency }` |
| `createMoney(100.126, currency)` | Rounds up | `{ amount: 100.13, currency }` |
| `createMoney(NaN, currency)` | Throws on non-finite | `Error: Amount must be finite` |
| `createMoney(Infinity, currency)` | Throws on Infinity | `Error: Amount must be finite` |
| `round2(1.005)` | Banker's rounding edge case | `1.01` |
| `round2(1.004)` | Standard rounding down | `1.00` |
| `add(m1, m2)` | Same currency addition | `Money { amount: m1.amount + m2.amount }` |
| `add(m1, m3)` | Different currency throws | `Error: Currency mismatch` |
| `subtract(m1, m2)` | Same currency subtraction | `Money { amount: m1.amount - m2.amount }` |
| `subtract(m1, m3)` | Different currency throws | `Error: Currency mismatch` |
| `format(money)` | Delegates to currency formatter | `currency.formatter.format(money.amount)` |

### 1.3 `UserSettings.test.ts`

**File:** `tests/unit/system-settings/UserSettings.test.ts`

| Test Case | Description | Expected |
|---|---|---|
| `DEFAULT_SETTINGS` | Matches spec defaults | `{ currencyCode: 'Bs', pageSize: 15, biometricEnabled: false, adEnabled: false }` |
| `UpdateSettingsInput` partial | All fields optional | Type accepts `{ currencyCode: '$' }` etc. |
| `UserSettings` readonly | All fields readonly | TypeScript compile-time check |

### 1.4 `settingsRules.test.ts`

**File:** `tests/unit/system-settings/settingsRules.test.ts`

| Test Case | Description | Expected |
|---|---|---|
| `validatePageSize(5)` | Minimum valid | No throw |
| `validatePageSize(100)` | Maximum valid | No throw |
| `validatePageSize(15)` | Default valid | No throw |
| `validatePageSize(4)` | Below minimum throws | `Error: pageSize must be between 5 and 100` |
| `validatePageSize(101)` | Above maximum throws | `Error: pageSize must be between 5 and 100` |
| `validatePageSize(15.5)` | Non-integer throws | `Error: pageSize must be an integer` |
| `validatePageSize(-10)` | Negative throws | `Error: pageSize must be between 5 and 100` |
| `validateCurrencyCode('Bs')` | Valid code | No throw, assertion passes |
| `validateCurrencyCode('$')` | Valid code | No throw, assertion passes |
| `validateCurrencyCode('€')` | Valid code | No throw, assertion passes |
| `validateCurrencyCode('GBP')` | Invalid code throws | `Error: Unsupported currency code: GBP` |
| `validateCurrencyCode('')` | Empty string throws | `Error: Unsupported currency code: ` |

---

## 2. Unit Tests — Hooks

### 2.1 `useSettings.test.ts`

**File:** `tests/unit/hooks/useSettings.test.ts`

| Test Case | Description | Expected |
|---|---|---|
| `useSettings()` initial | Returns DEFAULT_SETTINGS before hydration | `currencyCode: 'Bs', pageSize: 15, ...` |
| `useSettings()` after hydration | Returns SQLite values | Matches `settingsStore` state |
| `updateSettings({ currencyCode: '$' })` | Updates store + persists | Store updated, repository `update` called |
| `updateSettings({ pageSize: 20 })` | Validates pageSize | Store updated, repository `update` called |
| `updateSettings({ pageSize: 4 })` | Rejects invalid pageSize | Throws, store unchanged, repository not called |

### 2.2 `useSettingsCurrency.test.ts`

**File:** `tests/unit/hooks/useSettingsCurrency.test.ts`

| Test Case | Description | Expected |
|---|---|---|
| `useSettingsCurrency()` with Bs | Returns BOB currency | `currency.code === 'Bs'` |
| `useSettingsCurrency()` with $ | Returns USD currency | `currency.code === '$'` |
| `useSettingsCurrency()` with € | Returns EUR currency | `currency.code === '€'` |
| Reactive update | Changing store updates hook | Hook returns new currency after `updateSettings` |

### 2.3 `useSettingsPageSize.test.ts`

**File:** `tests/unit/hooks/useSettingsPageSize.test.ts`

| Test Case | Description | Expected |
|---|---|---|
| `useSettingsPageSize()` default | Returns 15 | `15` |
| `useSettingsPageSize()` after update | Returns new pageSize | Matches store |
| Used in list hook | `LIMIT ?` uses returned value | Integration verified in list hook tests |

### 2.4 `useAdEnabled.test.ts`

**File:** `tests/unit/hooks/useAdEnabled.test.ts`

| Test Case | Description | Expected |
|---|---|---|
| `useAdEnabled()` default | Returns false | `false` |
| `useAdEnabled()` after enable | Returns true | `true` |
| Used in Expenses list | Conditional render AdBanner | Component test verifies |

---

## 3. Integration Tests — Repository & Use Cases

### 3.1 `GetSettings.test.ts`

**File:** `tests/integration/system-settings/GetSettings.test.ts`

| Test Case | Description | Expected |
|---|---|---|
| Fresh DB (empty settings table) | Returns DEFAULT_SETTINGS | `DEFAULT_SETTINGS` + triggers AsyncStorage migration |
| DB with existing row | Returns stored values | Matches SQLite row |
| Migration from AsyncStorage | Reads `divisa`, `pageSize`, `biometricEnabled` | Upserts to SQLite, clears AsyncStorage keys |
| AsyncStorage empty | Uses defaults | No migration, defaults used |
| Concurrent calls | Returns same instance | Single query, cached |

### 3.2 `UpdateSettings.test.ts`

**File:** `tests/integration/system-settings/UpdateSettings.test.ts`

| Test Case | Description | Expected |
|---|---|---|
| Update currencyCode | Atomic update, returns new entity | `currency_code = '$'` in DB, `updated_at` updated |
| Update pageSize | Validates 5-100 | `page_size = 20` in DB |
| Update biometricEnabled | 0/1 stored | `biometric_enabled = 1` in DB |
| Update adEnabled | 0/1 stored | `ad_enabled = 1` in DB |
| Update multiple fields | Single transaction | All fields updated, single `updated_at` |
| Invalid pageSize (4) | Throws, no DB change | Error thrown, row unchanged |
| Invalid pageSize (101) | Throws, no DB change | Error thrown, row unchanged |
| Invalid currencyCode | Throws, no DB change | Error thrown, row unchanged |

### 3.3 `VerifyPin.test.ts`

**File:** `tests/integration/security/VerifyPin.test.ts`

| Test Case | Description | Expected |
|---|---|---|
| Correct PIN | Returns true | `true` |
| Incorrect PIN | Returns false | `false` |
| Empty PIN | Returns false | `false` |
| No PIN set (first run) | Returns false | `false` |
| Constant-time | Timing attack resistant | Implementation uses constant-time comparison |

### 3.4 `SetPin.test.ts`

**File:** `tests/integration/security/SetPin.test.ts`

| Test Case | Description | Expected |
|---|---|---|
| First-time set (currentPin=null) | Stores hash | `pin_hash` in SecureStore |
| Change PIN with correct current | Updates hash | New hash in SecureStore |
| Change PIN with wrong current | Throws | Error, hash unchanged |
| New PIN too short (<4) | Throws | Error, hash unchanged |
| New PIN too long (>6) | Throws | Error, hash unchanged |

### 3.5 `ExportDatabase.test.ts`

**File:** `tests/integration/system-settings/ExportDatabase.test.ts`

| Test Case | Description | Expected |
|---|---|---|
| Export creates file | `.db` file in backups dir | File exists, valid SQLite |
| Export filename format | `tpv-YYYYMMDD-HHmm.db` | Matches regex |
| Share called | `Sharing.shareAsync` invoked | Mock called with file URI |

### 3.6 `RestoreDatabase.test.ts`

**File:** `tests/integration/system-settings/RestoreDatabase.test.ts`

| Test Case | Description | Expected |
|---|---|---|
| Restore requires PIN | Calls `usePinGate` | PIN gate triggered before restore |
| Valid backup file | Replaces main DB | DB file copied, app reloads |
| Invalid file | Shows error toast | Error toast, DB unchanged |
| PIN gate cancelled | Aborts restore | No file copy, no reload |
| Stores invalidated | `invalidationStore` triggered | All keys invalidated |

---

## 4. Migration Tests

### 4.1 `v2_0_0_migration.test.ts`

**File:** `tests/migration/system-settings/v2_0_0_migration.test.ts`

| Test Case | Description | Expected |
|---|---|---|
| Migration creates settings table | Table exists with English columns | Schema matches spec |
| Singleton row inserted | `id = 1` row exists | `currency_code='Bs', page_size=15, biometric_enabled=0, ad_enabled=0` |
| CHECK constraint enforced | Cannot insert `id = 2` | SQLite constraint error |
| Index created | `idx_settings_id` exists | `.indexes` includes it |
| user_version updated | `PRAGMA user_version = 2` | Returns 2 |
| Idempotent run | Second run succeeds | No errors, single row |

### 4.2 `asyncStorageMigration.test.ts`

**File:** `tests/migration/system-settings/asyncStorageMigration.test.ts`

| Test Case | Description | Expected |
|---|---|---|
| AsyncStorage has divisa='€' | Migrates to currency_code='€' | SQLite row updated |
| AsyncStorage has pageSize=25 | Migrates to page_size=25 | SQLite row updated |
| AsyncStorage has biometricEnabled=true | Migrates to biometric_enabled=1 | SQLite row updated |
| AsyncStorage keys cleared | After migration, keys removed | `AsyncStorage.getItem` returns null |
| No AsyncStorage keys | Uses defaults, no error | Defaults used |
| Partial AsyncStorage | Migrates present keys only | Present keys migrated, others default |

---

## 5. PIN Gate & Security Tests

### 5.1 `pinGate.test.ts`

**File:** `tests/integration/security/pinGate.test.ts`

| Test Case | Description | Expected |
|---|---|---|
| PIN gate blocks delete transaction | `usePinGate` returns false without PIN | Action not executed |
| PIN gate allows with correct PIN | `usePinGate` returns true with PIN | Action executed |
| PIN gate rejects wrong PIN | `usePinGate` returns false | Action not executed |
| PIN gate cancelled | User dismisses modal | Action not executed |
| Critical actions gated | Change PIN, delete transaction, restore backup | All use `usePinGate` |

### 5.2 `pinForm.test.tsx`

**File:** `tests/components/security/pinForm.test.tsx`

| Test Case | Description | Expected |
|---|---|---|
| Renders 4-6 digit inputs | 6 input fields | 6 `TextInput` with `maxLength=1` |
| Masked input | `secureTextEntry={true}` | Dots shown |
| Validation: <4 digits | Shows error | `"El PIN debe tener 4-6 dígitos"` |
| Validation: mismatch | Shows error | `"Los PINs no coinciden"` |
| Success callback | On valid submit | `onSuccess` called with PIN |
| Cancel callback | On cancel press | `onCancel` called |

---

## 6. UI Component Tests

### 6.1 `CurrencySelector.test.tsx`

**File:** `tests/components/settings/CurrencySelector.test.tsx`

| Test Case | Description | Expected |
|---|---|---|
| Renders 3 radio options | Bs, $, € | 3 `Pressable`/`Radio` components |
| Current selection highlighted | Visual indicator | Selected option styled differently |
| Tap option updates store | `updateSettings({ currencyCode })` | Store updated, formatter changes |
| Accessibility labels | Screen reader support | `accessibilityLabel` on each option |

### 6.2 `PageSizeInput.test.tsx`

**File:** `tests/components/settings/PageSizeInput.test.tsx`

| Test Case | Description | Expected |
|---|---|---|
| Renders numeric input | `keyboardType="numeric"` | Numeric keyboard |
| Shows current value | Default 15 | `value="15"` |
| Accepts 5-100 | Valid input | Store updated |
| Rejects <5 | Shows helper error | `"Entre 5 y 100"` |
| Rejects >100 | Shows helper error | `"Entre 5 y 100"` |
| Rejects non-integer | Shows helper error | `"Entre 5 y 100"` |

### 6.3 `BiometricToggle.test.tsx`

**File:** `tests/components/settings/BiometricToggle.test.tsx`

| Test Case | Description | Expected |
|---|---|---|
| Renders switch | `Switch` component | Switch visible |
| Disabled when unavailable | `BiometricService.isAvailable()` false | Switch disabled, helper text |
| Toggle on → triggers auth | `LocalAuthentication.authenticateAsync` | Auth called |
| Auth success → store updated | `biometricEnabled = true` | Store + SQLite updated |
| Auth fail/cancel → switch off | Store unchanged | Switch off, no persistence |

### 6.4 `AdEnabledToggle.test.tsx`

**File:** `tests/components/settings/AdEnabledToggle.test.tsx`

| Test Case | Description | Expected |
|---|---|---|
| Renders switch | `Switch` component | Switch visible |
| Default off | `adEnabled = false` | Switch off |
| Toggle on → store updated | `adEnabled = true` | Store + SQLite updated |
| Helper text visible | `"Muestra banners solo en la lista de gastos"` | Text present |

### 6.5 `BackupActions.test.tsx`

**File:** `tests/components/settings/BackupActions.test.tsx`

| Test Case | Description | Expected |
|---|---|---|
| Export button | Triggers `useSettingsBackup.export` | Export function called |
| Restore button | Triggers PIN gate → `useSettingsBackup.restore` | PIN gate → restore |
| Export success toast | `"Copia exportada correctamente"` | Toast shown |
| Restore confirm dialog | Interpolated message with PIN warning | Dialog shows correct text |

---

## 7. E2E Tests

### 7.1 `settingsFlow.test.ts`

**File:** `tests/e2e/system-settings/settingsFlow.test.ts`

| Scenario | Steps | Verification |
|---|---|---|
| Change currency globally | 1. Open Settings → 2. Select $ → 3. Navigate to Ventas → 4. Check prices | Prices show `$` symbol, USD formatting |
| Change pageSize | 1. Open Settings → 2. Set pageSize=25 → 3. Open Productos → 4. Scroll | 25 items loaded per page |
| Toggle biometric | 1. Open Settings → 2. Enable biometric → 3. Auth → 4. Lock app → 5. Unlock with biometric | Biometric unlock works |

### 7.2 `pinChangeFlow.test.ts`

**File:** `tests/e2e/system-settings/pinChangeFlow.test.ts`

| Scenario | Steps | Verification |
|---|---|---|
| First-time PIN setup | 1. Open Settings → 2. Set PIN → 3. Lock app → 4. Enter PIN | App unlocks |
| Change PIN | 1. Open Settings → 2. Change PIN (enter old, new, confirm) → 3. Lock app → 4. Enter new PIN | App unlocks with new PIN |
| Wrong old PIN rejected | 1. Change PIN with wrong current | Error shown, PIN unchanged |

### 7.3 `biometricFlow.test.ts`

**File:** `tests/e2e/system-settings/biometricFlow.test.ts`

| Scenario | Steps | Verification |
|---|---|---|
| Enable biometric | 1. Settings → Security → Enable biometric → 2. Auth → 3. Lock → 4. Biometric unlock | Works |
| Disable biometric | 1. Settings → Security → Disable biometric → 2. Lock → 3. PIN required | PIN only |

### 7.4 `backupFlow.test.ts`

**File:** `tests/e2e/system-settings/backupFlow.test.ts`

| Scenario | Steps | Verification |
|---|---|---|
| Export backup | 1. Settings → Backup → Export → 2. Share sheet opens | `.db` file shared |
| Restore backup | 1. Create backup → 2. Modify data → 3. Restore backup → 4. Enter PIN → 5. App reloads | Data reverted to backup state |
| Restore cancelled | 1. Restore → 2. Cancel PIN gate | No reload, data unchanged |

### 7.5 `adEnabledFlow.test.ts`

**File:** `tests/e2e/system-settings/adEnabledFlow.test.ts`

| Scenario | Steps | Verification |
|---|---|---|
| Enable ads → Expenses list shows banner | 1. Settings → Ads → Enable → 2. Open Gastos | `AdBanner` visible in footer |
| Disable ads → Expenses list hides banner | 1. Settings → Ads → Disable → 2. Open Gastos | `AdBanner` not rendered |
| Ads never in forms | 1. Enable ads → 2. Open Gastos/Nuevo | No `AdBanner` in form |

### 7.6 `migrationIdempotency.test.ts`

**File:** `tests/e2e/system-settings/migrationIdempotency.test.ts`

| Scenario | Steps | Verification |
|---|---|---|
| Fresh install → migration runs | 1. Install app → 2. Launch | Settings table created, user_version=2 |
| Upgrade v1→v2 → migration runs | 1. Install v1 (AsyncStorage) → 2. Upgrade to v2 → 3. Launch | AsyncStorage migrated, SQLite populated |
| Re-run migration | 1. Manually trigger migration twice | No errors, single row |

---

## 8. Cash / Stock Integrity Tests (Cross-Module)

### 8.1 `currencyFormattingIntegrity.test.ts`

**File:** `tests/integration/system-settings/currencyFormattingIntegrity.test.ts`

| Test Case | Description | Expected |
|---|---|---|
| Sales totals use Currency | `useCurrency` in sales-checkout | Formatter matches settings |
| Expenses amounts use Money | `Money` in expenses-cashflow | `format()` matches Currency |
| Financial accumulators use Currency | `Currency` in financial-accumulators | Formatter matches settings |
| Catalog prices use Money | `Money` in catalog-inventory | `format()` matches Currency |

### 8.2 `pageSizePropagation.test.ts`

**File:** `tests/integration/system-settings/pageSizePropagation.test.ts`

| Test Case | Description | Expected |
|---|---|---|
| Productos list uses pageSize | `useSettingsPageSize` in hook | `LIMIT 15` default |
| Ventas list uses pageSize | `useSettingsPageSize` in hook | `LIMIT 15` default |
| Compras list uses pageSize | `useSettingsPageSize` in hook | `LIMIT 15` default |
| Gastos list uses pageSize | `useSettingsPageSize` in hook | `LIMIT 15` default |
| Transacciones list uses pageSize | `useSettingsPageSize` in hook | `LIMIT 15` default |
| Changed pageSize propagates | Update to 25 → all lists use 25 | All `LIMIT 25` |

---

## 9. Test Infrastructure

### 9.1 SQLite Test Harness
**File:** `tests/utils/sqliteTestHarness.ts`
- In-memory SQLite for unit/integration tests
- `withTransactionSync` mocked or real (expo-sqlite JSI)
- Auto-rollback between tests

### 9.2 Mock Factories
**File:** `tests/utils/mockFactories.ts`
- `createMockUserSettings(overrides?)`
- `createMockCurrency(code?)`
- `createMockMoney(amount?, currency?)`

### 9.3 Test Utilities
**File:** `tests/utils/testUtils.tsx`
- `renderWithProviders(ui)` — wraps with Zustand stores, i18n, NativeWind
- `actAsync(() => ...)` — async act wrapper
- `waitForStoreUpdate(store, selector)` — wait for Zustand change

---

## 10. CI Gate

```yaml
# .github/workflows/test.yml (excerpt)
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v2
      - run: pnpm install --frozen-lockfile
      - run: pnpm test:unit        # unit tests
      - run: pnpm test:integration # integration tests
      - run: pnpm test:migration   # migration tests
      - run: pnpm test:e2e         # e2e (on device/cloud)
      - run: pnpm typecheck        # tsc --noEmit
      - run: pnpm lint             # eslint
```

**Pass Criteria:** All test suites 100% pass, TypeScript strict no errors, ESLint no errors.

---

*Test Spec version: 1.0 — 2026-11-10*