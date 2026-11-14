package com.copitoB.mypos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.copitoB.mypos.application.catalog.ProductManagement
import com.copitoB.mypos.domain.entities.Product
import com.copitoB.mypos.domain.entities.ProductInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.viewModelScope

class ProductViewModel(
  private val productManagement: ProductManagement
) : ViewModel() {

  private val _products = MutableStateFlow<List<Product>>(emptyList())
  val products: StateFlow<List<Product>> = _products.asStateFlow()

  private val _loading = MutableStateFlow(false)
  val loading: StateFlow<Boolean> = _loading.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  fun loadProducts(search: String? = null, categoryId: Long? = null, limit: Int = 20, offset: Int = 0) {
    viewModelScope.launch {
      _loading.value = true
      _error.value = null
      try {
        _products.value = productManagement.listProducts(search, categoryId, limit, offset)
      } catch (e: Exception) {
        _error.value = e.message
      } finally {
        _loading.value = false
      }
    }
  }

  fun createProduct(input: ProductInput) {
    viewModelScope.launch {
      _loading.value = true
      _error.value = null
      try {
        productManagement.createProduct(input)
        loadProducts()
      } catch (e: Exception) {
        _error.value = e.message
      } finally {
        _loading.value = false
      }
    }
  }

  fun deleteProduct(id: Long) {
    viewModelScope.launch {
      productManagement.deleteProduct(id)
      loadProducts()
    }
  }

  fun adjustStock(productId: Long, newStock: String, reason: String) {
    viewModelScope.launch {
      val stock = com.copitoB.mypos.domain.value-objects.Stock.parseOrNull(newStock)
      if (stock == null) {
        _error.value = "Invalid stock value"
        return@launch
      }
      productManagement.adjustStock(productId, stock, reason)
      loadProducts()
    }
  }
}
