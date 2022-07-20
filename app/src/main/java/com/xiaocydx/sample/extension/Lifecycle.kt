@file:Suppress("PackageDirectoryMismatch")

package com.xiaocydx.sample

import androidx.annotation.CheckResult
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

val Fragment.viewLifecycle: Lifecycle
    get() = viewLifecycleOwner.lifecycle

val Fragment.viewLifecycleScope: CoroutineScope
    get() = viewLifecycleOwner.lifecycleScope

inline fun Lifecycle.doOnStateChanged(
    once: Boolean = false,
    crossinline action: (source: LifecycleOwner, event: Lifecycle.Event) -> Unit
): LifecycleObserver = object : LifecycleEventObserver {
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (once) source.lifecycle.removeObserver(this)
        action(source, event)
    }
}.also(::addObserver)

inline fun Lifecycle.doOnTargetState(
    state: Lifecycle.State,
    crossinline action: () -> Unit
): LifecycleObserver = object : LifecycleEventObserver {
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (source.lifecycle.currentState !== state) return
        source.lifecycle.removeObserver(this)
        action()
    }
}.also(::addObserver)

/**
 * 将接下来的操作约束为启动协程，调用[Lifecycle.repeatOnLifecycle]
 *
 * 若`flow`的最后一个操作符是[Flow.flowWithLifecycle]，则可以调用该函数进行优化，
 * 避免[Flow.flowWithLifecycle]创建[callbackFlow]，占用不必要的内存资源，例如：
 * ```
 * flow
 *     .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
 *     .launchIn(lifecycle.coroutineScope)
 * ```
 * 替换为
 * ```
 * flow.
 *     .repeatOnLifecycle(lifecycle, Lifecycle.State.STARTED)
 *     .launchInLifecycleScope()
 * ```
 */
@CheckResult
fun <T> Flow<T>.repeatOnLifecycle(
    lifecycle: Lifecycle,
    state: Lifecycle.State = Lifecycle.State.STARTED
): RepeatOnLifecycleBuilder<T> {
    return RepeatOnLifecycleBuilder(this, lifecycle, state)
}

class RepeatOnLifecycleBuilder<T> internal constructor(
    private val flow: Flow<T>,
    private val lifecycle: Lifecycle,
    private val state: Lifecycle.State
) {
    fun launchInLifecycleScope(): Job {
        return launchIn(lifecycle.coroutineScope)
    }

    fun launchIn(scope: CoroutineScope): Job = scope.launch {
        lifecycle.repeatOnLifecycle(state) { flow.collect() }
    }
}