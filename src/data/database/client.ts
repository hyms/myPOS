import * as SQLite from 'expo-sqlite';
import type { SQLiteDatabase } from 'expo-sqlite';

import { runMigrations } from './migrations';

const DB_NAME = 'tpv.db';

let dbInstance: SQLiteDatabase | null = null;

export function getDatabase(): SQLiteDatabase {
  if (!dbInstance) {
    dbInstance = SQLite.openDatabaseSync(DB_NAME, {
      enableChangeListener: true,
    });
    dbInstance.execSync('PRAGMA journal_mode = WAL;');
    dbInstance.execSync('PRAGMA synchronous = NORMAL;');
    dbInstance.execSync('PRAGMA foreign_keys = ON;');
    runMigrations(dbInstance);
  }
  return dbInstance;
}

export function closeDatabase(): void {
  if (dbInstance) {
    dbInstance.closeSync();
    dbInstance = null;
  }
}

export function resetDatabaseForTests(): void {
  closeDatabase();
}
