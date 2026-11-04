# Sales & Checkout — Specification (v2.0)

## 1. Bounded Context
Carrito de compras, registro de ventas, métodos de pago, cálculo de cambio e impresión/compartición de comprobantes.

## 2. Domain Entities & Value Objects

### 2.1 CarritoItem
**Archivo:** `src/domain/entities/CarritoItem.ts`

```ts
export type CarritoTipo = 'VENTA' | 'COMPRA';

export interface CarritoItem {
  readonly producto: Pick<
    Producto,
    'id' | 'nombre' | 'precioVenta' | 'precioCompra' | 'stockActual' | 'imagenUri'
  >;
  readonly cantidad: number;
  readonly descuento: number;
}
```

### 2.2 Transaccion
**Archivo:** `src/domain/entities/Transaccion.ts`

```ts
export type TipoTransaccion = 'VENTA' | 'COMPRA' | 'GASTO';
export type TipoPago = 'EFECTIVO' | 'TARJETA' | 'TRANSFERENCIA' | 'QR';

export interface Transaccion {
  readonly id: number;
  readonly tipo: TipoTransaccion;
  readonly montoTotal: number;
  readonly tipoPago: TipoPago;
  readonly detalle: string | null;
  readonly fechaRegistro: string;
  readonly fechaDelete: string | null;
}
```

### 2.3 DetalleTransaccion
**Archivo:** `src/domain/entities/DetalleTransaccion.ts`

```ts
export interface DetalleTransaccion {
  readonly id: number;
  readonly transaccionId: number;
  readonly productoId: number;
  readonly cantidad: number;
  readonly precioUnitario: number;
}
```

### 2.4 Value Objects (Money / Currency)
**Archivos:** `src/domain/value-objects/Money.ts`, `src/domain/value-objects/Currency.ts`

- `Currency`: `Bs` \| `$` \| `€` — solo formato visual.
- `Money`: monto numérico en moneda base; `Intl.NumberFormat` para presentación.

## 3. Domain Rules & Invariants

| ID | Regla | Descripción |
|---|---|---|
| RULE-SC-01 | Stock no negativo | `stockActual - cantidad >= 0` antes de cualquier `VENTA`. `validateStockDisponible` lanza `StockError` si falla. |
| RULE-SC-02 | Descuento ≤ subtotal | `applyDiscount`: si descuento > subtotal → total 0; si < 0 → sin descuento. |
| RULE-SC-03 | Pago único | Un solo `tipoPago` por transacción. No se modela pago mixto (INV-04). |
| RULE-SC-04 | Cambio en efectivo | `cambio = recibido - total` solo si `tipoPago === 'EFECTIVO'`. Verde si ≥ 0, rojo si negativo. |
| RULE-SC-05 | Totales redondeados | `round2(v) = Math.round(v * 100) / 100` en subtotal, descuento, total y cambio. |

Invariantes globales: **INV-01, INV-03, INV-04, INV-10**.

## 4. Domain Rules (cartRules)

**Archivo:** `src/domain/rules/cartRules.ts`

| Función | Firma | Descripción |
|---|---|---|
| `getUnitPrice` | `(item, tipo) => number` | `precioVenta` si VENTA, `precioCompra` si COMPRA. |
| `lineSubtotal` | `(item, tipo) => number` | `unitPrice * cantidad`. |
| `calcTotals` | `(items, tipo) => CartTotals` | `{ subtotal, descuento, total }` redondeados. |
| `calcChange` | `(total, recibido) => number` | `recibido - total` redondeado. |
| `applyDiscount` | `(subtotal, descuento) => number` | Clamp [0, subtotal]. |

## 5. Database Contract

```sql
-- Ver schema.sql.ts completo en src/data/database/schema.sql.ts
CREATE INDEX IF NOT EXISTS idx_transacciones_fecha ON transacciones(fecha_registro) WHERE fecha_delete IS NULL;
CREATE INDEX IF NOT EXISTS idx_transacciones_tipo ON transacciones(tipo) WHERE fecha_delete IS NULL;
CREATE INDEX IF NOT EXISTS idx_detalle_trx ON detalle_transacciones(transaccion_id);
CREATE INDEX IF NOT EXISTS idx_detalle_producto ON detalle_transacciones(producto_id);
```

## 6. Repository Interface (`ITransaccionRepository`)

**Archivo:** `src/data/repositories/ITransaccionRepository.ts`

```ts
export interface CreateTransaccionInput {
  readonly tipo: TipoTransaccion;
  readonly montoTotal: number;
  readonly tipoPago: TipoPago;
  readonly detalle: string | null;
}

export interface CreateDetalleInput {
  readonly productoId: number;
  readonly cantidad: number;
  readonly precioUnitario: number;
}

export interface ITransaccionRepository {
  findById(id: number): Transaccion | null;
  findDetalles(id: number): ReadonlyArray<DetalleTransaccion>;
  listar(options: { tipo?: TipoTransaccion; limit: number; offset: number }): ReadonlyArray<Transaccion>;
  listarPorMesAnio(options: { periodo: 'MONTH' | 'YEAR' }): ReadonlyArray<{ tipo: TipoTransaccion; total: number }>;
  gastosDelMes(limit: number, offset: number): ReadonlyArray<Transaccion>;
}
```

