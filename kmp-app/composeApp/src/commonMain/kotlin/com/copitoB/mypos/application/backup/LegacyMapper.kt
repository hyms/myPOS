package com.copitoB.mypos.application.backup

import com.copitoB.mypos.domain.valueobjects.PaymentMethod
import com.copitoB.mypos.domain.valueobjects.TransactionType

object LegacyMapper {

  private val legacyTypeToKmp = mapOf(
    "VENTA" to TransactionType.SALE,
    "COMPRA" to TransactionType.PURCHASE,
    "GASTO" to TransactionType.EXPENSE
  )

  private val legacyPaymentToKmp = mapOf(
    "EFECTIVO" to PaymentMethod.CASH,
    "TARJETA" to PaymentMethod.CARD,
    "TRANSFERENCIA" to PaymentMethod.TRANSFER,
    "QR" to PaymentMethod.QR
  )

  private val validKmpTypes = TransactionType.entries.map { it.name }.toSet()
  private val validKmpPayments = PaymentMethod.entries.map { it.code }.toSet()

  fun mapType(raw: String): String? {
    val upper = raw.trim().uppercase()
    legacyTypeToKmp[upper]?.let { return it.name }
    return if (upper in validKmpTypes) upper else null
  }

  fun mapPaymentMethod(raw: String): String? {
    val upper = raw.trim().uppercase()
    legacyPaymentToKmp[upper]?.let { return it.code }
    return if (upper in validKmpPayments) upper else null
  }

  fun isKnownType(raw: String): Boolean = mapType(raw) != null

  fun isKnownPayment(raw: String): Boolean = mapPaymentMethod(raw) != null

  fun normalizeTimestamp(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val trimmed = raw.trim()
    if (trimmed.length == 19 && trimmed[10] == ' ' && trimmed[4] == '-' && trimmed[13] == ':' && trimmed[16] == ':') {
      return trimmed.replace(' ', 'T') + "Z"
    }
    return trimmed
  }

  fun safeSku(raw: String?): String? = raw

  fun categoryUpdatedAt(registeredAt: String?): String? = normalizeTimestamp(registeredAt)

  fun defaultSettings() = BackupSettingsDto(
    currencyCode = "Bs",
    pageSize = 15,
    biometricEnabled = false
  )

  fun sanitizeSettings(raw: BackupSettingsDto?): BackupSettingsDto {
    val safe = raw ?: defaultSettings()
    val currency = if (safe.currencyCode in listOf("Bs", "$", "€")) safe.currencyCode else "Bs"
    val pageSize = safe.pageSize.coerceIn(5, 100)
    return BackupSettingsDto(
      currencyCode = currency,
      pageSize = pageSize,
      biometricEnabled = safe.biometricEnabled
    )
  }

  fun mapAccumulators(raw: BackupAccumulatorsDto?): BackupAccumulatorsDto {
    val safe = raw ?: BackupAccumulatorsDto(0.0, 0.0, 0.0, 0.0)
    return BackupAccumulatorsDto(
      totalSales = safe.totalSales,
      totalPurchases = safe.totalPurchases,
      totalExpenses = safe.totalExpenses,
      netBalance = safe.netBalance
    )
  }

  fun remapNullCategory(categoryId: Long?, validCategoryIds: Set<Long>, fallbackCategoryId: Long?): Long {
    return if (categoryId != null && categoryId in validCategoryIds) categoryId
    else fallbackCategoryId ?: throw IllegalArgumentException("No valid category for product")
  }
}