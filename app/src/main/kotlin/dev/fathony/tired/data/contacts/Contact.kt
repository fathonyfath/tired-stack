package dev.fathony.tired.data.contacts

import dev.fathony.tired.data.Name

data class Contact(
    val id: Long,
    val name: Name,
    val email: Email,
)
