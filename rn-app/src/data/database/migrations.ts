import type { SQLiteDatabase } from 'expo-sqlite';

import { SCHEMA_SQL, SEED_SQL } from './schema.sql';
import { CATALOG_INVENTORY_MIGRATION_SQL } from './migrations/v2_0_0_catalog_inventory_migration.sql';
import { SALES_CHECKOUT_MIGRATION_SQL } from './migrations/v2_0_0_sales_checkout_migration.sql';
import {
  FINANCIAL_ACCUMULATORS_COLUMNS_MIGRATION_SQL,
  FINANCIAL_ACCUMULATORS_MIGRATION_SQL,
} from './migrations/v2_0_0_financial_accumulators_migration.sql';
import { EXPENSES_CASHFLOW_MIGRATION_SQL } from './migrations/v2_0_0_expenses_cashflow_migration.sql';
import { SYSTEM_SETTINGS_MIGRATION_SQL } from './migrations/v2_0_0_system_settings_migration.sql';

export const TARGET_VERSION = 2;

interface MigrationStep {
  readonly sql: string;
  readonly shouldRun?: (db: SQLiteDatabase) => boolean;
}

interface Migration {
  readonly version: number;
  readonly steps: ReadonlyArray<MigrationStep>;
}

const hasLegacyCatalogSchema = (db: SQLiteDatabase): boolean => tableExists(db, 'productos');

const hasLegacySalesSchema = (db: SQLiteDatabase): boolean => tableExists(db, 'transacciones');

const hasLegacyFinancialTable = (db: SQLiteDatabase): boolean => tableExists(db, 'resumen_caja');

const hasLegacyFinancialColumns = (db: SQLiteDatabase): boolean =>
  columnExists(db, 'financial_accumulators', 'total_ventas');

const MIGRATIONS: ReadonlyArray<Migration> = [
  {
    version: 1,
    steps: [{ sql: `${SCHEMA_SQL}\n${SEED_SQL}` }],
  },
  {
    version: 2,
    steps: [
      { sql: CATALOG_INVENTORY_MIGRATION_SQL, shouldRun: hasLegacyCatalogSchema },
      { sql: SALES_CHECKOUT_MIGRATION_SQL, shouldRun: hasLegacySalesSchema },
      { sql: FINANCIAL_ACCUMULATORS_MIGRATION_SQL, shouldRun: hasLegacyFinancialTable },
      {
        sql: FINANCIAL_ACCUMULATORS_COLUMNS_MIGRATION_SQL,
        shouldRun: hasLegacyFinancialColumns,
      },
      { sql: EXPENSES_CASHFLOW_MIGRATION_SQL },
      { sql: SYSTEM_SETTINGS_MIGRATION_SQL },
    ],
  },
];

export function runMigrations(
  db: SQLiteDatabase,
  targetVersion: number = TARGET_VERSION,
): void {
  const foreignKeysEnabled = readForeignKeys(db);
  if (foreignKeysEnabled) {
    db.execSync('PRAGMA foreign_keys = OFF;');
  }

  try {
    for (const migration of MIGRATIONS) {
      if (migration.version > targetVersion) break;
      if (!needsMigration(db, migration)) continue;

      db.withTransactionSync(() => {
        for (const step of migration.steps) {
          if (step.shouldRun && !step.shouldRun(db)) continue;
          db.execSync(step.sql);
        }
        db.execSync(`PRAGMA user_version = ${migration.version};`);
      });
    }
  } finally {
    if (foreignKeysEnabled) {
      db.execSync('PRAGMA foreign_keys = ON;');
    }
  }
}

function needsMigration(db: SQLiteDatabase, migration: Migration): boolean {
  if (migration.version > readUserVersion(db)) return true;
  return migration.steps.some((step) => step.shouldRun?.(db) ?? false);
}

function readUserVersion(db: SQLiteDatabase): number {
  const result = db.getFirstSync<{ user_version: number }>('PRAGMA user_version;');
  return result?.user_version ?? 0;
}

function readForeignKeys(db: SQLiteDatabase): boolean {
  const result = db.getFirstSync<{ foreign_keys: number }>('PRAGMA foreign_keys;');
  return result?.foreign_keys === 1;
}

function tableExists(db: SQLiteDatabase, table: string): boolean {
  const row = db.getFirstSync<{ name: string }>(
    "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
    table,
  );
  return row !== null;
}

function columnExists(db: SQLiteDatabase, table: string, column: string): boolean {
  if (!tableExists(db, table)) return false;
  const columns = db.getAllSync<{ name: string }>(`PRAGMA table_info(${table});`);
  return columns.some((entry) => entry.name === column);
}
