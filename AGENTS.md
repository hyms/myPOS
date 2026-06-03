# AGENTS.md — Guía técnica y de construcción de TPV Móvil Express

Documento vivo que conserva todas las decisiones técnicas, arquitectura, reglas de
negocio y pasos de implementación para construir la app móvil **TPV Móvil Express**.
Cualquier agente o desarrollador que abra este repo debe leer este archivo primero.

---

## 1. Decisiones técnicas confirmadas

| Aspecto                | Elección                                                                       |
| ---------------------- | ------------------------------------------------------------------------------ |
| Framework              | Expo SDK 56 (Managed Workflow, TypeScript estricto)                            |
| Lenguaje               | TypeScript con `strict`, `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes` |
| Estilos                | NativeWind v4 + tokens de tema propios                                         |
| Navegación             | Expo Router v5 (file-based)                                                    |
| Listas                 | `@shopify/flash-list` v2 (`numColumns={3}`, sin `estimatedItemSize`)           |
| Base de datos          | `expo-sqlite` API síncrona JSI (`openDatabaseSync`, `withTransactionSync`)      |
| Estado UI              | Zustand (carrito, settings, sesión)                                            |
| Datos persistidos      | Hooks sobre SQLite con invalidación manual (estilo TanStack Query)             |
| Seguridad PIN          | `expo-secure-store`                                                             |
| Autenticación biométrica | `expo-local-authentication`                                                  |
| Imágenes               | `expo-image-manipulator` + `expo-file-system` + `expo-image`                    |
| Comprobantes           | `expo-print` + `expo-sharing`                                                  |
| Backup                 | `expo-document-picker` + `expo-file-system` + `expo-sharing`                    |
| Toasts                 | `react-native-toast-message`                                                   |
| Fechas                 | `dayjs`                                                                        |
| Gestor de paquetes     | pnpm                                                                           |

---

## 2. Estructura de carpetas (SOLID + separación de capas)

