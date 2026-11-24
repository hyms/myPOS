package com.copitoB.mypos.application.backup

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.copitoB.mypos.application.security.Sha256
import com.copitoB.mypos.application.security.VerifyPin
import com.copitoB.mypos.data.repository.SqliteBackupRepository
import com.copitoB.mypos.data.repository.TpvDatabase
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement

class ImportBackupUseCaseTest {

  private fun codec() = JsonCodec()

  private fun repository(driver: SqlDriver) = SqliteBackupRepository(driver)

  private fun buildUseCase(
    driver: SqlDriver,
    storedPin: String? = null
  ): ImportBackupUseCase {
    val pinStorage = FakePinHashStorage(storedPin?.let { Sha256.hash(it) })
    val verifyPin = VerifyPin(pinStorage)
    return ImportBackupUseCase(
      repository = repository(driver),
      codec = codec(),
      backupFileIo = TestBackupFileIo(driver),
      verifyPin = verifyPin
    )
  }

  private fun sampleData() = BackupDataDto(
    settings = BackupSettingsDto("Bs", 15, false),
    categories = listOf(BackupCategoryDto(1L, "General", "2025-01-15T10:00:00Z")),
    expenseCategories = listOf(BackupExpenseCategoryDto(1L, "General", "2025-01-15T10:00:00Z")),
    products = listOf(
      BackupProductDto(
        id = 1L,
        categoryId = 1L,
        sku = null,
        name = "Widget",
        sellingPrice = 25.0,
        purchasePrice = 10.0,
        currentStock = 10.0,
        minStock = 2.0,
        imageUri = null,
        registeredAt = "2025-01-15T10:00:00Z",
        updatedAt = "2025-01-15T10:00:00Z"
      )
    ),
    transactions = listOf(
      BackupTransactionDto(1L, "SALE", 100.0, "CASH", null, "2025-01-15T11:00:00Z", null),
      BackupTransactionDto(2L, "PURCHASE", 40.0, "CARD", null, "2025-01-15T12:00:00Z", null),
      BackupTransactionDto(3L, "EXPENSE", 10.0, "CASH", null, "2025-01-15T13:00:00Z", null)
    ),
    transactionDetails = listOf(
      BackupTransactionDetailDto(1L, 1L, 1L, 4.0, 25.0)
    ),
    accumulators = BackupAccumulatorsDto(0.0, 0.0, 0.0, 0.0)
  )

  private fun envelopeOf(data: BackupDataDto, schemaVersion: Int = 2, format: String = "tpv-backup") =
    BackupEnvelopeDto(
      format = format,
      schemaVersion = schemaVersion,
      appVersion = "2.0.0",
      exportedAt = "2025-01-15T13:00:00Z",
      source = "kmp",
      data = data
    )

  private fun createV1DbFile(path: String) {
    val driver = JdbcSqliteDriver("jdbc:sqlite:$path")
    try {
      driver.execute(null, "CREATE TABLE categorias (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL, fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP, fecha_delete TIMESTAMP DEFAULT NULL)", 0)
      driver.execute(null, "CREATE TABLE productos (id INTEGER PRIMARY KEY AUTOINCREMENT, categoria_id INTEGER, nombre TEXT NOT NULL, precio_venta REAL NOT NULL, precio_compra REAL NOT NULL, stock_actual REAL DEFAULT 0.0, stock_minimo REAL DEFAULT 0.0, imagen_uri TEXT, fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP, fecha_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP, fecha_delete TIMESTAMP DEFAULT NULL)", 0)
      driver.execute(null, "CREATE TABLE transacciones (id INTEGER PRIMARY KEY AUTOINCREMENT, tipo TEXT NOT NULL, monto_total REAL NOT NULL, tipo_pago TEXT NOT NULL, detalle TEXT, fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP, fecha_delete TIMESTAMP DEFAULT NULL)", 0)
      driver.execute(null, "CREATE TABLE detalle_transacciones (id INTEGER PRIMARY KEY AUTOINCREMENT, transaccion_id INTEGER NOT NULL, producto_id INTEGER NOT NULL, cantidad REAL NOT NULL, precio_unitario REAL NOT NULL)", 0)
      driver.execute(null, "CREATE TABLE resumen_caja (id INTEGER PRIMARY KEY CHECK (id = 1), total_ventas REAL DEFAULT 0.0, total_compras REAL DEFAULT 0.0, total_gastos REAL DEFAULT 0.0, saldo_actual REAL DEFAULT 0.0)", 0)
      driver.execute(null, "CREATE TABLE expense_categories (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL, fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP, fecha_delete TIMESTAMP DEFAULT NULL)", 0)
      driver.execute(null, "CREATE TABLE settings (id INTEGER PRIMARY KEY CHECK (id = 1), currency_code TEXT, page_size INTEGER, biometric_enabled INTEGER)", 0)

      driver.execute(null, "INSERT INTO categorias (nombre, fecha_registro) VALUES ('General', '2025-01-15 10:00:00')", 0)
      driver.execute(null, "INSERT INTO productos (categoria_id, nombre, precio_venta, precio_compra, stock_actual, stock_minimo, imagen_uri, fecha_registro, fecha_update) VALUES (1, 'Widget', 25.0, 10.0, 10.0, 2.0, null, '2025-01-15 10:00:00', '2025-01-15 10:00:00')", 0)
      driver.execute(null, "INSERT INTO transacciones (tipo, monto_total, tipo_pago, detalle, fecha_registro) VALUES ('VENTA', 100.0, 'EFECTIVO', null, '2025-01-15 11:00:00')", 0)
      driver.execute(null, "INSERT INTO detalle_transacciones (transaccion_id, producto_id, cantidad, precio_unitario) VALUES (1, 1, 4.0, 25.0)", 0)
      driver.execute(null, "INSERT INTO resumen_caja (id, total_ventas, total_compras, total_gastos, saldo_actual) VALUES (1, 100.0, 0.0, 0.0, 100.0)", 0)
      driver.execute(null, "INSERT INTO expense_categories (nombre, fecha_registro) VALUES ('General', '2025-01-15 10:00:00')", 0)
      driver.execute(null, "INSERT INTO settings (id, currency_code, page_size, biometric_enabled) VALUES (1, 'Bs', 15, 0)", 0)
      driver.execute(null, "PRAGMA user_version = 1", 0)
    } finally {
      driver.close()
    }
  }

