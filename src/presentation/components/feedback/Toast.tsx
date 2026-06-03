import React from 'react';
import { Text, View } from 'react-native';

interface Props {
  readonly text1?: string;
  readonly text2?: string;
  readonly props?: { variant?: 'info' | 'success' | 'error' | 'warning'; color?: string };
}

const ICON: Record<NonNullable<Props['props']>['variant'] & string, string> = {
  info: 'ℹ️',
  success: '✅',
  error: '⛔',
  warning: '⚠️',
};

export function CustomToast({ text1, text2, props }: Props) {
  const variant = props?.variant ?? 'info';
  const color = props?.color ?? '#0ea5e9';
  return (
    <View
      style={{
        backgroundColor: '#0f172a',
        borderLeftColor: color,
        borderLeftWidth: 4,
        paddingVertical: 12,
        paddingHorizontal: 14,
        borderRadius: 12,
        minWidth: '85%',
        maxWidth: '95%',
        shadowColor: '#000',
        shadowOpacity: 0.25,
        shadowRadius: 6,
        shadowOffset: { width: 0, height: 2 },
        elevation: 6,
      }}
    >
      <View style={{ flexDirection: 'row', alignItems: 'center' }}>
        <Text style={{ marginRight: 8, fontSize: 16 }}>{ICON[variant]}</Text>
        <Text style={{ color: 'white', fontWeight: '700', fontSize: 14, flex: 1 }}>{text1}</Text>
      </View>
      {text2 ? (
        <Text style={{ color: '#cbd5e1', fontSize: 12, marginTop: 2 }}>{text2}</Text>
      ) : null}
    </View>
  );
}
