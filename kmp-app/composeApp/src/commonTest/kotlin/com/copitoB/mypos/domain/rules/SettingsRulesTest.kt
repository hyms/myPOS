package com.copitoB.mypos.domain.rules

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SettingsRulesTest {
  @Test
  fun `validates page size boundaries`() {
    SettingsRules.validatePageSize(5)
    SettingsRules.validatePageSize(100)
    assertFailsWith<IllegalArgumentException> { SettingsRules.validatePageSize(4) }
    assertFailsWith<IllegalArgumentException> { SettingsRules.validatePageSize(101) }
  }

  @Test
  fun `validates currency codes`() {
    SettingsRules.validateCurrencyCode("Bs")
    SettingsRules.validateCurrencyCode("$")
    SettingsRules.validateCurrencyCode("€")
    assertFailsWith<IllegalArgumentException> { SettingsRules.validateCurrencyCode("XYZ") }
  }
}
