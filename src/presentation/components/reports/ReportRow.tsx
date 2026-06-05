import React, { memo } from 'react';
import { Text, View } from 'react-native';

import { cn } from '@/shared/utils/cn';
import type { StatTone } from './Stat';

const VALUE_COLOR: Readonly<Record<StatTone, string>> = {
  success: 'text-success',
  warning: 'text-warning',
  danger: 'text-danger',
  neutral: 'text-ink-strong',
};

interface Props {
  readonly label: string;
  readonly value: string;
  readonly tone?: StatTone;
  readonly bold?: boolean;
  readonly isLast?: boolean;
}

function RowComponent({ label, value, tone = 'neutral', bold, isLast }: Props) {
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
          'text-sm text-ink-strong',
          bold ? 'font-extrabold' : 'font-bold',
          VALUE_COLOR[tone],
        )}
      >
        {value}
      </Text>
    </View>
  );
}

export const ReportRow = memo(RowComponent);
