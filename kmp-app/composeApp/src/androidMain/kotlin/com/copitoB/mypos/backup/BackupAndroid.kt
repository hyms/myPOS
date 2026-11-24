package com.copitoB.mypos.application.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.OpenableColumns
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import java.nio.charset.StandardCharsets
import com.copitoB.mypos.application.backup.BackupAccumulatorsDto
import com.copitoB.mypos.application.backup.BackupCategoryDto
import com.copitoB.mypos.application.backup.BackupDataDto
import com.copitoB.mypos.application.backup.BackupDbClassification
import com.copitoB.mypos.application.backup.BackupDbValidator
import com.copitoB.mypos.application.backup.BackupErrors
import com.copitoB.mypos.application.backup.BackupException
import com.copitoB.mypos.application.backup.BackupExpenseCategoryDto
import com.copitoB.mypos.application.backup.BackupFileIo
import com.copitoB.mypos.application.backup.BackupProductDto
import com.copitoB.mypos.application.backup.BackupSettingsDto
import com.copitoB.mypos.application.backup.BackupSource
import com.copitoB.mypos.application.backup.BackupTransactionDetailDto
import com.copitoB.mypos.application.backup.BackupTransactionDto
import com.copitoB.mypos.application.backup.LegacyMapper
import com.copitoB.mypos.data.repository.TpvDatabase
import java.io.File

private fun android.database.Cursor.getStringOrNull(index: Int): String? =
  if (isNull(index)) null else getString(index)

private fun android.database.Cursor.getIntOrDefault(index: Int, default: Int): Int =
  if (isNull(index)) default else getInt(index)

private fun android.database.Cursor.getLongOrDefault(index: Int, default: Long): Long =
  if (isNull(index)) default else getLong(index)

private fun android.database.Cursor.getDoubleOrDefault(index: Int, default: Double): Double =
  if (isNull(index)) default else getDouble(index)

class AndroidBackupDbValidator : BackupDbValidator {

  private var db: SQLiteDatabase? = null
  private var openedPath: String? = null
  private var cachedTables: Set<String>? = null
  private var cachedUserVersion: Long = 0L
  private var columnCache: MutableMap<String, Set<String>> = mutableMapOf()

