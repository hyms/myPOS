import React, { memo } from 'react';
import { Text, View } from 'react-native';

import { cn } from '@/shared/utils/cn';
import { TONE_STYLES, type ResumenTone } from '@/presentation/components/ui/SemanticTone';

interface Props {
  readonly label: string;
  readonly value: string;
  readonly tone?: ResumenTone;
  readonly bold?: boolean;
  readonly isLast?: boolean;
}

function RowComponent({ label, value, tone = 'neutral', bold, isLast }: Props) {
  const s = TONE_STYLES[tone];
  return (
    <View
      className={cn(
        'flex-row items-center justify-between py-2.5',
        !isLast && 'border-b border-border-subtle',
      )}
    >
      <Text
        className={cn(
          'text-sm text-ink',
          bold ? 'font-semibold' : 'font-medium',
        )}
      >
        {label}
      </Text>
      <Text
        className={cn(
          'text-sm font-bold tabular-nums',
          bold && 'font-extrabold',
          s.text,
        )}
      >
        {value}
      </Text>
    </View>
  );
}

export const ReportRow = memo(RowComponent);
