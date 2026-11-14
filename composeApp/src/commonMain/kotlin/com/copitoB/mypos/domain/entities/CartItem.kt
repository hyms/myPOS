package com.copitoB.mypos.domain.entities

sealed interface ICartItem {
  val productId: Long
  val productName: String
  val quantity: Double
  val unitPrice: Double
  val subtotal: Double
}

data class SimpleCartItem(
  override val productId: Long,
  override val productName: String,
  override val quantity: Double,
  override val unitPrice: Double
) : ICartItem {
  override val subtotal: Double get() = quantity * unitPrice
}

object CartItemFactory {
  fun create(productId: Long, productName: String, quantity: Double, unitPrice: Double): ICartItem {
    require(quantity > 0) { "Quantity must be positive" }
    require(unitPrice >= 0) { "Unit price must be non-negative" }
    return SimpleCartItem(productId, productName, quantity, unitPrice)
  }

  fun applyDiscount(item: ICartItem, discount: Double): ICartItem {
    require(discount in 0.0..item.subtotal) { "Discount must be between 0 and subtotal" }
    return SimpleCartItem(
      productId = item.productId,
      productName = item.productName,
      quantity = item.quantity,
      unitPrice = item.unitPrice - (discount / item.quantity)
    )
  }
}