```
myPOS/
├── app/                              # Expo Router (Presentation layer)
│   ├── _layout.tsx                   # Root: providers, theme, DB init gate
│   ├── lock.tsx                      # Pantalla de PIN/Biometría
│   ├── (tabs)/
│   │   ├── _layout.tsx               # Tabs: Ventas, Compras, Productos, Caja, Más
│   │   ├── index.tsx                 # Ventas (carrito)
│   │   ├── compras.tsx               # Compras (carrito)
│   │   ├── productos.tsx             # Catálogo
│   │   ├── caja.tsx                  # Cuentas / Resumen
│   │   └── mas.tsx                   # Gastos + Settings (entry)
│   ├── gastos/
│   │   ├── index.tsx                 # Lista mes/año actual
│   │   └── nuevo.tsx                 # Formulario gasto
│   ├── productos/
│   │   ├── nuevo.tsx                 # Form crear
│   │   └── [id].tsx                  # Form editar
│   ├── transacciones/
│   │   └── [id].tsx                  # Detalle / eliminar
│   └── settings/
│       ├── index.tsx
│       ├── seguridad.tsx
│       ├── divisa.tsx
│       ├── paginacion.tsx
│       └── backup.tsx
│
├── src/
│   ├── domain/                       # 🔵 Capa de dominio (pura, sin dependencias externas)
│   │   ├── entities/
│   │   │   ├── Producto.ts
│   │   │   ├── Categoria.ts
│   │   │   ├── Transaccion.ts
│   │   │   ├── DetalleTransaccion.ts
│   │   │   ├── ResumenCaja.ts
│   │   │   └── CarritoItem.ts
│   │   ├── value-objects/
│   │   │   ├── Money.ts              # Manejo de divisa (Bs/$/€)
│   │   │   └── Currency.ts
│   │   └── rules/
│   │       ├── stockRules.ts         # validateStock, isBelowMin
│   │       └── cartRules.ts          # calcTotal, applyDiscount, calcChange
│   │
│   ├── data/                         # 🟢 Capa de datos
│   │   ├── database/
│   │   │   ├── client.ts             # openDatabaseSync singleton + migraciones
│   │   │   ├── schema.sql.ts         # CREATE TABLE strings
│   │   │   ├── migrations.ts         # Runner versionado (PRAGMA user_version)
│   │   │   └── seed.ts               # INSERT resumen_caja id=1
│   │   ├── repositories/             # Interfaces segregadas (I en SOLID)
│   │   │   ├── IProductoRepository.ts
│   │   │   ├── ICategoriaRepository.ts
│   │   │   ├── ITransaccionRepository.ts
│   │   │   ├── IResumenCajaRepository.ts
│   │   │   ├── SqliteProductoRepository.ts
│   │   │   ├── SqliteCategoriaRepository.ts
│   │   │   ├── SqliteTransaccionRepository.ts
│   │   │   └── SqliteResumenCajaRepository.ts
│   │   └── mappers/
│   │       └── rowMappers.ts         # Row -> Entity
│   │
│   ├── application/                  # 🟡 Use cases (orquestan repositorios)
│   │   ├── ventas/
│   │   │   ├── RegistrarVenta.ts     # withTransactionSync atómica
│   │   │   └── RevertirVenta.ts      # ⚠️ stock+, caja-
│   │   ├── compras/
│   │   │   ├── RegistrarCompra.ts
│   │   │   └── RevertirCompra.ts     # ⚠️ stock-, caja+ (devolver costo)
│   │   ├── gastos/
│   │   │   ├── RegistrarGasto.ts
│   │   │   └── RevertirGasto.ts      # ⚠️ caja+ (devolver gasto)
│   │   ├── productos/
│   │   │   ├── CrearProducto.ts
│   │   │   ├── ActualizarProducto.ts
│   │   │   └── EliminarProducto.ts   # soft delete
│   │   ├── reportes/
│   │   │   └── ResumenMesAnio.ts
│   │   └── backup/
│   │       ├── ExportarDB.ts
│   │       └── RestaurarDB.ts
│   │
│   ├── infrastructure/               # 🟠 Servicios concretos
│   │   ├── image/
│   │   │   ├── ImageOptimizer.ts     # 400x400, JPEG 70%, <40KB
│   │   │   └── ImageStorage.ts       # documentDirectory/productos/
│   │   ├── pdf/
│   │   │   └── ComprobanteRenderer.ts
│   │   ├── security/
│   │   │   ├── PinService.ts         # SecureStore + hash sha256
│   │   │   └── BiometricService.ts   # expo-local-authentication
│   │   ├── settings/
│   │   │   └── SettingsStore.ts      # AsyncStorage para divisa, pageSize, biometricEnabled
│   │   └── toast/
│   │       └── ToastService.ts
│   │
│   ├── presentation/                 # 🟣 UI compartida
│   │   ├── components/
│   │   │   ├── ui/                   # Button, Input, Modal, Card, Badge
│   │   │   ├── product/
│   │   │   │   ├── ProductGridCard.tsx       # memo + 3 col
│   │   │   │   ├── ProductFallbackAvatar.tsx # inicial sobre color sólido
│   │   │   │   ├── ProductFormHeader.tsx     # ⚠️ Imagen ARRIBA primero
│   │   │   │   ├── CategorySelector.tsx
│   │   │   │   └── QuickCategoryModal.tsx    # [+] sin perder form data
│   │   │   ├── cart/
│   │   │   │   ├── LineItemModal.tsx         # cantidad + descuento fijo
│   │   │   │   ├── PaymentModal.tsx          # con calculadora de cambio
│   │   │   │   └── CartSummaryBar.tsx
│   │   │   ├── filters/
│   │   │   │   ├── SearchBar.tsx
│   │   │   │   ├── CategoryChipsRow.tsx      # texto limpio sin colores
│   │   │   │   └── SortMenu.tsx              # por popularidad
│   │   │   ├── feedback/
│   │   │   │   ├── Toast.tsx                 # stock mínimo alert
│   │   │   │   └── ConfirmDialog.tsx
│   │   │   └── lists/
│   │   │       └── PaginatedFlashList.tsx    # LIMIT/OFFSET + infinite scroll
│   │   ├── hooks/
│   │   │   ├── useDatabase.ts
│   │   │   ├── useProductos.ts        # con search, categoría, sort
│   │   │   ├── useCategorias.ts
│   │   │   ├── useCarrito.ts          # Zustand selector
│   │   │   ├── useResumenCaja.ts      # lectura directa, sin agregaciones
│   │   │   ├── useTransacciones.ts    # paginado
│   │   │   ├── useGastos.ts           # filtro mes/año actual
│   │   │   ├── useStockAlert.ts
│   │   │   ├── useCurrency.ts
│   │   │   ├── usePinGate.ts          # bloqueo para acciones críticas
│   │   │   └── useInfiniteScroll.ts   # con pageSize de settings
│   │   ├── stores/                    # Zustand
│   │   │   ├── carritoStore.ts        # tipo: 'VENTA' | 'COMPRA'
│   │   │   ├── settingsStore.ts       # divisa, pageSize, biometric
│   │   │   └── sessionStore.ts        # unlocked: boolean
│   │   └── theme/
│   │       ├── tokens.ts              # colors, spacing, radii
│   │       └── tailwind.config.js
│   │
│   └── shared/
│       ├── types/                     # Tipos transversales
│       ├── utils/
│       │   ├── date.ts                # mes/año actual, formato
│       │   ├── format.ts              # currency, números
│       │   └── id.ts
│       └── constants.ts
│
├── assets/
├── app.json
├── babel.config.js
├── metro.config.js
├── tailwind.config.js
├── global.css
├── tsconfig.json                      # strict, noUncheckedIndexedAccess
├── package.json
└── pnpm-lock.yaml
```

