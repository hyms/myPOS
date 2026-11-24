package com.copitoB.mypos.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.copitoB.mypos.application.backup.BackupFileIo
import com.copitoB.mypos.application.backup.createBackupFileIo
import com.copitoB.mypos.application.security.PinHashStorage
import com.copitoB.mypos.data.repository.TpvDatabase
import com.copitoB.mypos.security.AndroidPinHashStorage
import org.koin.dsl.module

fun androidPlatformModule(context: Context) = module {
  single<SqlDriver> { AndroidSqliteDriver(TpvDatabase.Schema, context, "tpv.db") }
  single<PinHashStorage> { AndroidPinHashStorage(context) }
  single { createBackupFileIo(context) }
}
