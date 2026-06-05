# Tokens centralizados (SINGLE SOURCE OF TRUTH)

## Archivos

- `palette.json` → **Fuente de verdad única**. Editar SOLO este archivo para cambiar la paleta.
- `tokens.cjs` → CommonJS. Importa `palette.json`, deriva `DARK_PALETTE` y los tokens
  agregados (`ON`, `SCRIM`, `SHADOW`, `FOCUS`, `DISABLED`, `RIPPLE`, `tokens`).
- `tokens.ts` → Wrapper TypeScript. Importa `tokens.cjs` y re-exporta con tipos
  `Readonly<>`. **Usar este path** desde código TS/TSX.
- `index.ts` → Barrel para `import { ... } from '@/presentation/theme'`.
- `tailwind.config.js` → Importa `tokens.cjs` y registra las clases de Tailwind.

## Paleta cruda (`PALETTE` en `palette.json`)

| Variable     | Hex      | Uso sugerido                          |
| ------------ | -------- | ------------------------------------- |
| `primary`    | #2F578A  | Acentos principales, bordes           |
| `secondary`  | #36ADA3  | Acentos brillantes (teal)             |
| `accent`     | #943EA3  | Acentos especiales (purple highlight) |
| `dark`       | #232F72  | Superficie base (cards)               |
| `darkPage`   | #121358  | Canvas / fondo de pantalla            |
| `positive`   | #2FA149  | Éxito (VENTAS, OK)                    |
| `negative`   | #C43F4E  | Error / Eliminar                      |
| `info`       | #53A7B8  | Información                           |
| `warning`    | #BA8B09  | Advertencias                          |

## Mapeo semántico (`DARK_PALETTE`)

```
canvas      → darkPage     (#121358)
surface     → dark         (#232F72)
surfaceHi   → primary      (#2F578A)
surfaceLo   → #080A22
border      → primary      (#2F578A)
borderSubtle→ rgba(primary, 0.45)
accent      → primary      (#2F578A)
accentBright→ secondary    (#36ADA3)
accentPurple→ accent       (#943EA3)
success     → positive
danger      → negative
warning     → warning
info        → info
ink         → #E2E5EE
inkStrong   → #F8F9FC
inkMuted    → rgba(ink, 0.72)
inkFaint    → rgba(ink, 0.48)
```

## Tokens agregados (no son colores, son contratos de uso)

| Token              | Para qué sirve                                          |
| ------------------ | ------------------------------------------------------- |
| `ON.onAccent`      | Texto/icono sobre `bg-accent` (botones primary)         |
| `ON.onSuccess`     | Texto/icono sobre `bg-success`                          |
| `ON.onDanger`      | Texto/icono sobre `bg-danger` (botón eliminar)          |
| `ON.onWarning`     | Texto/icono sobre `bg-warning`                          |
| `ON.onInfo`        | Texto/icono sobre `bg-info`                             |
| `ON.onSurface`     | Texto/icono sobre `bg-surface` (default)                |
| `SCRIM.modal`      | Fondo semitransparente de sheets (70% black)            |
| `SCRIM.dialog`     | Fondo semitransparente de alerts/dialogs (50% black)    |
| `SHADOW.elevated`  | FAB, cards elevados                                     |
| `SHADOW.toast`     | Notificaciones toast                                    |
| `SHADOW.fab`       | FABs (mayor elevación)                                  |
| `FOCUS.border`     | Borde de Input/Select cuando recibe focus               |
| `FOCUS.ring`       | Halo/glow de focus (opcional)                           |
| `DISABLED.bg/text` | Fondo y texto para estado deshabilitado                 |
| `DISABLED.opacity` | Opacidad global (`0.5`) a aplicar sobre variantes       |
| `RIPPLE.light/strong/danger` | Color de ripple Android para Pressable         |

## Contraste WCAG verificado (sobre `bg-surface` = #232F72)

| Token            | Color    | Ratio   | Cumple            |
| ---------------- | -------- | ------- | ----------------- |
| `inkStrong`      | #F8F9FC  | 11.59   | AAA               |
| `ink`            | #E2E5EE  | 9.69    | AAA               |
| `inkMuted` (α72) | -        | 5.81    | AA normal         |
| `inkFaint` (α48) | -        | 3.45    | Solo Large text   |
| `success`        | #2FA149  | 3.67    | Solo Large text   |
| `warning`        | #BA8B09  | 3.94    | Solo Large text   |
| `danger`         | #C43F4E  | 2.42    | ❌ Falla AA Large |

## Reglas de uso

1. **Nunca** hardcodear hex en componentes. Usar `DARK_PALETTE.*` (TS) o clases
   semánticas de Tailwind (`bg-canvas`, `text-ink`, `bg-accent`, `text-onAccent`, etc.).
2. Para cambiar un color: editar `palette.json`. Todo lo demás se propaga por tokens.
3. `text-white` y `'#fff'`/`'#ffffff'` **NO** se permiten en código. Usar
   `ON.onAccent` (o el `onX` correspondiente) según el fondo. Ver `Commit 3` del
   refactor de la lista de pendientes.
4. El `ComprobanteRenderer.ts` (PDF) mantiene su paleta local porque se imprime
   en papel blanco; no comparte tokens con la UI.
5. Los scrims semitransparentes (`bg-black/X`) deben migrar a `bg-scrim-modal`
   o `bg-scrim-dialog` cuando se introduzcan las clases en los modales.
