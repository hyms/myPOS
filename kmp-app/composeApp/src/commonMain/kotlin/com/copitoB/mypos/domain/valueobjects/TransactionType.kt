package com.copitoB.mypos.domain.valueobjects

enum class TransactionType(val code: String) {
  SALE("SALE"),
  PURCHASE("PURCHASE"),
  EXPENSE("EXPENSE");
}
