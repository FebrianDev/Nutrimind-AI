package com.fdev.nutrimindai.core.cache

/**
 * Represents an in-memory cache slot that distinguishes an uninitialized state ([NotLoaded])
 * from a loaded value ([Value], which may itself hold `null` for nullable entities).
 */
sealed interface Cached<out T> {
    data object NotLoaded : Cached<Nothing>
    data class Value<T>(val data: T) : Cached<T>
}

val <T> Cached<T>.valueOrNull: T?
    get() = (this as? Cached.Value<T>)?.data