### Aplicación de SOLID

- **S** – Cada repositorio expone solo operaciones de su entidad; cada use case hace una sola cosa.
- **O** – Repos detrás de interfaces (`IProductoRepository`); se puede cambiar SQLite por otra fuente sin tocar use cases.
- **L** – Las implementaciones `Sqlite*Repository` son sustituibles por mocks en tests.
- **I** – Interfaces separadas por entidad (no un mega `IRepository`).
- **D** – Pantallas dependen de hooks → hooks de use cases → use cases de interfaces. La capa concreta SQLite se inyecta una sola vez en `app/_layout.tsx`.

---

## 3. Esquema SQLite y migraciones

### Archivo: `src/data/database/client.ts`

- Singleton: `const db = SQLite.openDatabaseSync('tpv.db')`.
- `PRAGMA journal_mode = WAL;` y `PRAGMA foreign_keys = ON;`
- Migración versionada con `PRAGMA user_version`:
  - **v1**: las 5 tablas + `INSERT OR IGNORE INTO resumen_caja(id=1, …)`.
  - **Índices** críticos para rendimiento:

```sql
CREATE INDEX IF NOT EXISTS idx_productos_categoria
  ON productos(categoria_id) WHERE fecha_delete IS NULL;
CREATE INDEX IF NOT EXISTS idx_productos_nombre
  ON productos(nombre) WHERE fecha_delete IS NULL;
CREATE INDEX IF NOT EXISTS idx_transacciones_fecha
  ON transacciones(fecha_registro) WHERE fecha_delete IS NULL;
CREATE INDEX IF NOT EXISTS idx_transacciones_tipo
  ON transacciones(tipo) WHERE fecha_delete IS NULL;
CREATE INDEX IF NOT EXISTS idx_detalle_trx
  ON detalle_transacciones(transaccion_id);
CREATE INDEX IF NOT EXISTS idx_detalle_producto
  ON detalle_transacciones(producto_id);
```

### Tablas (DDL exacto, punto 2 de la spec)

