package com.copitoB.mypos.presentation.di

import com.copitoB.mypos.application.catalog.ProductManagement
import com.copitoB.mypos.data.repository.SqliteCategoryRepository
import com.copitoB.mypos.data.repository.SqliteProductRepository
import com.copitoB.mypos.presentation.viewmodel.CategoryViewModel
import com.copitoB.mypos.presentation.viewmodel.ProductViewModel
import app.cash.sqldelight.db
import org.koin.dsl.module
import app.cash.sqldelight.driver.sqlite.jdbc.JdbcSqliteDriver

val appModule = org.koin.dsl.module {
  factory { ProductManagement(get()) }
  factory { CategoryViewModel(get()) }
  factory { ProductViewModel(get()) }
}

val databaseModule = org.koin.dsl.module {
  single { JdbcSqliteDriver("jdbc:sqlite:tpv.db") }
  single<com.copitoB.mypos.data.repository.TpvDatabase.Companion.Driver> { get() }
  single { SqliteProductRepository(get()) }
  single { SqliteCategoryRepository(get()) }
}