  private fun createV1DbFileWithNullCategory(path: String) {
    val driver = JdbcSqliteDriver("jdbc:sqlite:$path")
    try {
      driver.execute(null, "CREATE TABLE categorias (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL, fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP, fecha_delete TIMESTAMP DEFAULT NULL)", 0)
      driver.execute(null, "CREATE TABLE productos (id INTEGER PRIMARY KEY AUTOINCREMENT, categoria_id INTEGER, nombre TEXT NOT NULL, precio_venta REAL NOT NULL, precio_compra REAL NOT NULL, stock_actual REAL DEFAULT 0.0, stock_minimo REAL DEFAULT 0.0, imagen_uri TEXT, fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP, fecha_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP, fecha_delete TIMESTAMP DEFAULT NULL)", 0)
      driver.execute(null, "CREATE TABLE transacciones (id INTEGER PRIMARY KEY AUTOINCREMENT, tipo TEXT NOT NULL, monto_total REAL NOT NULL, tipo_pago TEXT NOT NULL, detalle TEXT, fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP, fecha_delete TIMESTAMP DEFAULT NULL)", 0)
      driver.execute(null, "CREATE TABLE detalle_transacciones (id INTEGER PRIMARY KEY AUTOINCREMENT, transaccion_id INTEGER NOT NULL, producto_id INTEGER NOT NULL, cantidad REAL NOT NULL, precio_unitario REAL NOT NULL)", 0)
      driver.execute(null, "CREATE TABLE resumen_caja (id INTEGER PRIMARY KEY CHECK (id = 1), total_ventas REAL DEFAULT 0.0, total_compras REAL DEFAULT 0.0, total_gastos REAL DEFAULT 0.0, saldo_actual REAL DEFAULT 0.0)", 0)
      driver.execute(null, "CREATE TABLE expense_categories (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL, fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP, fecha_delete TIMESTAMP DEFAULT NULL)", 0)
      driver.execute(null, "CREATE TABLE settings (id INTEGER PRIMARY KEY CHECK (id = 1), currency_code TEXT, page_size INTEGER, biometric_enabled INTEGER)", 0)

      driver.execute(null, "INSERT INTO categorias (nombre, fecha_registro) VALUES ('General', '2025-01-15 10:00:00')", 0)
      driver.execute(null, "INSERT INTO productos (categoria_id, nombre, precio_venta, precio_compra, stock_actual, stock_minimo, imagen_uri, fecha_registro, fecha_update) VALUES (NULL, 'Widget', 25.0, 10.0, 10.0, 2.0, null, '2025-01-15 10:00:00', '2025-01-15 10:00:00')", 0)
      driver.execute(null, "INSERT INTO transacciones (tipo, monto_total, tipo_pago, detalle, fecha_registro) VALUES ('VENTA', 100.0, 'EFECTIVO', null, '2025-01-15 11:00:00')", 0)
      driver.execute(null, "INSERT INTO detalle_transacciones (transaccion_id, producto_id, cantidad, precio_unitario) VALUES (1, 1, 4.0, 25.0)", 0)
      driver.execute(null, "INSERT INTO resumen_caja (id, total_ventas, total_compras, total_gastos, saldo_actual) VALUES (1, 100.0, 0.0, 0.0, 100.0)", 0)
      driver.execute(null, "INSERT INTO expense_categories (nombre, fecha_registro) VALUES ('General', '2025-01-15 10:00:00')", 0)
      driver.execute(null, "INSERT INTO settings (id, currency_code, page_size, biometric_enabled) VALUES (1, 'Bs', 15, 0)", 0)
      driver.execute(null, "PRAGMA user_version = 1", 0)
    } finally {
      driver.close()
    }
  }

