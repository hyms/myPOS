package com.copitoB.mypos.application.backup

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.copitoB.mypos.application.backup.BackupAccumulatorsDto
import com.copitoB.mypos.application.backup.BackupCategoryDto
import com.copitoB.mypos.application.backup.BackupDataDto
import com.copitoB.mypos.application.backup.BackupDbClassification
import com.copitoB.mypos.application.backup.BackupDbValidator
import com.copitoB.mypos.application.backup.BackupErrors
import com.copitoB.mypos.application.backup.BackupException
import com.copitoB.mypos.application.backup.BackupExpenseCategoryDto
import com.copitoB.mypos.application.backup.BackupProductDto
import com.copitoB.mypos.application.backup.BackupSettingsDto
import com.copitoB.mypos.application.backup.BackupTransactionDetailDto
import com.copitoB.mypos.application.backup.BackupTransactionDto
import com.copitoB.mypos.application.backup.LegacyMapper
import com.copitoB.mypos.data.repository.TpvDatabase
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement
import java.time.Instant

class JdbcBackupDbValidator : BackupDbValidator {

  private var connection: Connection? = null
  private var openedPath: String? = null
  private var cachedTables: Set<String>? = null
  private var cachedUserVersion: Long = 0L
  private var columnCache: MutableMap<String, Set<String>> = mutableMapOf()

  override fun open(path: String) {
    openedPath = path
    cachedTables = null
    columnCache = mutableMapOf()
    connection = DriverManager.getConnection("jdbc:sqlite:$path")
    connection?.setReadOnly(true)
  }

  override fun classify(): BackupDbClassification {
    val conn = connection ?: throw BackupException(BackupErrors.HEADER_INVALID)

    // Check SQLite header magic
    val header = runCatching {
      val file = File(openedPath!!)
      val raf = RandomAccessFile(file, "r")
      try {
        val bytes = ByteArray(16)
        raf.readFully(bytes)
        String(bytes, StandardCharsets.UTF_8)
      } finally {
        raf.close()
      }
    }.getOrNull()
    if (header != "SQLite format 3\u0000") {
      return BackupDbClassification.INVALID(BackupErrors.HEADER_INVALID)
    }

    // Integrity check
    val integrity = runCatching {
      conn.createStatement().use { stmt ->
        stmt.executeQuery("PRAGMA integrity_check").use { rs ->
          if (rs.next()) rs.getString(1) else null
        }
      }
    }.getOrNull()
    if (integrity != "ok") {
      return BackupDbClassification.INVALID(BackupErrors.INTEGRITY_FAILED)
    }

    // User version
    cachedUserVersion = runCatching {
      conn.createStatement().use { stmt ->
        stmt.executeQuery("PRAGMA user_version").use { rs ->
          if (rs.next()) rs.getLong(1) else 0L
        }
      }
    }.getOrNull() ?: 0L

    // Table list
    cachedTables = runCatching {
      conn.createStatement().use { stmt ->
        stmt.executeQuery("SELECT name FROM sqlite_master WHERE type = 'table'").use { rs ->
          val set = mutableSetOf<String>()
          while (rs.next()) {
            set += rs.getString(1)
          }
          set
        }
      }
    }.getOrNull() ?: emptySet()

    // Required v2 tables
    val requiredV2Tables = setOf(
      "settings", "categories", "products", "sales_transactions",
      "transaction_details", "expense_categories", "financial_accumulators"
    )

    if (requiredV2Tables.all { cachedTables!!.contains(it) }) {
      // Verify required columns exist
      val missingCols = mutableListOf<String>()
      val requiredColumns = mapOf(
        "settings" to setOf("id", "currency_code", "page_size", "biometric_enabled", "ad_enabled", "updated_at"),
        "categories" to setOf("id", "name", "registered_at", "updated_at", "deleted_at"),
        "products" to setOf("id", "category_id", "sku", "name", "selling_price", "purchase_price", "current_stock", "min_stock", "image_uri", "registered_at", "updated_at", "deleted_at"),
        "sales_transactions" to setOf("id", "type", "total_amount", "payment_method", "detail", "registered_at", "deleted_at"),
        "transaction_details" to setOf("id", "transaction_id", "product_id", "quantity", "unit_price"),
        "expense_categories" to setOf("id", "name", "registered_at", "deleted_at"),
        "financial_accumulators" to setOf("id", "total_sales", "total_purchases", "total_expenses", "net_balance", "updated_at")
      )
      for ((table, cols) in requiredColumns) {
        val tableCols = getTableColumns(conn, table)
        val missing = cols - tableCols
        if (missing.isNotEmpty()) {
          missingCols.add("$table: ${missing.joinToString(", ")}")
        }
      }
      if (missingCols.isNotEmpty()) {
        return BackupDbClassification.INVALID(BackupErrors.MISSING_COLUMNS.format(missingCols[0]))
      }
      return BackupDbClassification.V2_CURRENT(cachedUserVersion)
    }

    // Check for v1 legacy tables
    val v1Tables = setOf("categorias", "productos", "transacciones", "detalle_transacciones")
    val v1Found = v1Tables.filter { cachedTables!!.contains(it) }
    if (v1Found.size >= 2) { // At least 2 core tables
      // Verify v1 required columns
      val v1RequiredColumns = mapOf(
        "categorias" to setOf("id", "nombre", "fecha_registro", "fecha_delete"),
        "productos" to setOf("id", "categoria_id", "nombre", "precio_venta", "precio_compra", "stock_actual", "stock_minimo", "imagen_uri", "fecha_registro", "fecha_update", "fecha_delete"),
        "transacciones" to setOf("id", "tipo", "monto_total", "tipo_pago", "detalle", "fecha_registro", "fecha_delete"),
        "detalle_transacciones" to setOf("id", "transaccion_id", "producto_id", "cantidad", "precio_unitario")
      )
      val missingV1 = mutableListOf<String>()
      for (table in v1Found) {
        val tableCols = getTableColumns(conn, table)
        val required = v1RequiredColumns[table] ?: emptySet()
        val missing = required - tableCols
        if (missing.isNotEmpty()) {
          missingV1.add("$table: ${missing.joinToString(", ")}")
        }
      }
      if (missingV1.isNotEmpty()) {
        return BackupDbClassification.INVALID(BackupErrors.MISSING_COLUMNS.format(missingV1[0]))
      }
      return BackupDbClassification.V1_LEGACY(cachedUserVersion)
    }

    // Check for flipped schema (financial_accumulators with legacy column names)
    if (cachedTables!!.contains("financial_accumulators")) {
      val legacyCols = setOf("total_ventas", "total_compras", "total_gastos", "saldo_actual")
      val tableCols = getTableColumns(conn, "financial_accumulators")
      if (legacyCols.any { tableCols.contains(it) }) {
        return BackupDbClassification.INVALID(BackupErrors.SCHEMA_INCOMPLETE.format("financial_accumulators tiene columnas legacy (total_ventas, etc.)"))
      }
    }

    return BackupDbClassification.INVALID(BackupErrors.UNKNOWN_FORMAT)
  }

