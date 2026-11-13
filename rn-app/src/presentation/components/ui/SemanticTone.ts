// Sistema unificado de tonos semánticos para reportar/visualizar datos.
// Antes había 7 mapas TONE_STYLES dispersos (Stat, ReportRow, BreakdownBar,
// MovimientoListItem, Badge, RegistroSegmented, caja local). Este módulo
// centraliza el contrato: cualquier componente que necesite colorear por
// estado/resultado consume TONE_STYLES de aquí.

export type SemanticTone =
  | 'success'
  | 'warning'
  | 'danger'
  | 'info'
  | 'primary'
  | 'accent'
  | 'neutral';

export interface ToneStyle {
  readonly bg: string;
  readonly bar: string;
  readonly text: string;
  readonly hex: string;
  readonly on: string;
  readonly soft: string;
}

const T = (
  bg: string,
  bar: string,
  text: string,
  hex: string,
  on: string,
  soft: string,
): ToneStyle =>
  Object.freeze({
    bg,
    bar,
    text,
    hex,
    on,
    soft,
  });

export const TONE_STYLES: Readonly<Record<SemanticTone, ToneStyle>> = Object.freeze({
  success: T('bg-success-soft', 'bg-success', 'text-success', '#4ECB6F', 'text-onSuccess', 'bg-success-soft'),
  warning: T('bg-warning-soft', 'bg-warning', 'text-warning', '#E0B23A', 'text-onWarning', 'bg-warning-soft'),
  danger: T('bg-danger-soft', 'bg-danger', 'text-danger', '#E85A6B', 'text-onDanger', 'bg-danger-soft'),
  info: T('bg-info-soft', 'bg-info', 'text-info', '#53A7B8', 'text-onInfo', 'bg-info-soft'),
  primary: T('bg-surface-hi', 'bg-accent', 'text-accent', '#2F578A', 'text-onAccent', 'bg-surface-hi'),
  accent: T('bg-surface-hi', 'bg-accent-bright', 'text-accent-bright', '#36ADA3', 'text-onAccent', 'bg-surface-hi'),
  neutral: T('bg-surface-hi', 'bg-surface-hi', 'text-ink-strong', '#232F72', 'text-ink-strong', 'bg-surface-hi'),
});

export type ResumenTone = 'success' | 'warning' | 'danger' | 'neutral';

export function isResumenTone(tone: SemanticTone): tone is ResumenTone {
  return tone === 'success' || tone === 'warning' || tone === 'danger' || tone === 'neutral';
}
