package com.copitoB.mypos.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.copitoB.mypos.application.backup.BackupAccumulatorsDto
import com.copitoB.mypos.application.backup.BackupCategoryDto
import com.copitoB.mypos.application.backup.BackupDataDto
import com.copitoB.mypos.application.backup.BackupExpenseCategoryDto
import com.copitoB.mypos.application.backup.BackupProductDto
import com.copitoB.mypos.application.backup.BackupSettingsDto
import com.copitoB.mypos.application.backup.BackupTransactionDetailDto
import com.copitoB.mypos.application.backup.BackupTransactionDto

class SqliteBackupRepository(
  driver: SqlDriver
) {
  private val queries = TpvDatabase(driver)
  private val backup = queries.backupQueries

  fun exportAll(): BackupDataDto {
    val settingsRow = backup.backupSettingsFind().executeAsOneOrNull()
    val settings = if (settingsRow != null) {
      BackupSettingsDto(
        currencyCode = settingsRow.currency_code,
        pageSize = settingsRow.page_size.toInt(),
        biometricEnabled = settingsRow.biometric_enabled != 0L
      )
    } else {
      BackupSettingsDto("Bs", 15, false)
    }

    val categories = backup.backupCategoriesFindAll().executeAsList().map {
      BackupCategoryDto(
        id = it.id,
        name = it.name,
        registeredAt = it.registered_at ?: "",
        updatedAt = it.updated_at,
        deletedAt = it.deleted_at
      )
    }

    val expenseCategories = backup.backupExpenseCategoriesFindAll().executeAsList().map {
      BackupExpenseCategoryDto(
        id = it.id,
        name = it.name,
        registeredAt = it.registered_at ?: "",
        deletedAt = it.deleted_at
      )
    }

    val products = backup.backupProductsFindAll().executeAsList().map {
      BackupProductDto(
        id = it.id,
        categoryId = it.category_id,
        sku = it.sku,
        name = it.name,
        sellingPrice = it.selling_price,
        purchasePrice = it.purchase_price,
        currentStock = it.current_stock ?: 0.0,
        minStock = it.min_stock ?: 0.0,
        imageUri = it.image_uri,
        registeredAt = it.registered_at ?: "",
        updatedAt = it.updated_at ?: "",
        deletedAt = it.deleted_at
      )
    }

    val transactions = backup.backupTransactionsFindAll().executeAsList().map {
      BackupTransactionDto(
        id = it.id,
        type = it.type,
        totalAmount = it.total_amount,
        paymentMethod = it.payment_method,
        detail = it.detail,
        registeredAt = it.registered_at ?: "",
        deletedAt = it.deleted_at
      )
    }

    val transactionDetails = backup.backupTransactionDetailsFindAll().executeAsList().map {
      BackupTransactionDetailDto(
        id = it.id,
        transactionId = it.transaction_id,
        productId = it.product_id,
        quantity = it.quantity,
        unitPrice = it.unit_price
      )
    }

    val accumulatorRow = backup.backupAccumulatorSums().executeAsOne()
    val accumulators = BackupAccumulatorsDto(
      totalSales = accumulatorRow.total_sales,
      totalPurchases = accumulatorRow.total_purchases,
      totalExpenses = accumulatorRow.total_expenses,
      netBalance = accumulatorRow.total_sales - accumulatorRow.total_purchases - accumulatorRow.total_expenses
    )

    return BackupDataDto(
      settings = settings,
      categories = categories,
      expenseCategories = expenseCategories,
      products = products,
      transactions = transactions,
      transactionDetails = transactionDetails,
      accumulators = accumulators
    )
  }

  fun importAll(data: BackupDataDto) {
    val updatedAt = java.time.Instant.now().toString()
    backup.transaction {
      backup.backupDeleteTransactionDetails()
      backup.backupDeleteProducts()
      backup.backupDeleteSalesTransactions()
      backup.backupDeleteExpenseCategories()
      backup.backupDeleteCategories()

      backup.backupSettingsUpsert(
        currencyCode = data.settings.currencyCode,
        pageSize = data.settings.pageSize.toLong(),
        biometricEnabled = if (data.settings.biometricEnabled) 1L else 0L,
        updatedAt = updatedAt
      )

      data.categories.forEach {
        backup.backupCategoriesInsertWithId(
          id = it.id,
          name = it.name,
          registeredAt = it.registeredAt,
          updatedAt = it.updatedAt,
          deletedAt = it.deletedAt
        )
      }

      data.expenseCategories.forEach {
        backup.backupExpenseCategoriesInsertWithId(
          id = it.id,
          name = it.name,
          registeredAt = it.registeredAt,
          deletedAt = it.deletedAt
        )
      }

      data.products.forEach {
        backup.backupProductsInsertWithId(
          id = it.id,
          categoryId = it.categoryId,
          sku = it.sku,
          name = it.name,
          sellingPrice = it.sellingPrice,
          purchasePrice = it.purchasePrice,
          currentStock = it.currentStock,
          minStock = it.minStock,
          imageUri = it.imageUri,
          registeredAt = it.registeredAt,
          updatedAt = it.updatedAt,
          deletedAt = it.deletedAt
        )
      }

      data.transactions.forEach {
        backup.backupTransactionsInsertWithId(
          id = it.id,
          type = it.type,
          totalAmount = it.totalAmount,
          paymentMethod = it.paymentMethod,
          detail = it.detail,
          registeredAt = it.registeredAt,
          deletedAt = it.deletedAt
        )
      }

      data.transactionDetails.forEach {
        backup.backupTransactionDetailsInsertWithId(
          id = it.id,
          transactionId = it.transactionId,
          productId = it.productId,
          quantity = it.quantity,
          unitPrice = it.unitPrice
        )
      }

      val sums = backup.backupAccumulatorSums().executeAsOne()
      val net = sums.total_sales - sums.total_purchases - sums.total_expenses
      backup.backupAccumulatorsUpsert(
        totalSales = sums.total_sales,
        totalPurchases = sums.total_purchases,
        totalExpenses = sums.total_expenses,
        netBalance = net,
        updatedAt = updatedAt
      )
    }
  }
}