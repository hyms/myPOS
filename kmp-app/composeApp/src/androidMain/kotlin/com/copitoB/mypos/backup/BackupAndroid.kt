package com.copitoB.mypos.application.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.OpenableColumns
import com.copitoB.mypos.application.backup.BackupAccumulatorsDto
import com.copitoB.mypos.application.backup.BackupCategoryDto
import com.copitoB.mypos.application.backup.BackupDataDto
import com.copitoB.mypos.application.backup.BackupExpenseCategoryDto
import com.copitoB.mypos.application.backup.BackupFileIo
import com.copitoB.mypos.application.backup.BackupProductDto
import com.copitoB.mypos.application.backup.BackupSettingsDto
import com.copitoB.mypos.application.backup.BackupSource
import com.copitoB.mypos.application.backup.BackupTransactionDetailDto
import com.copitoB.mypos.application.backup.BackupTransactionDto
import com.copitoB.mypos.application.backup.LegacyDbReader
import java.io.File

actual class LegacyDbReader actual constructor() {

  actual fun read(path: String): BackupDataDto {
    val db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)
    try {
      db.use { database ->
        val categories = mutableListOf<BackupCategoryDto>()
        database.rawQuery("SELECT id, name, registered_at, updated_at, deleted_at FROM categories ORDER BY id ASC", null).use { c ->
          while (c.moveToNext()) {
            categories += BackupCategoryDto(
              id = c.getLong(0),
              name = c.getString(1),
              registeredAt = c.getString(2),
              updatedAt = c.getStringOrNull(3),
              deletedAt = c.getStringOrNull(4)
            )
          }
        }

        val expenseCategories = mutableListOf<BackupExpenseCategoryDto>()
        database.rawQuery("SELECT id, name, registered_at, deleted_at FROM expense_categories ORDER BY id ASC", null).use { c ->
          while (c.moveToNext()) {
            expenseCategories += BackupExpenseCategoryDto(
              id = c.getLong(0),
              name = c.getString(1),
              registeredAt = c.getString(2),
              deletedAt = c.getStringOrNull(3)
            )
          }
        }

        val products = mutableListOf<BackupProductDto>()
        database.rawQuery(
          "SELECT id, category_id, sku, name, selling_price, purchase_price, current_stock, min_stock, image_uri, registered_at, updated_at, deleted_at FROM products ORDER BY id ASC",
          null
        ).use { c ->
          while (c.moveToNext()) {
            products += BackupProductDto(
              id = c.getLong(0),
              categoryId = c.getLong(1),
              sku = c.getStringOrNull(2),
              name = c.getString(3),
              sellingPrice = c.getDouble(4),
              purchasePrice = c.getDouble(5),
              currentStock = c.getDouble(6),
              minStock = c.getDouble(7),
              imageUri = c.getStringOrNull(8),
              registeredAt = c.getString(9),
              updatedAt = c.getString(10),
              deletedAt = c.getStringOrNull(11)
            )
          }
        }

        val transactions = mutableListOf<BackupTransactionDto>()
        database.rawQuery(
          "SELECT id, type, total_amount, payment_method, detail, registered_at, deleted_at FROM sales_transactions ORDER BY id ASC",
          null
        ).use { c ->
          while (c.moveToNext()) {
            transactions += BackupTransactionDto(
              id = c.getLong(0),
              type = c.getString(1),
              totalAmount = c.getDouble(2),
              paymentMethod = c.getString(3),
              detail = c.getStringOrNull(4),
              registeredAt = c.getString(5),
              deletedAt = c.getStringOrNull(6)
            )
          }
        }

        val transactionDetails = mutableListOf<BackupTransactionDetailDto>()
        database.rawQuery(
          "SELECT id, transaction_id, product_id, quantity, unit_price FROM transaction_details ORDER BY id ASC",
          null
        ).use { c ->
          while (c.moveToNext()) {
            transactionDetails += BackupTransactionDetailDto(
              id = c.getLong(0),
              transactionId = c.getLong(1),
              productId = c.getLong(2),
              quantity = c.getDouble(3),
              unitPrice = c.getDouble(4)
            )
          }
        }

        val settings = runCatching {
          database.rawQuery(
            "SELECT currency_code, page_size, biometric_enabled FROM settings WHERE id = 1",
            null
          ).use { c ->
            if (c.moveToFirst()) {
              val currency = c.getStringOrNull(0) ?: "Bs"
              val pageSize = c.getInt(1)
              val biometric = c.getInt(2) != 0
              val code = if (currency in listOf("Bs", "$", "€")) currency else "Bs"
              BackupSettingsDto(
                currencyCode = code,
                pageSize = pageSize.coerceIn(5, 100),
                biometricEnabled = biometric
              )
            } else {
              BackupSettingsDto("Bs", 15, false)
            }
          }
        }.getOrElse { BackupSettingsDto("Bs", 15, false) }

        return BackupDataDto(
          settings = settings,
          categories = categories,
          expenseCategories = expenseCategories,
          products = products,
          transactions = transactions,
          transactionDetails = transactionDetails,
          accumulators = BackupAccumulatorsDto(0.0, 0.0, 0.0, 0.0)
        )
      }
    } finally {
      runCatching { db.close() }
    }
  }

  private fun android.database.Cursor.getStringOrNull(index: Int): String? =
    if (isNull(index)) null else getString(index)
}

actual class BackupFileIo actual constructor(context: Any?) {

  private val appContext: Context? = context as? Context

  actual fun save(fileName: String, bytes: ByteArray): String? {
    val ctx = appContext ?: return null
    val dir = File(ctx.filesDir, "backups").apply { mkdirs() }
    val file = File(dir, fileName)
    file.writeBytes(bytes)
    return file.absolutePath
  }

  actual fun load(uri: Any?): BackupSource? {
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
      val temp = File(ctx.cacheDir, "import_${System.currentTimeMillis()}.db")
      ctx.contentResolver.openInputStream(parsed)?.use { input ->
        temp.outputStream().use { output -> input.copyTo(output) }
      } ?: return null
      return BackupSource.LegacySqliteFile(temp.absolutePath)
    }

    val bytes = ctx.contentResolver.openInputStream(parsed)?.use { it.readBytes() } ?: return null
    return BackupSource.Json(bytes)
  }
}