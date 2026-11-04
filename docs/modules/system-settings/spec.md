# System & Settings — Specification (v2.0)

## 1. Bounded Context
Seguridad PIN/biometría, moneda (i18n), paginación, copia de seguridad, pantalla de bloqueo (lock).

## 2. Domain Entities & Value Objects

### 2.1 Currency (Value Object)
**Archivo:** `src/domain/value-objects/Currency.ts`

```ts
export type CurrencyCode = 'Bs' | '$' | '€';

export interface Currency {
  readonly code: CurrencyCode;
  readonly symbol: string;
  readonly formatter: Intl.NumberFormat;
}
```

### 2.2 Session (Estado de UI)
**Archivo:** `src/presentation/stores/sessionStore.ts`

```ts
export interface SessionState {
  readonly unlocked: boolean;
}
```

### 2.3 Settings (Estado de UI)
**Archivo:** `src/presentation/stores/settingsStore.ts`

```ts
export interface SettingsState {
  readonly currency: CurrencyCode;
  readonly pageSize: number;      // default 15
  readonly biometricEnabled: boolean;
}
```

## 3. Domain Rules & Invariants

| ID | Regla | Descripción |
|---|---|---|
| RULE-SS-01 | PIN cambio requiere PIN actual | `PinService.setPin(newPin)` exige PIN actual, excepto primera vez. |
| RULE-SS-02 | Biometría requiere autenticación | Switch de biometría usa `LocalAuthentication.authenticateAsync` antes de activarse. |
| RULE-SS-03 | PIN gate para acciones críticas | Cambio de PIN y eliminación de transacción requieren PIN actual (`usePinGate`). |
| RULE-SS-04 | Restauración SIN PIN | Restaurar backup no requiere PIN (flujo crítico de recuperación). `ConfirmDialog`: `"Esto sobrescribirá todos los datos actuales"`. |
| RULE-SS-05 | Divisa solo visual | `Currency` afecta formato (`Intl.NumberFormat`). SQLite almacena montos numéricos puros en moneda base. |
| RULE-SS-06 | Paginación por defecto | `pageSize` default 15. Todos los hooks históricos aplican `LIMIT ? OFFSET ?`. |

Invariantes globales: **INV-08, INV-10**.

## 4. Database Contract
No hay tabla dedicada para settings. Persistencia vía `AsyncStorage` (`SettingsStore`). PIN hash en `expo-secure-store`.

## 5. Infrastructure Services

### 5.1 PinService
**Archivo:** `src/infrastructure/security/PinService.ts`

| Método | Firma | Descripción |
|---|---|---|
| `setPin` | `(newPin: string) => void` | Exige PIN actual excepto primera vez. Guarda hash SHA-256 en SecureStore. |
| `verifyPin` | `(pin: string) => boolean` | Compara hash SHA-256. |

### 5.2 BiometricService
**Archivo:** `src/infrastructure/security/BiometricService.ts`

| Método | Firma | Descripción |
|---|---|---|
| `authenticate` | `() => Promise<boolean>` | `LocalAuthentication.authenticateAsync`. |

### 5.3 SettingsStore
**Archivo:** `src/infrastructure/settings/SettingsStore.ts`

Persiste `currency`, `pageSize`, `biometricEnabled` en `AsyncStorage`.

### 5.4 ToastService
**Archivo:** `src/infrastructure/toast/ToastService.ts`

Envoltorio sobre `react-native-toast-message`.

## 6. Use Cases / Servicios de Backup

### 6.1 `ExportarDB`
**Archivo:** `src/application/backup/ExportarDB.ts`

```ts
// FileSystem.copyAsync(dbPath, documentDirectory + 'backups/tpv-YYYYMMDD-HHmm.db')
// Sharing.shareAsync(uri)
```

### 6.2 `RestaurarDB`
**Archivo:** `src/application/backup/RestaurarDB.ts`

```ts
// DocumentPicker.getDocumentAsync → cerrar DB → copiar sobre archivo principal
// → reabrir → forzar recarga de stores / app reload
```

## 7. UI State Machines (texto visible en español)

