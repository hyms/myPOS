import React from 'react';
import { Text, View } from 'react-native';

interface EmptyStateProps {
  readonly title: string;
  readonly description?: string;
  readonly icon?: React.ReactNode;
}

export function EmptyState({ title, description, icon }: EmptyStateProps) {
  return (
    <View className="flex-1 items-center justify-center px-6 py-12">
      {icon ? <View className="mb-3">{icon}</View> : null}
      <Text className="text-center text-lg font-semibold text-surface-700 dark:text-surface-200">{title}</Text>
      {description ? (
        <Text className="mt-1 text-center text-sm text-surface-500">{description}</Text>
      ) : null}
    </View>
  );
}
