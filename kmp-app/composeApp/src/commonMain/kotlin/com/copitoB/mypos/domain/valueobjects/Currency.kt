package com.copitoB.mypos.domain.valueobjects

import java.math.BigDecimal
import java.util.Locale

sealed class Currency(val code: String, val symbol: String) {
  object Bs : Currency("Bs", "Bs")
  object Dollar : Currency("$", "$")
  object Euro : Currency("€", "€")

  val locales: List<Locale>
    get() = when (this) {
      is Bs -> listOf(Locale.forLanguageTag("es-BO"))
      is Dollar -> listOf(Locale.forLanguageTag("en-US"))
      is Euro -> listOf(Locale.forLanguageTag("de-DE"))
    }

  fun format(amount: BigDecimal): String {
    val isoCode = when (this) {
      is Bs -> "BOB"
      is Dollar -> "USD"
      is Euro -> "EUR"
    }
    val nf = java.text.NumberFormat.getCurrencyInstance(locales.firstOrNull() ?: Locale.getDefault())
    nf.currency = java.util.Currency.getInstance(isoCode)
    nf.maximumFractionDigits = 2
    nf.minimumFractionDigits = 2
    return nf.format(amount)
  }

  companion object {
    fun fromCode(code: String): Currency = when (code) {
      "Bs" -> Bs
      "$" -> Dollar
      "€" -> Euro
      else -> throw IllegalArgumentException("Unsupported currency code: $code")
    }
  }
}
