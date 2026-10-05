package dev.fathony.tired.data

@JvmInline
value class Name(
    private val value: String,
) {
    init {
        require(value.isNotBlank() && value.length <= 100) { "Not a name: $value" }
    }

    override fun toString() = value
}
