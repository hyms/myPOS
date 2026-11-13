import { getRepositories } from '@/data/repositories/container';
import type { TipoTransaccion } from '@/domain/entities/Transaccion';
import type { ResumenCaja } from '@/domain/entities/ResumenCaja';

export interface ResumenPeriodo {
  readonly periodo: 'MONTH' | 'YEAR';
  readonly totales: ReadonlyArray<{ tipo: TipoTransaccion; total: number }>;
  readonly totalVentas: number;
  readonly totalGastos: number;
  readonly totalCompras: number;
  readonly neto: number;
}

export function obtenerResumenCaja(): ResumenCaja {
  return getRepositories().resumenCaja.obtener();
}

export function obtenerResumenMes(): ResumenPeriodo {
  const totales = getRepositories().transacciones.listarPorMesAnio({ periodo: 'MONTH' });
  return buildResumen('MONTH', totales);
}

export function obtenerResumenAnio(): ResumenPeriodo {
  const totales = getRepositories().transacciones.listarPorMesAnio({ periodo: 'YEAR' });
  return buildResumen('YEAR', totales);
}

function buildResumen(
  periodo: 'MONTH' | 'YEAR',
  totales: ReadonlyArray<{ tipo: TipoTransaccion; total: number }>,
): ResumenPeriodo {
  const ventas = totales.find((t) => t.tipo === 'VENTA')?.total ?? 0;
  const compras = totales.find((t) => t.tipo === 'COMPRA')?.total ?? 0;
  const gastos = totales.find((t) => t.tipo === 'GASTO')?.total ?? 0;
  return {
    periodo,
    totales,
    totalVentas: ventas,
    totalCompras: compras,
    totalGastos: gastos,
    neto: ventas - compras - gastos,
  };
}
