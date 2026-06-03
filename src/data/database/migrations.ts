import type { SQLiteDatabase } from 'expo-sqlite';

import { SCHEMA_SQL, SEED_SQL } from './schema.sql';

const CURRENT_VERSION = 1;

interface Migration {
  version: number;
  up: (db: SQLiteDatabase) => void;
}

const MIGRATIONS: ReadonlyArray<Migration> = [
  {
    version: 1,
    up: (db) => {
      db.execSync(SCHEMA_SQL);
      db.execSync(SEED_SQL);
    },
  },
];

export function runMigrations(db: SQLiteDatabase): void {
  const currentVersion = readUserVersion(db);
  for (const migration of MIGRATIONS) {
    if (migration.version > currentVersion) {
      db.withTransactionSync(() => {
        migration.up(db);
        db.execSync(`PRAGMA user_version = ${migration.version};`);
      });
    }
  }
}

function readUserVersion(db: SQLiteDatabase): number {
  const result = db.getFirstSync<{ user_version: number }>('PRAGMA user_version;');
  return result?.user_version ?? 0;
}

export const TARGET_VERSION = CURRENT_VERSION;
