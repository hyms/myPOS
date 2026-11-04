# System & Settings — Implementation Plan

## 1. Component Breakdown
| Component | File | Type | Memoized? |
|---|---|---|---|
| `DivisaSection` | `src/presentation/components/settings/DivisaSection.tsx` | UI | No |
| `PaginacionSection` | `src/presentation/components/settings/PaginacionSection.tsx` | UI | No |
| `SeguridadSection` | `src/presentation/components/settings/SeguridadSection.tsx` | UI | No |
| `BackupSection` | `src/presentation/components/settings/BackupSection.tsx` | UI | No |
| `SectionHeader` | `src/presentation/components/settings/SectionHeader.tsx` | UI | No |

## 2. Implementation Steps
1. Implement `settingsStore` (divisa, pageSize, biometric) via Zustand + AsyncStorage.
2. Implement `sessionStore` (unlocked) + `usePinGate` for critical actions.
3. Implement `PinService` (SecureStore + sha256) and `BiometricService` (expo-local-authentication).
4. Build settings screens: divisa, paginación, seguridad, backup.
5. Implement `ExportarDB` / `RestaurarDB` (file copy + share / document-picker + reload).
6. Build `app/lock.tsx` PIN/biometric gate.

## 3. Migration Tasks (v1.0 → v2.0)
- [ ] Add `spec.md`, `plan.md`, `test.md` to `/docs/modules/system-settings/`.
- [ ] Confirm backup restore does NOT require PIN (decision 2026-06-03).
