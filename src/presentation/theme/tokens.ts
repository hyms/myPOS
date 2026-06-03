export const tokens = {
  spacing: {
    xs: 4,
    sm: 8,
    md: 12,
    lg: 16,
    xl: 24,
    xxl: 32,
  },
  radii: {
    sm: 6,
    md: 12,
    lg: 16,
    xl: 24,
    full: 9999,
  },
  fontSize: {
    xs: 11,
    sm: 13,
    base: 15,
    md: 17,
    lg: 20,
    xl: 24,
    xxl: 32,
  },
  fontWeight: {
    regular: '400' as const,
    medium: '500' as const,
    semibold: '600' as const,
    bold: '700' as const,
  },
};

export const FALLBACK_PALETTE: ReadonlyArray<string> = [
  '#0ea5e9',
  '#8b5cf6',
  '#ec4899',
  '#f97316',
  '#22c55e',
  '#06b6d4',
  '#eab308',
  '#ef4444',
  '#14b8a6',
  '#6366f1',
];

export function colorForId(id: number): string {
  const palette = FALLBACK_PALETTE;
  const idx = ((id % palette.length) + palette.length) % palette.length;
  return palette[idx] as string;
}
