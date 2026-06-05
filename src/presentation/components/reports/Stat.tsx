import React, { memo } from 'react';
import { Text, View } from 'react-native';

import { cn } from '@/shared/utils/cn';
import { TONE_STYLES, type ResumenTone } from '@/presentation/components/ui/SemanticTone';

export type { ResumenTone } from '@/presentation/components/ui/SemanticTone';

interface Props {
  readonly label: string;
  readonly value: string;
  readonly tone?: ResumenTone;
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
