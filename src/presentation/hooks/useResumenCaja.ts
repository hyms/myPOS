import { useCallback, useEffect, useState } from 'react';

import { obtenerResumenCaja } from '@/application/reportes/ResumenPeriodo';
import type { ResumenCaja } from '@/domain/entities/ResumenCaja';
import { useInvalidationStore } from '@/presentation/stores/invalidationStore';

export function useResumenCaja() {
  const [resumen, setResumen] = useState<ResumenCaja>(() => obtenerResumenCaja());
  const invalidVersion = useInvalidationStore((s) => s.versions.caja);

  useEffect(() => {
    setResumen(obtenerResumenCaja());
  }, [invalidVersion]);

  const refresh = useCallback(() => {
    setResumen(obtenerResumenCaja());
  }, []);

  return { resumen, refresh };
}
