package com.copitoB.mypos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.copitoB.mypos.application.accumulators.GetCashSummary
import com.copitoB.mypos.application.accumulators.GetPeriodSummary
import com.copitoB.mypos.application.accumulators.UpdateAccumulator
import com.copitoB.mypos.domain.entities.FinancialSummary
import com.copitoB.mypos.domain.entities.PeriodSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.viewModelScope

class CashSummaryViewModel(
  private val getCashSummary: GetCashSummary,
  private val updateAccumulator: UpdateAccumulator
) : ViewModel() {

  private val _summary = MutableStateFlow<FinancialSummary?>(null)
  val summary: StateFlow<FinancialSummary?> = _summary.asStateFlow()

  private val _loading = MutableStateFlow(false)
  val loading: StateFlow<Boolean> = _loading.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  fun loadSummary() {
    viewModelScope.launch {
      _loading.value = true
      _error.value = null
      try {
        _summary.value = getCashSummary()
      } catch (e: Exception) {
        _error.value = e.message
      } finally {
        _loading.value = false
      }
    }
  }

  fun update(salesDelta: Double, purchasesDelta: Double, expensesDelta: Double) {
    viewModelScope.launch {
      _loading.value = true
      _error.value = null
      try {
        updateAccumulator(
          UpdateAccumulator.Deltas(
            salesDelta = com.copitoB.mypos.domain.value-objects.Money(
              java.math.BigDecimal(salesDelta),
              com.copitoB.mypos.domain.value-objects.Currency.Bs
            ),
            purchasesDelta = com.copitoB.mypos.domain.value-objects.Money(
              java.math.BigDecimal(purchasesDelta),
              com.copitoB.mypos.domain.value-objects.Currency.Bs
            ),
            expensesDelta = com.copitoB.mypos.domain.value-objects.Money(
              java.math.BigDecimal(expensesDelta),
              com.copitoB.mypos.domain.value-objects.Currency.Bs
            )
          )
        )
        loadSummary()
      } catch (e: Exception) {
        _error.value = e.message
      } finally {
        _loading.value = false
      }
    }
  }
}

class MovementsViewModel(
  private val getPeriodSummary: GetPeriodSummary
) : ViewModel() {

  private val _periodSummary = MutableStateFlow<PeriodSummary?>(null)
  val periodSummary: StateFlow<PeriodSummary?> = _periodSummary.asStateFlow()

  private val _loading = MutableStateFlow(false)
  val loading: StateFlow<Boolean> = _loading.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  fun loadPeriodSummary(period: String) {
    viewModelScope.launch {
      _loading.value = true
      _error.value = null
      try {
        _periodSummary.value = getPeriodSummary(period)
      } catch (e: Exception) {
        _error.value = e.message
      } finally {
        _loading.value = false
      }
    }
  }
}
