package com.copitoB.mypos.application.backup

import com.copitoB.mypos.application.security.VerifyPin
import com.copitoB.mypos.data.repository.SqliteBackupRepository
import com.copitoB.mypos.data.repository.TpvDatabase
import kotlinx.coroutines.runBlocking
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
    return "tpv-backup-$stamp"
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
  private val backupFileIo: BackupFileIo,
  private val verifyPin: VerifyPin
) {
  operator fun invoke(source: BackupSource, pin: String): ImportReport {
    if (verifyPin.isConfigured()) {
      if (!verifyPin(pin)) {
        throw BackupException(BackupErrors.PIN_REQUIRED)
      }
    }

    val stagingWarnings = mutableListOf<String>()
    val raw: BackupDataDto = when (source) {
      is BackupSource.Json -> {
        val envelope = runCatching { codec.decode(source.bytes) }
          .getOrElse { throw BackupException(BackupErrors.ENVELOPE_INVALID.format(it.message ?: "desconocido")) }
        if (envelope.format != ExportBackupUseCase.FORMAT) {
          throw BackupException(BackupErrors.ENVELOPE_INVALID.format("formato '${envelope.format}' no reconocido"))
        }
        if (envelope.schemaVersion > ExportBackupUseCase.SCHEMA_VERSION) {
          throw BackupException(BackupErrors.BACKUP_TOO_NEW.format(envelope.schemaVersion, ExportBackupUseCase.SCHEMA_VERSION))
        }
        envelope.data
      }
      is BackupSource.LegacySqliteFile -> importSqliteStaging(source.path, stagingWarnings)
    }

    val warnings = mutableListOf<String>()
    warnings.addAll(stagingWarnings)
    val normalized = normalize(raw, warnings)

    val snapshot = repository.exportAll()
    if (!backupFileIo.acquireWriteLock()) {
      throw BackupException(BackupErrors.IMPORT_FAILED.format("no se pudo obtener bloqueo de escritura"))
    }
    try {
      repository.importAll(normalized)
    } catch (e: Exception) {
      try {
        repository.importAll(snapshot)
      } catch (restoreEx: Exception) {
        throw BackupException(BackupErrors.ROLLBACK_FAILED.format(restoreEx.message ?: "error desconocido"))
      }
      throw BackupException(BackupErrors.IMPORT_FAILED.format(e.message ?: "error desconocido"))
    } finally {
      backupFileIo.releaseWriteLock()
    }

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

  private fun importSqliteStaging(path: String, warnings: MutableList<String>): BackupDataDto {
    val stagingPath = backupFileIo.stageCopy(path) ?: path
    val validator = backupFileIo.createValidator()
    return try {
      validator.open(stagingPath)
      val classification = validator.classify()
      when (classification) {
        is BackupDbClassification.INVALID -> throw BackupException(classification.reason)
        is BackupDbClassification.V2_CURRENT -> stageToDto(validator, warnings)
        is BackupDbClassification.V1_LEGACY -> stageToDto(validator, warnings)
      }
    } finally {
      validator.close()
      if (stagingPath != path) {
        runCatching { java.io.File(stagingPath).delete() }
      }
    }
  }

  private fun stageToDto(validator: BackupDbValidator, warnings: MutableList<String>): BackupDataDto {
    val freshDriver = backupFileIo.createFreshDriver()
      ?: throw BackupException(BackupErrors.IMPORT_FAILED.format("no se pudo crear base temporal"))
    try {
      runBlocking { TpvDatabase.Schema.create(freshDriver).await() }
      val result = validator.stageInto(TpvDatabase(freshDriver))
      warnings.addAll(result.warnings)
      return SqliteBackupRepository(freshDriver).exportAll()
    } catch (e: BackupException) {
      throw e
    } catch (e: Exception) {
      throw BackupException(BackupErrors.IMPORT_FAILED.format(e.message ?: "error desconocido"))
    } finally {
      runCatching { freshDriver.close() }
    }
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