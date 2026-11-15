package com.copitoB.mypos.presentation.di

import com.copitoB.mypos.application.catalog.ProductManagement
import com.copitoB.mypos.application.expenses.RegisterExpense
import com.copitoB.mypos.application.expenses.ReverseExpense
import com.copitoB.mypos.application.sales.RegisterPurchase
import com.copitoB.mypos.application.sales.RegisterSale
import com.copitoB.mypos.data.repository.SqliteExpenseRepository
import com.copitoB.mypos.data.repository.SqliteProductRepository
import com.copitoB.mypos.data.repository.SqliteSaleRepository
import com.copitoB.mypos.domain.repository.ExpenseRepository
import com.copitoB.mypos.domain.repository.ProductRepository
import com.copitoB.mypos.domain.repository.SaleRepository
import com.copitoB.mypos.presentation.viewmodel.CartViewModel
import com.copitoB.mypos.presentation.viewmodel.ExpenseCategoryViewModel
import com.copitoB.mypos.presentation.viewmodel.ExpenseViewModel
import com.copitoB.mypos.presentation.viewmodel.ProductViewModel
import org.koin.dsl.module

val appModule = org.koin.dsl.module {
  single<ProductRepository> { SqliteProductRepository(get()) }
  single<SaleRepository> { SqliteSaleRepository(get()) }
  single<ExpenseRepository> { SqliteExpenseRepository(get()) }
  factory { ProductManagement(get()) }
  factory { RegisterSale(get(), get()) }
  factory { RegisterPurchase(get(), get()) }
  factory { RegisterExpense(get(), get()) }
  factory { ReverseExpense(get(), get()) }
  factory { ProductViewModel(get()) }
  factory { CartViewModel(get()) }
  factory { ExpenseViewModel(get(), get()) }
  factory { ExpenseCategoryViewModel(get()) }
}

val databaseModule = org.koin.dsl.module {
  single { SqliteProductRepository(get()) }
  single { SqliteSaleRepository(get()) }
  single { SqliteExpenseRepository(get()) }
}