  private fun getTableColumns(conn: Connection, table: String): Set<String> {
    val cached = columnCache[table]
    if (cached != null) return cached
    val result = runCatching {
      conn.createStatement().use { stmt ->
        stmt.executeQuery("PRAGMA table_info($table)").use { rs ->
          val set = mutableSetOf<String>()
          while (rs.next()) {
            set += rs.getString(2) // column name is at index 2
          }
          set
        }
      }
    }.getOrNull() ?: emptySet()
    columnCache[table] = result
    return result
  }

  override fun stageInto(target: TpvDatabase): StagingResult {
    val conn = connection ?: throw BackupException(BackupErrors.HEADER_INVALID)
    val classification = classify()
    val warnings = mutableListOf<String>()
    var rowsWritten = 0

    return when (classification) {
      is BackupDbClassification.V2_CURRENT -> stageV2(conn, target, warnings)
      is BackupDbClassification.V1_LEGACY -> stageV1(conn, target, warnings)
      else -> throw BackupException(BackupErrors.UNKNOWN_FORMAT)
    }
  }

  private fun queryOne(conn: Connection, sql: String, mapper: (ResultSet) -> Any?): Any? {
    return runCatching {
      conn.createStatement().use { stmt ->
        stmt.executeQuery(sql).use { rs ->
          if (rs.next()) mapper(rs) else null
        }
      }
    }.getOrNull()
  }

  private fun queryList(conn: Connection, sql: String, mapper: (ResultSet) -> Any): List<Any> {
    return runCatching {
      conn.createStatement().use { stmt ->
        stmt.executeQuery(sql).use { rs ->
          val list = mutableListOf<Any>()
          while (rs.next()) {
            list += mapper(rs)
          }
          list
        }
      }
    }.getOrElse { emptyList() }
  }

