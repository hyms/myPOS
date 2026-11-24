package com.copitoB.mypos.application.backup

import app.cash.sqldelight.db.SqlDriver
import com.copitoB.mypos.application.backup.BackupDataDto
import com.copitoB.mypos.application.backup.BackupDbClassification
import com.copitoB.mypos.application.backup.BackupDbValidator
import com.copitoB.mypos.application.backup.BackupErrors
import com.copitoB.mypos.application.backup.BackupFileIo
import com.copitoB.mypos.application.backup.BackupSource
import com.copitoB.mypos.data.repository.TpvDatabase

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

actual fun createBackupFileIo(context: Any?): BackupFileIo = IosBackupFileIo(context)

class IosBackupDbValidator : BackupDbValidator {
  override fun open(path: String) {}
  override fun classify(): BackupDbClassification =
    throw UnsupportedOperationException(BackupErrors.DB_IMPORT_UNSUPPORTED)
  override fun stageInto(target: TpvDatabase): StagingResult =
    throw UnsupportedOperationException(BackupErrors.DB_IMPORT_UNSUPPORTED)
  override fun close() {}
}

class IosBackupFileIo(context: Any?) : BackupFileIo {
  override fun save(fileName: String, bytes: ByteArray): String? =
    throw UnsupportedOperationException("Backup file I/O is not supported on iOS yet")

  override fun load(uri: Any?): BackupSource? =
    throw UnsupportedOperationException(BackupErrors.DB_IMPORT_UNSUPPORTED)

  override fun openDriver(path: String): SqlDriver? =
    throw UnsupportedOperationException("Backup file I/O is not supported on iOS yet")

  override fun createFreshDriver(): SqlDriver? =
    throw UnsupportedOperationException("Backup file I/O is not supported on iOS yet")

  override fun createValidator(): BackupDbValidator = IosBackupDbValidator()

  override fun stageCopy(path: String): String? =
    throw UnsupportedOperationException(BackupErrors.DB_IMPORT_UNSUPPORTED)

  override fun snapshotActiveDb(): String? =
    throw UnsupportedOperationException(BackupErrors.DB_IMPORT_UNSUPPORTED)

  override fun restoreActiveDb(snapshotPath: String?): Boolean =
    throw UnsupportedOperationException(BackupErrors.DB_IMPORT_UNSUPPORTED)

  override fun acquireWriteLock(): Boolean =
    throw UnsupportedOperationException(BackupErrors.DB_IMPORT_UNSUPPORTED)

  override fun releaseWriteLock() =
    throw UnsupportedOperationException(BackupErrors.DB_IMPORT_UNSUPPORTED)
}