package com.copitoB.mypos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.copitoB.mypos.application.catalog.ProductManagement
import com.copitoB.mypos.domain.entities.Product
import com.copitoB.mypos.domain.entities.ProductInput
import com.copitoB.mypos.domain.entities.ProductSort
import com.copitoB.mypos.domain.errors.StockError
import com.copitoB.mypos.domain.rules.LowStockAlert
import com.copitoB.mypos.domain.rules.StockRules
import com.copitoB.mypos.domain.valueobjects.Stock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductViewModel(
  private val productManagement: ProductManagement
) : ViewModel() {

  private val _products = MutableStateFlow<List<Product>>(emptyList())
  val products: StateFlow<List<Product>> = _products.asStateFlow()

  private val _loading = MutableStateFlow(false)
  val loading: StateFlow<Boolean> = _loading.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  fun consumeError() {
    _error.value = null
  }

  private val _lowStockAlerts = MutableStateFlow<List<LowStockAlert>>(emptyList())
  val lowStockAlerts: StateFlow<List<LowStockAlert>> = _lowStockAlerts.asStateFlow()

  private val _hasMore = MutableStateFlow(true)
  val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

  private val _search = MutableStateFlow<String>("")
  val search: StateFlow<String> = _search.asStateFlow()

  private val _categoryId = MutableStateFlow<Long?>(null)
  val categoryId: StateFlow<Long?> = _categoryId.asStateFlow()

  private val _sort = MutableStateFlow(ProductSort.NOMBRE_ASC)
  val sort: StateFlow<ProductSort> = _sort.asStateFlow()

  private val _lowStockOnly = MutableStateFlow(false)
  val lowStockOnly: StateFlow<Boolean> = _lowStockOnly.asStateFlow()

  private var activeSearch: String? = null
  private var activeCategoryId: Long? = null
  private var activeSort: ProductSort = ProductSort.NOMBRE_ASC
  private var activeLowStockOnly: Boolean = false
  private var limit: Int = 15
  private var loaded: Int = 0

  fun setSearch(query: String) {
    _search.value = query
    activeSearch = query.takeIf { it.isNotBlank() }
    reset()
  }

  fun setCategory(id: Long?) {
    _categoryId.value = id
    activeCategoryId = id
    reset()
  }

  fun setSort(value: ProductSort) {
    _sort.value = value
    if (activeSort == value) return
    activeSort = value
    reset()
  }

  fun setLowStockOnly(enabled: Boolean) {
    _lowStockOnly.value = enabled
    if (activeLowStockOnly == enabled) return
    activeLowStockOnly = enabled
    reset()
  }

  fun setPageSize(size: Int) {
    if (size == limit) return
    limit = size
    reset()
  }

  private fun reset() {
    loaded = 0
    _hasMore.value = true
    _products.value = emptyList()
    loadProducts()
  }

  fun loadProducts() {
    viewModelScope.launch {
      _loading.value = true
      _error.value = null
      try {
        val page = productManagement.listProducts(
          search = activeSearch,
          categoryId = activeCategoryId,
          limit = limit,
          offset = 0,
          sort = activeSort,
          lowStockOnly = activeLowStockOnly
        )
        _products.value = page
        loaded = page.size
        _hasMore.value = page.size == limit
      } catch (e: Exception) {
        _error.value = e.message
      } finally {
        _loading.value = false
      }
    }
  }

  fun loadNextPage() {
    if (!_hasMore.value || _loading.value) return
    viewModelScope.launch {
      _loading.value = true
      _error.value = null
      try {
        val next = productManagement.listProducts(
          search = activeSearch,
          categoryId = activeCategoryId,
          limit = limit,
          offset = loaded,
          sort = activeSort,
          lowStockOnly = activeLowStockOnly
        )
        _products.value = _products.value + next
        loaded += next.size
        _hasMore.value = next.size == limit
      } catch (e: Exception) {
        _error.value = e.message
      } finally {
        _loading.value = false
      }
    }
  }

  fun createProduct(input: ProductInput, onDone: () -> Unit = {}) {
    viewModelScope.launch {
      _loading.value = true
      _error.value = null
      try {
        productManagement.createProduct(input)
        loadProducts()
        refreshLowStock()
        onDone()
      } catch (e: Exception) {
        _error.value = e.message
      } finally {
        _loading.value = false
      }
    }
  }

  fun updateProduct(id: Long, input: ProductInput, onDone: () -> Unit = {}) {
    viewModelScope.launch {
      _loading.value = true
      _error.value = null
      try {
        productManagement.updateProduct(id, input)
        loadProducts()
        refreshLowStock()
        onDone()
      } catch (e: Exception) {
        _error.value = e.message
      } finally {
        _loading.value = false
      }
    }
  }

  fun deleteProduct(id: Long, onDone: () -> Unit = {}) {
    viewModelScope.launch {
      _error.value = null
      try {
        productManagement.deleteProduct(id)
        loadProducts()
        refreshLowStock()
        onDone()
      } catch (e: Exception) {
        _error.value = e.message
      }
    }
  }

  fun adjustStock(productId: Long, newStock: String, reason: String, onDone: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      _error.value = null
      try {
        val stock = Stock.parseOrNull(newStock)
          ?: throw StockError.Invalid("'$newStock'")
        productManagement.adjustStock(productId, stock, reason)
        loadProducts()
        refreshLowStock()
        onDone(true)
      } catch (e: StockError.Invalid) {
        _error.value = e.message
        onDone(false)
      } catch (e: StockError.Insufficient) {
        _error.value = e.message
        onDone(false)
      } catch (e: Exception) {
        _error.value = e.message
        onDone(false)
      }
    }
  }

  fun refreshLowStock() {
    viewModelScope.launch {
      try {
        val all = productManagement.getLowStockProducts()
        _lowStockAlerts.value = all.mapNotNull { StockRules.checkLowStock(it) }
      } catch (_: Exception) {
        _lowStockAlerts.value = emptyList()
      }
    }
  }

  suspend fun generateSku(): String = productManagement.generateUniqueSku()
}