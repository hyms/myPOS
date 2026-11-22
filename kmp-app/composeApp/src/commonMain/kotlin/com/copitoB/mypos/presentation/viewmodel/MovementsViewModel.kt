package com.copitoB.mypos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.copitoB.mypos.application.transactions.ListMovements
import com.copitoB.mypos.application.transactions.ReverseTransaction
import com.copitoB.mypos.domain.entities.SaleTransaction
import com.copitoB.mypos.domain.entities.TransactionDetail
import com.copitoB.mypos.domain.errors.StockError
import com.copitoB.mypos.domain.repository.ProductRepository
import com.copitoB.mypos.domain.repository.SaleRepository
import com.copitoB.mypos.domain.valueobjects.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RevertState {
  data object Idle : RevertState
  data object AwaitingPin : RevertState
  data object Confirming : RevertState
  data object Reverting : RevertState
  data object Done : RevertState
  data class Failed(val reasonKey: String) : RevertState
}

class MovementsViewModel(
  private val listMovements: ListMovements,
  private val reverseTransaction: ReverseTransaction,
  private val saleRepository: SaleRepository,
  private val productRepository: ProductRepository
) : ViewModel() {

  private val _movements = MutableStateFlow<List<SaleTransaction>>(emptyList())
  val movements: StateFlow<List<SaleTransaction>> = _movements.asStateFlow()

  private val _filter = MutableStateFlow<TransactionType?>(null)
  val filter: StateFlow<TransactionType?> = _filter.asStateFlow()

  private val _search = MutableStateFlow("")
  val search: StateFlow<String> = _search.asStateFlow()

  private val _total = MutableStateFlow(0L)
  val total: StateFlow<Long> = _total.asStateFlow()

  private val _hasMore = MutableStateFlow(false)
  val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

  private val _loading = MutableStateFlow(false)
  val loading: StateFlow<Boolean> = _loading.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  private val _selected = MutableStateFlow<SaleTransaction?>(null)
  val selected: StateFlow<SaleTransaction?> = _selected.asStateFlow()

  private val _detail = MutableStateFlow<List<TransactionDetail>>(emptyList())
  val detail: StateFlow<List<TransactionDetail>> = _detail.asStateFlow()

  private val _productNames = MutableStateFlow<Map<Long, String>>(emptyMap())
  val productNames: StateFlow<Map<Long, String>> = _productNames.asStateFlow()

  private val _revertState = MutableStateFlow<RevertState>(RevertState.Idle)
  val revertState: StateFlow<RevertState> = _revertState.asStateFlow()

  private var pageSize = 15
  private var loaded = 0

  fun setPageSize(size: Int) {
    if (size == pageSize) return
    pageSize = size
    reset()
  }

  fun setFilter(type: TransactionType?) {
    if (type == _filter.value) return
    _filter.value = type
    reset()
  }

  fun setSearch(value: String) {
    if (value == _search.value) return
    _search.value = value
    reset()
  }

  private fun reset() {
    loaded = 0
    _hasMore.value = false
    _movements.value = emptyList()
    load()
  }

  fun load() {
    viewModelScope.launch {
      _loading.value = true
      _error.value = null
      try {
        val page = listMovements.execute(
          type = _filter.value?.code,
          search = _search.value.ifBlank { null },
          from = null,
          to = null,
          limit = pageSize,
          offset = 0
        )
        _movements.value = page.transactions
        _total.value = page.total
        loaded = page.transactions.size
        _hasMore.value = page.transactions.size == pageSize
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
        val next = listMovements.execute(
          type = _filter.value?.code,
          search = _search.value.ifBlank { null },
          from = null,
          to = null,
          limit = pageSize,
          offset = loaded
        )
        _movements.value = _movements.value + next.transactions
        _total.value = next.total
        loaded += next.transactions.size
        _hasMore.value = next.transactions.size == pageSize
      } catch (e: Exception) {
        _error.value = e.message
      } finally {
        _loading.value = false
      }
    }
  }

  fun openDetail(transaction: SaleTransaction) {
    _selected.value = transaction
    _revertState.value = RevertState.Idle
    viewModelScope.launch {
      val details = saleRepository.listDetails(transaction.id ?: 0L)
      _detail.value = details
      val names = details.mapNotNull { detail ->
        val product = productRepository.findById(detail.productId)
        if (product != null) detail.productId to product.name else null
      }.toMap()
      _productNames.value = names
    }
  }

  fun closeDetail() {
    _selected.value = null
    _detail.value = emptyList()
    _productNames.value = emptyMap()
    _revertState.value = RevertState.Idle
  }

  fun requestRevert(hasPin: Boolean) {
    _revertState.value = if (hasPin) RevertState.AwaitingPin else RevertState.Confirming
  }

  fun submitPin(pin: String, verify: suspend (String) -> Boolean, onVerified: () -> Unit = {}) {
    viewModelScope.launch {
      val ok = verify(pin)
      _revertState.value = if (ok) RevertState.Confirming else RevertState.AwaitingPin
      if (ok) onVerified()
    }
  }

  fun cancelRevert() {
    _revertState.value = RevertState.Idle
  }

  fun confirmRevert(onDone: (Boolean) -> Unit = {}) {
    val transaction = _selected.value ?: return onDone(false)
    val id = transaction.id ?: return onDone(false)
    _revertState.value = RevertState.Reverting
    viewModelScope.launch {
      try {
        reverseTransaction.execute(id)
        _revertState.value = RevertState.Done
        closeDetail()
        load()
        onDone(true)
      } catch (e: StockError.Insufficient) {
        _revertState.value = RevertState.Failed("accumulator_revert_error_stock")
        onDone(false)
      } catch (e: IllegalStateException) {
        _revertState.value = RevertState.Failed("accumulator_revert_error_already")
        onDone(false)
      } catch (e: IllegalArgumentException) {
        _revertState.value = RevertState.Failed("accumulator_revert_error_generic")
        onDone(false)
      } catch (e: Exception) {
        _revertState.value = RevertState.Failed("accumulator_revert_error_generic")
        onDone(false)
      }
    }
  }

  fun consumeError() {
    _error.value = null
  }
}