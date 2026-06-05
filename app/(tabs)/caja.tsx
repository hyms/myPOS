import React, { useCallback, useEffect, useState } from 'react';
import { RefreshControl, ScrollView, Text, View } from 'react-native';
import dayjs from 'dayjs';

import { useResumenCaja } from '@/presentation/hooks/useResumenCaja';
import { useCurrency } from '@/presentation/hooks/useCurrency';
import {
  obtenerResumenAnio,
  obtenerResumenMes,
} from '@/application/reportes/ResumenPeriodo';
import { Card } from '@/presentation/components/ui/Card';
import { Icon } from '@/presentation/components/ui/Icon';
import { Skeleton } from '@/presentation/components/ui/Skeleton';
import { Stat, type ResumenTone } from '@/presentation/components/reports/Stat';
import { ReportRow } from '@/presentation/components/reports/ReportRow';
import { Breakdown } from '@/presentation/components/reports/BreakdownBar';
import type { ResumenPeriodo } from '@/application/reportes/ResumenPeriodo';
import { cn } from '@/shared/utils/cn';
import { DARK_PALETTE } from '@/presentation/theme/tokens';

function SkeletonCaja() {
  return (
    <View className="gap-4">
      <Card>
        <Skeleton className="mb-2 h-3 w-20" />
        <Skeleton className="mb-3 h-10 w-40" />
        <View className="flex-row gap-3">
          <Skeleton className="h-14 flex-1 rounded-lg" />
          <Skeleton className="h-14 flex-1 rounded-lg" />
          <Skeleton className="h-14 flex-1 rounded-lg" />
        </View>
      </Card>
      <Card>
        <Skeleton className="mb-3 h-4 w-32" />
        <View className="gap-3">
          <View><Skeleton className="mb-1 h-3 w-24" /><Skeleton className="h-1.5 w-full" /></View>
          <View><Skeleton className="mb-1 h-3 w-24" /><Skeleton className="h-1.5 w-full" /></View>
          <View><Skeleton className="mb-1 h-3 w-24" /><Skeleton className="h-1.5 w-full" /></View>
        </View>
      </Card>
    </View>
  );
}

