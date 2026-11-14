package com.copitoB.mypos.presentation.di

import com.copitoB.mypos.application.accumulators.GetCashSummary
import com.copitoB.mypos.application.accumulators.GetPeriodSummary
import com.copitoB.mypos.application.accumulators.UpdateAccumulator
import com.copitoB.mypos.data.repository.SqliteAccumulatorRepository
import com.copitoB.mypos.presentation.viewmodel.AccumulatorViewModels
import org.koin.dsl.module

val accumulatorModule = org.koin.dsl.module {
  factory { SqliteAccumulatorRepository(get()) }
  factory { GetCashSummary(get()) }
  factory { GetPeriodSummary(get()) }
  factory { UpdateAccumulator(get()) }
  factory { AccumulatorViewModels::class.java, parameters = true }
}
