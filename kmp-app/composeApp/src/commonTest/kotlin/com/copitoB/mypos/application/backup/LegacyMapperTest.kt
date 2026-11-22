package com.copitoB.mypos.application.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LegacyMapperTest {

  @Test
  fun `maps legacy transaction types`() {
    assertEquals("SALE", LegacyMapper.mapType("VENTA"))
    assertEquals("PURCHASE", LegacyMapper.mapType("COMPRA"))
    assertEquals("EXPENSE", LegacyMapper.mapType("GASTO"))
  }

  @Test
  fun `passes through already-kmp transaction types`() {
    assertEquals("SALE", LegacyMapper.mapType("SALE"))
    assertEquals("PURCHASE", LegacyMapper.mapType("purchase"))
    assertEquals("EXPENSE", LegacyMapper.mapType("EXPENSE"))
  }

  @Test
  fun `returns null for unknown transaction type`() {
    assertNull(LegacyMapper.mapType("UNKNOWN"))
  }

  @Test
  fun `maps legacy payment methods`() {
    assertEquals("CASH", LegacyMapper.mapPaymentMethod("EFECTIVO"))
    assertEquals("CARD", LegacyMapper.mapPaymentMethod("TARJETA"))
    assertEquals("TRANSFER", LegacyMapper.mapPaymentMethod("TRANSFERENCIA"))
    assertEquals("QR", LegacyMapper.mapPaymentMethod("QR"))
  }

  @Test
  fun `passes through already-kmp payment methods`() {
    assertEquals("CASH", LegacyMapper.mapPaymentMethod("CASH"))
    assertEquals("CARD", LegacyMapper.mapPaymentMethod("card"))
  }

  @Test
  fun `returns null for unknown payment method`() {
    assertNull(LegacyMapper.mapPaymentMethod("BITCOIN"))
  }

  @Test
  fun `normalizes legacy space timestamp to ISO-8601`() {
    assertEquals("2025-01-15T13:45:30Z", LegacyMapper.normalizeTimestamp("2025-01-15 13:45:30"))
  }

  @Test
  fun `passes through already ISO timestamps`() {
    assertEquals("2025-01-15T13:45:30Z", LegacyMapper.normalizeTimestamp("2025-01-15T13:45:30Z"))
  }

  @Test
  fun `returns null for blank timestamp`() {
    assertNull(LegacyMapper.normalizeTimestamp(""))
    assertNull(LegacyMapper.normalizeTimestamp(null))
    assertNull(LegacyMapper.normalizeTimestamp("   "))
  }

  @Test
  fun `sanitizes settings with invalid currency`() {
    val result = LegacyMapper.sanitizeSettings(BackupSettingsDto("XYZ", 999, true))
    assertEquals("Bs", result.currencyCode)
    assertEquals(100, result.pageSize)
  }

  @Test
  fun `uses defaults when settings missing`() {
    val result = LegacyMapper.sanitizeSettings(null)
    assertEquals("Bs", result.currencyCode)
    assertEquals(15, result.pageSize)
    assertEquals(false, result.biometricEnabled)
  }

  @Test
  fun `category updated at falls back to registered at`() {
    assertEquals("2025-01-15T13:45:30Z", LegacyMapper.categoryUpdatedAt("2025-01-15 13:45:30"))
  }

  @Test
  fun `safe sku returns raw value`() {
    assertNull(LegacyMapper.safeSku(null))
    assertEquals("ABC-1", LegacyMapper.safeSku("ABC-1"))
  }
}