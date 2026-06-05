import React, { memo } from 'react';
import { Text, View } from 'react-native';

import { colorForId, DARK_PALETTE } from '@/presentation/theme/tokens';
import { cn } from '@/shared/utils/cn';

interface AvatarProps {
  readonly id: number;
  readonly nombre: string;
  readonly size?: number;
  readonly className?: string;
}

function getInitial(nombre: string): string {
  const trimmed = nombre.trim();
  if (trimmed.length === 0) return '?';
  return trimmed.charAt(0).toUpperCase();
}

function FallbackAvatarComponent({ id, nombre, size = 56, className }: AvatarProps) {
  const color = colorForId(id);
  const fontSize = Math.floor(size * 0.4);
  return (
    <View
      className={cn('items-center justify-center rounded-xl', className)}
      style={{ width: size, height: size, backgroundColor: color }}
    >
      <Text style={{ color: DARK_PALETTE.inkStrong, fontSize, fontWeight: '700' }}>{getInitial(nombre)}</Text>
    </View>
  );
}

export const ProductFallbackAvatar = memo(FallbackAvatarComponent);