```sql
CREATE TABLE IF NOT EXISTS categorias (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_delete TIMESTAMP DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS productos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    categoria_id INTEGER,
    nombre TEXT NOT NULL,
    precio_venta REAL NOT NULL,
    precio_compra REAL NOT NULL,
    stock_actual REAL DEFAULT 0.0,
    stock_minimo REAL DEFAULT 0.0,
    imagen_uri TEXT,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_delete TIMESTAMP DEFAULT NULL,
    FOREIGN KEY(categoria_id) REFERENCES categorias(id)
);

CREATE TABLE IF NOT EXISTS transacciones (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tipo TEXT NOT NULL, -- 'VENTA', 'COMPRA', 'GASTO'
    monto_total REAL NOT NULL,
    tipo_pago TEXT NOT NULL, -- 'EFECTIVO', 'TARJETA', 'TRANSFERENCIA', 'QR'
    detalle TEXT,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_delete TIMESTAMP DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS detalle_transacciones (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    transaccion_id INTEGER NOT NULL,
    producto_id INTEGER NOT NULL,
    cantidad REAL NOT NULL,
    precio_unitario REAL NOT NULL,
    FOREIGN KEY(transaccion_id) REFERENCES transacciones(id),
    FOREIGN KEY(producto_id) REFERENCES productos(id)
);

CREATE TABLE IF NOT EXISTS resumen_caja (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    total_ventas REAL DEFAULT 0.0,
    total_compras REAL DEFAULT 0.0,
    total_gastos REAL DEFAULT 0.0,
    saldo_actual REAL DEFAULT 0.0
);

INSERT OR IGNORE INTO resumen_caja (id, total_ventas, total_compras, total_gastos, saldo_actual)
VALUES (1, 0.0, 0.0, 0.0, 0.0);
```

---

## 4. Lógica atómica de transacciones (corazón del sistema)

Todas las operaciones de escritura usan `db.withTransactionSync(() => { … })` para
garantizar atomicidad: si cualquier `UPDATE` falla, se hace rollback automático y
no queda ningún descuadre.

### 4.1 `RegistrarVenta`

```ts
db.withTransactionSync(() => {
  const r = db.runSync(
    'INSERT INTO transacciones (tipo, monto_total, tipo_pago, detalle) VALUES (?, ?, ?, ?)',
    'VENTA', total, tipoPago, detalle
  );
  const transaccionId = r.lastInsertRowId;

  for (const it of items) {
    db.runSync(
      'INSERT INTO detalle_transacciones (transaccion_id, producto_id, cantidad, precio_unitario) VALUES (?, ?, ?, ?)',
      transaccionId, it.productoId, it.cantidad, it.precioUnitario
    );
    db.runSync(
      'UPDATE productos SET stock_actual = stock_actual - ?, fecha_update = CURRENT_TIMESTAMP WHERE id = ?',
      it.cantidad, it.productoId
    );
  }

  db.runSync(
    'UPDATE resumen_caja SET total_ventas = total_ventas + ?, saldo_actual = saldo_actual + ? WHERE id = 1',
    total, total
  );
});
// Post-commit: emitir Toast si stock_actual < stock_minimo
```

### 4.2 `RegistrarCompra`

Igual que venta pero: `stock + cantidad`, `total_compras +`, `saldo_actual -`.

### 4.3 `RegistrarGasto`

`INSERT transacciones (tipo='GASTO', …)` + `total_gastos +`, `saldo_actual -`. No tiene
detalle de productos.

### 4.4 `EliminarTransaccion(id)` — reversión con soft delete

