# Documentation Index — TPV Móvil Express v2.0

| File | Purpose |
|---|---|
| `/docs/global.spec.md` | High-level spec: bounded contexts, NFRs, invariants, build flavors |
| `/docs/plan.md` | Migration roadmap v1.0 → v2.0 (Phases 0–7) |
| `/docs/MIGRATION_CHECKLIST.md` | RN → KMP feature parity tracker (`[x]` done / `[ ]` Phase 6) |
| `/docs/modules/_blueprint/README.md` | Triad template (spec/plan/test) |
| `/docs/modules/README.md` | Module inventory |

## Module Triads
- `catalog-inventory/` — spec, plan, test
- `sales-checkout/` — spec, plan, test
- `expenses-cashflow/` — spec, plan, test
- `financial-accumulators/` — spec, plan, test
- `system-settings/` — spec, plan, test (owns backup format + PIN)

## Conventions
- Code/architecture language: English.
- UI visible strings: Spanish via `composeApp/src/commonMain/composeResources/values-es/strings.xml`.
- Default theme: Strict Dark Mode with Material3 design tokens.
- Ads: compile-time only — `free` (`ADS_ENABLED = true`) / `pro` (`false`), shared `.jks`, identical `applicationId`.
- Tests: Kotlin (`kotlin.test`, Koin test, coroutines-test, Compose UI test).

## Verification Commands (from `kmp-app/`)
```bash
./gradlew :composeApp:generateCommonMainTpvDatabaseInterface
./gradlew :composeApp:compileAndroidMain
./gradlew :composeApp:testAndroidHostTest
./gradlew :androidApp:assembleFreeDebug
./gradlew :androidApp:assembleProDebug
./gradlew detekt
```
