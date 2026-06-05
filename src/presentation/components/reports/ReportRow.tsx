import React, { memo } from 'react';
import { Text, View } from 'react-native';

import { cn } from '@/shared/utils/cn';
import type { StatTone } from './Stat';

const VALUE_COLOR: Readonly<Record<StatTone, string>> = {
  success: 'text-success-700',
  warning: 'text-warning-700',
  danger: 'text-danger-700',
  neutral: 'text-surface-900 dark:text-surface-50',
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
        !isLast && 'border-b border-surface-100 dark:border-surface-800',
      )}
    >
      <Text
        className={cn(
          'text-sm text-surface-700 dark:text-surface-200',
          bold ? 'font-semibold' : 'font-medium',
        )}
      >
        {label}
      </Text>
      <Text
        className={cn(
          'text-sm text-surface-900 dark:text-surface-50',
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