### Pantalla Lock
| Estado | Disparador | Siguiente | Texto visible (ES) |
|---|---|---|---|
| `Bloqueada` | App inicia, PIN configurado | `Autenticando` | `"Ingrese su PIN"` |
| `Autenticando` | Biometría exitosa | `Desbloqueada` | (silencioso) |
| `Autenticando` | PIN correcto | `Desbloqueada` | (silencioso) |
| `Autenticando` | PIN incorrecto | `Error` | `"PIN incorrecto"` |

### Pantalla Settings
| Estado | Disparador | Siguiente | Texto visible (ES) |
|---|---|---|---|
| `Settings` | Abrir configuración | — | `"Configuración"` |
| `Divisa` | Seleccionar moneda | — | `"Bs" / "$" / "€"` |
| `Paginación` | Cambiar pageSize | — | `"Filas por consulta"` |
| `Seguridad` | Cambiar PIN | `PinGate` | `"Nuevo PIN"` / `"PIN actual"` |
| `Seguridad` | Toggle biometría | `BiometricAuth` | `"Activar huella/dactilar"` |
| `Backup` | Tocar Exportar | — | `"Exportar copia de seguridad"` |
| `Backup` | Tocar Restaurar | `ConfirmDialog` | `"Esto sobrescribirá todos los datos actuales"` |

## 8. Componentes UI (Settings)

| Componente | Archivo | Tipo | Memoizado |
|---|---|---|---|
| `DivisaSection` | `src/presentation/components/settings/DivisaSection.tsx` | UI | No |
| `PaginacionSection` | `src/presentation/components/settings/PaginacionSection.tsx` | UI | No |
| `SeguridadSection` | `src/presentation/components/settings/SeguridadSection.tsx` | UI | No |
| `BackupSection` | `src/presentation/components/settings/BackupSection.tsx` | UI | No |
| `SectionHeader` | `src/presentation/components/settings/SectionHeader.tsx` | UI | No |

## 9. Pantallas

| Pantalla | Archivo | Responsabilidad |
|---|---|---|
| `SettingsIndex` | `app/settings/index.tsx` | Contenedor de 4 secciones |
| `Lock` | `app/lock.tsx` | Pantalla de PIN / Biometría |

## 10. Hooks

| Hook | Archivo | Responsabilidad |
|---|---|---|
| `useSettingsCurrency` | `src/presentation/hooks/useSettingsCurrency.ts` | Divisa seleccionada |
| `useSettingsPageSize` | `src/presentation/hooks/useSettingsPageSize.ts` | PageSize para paginación |
| `useSettingsSecurity` | `src/presentation/hooks/useSettingsSecurity.ts` | PIN y biometría |
| `useSettingsBackup` | `src/presentation/hooks/useSettingsBackup.ts` | Exportar / Restaurar DB |
| `usePinGate` | `src/presentation/hooks/usePinGate.ts` | Bloqueo acciones críticas |
| `useCurrency` | `src/presentation/hooks/useCurrency.ts` | Formateador `Intl.NumberFormat` |
| `useReducedMotion` | `src/presentation/hooks/useReducedMotion.ts` | Preferencia de movimiento reducido |

## 11. Almacenes Zustand

| Store | Archivo | Responsabilidad |
|---|---|---|
| `settingsStore` | `src/presentation/stores/settingsStore.ts` | `divisa`, `pageSize`, `biometric` |
| `sessionStore` | `src/presentation/stores/sessionStore.ts` | `unlocked: boolean` |
| `invalidationStore` | `src/presentation/stores/invalidationStore.ts` | Invalidación manual (estilo TanStack Query) |
| `registroSubtabStore` | `src/presentation/stores/registroSubtabStore.ts` | Sub-tab activo `VENTA` / `COMPRA` |

## 12. NFRs Aplicables
- NFR-06: Dark mode estricto + tokens de diseño en tiempo de ejecución (NativeWind v4).
- NFR-08: UI en español; código/DB en inglés.
- NFR-09: TypeScript estricto.

## 13. Puntos de extensión (pendiente de refinamiento)
- [ ] Bloqueo automático tras inactividad.
- [ ] Recuperación de PIN por pregunta de seguridad.
- [ ] Backup programado automático.
