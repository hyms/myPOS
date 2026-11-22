package com.copitoB.mypos.security

import android.content.Context
import com.copitoB.mypos.application.security.PinHashStorage

class AndroidPinHashStorage(
  private val context: Context
) : PinHashStorage {
  private val prefs = context.applicationContext
    .getSharedPreferences("mypos_security", Context.MODE_PRIVATE)

  override fun getPinHash(): String? = prefs.getString("pin_hash", null)

  override fun storePinHash(hash: String) {
    prefs.edit().putString("pin_hash", hash).apply()
  }

  override fun clearPin() {
    prefs.edit().remove("pin_hash").apply()
  }
}