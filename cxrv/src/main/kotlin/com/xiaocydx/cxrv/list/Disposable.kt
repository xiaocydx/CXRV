/*
 * Copyright 2022 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaocydx.cxrv.list

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

/**
 * 表示可废弃的行为，不保证线程安全
 *
 * [Disposable.dispose]具有幂等性，重复调用不会有副作用
 *
 * ### 调用方式
 * ```
 * val disposable = ...
 * // 主动废弃
 * disposable.dispose()
 * // 自动废弃
 * disposable.autoDispose(lifecycle)
 * disposable.autoDispose(lifecycleOwner)
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
 * 当[Lifecycle.getCurrentState]为[Lifecycle.State.DESTROYED]时，自动调用[Disposable.dispose]
 */
fun Disposable.autoDispose(owner: LifecycleOwner) {
    autoDispose(owner.lifecycle)
}

/**
 * 当[Lifecycle.getCurrentState]为[Lifecycle.State.DESTROYED]时，自动调用[Disposable.dispose]
 */
fun Disposable.autoDispose(lifecycle: Lifecycle) {
    lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (source.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                source.lifecycle.removeObserver(this)
                dispose()
            }
        }
    })
}

/**
 * 空的废弃行为对象，可用于属性初始化场景
 */
fun emptyDisposable(): Disposable = EmptyDisposable

/**
 * [Disposable]的包装类，可用于执行过程更改为实际[Disposable]的场景
 */
class DisposableWrapper : Disposable {
    private var delegate: Disposable = emptyDisposable()
    override val isDisposed: Boolean
        get() = delegate.isDisposed

    fun attach(delegate: Disposable) {
        this.delegate = delegate
    }

    fun attachIfEmpty(delegate: Disposable) {
        if (this.delegate != emptyDisposable()) return
        attach(delegate)
    }

    override fun dispose() {
        delegate.dispose()
    }
}

/**
 * [EmptyDisposable]不对外开放，而是提供[emptyDisposable]，
 * 是为了让调用处将[EmptyDisposable]类型推导为[Disposable]，
 * 便于可变属性使用“+=”的合并写法，详细描述请看[Disposable.plus]。
 */
private object EmptyDisposable : Disposable {
    override val isDisposed: Boolean = true

    override fun dispose() = Unit

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