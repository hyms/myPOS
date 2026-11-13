package com.tpvmovilexpress.domain.valueobjects

import kotlin.text.Regex

/**
 * Immutable value object representing a Stock Keeping Unit (SKU).
 * Normalizes input to uppercase alphanumeric with hyphens/underscores only.
 * Maximum length of 32 characters.
 */
@JvmInline
public value class SKU private constructor(
    public val value: String
) {

    public companion object {
        private const val MAX_LENGTH = 32
        private val VALID_PATTERN = Regex("^[A-Z0-9_-]+$")

        /**
         * Creates an [SKU] from raw input.
         * Normalizes by trimming and converting to uppercase.
         * @throws IllegalArgumentException if SKU is empty, too long, or contains invalid characters
         */
        public fun create(raw: String): SKU {
            val normalized = raw.trim().uppercase()
            require(normalized.isNotBlank()) { "SKU cannot be empty" }
            require(normalized.length <= MAX_LENGTH) { "SKU max length is $MAX_LENGTH, got ${normalized.length}" }
            require(VALID_PATTERN.matches(normalized)) {
                "SKU must be alphanumeric with hyphen/underscore only, got: $normalized"
            }
            return SKU(normalized)
        }

        /**
         * Creates an [SKU] from an already normalized value (internal use).
         */
        internal fun createNormalized(normalized: String): SKU = SKU(normalized)
    }

    /**
     * Checks if this SKU equals another.
     */
    public fun equals(other: SKU): Boolean = this.value == other.value

    override fun toString(): String = value
}

/**
 * Extension function for easier SKU creation.
 */
public fun String.toSKU(): SKU = SKU.create(this)
