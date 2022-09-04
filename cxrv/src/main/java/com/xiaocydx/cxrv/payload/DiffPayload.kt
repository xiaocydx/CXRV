package com.xiaocydx.cxrv.payload

import androidx.annotation.IntRange

/**
 * @author xcc
 * @date 2022/9/4
 */
internal class DiffPayload<T : Any>(
    private var _oldItem: T? = null,
    private var _newItem: T? = null
) : Payload() {
    @PublishedApi
    internal val oldItem: T
        get() = requireNotNull(_newItem) { "已完成添加" }

    @PublishedApi
    internal val newItem: T
        get() = requireNotNull(_newItem) { "已完成添加" }

    /**
     * 获取不可空的[oldItem]和[newItem]，避免对比基本类型产生装箱开销
     */
    inline fun <K> ifNotEquals(key: T.() -> K): IfNotEquals {
        return IfNotEquals(if (oldItem.key() == newItem.key()) null else this)
    }

    override fun complete(): Payload {
        _oldItem = null
        _newItem = null
        return super.complete()
    }
}

// TODO: 验证不会创建内联值类对象
@JvmInline
internal value class IfNotEquals(private val payload: Payload?) {
    fun add(@IntRange(from = 1) value: Int) {
        payload?.add(value)
    }
}

internal inline fun <T : Any> Payload(oldItem: T, newItem: T, block: DiffPayload<T>.() -> Unit): Payload {
    return DiffPayload(oldItem, newItem).apply(block).complete()
}