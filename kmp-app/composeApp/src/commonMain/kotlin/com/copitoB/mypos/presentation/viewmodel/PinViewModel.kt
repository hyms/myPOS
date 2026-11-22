package com.copitoB.mypos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.copitoB.mypos.application.security.PinHashStorage
import com.copitoB.mypos.application.security.RemovePin
import com.copitoB.mypos.application.security.SetPin
import com.copitoB.mypos.application.security.VerifyPin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PinViewModel(
  private val pinHashStorage: PinHashStorage,
  private val verifyPin: VerifyPin,
  private val setPin: SetPin,
  private val removePin: RemovePin
) : ViewModel() {

  private val _hasPin = MutableStateFlow(pinHashStorage.getPinHash() != null)
  val hasPin: StateFlow<Boolean> = _hasPin.asStateFlow()

  private val _pinStateLoaded = MutableStateFlow(false)
  val pinStateLoaded: StateFlow<Boolean> = _pinStateLoaded.asStateFlow()

  private val _unlocked = MutableStateFlow(false)
  val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  private val _attempts = MutableStateFlow(0)
  val attempts: StateFlow<Int> = _attempts.asStateFlow()

  fun refresh() {
    _hasPin.value = pinHashStorage.getPinHash() != null
    _pinStateLoaded.value = true
  }

  fun unlock(pin: String, onResult: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      val ok = verifyPin(pin)
      if (ok) {
        _unlocked.value = true
        _attempts.value = 0
        _error.value = null
      } else {
        _attempts.value = _attempts.value + 1
        _error.value = "incorrect_pin"
      }
      onResult(ok)
    }
  }

  fun changePin(currentPin: String, newPin: String, onResult: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      try {
        setPin(newPin, currentPin)
        _error.value = null
        onResult(true)
      } catch (e: Exception) {
        _error.value = e.message ?: "pin_error"
        onResult(false)
      }
    }
  }

  fun setInitialPin(newPin: String, onResult: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      try {
        setPin(newPin, null)
        _hasPin.value = true
        _error.value = null
        onResult(true)
      } catch (e: Exception) {
        _error.value = e.message ?: "pin_error"
        onResult(false)
      }
    }
  }

  fun removePin(currentPin: String, onResult: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      try {
        removePin(currentPin)
        _hasPin.value = false
        _unlocked.value = true
        _error.value = null
        onResult(true)
      } catch (e: Exception) {
        _error.value = e.message ?: "pin_error"
        onResult(false)
      }
    }
  }

  fun clearError() {
    _error.value = null
  }
}