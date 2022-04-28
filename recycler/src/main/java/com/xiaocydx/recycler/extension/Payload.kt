package com.xiaocydx.recycler.extension

import androidx.annotation.IntRange

/**
 * @author xcc
 * @date 2022/4/27
 */
class Payload {
    private var values = 0

    fun isEmpty(): Boolean {
        return values == 0
    }

    fun add(@IntRange(from = 1) value: Int) {
        values = values or value
    }

    fun remove(@IntRange(from = 1) value: Int) {
        values = values and value.inv()
    }

    fun contains(@IntRange(from = 1) value: Int): Boolean {
        return values and value == value
    }

    companion object {
        @PublishedApi
        internal val empty = Payload()

        inline fun filter(payloads: List<Any>, block: Payload.() -> Unit) {
            if (payloads.isEmpty()) return empty.block()
            payloads.forEach { (it as? Payload)?.block() }
        }
    }
}

inline fun Payload(block: Payload.() -> Unit): Payload {
    return Payload().apply(block)
}

inline fun <T : Any> Payload.addIfNotEquals(
    @IntRange(from = 1) value: Int,
    oldItem: T,
    newItem: T,
    key: T.() -> Any?
): Payload = addIf(
    value = value,
    predicate = oldItem.key() != newItem.key()
)

fun Payload.addIf(@IntRange(from = 1) value: Int, predicate: Boolean): Payload {
    if (predicate) add(value)
    return this
}

inline fun Payload.ifEmpty(block: () -> Unit): Payload {
    if (isEmpty()) block()
    return this
}

inline fun Payload.ifContains(@IntRange(from = 1) value: Int, block: () -> Unit): Payload {
    if (contains(value)) block()
    return this
}