package com.copitoB.mypos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.copitoB.mypos.application.expenses.RegisterExpense
import com.copitoB.mypos.domain.entities.Expense
import com.copitoB.mypos.domain.entities.ExpenseCategory
import com.copitoB.mypos.domain.repository.ExpenseRepository
import com.copitoB.mypos.domain.rules.ExpenseRules
import com.copitoB.mypos.domain.valueobjects.Currency
import com.copitoB.mypos.domain.valueobjects.Money
import com.copitoB.mypos.domain.valueobjects.PaymentMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

sealed interface ExpenseFormError {
  data object Amount : ExpenseFormError
  data object Description : ExpenseFormError
  data object Category : ExpenseFormError
}

class ExpenseViewModel(
  private val expenseRepository: ExpenseRepository,
  private val registerExpense: RegisterExpense
) : ViewModel() {

  private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
  val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

  private val _categories = MutableStateFlow<List<ExpenseCategory>>(emptyList())
  val categories: StateFlow<List<ExpenseCategory>> = _categories.asStateFlow()

  private val _availableMonths = MutableStateFlow<List<String>>(emptyList())
  val availableMonths: StateFlow<List<String>> = _availableMonths.asStateFlow()

  private val _month = MutableStateFlow<String?>(null)
  val month: StateFlow<String?> = _month.asStateFlow()

  private val _hasMore = MutableStateFlow(false)
  val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

  private val _loading = MutableStateFlow(false)
  val loading: StateFlow<Boolean> = _loading.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  private val _formError = MutableStateFlow<ExpenseFormError?>(null)
  val formError: StateFlow<ExpenseFormError?> = _formError.asStateFlow()

  private val _amountText = MutableStateFlow("")
  val amountText: StateFlow<String> = _amountText.asStateFlow()

  private val _description = MutableStateFlow("")
  val description: StateFlow<String> = _description.asStateFlow()

  private val _selectedCategoryId = MutableStateFlow<Long?>(null)
  val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

  private val _selectedPaymentMethod = MutableStateFlow(PaymentMethod.CASH)
  val selectedPaymentMethod: StateFlow<PaymentMethod> = _selectedPaymentMethod.asStateFlow()

  private var pageSize = 15
  private var loaded = 0

  fun setPageSize(size: Int) {
    if (size == pageSize) return
    pageSize = size
    reset()
  }

  fun setMonth(value: String?) {
    if (value == _month.value) return
    _month.value = value
    reset()
  }

  fun setCategory(id: Long?) {
    _selectedCategoryId.value = id
    _formError.value = null
  }

  fun setPaymentMethod(method: PaymentMethod) {
    _selectedPaymentMethod.value = method
  }

  fun setAmount(value: String) {
    _amountText.value = value
    _formError.value = null
  }

  fun setDescription(value: String) {
    _description.value = value
    _formError.value = null
  }

  private fun reset() {
    loaded = 0
    _hasMore.value = false
    _expenses.value = emptyList()
    loadExpenses()
  }

  fun loadExpenses() {
    viewModelScope.launch {
      _loading.value = true
      _error.value = null
      try {
        _categories.value = expenseRepository.listCategories()
        val month = _month.value
        val page = if (month != null) {
          val all = expenseRepository.listByMonth(month)
          all.take(pageSize)
        } else {
          expenseRepository.list(limit = pageSize, offset = 0)
        }
        _expenses.value = page
        loaded = page.size
        _hasMore.value = month == null && page.size == pageSize
        if (month == null) {
          _availableMonths.value = expenseRepository.list(1000, 0)
            .mapNotNull { it.registeredAt?.take(7) }
            .distinct()
            .sortedDescending()
        }
      } catch (e: Exception) {
        _error.value = e.message
      } finally {
        _loading.value = false
      }
    }
  }

  fun loadNextPage() {
    if (!_hasMore.value || _loading.value || _month.value != null) return
    viewModelScope.launch {
      _loading.value = true
      _error.value = null
      try {
        val next = expenseRepository.list(limit = pageSize, offset = loaded)
        _expenses.value = _expenses.value + next
        loaded += next.size
        _hasMore.value = next.size == pageSize
      } catch (e: Exception) {
        _error.value = e.message
      } finally {
        _loading.value = false
      }
    }
  }

  fun validate(): Boolean {
    val amount = Money(BigDecimal(_amountText.value.trim()).setScale(2, RoundingMode.HALF_UP), Currency.Bs)
    val amountOk = runCatching { ExpenseRules.validateExpenseAmount(amount) }.isSuccess
    val descriptionOk = _description.value.trim().isNotEmpty()
    val categoryOk = _selectedCategoryId.value != null
    _formError.value = when {
      !amountOk -> ExpenseFormError.Amount
      !descriptionOk -> ExpenseFormError.Description
      !categoryOk -> ExpenseFormError.Category
      else -> null
    }
    return _formError.value == null
  }

  fun registerExpense(onDone: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      _error.value = null
      if (!validate()) {
        onDone(false)
        return@launch
      }
      _loading.value = true
      try {
        val amount = Money(
          BigDecimal(_amountText.value.trim()).setScale(2, RoundingMode.HALF_UP),
          Currency.Bs
        )
        registerExpense.execute(
          categoryId = _selectedCategoryId.value!!,
          amount = amount,
          description = _description.value,
          paymentMethod = _selectedPaymentMethod.value
        )
        _amountText.value = ""
        _description.value = ""
        _selectedCategoryId.value = null
        _selectedPaymentMethod.value = PaymentMethod.CASH
        _formError.value = null
        loadExpenses()
        onDone(true)
      } catch (e: IllegalArgumentException) {
        _error.value = "expense_error_amount"
        onDone(false)
      } catch (e: Exception) {
        _error.value = e.message
        onDone(false)
      } finally {
        _loading.value = false
      }
    }
  }

  fun createCategory(name: String, onDone: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      _error.value = null
      try {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty())
        val category = expenseRepository.createCategory(trimmed)
        _categories.value = expenseRepository.listCategories()
        _selectedCategoryId.value = category.id
        _formError.value = null
        onDone(true)
      } catch (e: Exception) {
        _error.value = e.message
        onDone(false)
      }
    }
  }

  fun consumeError() {
    _error.value = null
  }
}