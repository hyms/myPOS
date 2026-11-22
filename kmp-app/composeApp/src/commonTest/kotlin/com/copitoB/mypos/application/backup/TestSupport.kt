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
      driver.execute(0, "PRAGMA foreign_keys = ON", 0, null)
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