import React, { forwardRef } from 'react';
import { Text, TextInput, View, type TextInputProps } from 'react-native';

import { cn } from '@/shared/utils/cn';

interface InputProps extends TextInputProps {
  readonly label?: string;
  readonly error?: string;
  readonly helper?: string;
  readonly rightAdornment?: React.ReactNode;
  readonly containerClassName?: string;
}

export const Input = forwardRef<TextInput, InputProps>(function Input(
  { label, error, helper, rightAdornment, containerClassName, className, accessibilityLabel, ...rest },
  ref,
) {
  const labelText = accessibilityLabel ?? label;
  return (
    <View className={cn('w-full', containerClassName)}>
      {label ? (
        <Text
          nativeID={labelText ? `${labelText}-label` : undefined}
          className="mb-1 text-sm font-semibold text-surface-700 dark:text-surface-200"
        >
          {label}
        </Text>
      ) : null}
      <View
        className={cn(
          'flex-row items-center rounded-xl border bg-white px-3 dark:bg-surface-900',
          error
            ? 'border-danger-500 bg-danger-50/30 dark:bg-danger-950/30'
            : 'border-surface-200 dark:border-surface-700',
        )}
      >
        <TextInput
          ref={ref}
          placeholderTextColor="#94a3b8"
          accessibilityLabel={labelText}
          accessibilityLabelledBy={labelText ? `${labelText}-label` : undefined}
          accessibilityHint={helper}
          className={cn(
            'min-h-[44px] flex-1 py-2.5 text-base text-surface-900 dark:text-surface-50',
            className,
          )}
          {...rest}
        />
        {rightAdornment}
      </View>
      {error ? (
        <Text
          accessibilityLiveRegion="polite"
          accessibilityRole="alert"
          className="mt-1 text-xs font-medium text-danger-600"
        >
          {error}
        </Text>
      ) : null}
      {!error && helper ? (
        <Text className="mt-1 text-xs text-surface-500">{helper}</Text>
      ) : null}
    </View>
  );
});