  private fun stageV2(conn: Connection, target: TpvDatabase, warnings: MutableList<String>): StagingResult {
    var rowsWritten = 0
    target.transaction {
      // Settings
      val settings = queryOne(conn, "SELECT currency_code, page_size, biometric_enabled, ad_enabled FROM settings WHERE id = 1") { rs ->
        val currency = rs.getString(1) ?: "Bs"
        val pageSize = rs.getInt(2)
        val biometric = rs.getInt(3) != 0
        val adEnabled = rs.getInt(4) != 0
        val code = if (currency in listOf("Bs", "$", "€")) currency else "Bs"
        BackupSettingsDto(code, pageSize.coerceIn(5, 100), biometric)
      } as? BackupSettingsDto
      target.backupQueries.backupSettingsUpsert(
        currencyCode = settings?.currencyCode ?: "Bs",
        pageSize = (settings?.pageSize ?: 15).toLong(),
        biometricEnabled = if (settings?.biometricEnabled == true) 1L else 0L,
        updatedAt = Instant.now().toString()
      )
      rowsWritten += 1

      // Categories
      val categories = queryList(conn, "SELECT id, name, registered_at, updated_at, deleted_at FROM categories ORDER BY id ASC") { rs ->
        BackupCategoryDto(
          id = rs.getLong(1),
          name = rs.getString(2),
          registeredAt = rs.getString(3) ?: "",
          updatedAt = rs.getString(4),
          deletedAt = rs.getString(5)
        )
      } as List<BackupCategoryDto>
      for (c in categories) {
        target.backupQueries.backupCategoriesInsertWithId(
          id = c.id,
          name = c.name,
          registeredAt = c.registeredAt,
          updatedAt = c.updatedAt,
          deletedAt = c.deletedAt
        )
        rowsWritten++
      }

      // Expense categories
      val expenseCategories = queryList(conn, "SELECT id, name, registered_at, deleted_at FROM expense_categories ORDER BY id ASC") { rs ->
        BackupExpenseCategoryDto(
          id = rs.getLong(1),
          name = rs.getString(2),
          registeredAt = rs.getString(3) ?: "",
          deletedAt = rs.getString(4)
        )
      } as List<BackupExpenseCategoryDto>
      for (c in expenseCategories) {
        target.backupQueries.backupExpenseCategoriesInsertWithId(
          id = c.id,
          name = c.name,
          registeredAt = c.registeredAt,
          deletedAt = c.deletedAt
        )
        rowsWritten++
      }

      // Products
      val products = queryList(conn, "SELECT id, category_id, sku, name, selling_price, purchase_price, current_stock, min_stock, image_uri, registered_at, updated_at, deleted_at FROM products ORDER BY id ASC") { rs ->
        BackupProductDto(
          id = rs.getLong(1),
          categoryId = rs.getLong(2),
          sku = rs.getString(3),
          name = rs.getString(4),
          sellingPrice = rs.getDouble(5),
          purchasePrice = rs.getDouble(6),
          currentStock = rs.getDouble(7),
          minStock = rs.getDouble(8),
          imageUri = rs.getString(9),
          registeredAt = rs.getString(10) ?: "",
          updatedAt = rs.getString(11) ?: "",
          deletedAt = rs.getString(12)
        )
      } as List<BackupProductDto>
      for (p in products) {
        target.backupQueries.backupProductsInsertWithId(
          id = p.id,
          categoryId = p.categoryId,
          sku = p.sku,
          name = p.name,
          sellingPrice = p.sellingPrice,
          purchasePrice = p.purchasePrice,
          currentStock = p.currentStock,
          minStock = p.minStock,
          imageUri = p.imageUri,
          registeredAt = p.registeredAt,
          updatedAt = p.updatedAt,
          deletedAt = p.deletedAt
        )
        rowsWritten++
      }

      // Transactions
      val transactions = queryList(conn, "SELECT id, type, total_amount, payment_method, detail, registered_at, deleted_at FROM sales_transactions ORDER BY id ASC") { rs ->
        BackupTransactionDto(
          id = rs.getLong(1),
          type = rs.getString(2),
          totalAmount = rs.getDouble(3),
          paymentMethod = rs.getString(4),
          detail = rs.getString(5),
          registeredAt = rs.getString(6) ?: "",
          deletedAt = rs.getString(7)
        )
      } as List<BackupTransactionDto>
      for (t in transactions) {
        target.backupQueries.backupTransactionsInsertWithId(
          id = t.id,
          type = t.type,
          totalAmount = t.totalAmount,
          paymentMethod = t.paymentMethod,
          detail = t.detail,
          registeredAt = t.registeredAt,
          deletedAt = t.deletedAt
        )
        rowsWritten++
      }

      // Transaction details
      val transactionDetails = queryList(conn, "SELECT id, transaction_id, product_id, quantity, unit_price FROM transaction_details ORDER BY id ASC") { rs ->
        BackupTransactionDetailDto(
          id = rs.getLong(1),
          transactionId = rs.getLong(2),
          productId = rs.getLong(3),
          quantity = rs.getDouble(4),
          unitPrice = rs.getDouble(5)
        )
      } as List<BackupTransactionDetailDto>
      for (d in transactionDetails) {
        target.backupQueries.backupTransactionDetailsInsertWithId(
          id = d.id,
          transactionId = d.transactionId,
          productId = d.productId,
          quantity = d.quantity,
          unitPrice = d.unitPrice
        )
        rowsWritten++
      }

      // Accumulators
      val accumulators = queryOne(conn, "SELECT total_sales, total_purchases, total_expenses, net_balance FROM financial_accumulators WHERE id = 1") { rs ->
        BackupAccumulatorsDto(
          totalSales = rs.getDouble(1),
          totalPurchases = rs.getDouble(2),
          totalExpenses = rs.getDouble(3),
          netBalance = rs.getDouble(4)
        )
      } as? BackupAccumulatorsDto
      val acc = accumulators ?: BackupAccumulatorsDto(0.0, 0.0, 0.0, 0.0)
      target.backupQueries.backupAccumulatorsUpsert(
        totalSales = acc.totalSales,
        totalPurchases = acc.totalPurchases,
        totalExpenses = acc.totalExpenses,
        netBalance = acc.netBalance,
        updatedAt = Instant.now().toString()
      )
      rowsWritten++
    }
    return StagingResult(rowsWritten, warnings)
  }

