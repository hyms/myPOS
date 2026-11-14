package com.copitoB.mypos.domain.value-objects

import java.util.Currency as JCurrency
import java.util.Locale

sealed class Currency(val code: String, val symbol: String) {
  object Bs : Currency("Bs", "Bs")
  object Dollar : Currency("$", "$")
  object Euro : Currency("€", "€")

  val locales: List<Locale>
    get() = when (this) {
      is Bs -> listOf(Locale("es", "BO"))
      is Dollar -> listOf(Locale("en", "US"))
      is Euro -> listOf(Locale("de", "DE"))
    }

  fun format(amount: BigDecimal): String {
    val nf = java.text.NumberFormat.getCurrencyInstance(locales.firstOrNull() ?: Locale.getDefault())
    nf.currency = JCurrency.getInstance(code)
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
