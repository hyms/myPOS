package com.copitoB.mypos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.copitoB.mypos.application.expenses.RegisterExpense
import com.copitoB.mypos.data.repository.ExpenseRepository
import com.copitoB.mypos.domain.entities.Expense
import com.copitoB.mypos.domain.entities.ExpenseCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.viewModelScope

class ExpenseViewModel(
  private val expenseRepository: ExpenseRepository,
  private val registerExpense: RegisterExpense
) : ViewModel() {

  private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
  val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

  private val _loading = MutableStateFlow(false)
  val loading: StateFlow<Boolean> = _loading.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  fun loadExpenses(limit: Int = 20, offset: Int = 0) {
    viewModelScope.launch {
      _loading.value = true
      _error.value = null
      try {
        _expenses.value = expenseRepository.list(limit, offset)
      } catch (e: Exception) {
        _error.value = e.message
      } finally {
        _loading.value = false
      }
    }
  }

  fun registerExpense(
    categoryId: Long,
    amount: String,
    description: String,
    paymentMethod: String
  ) {
    viewModelScope.launch {
      _loading.value = true
      _error.value = null
      try {
        val result = registerExpense.execute(categoryId, amount, description, paymentMethod)
        loadExpenses()
      } catch (e: Exception) {
        _error.value = e.message
      } finally {
        _loading.value = false
      }
    }
  }
}

class ExpenseCategoryViewModel(
  private val expenseRepository: ExpenseRepository
) : ViewModel() {

  private val _categories = MutableStateFlow<List<ExpenseCategory>>(emptyList())
  val categories: StateFlow<List<ExpenseCategory>> = _categories.asStateFlow()

  fun loadCategories() {
    viewModelScope.launch {
      _categories.value = expenseRepository.listCategories()
    }
  }

  fun createCategory(name: String) {
    viewModelScope.launch {
      expenseRepository.createCategory(name)
      loadCategories()
    }
  }
}