```ts
db.withTransactionSync(() => {
  const trx = repo.findById(id);              // tipo, monto_total
  const items = repo.findDetalles(id);        // [{producto_id, cantidad}]

  switch (trx.tipo) {
    case 'VENTA':
      for (const it of items) {
        db.runSync(
          'UPDATE productos SET stock_actual = stock_actual + ?, fecha_update = CURRENT_TIMESTAMP WHERE id = ?',
          it.cantidad, it.producto_id
        );
      }
      db.runSync(
        'UPDATE resumen_caja SET total_ventas = total_ventas - ?, saldo_actual = saldo_actual - ? WHERE id = 1',
        trx.monto_total, trx.monto_total
      );
      break;
    case 'COMPRA':
      for (const it of items) {
        db.runSync(
          'UPDATE productos SET stock_actual = stock_actual - ? WHERE id = ?',
          it.cantidad, it.producto_id
        );
      }
      db.runSync(
        'UPDATE resumen_caja SET total_compras = total_compras - ?, saldo_actual = saldo_actual + ? WHERE id = 1',
        trx.monto_total, trx.monto_total
      );
      break;
    case 'GASTO':
      db.runSync(
        'UPDATE resumen_caja SET total_gastos = total_gastos - ?, saldo_actual = saldo_actual + ? WHERE id = 1',
        trx.monto_total, trx.monto_total
      );
      break;
  }

  db.runSync(
    'UPDATE transacciones SET fecha_delete = CURRENT_TIMESTAMP WHERE id = ?',
    id
  );
});
```

### Reglas de stock en reversión

- **VENTA eliminada**: `Stock Actual = Stock Actual + Cantidad`; `total_ventas -= monto_total`, `saldo_actual -= monto_total`.
- **COMPRA eliminada**: `Stock Actual = Stock Actual − Cantidad`; `total_compras -= monto_total`, `saldo_actual += monto_total`.
- **GASTO eliminado**: `total_gastos -= monto_total`, `saldo_actual += monto_total`.

### Validación previa (fuera de la transacción)

Si revertir una COMPRA dejaría stock negativo, lanzar error explícito con el producto
afectado antes de iniciar la transacción. Esto evita revertir parcialmente.

---

## 5. Módulos UI/UX (detalle)

### A. Productos

- **Form (`app/productos/nuevo.tsx` y `[id].tsx`)**:
  - Orden vertical estricto: **`ProductFormHeader` (cámara/galería) PRIMERO**, luego
    `Input nombre`, `Input precio_venta`, `Input precio_compra`, `Input stock_actual`,
    `Input stock_minimo`, `CategorySelector`.
  - Pipeline de imagen:
    1. `ImageManipulator.manipulateAsync(uri, [{ resize: { width: 400, height: 400 } }], { compress: 0.7, format: SaveFormat.JPEG })`.
    2. `FileSystem.copyAsync` → `documentDirectory + 'productos/<uuid>.jpg'`.
    3. Verificar tamaño con `FileSystem.getInfoAsync` (objetivo < 40 KB).
  - Persistir solo la URI final.
- **`CategorySelector`**: dropdown con botón **[+]** que abre `QuickCategoryModal`
  (sheet nativo). El form guarda su estado en `productDraftStore` (Zustand) para que
  el modal no destruya los datos ya escritos.
- **`useStockAlert`**: se activa tras ventas y ediciones de producto; emite Toast no
  obstructivo si `stock_actual < stock_minimo`.

### B. Ventas y Compras (mismo motor, flag `tipo`)

- **Cuadrícula**: `FlashList` v2 con `numColumns={3}`. `ProductGridCard` memoizado.
- **Fallback sin imagen**: `ProductFallbackAvatar` muestra la primera letra del nombre
  sobre un color sólido determinístico (hash del id → paleta).
- **Filtros**:
  - Búsqueda por texto (debounce 200 ms).
  - Chips de categoría (texto limpio, sin colores).
  - Orden por popularidad de ventas:

  ```sql
  SELECT p.*, COALESCE(SUM(d.cantidad), 0) AS popularidad
  FROM productos p
  LEFT JOIN detalle_transacciones d ON d.producto_id = p.id
  LEFT JOIN transacciones t
    ON t.id = d.transaccion_id
   AND t.tipo = 'VENTA'
   AND t.fecha_delete IS NULL
  WHERE p.fecha_delete IS NULL
    AND (? IS NULL OR p.categoria_id = ?)
    AND p.nombre LIKE ?
  GROUP BY p.id
  ORDER BY popularidad DESC, p.nombre ASC
  LIMIT ? OFFSET ?;
  ```

