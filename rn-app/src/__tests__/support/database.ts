import { openDatabaseSync, type SQLiteDatabase } from 'expo-sqlite';

import { runMigrations } from '@/data/database/migrations';

export function createMigratedDatabase(): SQLiteDatabase {
  const db = openDatabaseSync(':memory:');
  runMigrations(db);
  return db;
}

export function createLegacyDatabase(): SQLiteDatabase {
  const db = openDatabaseSync(':memory:');
  runMigrations(db, 1);
  return db;
}
