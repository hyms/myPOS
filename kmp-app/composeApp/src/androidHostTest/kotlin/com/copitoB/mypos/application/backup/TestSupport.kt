package com.copitoB.mypos.application.backup

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.copitoB.mypos.data.repository.TpvDatabase
import kotlinx.coroutines.runBlocking

class TestBackupFileIo(private val driver: SqlDriver) : BackupFileIo {
  override fun save(fileName: String, bytes: ByteArray): String? = null

  override fun load(uri: Any?): BackupSource? = null

  override fun openDriver(path: String): SqlDriver? = null

  override fun createFreshDriver(): SqlDriver? {
    return runCatching {
      val fresh = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
      runBlocking { TpvDatabase.Schema.create(fresh).await() }
      fresh
    }.getOrNull()
  }

  override fun createValidator(): BackupDbValidator {
    return JdbcBackupDbValidator()
  }

  override fun stageCopy(path: String): String? = path

  override fun snapshotActiveDb(): String? = null

  override fun restoreActiveDb(snapshotPath: String?): Boolean = false

  override fun acquireWriteLock(): Boolean = true

  override fun releaseWriteLock() {}
}