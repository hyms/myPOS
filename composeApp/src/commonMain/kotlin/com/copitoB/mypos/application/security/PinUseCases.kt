package com.copitoB.mypos.application.security

import kotlin.math.BigInteger
import kotlin.security.MessageDigest

class VerifyPin(
  private val pinHashStorage: PinHashStorage
) {
  operator fun invoke(pin: String): Boolean {
    val storedHash = pinHashStorage.getPinHash() ?: return false
    val inputHash = sha256(pin)
    return constantTimeCompare(storedHash, inputHash)
  }

  private fun sha256(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
  }

  private fun constantTimeCompare(a: String, b: String): Boolean {
    if (a.length != b.length) return false
    var result = 0
    for (i in a.indices) {
      result = result or (a[i].code.toInt() xor b[i].code.toInt())
    }
    return result == 0
  }
}

class SetPin(
  private val pinHashStorage: PinHashStorage,
  private val verifyPin: VerifyPin
) {
  operator fun invoke(newPin: String, currentPin: String? = null) {
    if (currentPin != null) {
      require(verifyPin(currentPin)) { "Current PIN is incorrect" }
    }
    val hash = sha256(newPin)
    pinHashStorage.storePinHash(hash)
  }

  private fun sha256(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
  }
}

interface PinHashStorage {
  fun getPinHash(): String?
  fun storePinHash(hash: String)
}
