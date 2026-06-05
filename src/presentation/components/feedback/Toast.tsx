import React from 'react';
import { Text, View } from 'react-native';
import { Icon } from '@/presentation/components/ui/Icon';
import type { IconName } from '@/presentation/components/ui/Icon';

interface Props {
  readonly text1?: string;
  readonly text2?: string;
  readonly props?: { variant?: 'info' | 'success' | 'error' | 'warning'; color?: string };
}

const ICON_MAP: Record<'info' | 'success' | 'error' | 'warning', { name: IconName; color: string }> = {
  info: { name: 'information-circle', color: '#8b5cf6' },
  success: { name: 'checkmark-circle', color: '#22c55e' },
  error: { name: 'alert-circle', color: '#ef4444' },
  warning: { name: 'warning', color: '#f59e0b' },
};

export function CustomToast({ text1, text2, props }: Props) {
  const variant = props?.variant ?? 'info';
  const iconConfig = ICON_MAP[variant];
  const color = props?.color ?? iconConfig.color;
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
        <View style={{ marginRight: 8 }}>
          <Icon name={iconConfig.name} size={18} color={iconConfig.color} />
        </View>
        <Text style={{ color: 'white', fontWeight: '700', fontSize: 14, flex: 1 }}>{text1}</Text>
      </View>
      {text2 ? (
        <Text style={{ color: '#cbd5e1', fontSize: 12, marginTop: 2 }}>{text2}</Text>
      ) : null}
    </View>
  );
}
