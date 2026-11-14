package com.copitoB.mypos.domain.entities

import com.copitoB.mypos.domain.value-objects.Currency

data class UserSettings(
  val id: Long = 1,
  val currencyCode: String,
  val pageSize: Int,
  val biometricEnabled: Boolean,
  val adEnabled: Boolean,
  val updatedAt: String? = null
) {
  fun currency(): Currency = Currency.fromCode(currencyCode)
}

data class Session(
  val unlocked: Boolean = false
)

data class UpdateSettingsInput(
  val currencyCode: String? = null,
  val pageSize: Int? = null,
  val biometricEnabled: Boolean? = null,
  val adEnabled: Boolean? = null
)

object DefaultSettings {
  val CURRENCY_CODE = "Bs"
  val PAGE_SIZE = 15
  val BIOMETRIC_ENABLED = false
  val AD_ENABLED = false

  val INSTANCE = UserSettings(
    id = 1,
    currencyCode = CURRENCY_CODE,
    pageSize = PAGE_SIZE,
    biometricEnabled = BIOMETRIC_ENABLED,
    adEnabled = AD_ENABLED
  )
}
