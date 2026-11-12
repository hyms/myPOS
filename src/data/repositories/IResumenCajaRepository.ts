import type { ResumenCaja } from '@/domain/entities/ResumenCaja';

export interface IResumenCajaRepository {
  obtener(): ResumenCaja;
  aplicarDelta(input: {
    readonly salesDelta?: number;
    readonly purchasesDelta?: number;
    readonly expensesDelta?: number;
  }): ResumenCaja;
}
