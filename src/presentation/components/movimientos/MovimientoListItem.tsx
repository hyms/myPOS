import React, { memo } from 'react';
import { Text, View } from 'react-native';

import { AnimatedPressable } from '@/presentation/components/ui/AnimatedPressable';
import { Icon, type IconName } from '@/presentation/components/ui/Icon';
import type { Transaccion, TipoTransaccion, TipoPago } from '@/domain/entities/Transaccion';
import { formatFechaCorta } from '@/shared/utils/date';
import { cn } from '@/shared/utils/cn';
import { DARK_PALETTE } from '@/presentation/theme/tokens';
import { TONE_STYLES, type SemanticTone } from '@/presentation/components/ui/SemanticTone';

const TIPO_TONE: Readonly<Record<TipoTransaccion, Extract<SemanticTone, 'success' | 'warning' | 'danger'>>> = {
  VENTA: 'success',
  COMPRA: 'warning',
  GASTO: 'danger',
};

const TIPO_SIGN: Readonly<Record<TipoTransaccion, '+' | '-'>> = {
  VENTA: '+',
  COMPRA: '-',
  GASTO: '-',
};

const TIPO_ICON: Readonly<Record<TipoTransaccion, IconName>> = {
  VENTA: 'trending-up',
  COMPRA: 'cart',
  GASTO: 'receipt',
};

const TIPO_LABEL: Readonly<Record<TipoTransaccion, string>> = {
  VENTA: 'Venta',
  COMPRA: 'Compra',
  GASTO: 'Gasto',
};

const PAGO_ICON: Readonly<Record<TipoPago, IconName>> = {
  EFECTIVO: 'cash-outline',
  TARJETA: 'card-outline',
  TRANSFERENCIA: 'swap-horizontal-outline',
  QR: 'qr-code-outline',
};

const PAGO_LABEL: Readonly<Record<TipoPago, string>> = {
  EFECTIVO: 'Efect',
  TARJETA: 'Tarj',
  TRANSFERENCIA: 'Transf',
  QR: 'QR',
};

interface Props {
  readonly item: Transaccion;
  readonly detailCount?: number;
  readonly onPress: (id: number) => void;
  readonly format: (amount: number) => string;
}

function MovimientoListItemComponent({ item, detailCount, onPress, format }: Props) {
  const tone = TIPO_TONE[item.tipo];
  const s = TONE_STYLES[tone];
  const detalle =
    item.detalle ??
    (detailCount && detailCount > 0
      ? `${detailCount} ${detailCount === 1 ? 'producto' : 'productos'}`
      : null);

  return (
    <AnimatedPressable
      onPress={() => onPress(item.id)}
      accessibilityRole="button"
      accessibilityLabel={`${TIPO_LABEL[item.tipo]} de ${format(item.montoTotal)}${detalle ? `, ${detalle}` : ''}`}
      className="mx-3 my-1.5 overflow-hidden rounded-2xl border border-border-subtle bg-surface"
    >
      <View className={cn('h-1 w-full', s.bar)} />
      <View className="flex-row items-center justify-between p-3.5">
        <View className="flex-1 pr-2">
          <View className="flex-row items-center gap-2">
            <View className={cn('rounded-md p-1', s.soft)}>
              <Icon name={TIPO_ICON[item.tipo]} size={14} color={s.hex} />
            </View>
            <Text className={cn('text-xs font-bold uppercase tracking-wide', s.text)}>
              {TIPO_LABEL[item.tipo]}
            </Text>
            <View className="flex-row items-center gap-1">
              <Icon name={PAGO_ICON[item.tipoPago]} size={11} color={DARK_PALETTE.inkMuted} />
              <Text className="text-[10px] font-medium text-ink-muted">
                {PAGO_LABEL[item.tipoPago]}
              </Text>
            </View>
          </View>
          {detalle ? (
            <Text
              className="mt-1 text-sm font-semibold text-ink-strong"
              numberOfLines={1}
            >
              {detalle}
            </Text>
          ) : null}
          <Text
            className={cn(
              'text-xs tabular-nums text-ink-muted',
              detalle ? 'mt-0.5' : 'mt-1.5',
            )}
          >
            {formatFechaCorta(item.fechaRegistro)}
          </Text>
        </View>
        <View className="ml-2 flex-row items-center gap-1.5">
          <Text className={cn('text-lg font-extrabold tabular-nums', s.text)}>
            {TIPO_SIGN[item.tipo]}{format(Math.abs(item.montoTotal))}
          </Text>
          <Icon name="chevron-forward" size={14} color={DARK_PALETTE.inkFaint} />
        </View>
      </View>
    </AnimatedPressable>
  );
}

export const MovimientoListItem = memo(MovimientoListItemComponent);
