package com.copitoB.mypos.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.copitoB.mypos.domain.entities.UpdateSettingsInput
import com.copitoB.mypos.domain.entities.UserSettings
import com.copitoB.mypos.domain.entities.DefaultSettings
import com.copitoB.mypos.domain.repository.SettingsRepository
import com.copitoB.mypos.data.repository.TpvDatabase
import java.time.Instant

class SqliteSettingsRepository(
  private val driver: SqlDriver
) : SettingsRepository {
  private val queries = TpvDatabase(driver)

  override fun get(): UserSettings {
    val row = queries.settingsQueries.settingsFindById(id = 1L).executeAsList().firstOrNull()
    return if (row != null) {
      UserSettings(
        id = row.id,
        currencyCode = row.currency_code,
        pageSize = row.page_size.toInt(),
        biometricEnabled = row.biometric_enabled != 0L,
        adEnabled = row.ad_enabled != 0L,
        updatedAt = row.updated_at
      )
    } else {
      DefaultSettings.INSTANCE
    }
  }

  override fun update(input: UpdateSettingsInput): UserSettings {
    val current = get()
    val merged = current.copy(
      currencyCode = input.currencyCode ?: current.currencyCode,
      pageSize = input.pageSize ?: current.pageSize,
      biometricEnabled = input.biometricEnabled ?: current.biometricEnabled,
      adEnabled = input.adEnabled ?: current.adEnabled,
      updatedAt = Instant.now().toString()
    )
    queries.settingsQueries.settingsUpsert(
      currencyCode = merged.currencyCode,
      pageSize = merged.pageSize.toLong(),
      biometricEnabled = if (merged.biometricEnabled) 1L else 0L,
      adEnabled = if (merged.adEnabled) 1L else 0L,
      updatedAt = merged.updatedAt
    )
    return merged
  }
}