- **`LineItemModal`**: input cantidad (numérico, soporta decimales), input descuento
  por monto fijo (validado `<=` subtotal).
- **`PaymentModal`**:
  - Tipo de pago: `EFECTIVO | TARJETA | TRANSFERENCIA | QR`.
  - Solo si `EFECTIVO` se muestra el `Input "Monto recibido"` con cálculo en vivo:
    `cambio = recibido − total` (`useMemo`). Verde si ≥ 0, rojo si negativo.
- **Comprobante (`ComprobanteRenderer`)**:
  - Plantilla HTML minimalista (logo opcional, líneas, total, cambio).
  - `Print.printToFileAsync({ html })` → `Sharing.shareAsync(uri)`.

### C. Gastos

- Form directo: solo `monto` y `descripcion`.
- `ConfirmDialog` con texto interpolado:
  `"¿Está seguro de registrar este gasto por un monto de [Monto] [Divisa] con el detalle [Descripción]?"`
  Opciones: **Cancelar** | **Confirmar**.
- Lista: scroll infinito filtrado por mes y año actual:

  ```sql
  WHERE tipo = 'GASTO'
    AND strftime('%Y-%m', fecha_registro) = strftime('%Y-%m', 'now', 'localtime')
    AND fecha_delete IS NULL
  ORDER BY fecha_registro DESC
  LIMIT ? OFFSET ?;
  ```

### D. Caja

- `useResumenCaja()` lee **únicamente** una fila:
  ```sql
  SELECT saldo_actual, total_ventas, total_compras, total_gastos
  FROM resumen_caja WHERE id = 1;
  ```
  Sin agregaciones costosas.
- **Reportes Rápidos** (tarjetas resumen mes/año) vía `ResumenMesAnio`:
  ```sql
  SELECT tipo, SUM(monto_total) AS total
  FROM transacciones
  WHERE fecha_delete IS NULL
    AND strftime('%Y', fecha_registro) = strftime('%Y','now','localtime')
    AND (? = 'YEAR' OR strftime('%m', fecha_registro) = strftime('%m','now','localtime'))
  GROUP BY tipo;
  ```

### E. Configuración (Settings)

- **Paginación**: input numérico, default `15`. Persistido en `SettingsStore`. Todos
  los hooks históricos aplican `LIMIT ? OFFSET ?` con este valor.
- **Seguridad**:
  - `PinService.setPin(newPin)` exige el PIN actual (excepto la primera vez).
  - Switch de biometría usa `LocalAuthentication.authenticateAsync` antes de activarse.
  - Eliminar transacciones o reportes pasa por `usePinGate()`.
- **Divisa**: radio `Bs | $ | €` → `settingsStore.currency`. `useCurrency()` expone
  formateador (`Intl.NumberFormat`).
- **Backup**:
  - Exportar: `FileSystem.copyAsync(dbPath, documentDirectory + 'backups/tpv-YYYYMMDD-HHmm.db')`
    + `Sharing.shareAsync`.
  - Restaurar: `DocumentPicker.getDocumentAsync` → cerrar DB → copiar sobre archivo
    principal → reabrir → forzar `app reload` con `expo-updates` o reinicio in-memory
    de stores.

---

## 6. Performance táctil

- `ProductGridCard`, `ProductFallbackAvatar`, `CategoryChip` envueltos en `React.memo`.
- `keyExtractor`, `renderItem` con `useCallback`.
- Listas de catálogo cargan **solo columnas necesarias**
  (`id, nombre, precio_venta, stock_actual, imagen_uri, categoria_id`).
- Imágenes mostradas con `expo-image` (`contentFit="cover"`, `cachePolicy="memory-disk"`).
- Búsqueda con debounce 200 ms; consulta única indexada.
- Carrito en Zustand con **selectores granulares** para evitar re-render global.

---