  private fun stageV1(conn: Connection, target: TpvDatabase, warnings: MutableList<String>): StagingResult {
    var rowsWritten = 0

    // Read all v1 data first
    val v1Categories = readV1Categories(conn)
    val v1ExpenseCategories = readV1ExpenseCategories(conn)
    val v1Products = readV1Products(conn)
    val v1Transactions = readV1Transactions(conn)
    val v1TransactionDetails = readV1TransactionDetails(conn)
    val v1Settings = readV1Settings(conn)
    val v1Accumulators = readV1Accumulators(conn)

    // Build valid category IDs set for product remapping
    val validCategoryIds = (v1Categories.map { it.id } + v1ExpenseCategories.map { it.id }).toSet()
    val fallbackCategoryId = v1Categories.firstOrNull()?.id

    target.transaction {
      // Categories
      for (c in v1Categories) {
        target.backupQueries.backupCategoriesInsertWithId(
          id = c.id,
          name = c.name,
          registeredAt = LegacyMapper.normalizeTimestamp(c.fechaRegistro) ?: "",
          updatedAt = LegacyMapper.normalizeTimestamp(c.fechaRegistro),
          deletedAt = LegacyMapper.normalizeTimestamp(c.fechaDelete)
        )
        rowsWritten++
      }

      // Expense categories
      for (c in v1ExpenseCategories) {
        target.backupQueries.backupExpenseCategoriesInsertWithId(
          id = c.id,
          name = c.name,
          registeredAt = LegacyMapper.normalizeTimestamp(c.fechaRegistro) ?: "",
          deletedAt = LegacyMapper.normalizeTimestamp(c.fechaDelete)
        )
        rowsWritten++
      }

      // Settings
      val settings = if (v1Settings != null) {
        val currency = v1Settings.currencyCode
        val pageSize = v1Settings.pageSize
        val biometric = v1Settings.biometricEnabled != 0L
        val code = if (currency in listOf("Bs", "$", "€")) currency else "Bs"
        BackupSettingsDto(code, pageSize.coerceIn(5, 100), biometric)
      } else {
        BackupSettingsDto("Bs", 15, false)
      }
      target.backupQueries.backupSettingsUpsert(
        currencyCode = settings.currencyCode,
        pageSize = settings.pageSize.toLong(),
        biometricEnabled = if (settings.biometricEnabled) 1L else 0L,
        updatedAt = Instant.now().toString()
      )
      rowsWritten++

      // Products - remap null/dangling category_id
      for (p in v1Products) {
        val safeCategoryId = LegacyMapper.remapNullCategory(p.categoriaId, validCategoryIds, fallbackCategoryId)
        target.backupQueries.backupProductsInsertWithId(
          id = p.id,
          categoryId = safeCategoryId,
          sku = null,
          name = p.nombre,
          sellingPrice = p.precioVenta,
          purchasePrice = p.precioCompra,
          currentStock = p.stockActual,
          minStock = p.stockMinimo,
          imageUri = p.imagenUri,
          registeredAt = LegacyMapper.normalizeTimestamp(p.fechaRegistro) ?: "",
          updatedAt = LegacyMapper.normalizeTimestamp(p.fechaUpdate) ?: "",
          deletedAt = LegacyMapper.normalizeTimestamp(p.fechaDelete)
        )
        rowsWritten++
      }

      // Transactions - skip unknown type/payment
      for (t in v1Transactions) {
        val type = LegacyMapper.mapType(t.tipo)
        val method = LegacyMapper.mapPaymentMethod(t.tipoPago)
        if (type == null) {
          warnings += "skipped transaction ${t.id}: unknown type '${t.tipo}'"
          continue
        }
        if (method == null) {
          warnings += "skipped transaction ${t.id}: unknown payment method '${t.tipoPago}'"
          continue
        }
        target.backupQueries.backupTransactionsInsertWithId(
          id = t.id,
          type = type,
          totalAmount = t.montoTotal,
          paymentMethod = method,
          detail = t.detalle,
          registeredAt = LegacyMapper.normalizeTimestamp(t.fechaRegistro) ?: "",
          deletedAt = LegacyMapper.normalizeTimestamp(t.fechaDelete)
        )
        rowsWritten++
      }

      // Transaction details - skip orphans
      val validTransactionIds = v1Transactions.map { it.id }.toSet()
      val validProductIds = v1Products.map { it.id }.toSet()
      for (d in v1TransactionDetails) {
        if (d.transaccionId !in validTransactionIds) {
          warnings += "skipped detail ${d.id}: orphan transaction ${d.transaccionId}"
          continue
        }
        if (d.productoId !in validProductIds) {
          warnings += "skipped detail ${d.id}: orphan product ${d.productoId}"
          continue
        }
        target.backupQueries.backupTransactionDetailsInsertWithId(
          id = d.id,
          transactionId = d.transaccionId,
          productId = d.productoId,
          quantity = d.cantidad,
          unitPrice = d.precioUnitario
        )
        rowsWritten++
      }

      // Accumulators (will be recomputed on import anyway)
      val acc = if (v1Accumulators != null) {
        BackupAccumulatorsDto(
          totalSales = v1Accumulators.totalVentas,
          totalPurchases = v1Accumulators.totalCompras,
          totalExpenses = v1Accumulators.totalGastos,
          netBalance = v1Accumulators.saldoActual
        )
      } else {
        BackupAccumulatorsDto(0.0, 0.0, 0.0, 0.0)
      }
      target.backupQueries.backupAccumulatorsUpsert(
        totalSales = acc.totalSales,
        totalPurchases = acc.totalPurchases,
        totalExpenses = acc.totalExpenses,
        netBalance = acc.netBalance,
        updatedAt = Instant.now().toString()
      )
      rowsWritten++
    }

    if (cachedUserVersion != 1L) {
      warnings += "v1 legacy backup has user_version=${cachedUserVersion} (expected 1)"
    }

    return StagingResult(rowsWritten, warnings)
  }