  private fun createV1DbFileWithUnknownType(path: String) {
    val driver = JdbcSqliteDriver("jdbc:sqlite:$path")
    try {
      driver.execute(null, "CREATE TABLE categorias (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL, fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP, fecha_delete TIMESTAMP DEFAULT NULL)", 0)
      driver.execute(null, "CREATE TABLE productos (id INTEGER PRIMARY KEY AUTOINCREMENT, categoria_id INTEGER, nombre TEXT NOT NULL, precio_venta REAL NOT NULL, precio_compra REAL NOT NULL, stock_actual REAL DEFAULT 0.0, stock_minimo REAL DEFAULT 0.0, imagen_uri TEXT, fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP, fecha_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP, fecha_delete TIMESTAMP DEFAULT NULL)", 0)
      driver.execute(null, "CREATE TABLE transacciones (id INTEGER PRIMARY KEY AUTOINCREMENT, tipo TEXT NOT NULL, monto_total REAL NOT NULL, tipo_pago TEXT NOT NULL, detalle TEXT, fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP, fecha_delete TIMESTAMP DEFAULT NULL)", 0)
      driver.execute(null, "CREATE TABLE detalle_transacciones (id INTEGER PRIMARY KEY AUTOINCREMENT, transaccion_id INTEGER NOT NULL, producto_id INTEGER NOT NULL, cantidad REAL NOT NULL, precio_unitario REAL NOT NULL)", 0)
      driver.execute(null, "CREATE TABLE resumen_caja (id INTEGER PRIMARY KEY CHECK (id = 1), total_ventas REAL DEFAULT 0.0, total_compras REAL DEFAULT 0.0, total_gastos REAL DEFAULT 0.0, saldo_actual REAL DEFAULT 0.0)", 0)
      driver.execute(null, "CREATE TABLE expense_categories (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL, fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP, fecha_delete TIMESTAMP DEFAULT NULL)", 0)
      driver.execute(null, "CREATE TABLE settings (id INTEGER PRIMARY KEY CHECK (id = 1), currency_code TEXT, page_size INTEGER, biometric_enabled INTEGER)", 0)

      driver.execute(null, "INSERT INTO categorias (nombre, fecha_registro) VALUES ('General', '2025-01-15 10:00:00')", 0)
      driver.execute(null, "INSERT INTO productos (categoria_id, nombre, precio_venta, precio_compra, stock_actual, stock_minimo, imagen_uri, fecha_registro, fecha_update) VALUES (1, 'Widget', 25.0, 10.0, 10.0, 2.0, null, '2025-01-15 10:00:00', '2025-01-15 10:00:00')", 0)
      driver.execute(null, "INSERT INTO transacciones (tipo, monto_total, tipo_pago, detalle, fecha_registro) VALUES ('UNKNOWN_TYPE', 100.0, 'EFECTIVO', null, '2025-01-15 11:00:00')", 0)
      driver.execute(null, "INSERT INTO transacciones (tipo, monto_total, tipo_pago, detalle, fecha_registro) VALUES ('VENTA', 50.0, 'EFECTIVO', null, '2025-01-15 12:00:00')", 0)
      driver.execute(null, "INSERT INTO detalle_transacciones (transaccion_id, producto_id, cantidad, precio_unitario) VALUES (2, 1, 2.0, 25.0)", 0)
      driver.execute(null, "INSERT INTO resumen_caja (id, total_ventas, total_compras, total_gastos, saldo_actual) VALUES (1, 150.0, 0.0, 0.0, 150.0)", 0)
      driver.execute(null, "INSERT INTO expense_categories (nombre, fecha_registro) VALUES ('General', '2025-01-15 10:00:00')", 0)
      driver.execute(null, "INSERT INTO settings (id, currency_code, page_size, biometric_enabled) VALUES (1, 'Bs', 15, 0)", 0)
      driver.execute(null, "PRAGMA user_version = 1", 0)
    } finally {
      driver.close()
    }
  }

