export const SYSTEM_SETTINGS_MIGRATION_SQL = `
-- v2.0.0 · system-settings
-- SQLite singleton settings row. Application settings currently hydrate from
-- AsyncStorage; this table is the v2.0.0 persistence target.

CREATE TABLE IF NOT EXISTS settings (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    currency_code TEXT NOT NULL DEFAULT 'Bs',
    page_size INTEGER NOT NULL DEFAULT 15,
    biometric_enabled INTEGER NOT NULL DEFAULT 0,
    ad_enabled INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT OR IGNORE INTO settings
    (id, currency_code, page_size, biometric_enabled, ad_enabled, updated_at)
VALUES
    (1, 'Bs', 15, 0, 0, CURRENT_TIMESTAMP);

CREATE INDEX IF NOT EXISTS idx_settings_id ON settings(id);
`;
