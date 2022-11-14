package com.xiaocydx.cxrv.payload

import androidx.annotation.CallSuper
import androidx.annotation.IntRange

/**
 * 保存多个`value`的Payload对象
 *
 * @author xcc
 * @date 2022/4/27
 */
open class Payload
@PublishedApi internal constructor() {
    private var isComplete = false
    private var values = EMPTY

    /**
     * 添加[value]
     *
     * **注意**：[value]没有使用value class进行约束，
     * 原因是访问、传递、保存[value]是常见的操作，
     * 如果没处理好这些操作，那么编译结果会产生装箱开销，
     * 这反倒会降低性能和可用性，因此不使用value class。
     *
     * @param value 若不是2的幂次方或者已经添加过，则作为无效添加
     */
    fun add(@IntRange(from = 1) value: Int) {
        checkComplete()
        if (!validate(value) || contains(value)) return
        values = values or value
    }

    protected fun checkComplete() = check(!isComplete) { "已完成添加" }

    /**
     * [values]是否为空，若为空，则表示未调用[add]添加`value`
     */
    @PublishedApi
    internal fun isEmpty() = values == EMPTY

    /**
     * 取[values]的最高位，可作为遍历的起始值
     */
    @PublishedApi
    internal fun takeHighestValue() = values.takeHighestOneBit()

    /**
     * 取[values]的最高位，可作为遍历的结束值
     */
    @PublishedApi
    internal fun takeLowestValue() = values.takeLowestOneBit()

    /**
     * 验证[value]是否为2的幂次方
     */
    @PublishedApi
    internal fun validate(@IntRange(from = 1) value: Int) = (value and (value - 1)) == 0

    /**
     * [values]是否包含[value]
     */
    @PublishedApi
    internal fun contains(@IntRange(from = 1) value: Int) = (values and value) == value

    @CallSuper
    @PublishedApi
    internal open fun complete() = apply { isComplete = true }

    companion object {
        @PublishedApi
        internal const val EMPTY = 0

        @PublishedApi
        internal const val BASE = 1
    }
}