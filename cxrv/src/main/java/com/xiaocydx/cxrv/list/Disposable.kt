package com.xiaocydx.cxrv.list

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job

/**
 * 表示可废弃的行为
 *
 * [Disposable.dispose]具有幂等性，重复调用不会有副作用
 *
 * ### 调用方式
 * ```
 * val disposable = ...
 * // 主动废弃
 * disposable.dispose()
 * // 自动废弃
 * disposable.autoDispose(lifecycleOwner)
 * disposable.autoDispose(coroutineScope)
 * disposable.autoDispose(job)
 * ```
 *
 * ### 合并方式
 * 用于多个[Disposable]一起废弃的场景
 * ```
 * var disposable = emptyDisposable()
 * val disposable1 = ...
 * val disposable2 = ...
 * disposable = disposable1 + disposable2
 *
 * val disposable3 = ...
 * disposable += disposable3
 * ```
 * @author xcc
 * @date 2021/9/9
 */
interface Disposable {

    /**
     * 是否已废弃
     */
    val isDisposed: Boolean

    /**
     * 执行废弃操作
     *
     * 具有幂等性，重复调用不会有副作用
     */
    fun dispose()

    /**
     * 两个[Disposable]合并为一个，用于多个[Disposable]一起废弃的场景
     */
    operator fun plus(other: Disposable): Disposable = when (other) {
        is EmptyDisposable -> this
        else -> CombinedDisposable(this, other)
    }
}

/**
 * 当[owner]的[Lifecycle]处于销毁状态时，则自动废弃
 */
fun Disposable.autoDispose(owner: LifecycleOwner) {
    owner.lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (source.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                source.lifecycle.removeObserver(this)
                dispose()
            }
        }
    })
}

/**
 * 当[scope]的[Job]处于完成状态时，则自动废弃
 */
fun Disposable.autoDispose(scope: CoroutineScope) {
    require(scope.coroutineContext[Job] != null) {
        "协程作用域的上下文缺少Job"
    }
    autoDispose(scope.coroutineContext.job)
}

/**
 * 当[job]处于完成状态时，则自动废弃
 */
fun Disposable.autoDispose(job: Job) {
    job.invokeOnCompletion { dispose() }
}

/**
 * 空的废弃行为对象，可用于属性初始化场景
 */
fun emptyDisposable(): Disposable = EmptyDisposable

/**
 * [EmptyDisposable]不对外开放，而是提供[emptyDisposable]，
 * 是为了让调用处将[EmptyDisposable]类型推导为[Disposable]，
 * 便于可变属性使用“+=”的合并写法，详细描述请看[Disposable.plus]。
 */
private object EmptyDisposable : Disposable {
    override val isDisposed: Boolean = true

    override fun dispose() {
    }

    override fun plus(other: Disposable): Disposable = other
}

private class CombinedDisposable(
    first: Disposable,
    second: Disposable
) : Disposable {
    private var first: Disposable? = first
    private var second: Disposable? = second
    override val isDisposed: Boolean
        get() = first == null && second == null

    override fun dispose() {
        first?.dispose()
        second?.dispose()
        first = null
        second = null
    }
}