// Único consumer de tokens.cjs fuera del runtime TS.
// Importa PALETTE/DARK_PALETTE y registra las clases semánticas de Tailwind.
// NO hardcodear hex aquí — todo viene de tokens.cjs.

const { DARK_PALETTE, ON, FOCUS, DISABLED, TYPOGRAPHY, RADII, SPACING } = require('./src/presentation/theme/tokens.cjs');

const c = (v) => v;

module.exports = {
  darkMode: 'class',
  content: [
    './app/**/*.{js,jsx,ts,tsx}',
    './src/**/*.{js,jsx,ts,tsx}',
  ],
  presets: [require('nativewind/preset')],
  theme: {
    extend: {
      colors: {
        canvas: {
          DEFAULT: DARK_PALETTE.canvas,
          light: DARK_PALETTE.surface,
          dark: DARK_PALETTE.surfaceLo,
        },
        surface: {
          DEFAULT: DARK_PALETTE.surface,
          hi: DARK_PALETTE.surfaceHi,
          lo: DARK_PALETTE.canvas,
        },
        border: {
          DEFAULT: DARK_PALETTE.border,
          subtle: DARK_PALETTE.borderSubtle,
          focus: FOCUS.border,
        },
        ink: {
          DEFAULT: DARK_PALETTE.ink,
          strong: DARK_PALETTE.inkStrong,
          muted: DARK_PALETTE.inkMuted,
          faint: DARK_PALETTE.inkFaint,
          inverse: DARK_PALETTE.canvas,
        },
        accent: {
          DEFAULT: DARK_PALETTE.accent,
          bright: DARK_PALETTE.accentBright,
          purple: DARK_PALETTE.accentPurple,
        },
        on: {
          accent: ON.onAccent,
          success: ON.onSuccess,
          danger: ON.onDanger,
          warning: ON.onWarning,
          info: ON.onInfo,
          surface: ON.onSurface,
        },
        success: {
          50: DARK_PALETTE.successSoft,
          100: DARK_PALETTE.successSoft,
          DEFAULT: DARK_PALETTE.success,
          500: DARK_PALETTE.success,
          600: DARK_PALETTE.success,
          700: DARK_PALETTE.success,
          800: DARK_PALETTE.success,
          900: DARK_PALETTE.success,
        },
        danger: {
          50: DARK_PALETTE.dangerSoft,
          100: DARK_PALETTE.dangerSoft,
          DEFAULT: DARK_PALETTE.danger,
          500: DARK_PALETTE.danger,
          600: DARK_PALETTE.danger,
          700: DARK_PALETTE.danger,
          800: DARK_PALETTE.danger,
          900: DARK_PALETTE.danger,
        },
        warning: {
          50: DARK_PALETTE.warningSoft,
          DEFAULT: DARK_PALETTE.warning,
          500: DARK_PALETTE.warning,
          600: DARK_PALETTE.warning,
          700: DARK_PALETTE.warning,
          800: DARK_PALETTE.warning,
        },
        info: {
          50: DARK_PALETTE.infoSoft,
          DEFAULT: DARK_PALETTE.info,
          500: DARK_PALETTE.info,
        },
        disabled: {
          bg: DISABLED.bg,
          text: DISABLED.text,
          border: DISABLED.border,
        },
        scrim: {
          modal: c('rgba(0, 0, 0, 0.70)'),
          dialog: c('rgba(0, 0, 0, 0.50)'),
        },
      },
      fontSize: {
        xs: [TYPOGRAPHY.size.xs, { lineHeight: TYPOGRAPHY.lineHeight.tight }],
        sm: [TYPOGRAPHY.size.sm, { lineHeight: TYPOGRAPHY.lineHeight.tight }],
        base: [TYPOGRAPHY.size.base, { lineHeight: TYPOGRAPHY.lineHeight.normal }],
        md: [TYPOGRAPHY.size.md, { lineHeight: TYPOGRAPHY.lineHeight.normal }],
        lg: [TYPOGRAPHY.size.lg, { lineHeight: TYPOGRAPHY.lineHeight.normal }],
        xl: [TYPOGRAPHY.size.xl, { lineHeight: TYPOGRAPHY.lineHeight.normal }],
        xxl: [TYPOGRAPHY.size.xxl, { lineHeight: TYPOGRAPHY.lineHeight.tight }],
      },
      fontWeight: {
        regular: TYPOGRAPHY.weight.regular,
        medium: TYPOGRAPHY.weight.medium,
        semibold: TYPOGRAPHY.weight.semibold,
        bold: TYPOGRAPHY.weight.bold,
      },
      borderRadius: {
        sm: RADII.sm,
        md: RADII.md,
        lg: RADII.lg,
        xl: RADII.xl,
        '2xl': RADII.xl,
        full: RADII.full,
      },
      spacing: {
        'ds-xs': SPACING.xs,
        'ds-sm': SPACING.sm,
        'ds-md': SPACING.md,
        'ds-lg': SPACING.lg,
        'ds-xl': SPACING.xl,
        'ds-xxl': SPACING.xxl,
      },
    },
  },
  plugins: [],
};

