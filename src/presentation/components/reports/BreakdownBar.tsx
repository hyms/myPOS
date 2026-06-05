import React, { useMemo } from 'react';
import { Text, View, type ViewStyle } from 'react-native';

import { cn } from '@/shared/utils/cn';
import { useCurrency } from '@/presentation/hooks/useCurrency';

interface BreakdownBarProps {
  readonly label: string;
  readonly value: number;
  readonly max: number;
  readonly tone: 'success' | 'warning' | 'danger' | 'primary';
  readonly format?: (n: number) => string;
  readonly className?: string;
}

const TONE_BG: Readonly<Record<BreakdownBarProps['tone'], string>> = {
  success: 'bg-success',
  warning: 'bg-warning',
  danger: 'bg-danger',
  primary: 'bg-accent',
};

const TONE_TEXT: Readonly<Record<BreakdownBarProps['tone'], string>> = {
  success: 'text-success',
  warning: 'text-warning',
  danger: 'text-danger',
  primary: 'text-ink-strong',
};

function BreakdownBarComponent({
  label,
  value,
  max,
  tone,
  format,
  className,
}: BreakdownBarProps) {
  const safeMax = max <= 0 ? 1 : max;
  const pct = useMemo(
    () => Math.max(0.02, Math.min(1, value / safeMax)),
    [value, safeMax],
  );
  const { format: fallbackFormat } = useCurrency();
  const fmt = format ?? fallbackFormat;
  const pctLabel = `${Math.round(pct * 100)} por ciento`;
  return (
    <View className={cn('gap-1.5', className)}>
      <View className="flex-row items-center justify-between">
        <View className="flex-row items-center gap-2">
          <View className={cn('h-2 w-2 rounded-full', TONE_BG[tone])} />
          <Text className="text-xs font-medium text-ink">
            {label}
          </Text>
        </View>
        <Text className={cn('text-sm font-bold tabular-nums', TONE_TEXT[tone])}>
          {fmt(value)}
        </Text>
      </View>
      <View
        accessibilityRole="progressbar"
        accessibilityLabel={`${label}: ${fmt(value)}`}
        accessibilityValue={{ min: 0, max: 100, now: Math.round(pct * 100) }}
        className="h-1.5 w-full overflow-hidden rounded-full bg-surface-hi"
      >
        <View
          className={cn('h-full rounded-full', TONE_BG[tone])}
          style={{ width: `${pct * 100}%` as ViewStyle['width'] }}
        />
      </View>
      <Text
        accessibilityElementsHidden
        importantForAccessibility="no"
        className="text-[10px] text-ink-muted"
      >
        {pctLabel}
      </Text>
    </View>
  );
}

export const BreakdownBar = React.memo(BreakdownBarComponent);

interface BreakdownProps {
  readonly items: ReadonlyArray<{
    label: string;
    value: number;
    tone: BreakdownBarProps['tone'];
  }>;
  readonly className?: string;
}

function BreakdownComponent({ items, className }: BreakdownProps) {
  const max = useMemo(
    () => items.reduce((acc, it) => Math.max(acc, it.value), 0),
    [items],
  );
  return (
    <View className={cn('gap-3', className)}>
      {items.map((it) => (
        <BreakdownBar
          key={it.label}
          label={it.label}
          value={it.value}
          max={max}
          tone={it.tone}
        />
      ))}
    </View>
  );
}

export const Breakdown = React.memo(BreakdownComponent);
