export const CATALOG_INVENTORY_MIGRATION_SQL = `
-- v2.0.0 · catalog-inventory
-- Renames the legacy Spanish catalog schema to English identifiers.

ALTER TABLE categorias RENAME TO categories;
ALTER TABLE categories RENAME COLUMN nombre TO name;
ALTER TABLE categories RENAME COLUMN fecha_registro TO registered_at;
ALTER TABLE categories RENAME COLUMN fecha_delete TO deleted_at;

ALTER TABLE productos RENAME TO products;
ALTER TABLE products RENAME COLUMN categoria_id TO category_id;
ALTER TABLE products RENAME COLUMN nombre TO name;
ALTER TABLE products RENAME COLUMN precio_venta TO selling_price;
ALTER TABLE products RENAME COLUMN precio_compra TO purchase_price;
ALTER TABLE products RENAME COLUMN stock_actual TO current_stock;
ALTER TABLE products RENAME COLUMN stock_minimo TO min_stock;
ALTER TABLE products RENAME COLUMN imagen_uri TO image_uri;
ALTER TABLE products RENAME COLUMN fecha_registro TO registered_at;
ALTER TABLE products RENAME COLUMN fecha_update TO updated_at;
ALTER TABLE products RENAME COLUMN fecha_delete TO deleted_at;

DROP INDEX IF EXISTS idx_productos_categoria;
DROP INDEX IF EXISTS idx_productos_nombre;
DROP INDEX IF EXISTS idx_productos_delete;

-- INV-05: every product must belong to a category. Legacy rows with a NULL
-- category receive the oldest available category, creating "General" only when
-- there is no category at all.
INSERT INTO categories (name)
SELECT 'General'
 WHERE EXISTS (SELECT 1 FROM products WHERE category_id IS NULL)
   AND NOT EXISTS (SELECT 1 FROM categories);

UPDATE products
   SET category_id = (SELECT MIN(id) FROM categories)
 WHERE category_id IS NULL;

-- SQLite cannot add a NOT NULL constraint in place, so rebuild the table.
CREATE TABLE products_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    category_id INTEGER NOT NULL,
    sku TEXT,
    name TEXT NOT NULL,
    selling_price REAL NOT NULL,
    purchase_price REAL NOT NULL,
    current_stock REAL DEFAULT 0.0,
    min_stock REAL DEFAULT 0.0,
    image_uri TEXT,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL,
    FOREIGN KEY(category_id) REFERENCES categories(id)
);

INSERT INTO products_new (
    id, category_id, sku, name, selling_price, purchase_price,
    current_stock, min_stock, image_uri, registered_at, updated_at, deleted_at
)
SELECT id, category_id, NULL, name, selling_price, purchase_price,
       current_stock, min_stock, image_uri, registered_at, updated_at, deleted_at
  FROM products;

DROP TABLE products;
ALTER TABLE products_new RENAME TO products;

CREATE INDEX IF NOT EXISTS idx_products_category
  ON products(category_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_products_name
  ON products(name) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_products_sku
  ON products(sku) WHERE deleted_at IS NULL AND sku IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_products_deleted_at
  ON products(deleted_at) WHERE deleted_at IS NULL;
`;
