import type { Categoria, CreateCategoriaInput } from '@/domain/entities/Categoria';

export interface CategoryRowShape {
  readonly id: number;
  readonly nombre: string;
  readonly fechaRegistro: string;
  readonly fechaDelete: string | null;
}

export function normalizeCategoryName(name: string): string {
  const trimmed = name.trim();
  if (trimmed.length === 0) {
    throw new Error('El nombre de la categoría es obligatorio.');
  }
  return trimmed;
}

export function createCategory(input: CreateCategoriaInput): { readonly name: string } {
  return { name: normalizeCategoryName(input.nombre) };
}

export function hydrateCategory(row: CategoryRowShape): Categoria {
  return {
    id: row.id,
    nombre: row.nombre,
    fechaRegistro: row.fechaRegistro,
    fechaDelete: row.fechaDelete,
  };
}
