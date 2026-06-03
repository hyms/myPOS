import dayjs from 'dayjs';

import type { CurrencyCode } from '@/domain/value-objects/Currency';
import { CURRENCY_SYMBOLS } from '@/domain/value-objects/Currency';
import type { TipoTransaccion } from '@/domain/entities/Transaccion';
import type { CarritoItem } from '@/domain/entities/CarritoItem';
import { calcTotals } from '@/domain/rules/cartRules';

const TITULOS: Record<TipoTransaccion, string> = {
  VENTA: 'COMPROBANTE DE VENTA',
  COMPRA: 'COMPROBANTE DE COMPRA',
  GASTO: 'COMPROBANTE DE GASTO',
};

const LEYENDAS: Record<TipoTransaccion, string> = {
  VENTA: 'Gracias por su compra.',
  COMPRA: 'Compra registrada en inventario.',
  GASTO: 'Gasto registrado en caja.',
};

export interface ComprobanteVentaData {
  readonly tipo: 'VENTA';
  readonly transaccionId: number;
  readonly items: ReadonlyArray<CarritoItem>;
  readonly tipoPago: string;
  readonly recibido: number | null;
  readonly cambio: number | null;
  readonly currency: CurrencyCode;
}

export interface ComprobanteCompraData {
  readonly tipo: 'COMPRA';
  readonly transaccionId: number;
  readonly items: ReadonlyArray<CarritoItem>;
  readonly tipoPago: string;
  readonly currency: CurrencyCode;
}

export interface ComprobanteGastoData {
  readonly tipo: 'GASTO';
  readonly transaccionId: number;
  readonly monto: number;
  readonly descripcion: string;
  readonly tipoPago: string;
  readonly currency: CurrencyCode;
}

export type ComprobanteData = ComprobanteVentaData | ComprobanteCompraData | ComprobanteGastoData;

export function buildComprobanteHTML(data: ComprobanteData): string {
  const now = dayjs().format('DD/MM/YYYY HH:mm');
  const symbol = CURRENCY_SYMBOLS[data.currency];
  const fmt = (n: number) => `${symbol} ${n.toFixed(2)}`;

  if (data.tipo === 'GASTO') {
    return `
<!DOCTYPE html>
<html><head><meta charset="utf-8"><style>${baseStyles()}</style></head>
<body><div class="receipt">
  <h1>${TITULOS.GASTO}</h1>
  <p class="muted">#${data.transaccionId} · ${now}</p>
  <hr/>
  <p><strong>Descripción:</strong> ${escapeHTML(data.descripcion)}</p>
  <p><strong>Tipo de pago:</strong> ${data.tipoPago}</p>
  <hr/>
  <p class="total">TOTAL: ${fmt(data.monto)}</p>
  <p class="footer">${LEYENDAS.GASTO}</p>
</div></body></html>`;
  }

  const totals = calcTotals(data.items, data.tipo);
  const itemsHTML = data.items
    .map((it) => {
      const unit = data.tipo === 'VENTA' ? it.producto.precioVenta : it.producto.precioCompra;
      return `<tr>
        <td>${escapeHTML(it.producto.nombre)}</td>
        <td>${it.cantidad}</td>
        <td>${fmt(unit)}</td>
        <td>${fmt(unit * it.cantidad)}</td>
      </tr>`;
    })
    .join('');

  const recibidoPago = data.tipo === 'VENTA' && data.recibido !== null
    ? `<p><strong>Recibido:</strong> ${fmt(data.recibido)}</p>
       <p><strong>Cambio:</strong> ${data.cambio !== null ? fmt(data.cambio) : '-'}</p>`
    : '';

  return `
<!DOCTYPE html>
<html><head><meta charset="utf-8"><style>${baseStyles()}</style></head>
<body><div class="receipt">
  <h1>${TITULOS[data.tipo]}</h1>
  <p class="muted">#${data.transaccionId} · ${now}</p>
  <hr/>
  <table>
    <thead><tr><th>Producto</th><th>Cant.</th><th>Precio</th><th>Subtotal</th></tr></thead>
    <tbody>${itemsHTML}</tbody>
  </table>
  <hr/>
  <p><strong>Subtotal:</strong> ${fmt(totals.subtotal)}</p>
  <p><strong>Descuento:</strong> ${fmt(totals.descuento)}</p>
  <p class="total">TOTAL: ${fmt(totals.total)}</p>
  <p><strong>Tipo de pago:</strong> ${data.tipoPago}</p>
  ${recibidoPago}
  <p class="footer">${LEYENDAS[data.tipo]}</p>
</div></body></html>`;
}

function baseStyles(): string {
  return `
    body { font-family: -apple-system, Roboto, sans-serif; color: #0f172a; padding: 16px; }
    .receipt { max-width: 360px; margin: 0 auto; }
    h1 { font-size: 18px; text-align: center; margin: 0 0 4px; }
    .muted { color: #64748b; font-size: 12px; text-align: center; margin: 0 0 8px; }
    hr { border: none; border-top: 1px dashed #cbd5e1; margin: 8px 0; }
    table { width: 100%; font-size: 12px; border-collapse: collapse; }
    th, td { padding: 4px 2px; text-align: left; }
    th { border-bottom: 1px solid #cbd5e1; }
    .total { font-size: 16px; font-weight: 700; text-align: right; margin: 8px 0 0; }
    .footer { text-align: center; color: #64748b; font-size: 11px; margin-top: 12px; }
  `;
}

function escapeHTML(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