  override fun close() {
    runCatching { connection?.close() }
    connection = null
    openedPath = null
    cachedTables = null
    columnCache = mutableMapOf()
  }

  /* ---------- v1 reads from JDBC ---------- */

  private data class V1Category(
    val id: Long, val name: String, val fechaRegistro: String?, val fechaDelete: String?
  )
  private data class V1Product(
    val id: Long, val categoriaId: Long, val nombre: String, val precioVenta: Double,
    val precioCompra: Double, val stockActual: Double, val stockMinimo: Double,
    val imagenUri: String?, val fechaRegistro: String?, val fechaUpdate: String?, val fechaDelete: String?
  )
  private data class V1Transaction(
    val id: Long, val tipo: String, val montoTotal: Double, val tipoPago: String,
    val detalle: String?, val fechaRegistro: String?, val fechaDelete: String?
  )
  private data class V1TransactionDetail(
    val id: Long, val transaccionId: Long, val productoId: Long, val cantidad: Double, val precioUnitario: Double
  )
  private data class V1Settings(
    val currencyCode: String, val pageSize: Int, val biometricEnabled: Long
  )
  private data class V1Accumulators(
    val totalVentas: Double, val totalCompras: Double, val totalGastos: Double, val saldoActual: Double
  )

  private fun readV1Categories(conn: Connection): List<V1Category> {
    return queryList(conn, "SELECT id, nombre, fecha_registro, fecha_delete FROM categorias ORDER BY id ASC") { rs ->
      V1Category(
        id = rs.getLong(1),
        name = rs.getString(2),
        fechaRegistro = rs.getString(3),
        fechaDelete = rs.getString(4)
      )
    } as List<V1Category>
  }

