# System & Settings — Test Plan

## 1. Unit Tests (TDD)
| Test | File | Coverage |
|---|---|---|
| PinService.setPin | `src/__tests__/infrastructure/security/PinService.test.ts` | Current PIN required |
| BiometricService | `src/__tests__/infrastructure/security/BiometricService.test.ts` | authenticateAsync |
| settingsStore | `src/__tests__/stores/settingsStore.test.ts` | divisa/pageSize/biometric |
| sessionStore | `src/__tests__/stores/sessionStore.test.ts` | unlocked toggle |

## 2. Integration Tests
| Test | File | Scenario |
|---|---|---|
| Export DB | `src/__tests__/application/backup/ExportarDB.test.ts` | File copy + share |

## 3. Cash / Stock Integrity Tests
| Test | Invariant | File |
|---|---|---|
| PIN gate blocks deletion | INV-08 | `src/__tests__/presentation/hooks/usePinGate.test.ts` |

## 4. Gate
All tests pass before module marked complete.
