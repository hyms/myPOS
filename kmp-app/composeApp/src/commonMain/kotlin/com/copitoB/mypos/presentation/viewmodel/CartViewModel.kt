package com.copitoB.mypos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.copitoB.mypos.domain.entities.ICartItem
import com.copitoB.mypos.application.sales.RegisterSale
import com.copitoB.mypos.domain.value-objects.ChangeAmount
import com.copitoB.mypos.domain.value-objects.Money
import com.copitoB.mypos.domain.value-objects.PaymentMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.viewModelScope

class CartViewModel(
  private val registerSale: RegisterSale
) : ViewModel() {

  private val _items = MutableStateFlow<List<ICartItem>>(emptyList())
  val items: StateFlow<List<ICartItem>> = _items.asStateFlow()

  private val _loading = MutableStateFlow(false)
  val loading: StateFlow<Boolean> = _loading.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  private val _change = MutableStateFlow<ChangeAmount?>(null)
  val change: StateFlow<ChangeAmount?> = _change.asStateFlow()

  fun addItem(item: ICartItem) {
    _items.update { it + item }
    _error.value = null
  }

  fun removeItem(index: Int) {
    _items.update { it.minusAt(index) }
    _error.value = null
  }

  fun clearCart() {
    _items.value = emptyList()
    _change.value = null
    _error.value = null
  }

  fun setChange(amount: String, currency: com.copitoB.mypos.domain.value-objects.Currency) {
    try {
      val received = Money(java.math.BigDecimal(amount), currency)
      if (_items.value.isNotEmpty()) {
        val subtotal = _items.value.sumOf { it.subtotal }
        val total = Money(java.math.BigDecimal(subtotal), currency)
        _change.value = ChangeAmount(received = received, total = total)
      }
    } catch (e: Exception) {
      _error.value = "Invalid amount"
    }
  }

  fun checkout(paymentMethod: PaymentMethod, received: Money?, detail: String? = null) {
    viewModelScope.launch {
      _loading.value = true
      _error.value = null
      try {
        val result = registerSale.execute(
          items = _items.value,
          paymentMethod = paymentMethod,
          received = received,
          detail = detail
        )
        _change.value = result.change
        clearCart()
      } catch (e: Exception) {
        _error.value = e.message
      } finally {
        _loading.value = false
      }
    }
  }
}
