import React from 'react';
import { Text, View } from 'react-native';
import { Icon, type IconName } from '@/presentation/components/ui/Icon';
import { cn } from '@/shared/utils/cn';

interface EmptyStateProps {
  readonly title: string;
  readonly description?: string;
  readonly icon?: IconName;
  readonly className?: string;
}

export function EmptyState({ title, description, icon, className }: EmptyStateProps) {
  return (
    <View className={cn('flex-1 items-center justify-center px-6 py-12', className)}>
      {icon ? (
        <View className="mb-4 rounded-full bg-surface-100 p-4 dark:bg-surface-800">
          <Icon name={icon} size={32} color="#94a3b8" />
        </View>
      ) : null}
      <Text className="text-center text-lg font-semibold text-surface-700 dark:text-surface-200">{title}</Text>
      {description ? (
        <Text className="mt-1 text-center text-sm text-surface-500">{description}</Text>
      ) : null}
    </View>
  );
}
