package com.copitoB.mypos.application.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupSettingsDto(
  val currencyCode: String,
  val pageSize: Int,
  val biometricEnabled: Boolean
)

@Serializable
data class BackupCategoryDto(
  val id: Long,
  val name: String,
  val registeredAt: String,
  val updatedAt: String? = null,
  val deletedAt: String? = null
)

@Serializable
data class BackupExpenseCategoryDto(
  val id: Long,
  val name: String,
  val registeredAt: String,
  val deletedAt: String? = null
)

@Serializable
data class BackupProductDto(
  val id: Long,
  val categoryId: Long,
  val sku: String? = null,
  val name: String,
  val sellingPrice: Double,
  val purchasePrice: Double,
  val currentStock: Double,
  val minStock: Double,
  val imageUri: String? = null,
  val registeredAt: String,
  val updatedAt: String,
  val deletedAt: String? = null
)

@Serializable
data class BackupTransactionDto(
  val id: Long,
  val type: String,
  val totalAmount: Double,
  val paymentMethod: String,
  val detail: String? = null,
  val registeredAt: String,
  val deletedAt: String? = null
)

@Serializable
data class BackupTransactionDetailDto(
  val id: Long,
  val transactionId: Long,
  val productId: Long,
  val quantity: Double,
  val unitPrice: Double
)

@Serializable
data class BackupAccumulatorsDto(
  val totalSales: Double,
  val totalPurchases: Double,
  val totalExpenses: Double,
  val netBalance: Double
)

@Serializable
data class BackupDataDto(
  val settings: BackupSettingsDto,
  val categories: List<BackupCategoryDto>,
  val expenseCategories: List<BackupExpenseCategoryDto>,
  val products: List<BackupProductDto>,
  val transactions: List<BackupTransactionDto>,
  val transactionDetails: List<BackupTransactionDetailDto>,
  val accumulators: BackupAccumulatorsDto
)

@Serializable
data class BackupEnvelopeDto(
  val format: String,
  val schemaVersion: Int,
  val appVersion: String,
  val exportedAt: String,
  val source: String,
  val data: BackupDataDto
)

sealed interface BackupSource {
  data class Json(val bytes: ByteArray) : BackupSource {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is Json) return false
      return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = bytes.contentHashCode()
  }

  data class LegacySqliteFile(val path: String) : BackupSource
}

data class ImportReport(
  val inserted: Int,
  val updated: Int,
  val skipped: Int,
  val warnings: List<String>
)

class BackupException(message: String) : Exception(message)