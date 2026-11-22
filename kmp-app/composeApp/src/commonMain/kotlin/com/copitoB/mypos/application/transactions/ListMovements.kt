package com.copitoB.mypos.application.transactions

import com.copitoB.mypos.domain.entities.SaleTransaction
import com.copitoB.mypos.domain.repository.SaleRepository

class ListMovements(
  private val saleRepository: SaleRepository
) {
  data class Page(
    val transactions: List<SaleTransaction>,
    val total: Long
  )

  suspend fun execute(type: String?, search: String?, from: String?, to: String?, limit: Int, offset: Int): Page = Page(
    transactions = saleRepository.listMovements(type, search, from, to, limit, offset),
    total = saleRepository.countMovements(type, search, from, to)
  )
}