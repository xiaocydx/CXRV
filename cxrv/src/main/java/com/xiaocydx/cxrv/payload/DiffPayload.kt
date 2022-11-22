package com.xiaocydx.cxrv.payload

import androidx.annotation.IntRange

/**
 * 比较[oldItem]和[newItem]，保存多个`value`的Payload对象
 *
 * @author xcc
 * @date 2022/9/4
 */
class DiffPayload<T : Any> internal constructor() : Payload() {
    private var oldItem: T? = null
    private var newItem: T? = null

    @PublishedApi
    internal fun init(oldItem: T, newItem: T): DiffPayload<T> {
        checkComplete()
        this.oldItem = oldItem
        this.newItem = newItem
        return this
    }

    /**
     * 获取不可空的`oldItem`，避免比较基本类型的属性产生装箱开销
     */
    @PublishedApi
    internal fun oldItem(): T {
        checkComplete()
        return oldItem!!
    }

    /**
     * 获取不可空的`newItem`，避免比较基本类型的属性产生装箱开销
     */
    @PublishedApi
    internal fun newItem(): T {
        checkComplete()
        return newItem!!
    }

    @PublishedApi
    override fun complete(): Payload {
        oldItem = null
        newItem = null
        return super.complete()
    }

    @PublishedApi
    override fun reset(): Payload {
        oldItem = null
        newItem = null
        return super.reset()
    }
}

/**
 * 若`oldItem`和`newItem`的指定属性不相等，则调用[Payload.add]添加`value`
 */
@JvmInline
value class IfNotEquals
@PublishedApi internal constructor(private val payload: Payload?) {
    fun add(@IntRange(from = 1) value: Int) {
        payload?.add(value)
    }
}