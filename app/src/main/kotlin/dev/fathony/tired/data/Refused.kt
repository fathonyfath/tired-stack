package dev.fathony.tired.data

/**
 * A request the rules turn down. Thrown inside a write, it also undoes what that write had done so far.
 */
abstract class Refused(
    message: String,
) : Exception(message)
