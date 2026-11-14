package com.copitoB.mypos.application.settings

import com.copitoB.mypos.domain.entities.UserSettings
import com.copitoB.mypos.domain.entities.UpdateSettingsInput
import com.copitoB.mypos.domain.rules.SettingsRules
import com.copitoB.mypos.data.repository.SettingsRepository

class GetSettings(
  private val settingsRepository: SettingsRepository
) {
  operator fun invoke(): UserSettings {
    return settingsRepository.get()
  }
}

class UpdateSettings(
  private val settingsRepository: SettingsRepository
) {
  operator fun invoke(input: UpdateSettingsInput): UserSettings {
    input.pageSize?.let { SettingsRules.validatePageSize(it) }
    input.currencyCode?.let { SettingsRules.validateCurrencyCode(it) }
    return settingsRepository.update(input)
  }
}
