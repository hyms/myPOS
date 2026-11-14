package com.copitoB.mypos.presentation.di

import com.copitoB.mypos.application.settings.GetSettings
import com.copitoB.mypos.application.settings.UpdateSettings
import com.copitoB.mypos.data.repository.SqliteSettingsRepository
import com.copitoB.mypos.presentation.viewmodel.AccumulatorViewModels
import com.copitoB.mypos.presentation.viewmodel.SettingsViewModels
import org.koin.dsl.module

val settingsModule = org.koin.dsl.module {
  factory { SqliteSettingsRepository(get()) }
  factory { GetSettings(get()) }
  factory { UpdateSettings(get()) }
  factory { SettingsViewModels::class.java, parameters = true }
  factory { SettingsViewModels.AdEnabledProvider(get()) }
}
