package com.copitoB.mypos.domain.rules

object SettingsRules {
  fun validatePageSize(pageSize: Int) {
    require(pageSize in 5..100) { "Page size must be between 5 and 100" }
    require(pageSize == pageSize.toInt()) { "Page size must be an integer" }
  }

  fun validateCurrencyCode(code: String) {
    require(code in listOf("Bs", "$", "€")) { "Invalid currency code: $code" }
  }
}
