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
