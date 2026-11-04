# Catalog & Inventory — Specification (v2.0)

## 1. Bounded Context
Responsable del ciclo de vida de productos, categorías, niveles de stock y alertas de stock mínimo.

## 2. Domain Entities & Value Objects

### 2.1 Producto
**Archivo:** `src/domain/entities/Producto.ts`

```ts
export type ProductoId = number;
export type CategoriaId = number;

export interface Producto {
  readonly id: ProductoId;
  readonly categoriaId: CategoriaId;
  readonly nombre: string;
  readonly precioVenta: number;
  readonly precioCompra: number;
  readonly stockActual: number;
  readonly stockMinimo: number;
  readonly imagenUri: string | null;
  readonly fechaRegistro: string;
  readonly fechaUpdate: string;
  readonly fechaDelete: string | null;
}

export type ProductoResumen = Pick<
  Producto,
  'id' | 'nombre' | 'precioVenta' | 'precioCompra' | 'stockActual' | 'imagenUri' | 'categoriaId'
>;

export type CreateProductoInput = {
  readonly categoriaId: CategoriaId;
  readonly nombre: string;
  readonly precioVenta: number;
  readonly precioCompra: number;
  readonly stockActual: number;
  readonly stockMinimo: number;
  readonly imagenUri: string | null;
};

export type UpdateProductoInput = {
  readonly categoriaId: CategoriaId;
  readonly nombre: string;
  readonly precioVenta: number;
  readonly precioCompra: number;
  readonly stockActual: number;
  readonly stockMinimo: number;
  readonly imagenUri: string | null;
};
```

### 2.2 Categoria
**Archivo:** `src/domain/entities/Categoria.ts`

```ts
export type CategoriaId = number;

export interface Categoria {
  readonly id: CategoriaId;
  readonly nombre: string;
  readonly fechaRegistro: string;
  readonly fechaDelete: string | null;
}

export type CreateCategoriaInput = {
  readonly nombre: string;
};
```

## 3. Domain Rules & Invariants

| ID | Regla | Descripción |
|---|---|---|
| RULE-CI-01 | Categoría obligatoria | `categoria_id` debe existir y no ser NULL al crear/editar producto. Validado en formulario y en `SqliteProductoRepository.crear`/`actualizar`. |
| RULE-CI-02 | Stock mínimo | `isBelowMin(producto)` devuelve `true` cuando `stockActual < stockMinimo`. |
| RULE-CI-03 | Stock entero en UI | Cantidades y stock se capturan como enteros (`keyboardType="number-pad"`), `Math.floor` al persistir. Columna `REAL` por forward-compat. |
| RULE-CI-04 | Soft delete | Producto eliminado establece `fecha_delete = CURRENT_TIMESTAMP`. Las consultas activas filtran `WHERE fecha_delete IS NULL`. |
| RULE-CI-05 | Imagen persistida | Solo URI final en `documentDirectory/productos/<uuid>.jpg`. Nunca URI temporal del manipulador. Tamaño objetivo < 40 KB. |

Invariantes globales aplicables: **INV-01, INV-05, INV-07, INV-09**.

## 4. Database Contract

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

-- Índices parciales (fecha_delete IS NULL)
CREATE INDEX IF NOT EXISTS idx_productos_categoria ON productos(categoria_id) WHERE fecha_delete IS NULL;
CREATE INDEX IF NOT EXISTS idx_productos_nombre ON productos(nombre) WHERE fecha_delete IS NULL;
CREATE INDEX IF NOT EXISTS idx_productos_delete ON productos(fecha_delete) WHERE fecha_delete IS NULL;
```

## 5. Repository Interface (`IProductoRepository`)

**Archivo:** `src/data/repositories/IProductoRepository.ts`

```ts
export type SortOrder = 'NOMBRE_ASC' | 'POPULARIDAD_DESC';

export interface ProductoQuery {
  readonly search?: string;
  readonly categoriaId?: CategoriaId;
  readonly sort: SortOrder;
  readonly limit: number;
  readonly offset: number;
}

