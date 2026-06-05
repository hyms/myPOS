// SINGLE SOURCE OF TRUTH — CommonJS module
// Importable desde: tailwind.config.js, tokens.ts (via esModuleInterop),
// scripts de build, y cualquier archivo JS. NO usar desde componentes TSX
// directamente; usar @/presentation/theme (el barrel) o @/presentation/theme/tokens.

const PALETTE = require('./palette.json');

const DARK_PALETTE = Object.freeze({
  canvas: PALETTE.darkPage,
  surface: PALETTE.dark,
  surfaceHi: PALETTE.surfaceHi,
  surfaceLo: PALETTE.surfaceLo,
  border: PALETTE.border,
  borderSubtle: PALETTE.borderSubtle,
  ink: PALETTE.ink,
  inkStrong: PALETTE.inkStrong,
  inkMuted: PALETTE.inkMuted,
  inkFaint: PALETTE.inkFaint,
  accent: PALETTE.primary,
  accentBright: PALETTE.secondary,
  accentPurple: PALETTE.accent,
  success: PALETTE.positive,
  successSoft: PALETTE.successSoft,
  danger: PALETTE.negative,
  dangerSoft: PALETTE.dangerSoft,
  warning: PALETTE.warning,
  warningSoft: PALETTE.warningSoft,
  info: PALETTE.info,
  infoSoft: PALETTE.infoSoft,
});

// "Foreground sobre color saturado" — para texto/icono dentro de botones, chips y
// badges. Equivale a inkStrong (#F8F9FC, 11.6:1 sobre surface). Centralizar evita
// que `text-white` se filtre por copy-paste a fondos de color donde pierde semántica.
const ON = Object.freeze({
  onAccent: PALETTE.inkStrong,
  onSuccess: PALETTE.inkStrong,
  onDanger: PALETTE.inkStrong,
  onWarning: PALETTE.inkStrong,
  onInfo: PALETTE.inkStrong,
  onSurface: PALETTE.ink,
});

// Scrims semitransparentes sobre fondo. `modal` para sheets (más opaco), `dialog`
// para confirmaciones/alerts (menos opaco, deja ver contexto). Anteriormente había
// 4 archivos con `bg-black/50` o `bg-black/70` hardcoded.
const SCRIM = Object.freeze({
  modal: 'rgba(0, 0, 0, 0.70)',
  dialog: 'rgba(0, 0, 0, 0.50)',
});

// Sombras reutilizables. Reemplazan los `boxShadow` con `rgba` hardcoded en FABs
// y Toast. Usadas vía `style={[SHADOW.elevated, style]}`.
const SHADOW = Object.freeze({
  elevated: Object.freeze({
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.40,
    shadowRadius: 16,
    elevation: 6,
  }),
  toast: Object.freeze({
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.30,
    shadowRadius: 8,
    elevation: 6,
  }),
  fab: Object.freeze({
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.40,
    shadowRadius: 16,
    elevation: 8,
  }),
});

// Estados de focus. `borderFocus` para outline de Input/Select cuando recibe
// teclado; `ringFocus` para halos/glows si se usan.
const FOCUS = Object.freeze({
  border: PALETTE.secondary,
  ring: 'rgba(54, 173, 163, 0.30)',
});

// Estados de disabled. `opacity` para aplicar globalmente sobre cualquier fondo.
// `bgDisabled` y `textDisabled` para los casos donde se quiera un look más
// "diferenciado" que solo opacar.
const DISABLED = Object.freeze({
  bg: PALETTE.surfaceLo,
  text: PALETTE.inkFaint,
  border: PALETTE.borderSubtle,
  opacity: 0.5,
});

// Ripple para android_ripple en Pressable. `light` para superficies neutras,
// `strong` para chips activos, `danger` para botones destructivos.
const RIPPLE = Object.freeze({
  light: 'rgba(226, 229, 238, 0.10)',
  strong: 'rgba(226, 229, 238, 0.18)',
  danger: 'rgba(232, 90, 107, 0.20)',
});

// Escala tipográfica. Consumida por tailwind.config.js para registrar las
// clases `text-xs/sm/base/...`, `font-{regular,medium,semibold,bold}`, y los
// `lineHeight` correspondientes. No se exporta como `TOKENS.fontSize` porque
// el acceso programático desde TSX no tiene consumidores (cero referencias).
const TYPOGRAPHY = Object.freeze({
  size: Object.freeze({
    xs: 11,
    sm: 13,
    base: 15,
    md: 17,
    lg: 20,
    xl: 24,
    xxl: 32,
  }),
  lineHeight: Object.freeze({
    tight: 1.2,
    normal: 1.4,
  }),
  weight: Object.freeze({
    regular: '400',
    medium: '500',
    semibold: '600',
    bold: '700',
  }),
});

// Radios registrados como `rounded-sm/md/lg/xl/2xl/full`. 2xl y xl comparten
// valor (24) por simplicidad; el resto respeta la progresión.
const RADII = Object.freeze({
  sm: 6,
  md: 12,
  lg: 16,
  xl: 24,
  full: 9999,
});

// Espaciado semántico del design system. Expuesto en Tailwind como
// `ds-xs`/`ds-sm`/`ds-md`/`ds-lg`/`ds-xl`/`ds-xxl` para `p-*`/`m-*`/`gap-*`.
// Las clases numéricas nativas (p-1, p-2, gap-1.5, etc.) siguen disponibles;
// este set es para casos que necesitan anclarse al design system explícito.
const SPACING = Object.freeze({
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 24,
  xxl: 32,
});

// Tokens agregados. `tokens.colors` apunta a DARK_PALETTE para que
// consumidores antiguos (`tokens.colors.surface`) sigan funcionando.
const TOKENS = Object.freeze({
  colors: DARK_PALETTE,
  on: ON,
  scrim: SCRIM,
  shadow: SHADOW,
  focus: FOCUS,
  disabled: DISABLED,
  ripple: RIPPLE,
});

// Paleta cíclica para avatares de productos sin imagen.
const FALLBACK_PALETTE = Object.freeze([
  PALETTE.primary,
  PALETTE.secondary,
  PALETTE.accent,
  PALETTE.positive,
  PALETTE.info,
  PALETTE.warning,
  PALETTE.negative,
]);

function colorForId(id) {
  const palette = FALLBACK_PALETTE;
  const len = palette.length;
  const idx = ((id % len) + len) % len;
  return palette[idx] || '';
}

module.exports = {
  PALETTE,
  DARK_PALETTE,
  ON,
  SCRIM,
  SHADOW,
  FOCUS,
  DISABLED,
  RIPPLE,
  TYPOGRAPHY,
  RADII,
  SPACING,
  TOKENS,
  FALLBACK_PALETTE,
  colorForId,
};
