package com.copitoB.mypos.application.backup

import com.copitoB.mypos.application.security.VerifyPin
import com.copitoB.mypos.data.repository.SqliteBackupRepository
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ExportBackupUseCase(
  private val repository: SqliteBackupRepository,
  private val fileIo: BackupFileIo,
  private val codec: JsonCodec
) {
  operator fun invoke(): String? {
    val data = repository.exportAll()
    val envelope = BackupEnvelopeDto(
      format = FORMAT,
      schemaVersion = SCHEMA_VERSION,
      appVersion = APP_VERSION,
      exportedAt = Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT),
      source = SOURCE_KMP,
      data = data
    )
    val bytes = codec.encode(envelope)
    return fileIo.save(buildFileName(), bytes)
  }

  private fun buildFileName(): String {
    val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")
      .withZone(ZoneOffset.UTC)
      .format(Instant.now())
    return "tpv-backup-$stamp.json"
  }

  companion object {
    const val FORMAT = "tpv-backup"
    const val SCHEMA_VERSION = 2
    const val APP_VERSION = "2.0.0"
    const val SOURCE_KMP = "kmp"
  }
}

class ImportBackupUseCase(
  private val repository: SqliteBackupRepository,
  private val codec: JsonCodec,
  private val legacyDbReader: LegacyDbReader,
  private val verifyPin: VerifyPin
) {
  operator fun invoke(source: BackupSource, pin: String): ImportReport {
    if (verifyPin.isConfigured()) {
      require(verifyPin(pin)) { "PIN required for import" }
    }

    val raw: BackupDataDto = when (source) {
      is BackupSource.Json -> {
        val envelope = runCatching { codec.decode(source.bytes) }
          .getOrElse { throw BackupException("Invalid backup file: ${it.message}") }
        if (envelope.format != ExportBackupUseCase.FORMAT) {
          throw BackupException("Invalid backup format: ${envelope.format}")
        }
        if (envelope.schemaVersion > ExportBackupUseCase.SCHEMA_VERSION) {
          throw BackupException("Backup schema version ${envelope.schemaVersion} is newer than supported")
        }
        envelope.data
      }
      is BackupSource.LegacySqliteFile -> legacyDbReader.read(source.path)
    }

    val warnings = mutableListOf<String>()
    val normalized = normalize(raw, warnings)

    repository.importAll(normalized)

    val inserted = normalized.categories.size +
      normalized.expenseCategories.size +
      normalized.products.size +
      normalized.transactions.size +
      normalized.transactionDetails.size

    return ImportReport(
      inserted = inserted,
      updated = 1,
      skipped = warnings.count { it.startsWith("skipped") },
      warnings = warnings
    )
  }

  private fun normalize(raw: BackupDataDto, warnings: MutableList<String>): BackupDataDto {
    val settings = LegacyMapper.sanitizeSettings(raw.settings)

    val categories = raw.categories.map { category ->
      val registeredAt = LegacyMapper.normalizeTimestamp(category.registeredAt) ?: ""
      category.copy(
        registeredAt = registeredAt,
        updatedAt = LegacyMapper.categoryUpdatedAt(category.updatedAt ?: category.registeredAt)
      )
    }

    var expenseCategories = raw.expenseCategories.map {
      it.copy(registeredAt = LegacyMapper.normalizeTimestamp(it.registeredAt) ?: "")
    }
    if (expenseCategories.isEmpty()) {
      expenseCategories = listOf(
        BackupExpenseCategoryDto(
          id = 1L,
          name = "General",
          registeredAt = Instant.now().toString()
        )
      )
      warnings += "seeded default expense category"
    }

    val products = raw.products.map {
      it.copy(
        sku = LegacyMapper.safeSku(it.sku),
        registeredAt = LegacyMapper.normalizeTimestamp(it.registeredAt) ?: "",
        updatedAt = LegacyMapper.normalizeTimestamp(it.updatedAt) ?: "",
        deletedAt = LegacyMapper.normalizeTimestamp(it.deletedAt)
      )
    }

    val transactions = mutableListOf<BackupTransactionDto>()
    for (t in raw.transactions) {
      val type = LegacyMapper.mapType(t.type)
      val method = LegacyMapper.mapPaymentMethod(t.paymentMethod)
      if (type == null) {
        warnings += "skipped transaction ${t.id}: unknown type '${t.type}'"
        continue
      }
      if (method == null) {
        warnings += "skipped transaction ${t.id}: unknown payment method '${t.paymentMethod}'"
        continue
      }
      transactions += t.copy(
        type = type,
        paymentMethod = method,
        registeredAt = LegacyMapper.normalizeTimestamp(t.registeredAt) ?: "",
        deletedAt = LegacyMapper.normalizeTimestamp(t.deletedAt)
      )
    }

    val transactionDetails = raw.transactionDetails

    val accumulators = LegacyMapper.mapAccumulators(raw.accumulators)

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
}