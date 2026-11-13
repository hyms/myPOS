import { getDatabase } from '@/data/database/client';
import type { TipoPago } from '@/domain/entities/Transaccion';

export interface RegistrarGastoInput {
  readonly monto: number;
  readonly descripcion: string;
  readonly tipoPago: TipoPago;
}

export interface RegistrarGastoResult {
  readonly transaccionId: number;
}

export function registrarGasto(input: RegistrarGastoInput): RegistrarGastoResult {
  if (!Number.isFinite(input.monto) || input.monto <= 0) {
    throw new Error('El monto del gasto debe ser mayor a 0.');
  }
  const detalle = input.descripcion.trim();
  if (detalle.length === 0) {
    throw new Error('La descripción del gasto es obligatoria.');
  }

  const db = getDatabase();
  let transaccionId = 0;
  db.withTransactionSync(() => {
    const result = db.runSync(
      "INSERT INTO sales_transactions (type, total_amount, payment_method, detail) VALUES ('GASTO', ?, ?, ?)",
      input.monto,
      input.tipoPago,
      detalle,
    );
    transaccionId = result.lastInsertRowId;
    db.runSync(
      'UPDATE financial_accumulators SET total_expenses = total_expenses + ?, net_balance = net_balance - ?, updated_at = CURRENT_TIMESTAMP WHERE id = 1',
      input.monto,
      input.monto,
    );
  });

  return { transaccionId };
}