  override fun open(path: String) {
    db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)
    openedPath = path
    cachedTables = null
    columnCache = mutableMapOf()
  }

  override fun classify(): BackupDbClassification {
    val database = db ?: throw BackupException(BackupErrors.HEADER_INVALID)

    // Check SQLite header magic
    val header = runCatching {
      val file = File(openedPath!!)
      val raf = java.io.RandomAccessFile(file, "r")
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
      database.rawQuery("PRAGMA integrity_check", null).use { c ->
        if (c.moveToFirst()) c.getString(0) else null
      }
    }.getOrNull()
    if (integrity != "ok") {
      return BackupDbClassification.INVALID(BackupErrors.INTEGRITY_FAILED)
    }

    // User version
    cachedUserVersion = runCatching {
      database.rawQuery("PRAGMA user_version", null).use { c ->
        if (c.moveToFirst()) c.getLong(0) else 0L
      }
    }.getOrNull() ?: 0L

    // Table list
    cachedTables = runCatching {
      database.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table'", null).use { c ->
        val set = mutableSetOf<String>()
        while (c.moveToNext()) {
          set += c.getString(0)
        }
        set
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
        val tableCols = getTableColumns(database, table)
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
        val tableCols = getTableColumns(database, table)
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
      val tableCols = getTableColumns(database, "financial_accumulators")
      if (legacyCols.any { tableCols.contains(it) }) {
        return BackupDbClassification.INVALID(BackupErrors.SCHEMA_INCOMPLETE.format("financial_accumulators tiene columnas legacy (total_ventas, etc.)"))
      }
    }

    return BackupDbClassification.INVALID(BackupErrors.UNKNOWN_FORMAT)
  }

  private fun getTableColumns(database: SQLiteDatabase, table: String): Set<String> {
    val cached = columnCache[table]
    if (cached != null) return cached
    val result = runCatching {
      database.rawQuery("PRAGMA table_info($table)", null).use { c ->
        val set = mutableSetOf<String>()
        while (c.moveToNext()) {
          set += c.getString(1)
        }
        set
      }
    }.getOrNull() ?: emptySet()
    columnCache[table] = result
    return result
  }

  override fun stageInto(target: TpvDatabase): StagingResult {
    val database = db ?: throw BackupException(BackupErrors.HEADER_INVALID)
    val classification = classify()
    val warnings = mutableListOf<String>()
    var rowsWritten = 0

    return when (classification) {
      is BackupDbClassification.V2_CURRENT -> stageV2(database, target, warnings)
      is BackupDbClassification.V1_LEGACY -> stageV1(database, target, warnings)
      else -> throw BackupException(BackupErrors.UNKNOWN_FORMAT)
    }
  }

  private fun stageV2(database: SQLiteDatabase, target: TpvDatabase, warnings: MutableList<String>): StagingResult {
    var rowsWritten = 0
    target.transaction {
      // Settings
      val settings = runCatching {
        database.rawQuery("SELECT currency_code, page_size, biometric_enabled, ad_enabled FROM settings WHERE id = 1", null).use { c ->
          if (c.moveToFirst()) {
            val currency = c.getStringOrNull(0) ?: "Bs"
            val pageSize = c.getIntOrDefault(1, 15)
            val biometric = c.getIntOrDefault(2, 0) != 0
            val adEnabled = c.getIntOrDefault(3, 0) != 0
            val code = if (currency in listOf("Bs", "$", "€")) currency else "Bs"
            BackupSettingsDto(code, pageSize.coerceIn(5, 100), biometric)
          } else null
        }
      }.getOrNull()
      target.backupQueries.backupSettingsUpsert(
        currencyCode = settings?.currencyCode ?: "Bs",
        pageSize = (settings?.pageSize ?: 15).toLong(),
        biometricEnabled = if (settings?.biometricEnabled == true) 1L else 0L,
        updatedAt = java.time.Instant.now().toString()
      )
      rowsWritten += 1

      // Categories
      val categories = runCatching {
        database.rawQuery("SELECT id, name, registered_at, updated_at, deleted_at FROM categories ORDER BY id ASC", null).use { c ->
          val list = mutableListOf<BackupCategoryDto>()
          while (c.moveToNext()) {
            list += BackupCategoryDto(
              id = c.getLong(0),
              name = c.getString(1),
              registeredAt = c.getStringOrNull(2) ?: "",
              updatedAt = c.getStringOrNull(3),
              deletedAt = c.getStringOrNull(4)
            )
          }
          list
        }
      }.getOrElse { emptyList() }
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
      val expenseCategories = runCatching {
        database.rawQuery("SELECT id, name, registered_at, deleted_at FROM expense_categories ORDER BY id ASC", null).use { c ->
          val list = mutableListOf<BackupExpenseCategoryDto>()
          while (c.moveToNext()) {
            list += BackupExpenseCategoryDto(
              id = c.getLong(0),
              name = c.getString(1),
              registeredAt = c.getStringOrNull(2) ?: "",
              deletedAt = c.getStringOrNull(3)
            )
          }
          list
        }
      }.getOrElse { emptyList() }
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
      val products = runCatching {
        database.rawQuery(
          "SELECT id, category_id, sku, name, selling_price, purchase_price, current_stock, min_stock, image_uri, registered_at, updated_at, deleted_at FROM products ORDER BY id ASC",
          null
        ).use { c ->
          val list = mutableListOf<BackupProductDto>()
          while (c.moveToNext()) {
            list += BackupProductDto(
              id = c.getLong(0),
              categoryId = c.getLong(1),
              sku = c.getStringOrNull(2),
              name = c.getString(3),
              sellingPrice = c.getDoubleOrDefault(4, 0.0),
              purchasePrice = c.getDoubleOrDefault(5, 0.0),
              currentStock = c.getDoubleOrDefault(6, 0.0),
              minStock = c.getDoubleOrDefault(7, 0.0),
              imageUri = c.getStringOrNull(8),
              registeredAt = c.getStringOrNull(9) ?: "",
              updatedAt = c.getStringOrNull(10) ?: "",
              deletedAt = c.getStringOrNull(11)
            )
          }
          list
        }
      }.getOrElse { emptyList() }
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
      val transactions = runCatching {
        database.rawQuery(
          "SELECT id, type, total_amount, payment_method, detail, registered_at, deleted_at FROM sales_transactions ORDER BY id ASC",
          null
        ).use { c ->
          val list = mutableListOf<BackupTransactionDto>()
          while (c.moveToNext()) {
            list += BackupTransactionDto(
              id = c.getLong(0),
              type = c.getString(1),
              totalAmount = c.getDoubleOrDefault(2, 0.0),
              paymentMethod = c.getString(3),
              detail = c.getStringOrNull(4),
              registeredAt = c.getStringOrNull(5) ?: "",
              deletedAt = c.getStringOrNull(6)
            )
          }
          list
        }
      }.getOrElse { emptyList() }
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
      val transactionDetails = runCatching {
        database.rawQuery(
          "SELECT id, transaction_id, product_id, quantity, unit_price FROM transaction_details ORDER BY id ASC",
          null
        ).use { c ->
          val list = mutableListOf<BackupTransactionDetailDto>()
          while (c.moveToNext()) {
            list += BackupTransactionDetailDto(
              id = c.getLong(0),
              transactionId = c.getLong(1),
              productId = c.getLong(2),
              quantity = c.getDoubleOrDefault(3, 0.0),
              unitPrice = c.getDoubleOrDefault(4, 0.0)
            )
          }
          list
        }
      }.getOrElse { emptyList() }
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
      val accumulators = runCatching {
        database.rawQuery("SELECT total_sales, total_purchases, total_expenses, net_balance FROM financial_accumulators WHERE id = 1", null).use { c ->
          if (c.moveToFirst()) {
            BackupAccumulatorsDto(
              totalSales = c.getDoubleOrDefault(0, 0.0),
              totalPurchases = c.getDoubleOrDefault(1, 0.0),
              totalExpenses = c.getDoubleOrDefault(2, 0.0),
              netBalance = c.getDoubleOrDefault(3, 0.0)
            )
          } else null
        }
      }.getOrNull()
      val acc = accumulators ?: BackupAccumulatorsDto(0.0, 0.0, 0.0, 0.0)
      target.backupQueries.backupAccumulatorsUpsert(
        totalSales = acc.totalSales,
        totalPurchases = acc.totalPurchases,
        totalExpenses = acc.totalExpenses,
        netBalance = acc.netBalance,
        updatedAt = java.time.Instant.now().toString()
      )
      rowsWritten++
    }
    return StagingResult(rowsWritten, warnings)
  }

  private fun stageV1(database: SQLiteDatabase, target: TpvDatabase, warnings: MutableList<String>): StagingResult {
    var rowsWritten = 0

    // Read all v1 data first
    val v1Categories = readV1Categories(database)
    val v1ExpenseCategories = readV1ExpenseCategories(database)
    val v1Products = readV1Products(database)
    val v1Transactions = readV1Transactions(database)
    val v1TransactionDetails = readV1TransactionDetails(database)
    val v1Settings = readV1Settings(database)
    val v1Accumulators = readV1Accumulators(database)

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
        updatedAt = java.time.Instant.now().toString()
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
        updatedAt = java.time.Instant.now().toString()
      )
      rowsWritten++
    }

    if (cachedUserVersion != 1L) {
      warnings += "v1 legacy backup has user_version=${cachedUserVersion} (expected 1)"
    }

    return StagingResult(rowsWritten, warnings)
  }

  override fun close() {
    runCatching { db?.close() }
    db = null
    openedPath = null
    cachedTables = null
    columnCache = mutableMapOf()
  }

  /* ---------- v1 reads from SQLiteDatabase ---------- */

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

  private fun readV1Categories(database: SQLiteDatabase): List<V1Category> {
    return runCatching {
      database.rawQuery("SELECT id, nombre, fecha_registro, fecha_delete FROM categorias ORDER BY id ASC", null).use { c ->
        val list = mutableListOf<V1Category>()
        while (c.moveToNext()) {
          list += V1Category(
            id = c.getLong(0),
            name = c.getString(1),
            fechaRegistro = c.getStringOrNull(2),
            fechaDelete = c.getStringOrNull(3)
          )
        }
        list
      }
    }.getOrElse { emptyList() }
  }

  private fun readV1ExpenseCategories(database: SQLiteDatabase): List<V1Category> {
    return runCatching {
      database.rawQuery("SELECT id, nombre, fecha_registro, fecha_delete FROM expense_categories ORDER BY id ASC", null).use { c ->
        val list = mutableListOf<V1Category>()
        while (c.moveToNext()) {
          list += V1Category(
            id = c.getLong(0),
            name = c.getString(1),
            fechaRegistro = c.getStringOrNull(2),
            fechaDelete = c.getStringOrNull(3)
          )
        }
        list
      }
    }.getOrElse { emptyList() }
  }

  private fun readV1Products(database: SQLiteDatabase): List<V1Product> {
    return runCatching {
      database.rawQuery(
        "SELECT id, categoria_id, nombre, precio_venta, precio_compra, stock_actual, stock_minimo, imagen_uri, fecha_registro, fecha_update, fecha_delete FROM productos ORDER BY id ASC",
        null
      ).use { c ->
        val list = mutableListOf<V1Product>()
        while (c.moveToNext()) {
          list += V1Product(
            id = c.getLong(0),
            categoriaId = c.getLong(1),
            nombre = c.getString(2),
            precioVenta = c.getDoubleOrDefault(3, 0.0),
            precioCompra = c.getDoubleOrDefault(4, 0.0),
            stockActual = c.getDoubleOrDefault(5, 0.0),
            stockMinimo = c.getDoubleOrDefault(6, 0.0),
            imagenUri = c.getStringOrNull(7),
            fechaRegistro = c.getStringOrNull(8),
            fechaUpdate = c.getStringOrNull(9),
            fechaDelete = c.getStringOrNull(10)
          )
        }
        list
      }
    }.getOrElse { emptyList() }
  }

  private fun readV1Transactions(database: SQLiteDatabase): List<V1Transaction> {
    return runCatching {
      database.rawQuery(
        "SELECT id, tipo, monto_total, tipo_pago, detalle, fecha_registro, fecha_delete FROM transacciones ORDER BY id ASC",
        null
      ).use { c ->
        val list = mutableListOf<V1Transaction>()
        while (c.moveToNext()) {
          list += V1Transaction(
            id = c.getLong(0),
            tipo = c.getString(1),
            montoTotal = c.getDoubleOrDefault(2, 0.0),
            tipoPago = c.getString(3),
            detalle = c.getStringOrNull(4),
            fechaRegistro = c.getStringOrNull(5),
            fechaDelete = c.getStringOrNull(6)
          )
        }
        list
      }
    }.getOrElse { emptyList() }
  }

  private fun readV1TransactionDetails(database: SQLiteDatabase): List<V1TransactionDetail> {
    return runCatching {
      database.rawQuery(
        "SELECT id, transaccion_id, producto_id, cantidad, precio_unitario FROM detalle_transacciones ORDER BY id ASC",
        null
      ).use { c ->
        val list = mutableListOf<V1TransactionDetail>()
        while (c.moveToNext()) {
          list += V1TransactionDetail(
            id = c.getLong(0),
            transaccionId = c.getLong(1),
            productoId = c.getLong(2),
            cantidad = c.getDoubleOrDefault(3, 0.0),
            precioUnitario = c.getDoubleOrDefault(4, 0.0)
          )
        }
        list
      }
    }.getOrElse { emptyList() }
  }

  private fun readV1Settings(database: SQLiteDatabase): V1Settings? {
    return runCatching {
      database.rawQuery("SELECT currency_code, page_size, biometric_enabled FROM settings WHERE id = 1", null).use { c ->
        if (c.moveToFirst()) {
          V1Settings(
            currencyCode = c.getStringOrNull(0) ?: "Bs",
            pageSize = c.getIntOrDefault(1, 15),
            biometricEnabled = c.getIntOrDefault(2, 0).toLong()
          )
        } else null
      }
    }.getOrNull()
  }

  private fun readV1Accumulators(database: SQLiteDatabase): V1Accumulators? {
    return runCatching {
      database.rawQuery("SELECT total_ventas, total_compras, total_gastos, saldo_actual FROM resumen_caja WHERE id = 1", null).use { c ->
        if (c.moveToFirst()) {
          V1Accumulators(
            totalVentas = c.getDoubleOrDefault(0, 0.0),
            totalCompras = c.getDoubleOrDefault(1, 0.0),
            totalGastos = c.getDoubleOrDefault(2, 0.0),
            saldoActual = c.getDoubleOrDefault(3, 0.0)
          )
        } else null
      }
    }.getOrNull()
  }
}

