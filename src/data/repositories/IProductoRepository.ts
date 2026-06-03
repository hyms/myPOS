import type {
  CreateProductoInput,
  Producto,
  ProductoId,
  UpdateProductoInput,
} from '@/domain/entities/Producto';
import type { CategoriaId } from '@/domain/entities/Categoria';

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
  eliminar(id: ProductoId): void;
  findByIds(ids: ReadonlyArray<ProductoId>): ReadonlyArray<Producto>;
}
