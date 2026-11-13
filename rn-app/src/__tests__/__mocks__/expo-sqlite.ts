/**
 * expo-sqlite mock backed by better-sqlite3 for real SQL execution.
 */
import Database from 'better-sqlite3';

type Params = readonly unknown[];

interface MockResult {
  lastInsertRowId: number;
  changes: number;
}

export interface SQLiteDatabase {
  execSync: (sql: string) => void;
  runSync: (sql: string, ...params: Params) => MockResult;
  getFirstSync: <T>(sql: string, ...params: Params) => T | null;
  getAllSync: <T>(sql: string, ...params: Params) => T[];
  withTransactionSync: (fn: () => void) => void;
  closeSync: () => void;
}

export function openDatabaseSync(
  _name: string,
  _options?: Record<string, unknown>,
): SQLiteDatabase {
  const db = new Database(':memory:');

  const api: SQLiteDatabase = {
    execSync(sql: string) {
      db.exec(sql);
    },

    runSync(sql: string, ...params: Params): MockResult {
      const stmt = db.prepare(sql);
      const result = stmt.run(...params);
      return {
        lastInsertRowId: Number(result.lastInsertRowid),
        changes: result.changes,
      };
    },

    getFirstSync<T>(sql: string, ...params: Params): T | null {
      const stmt = db.prepare(sql);
      const row = stmt.get(...params);
      return (row as T) ?? null;
    },

    getAllSync<T>(sql: string, ...params: Params): T[] {
      const stmt = db.prepare(sql);
      const rows = stmt.all(...params);
      return rows as T[];
    },

    withTransactionSync(fn: () => void) {
      const txn = db.transaction(fn);
      txn();
    },

    closeSync() {
      db.close();
    },
  };

  return api;
}
