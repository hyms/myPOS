package com.copitoB.mypos.domain.value-objects

data class ChangeAmount(val received: Money, val total: Money) {
  init {
    require(received.currency == total.currency) { "Received and total must use same currency" }
  }

  val change: Money get() = Money(received.amount.minus(total.amount), received.currency)

  fun isSufficient(): Boolean = received.amount.compareTo(total.amount) >= 0

  val colorClass: String get() = if (isSufficient()) "green" else "red"

  override fun toString(): String = "Change: ${change.amount} ${received.currency.code}"
}
