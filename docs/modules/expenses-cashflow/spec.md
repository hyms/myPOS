# Expenses & Cash Flow — Specification (v2.0)

## 1. Bounded Context
Registro de gastos con diálogo de confirmación; seguimiento del flujo de caja de salida.

## 2. Domain Entities & Value Objects

### 2.1 Transaccion (tipo GASTO)
**Archivo:** `src/domain/entities/Transaccion.ts`

```ts
export type TipoTransaccion = 'VENTA' | 'COMPRA' | 'GASTO';
// GASTO no tiene DetalleTransaccion asociado.
```

## 3. Domain Rules & Invariants

| ID | Regla | Descripción |
|---|---|---|
| RULE-EC-01 | Monto válido | `monto > 0` y `Number.isFinite(monto)`. |
| RULE-EC-02 | Descripción obligatoria | `descripcion.trim().length > 0`. |
| RULE-EC-03 | Sin detalle de productos | GASTO no inserta filas en `detalle_transacciones`. |
| RULE-EC-04 | ConfirmDialog interpolado | Texto: `"¿Está seguro de registrar este gasto por un monto de [Monto] [Divisa] con el detalle [Descripción]?"`. Opciones: Cancelar \| Confirmar. |

Invariantes globales: **INV-03, INV-05, INV-10**.

## 4. Database Contract

```sql
-- Tabla transacciones (tipo='GASTO'). Sin filas en detalle_transacciones.
-- Consulta de lista (mes/año actual):
WHERE tipo = 'GASTO'
  AND strftime('%Y-%m', fecha_registro) = strftime('%Y-%m', 'now', 'localtime')
  AND fecha_delete IS NULL
ORDER BY fecha_registro DESC
LIMIT ? OFFSET ?;
```

## 5. Repository Interface (`ITransaccionRepository`)

Métodos relevantes para gastos (ver `src/data/repositories/ITransaccionRepository.ts`):
- `listar({ tipo: 'GASTO', limit, offset })` — listado paginado de gastos.
- `gastosDelMes(limit, offset)` — gastos del mes actual con filtro `strftime('%Y-%m')`.

## 6. Use Case (`registrarGasto`)

**Archivo:** `src/application/gastos/RegistrarGasto.ts`

```ts
export interface RegistrarGastoInput {
  readonly monto: number;
  readonly descripcion: string;
  readonly tipoPago: TipoPago;
}

export interface RegistrarGastoResult {
  readonly transaccionId: number;
}
```

**Flujo atómico (`db.withTransactionSync`):**
1. Validar `monto > 0` y `descripcion` no vacía.
2. `INSERT INTO transacciones (tipo='GASTO', monto_total, tipo_pago, detalle=descripcion)`.
3. `UPDATE resumen_caja SET total_gastos += monto, saldo_actual -= monto WHERE id = 1`.
4. Retornar `transaccionId`.

## 7. UI State Machines (texto visible en español)

| Estado | Disparador | Siguiente | Texto visible (ES) |
|---|---|---|---|
| `FormVacío` | Abrir formulario gasto | `Editando` | `"Registrar gasto"` |
| `Editando` | Monto y descripción válidos | `ConfirmDialog` | `"¿Registrar gasto?"` |
| `ConfirmDialog` | Usuario toca Confirmar | `Registrado` | `"¿Está seguro de registrar este gasto por un monto de [Monto] [Divisa] con el detalle [Descripción]?"` |
| `Registrado` | ConfirmDialog aceptado | `ListaGastos` | `"Gasto registrado"` (Toast) |
| `Cualquiera` | Usuario toca Cancelar | `Editando` | `"Cancelar"` |

## 8. Componentes UI

| Componente | Archivo | Tipo | Memoizado |
|---|---|---|---|
| `ConfirmDialog` | `src/presentation/components/feedback/ConfirmDialog.tsx` | UI | No |
| `EmptyState` | `src/presentation/components/feedback/EmptyState.tsx` | UI | No |

## 9. Hooks

| Hook | Archivo | Responsabilidad |
|---|---|---|
| `useGastos` | `src/presentation/hooks/useGastos.ts` | Filtro mes/año actual, scroll infinito |

## 10. Pantallas

| Pantalla | Archivo | Responsabilidad |
|---|---|---|
| `GastosIndex` | `app/gastos/index.tsx` | Lista mes/año actual (scroll infinito) |
| `GastoNuevo` | `app/gastos/nuevo.tsx` | Formulario monto + descripción |

## 11. NFRs Aplicables
- NFR-04: Paginación `LIMIT ? OFFSET ?`, default 15.
- NFR-07: Atomic transaction para gasto + caja.
- NFR-08: UI strings en español.

## 12. Puntos de extensión (pendiente de refinamiento)
- [ ] Reversión de gasto (`RevertirGasto`) — actualmente unificado en `RevertirTransaccion`.
- [ ] Categorías de gasto.
