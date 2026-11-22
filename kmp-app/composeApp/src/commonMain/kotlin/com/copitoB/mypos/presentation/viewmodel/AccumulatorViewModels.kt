package com.copitoB.mypos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.copitoB.mypos.application.accumulators.GetCashSummary
import com.copitoB.mypos.application.accumulators.GetPeriodSummary
import com.copitoB.mypos.application.accumulators.UpdateAccumulator
import com.copitoB.mypos.domain.entities.FinancialSummary
import com.copitoB.mypos.domain.entities.PeriodSummary
import com.copitoB.mypos.domain.valueobjects.Money
import com.copitoB.mypos.domain.valueobjects.Currency
import com.copitoB.mypos.domain.valueobjects.PaymentMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

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
            salesDelta = Money(BigDecimal.valueOf(salesDelta), Currency.Bs),
            purchasesDelta = Money(BigDecimal.valueOf(purchasesDelta), Currency.Bs),
            expensesDelta = Money(BigDecimal.valueOf(expensesDelta), Currency.Bs)
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

class MovementsPeriodViewModel(
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

enum class ReportPeriod { TODAY, MONTH, CUSTOM }

class ReportsViewModel(
  private val getReportSummary: com.copitoB.mypos.application.accumulators.GetReportSummary,
  private val getPaymentBreakdown: com.copitoB.mypos.application.accumulators.GetPaymentBreakdown
) : ViewModel() {

  data class ReportState(
    val totals: PeriodSummary? = null,
    val payment: Map<PaymentMethod, Money> = emptyMap(),
    val monthly: List<com.copitoB.mypos.domain.entities.MonthlyBreakdown> = emptyList(),
    val annual: List<com.copitoB.mypos.domain.entities.AnnualBreakdown> = emptyList()
  )

  private val _report = MutableStateFlow(ReportState())
  val report: StateFlow<ReportState> = _report.asStateFlow()

  private val _period = MutableStateFlow(ReportPeriod.MONTH)
  val period: StateFlow<ReportPeriod> = _period.asStateFlow()

  private val _from = MutableStateFlow<String?>(null)
  val from: StateFlow<String?> = _from.asStateFlow()

  private val _to = MutableStateFlow<String?>(null)
  val to: StateFlow<String?> = _to.asStateFlow()

  private val _customError = MutableStateFlow(false)
  val customError: StateFlow<Boolean> = _customError.asStateFlow()

  private val _loading = MutableStateFlow(false)
  val loading: StateFlow<Boolean> = _loading.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  fun setPeriod(value: ReportPeriod) {
    _period.value = value
    if (value != ReportPeriod.CUSTOM) {
      _from.value = null
      _to.value = null
      _customError.value = false
      load()
    }
  }

  fun applyCustom(fromIso: String?, toIso: String?) {
    val from = fromIso?.trim()?.takeIf { it.isNotEmpty() }
    val to = toIso?.trim()?.takeIf { it.isNotEmpty() }
    if (from != null && to != null && from > to) {
      _customError.value = true
      return
    }
    _customError.value = false
    _from.value = from
    _to.value = to
    _period.value = ReportPeriod.CUSTOM
    load()
  }

  fun load() {
    viewModelScope.launch {
      _loading.value = true
      _error.value = null
      try {
        val result = getReportSummary(_from.value, _to.value)
        val payment = getPaymentBreakdown(_from.value, _to.value)
        _report.value = ReportState(
          totals = result.totals,
          payment = payment,
          monthly = result.monthly,
          annual = result.annual
        )
      } catch (e: Exception) {
        _error.value = e.message
      } finally {
        _loading.value = false
      }
    }
  }
}
