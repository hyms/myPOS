import React, { type Ref } from 'react';
import { Text, TextInput, View, type TextInputProps } from 'react-native';

import { cn } from '@/shared/utils/cn';
import { DARK_PALETTE } from '@/presentation/theme/tokens';

interface InputProps extends TextInputProps {
  readonly label?: string;
  readonly error?: string;
  readonly helper?: string;
  readonly rightAdornment?: React.ReactNode;
  readonly containerClassName?: string;
  readonly ref?: Ref<TextInput>;
}

export function Input({
  label,
  error,
  helper,
  rightAdornment,
  containerClassName,
  className,
  accessibilityLabel,
  ref,
  ...rest
}: InputProps) {
  const labelText = accessibilityLabel ?? label;
  return (
    <View className={cn('w-full', containerClassName)}>
      {label ? (
        <Text
          nativeID={labelText ? `${labelText}-label` : undefined}
          className="mb-1 text-sm font-semibold text-ink"
        >
          {label}
        </Text>
      ) : null}
      <View
        className={cn(
          'flex-row items-center rounded-xl border bg-surface px-3',
          error
            ? 'border-danger'
            : 'border-border-subtle',
        )}
      >
        <TextInput
          ref={ref}
          placeholderTextColor={DARK_PALETTE.inkMuted}
          accessibilityLabel={labelText}
          accessibilityLabelledBy={labelText ? `${labelText}-label` : undefined}
          accessibilityHint={helper}
          className={cn(
            'min-h-[44px] flex-1 py-2.5 text-base text-ink-strong',
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
          className="mt-1 text-xs font-medium text-danger"
        >
          {error}
        </Text>
      ) : null}
      {!error && helper ? (
        <Text className="mt-1 text-xs text-ink-muted">{helper}</Text>
      ) : null}
    </View>
  );
}
