package com.copitoB.mypos.presentation.di

import com.copitoB.mypos.application.expenses.RegisterExpense
import com.copitoB.mypos.data.repository.SqliteExpenseRepository
import com.copitoB.mypos.presentation.viewmodel.ExpenseViewModels
import org.koin.dsl.module

val expensesModule = org.koin.dsl.module {
  factory { SqliteExpenseRepository(get()) }
  factory { RegisterExpense(get(), get()) }
  factory { ExpenseViewModels::class.java, parameters = true }
}