  private fun createV2DbFile(path: String) {
    val driver = JdbcSqliteDriver("jdbc:sqlite:$path")
    try {
      runBlocking { TpvDatabase.Schema.create(driver).await() }
      driver.execute(null, "PRAGMA user_version = 2", 0)

      driver.execute(null, "INSERT INTO categories (name, registered_at, updated_at) VALUES ('General', '2025-01-15T10:00:00Z', '2025-01-15T10:00:00Z')", 0)
      driver.execute(null, "INSERT INTO products (category_id, sku, name, selling_price, purchase_price, current_stock, min_stock, image_uri, registered_at, updated_at) VALUES (1, null, 'Widget', 25.0, 10.0, 10.0, 2.0, null, '2025-01-15T10:00:00Z', '2025-01-15T10:00:00Z')", 0)
      driver.execute(null, "INSERT INTO sales_transactions (type, total_amount, payment_method, detail, registered_at) VALUES ('SALE', 100.0, 'CASH', null, '2025-01-15T11:00:00Z')", 0)
      driver.execute(null, "INSERT INTO transaction_details (transaction_id, product_id, quantity, unit_price) VALUES (1, 1, 4.0, 25.0)", 0)
      driver.execute(null, "INSERT INTO expense_categories (name, registered_at) VALUES ('General', '2025-01-15T10:00:00Z')", 0)
      driver.execute(null, "INSERT INTO financial_accumulators (id, total_sales, total_purchases, total_expenses, net_balance) VALUES (1, 100.0, 0.0, 0.0, 100.0)", 0)
      driver.execute(null, "INSERT INTO settings (id, currency_code, page_size, biometric_enabled, ad_enabled, updated_at) VALUES (1, 'Bs', 15, 0, 0, '2025-01-15T10:00:00Z')", 0)
    } finally {
      driver.close()
    }
  }

  private fun createV2DbFileMissingColumns(path: String) {
    val driver = JdbcSqliteDriver("jdbc:sqlite:$path")
    try {
      driver.execute(null, "CREATE TABLE settings (id INTEGER PRIMARY KEY CHECK (id = 1), currency_code TEXT NOT NULL DEFAULT 'Bs', page_size INTEGER NOT NULL DEFAULT 15, biometric_enabled INTEGER NOT NULL DEFAULT 0, updated_at TEXT DEFAULT CURRENT_TIMESTAMP)", 0)
      driver.execute(null, "CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, registered_at TEXT DEFAULT CURRENT_TIMESTAMP, deleted_at TEXT DEFAULT NULL)", 0)
      driver.execute(null, "CREATE TABLE products (id INTEGER PRIMARY KEY AUTOINCREMENT, category_id INTEGER NOT NULL, sku TEXT UNIQUE, name TEXT NOT NULL, selling_price REAL NOT NULL, purchase_price REAL NOT NULL, current_stock REAL DEFAULT 0.0, min_stock REAL DEFAULT 0.0, image_uri TEXT, registered_at TEXT DEFAULT CURRENT_TIMESTAMP, updated_at TEXT DEFAULT CURRENT_TIMESTAMP, deleted_at TEXT DEFAULT NULL, FOREIGN KEY(category_id) REFERENCES categories(id))", 0)
      driver.execute(null, "CREATE TABLE sales_transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL, total_amount REAL NOT NULL, payment_method TEXT NOT NULL, detail TEXT, registered_at TEXT DEFAULT CURRENT_TIMESTAMP, deleted_at TEXT DEFAULT NULL)", 0)
      driver.execute(null, "CREATE TABLE transaction_details (id INTEGER PRIMARY KEY AUTOINCREMENT, transaction_id INTEGER NOT NULL, product_id INTEGER NOT NULL, quantity REAL NOT NULL, unit_price REAL NOT NULL, FOREIGN KEY(transaction_id) REFERENCES sales_transactions(id), FOREIGN KEY(product_id) REFERENCES products(id))", 0)
      driver.execute(null, "CREATE TABLE expense_categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, registered_at TEXT DEFAULT CURRENT_TIMESTAMP, deleted_at TEXT DEFAULT NULL)", 0)
      driver.execute(null, "CREATE TABLE financial_accumulators (id INTEGER PRIMARY KEY CHECK (id = 1), total_sales REAL NOT NULL DEFAULT 0.0, total_purchases REAL NOT NULL DEFAULT 0.0, total_expenses REAL NOT NULL DEFAULT 0.0, net_balance REAL NOT NULL DEFAULT 0.0, updated_at TEXT DEFAULT CURRENT_TIMESTAMP)", 0)

      driver.execute(null, "INSERT INTO categories (name, registered_at) VALUES ('General', '2025-01-15T10:00:00Z')", 0)
      driver.execute(null, "INSERT INTO products (category_id, name, selling_price, purchase_price, current_stock, min_stock, registered_at, updated_at) VALUES (1, 'Widget', 25.0, 10.0, 10.0, 2.0, '2025-01-15T10:00:00Z', '2025-01-15T10:00:00Z')", 0)
      driver.execute(null, "INSERT INTO sales_transactions (type, total_amount, payment_method, registered_at) VALUES ('SALE', 100.0, 'CASH', '2025-01-15T11:00:00Z')", 0)
      driver.execute(null, "INSERT INTO transaction_details (transaction_id, product_id, quantity, unit_price) VALUES (1, 1, 4.0, 25.0)", 0)
      driver.execute(null, "INSERT INTO expense_categories (name, registered_at) VALUES ('General', '2025-01-15T10:00:00Z')", 0)
      driver.execute(null, "INSERT INTO financial_accumulators (id, total_sales, total_purchases, total_expenses, net_balance) VALUES (1, 100.0, 0.0, 0.0, 100.0)", 0)
      driver.execute(null, "INSERT INTO settings (id, currency_code, page_size, biometric_enabled, updated_at) VALUES (1, 'Bs', 15, 0, '2025-01-15T10:00:00Z')", 0)
      driver.execute(null, "PRAGMA user_version = 2", 0)
    } finally {
      driver.close()
    }
  }

