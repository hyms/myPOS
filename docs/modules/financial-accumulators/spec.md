# Financial Accumulators — Specification (v2.0)

## 1. Bounded Context
Cuentas / acumuladores de caja, historial de transacciones, reportes por periodo (mes/año), reversión de transacciones.

## 2. Domain Entities & Value Objects

### 2.1 ResumenCaja
**Archivo:** `src/domain/entities/ResumenCaja.ts`

```ts
export interface ResumenCaja {
  readonly totalVentas: number;
  readonly totalCompras: number;
  readonly totalGastos: number;
  readonly saldoActual: number;
}
```

### 2.2 Transaccion (lectura)
**Archivo:** `src/domain/entities/Transaccion.ts`

```ts
export type TipoTransaccion = 'VENTA' | 'COMPRA' | 'GASTO';
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

## 3. Domain Rules & Invariants

| ID | Regla | Descripción |
|---|---|---|
| RULE-FA-01 | ResumenCaja singleton | `resumen_caja` tiene exactamente una fila (`id=1`, `CHECK (id=1)`). Nunca se elimina. |
| RULE-FA-02 | Lectura de una sola fila | `useResumenCaja()` lee únicamente `SELECT ... FROM resumen_caja WHERE id = 1`. Sin agregaciones en tiempo de ejecución. |
| RULE-FA-03 | Reversión atómica | `revertirTransaccion` ajusta stock y caja dentro de `db.withTransactionSync`. Si cualquier UPDATE falla, rollback completo. |
| RULE-FA-04 | Stock seguro en reversión de COMPRA | Si revertir una compra dejaría `stockActual < 0`, lanzar `StockError` antes de abrir la transacción. |
| RULE-FA-05 | Transacción ya revertida | Si `fechaDelete !== null`, lanzar error: "ya fue revertida". |

Invariantes globales: **INV-02, INV-03, INV-05, INV-06**.

## 4. Database Contract

```sql
CREATE TABLE IF NOT EXISTS resumen_caja (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    total_ventas REAL DEFAULT 0.0,
    total_compras REAL DEFAULT 0.0,
    total_gastos REAL DEFAULT 0.0,
    saldo_actual REAL DEFAULT 0.0
);

INSERT OR IGNORE INTO resumen_caja (id, total_ventas, total_compras, total_gastos, saldo_actual)
VALUES (1, 0.0, 0.0, 0.0, 0.0);

-- Lectura de caja (una sola fila)
SELECT saldo_actual, total_ventas, total_compras, total_gastos
FROM resumen_caja WHERE id = 1;

-- Reporte por periodo (mes/año)
SELECT tipo, SUM(monto_total) AS total
FROM transacciones
WHERE fecha_delete IS NULL
  AND strftime('%Y', fecha_registro) = strftime('%Y','now','localtime')
  AND (? = 'YEAR' OR strftime('%m', fecha_registro) = strftime('%m','now','localtime'))
GROUP BY tipo;
```

## 5. Repository Interface (`IResumenCajaRepository`)

**Archivo:** `src/data/repositories/IResumenCajaRepository.ts`

```ts
export interface IResumenCajaRepository {
  obtener(): ResumenCaja;  // lee fila id=1
}
```

## 6. Use Cases

### 6.1 `obtenerResumenCaja`
**Archivo:** `src/application/reportes/ResumenPeriodo.ts`

```ts
export function obtenerResumenCaja(): ResumenCaja {
  return getRepositories().resumenCaja.obtener();
}
```

### 6.2 `obtenerResumenMes` / `obtenerResumenAnio`
**Archivo:** `src/application/reportes/ResumenPeriodo.ts`

```ts
export interface ResumenPeriodo {
  readonly periodo: 'MONTH' | 'YEAR';
  readonly totales: ReadonlyArray<{ tipo: TipoTransaccion; total: number }>;
  readonly totalVentas: number;
  readonly totalGastos: number;
  readonly totalCompras: number;
  readonly neto: number;  // ventas - compras - gastos
}

