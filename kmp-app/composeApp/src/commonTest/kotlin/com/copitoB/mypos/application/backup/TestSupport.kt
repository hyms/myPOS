package com.copitoB.mypos.application.backup

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.copitoB.mypos.application.security.PinHashStorage
import com.copitoB.mypos.data.repository.TpvDatabase
import kotlinx.coroutines.runBlocking

object TestDb {
  fun createDriver(foreignKeys: Boolean = true): SqlDriver {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    runBlocking { TpvDatabase.Schema.create(driver).await() }
    if (foreignKeys) {
      driver.execute(null, "PRAGMA foreign_keys = ON", 0)
    }
    return driver
  }
}

class FakePinHashStorage(initial: String? = null) : PinHashStorage {
  private var hash: String? = initial
  override fun getPinHash(): String? = hash
  override fun storePinHash(hash: String) {
    this.hash = hash
  }
  override fun clearPin() {
    hash = null
  }
}

class FakeBackupFileIo : BackupFileIo {
  override fun save(fileName: String, bytes: ByteArray): String? = null

  override fun load(uri: Any?): BackupSource? = null

  override fun openDriver(path: String): SqlDriver? = null

  override fun createFreshDriver(): SqlDriver? = null

  override fun createValidator(): BackupDbValidator = object : BackupDbValidator {
    override fun open(path: String) {}
    override fun classify(): BackupDbClassification =
      throw UnsupportedOperationException("Test validator not implemented")
    override fun stageInto(target: com.copitoB.mypos.data.repository.TpvDatabase): StagingResult =
      throw UnsupportedOperationException("Test validator not implemented")
    override fun close() {}
  }

  override fun stageCopy(path: String): String? = path

  override fun snapshotActiveDb(): String? = null

  override fun restoreActiveDb(snapshotPath: String?): Boolean = false

  override fun acquireWriteLock(): Boolean = true

  override fun releaseWriteLock() {}
}