export interface IProductoRepository {
  listar(query: ProductoQuery): ReadonlyArray<Producto & { popularidad: number }>;
  findById(id: ProductoId): Producto | null;
  crear(input: CreateProductoInput): Producto;
  actualizar(id: ProductoId, input: UpdateProductoInput): Producto;
  eliminar(id: ProductoId): void;           // soft delete
  findByIds(ids: ReadonlyArray<ProductoId>): ReadonlyArray<Producto>;
}
```

Query de popularidad (ventas):
```sql
SELECT p.*, COALESCE(SUM(d.cantidad), 0) AS popularidad
FROM productos p
LEFT JOIN detalle_transacciones d ON d.producto_id = p.id
LEFT JOIN transacciones t
  ON t.id = d.transaccion_id AND t.tipo = 'VENTA' AND t.fecha_delete IS NULL
WHERE p.fecha_delete IS NULL
  AND (? IS NULL OR p.categoria_id = ?)
  AND p.nombre LIKE ?
GROUP BY p.id
ORDER BY popularidad DESC, p.nombre ASC
LIMIT ? OFFSET ?;
```

## 6. Use Cases (`GestionProductos`)

**Archivo:** `src/application/productos/GestionProductos.ts`

| Caso de uso | Firma | Atómico | Precondiciones | Postcondiciones |
|---|---|---|---|---|
| `crearProducto` | `(input: CreateProductoInput) => Producto` | Sí (repo txn) | categoria existe | Producto insertado |
| `actualizarProducto` | `(id, input: UpdateProductoInput) => Producto` | Sí (repo txn) | Producto existe | Producto actualizado, `fecha_update` = ahora |
| `eliminarProducto` | `(id: ProductoId) => void` | Sí (soft delete) | Producto existe | `fecha_delete` = ahora |

## 7. UI State Machines (texto visible en español)

| Estado | Disparador | Siguiente estado | Texto visible (ES) |
|---|---|---|---|
| `FormVacío` | Usuario abre formulario producto | `Editando` | `"Nuevo producto"` / `"Editar producto"` |
| `Editando` | Usuario toca [+] categoría | `ModalCategoriaRapida` | `"Crear categoría"` |
| `ModalCategoriaRapida` | Categoría guardada | `Editando` | Categoría seleccionada en `CategorySelector` |
| `Editando` | Usuario guarda | `Catálogo` | `"Producto guardado"` (Toast) |
| `Cualquiera` | `stockActual < stockMinimo` post-venta | `AlertaStock` | `"Stock bajo: [nombre]"` (Toast no obstructivo) |

## 8. Componentes UI

| Componente | Archivo | Tipo | Memoizado |
|---|---|---|---|
| `ProductGridCard` | `src/presentation/components/product/ProductGridCard.tsx` | UI | Sí (`React.memo`) |
| `ProductFallbackAvatar` | `src/presentation/components/product/ProductFallbackAvatar.tsx` | UI | Sí (`React.memo`) |
| `ProductFormHeader` | `src/presentation/components/product/ProductFormHeader.tsx` | UI | No |
| `CategorySelector` | `src/presentation/components/product/CategorySelector.tsx` | UI | No |
| `QuickCategoryModal` | `src/presentation/components/product/QuickCategoryModal.tsx` | UI | No |

## 9. Hooks

| Hook | Archivo | Responsabilidad |
|---|---|---|
| `useProductos` | `src/presentation/hooks/useProductos.ts` | Catálogo con search, categoría, sort, paginación |
| `useStockAlert` | `src/presentation/hooks/useStockAlert.ts` | Toast si `stockActual < stockMinimo` tras ventas/ediciones |

## 10. Almacenes Zustand

| Store | Archivo | Responsabilidad |
|---|---|---|
| `productoDraftStore` | `src/presentation/stores/productoDraftStore.ts` | Borrador del formulario de producto (sobrevive a `QuickCategoryModal`) |

## 11. NFRs Aplicables
- NFR-01: Offline-first SQLite JSI.
- NFR-05: Imagen 400x400, JPEG 70%, objetivo < 40 KB.
- NFR-06: Dark mode estricto + tokens de diseño en tiempo de ejecución.
- NFR-09: TypeScript estricto.

## 12. Puntos de extensión (pendiente de refinamiento)
- [ ] Validación de `categoria_id` NOT NULL en DB (actualmente `INTEGER` sin NOT NULL en DDL).
- [ ] Endpoint de reabastecimiento de stock por compra.
