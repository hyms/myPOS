package com.copitoB.mypos.domain.rules

import com.copitoB.mypos.domain.entities.ICartItem
import com.copitoB.mypos.domain.value-objects.ChangeAmount
import com.copitoB.mypos.domain.value-objects.Money

object CartRules {
  fun getUnitPrice(item: ICartItem, saleType: String): Double {
    return item.unitPrice
  }

  fun lineSubtotal(item: ICartItem): Double = item.subtotal

  fun calcTotals(items: List<ICartItem>, discount: Double = 0.0): CartTotals {
    val subtotal = items.sumOf { it.subtotal }
    val actualDiscount = discount.coerceIn(0.0, subtotal)
    val total = subtotal - actualDiscount
    return CartTotals(subtotal = subtotal, discount = actualDiscount, total = total)
  }

  fun calcChange(total: Money, received: Money): ChangeAmount {
    return ChangeAmount(received = received, total = total)
  }

  fun applyDiscount(subtotal: Double, discount: Double): Double {
    return discount.coerceIn(0.0, subtotal)
  }
}

data class CartTotals(
  val subtotal: Double,
  val discount: Double,
  val total: Double
)
