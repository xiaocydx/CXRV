package com.xiaocydx.cxrv.payload

import androidx.annotation.CallSuper
import androidx.annotation.CheckResult
import androidx.annotation.IntRange

/**
 * 保存多个`value`的Payload对象
 *
 * @author xcc
 * @date 2022/11/22
 */
sealed class Payload {
    private var values = EMPTY
    private var isComplete = false

    /**
     * 添加[value]
     *
     * **注意**：[value]没有使用value class进行约束，
     * 原因是访问、传递、保存[value]是常见的操作，
     * 如果没处理好这些操作，那么编译结果会产生装箱开销，
     * 这反倒会降低性能和可用性，因此不使用value class。
     *
     * @param value 若不是2的幂次方，则为无效添加
     */
    fun add(@IntRange(from = 1) value: Int) {
        checkComplete()
        if (!check(value)) return
        values = values or value
    }

    protected fun checkComplete() = check(!isComplete) { "已完成添加" }

    /**
     * [values]是否为空，若为空，则表示未调用[add]添加`value`
     */
    @PublishedApi
    internal fun isEmpty() = values == EMPTY

    /**
     * 取[values]的最高位1，可作为遍历的起始值
     */
    @PublishedApi
    internal fun takeHighestValue() = values.takeHighestOneBit()

    /**
     * 取[values]的最低位1，可作为遍历的结束值
     */
    @PublishedApi
    internal fun takeLowestValue() = values.takeLowestOneBit()

    /**
     * 检查[value]是否为2的幂次方
     */
    @PublishedApi
    internal fun check(@IntRange(from = 1) value: Int) = (value and (value - 1)) == 0

    /**
     * [values]是否包含[value]
     */
    @PublishedApi
    internal fun contains(@IntRange(from = 1) value: Int) = (values and value) == value

    /**
     * 去除重复的`value`，合并为一个`values`，[values]已通过检查
     */
    @CheckResult
    internal fun mergeValuesChecked(values: Int): Int {
        return this.values or values
    }

    /**
     * 设置`values`，[values]已通过检查
     */
    internal fun setValuesChecked(values: Int) {
        checkComplete()
        this.values = values
    }

    @PublishedApi
    @CallSuper
    internal open fun complete(): Payload {
        isComplete = true
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Payload
        if (values != other.values) return false
        if (isComplete != other.isComplete) return false
        return true
    }

    override fun hashCode(): Int {
        var result = values
        result = 31 * result + isComplete.hashCode()
        return result
    }

    companion object {
        @PublishedApi
        internal const val EMPTY = 0

        @PublishedApi
        internal const val BASE = 1

        @PublishedApi
        internal fun obtain(): Payload = DiffPayload<Any>()

        @PublishedApi
        internal fun <T : Any> obtainDiff(oldItem: T, newItem: T): DiffPayload<T> {
            @Suppress("UNCHECKED_CAST")
            val payload = (obtain() as? DiffPayload<T>) ?: DiffPayload()
            return payload.init(oldItem, newItem)
        }
    }
}

/**
 * 提取[payloads]的[Payload]，去除重复的`value`，合并为一个[Payload]
 */
@PublishedApi
@CheckResult
internal fun Payload.Companion.merge(payloads: List<Any>): Payload {
    var values = EMPTY
    payloads.forEach action@{
        if (it !is Payload) return@action
        values = it.mergeValuesChecked(values)
    }
    return Payload { setValuesChecked(values) }
}

/**
 * 对每个`value`执行[action]
 */
@PublishedApi
internal inline fun Payload.forEach(action: (value: Int) -> Unit): Payload {
    var value = takeHighestValue()
    val endValue = takeLowestValue()
    while (value != Payload.EMPTY && value >= endValue) {
        if (contains(value)) action(value)
        value = value ushr 1
    }
    return this
}