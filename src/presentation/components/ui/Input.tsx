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
  { label, error, helper, rightAdornment, containerClassName, className, ...rest },
  ref,
) {
  return (
    <View className={cn('w-full', containerClassName)}>
      {label ? <Text className="mb-1 text-sm font-medium text-surface-700 dark:text-surface-200">{label}</Text> : null}
      <View
        className={cn(
          'flex-row items-center rounded-lg border bg-white px-3 dark:bg-surface-900',
          error ? 'border-danger-500' : 'border-surface-200 dark:border-surface-700',
        )}
      >
        <TextInput
          ref={ref}
          placeholderTextColor="#94a3b8"
          className={cn(
            'flex-1 py-3 text-base text-surface-900 dark:text-surface-50',
            className,
          )}
          {...rest}
        />
        {rightAdornment}
      </View>
      {error ? <Text className="mt-1 text-xs text-danger-500">{error}</Text> : null}
      {!error && helper ? <Text className="mt-1 text-xs text-surface-500">{helper}</Text> : null}
    </View>
  );
});