  private fun createFlippedSchemaDbFile(path: String) {
    val driver = JdbcSqliteDriver("jdbc:sqlite:$path")
    try {
      driver.execute(null, "CREATE TABLE settings (id INTEGER PRIMARY KEY CHECK (id = 1), currency_code TEXT NOT NULL DEFAULT 'Bs', page_size INTEGER NOT NULL DEFAULT 15, biometric_enabled INTEGER NOT NULL DEFAULT 0, ad_enabled INTEGER NOT NULL DEFAULT 0, updated_at TEXT DEFAULT CURRENT_TIMESTAMP)", 0)
      driver.execute(null, "CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, registered_at TEXT DEFAULT CURRENT_TIMESTAMP, updated_at TEXT DEFAULT CURRENT_TIMESTAMP, deleted_at TEXT DEFAULT NULL)", 0)
      driver.execute(null, "CREATE TABLE products (id INTEGER PRIMARY KEY AUTOINCREMENT, category_id INTEGER NOT NULL, sku TEXT UNIQUE, name TEXT NOT NULL, selling_price REAL NOT NULL, purchase_price REAL NOT NULL, current_stock REAL DEFAULT 0.0, min_stock REAL DEFAULT 0.0, image_uri TEXT, registered_at TEXT DEFAULT CURRENT_TIMESTAMP, updated_at TEXT DEFAULT CURRENT_TIMESTAMP, deleted_at TEXT DEFAULT NULL, FOREIGN KEY(category_id) REFERENCES categories(id))", 0)
      driver.execute(null, "CREATE TABLE sales_transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL, total_amount REAL NOT NULL, payment_method TEXT NOT NULL, detail TEXT, registered_at TEXT DEFAULT CURRENT_TIMESTAMP, deleted_at TEXT DEFAULT NULL)", 0)
      driver.execute(null, "CREATE TABLE transaction_details (id INTEGER PRIMARY KEY AUTOINCREMENT, transaction_id INTEGER NOT NULL, product_id INTEGER NOT NULL, quantity REAL NOT NULL, unit_price REAL NOT NULL, FOREIGN KEY(transaction_id) REFERENCES sales_transactions(id), FOREIGN KEY(product_id) REFERENCES products(id))", 0)
      driver.execute(null, "CREATE TABLE expense_categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, registered_at TEXT DEFAULT CURRENT_TIMESTAMP, deleted_at TEXT DEFAULT NULL)", 0)
      driver.execute(null, "CREATE TABLE financial_accumulators (id INTEGER PRIMARY KEY CHECK (id = 1), total_ventas REAL NOT NULL DEFAULT 0.0, total_compras REAL NOT NULL DEFAULT 0.0, total_gastos REAL NOT NULL DEFAULT 0.0, saldo_actual REAL NOT NULL DEFAULT 0.0, updated_at TEXT DEFAULT CURRENT_TIMESTAMP)", 0)

      driver.execute(null, "INSERT INTO settings (id, currency_code, page_size, biometric_enabled, ad_enabled) VALUES (1, 'Bs', 15, 0, 0)", 0)
      driver.execute(null, "PRAGMA user_version = 2", 0)
    } finally {
      driver.close()
    }
  }

  private fun createCorruptDbFile(path: String) {
    File(path).writeBytes(byteArrayOf(0xCA, 0xFE, 0xBA, 0xBE, 0x01, 0x02, 0x03, 0x04))
  }