export default function CajaScreen() {
  const { resumen, refresh } = useResumenCaja();
  const { format } = useCurrency();
  const [mes, setMes] = useState<ResumenPeriodo>(obtenerResumenMes());
  const [anio, setAnio] = useState<ResumenPeriodo>(obtenerResumenAnio());
  const [refreshing, setRefreshing] = useState(false);

  useEffect(() => {
    const t = setInterval(() => {
      setMes(obtenerResumenMes());
      setAnio(obtenerResumenAnio());
    }, 60_000);
    return () => clearInterval(t);
  }, []);

  const handleRefresh = useCallback(async () => {
    setRefreshing(true);
    refresh();
    setMes(obtenerResumenMes());
    setAnio(obtenerResumenAnio());
    setTimeout(() => setRefreshing(false), 400);
  }, [refresh]);

  const isInitialLoad =
    resumen.totalVentas === 0 &&
    resumen.totalCompras === 0 &&
    resumen.totalGastos === 0 &&
    resumen.saldoActual === 0;

  const saldoTone: ResumenTone =
    resumen.saldoActual > 0
      ? 'success'
      : resumen.saldoActual < 0
        ? 'danger'
        : 'neutral';

  if (isInitialLoad) {
    return (
      <ScrollView
        className="flex-1 bg-surface"
        contentContainerClassName="gap-4 p-4"
      >
        <SkeletonCaja />
      </ScrollView>
    );
  }

  const saldoBgClass =
    saldoTone === 'success'
      ? 'bg-success-soft'
      : saldoTone === 'danger'
        ? 'bg-danger-soft'
        : 'bg-surface-hi';

  const saldoAccentClass =
    saldoTone === 'success'
      ? 'bg-success'
      : saldoTone === 'danger'
        ? 'bg-danger'
        : 'bg-accent';

  const saldoTextClass =
    saldoTone === 'success'
      ? 'text-success'
      : saldoTone === 'danger'
        ? 'text-danger'
        : 'text-ink-strong';

  return (
    <ScrollView
      className="flex-1 bg-surface"
      contentContainerClassName="gap-4 p-4 pb-8"
      refreshControl={
        <RefreshControl
          refreshing={refreshing}
          onRefresh={handleRefresh}
          tintColor={DARK_PALETTE.accentBright}
          colors={[DARK_PALETTE.accentBright]}
        />
      }
    >
      <View
        className={cn(
          'overflow-hidden rounded-3xl border border-border-subtle',
          saldoBgClass,
        )}
      >
        <View className={cn('h-1.5 w-full', saldoAccentClass)} />
        <View className="p-5">
          <View className="flex-row items-center gap-2.5">
            <View
              className={cn(
                'rounded-xl p-2.5',
                saldoTone === 'success'
                  ? 'bg-success-soft'
                  : saldoTone === 'danger'
                    ? 'bg-danger-soft'
                    : 'bg-surface-lo',
              )}
            >
              <Icon
                name="wallet"
                size={22}
                color={
                  saldoTone === 'success'
                    ? DARK_PALETTE.success
                    : saldoTone === 'danger'
                      ? DARK_PALETTE.danger
                      : DARK_PALETTE.inkMuted
                }
              />
            </View>
            <Text className="text-sm font-bold uppercase tracking-widest text-ink-muted">
              Saldo actual
            </Text>
          </View>
          <Text
            accessibilityLabel={`Saldo actual: ${format(resumen.saldoActual)}`}
            className={cn(
              'mt-3 text-5xl font-extrabold tabular-nums leading-tight',
              saldoTextClass,
            )}
            selectable
          >
            {format(resumen.saldoActual)}
          </Text>
          <View className="mt-5 flex-row gap-2.5">
            <Stat label="Ventas" value={format(resumen.totalVentas)} tone="success" />
            <Stat label="Compras" value={format(resumen.totalCompras)} tone="warning" />
            <Stat label="Gastos" value={format(resumen.totalGastos)} tone="danger" />
          </View>
        </View>
      </View>

      <Card>
        <View className="mb-3 flex-row items-center gap-2">
          <Icon name="calendar" size={16} color={DARK_PALETTE.inkMuted} />
          <Text className="text-sm font-semibold text-ink">Resumen del mes</Text>
          <Text className="ml-auto text-xs capitalize text-ink-muted">
            {dayjs().format('MMMM YYYY')}
          </Text>
        </View>
        <Breakdown
          items={[
            { label: 'Ventas', value: mes.totalVentas, tone: 'success' },
            { label: 'Compras', value: mes.totalCompras, tone: 'warning' },
            { label: 'Gastos', value: mes.totalGastos, tone: 'danger' },
          ]}
        />
        <View className="mt-3 border-t border-border-subtle pt-3">
          <ReportRow
            label="Neto del mes"
            value={format(mes.neto)}
            tone={mes.neto >= 0 ? 'success' : 'danger'}
            bold
          />
        </View>
      </Card>

      <Card>
        <View className="mb-3 flex-row items-center gap-2">
          <Icon name="calendar" size={16} color={DARK_PALETTE.inkMuted} />
          <Text className="text-sm font-semibold text-ink">Resumen del año</Text>
          <Text className="ml-auto text-xs text-ink-muted">{dayjs().format('YYYY')}</Text>
        </View>
        <Breakdown
          items={[
            { label: 'Ventas', value: anio.totalVentas, tone: 'success' },
            { label: 'Compras', value: anio.totalCompras, tone: 'warning' },
            { label: 'Gastos', value: anio.totalGastos, tone: 'danger' },
          ]}
        />
        <View className="mt-3 border-t border-border-subtle pt-3">
          <ReportRow
            label="Neto del año"
            value={format(anio.neto)}
            tone={anio.neto >= 0 ? 'success' : 'danger'}
            bold
          />
        </View>
      </Card>
    </ScrollView>
  );
}
