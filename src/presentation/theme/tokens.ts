// TypeScript wrapper sobre tokens.cjs. Re-exporta con tipos Readonly<> para
// que el sistema de tipos detecte accesos indebidos a propiedades congeladas.

import {
  PALETTE as _PALETTE,
  DARK_PALETTE as _DARK_PALETTE,
  ON as _ON,
  SCRIM as _SCRIM,
  SHADOW as _SHADOW,
  FOCUS as _FOCUS,
  DISABLED as _DISABLED,
  RIPPLE as _RIPPLE,
  TOKENS as _TOKENS,
  FALLBACK_PALETTE as _FALLBACK_PALETTE,
  colorForId as _colorForId,
} from './tokens.cjs';

type HexOrRgba = string;

export interface PaletteJson {
  readonly primary: HexOrRgba;
  readonly secondary: HexOrRgba;
  readonly accent: HexOrRgba;
  readonly dark: HexOrRgba;
  readonly darkPage: HexOrRgba;
  readonly positive: HexOrRgba;
  readonly negative: HexOrRgba;
  readonly info: HexOrRgba;
  readonly warning: HexOrRgba;
  readonly surfaceHi: HexOrRgba;
  readonly surfaceLo: HexOrRgba;
  readonly border: HexOrRgba;
  readonly borderSubtle: HexOrRgba;
  readonly ink: HexOrRgba;
  readonly inkStrong: HexOrRgba;
  readonly inkMuted: HexOrRgba;
  readonly inkFaint: HexOrRgba;
  readonly successSoft: HexOrRgba;
  readonly dangerSoft: HexOrRgba;
  readonly warningSoft: HexOrRgba;
  readonly infoSoft: HexOrRgba;
}

export interface DarkPalette {
  readonly canvas: HexOrRgba;
  readonly surface: HexOrRgba;
  readonly surfaceHi: HexOrRgba;
  readonly surfaceLo: HexOrRgba;
  readonly border: HexOrRgba;
  readonly borderSubtle: HexOrRgba;
  readonly ink: HexOrRgba;
  readonly inkStrong: HexOrRgba;
  readonly inkMuted: HexOrRgba;
  readonly inkFaint: HexOrRgba;
  readonly accent: HexOrRgba;
  readonly accentBright: HexOrRgba;
  readonly accentPurple: HexOrRgba;
  readonly success: HexOrRgba;
  readonly successSoft: HexOrRgba;
  readonly danger: HexOrRgba;
  readonly dangerSoft: HexOrRgba;
  readonly warning: HexOrRgba;
  readonly warningSoft: HexOrRgba;
  readonly info: HexOrRgba;
  readonly infoSoft: HexOrRgba;
}

export interface OnTokens {
  readonly onAccent: HexOrRgba;
  readonly onSuccess: HexOrRgba;
  readonly onDanger: HexOrRgba;
  readonly onWarning: HexOrRgba;
  readonly onInfo: HexOrRgba;
  readonly onSurface: HexOrRgba;
}

export interface ScrimTokens {
  readonly modal: HexOrRgba;
  readonly dialog: HexOrRgba;
}

export interface ShadowToken {
  readonly shadowColor: string;
  readonly shadowOffset: { readonly width: number; readonly height: number };
  readonly shadowOpacity: number;
  readonly shadowRadius: number;
  readonly elevation: number;
}

export interface ShadowTokens {
  readonly elevated: ShadowToken;
  readonly toast: ShadowToken;
  readonly fab: ShadowToken;
}

export interface FocusTokens {
  readonly border: HexOrRgba;
  readonly ring: HexOrRgba;
}

export interface DisabledTokens {
  readonly bg: HexOrRgba;
  readonly text: HexOrRgba;
  readonly border: HexOrRgba;
  readonly opacity: number;
}

export interface RippleTokens {
  readonly light: HexOrRgba;
  readonly strong: HexOrRgba;
  readonly danger: HexOrRgba;
}

export interface Tokens {
  readonly spacing: Readonly<Record<'xs' | 'sm' | 'md' | 'lg' | 'xl' | 'xxl', number>>;
  readonly radii: Readonly<Record<'sm' | 'md' | 'lg' | 'xl' | 'full', number>>;
  readonly fontSize: Readonly<Record<'xs' | 'sm' | 'base' | 'md' | 'lg' | 'xl' | 'xxl', number>>;
  readonly fontWeight: Readonly<Record<'regular' | 'medium' | 'semibold' | 'bold', string>>;
  readonly colors: DarkPalette;
  readonly on: OnTokens;
  readonly scrim: ScrimTokens;
  readonly shadow: ShadowTokens;
  readonly focus: FocusTokens;
  readonly disabled: DisabledTokens;
  readonly ripple: RippleTokens;
}

export const PALETTE: Readonly<PaletteJson> = _PALETTE;
export const DARK_PALETTE: Readonly<DarkPalette> = _DARK_PALETTE;
export const ON: Readonly<OnTokens> = _ON;
export const SCRIM: Readonly<ScrimTokens> = _SCRIM;
export const SHADOW: Readonly<ShadowTokens> = _SHADOW;
export const FOCUS: Readonly<FocusTokens> = _FOCUS;
export const DISABLED: Readonly<DisabledTokens> = _DISABLED;
export const RIPPLE: Readonly<RippleTokens> = _RIPPLE;
export const tokens: Readonly<Tokens> = _TOKENS;
export const FALLBACK_PALETTE: ReadonlyArray<string> = _FALLBACK_PALETTE;
export const colorForId: (id: number) => string = _colorForId;
