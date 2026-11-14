package com.copitoB.mypos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.copitoB.mypos.application.settings.GetSettings
import com.copitoB.mypos.application.settings.UpdateSettings
import com.copitoB.mypos.domain.entities.UserSettings
import com.copitoB.mypos.domain.entities.UpdateSettingsInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.viewModelScope

class SettingsViewModel(
  private val getSettings: GetSettings,
  private val updateSettings: UpdateSettings
) : ViewModel() {

  private val _settings = MutableStateFlow<UserSettings?>(null)
  val settings: StateFlow<UserSettings?> = _settings.asStateFlow()

  private val _loading = MutableStateFlow(false)
  val loading: StateFlow<Boolean> = _loading.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  fun loadSettings() {
    viewModelScope.launch {
      _loading.value = true
      _error.value = null
      try {
        _settings.value = getSettings()
      } catch (e: Exception) {
        _error.value = e.message
      } finally {
        _loading.value = false
      }
    }
  }

  fun updateSettings(input: UpdateSettingsInput) {
    viewModelScope.launch {
      _loading.value = true
      _error.value = null
      try {
        _settings.value = updateSettings(input)
      } catch (e: Exception) {
        _error.value = e.message
      } finally {
        _loading.value = false
      }
    }
  }
}

class AdEnabledProvider(
  private val settingsViewModel: SettingsViewModel
) {
  val isAdEnabled: Boolean
    get() = settingsViewModel.settings.value?.adEnabled == true
}
