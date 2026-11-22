package com.copitoB.mypos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.copitoB.mypos.application.sales.RegisterPurchase
import com.copitoB.mypos.domain.entities.ICartItem
import com.copitoB.mypos.domain.errors.StockError
import com.copitoB.mypos.domain.valueobjects.Money
import com.copitoB.mypos.domain.valueobjects.PaymentMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PurchaseCartViewModel(
  private val registerPurchase: RegisterPurchase
) : ViewModel() {

  private val _items = MutableStateFlow<List<ICartItem>>(emptyList())
  val items: StateFlow<List<ICartItem>> = _items.asStateFlow()

  private val _loading = MutableStateFlow(false)
  val loading: StateFlow<Boolean> = _loading.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  fun consumeError() {
    _error.value = null
  }

  private val _discount = MutableStateFlow(0.0)
  val discount: StateFlow<Double> = _discount.asStateFlow()

  fun addItem(item: ICartItem) {
    _items.update { it + item }
    _error.value = null
  }

  fun replaceItem(index: Int, item: ICartItem) {
    _items.update { current -> current.mapIndexed { i, old -> if (i == index) item else old } }
    _error.value = null
  }

  fun removeItem(index: Int) {
    _items.update { it.filterIndexed { i, _ -> i != index } }
    _error.value = null
  }

  fun clearCart() {
    _items.value = emptyList()
    _discount.value = 0.0
    _error.value = null
  }

  fun setDiscount(amount: String) {
    val parsed = amount.toDoubleOrNull() ?: 0.0
    val subtotal = _items.value.sumOf { it.subtotal }
    _discount.value = parsed.coerceIn(0.0, subtotal)
    _error.value = null
  }

  fun checkout(paymentMethod: PaymentMethod, detail: String? = null, onDone: (Boolean) -> Unit = {}) {
    _loading.value = true
    _error.value = null
    var outcome = false
    try {
      kotlinx.coroutines.runBlocking {
        registerPurchase.execute(
          items = _items.value,
          paymentMethod = paymentMethod,
          discount = _discount.value,
          detail = detail
        )
      }
      _items.value = emptyList()
      _discount.value = 0.0
      outcome = true
    } catch (e: StockError.Insufficient) {
      _error.value = "stock_error"
    } catch (e: Exception) {
      _error.value = e.message
    } finally {
      _loading.value = false
    }
    onDone(outcome)
  }
}