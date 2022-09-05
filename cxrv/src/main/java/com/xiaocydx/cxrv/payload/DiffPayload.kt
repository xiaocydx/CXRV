package com.xiaocydx.cxrv.payload

import androidx.annotation.IntRange

/**
 * @author xcc
 * @date 2022/9/4
 */
internal class DiffPayload<T : Any>(oldItem: T, newItem: T) : Payload() {
    private var oldItem: T? = oldItem
    private var newItem: T? = newItem

    @PublishedApi
    internal fun oldItem(): T {
        checkComplete()
        return oldItem!!
    }

    @PublishedApi
    internal fun newItem(): T {
        checkComplete()
        return newItem!!
    }

    override fun complete(): Payload {
        oldItem = null
        newItem = null
        return super.complete()
    }
}

internal inline fun <T : Any> Payload(oldItem: T, newItem: T, block: DiffPayload<T>.() -> Unit): Payload {
    return DiffPayload(oldItem, newItem).apply(block).complete()
}

/**
 * 获取不可空的[DiffPayload.oldItem]和[DiffPayload.newItem]，避免对比基本类型产生装箱开销
 */
internal inline fun <T : Any, K> DiffPayload<T>.ifNotEquals(key: T.() -> K): IfNotEquals {
    return IfNotEquals(if (oldItem().key() == newItem().key()) null else this)
}

// TODO: 验证不会创建内联值类对象
@JvmInline
internal value class IfNotEquals(private val payload: Payload?) {
    fun add(@IntRange(from = 1) value: Int) {
        payload?.add(value)
    }
}