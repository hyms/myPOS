package com.copitoB.mypos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.copitoB.mypos.domain.entities.Category
import com.copitoB.mypos.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.viewModelScope

class CategoryViewModel(
  private val categoryRepository: CategoryRepository
) : ViewModel() {

  private val _categories = MutableStateFlow<List<Category>>(emptyList())
  val categories: StateFlow<List<Category>> = _categories.asStateFlow()

  private val _loading = MutableStateFlow(false)
  val loading: StateFlow<Boolean> = _loading.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  fun loadCategories() {
    viewModelScope.launch {
      _loading.value = true
      _error.value = null
      try {
        _categories.value = categoryRepository.list()
      } catch (e: Exception) {
        _error.value = e.message
      } finally {
        _loading.value = false
      }
    }
  }

  fun createCategory(name: String) {
    viewModelScope.launch {
      _loading.value = true
      _error.value = null
      try {
        categoryRepository.create(name)
        loadCategories()
      } catch (e: Exception) {
        _error.value = e.message
      } finally {
        _loading.value = false
      }
    }
  }
}