  @Test
  fun `imports JSON and normalizes legacy enums and timestamps`() {
    val driver = TestDb.createDriver()
    val repo = repository(driver)
    val useCase = buildUseCase(driver)

    val bytes = codec().encode(envelopeOf(sampleData()))
    val report = useCase(BackupSource.Json(bytes), "")

    assertEquals(1 + 1 + 1 + 3 + 1, report.inserted)

    val exported = repo.exportAll()
    val types = exported.transactions.map { it.type }
    assertEquals(listOf("SALE", "PURCHASE", "EXPENSE"), types)
    val methods = exported.transactions.map { it.paymentMethod }
    assertEquals(listOf("CASH", "CARD", "CASH"), methods)
    assertEquals("2025-01-15T11:00:00Z", exported.transactions[0].registeredAt)
  }

  @Test
  fun `recomputes accumulators from transactions, never trusting the file`() {
    val driver = TestDb.createDriver()
    val repo = repository(driver)
    val useCase = buildUseCase(driver)

    val data = sampleData().copy(accumulators = BackupAccumulatorsDto(999.0, 999.0, 999.0, 999.0))
    useCase(BackupSource.Json(codec().encode(envelopeOf(data))), "")

    val acc = repo.exportAll().accumulators
    assertEquals(100.0, acc.totalSales, 0.0001)
    assertEquals(40.0, acc.totalPurchases, 0.0001)
    assertEquals(10.0, acc.totalExpenses, 0.0001)
    assertEquals(50.0, acc.netBalance, 0.0001)
  }

  @Test
  fun `import is idempotent`() {
    val driver = TestDb.createDriver()
    val repo = repository(driver)
    val useCase = buildUseCase(driver)
    val bytes = codec().encode(envelopeOf(sampleData()))

    useCase(BackupSource.Json(bytes), "")
    val first = repo.exportAll()
    useCase(BackupSource.Json(bytes), "")
    val second = repo.exportAll()

    assertEquals(first, second)
  }

  @Test
  fun `requires correct PIN when configured`() {
    val driver = TestDb.createDriver()
    val useCase = buildUseCase(driver, storedPin = "1234")
    val bytes = codec().encode(envelopeOf(sampleData()))

    assertFailsWith<Throwable> { useCase(BackupSource.Json(bytes), "0000") }
  }

  @Test
  fun `allows import without PIN when none configured`() {
    val driver = TestDb.createDriver()
    val repo = repository(driver)
    val useCase = buildUseCase(driver, storedPin = null)

    val report = useCase(BackupSource.Json(codec().encode(envelopeOf(sampleData()))), "")
    assertTrue(report.inserted > 0)
    assertTrue(repo.exportAll().categories.isNotEmpty())
  }

  @Test
  fun `rejects wrong envelope format`() {
    val driver = TestDb.createDriver()
    val useCase = buildUseCase(driver)
    val bytes = codec().encode(envelopeOf(sampleData(), format = "something-else"))

    val ex = assertFailsWith<BackupException> { useCase(BackupSource.Json(bytes), "") }
    assertTrue(ex.message!!.contains("formato"))
  }

  @Test
  fun `rejects newer schema version`() {
    val driver = TestDb.createDriver()
    val useCase = buildUseCase(driver)
    val bytes = codec().encode(envelopeOf(sampleData(), schemaVersion = 3))

    val ex = assertFailsWith<BackupException> { useCase(BackupSource.Json(bytes), "") }
    assertTrue(ex.message!!.contains("versión más nueva"))
  }

  @Test
  fun `unknown transaction rows are skipped with a warning`() {
    val driver = TestDb.createDriver()
    val repo = repository(driver)
    val useCase = buildUseCase(driver)

    val data = sampleData().copy(
      transactions = sampleData().transactions + BackupTransactionDto(
        99L, "WEIRD", 5.0, "CASH", null, "2025-01-15T14:00:00Z", null
      )
    )
    val report = useCase(BackupSource.Json(codec().encode(envelopeOf(data))), "")

    assertEquals(1, report.skipped)
    assertTrue(report.warnings.any { it.startsWith("skipped") })
    assertEquals(3, repo.exportAll().transactions.size)
  }

  @Test
  fun `failed import rolls back and preserves prior data`() {
    val driver = TestDb.createDriver(foreignKeys = true)
    val repo = repository(driver)
    val useCase = buildUseCase(driver)

    val good = sampleData()
    useCase(BackupSource.Json(codec().encode(envelopeOf(good))), "")
    val before = repo.exportAll()

    val bad = good.copy(
      transactionDetails = good.transactionDetails + BackupTransactionDetailDto(
        2L, 1L, 999L, 1.0, 25.0
      )
    )
    assertFailsWith<Throwable> { useCase(BackupSource.Json(codec().encode(envelopeOf(bad))), "") }

    val after = repo.exportAll()
    assertEquals(before, after)
    assertEquals(1, after.transactionDetails.size)
  }

