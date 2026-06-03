import type { ResumenCaja } from '@/domain/entities/ResumenCaja';

export interface IResumenCajaRepository {
  obtener(): ResumenCaja;
}
