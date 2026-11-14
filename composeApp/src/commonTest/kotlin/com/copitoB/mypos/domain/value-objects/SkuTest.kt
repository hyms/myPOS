package com.copitoB.mypos.domain.value-objects

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SkuTest {
  @Test
  fun `creates valid SKU`() {
    val sku = SKU.create("ABC-123")
    assertEquals("ABC-123", sku.value)
  }

  @Test
  fun `uppercases SKU on creation`() {
    val sku = SKU.create("abc-123")
    assertEquals("ABC-123", sku.value)
  }

  @Test
  fun `rejects empty SKU`() {
    assertFailsWith<IllegalArgumentException> { SKU.create("") }
  }

  @Test
  fun `rejects SKU longer than 32 chars`() {
    assertFailsWith<IllegalArgumentException> {
      SKU.create("A".repeat(33))
    }
  }

  @Test
  fun `rejects invalid characters`() {
    assertFailsWith<IllegalArgumentException> { SKU.create("INVALID!") }
  }

  @Test
  fun `equals and hashCode work`() {
    assertEquals(SKU.create("ABC"), SKU.create("ABC"))
    assertEquals(SKU.create("ABC").hashCode(), SKU.create("ABC").hashCode())
  }
}
