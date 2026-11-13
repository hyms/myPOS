export const SCHEMA_SQL = `
CREATE TABLE IF NOT EXISTS categorias (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_delete TIMESTAMP DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS productos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    categoria_id INTEGER,
    nombre TEXT NOT NULL,
    precio_venta REAL NOT NULL,
    precio_compra REAL NOT NULL,
    stock_actual REAL DEFAULT 0.0,
    stock_minimo REAL DEFAULT 0.0,
    imagen_uri TEXT,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_delete TIMESTAMP DEFAULT NULL,
    FOREIGN KEY(categoria_id) REFERENCES categorias(id)
);

CREATE TABLE IF NOT EXISTS transacciones (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tipo TEXT NOT NULL,
    monto_total REAL NOT NULL,
    tipo_pago TEXT NOT NULL,
    detalle TEXT,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_delete TIMESTAMP DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS detalle_transacciones (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    transaccion_id INTEGER NOT NULL,
    producto_id INTEGER NOT NULL,
    cantidad REAL NOT NULL,
    precio_unitario REAL NOT NULL,
    FOREIGN KEY(transaccion_id) REFERENCES transacciones(id),
    FOREIGN KEY(producto_id) REFERENCES productos(id)
);

CREATE TABLE IF NOT EXISTS resumen_caja (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    total_ventas REAL DEFAULT 0.0,
    total_compras REAL DEFAULT 0.0,
    total_gastos REAL DEFAULT 0.0,
    saldo_actual REAL DEFAULT 0.0
);

CREATE INDEX IF NOT EXISTS idx_productos_categoria
  ON productos(categoria_id) WHERE fecha_delete IS NULL;
CREATE INDEX IF NOT EXISTS idx_productos_nombre
  ON productos(nombre) WHERE fecha_delete IS NULL;
CREATE INDEX IF NOT EXISTS idx_productos_delete
  ON productos(fecha_delete) WHERE fecha_delete IS NULL;
CREATE INDEX IF NOT EXISTS idx_transacciones_fecha
  ON transacciones(fecha_registro) WHERE fecha_delete IS NULL;
CREATE INDEX IF NOT EXISTS idx_transacciones_tipo
  ON transacciones(tipo) WHERE fecha_delete IS NULL;
CREATE INDEX IF NOT EXISTS idx_transacciones_fecha_tipo
  ON transacciones(fecha_registro, tipo, fecha_delete);
CREATE INDEX IF NOT EXISTS idx_detalle_trx
  ON detalle_transacciones(transaccion_id);
CREATE INDEX IF NOT EXISTS idx_detalle_producto
  ON detalle_transacciones(producto_id);
CREATE INDEX IF NOT EXISTS idx_detalle_transaccion
  ON detalle_transacciones(transaccion_id);
`;

export const SEED_SQL = `
INSERT OR IGNORE INTO resumen_caja (id, total_ventas, total_compras, total_gastos, saldo_actual)
VALUES (1, 0.0, 0.0, 0.0, 0.0);
`;
