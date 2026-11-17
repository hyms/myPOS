package com.copitoB.mypos.domain.repository

import com.copitoB.mypos.domain.entities.UserSettings
import com.copitoB.mypos.domain.entities.UpdateSettingsInput

interface SettingsRepository {
  fun get(): UserSettings
  fun update(input: UpdateSettingsInput): UserSettings
}