import { useCallback, useEffect, useState } from 'react';

import { obtenerResumenCaja } from '@/application/reportes/ResumenPeriodo';
import type { ResumenCaja } from '@/domain/entities/ResumenCaja';

export function useResumenCaja() {
  const [resumen, setResumen] = useState<ResumenCaja>({
    totalVentas: 0,
    totalCompras: 0,
    totalGastos: 0,
    saldoActual: 0,
  });

  const refresh = useCallback(() => {
    setResumen(obtenerResumenCaja());
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  return { resumen, refresh };
}
