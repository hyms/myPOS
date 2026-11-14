package com.copitoB.mypos

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

import UIKit

class MainViewController : UIViewController() {
  override fun viewDidLoad() {
    super.viewDidLoad()
    ComposeUIViewController(
      theme = MaterialTheme,
      content = {
        Surface(modifier = Modifier.fillMaxSize()) {
          App()
        }
      }
    ).also { addChild(it); view.addSubview(it.view); it.didMove(toParent: this) }
  }
}
