import React, { memo, type ReactNode } from 'react';
import { Text, View } from 'react-native';

import { Card } from '@/presentation/components/ui/Card';
import { cn } from '@/shared/utils/cn';

interface Props {
  readonly title: string;
  readonly subtitle?: string;
  readonly children: ReactNode;
  readonly className?: string;
}

function ReportCardComponent({ title, subtitle, children, className }: Props) {
  return (
    <Card className={cn('gap-1', className)}>
      <Text className="text-sm font-semibold uppercase tracking-wide text-ink-strong">
        {title}
      </Text>
      {subtitle ? <Text className="text-xs text-ink-muted">{subtitle}</Text> : null}
      <View className="mt-1">{children}</View>
    </Card>
  );
}

export const ReportCard = memo(ReportCardComponent);