## 7. Use Case (`registrarVenta`)

**Archivo:** `src/application/ventas/RegistrarVenta.ts`

```ts
export interface RegistrarVentaInput {
  readonly items: ReadonlyArray<CarritoItem>;
  readonly tipoPago: TipoPago;
  readonly detalle: string | null;
}

export interface RegistrarVentaResult {
  readonly transaccionId: number;
  readonly total: number;
  readonly stockAlerts: ReadonlyArray<{
    id: number;
    nombre: string;
    stockActual: number;
    stockMinimo: number;
  }>;
}
```

**Flujo atómico (`db.withTransactionSync`):**
1. Validar carrito no vacío.
2. `calcTotals(items, 'VENTA')`; total > 0.
3. `validateStockDisponible` para cada item.
4. `INSERT INTO transacciones (tipo='VENTA', monto_total, tipo_pago, detalle)` → `lastInsertRowId`.
5. Para cada item: `INSERT INTO detalle_transacciones` + `UPDATE productos SET stock_actual = stock_actual - ?`.
6. `UPDATE resumen_caja SET total_ventas += total, saldo_actual += total WHERE id = 1`.
7. Post-commit: `collectStockAlerts` → Toast si `stockActual < stockMinimo`.

## 8. UI State Machines (texto visible en español)

### Pantalla Ventas (catálogo + grid)
| Estado | Disparador | Siguiente | Texto visible (ES) |
|---|---|---|---|
| `CatálogoVacío` | Resultado de búsqueda vacío | — | `"No se encontraron productos"` (EmptyState) |
| `ProductoSeleccionado` | Tocar tarjeta producto | `LineItemModal` | `"Agregar al carrito"` |

### LineItemModal
| Estado | Disparador | Siguiente | Texto visible (ES) |
|---|---|---|---|
| `CantidadInicial` | Abrir modal | — | `"Cantidad"` (numérico, decimales permitidos) |
| `DescuentoIngresado` | Tocar descuento | — | `"Descuento (monto fijo)"`, validado ≤ subtotal |
| `Confirmado` | Tocar Agregar | Carrito actualizado | `"Producto agregado"` |

### PaymentModal
| Estado | Disparador | Siguiente | Texto visible (ES) |
|---|---|---|---|
| `SeleccionarPago` | Abrir pago | — | `"Tipo de pago"` (EFECTIVO / TARJETA / TRANSFERENCIA / QR) |
| `MontoRecibido` | Seleccionar EFECTIVO | — | `"Monto recibido"` |
| `CálculoCambio` | Cambiar monto recibido | — | `cambio = recibido - total` (verde ≥ 0, rojo < 0) |
| `Confirmar` | Tocar Confirmar | Comprobante | `"Pagar"` |

### Comprobante
| Estado | Disparador | Siguiente | Texto visible (ES) |
|---|---|---|---|
| `VistaPrevia` | Pago exitoso | — | `"Recibo"` / `"Comprobante"` |
| `Compartido` | Tocar Compartir | — | `Sharing.shareAsync(uri)` |

## 9. Componentes UI

| Componente | Archivo | Tipo | Memoizado |
|---|---|---|---|
| `ProductGridCard` | `src/presentation/components/product/ProductGridCard.tsx` | UI | Sí |
| `ProductFallbackAvatar` | `src/presentation/components/product/ProductFallbackAvatar.tsx` | UI | Sí |
| `LineItemModal` | `src/presentation/components/cart/LineItemModal.tsx` | UI | No |
| `PaymentModal` | `src/presentation/components/cart/PaymentModal.tsx` | UI | No |
| `CartSummaryBar` | `src/presentation/components/cart/CartSummaryBar.tsx` | UI | No |
| `SearchBar` | `src/presentation/components/filters/SearchBar.tsx` | UI | No |
| `CategoryChipsRow` | `src/presentation/components/filters/CategoryChipsRow.tsx` | UI | No |
| `SortMenu` | `src/presentation/components/filters/SortMenu.tsx` | UI | No |

## 10. Hooks

| Hook | Archivo | Responsabilidad |
|---|---|---|
| `useCarrito` | `src/presentation/hooks/useCarrito.ts` | Selectores granulares de carrito (Zustand) |
| `useTransacciones` | `src/presentation/hooks/useTransacciones.ts` | Listado paginado de transacciones |

## 11. Almacenes Zustand

| Store | Archivo | Responsabilidad |
|---|---|---|
| `carritoStore` | `src/presentation/stores/carritoStore.ts` | Carrito con tipo `VENTA` \| `COMPRA` |

## 12. NFRs Aplicables
- NFR-02: FlashList `numColumns={3}`, sub-50ms render, sin `estimatedItemSize`.
- NFR-04: Paginación `LIMIT ? OFFSET ?` con `pageSize` de settings (default 15).
- NFR-07: Atomic rollback via `withTransactionSync`.
- NFR-08: UI en español; código en inglés.
- NFR-10: TypeScript estricto.

## 13. Puntos de extensión (pendiente de refinamiento)
- [ ] Pago mixto (múltiples `tipoPago` por transacción) — actualmente no soportado (INV-04).
- [ ] Impresión térmica nativa (actualmente `expo-print` HTML → archivo → compartir).
