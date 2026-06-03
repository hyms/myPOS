import { getDatabase } from '@/data/database/client';
import { SqliteCategoriaRepository } from '@/data/repositories/SqliteCategoriaRepository';
import { SqliteProductoRepository } from '@/data/repositories/SqliteProductoRepository';
import { SqliteResumenCajaRepository } from '@/data/repositories/SqliteResumenCajaRepository';
import { SqliteTransaccionRepository } from '@/data/repositories/SqliteTransaccionRepository';

export interface RepositoryContainer {
  readonly productos: SqliteProductoRepository;
  readonly categorias: SqliteCategoriaRepository;
  readonly transacciones: SqliteTransaccionRepository;
  readonly resumenCaja: SqliteResumenCajaRepository;
}

let containerInstance: RepositoryContainer | null = null;

export function getRepositories(): RepositoryContainer {
  if (!containerInstance) {
    const db = getDatabase();
    containerInstance = {
      productos: new SqliteProductoRepository(db),
      categorias: new SqliteCategoriaRepository(db),
      transacciones: new SqliteTransaccionRepository(db),
      resumenCaja: new SqliteResumenCajaRepository(db),
    };
  }
  return containerInstance;
}

export function resetRepositoriesForTests(): void {
  containerInstance = null;
}
