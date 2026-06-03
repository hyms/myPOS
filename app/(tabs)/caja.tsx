import React, { useEffect } from 'react';
import { RefreshControl, ScrollView, Text, View } from 'react-native';

import { useResumenCaja } from '@/presentation/hooks/useResumenCaja';
import { useCurrency } from '@/presentation/hooks/useCurrency';
import { obtenerResumenAnio, obtenerResumenMes } from '@/application/reportes/ResumenPeriodo';
import { Card } from '@/presentation/components/ui/Card';
import dayjs from 'dayjs';

export default function CajaScreen() {
  const { resumen, refresh } = useResumenCaja();
  const { format } = useCurrency();
  const [mes, setMes] = React.useState(obtenerResumenMes());
  const [anio, setAnio] = React.useState(obtenerResumenAnio());

  useEffect(() => {
    const unsub = setInterval(() => {
      setMes(obtenerResumenMes());
      setAnio(obtenerResumenAnio());
    }, 60_000);
    return () => clearInterval(unsub);
  }, []);

  return (
    <ScrollView
      className="flex-1 bg-surface-50 dark:bg-surface-950"
      contentContainerClassName="gap-4 p-4"
      refreshControl={<RefreshControl refreshing={false} onRefresh={refresh} />}
    >
      <Card>
        <Text className="text-xs font-semibold uppercase text-surface-500">Saldo actual</Text>
        <Text className="mt-1 text-4xl font-bold text-primary-700">{format(resumen.saldoActual)}</Text>
        <View className="mt-3 flex-row gap-3">
          <Stat label="Ventas" value={format(resumen.totalVentas)} tone="success" />
          <Stat label="Compras" value={format(resumen.totalCompras)} tone="warning" />
          <Stat label="Gastos" value={format(resumen.totalGastos)} tone="danger" />
        </View>
      </Card>

      <Card>
        <Text className="mb-2 text-sm font-semibold text-surface-700 dark:text-surface-200">
          Resumen del mes
        </Text>
        <Text className="text-xs text-surface-500 mb-2">
          {dayjs().format('MMMM YYYY')}
        </Text>
        <Row label="Ventas" value={format(mes.totalVentas)} tone="success" />
        <Row label="Compras" value={format(mes.totalCompras)} tone="warning" />
        <Row label="Gastos" value={format(mes.totalGastos)} tone="danger" />
        <Row label="Neto del mes" value={format(mes.neto)} tone={mes.neto >= 0 ? 'success' : 'danger'} bold />
      </Card>

      <Card>
        <Text className="mb-2 text-sm font-semibold text-surface-700 dark:text-surface-200">
          Resumen del año {dayjs().format('YYYY')}
        </Text>
        <Row label="Ventas" value={format(anio.totalVentas)} tone="success" />
        <Row label="Compras" value={format(anio.totalCompras)} tone="warning" />
        <Row label="Gastos" value={format(anio.totalGastos)} tone="danger" />
        <Row label="Neto del año" value={format(anio.neto)} tone={anio.neto >= 0 ? 'success' : 'danger'} bold />
      </Card>
    </ScrollView>
  );
}

function Stat({ label, value, tone }: { label: string; value: string; tone: 'success' | 'warning' | 'danger' }) {
  const map = {
    success: { bg: 'bg-success-50', text: 'text-success-700' },
    warning: { bg: 'bg-warning-50', text: 'text-warning-600' },
    danger: { bg: 'bg-danger-50', text: 'text-danger-700' },
  } as const;
  const m = map[tone];
  return (
    <View className={`flex-1 rounded-lg p-2 ${m.bg}`}>
      <Text className="text-[10px] uppercase text-surface-500">{label}</Text>
      <Text className={`text-base font-bold ${m.text}`}>{value}</Text>
    </View>
  );
}

function Row({
  label,
  value,
  tone,
  bold,
}: {
  label: string;
  value: string;
  tone: 'success' | 'warning' | 'danger' | 'neutral';
  bold?: boolean;
}) {
  const colorMap = {
    success: 'text-success-700',
    warning: 'text-warning-600',
    danger: 'text-danger-700',
    neutral: 'text-surface-900 dark:text-surface-50',
  } as const;
  return (
    <View className="flex-row items-center justify-between border-b border-surface-100 py-2 dark:border-surface-800">
      <Text className="text-sm text-surface-600">{label}</Text>
      <Text className={`text-sm ${bold ? 'font-bold' : 'font-semibold'} ${colorMap[tone]}`}>
        {value}
      </Text>
    </View>
  );
}
