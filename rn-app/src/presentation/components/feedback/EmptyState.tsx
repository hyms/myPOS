import React from 'react';
import { Text, View } from 'react-native';

import { Icon } from '@/presentation/components/ui/Icon';
import type { IconName } from '@/presentation/components/ui/Icon';
import { DARK_PALETTE } from '@/presentation/theme/tokens';

interface Props {
  readonly icon: IconName;
  readonly title: string;
  readonly description?: string;
  readonly action?: React.ReactNode;
}

export function EmptyState({ icon, title, description, action }: Props) {
  return (
    <View
      accessibilityRole="summary"
      className="flex-1 items-center justify-center px-8 py-12"
    >
      <View
        accessibilityElementsHidden
        importantForAccessibility="no"
        className="mb-3 rounded-full bg-surface-hi p-4"
      >
        <Icon name={icon} size={32} color={DARK_PALETTE.inkMuted} />
      </View>
      <Text
        accessibilityRole="header"
        className="text-base font-bold text-ink-strong"
      >
        {title}
      </Text>
      {description ? (
        <Text className="mt-1 text-center text-sm text-ink-muted">{description}</Text>
      ) : null}
      {action ? <View className="mt-4">{action}</View> : null}
    </View>
  );
}
