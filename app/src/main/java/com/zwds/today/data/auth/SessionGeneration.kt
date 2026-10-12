package com.zwds.today.data.auth

/** A result from a previous account connection must never enter the local history. */
class SessionGeneration {
    private var value = 0L
    @Synchronized fun advance() { value++ }
    @Synchronized fun current(): Long = value
    @Synchronized fun matches(receipt: Long): Boolean = receipt == value
}