  private fun readV1ExpenseCategories(conn: Connection): List<V1Category> {
    return queryList(conn, "SELECT id, nombre, fecha_registro, fecha_delete FROM expense_categories ORDER BY id ASC") { rs ->
      V1Category(
        id = rs.getLong(1),
        name = rs.getString(2),
        fechaRegistro = rs.getString(3),
        fechaDelete = rs.getString(4)
      )
    } as List<V1Category>
  }

  private fun readV1Products(conn: Connection): List<V1Product> {
    return queryList(conn, "SELECT id, categoria_id, nombre, precio_venta, precio_compra, stock_actual, stock_minimo, imagen_uri, fecha_registro, fecha_update, fecha_delete FROM productos ORDER BY id ASC") { rs ->
      V1Product(
        id = rs.getLong(1),
        categoriaId = rs.getLong(2),
        nombre = rs.getString(3),
        precioVenta = rs.getDouble(4),
        precioCompra = rs.getDouble(5),
        stockActual = rs.getDouble(6),
        stockMinimo = rs.getDouble(7),
        imagenUri = rs.getString(8),
        fechaRegistro = rs.getString(9),
        fechaUpdate = rs.getString(10),
        fechaDelete = rs.getString(11)
      )
    } as List<V1Product>
  }

  private fun readV1Transactions(conn: Connection): List<V1Transaction> {
    return queryList(conn, "SELECT id, tipo, monto_total, tipo_pago, detalle, fecha_registro, fecha_delete FROM transacciones ORDER BY id ASC") { rs ->
      V1Transaction(
        id = rs.getLong(1),
        tipo = rs.getString(2),
        montoTotal = rs.getDouble(3),
        tipoPago = rs.getString(4),
        detalle = rs.getString(5),
        fechaRegistro = rs.getString(6),
        fechaDelete = rs.getString(7)
      )
    } as List<V1Transaction>
  }

  private fun readV1TransactionDetails(conn: Connection): List<V1TransactionDetail> {
    return queryList(conn, "SELECT id, transaccion_id, producto_id, cantidad, precio_unitario FROM detalle_transacciones ORDER BY id ASC") { rs ->
      V1TransactionDetail(
        id = rs.getLong(1),
        transaccionId = rs.getLong(2),
        productoId = rs.getLong(3),
        cantidad = rs.getDouble(4),
        precioUnitario = rs.getDouble(5)
      )
    } as List<V1TransactionDetail>
  }

  private fun readV1Settings(conn: Connection): V1Settings? {
    return queryOne(conn, "SELECT currency_code, page_size, biometric_enabled FROM settings WHERE id = 1") { rs ->
      V1Settings(
        currencyCode = rs.getString(1) ?: "Bs",
        pageSize = rs.getInt(2),
        biometricEnabled = rs.getLong(3)
      )
    } as? V1Settings
  }

  private fun readV1Accumulators(conn: Connection): V1Accumulators? {
    return queryOne(conn, "SELECT total_ventas, total_compras, total_gastos, saldo_actual FROM resumen_caja WHERE id = 1") { rs ->
      V1Accumulators(
        totalVentas = rs.getDouble(1),
        totalCompras = rs.getDouble(2),
        totalGastos = rs.getDouble(3),
        saldoActual = rs.getDouble(4)
      )
    } as? V1Accumulators
  }
}