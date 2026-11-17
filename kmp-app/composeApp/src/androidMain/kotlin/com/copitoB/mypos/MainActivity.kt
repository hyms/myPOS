package com.copitoB.mypos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.copitoB.mypos.di.androidPlatformModule
import com.copitoB.mypos.presentation.di.allModules
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    startKoin {
      modules(allModules + androidPlatformModule(applicationContext))
    }
    setContent {
      App()
    }
  }
}
