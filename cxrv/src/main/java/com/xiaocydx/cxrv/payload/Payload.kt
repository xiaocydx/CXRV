package com.xiaocydx.cxrv.payload

import androidx.annotation.CallSuper
import androidx.annotation.IntRange

/**
 * @author xcc
 * @date 2022/4/27
 */
internal open class Payload {
    private var isComplete = false
    private var values = 0

    fun isEmpty(): Boolean {
        return values == 0
    }

    fun add(@IntRange(from = 1) value: Int) {
        checkComplete()
        if (contains(value)) return
        values = values or value
    }

    fun contains(@IntRange(from = 1) value: Int): Boolean {
        return values and value == value
    }

    @CallSuper
    @PublishedApi
    internal open fun complete(): Payload {
        isComplete = true
        return this
    }

    protected fun checkComplete() {
        check(!isComplete) { "已完成添加" }
    }

    companion object {
        @PublishedApi
        internal val empty = Payload()

        inline fun filter(payloads: List<Any>, block: Payload.() -> Unit) {
            var hasPayload = false
            payloads.forEach { (it as? Payload)?.apply { hasPayload = true }?.block() }
            if (!hasPayload) empty.block()
        }
    }
}

internal inline fun Payload(block: Payload.() -> Unit): Payload {
    return Payload().apply(block)
}

internal inline fun Payload.ifEmpty(block: () -> Unit): Payload {
    if (isEmpty()) block()
    return this
}

internal inline fun Payload.ifContains(@IntRange(from = 1) value: Int, block: () -> Unit): Payload {
    if (contains(value)) block()
    return this
}