  @Test
  fun `settings are persisted with sanitized values`() {
    val driver = TestDb.createDriver()
    val repo = repository(driver)
    val useCase = buildUseCase(driver)

    val data = sampleData().copy(settings = BackupSettingsDto("USX", 999, true))
    useCase(BackupSource.Json(codec().encode(envelopeOf(data))), "")

    val settings = repo.exportAll().settings
    assertEquals("Bs", settings.currencyCode)
    assertEquals(100, settings.pageSize)
    assertEquals(true, settings.biometricEnabled)
    assertNotNull(settings)
  }

  @Test
  fun `imports v1 legacy db into v2 schema`() {
    val tempFile = File.createTempFile("v1-backup-test", ".db")
    tempFile.deleteOnExit()
    createV1DbFile(tempFile.absolutePath)

    val driver = TestDb.createDriver()
    val repo = repository(driver)
    val useCase = buildUseCase(driver)

    // Pre-seed some data to verify it gets replaced
    repo.importAll(sampleData())
    val before = repo.exportAll()
    assertTrue(before.categories.isNotEmpty())

    val report = useCase(BackupSource.LegacySqliteFile(tempFile.absolutePath), "")

    val after = repo.exportAll()
    assertEquals(1, after.categories.size)
    assertEquals("General", after.categories[0].name)
    assertEquals(1, after.products.size)
    assertEquals("Widget", after.products[0].name)
    assertEquals(25.0, after.products[0].sellingPrice, 0.0001)
    assertEquals(1, after.transactions.size)
    assertEquals("SALE", after.transactions[0].type)
    assertEquals("CASH", after.transactions[0].paymentMethod)
    assertEquals(1, after.transactionDetails.size)
    assertEquals(100.0, after.accumulators.totalSales, 0.0001)
    assertEquals(100.0, after.accumulators.netBalance, 0.0001)
    assertTrue(report.inserted > 0)
    // Check user_version warning
    assertTrue(report.warnings.any { it.contains("user_version") })
  }

  @Test
  fun `imports v1 legacy db with null category_id remaps to fallback`() {
    val tempFile = File.createTempFile("v1-backup-null-cat", ".db")
    tempFile.deleteOnExit()
    createV1DbFileWithNullCategory(tempFile.absolutePath)

    val driver = TestDb.createDriver()
    val repo = repository(driver)
    val useCase = buildUseCase(driver)

    val report = useCase(BackupSource.LegacySqliteFile(tempFile.absolutePath), "")

    val after = repo.exportAll()
    assertEquals(1, after.products.size)
    assertEquals(1L, after.products[0].categoryId)
    assertTrue(report.warnings.any { it.contains("remap") || it.contains("fallback") || it.contains("null") })
  }

  @Test
  fun `imports v1 legacy db skips unknown transaction type`() {
    val tempFile = File.createTempFile("v1-backup-unknown-type", ".db")
    tempFile.deleteOnExit()
    createV1DbFileWithUnknownType(tempFile.absolutePath)

    val driver = TestDb.createDriver()
    val repo = repository(driver)
    val useCase = buildUseCase(driver)

    val report = useCase(BackupSource.LegacySqliteFile(tempFile.absolutePath), "")

    val after = repo.exportAll()
    assertEquals(1, after.transactions.size) // Only the valid VENTA
    assertEquals("SALE", after.transactions[0].type)
    assertTrue(report.warnings.any { it.contains("skipped transaction") && it.contains("UNKNOWN_TYPE") })
  }

  @Test
  fun `imports v2 db backup into active database`() {
    val tempFile = File.createTempFile("v2-backup-test", ".db")
    tempFile.deleteOnExit()
    createV2DbFile(tempFile.absolutePath)

    val driver = TestDb.createDriver()
    val repo = repository(driver)
    val useCase = buildUseCase(driver)

    // Pre-seed some data to verify it gets replaced
    repo.importAll(sampleData())
    val before = repo.exportAll()
    assertTrue(before.categories.isNotEmpty())

    val report = useCase(BackupSource.LegacySqliteFile(tempFile.absolutePath), "")

    val after = repo.exportAll()
    assertEquals(1, after.categories.size)
    assertEquals("General", after.categories[0].name)
    assertEquals(1, after.products.size)
    assertEquals("Widget", after.products[0].name)
    assertEquals(1, after.transactions.size)
    assertEquals("SALE", after.transactions[0].type)
    assertEquals("CASH", after.transactions[0].paymentMethod)
    assertEquals(1, after.transactionDetails.size)
    assertTrue(report.inserted > 0)
  }

