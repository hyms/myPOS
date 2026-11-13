import type { Categoria, CategoriaId, CreateCategoriaInput } from '@/domain/entities/Categoria';

export interface ICategoriaRepository {
  listar(): ReadonlyArray<Categoria>;
  findById(id: CategoriaId): Categoria | null;
  crear(input: CreateCategoriaInput): Categoria;
}
