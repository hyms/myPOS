import React, { memo } from 'react';
import { Pressable, ScrollView, Text } from 'react-native';

import { Icon, type IconName } from '@/presentation/components/ui/Icon';
import { cn } from '@/shared/utils/cn';
import { DARK_PALETTE } from '@/presentation/theme/tokens';

export type TipoFilter = 'ALL' | 'VENTA' | 'COMPRA' | 'GASTO';

interface Props {
  readonly value: TipoFilter;
  readonly onChange: (value: TipoFilter) => void;
}

interface ChipOption {
  value: TipoFilter;
  label: string;
  icon: IconName;
  activeClass: string;
  activeTextClass: string;
  activeIconColor: string;
}

const OPTIONS: ReadonlyArray<ChipOption> = [
  {
    value: 'ALL',
    label: 'Todas',
    icon: 'apps',
    activeClass: 'bg-surface-lo border-border',
    activeTextClass: 'text-ink-strong',
    activeIconColor: DARK_PALETTE.inkStrong,
  },
  {
    value: 'VENTA',
    label: 'Ventas',
    icon: 'trending-up',
    activeClass: 'bg-success border-success',
    activeTextClass: 'text-white',
    activeIconColor: '#ffffff',
  },
  {
    value: 'COMPRA',
    label: 'Compras',
    icon: 'cart',
    activeClass: 'bg-warning border-warning',
    activeTextClass: 'text-white',
    activeIconColor: '#ffffff',
  },
  {
    value: 'GASTO',
    label: 'Gastos',
    icon: 'receipt',
    activeClass: 'bg-danger border-danger',
    activeTextClass: 'text-white',
    activeIconColor: '#ffffff',
  },
];

function TipoFilterChipsComponent({ value, onChange }: Props) {
  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      contentContainerClassName="gap-2 px-3 py-2"
    >
      {OPTIONS.map((opt) => {
        const active = value === opt.value;
        return (
          <Pressable
            key={opt.value}
            onPress={() => onChange(opt.value)}
            accessibilityRole="button"
            accessibilityState={{ selected: active }}
            hitSlop={4}
            className={cn(
              'min-h-[36px] flex-row items-center gap-1.5 rounded-full border px-3.5 py-1.5',
              active
                ? opt.activeClass
                : 'border-border bg-surface',
            )}
          >
            <Icon
              name={opt.icon}
              size={14}
              color={active ? opt.activeIconColor : DARK_PALETTE.inkMuted}
            />
            <Text
              className={cn(
                'text-sm font-semibold',
                active ? opt.activeTextClass : 'text-ink',
              )}
            >
              {opt.label}
            </Text>
          </Pressable>
        );
      })}
    </ScrollView>
  );
}

export const TipoFilterChips = memo(TipoFilterChipsComponent);