  @Test
  fun `rejects v2 db missing required columns`() {
    val tempFile = File.createTempFile("v2-backup-missing-cols", ".db")
    tempFile.deleteOnExit()
    createV2DbFileMissingColumns(tempFile.absolutePath)

    val driver = TestDb.createDriver()
    val repo = repository(driver)
    val useCase = buildUseCase(driver)

    repo.importAll(sampleData())
    val before = repo.exportAll()

    val ex = assertFailsWith<BackupException> {
      useCase(BackupSource.LegacySqliteFile(tempFile.absolutePath), "")
    }
    assertTrue(ex.message!!.contains("columnas requeridas"))

    val after = repo.exportAll()
    assertEquals(before.categories, after.categories)
    assertEquals(before.products, after.products)
  }

  @Test
  fun `rejects flipped schema db with legacy column names`() {
    val tempFile = File.createTempFile("flipped-schema-test", ".db")
    tempFile.deleteOnExit()
    createFlippedSchemaDbFile(tempFile.absolutePath)

    val driver = TestDb.createDriver()
    val repo = repository(driver)
    val useCase = buildUseCase(driver)

    repo.importAll(sampleData())
    val before = repo.exportAll()

    val ex = assertFailsWith<BackupException> {
      useCase(BackupSource.LegacySqliteFile(tempFile.absolutePath), "")
    }
    assertTrue(ex.message!!.contains("legacy") || ex.message!!.contains("incompleto"))

    val after = repo.exportAll()
    assertEquals(before.categories, after.categories)
    assertEquals(before.products, after.products)
  }

  @Test
  fun `rejects corrupt db file without losing existing data`() {
    val tempFile = File.createTempFile("corrupt-backup-test", ".db")
    tempFile.deleteOnExit()
    createCorruptDbFile(tempFile.absolutePath)

    val driver = TestDb.createDriver()
    val repo = repository(driver)
    val useCase = buildUseCase(driver)

    repo.importAll(sampleData())
    val before = repo.exportAll()

    val ex = assertFailsWith<BackupException> {
      useCase(BackupSource.LegacySqliteFile(tempFile.absolutePath), "")
    }
    assertTrue(ex.message!!.isNotEmpty())

    val after = repo.exportAll()
    assertEquals(before.categories, after.categories)
    assertEquals(before.products, after.products)
    assertEquals(before.transactions, after.transactions)
  }

  @Test
  fun `rejects unknown db format without losing existing data`() {
    val tempFile = File.createTempFile("unknown-backup-test", ".db")
    tempFile.deleteOnExit()
    // Create a valid SQLite file with no recognized tables
    val driver = JdbcSqliteDriver("jdbc:sqlite:${tempFile.absolutePath}")
    try {
      driver.execute(null, "CREATE TABLE foo (id INTEGER)", 0)
      driver.execute(null, "PRAGMA user_version = 99", 0)
    } finally {
      driver.close()
    }

    val activeDriver = TestDb.createDriver()
    val repo = repository(activeDriver)
    val useCase = buildUseCase(activeDriver)

    repo.importAll(sampleData())
    val before = repo.exportAll()

    val ex = assertFailsWith<BackupException> {
      useCase(BackupSource.LegacySqliteFile(tempFile.absolutePath), "")
    }
    assertTrue(ex.message!!.isNotEmpty())

    val after = repo.exportAll()
    assertEquals(before.categories, after.categories)
    assertEquals(before.products, after.products)
    assertEquals(before.transactions, after.transactions)
  }

  @Test
  fun `rejects v1 db with missing required v1 columns`() {
    val tempFile = File.createTempFile("v1-backup-missing-cols", ".db")
    tempFile.deleteOnExit()
    val driver = JdbcSqliteDriver("jdbc:sqlite:${tempFile.absolutePath}")
    try {
      driver.execute(null, "CREATE TABLE categorias (id INTEGER PRIMARY KEY, nombre TEXT)", 0)
      driver.execute(null, "PRAGMA user_version = 1", 0)
    } finally {
      driver.close()
    }

    val activeDriver = TestDb.createDriver()
    val repo = repository(activeDriver)
    val useCase = buildUseCase(activeDriver)

    repo.importAll(sampleData())
    val before = repo.exportAll()

    val ex = assertFailsWith<BackupException> {
      useCase(BackupSource.LegacySqliteFile(tempFile.absolutePath), "")
    }
    assertTrue(ex.message!!.contains("columnas requeridas"))

    val after = repo.exportAll()
    assertEquals(before.categories, after.categories)
  }
}