## 7. Inicialización de la app (`app/_layout.tsx`)

1. Splash visible mientras corre `bootstrap()`:
   - `openDatabaseSync` + migraciones + seed.
   - Load `settingsStore` (divisa, pageSize, biometric).
   - Load `sessionStore` (¿hay PIN configurado?).
2. Si PIN configurado → redirigir a `/lock`. Si biometría habilitada,
   `authenticateAsync` se lanza automáticamente.
3. Tras desbloquear → `(tabs)`.

---

## 8. TypeScript estricto (interfaces clave)

`src/domain/entities/*` define todas las entidades. Fechas como `string` ISO, IDs
como `number`, `readonly` donde aplique.

```ts
export interface Producto {
  id: number;
  categoriaId: number | null;
  nombre: string;
  precioVenta: number;
  precioCompra: number;
  stockActual: number;
  stockMinimo: number;
  imagenUri: string | null;
  fechaRegistro: string;
  fechaUpdate: string;
  fechaDelete: string | null;
}

export interface Categoria {
  id: number;
  nombre: string;
  fechaRegistro: string;
  fechaDelete: string | null;
}

export type TipoTransaccion = 'VENTA' | 'COMPRA' | 'GASTO';
export type TipoPago = 'EFECTIVO' | 'TARJETA' | 'TRANSFERENCIA' | 'QR';

export interface Transaccion {
  id: number;
  tipo: TipoTransaccion;
  montoTotal: number;
  tipoPago: TipoPago;
  detalle: string | null;
  fechaRegistro: string;
  fechaDelete: string | null;
}

export interface DetalleTransaccion {
  id: number;
  transaccionId: number;
  productoId: number;
  cantidad: number;
  precioUnitario: number;
}

export interface ResumenCaja {
  totalVentas: number;
  totalCompras: number;
  totalGastos: number;
  saldoActual: number;
}

export interface CarritoItem {
  producto: Pick<
    Producto,
    'id' | 'nombre' | 'precioVenta' | 'precioCompra' | 'stockActual' | 'imagenUri'
  >;
  cantidad: number;
  descuento: number;
}
```

`tsconfig.json`:
- `"strict": true`
- `"noUncheckedIndexedAccess": true`
- `"noImplicitOverride": true`
- `"exactOptionalPropertyTypes": true`

---

## 9. Dependencias a instalar (`package.json` esperado)

Núcleo:

- `expo` ^56
- `expo-router`
- `expo-status-bar`, `expo-splash-screen`, `expo-system-ui`
- `react`, `react-native`
- `expo-sqlite`
- `expo-file-system`
- `expo-image-manipulator`
- `expo-image`
- `expo-print`
- `expo-sharing`
- `expo-document-picker`
- `expo-secure-store`
- `expo-local-authentication`
- `expo-updates` (para recarga tras restaurar backup)
- `@shopify/flash-list` ^2
- `zustand`
- `nativewind` ^4
- `tailwindcss`
- `react-native-reanimated`
- `react-native-gesture-handler`
- `react-native-safe-area-context`
- `react-native-screens`
- `react-native-toast-message`
- `dayjs`

Dev:

- `typescript`
- `@types/react`, `@types/react-native`
- `eslint`, `@react-native/eslint-config`, `prettier`, `eslint-plugin-tailwindcss`

---

## 10. Orden de implementación propuesto

1. **Scaffold**: `pnpm create expo-app@latest myPOS --template default` → limpiar demo
   → configurar Expo Router, NativeWind, tsconfig estricto.
2. **Capa de datos**: `client.ts` + migraciones + seed + repos SQLite.
3. **Use cases críticos**: `RegistrarVenta`, `RegistrarCompra`, `RegistrarGasto`,
   `EliminarTransaccion` (con tests manuales por consola al inicio).
4. **Stores Zustand**: `settingsStore`, `sessionStore`, `carritoStore`.
5. **Tema** NativeWind + tokens + componentes UI base.
6. **Pantalla Productos** (catálogo + form + modal categoría rápida + optimizador
   de imagen).
