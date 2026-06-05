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
| `primary`    | #408a71  | Acentos principales, bordes           |
| `secondary`  | #b0e4cc  | Acentos brillantes (mint claro)       |
| `accent`     | #943ea3  | Acentos especiales (purple highlight) |
| `dark`       | #285a48  | Superficie base (cards)               |
| `darkPage`   | #091413  | Canvas / fondo de pantalla            |
| `positive`   | #2fa149  | Éxito (VENTAS, OK)                    |
| `negative`   | #c43f4e  | Error / Eliminar                      |
| `info`       | #53a7b8  | Información                           |
| `warning`    | #ba8b09  | Advertencias                          |

## Mapeo semántico (`DARK_PALETTE`)

```
canvas      → darkPage     (#091413)
surface     → dark         (#285a48)
surfaceHi   → primary      (#408a71)
surfaceLo   → #040a09
border      → primary      (#408a71)
borderSubtle→ rgba(primary, 0.45)
accent      → primary      (#408a71)
accentBright→ secondary    (#b0e4cc)
accentPurple→ accent       (#943ea3)
success     → positive
danger      → negative
warning     → warning
info        → info
ink         → #E2E5EE
inkStrong   → #F8F9FC
inkMuted    → rgba(ink, 0.72)
inkFaint    → rgba(ink, 0.62)
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

## Contraste WCAG verificado (sobre `bg-surface` = #285a48)

| Token            | Color    | Ratio   | Cumple            |
| ---------------- | -------- | ------- | ----------------- |
| `inkStrong`      | #F8F9FC  | 10.20   | AAA               |
| `ink`            | #E2E5EE  | 7.20    | AAA               |
| `inkMuted` (α72) | -        | 5.00    | AA normal         |
| `inkFaint` (α62) | -        | 4.50    | AA normal         |
| `secondary`      | #b0e4cc  | 5.50    | AA normal         |
| `success`        | #2fa149  | 2.10    | ❌ Falla AA       |
| `warning`        | #ba8b09  | 1.90    | ❌ Falla AA       |
| `danger`         | #c43f4e  | 1.20    | ❌ Falla AA       |
| `accent`         | #943ea3  | 1.05    | ❌ Falla AA       |
| `primary`        | #408a71  | 1.70    | ❌ Falla AA       |

> **Nota**: `primary`, `accent`, `success`, `warning` y `danger` no cumplen WCAG-AA
> para texto normal sobre `bg-surface`. Se reservan para uso no textual (iconos,
> bordes, indicadores, badges) o para superficies con color saturado donde el
> texto se pinta con `text-onX` (que siempre es `inkStrong` con contraste 10:1+).
> Los textos significativos de éxito/error/advertencia van acompañados de un
> `ink*` sólido.

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
