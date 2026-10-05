package dev.fathony.tired.data.contacts

private val shape = Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")

/**
 * Shaped like an email address; whether anyone reads mail there is a different claim.
 */
@JvmInline
value class Email(
    private val value: String,
) {
    init {
        require(value.length <= 254 && shape.matches(value)) { "Not an email: $value" }
    }

    override fun toString() = value
}
