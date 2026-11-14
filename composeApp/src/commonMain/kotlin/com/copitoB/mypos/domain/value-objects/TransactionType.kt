package com.copitoB.mypos.domain.value-objects

enum class TransactionType(val code: String) {
  SALE("SALE"),
  PURCHASE("PURCHASE"),
  EXPENSE("EXPENSE");
}