7. **Pantalla Ventas** (grid FlashList + filtros + LineItemModal + PaymentModal +
   comprobante).
8. **Pantalla Compras** (reutiliza el motor de Ventas con flag).
9. **Pantalla Gastos** + ConfirmDialog.
10. **Pantalla Caja** + tarjetas mes/año.
11. **Settings**: paginación, divisa, PIN, biometría, backup/restore.
12. **Pantalla Lock** + gates de seguridad.
13. **Pulido**: animaciones de Reanimated, Toasts, empty/error states.

---

## 11. Decisiones cerradas (confirmadas 2026-06-03)

1. **Categoría obligatoria al crear producto**: ✅ Sí. `categoria_id` se exige al
   crear/editar producto; nunca se persiste con `NULL`. Validación en formulario
   y en `SqliteProductoRepository.crear`/`actualizar`.
2. **Comprobante para los tres tipos**: ✅ `VENTA`, `COMPRA` y `GASTO` generan
   comprobante (recibo) al finalizar. `ComprobanteRenderer` recibe el tipo y
   ajusta título/leyenda.
3. **Stock en unidades enteras**: ✅ Inputs de cantidad y `stock_actual` /
   `stock_minimo` solo aceptan enteros. Aunque la columna es `REAL` por
   tolerancia/forward-compat, en la UI se fuerza `keyboardType="number-pad"`
   y se redondea con `Math.floor` al persistir.
4. **Eliminar producto**: ✅ Solo **soft delete** (`fecha_delete = CURRENT_TIMESTAMP`).
   El producto se oculta del catálogo activo pero las `transacciones` y
   `detalle_transacciones` históricas quedan intactas para reportes forenses.
5. **Pago único por transacción**: ✅ Un solo `tipo_pago` por `transaccion`. No
   se modela pago mixto.
6. **Restauración de backup**: ✅ **No** requiere PIN. La acción de restaurar
   es libre (flujo crítico de recuperación de datos). Se documenta un
   `ConfirmDialog` con texto explícito: *"Esto sobrescribirá todos los datos
   actuales"* antes de ejecutar.

---

## 12. Convenciones de código

- **NO** agregar comentarios salvo que el usuario lo pida explícitamente.
- Usar **TypeScript estricto** siempre.
- Nombres en `camelCase` para variables/funciones, `PascalCase` para componentes,
  `SCREAMING_SNAKE_CASE` para constantes.
- Imports ordenados: externos → internos absolutos `@/...` (alias configurado).
- Estilos: preferir clases de NativeWind; tokens para colores/espaciados.
- Todo `useState`/`useEffect` con dependencias completas; ESLint con reglas
  `exhaustive-deps` activas.
- Toda query SQL parametrizada (nunca concatenar strings con datos del usuario).
- Ningún `console.log` en producción; usar `ToastService` o logger central.
- Commits con Conventional Commits (`feat:`, `fix:`, `refactor:`, `chore:`).

---

## 13. Reglas de negocio explícitas

- **No se permite stock negativo** en ventas. Si `stock_actual - cantidad < 0`,
  rechazar la venta con error antes de ejecutar la transacción SQL.
- **No se permite revertir una COMPRA** que dejaría stock negativo en algún producto.
- **Todo cambio de PIN** o **eliminación de transacción** exige el PIN actual.
- **Todo `fecha_delete` es soft delete**. Las consultas siempre filtran
  `WHERE fecha_delete IS NULL` salvo reportes forenses.
- **Divisa** afecta solo formato visual; los montos en SQLite son siempre numéricos
  puros en la moneda base.
- **Resumen de caja** siempre tiene `id = 1` (constraint `CHECK (id = 1)`). Es la
  única fila y nunca se elimina.
- **Imágenes** solo se persisten en `documentDirectory/productos/<uuid>.jpg`. La
  URI relativa se guarda en SQLite; nunca guardar la URI temporal del manipulador.