export function obtenerResumenMes(): ResumenPeriodo;
export function obtenerResumenAnio(): ResumenPeriodo;
```

### 6.3 `revertirTransaccion` (unificado VENTA / COMPRA / GASTO)
**Archivo:** `src/application/transacciones/RevertirTransaccion.ts`

```ts
export function revertirTransaccion(id: number): {
  readonly transaccionId: number;
  readonly tipo: 'VENTA' | 'COMPRA' | 'GASTO';
}
```

**Aritmética de reversión (dentro de `db.withTransactionSync`):**

| Tipo | Ajuste de stock | Ajuste de caja |
|---|---|---|
| `VENTA` | `stock_actual += cantidad` por cada detalle | `total_ventas -= monto`, `saldo_actual -= monto` |
| `COMPRA` | `stock_actual -= cantidad` por cada detalle | `total_compras -= monto`, `saldo_actual += monto` |
| `GASTO` | (sin detalle) | `total_gastos -= monto`, `saldo_actual += monto` |

Posterior: `UPDATE transacciones SET fecha_delete = CURRENT_TIMESTAMP WHERE id = ?`.

Validación previa (fuera de transacción): si es COMPRA y `producto.stockActual - cantidad < 0` → `StockError`.

## 7. UI State Machines (texto visible en español)

| Estado | Disparador | Siguiente | Texto visible (ES) |
|---|---|---|---|
| `CajaCargada` | Abrir pantalla Caja | `ResumenMostrado` | `"Saldo actual"` |
| `TransacciónSeleccionada` | Tocar eliminar en transacción | `PinGate` | `"Ingrese su PIN"` |
| `PinGate` | PIN correcto | `ConfirmarEliminar` | `"¿Eliminar transacción?"` |
| `ConfirmarEliminar` | Tocar Confirmar | `Revertida` | `"Transacción revertida"` (Toast) |
| `Cualquiera` | PIN incorrecto | `Error` | `"PIN incorrecto"` |

## 8. Componentes UI (Reportes)

| Componente | Archivo | Tipo | Memoizado |
|---|---|---|---|
| `Stat` | `src/presentation/components/reports/Stat.tsx` | UI | No |
| `ReportCard` | `src/presentation/components/reports/ReportCard.tsx` | UI | No |
| `ReportRow` | `src/presentation/components/reports/ReportRow.tsx` | UI | No |
| `BreakdownBar` | `src/presentation/components/reports/BreakdownBar.tsx` | UI | No |

## 9. Componentes UI (Movimientos)

| Componente | Archivo | Tipo | Memoizado |
|---|---|---|---|
| `MovimientoListItem` | `src/presentation/components/movimientos/MovimientoListItem.tsx` | UI | No |
| `TipoFilterChips` | `src/presentation/components/movimientos/TipoFilterChips.tsx` | UI | No |

## 10. Pantallas

| Pantalla | Archivo | Responsabilidad |
|---|---|---|
| `CajaIndex` | `app/caja.tsx` | Resumen de caja + reportes mes/año |
| `TransaccionDetalle` | `app/transacciones/[id].tsx` | Detalle / eliminar transacción |
| `MovimientosIndex` | `app/movimientos/index.tsx` | Listado de movimientos paginado |

## 11. Hooks

| Hook | Archivo | Responsabilidad |
|---|---|---|
| `useResumenCaja` | `src/presentation/hooks/useResumenCaja.ts` | Lectura directa de fila id=1 |
| `useMovimientos` | `src/presentation/hooks/useMovimientos.ts` | Listado paginado de transacciones |
| `usePinGate` | `src/presentation/hooks/usePinGate.ts` | Bloqueo para acciones críticas (eliminar) |

## 12. Almacenes Zustand

| Store | Archivo | Responsabilidad |
|---|---|---|
| `sessionStore` | `src/presentation/stores/sessionStore.ts` | `unlocked: boolean` |

## 13. NFRs Aplicables
- NFR-03: 0ms cálculos de caja (tabla acumuladora `resumen_caja`).
- NFR-04: Paginación `LIMIT ? OFFSET ?`.
- NFR-07: Atomic rollback para reversiones.
- NFR-08: PIN gate para eliminar transacción.

## 14. Puntos de extensión (pendiente de refinamiento)
- [ ] Reportes forenses (incluir `fecha_delete IS NOT NULL`).
- [ ] Exportación de reporte a PDF/CSV.
