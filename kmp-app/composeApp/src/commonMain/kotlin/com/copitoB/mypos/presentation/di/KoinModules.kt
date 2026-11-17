package com.copitoB.mypos.presentation.di

import com.copitoB.mypos.application.accumulators.GetCashSummary
import com.copitoB.mypos.application.accumulators.GetPeriodSummary
import com.copitoB.mypos.application.accumulators.UpdateAccumulator
import com.copitoB.mypos.application.catalog.ProductManagement
import com.copitoB.mypos.application.expenses.RegisterExpense
import com.copitoB.mypos.application.expenses.ReverseExpense
import com.copitoB.mypos.application.sales.RegisterPurchase
import com.copitoB.mypos.application.sales.RegisterSale
import com.copitoB.mypos.application.security.SetPin
import com.copitoB.mypos.application.security.VerifyPin
import com.copitoB.mypos.application.settings.GetSettings
import com.copitoB.mypos.application.settings.UpdateSettings
import com.copitoB.mypos.data.repository.SqliteAccumulatorRepository
import com.copitoB.mypos.data.repository.SqliteCategoryRepository
import com.copitoB.mypos.data.repository.SqliteExpenseRepository
import com.copitoB.mypos.data.repository.SqliteProductRepository
import com.copitoB.mypos.data.repository.SqliteSaleRepository
import com.copitoB.mypos.data.repository.SqliteSettingsRepository
import com.copitoB.mypos.domain.repository.AccumulatorRepository
import com.copitoB.mypos.domain.repository.CategoryRepository
import com.copitoB.mypos.domain.repository.ExpenseRepository
import com.copitoB.mypos.domain.repository.ProductRepository
import com.copitoB.mypos.domain.repository.SaleRepository
import com.copitoB.mypos.domain.repository.SettingsRepository
import com.copitoB.mypos.presentation.viewmodel.CartViewModel
import com.copitoB.mypos.presentation.viewmodel.CashSummaryViewModel
import com.copitoB.mypos.presentation.viewmodel.CategoryViewModel
import com.copitoB.mypos.presentation.viewmodel.ExpenseCategoryViewModel
import com.copitoB.mypos.presentation.viewmodel.ExpenseViewModel
import com.copitoB.mypos.presentation.viewmodel.MovementsViewModel
import com.copitoB.mypos.presentation.viewmodel.ProductViewModel
import com.copitoB.mypos.presentation.viewmodel.SettingsViewModel
import org.koin.dsl.module

val dataModule = module {
  single<ProductRepository> { SqliteProductRepository(get()) }
  single<SaleRepository> { SqliteSaleRepository(get()) }
  single<ExpenseRepository> { SqliteExpenseRepository(get()) }
  single<CategoryRepository> { SqliteCategoryRepository(get()) }
  single<SettingsRepository> { SqliteSettingsRepository(get()) }
  single<AccumulatorRepository> { SqliteAccumulatorRepository(get()) }
}

val applicationModule = module {
  factory { ProductManagement(get()) }
  factory { RegisterSale(get(), get(), get()) }
  factory { RegisterPurchase(get(), get(), get()) }
  factory { RegisterExpense(get(), get()) }
  factory { ReverseExpense(get(), get()) }
  factory { GetSettings(get()) }
  factory { UpdateSettings(get()) }
  factory { GetCashSummary(get()) }
  factory { GetPeriodSummary(get()) }
  factory { UpdateAccumulator(get()) }
  factory { VerifyPin(get()) }
  factory { SetPin(get(), get()) }
}

val presentationModule = module {
  factory { ProductViewModel(get()) }
  factory { CartViewModel(get()) }
  factory { CategoryViewModel(get()) }
  factory { ExpenseViewModel(get(), get()) }
  factory { ExpenseCategoryViewModel(get()) }
  factory { SettingsViewModel(get(), get()) }
  factory { CashSummaryViewModel(get(), get()) }
  factory { MovementsViewModel(get()) }
}

val allModules = listOf(
  dataModule,
  applicationModule,
  presentationModule
)
