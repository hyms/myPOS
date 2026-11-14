package com.copitoB.mypos.domain.value-objects

import java.util.regex.Pattern

class SKU private constructor(val value: String) {
  init {
    require(isValid(value)) { "Invalid SKU: $value" }
  }

  private fun isValid(s: String): Boolean {
    val skuRegex = Pattern.compile("^[A-Z0-9_-]{1,32}$")
    return skuRegex.matcher(s).matches()
  }

  override fun toString(): String = value

  override fun equals(other: Any?): Boolean = other is SKU && value == other.value

  override fun hashCode(): Int = value.hashCode()

  companion object {
    fun create(value: String): SKU = SKU(value.uppercase())
  }
}
