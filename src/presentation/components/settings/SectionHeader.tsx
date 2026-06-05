import React, { memo } from 'react';
import { Text, View } from 'react-native';

interface SectionHeaderProps {
  readonly title: string;
  readonly description?: string;
}

function SectionHeaderComponent({ title, description }: SectionHeaderProps) {
  return (
    <View className="mb-2 mt-2">
      <Text className="text-sm font-semibold uppercase tracking-wide text-primary-700">{title}</Text>
      {description ? <Text className="mt-1 text-xs text-surface-500">{description}</Text> : null}
    </View>
  );
}

export const SectionHeader = memo(SectionHeaderComponent);
