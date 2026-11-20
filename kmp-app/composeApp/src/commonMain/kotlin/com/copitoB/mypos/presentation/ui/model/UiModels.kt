package com.copitoB.mypos.presentation.ui.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.copitoB.mypos.domain.entities.Expense
import com.copitoB.mypos.domain.entities.FinancialSummary
import com.copitoB.mypos.domain.entities.ICartItem
import com.copitoB.mypos.domain.entities.Product
import com.copitoB.mypos.domain.entities.UserSettings
import com.copitoB.mypos.domain.valueobjects.Currency
import com.copitoB.mypos.domain.valueobjects.PaymentMethod
import com.copitoB.mypos.generated.resources.Res
import com.copitoB.mypos.generated.resources.payment_card
import com.copitoB.mypos.generated.resources.payment_cash
import com.copitoB.mypos.generated.resources.payment_qr
import com.copitoB.mypos.generated.resources.payment_transfer
import org.jetbrains.compose.resources.stringResource
import java.math.BigDecimal
import java.math.RoundingMode

@Immutable
data class ProductListUiState(
  val products: List<ProductRowUi>,
  val isLoading: Boolean = false,
  val error: String? = null
)

@Immutable
data class ProductRowUi(
  val id: Long,
  val name: String,
  val skuLabel: String,
  val stockLabel: String,
  val minStockLabel: String,
  val priceLabel: String,
  val isLowStock: Boolean
)

@Immutable
data class CartUiState(
  val lines: List<CartLineUi>,
  val subtotalLabel: String,
  val itemCount: Int
)

@Immutable
data class CartLineUi(
  val productId: Long,
  val name: String,
  val quantityLabel: String,
  val unitPriceLabel: String,
  val subtotalLabel: String
)

@Immutable
data class ExpenseListUiState(
  val expenses: List<ExpenseRowUi>,
  val cashSummary: CashSummaryUi? = null,
  val isLoading: Boolean = false,
  val error: String? = null
)

@Immutable
data class ExpenseRowUi(
  val id: Long,
  val amountLabel: String,
  val description: String,
  val categoryLabel: String?,
  val dateLabel: String,
  val paymentMethodLabel: String
)

@Immutable
data class CashSummaryUi(
  val totalSalesLabel: String,
  val totalPurchasesLabel: String,
  val totalExpensesLabel: String,
  val netLabel: String,
  val isNegative: Boolean
)

@Immutable
data class SettingsUiState(
  val currencyCode: String,
  val pageSizeText: String,
  val biometricEnabled: Boolean,
  val error: String? = null
)

@Immutable
data class PinUiState(
  val hasPin: Boolean,
  val isLocked: Boolean,
  val error: String? = null,
  val isSubmitting: Boolean = false
)

fun Product.toRowUi(currency: Currency): ProductRowUi = ProductRowUi(
  id = id ?: 0L,
  name = name,
  skuLabel = sku?.value ?: "N/A",
  stockLabel = currentStock.toString(),
  minStockLabel = minStock.toString(),
  priceLabel = currency.format(sellingPrice.amount),
  isLowStock = isBelowMinStock()
)

@Composable
fun Expense.toRowUi(currency: Currency): ExpenseRowUi = ExpenseRowUi(
  id = id ?: 0L,
  amountLabel = currency.format(amount.amount),
  description = description,
  categoryLabel = null,
  dateLabel = registeredAt?.take(10) ?: "",
  paymentMethodLabel = paymentMethod.label()
)

fun FinancialSummary.toUi(): CashSummaryUi = CashSummaryUi(
  totalSalesLabel = totalSales.toString(),
  totalPurchasesLabel = totalPurchases.toString(),
  totalExpensesLabel = totalExpenses.toString(),
  netLabel = if (netBalanceIsNegative) "-${netBalance.toString()}" else netBalance.toString(),
  isNegative = netBalanceIsNegative
)

fun ICartItem.toLineUi(currency: Currency): CartLineUi = CartLineUi(
  productId = productId,
  name = productName,
  quantityLabel = quantity.toString(),
  unitPriceLabel = currency.format(BigDecimal.valueOf(unitPrice).setScale(2, RoundingMode.HALF_UP)),
  subtotalLabel = currency.format(BigDecimal.valueOf(subtotal).setScale(2, RoundingMode.HALF_UP))
)

fun UserSettings.toUi(): SettingsUiState = SettingsUiState(
  currencyCode = currencyCode,
  pageSizeText = pageSize.toString(),
  biometricEnabled = biometricEnabled,
  error = null
)

@Composable
fun PaymentMethod.label(): String = when (this) {
  PaymentMethod.CASH -> stringResource(Res.string.payment_cash)
  PaymentMethod.CARD -> stringResource(Res.string.payment_card)
  PaymentMethod.TRANSFER -> stringResource(Res.string.payment_transfer)
  PaymentMethod.QR -> stringResource(Res.string.payment_qr)
}
