package com.copitoB.mypos.application.security

class VerifyPin(
  private val pinHashStorage: PinHashStorage
) {
  operator fun invoke(pin: String): Boolean {
    val storedHash = pinHashStorage.getPinHash() ?: return false
    val inputHash = Sha256.hash(pin)
    return constantTimeCompare(storedHash, inputHash)
  }

  private fun constantTimeCompare(a: String, b: String): Boolean {
    if (a.length != b.length) return false
    var result = 0
    for (i in a.indices) {
      result = result or (a[i].code xor b[i].code)
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
    pinHashStorage.storePinHash(Sha256.hash(newPin))
  }
}

interface PinHashStorage {
  fun getPinHash(): String?
  fun storePinHash(hash: String)
}