class AndroidBackupFileIo(context: Any?) : BackupFileIo {

  private val appContext: Context? = context as? Context
  private var writeLockDb: SQLiteDatabase? = null

  override fun save(fileName: String, bytes: ByteArray): String? {
    val ctx = appContext ?: return null
    val dir = File(ctx.filesDir, "backups").apply { mkdirs() }
    val file = File(dir, fileName)
    file.writeBytes(bytes)
    return file.absolutePath
  }

  override fun load(uri: Any?): BackupSource? {
    val ctx = appContext ?: return null
    val parsed = uri as? Uri ?: return null
    val displayName = runCatching {
      ctx.contentResolver.query(parsed, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst() && !c.isNull(0)) c.getString(0) else null
      }
    }.getOrNull()

    val isDb = displayName?.endsWith(".db", ignoreCase = true) == true ||
      displayName?.endsWith(".sqlite", ignoreCase = true) == true

    if (isDb) {
      val temp = File(ctx.cacheDir, "import_staging_${System.currentTimeMillis()}.db")
      ctx.contentResolver.openInputStream(parsed)?.use { input ->
        temp.outputStream().use { output -> input.copyTo(output) }
      } ?: return null
      return BackupSource.LegacySqliteFile(temp.absolutePath)
    }

    val bytes = ctx.contentResolver.openInputStream(parsed)?.use { it.readBytes() } ?: return null
    return BackupSource.Json(bytes)
  }

  override fun openDriver(path: String): SqlDriver? {
    return runCatching {
      val temp = File.createTempFile("driver_", ".db")
      File(path).copyTo(temp)
      AndroidSqliteDriver(TpvDatabase.Schema, appContext!!, temp.absolutePath)
    }.getOrNull()
  }

  override fun createFreshDriver(): SqlDriver? {
    return runCatching {
      val temp = File.createTempFile("fresh_", ".db")
      AndroidSqliteDriver(TpvDatabase.Schema, appContext!!, temp.absolutePath)
    }.getOrNull()
  }

  override fun createValidator(): BackupDbValidator = AndroidBackupDbValidator()

  override fun stageCopy(path: String): String? {
    // Already staged by load(), just return the path
    return path
  }

  override fun snapshotActiveDb(): String? {
    val ctx = appContext ?: return null
    val dbFile = File(ctx.getDatabasePath("tpv.db").absolutePath)
    if (!dbFile.exists()) return null
    val snapshotDir = File(ctx.filesDir, "backups/snapshots").apply { mkdirs() }
    val snapshot = File(snapshotDir, "tpv-snapshot-${System.currentTimeMillis()}.db")
    try {
      dbFile.copyTo(snapshot)
      // Also copy -wal and -shm if they exist
      val walFile = File(dbFile.absolutePath + "-wal")
      if (walFile.exists()) {
        walFile.copyTo(File(snapshot.absolutePath + "-wal"))
      }
      val shmFile = File(dbFile.absolutePath + "-shm")
      if (shmFile.exists()) {
        shmFile.copyTo(File(snapshot.absolutePath + "-shm"))
      }
      // Cleanup old snapshots, keep last 3
      val snapshots = snapshotDir.listFiles()?.filter { it.name.startsWith("tpv-snapshot-") }
        ?.sortedByDescending { it.lastModified() }
        ?.drop(3)
      snapshots?.forEach { it.delete() }
      return snapshot.absolutePath
    } catch (e: Exception) {
      return null
    }
  }

  override fun restoreActiveDb(snapshotPath: String?): Boolean {
    val ctx = appContext ?: return false
    val snapshotPathStr = snapshotPath ?: return false
    val snapshotFile = File(snapshotPathStr)
    if (!snapshotFile.exists()) return false
    val dbFile = File(ctx.getDatabasePath("tpv.db").absolutePath)
    try {
      // Close any open connections first
      runCatching { writeLockDb?.close() }
      writeLockDb = null
      // Copy snapshot back
      snapshotFile.copyTo(dbFile)
      val walFile = File(snapshotFile.absolutePath + "-wal")
      if (walFile.exists()) {
        walFile.copyTo(File(dbFile.absolutePath + "-wal"))
      }
      val shmFile = File(snapshotFile.absolutePath + "-shm")
      if (shmFile.exists()) {
        shmFile.copyTo(File(dbFile.absolutePath + "-shm"))
      }
      return true
    } catch (e: Exception) {
      return false
    }
  }

  override fun acquireWriteLock(): Boolean {
    val ctx = appContext ?: return false
    try {
      writeLockDb = SQLiteDatabase.openDatabase(
        ctx.getDatabasePath("tpv.db").absolutePath,
        null,
        SQLiteDatabase.OPEN_READWRITE
      )
      writeLockDb?.execSQL("PRAGMA locking_mode = EXCLUSIVE")
      writeLockDb?.beginTransaction()
      return true
    } catch (e: Exception) {
      runCatching { writeLockDb?.close() }
      writeLockDb = null
      return false
    }
  }

  override fun releaseWriteLock() {
    try {
      writeLockDb?.endTransaction()
      writeLockDb?.close()
    } catch (e: Exception) {
      // ignore
    }
    writeLockDb = null
  }
}

actual interface BackupDbValidator {
  actual fun open(path: String)
  actual fun classify(): BackupDbClassification
  actual fun stageInto(target: TpvDatabase): StagingResult
  actual fun close()
}

actual interface BackupFileIo {
  actual fun save(fileName: String, bytes: ByteArray): String?
  actual fun load(uri: Any?): BackupSource?
  actual fun openDriver(path: String): SqlDriver?
  actual fun createFreshDriver(): SqlDriver?
  actual fun createValidator(): BackupDbValidator
  actual fun stageCopy(path: String): String?
  actual fun snapshotActiveDb(): String?
  actual fun restoreActiveDb(snapshotPath: String?): Boolean
  actual fun acquireWriteLock(): Boolean
  actual fun releaseWriteLock()
}

actual fun createBackupFileIo(context: Any?): BackupFileIo = AndroidBackupFileIo(context)