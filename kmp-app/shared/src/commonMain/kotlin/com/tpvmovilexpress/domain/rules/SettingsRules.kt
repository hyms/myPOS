package com.tpvmovilexpress.domain.rules

import com.tpvmovilexpress.domain.valueobjects.CurrencyCode

/**
 * Domain rules for settings validation.
 */
object SettingsRules {

    private const val MIN_PAGE_SIZE = 5
    private const val MAX_PAGE_SIZE = 100

    /**
     * Validates that page size is within allowed range.
     * @throws IllegalArgumentException if page size is out of bounds
     */
    fun validatePageSize(pageSize: Int): Int {
        require(pageSize in MIN_PAGE_SIZE..MAX_PAGE_SIZE) {
            "Page size must be between $MIN_PAGE_SIZE and $MAX_PAGE_SIZE, got $pageSize"
        }
        return pageSize
    }

    /**
     * Validates a currency code string.
     * @throws IllegalArgumentException if code is not supported
     */
    fun validateCurrencyCode(code: String): CurrencyCode {
        return try {
            CurrencyCode.valueOf(code.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Unsupported currency code: $code. Supported: ${CurrencyCode.values().joinToString(", ")}")
        }
    }
}