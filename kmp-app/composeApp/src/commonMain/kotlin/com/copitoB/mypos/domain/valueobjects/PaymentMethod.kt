package com.copitoB.mypos.domain.valueobjects

enum class PaymentMethod(val code: String) {
  CASH("CASH"),
  CARD("CARD"),
  TRANSFER("TRANSFER"),
  QR("QR");

  fun isCash(): Boolean = this == CASH
}
