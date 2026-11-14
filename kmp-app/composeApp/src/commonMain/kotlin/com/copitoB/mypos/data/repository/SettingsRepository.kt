package com.copitoB.mypos.data.repository

import com.copitoB.mypos.domain.entities.UserSettings
import com.copitoB.mypos.domain.entities.UpdateSettingsInput
import app.cash.sqldelight.CoroutinesExtensions.awaitFirstOrNull
import app.cash.sqldelight.db
import com.copitoB.mypos.data.repository.TpvDatabase.Companion.Driver

interface SettingsRepository {
  fun get(): UserSettings
  fun update(input: UpdateSettingsInput): UserSettings
}

class SqliteSettingsRepository(
  private val driver: Driver
) : SettingsRepository {
  private val queries = com.copitoB.mypos.data.repository.TpvDatabase(driver)

  override fun get(): UserSettings {
    val row = queries.SettingsQueries.findById(id = 1L).awaitFirstOrNull()
      ?: throw IllegalStateException("Settings not initialized")
    return UserSettings(
      id = row.id,
      currencyCode = row.currencyCode,
      pageSize = row.pageSize,
      biometricEnabled = row.biometricEnabled == 1,
      adEnabled = row.adEnabled == 1,
      updatedAt = row.updatedAt
    )
  }

  override fun update(input: UpdateSettingsInput): UserSettings {
    val current = get()
    queries.db.withTransaction {
      queries.SettingsQueries.update(
        id = 1L,
        currencyCode = input.currencyCode ?: current.currencyCode,
        pageSize = input.pageSize ?: current.pageSize,
        biometricEnabled = if (input.biometricEnabled != null) input.biometricEnabled else current.biometricEnabled,
        adEnabled = if (input.adEnabled != null) input.adEnabled else current.adEnabled,
        updatedAt = java.time.Instant.now().toString()
      )
    }
    return get()
  }
}
