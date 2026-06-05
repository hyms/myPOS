import React, { memo } from 'react';
import { Text, View } from 'react-native';

import { cn } from '@/shared/utils/cn';

export type StatTone = 'success' | 'warning' | 'danger' | 'neutral';

interface ToneStyle {
  readonly bg: string;
  readonly bar: string;
  readonly text: string;
}

const TONE_STYLES: Readonly<Record<StatTone, ToneStyle>> = {
  success: { bg: 'bg-success-soft', bar: 'bg-success', text: 'text-success' },
  warning: { bg: 'bg-warning-soft', bar: 'bg-warning', text: 'text-warning' },
  danger: { bg: 'bg-danger-soft', bar: 'bg-danger', text: 'text-danger' },
  neutral: { bg: 'bg-surface-hi', bar: 'bg-surface-hi', text: 'text-ink-strong' },
};

interface Props {
  readonly label: string;
  readonly value: string;
  readonly tone?: StatTone;
  readonly className?: string;
}

function StatComponent({ label, value, tone = 'neutral', className }: Props) {
  const s = TONE_STYLES[tone];
  return (
    <View className={cn('flex-1 overflow-hidden rounded-lg', s.bg, className)}>
      <View className={cn('h-1 w-full', s.bar)} />
      <View className="p-2.5">
        <Text
          className="text-xs font-semibold uppercase tracking-wide text-ink"
          numberOfLines={1}
        >
          {label}
        </Text>
        <Text className={cn('mt-0.5 text-base font-extrabold', s.text)} numberOfLines={1}>
          {value}
        </Text>
      </View>
    </View>
  );
}

export const Stat = memo(StatComponent);
