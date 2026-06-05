import React from 'react';
import { Ionicons } from '@expo/vector-icons';

import { DARK_PALETTE } from '@/presentation/theme/tokens';

export type IconName = keyof typeof Ionicons.glyphMap;

interface IconProps {
  readonly name: IconName;
  readonly size?: number;
  readonly color?: string;
  readonly className?: string;
}

export function Icon({ name, size = 24, color = DARK_PALETTE.inkMuted, className }: IconProps) {
  return <Ionicons name={name} size={size} color={color} className={className} />;
}
