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

data class StagingResult(
  val rowsWritten: Int,
  val warnings: List<String>
)

sealed interface BackupDbClassification {
  data class V2_CURRENT(val userVersion: Long) : BackupDbClassification
  data class V1_LEGACY(val userVersion: Long) : BackupDbClassification
  data class INVALID(val reason: String) : BackupDbClassification
}

class BackupException(message: String) : Exception(message)

object BackupErrors {
  const val HEADER_INVALID = "Archivo no es una base de datos SQLite válida"
  const val INTEGRITY_FAILED = "La base de datos falló la comprobación de integridad"
  const val UNKNOWN_FORMAT = "Formato de base de datos no reconocido"
  const val MISSING_TABLES = "Faltan tablas requeridas: %s"
  const val MISSING_COLUMNS = "Faltan columnas requeridas en %s: %s"
  const val SCHEMA_INCOMPLETE = "Esquema v2 incompleto (columnas legacy detectadas): %s"
  const val BACKUP_TOO_NEW = "La copia de seguridad es de una versión más nueva (%d) que la soportada (%d)"
  const val ENVELOPE_INVALID = "Envoltorio de copia inválido: %s"
  const val PIN_REQUIRED = "Se requiere PIN para importar"
  const val IMPORT_FAILED = "No se pudo importar la copia de seguridad: %s"
  const val ROLLBACK_FAILED = "La importación falló y no se pudo restaurar la base de datos anterior: %s"
  const val DB_IMPORT_UNSUPPORTED = "Importación de .db no soportada en esta plataforma"
  const val ROW_IMPORT_FAILED = "Error importando fila: %s"
}