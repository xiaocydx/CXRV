package com.xiaocydx.recycler.extension

import android.os.Looper
import android.view.View
import androidx.core.os.HandlerCompat
import androidx.core.view.doOnPreDraw
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 主线程异步消息Handler
 */
private val asyncHandler = HandlerCompat.createAsync(Looper.getMainLooper())

/**
 * 当前是否为主线程
 */
internal val isMainThread: Boolean
    get() = Looper.getMainLooper().thread === Thread.currentThread()

/**
 * 若当前不为主线程，则将[action]post到主线程中执行
 */
internal inline fun runOnMainThread(crossinline action: () -> Unit) {
    if (!isMainThread) {
        asyncHandler.post { action() }
    } else {
        action()
    }
}

/**
 * 断言当前为主线程
 */
internal fun assertMainThread() {
    assert(isMainThread) { "只能在主线程中调用当前函数。" }
}

/**
 * 即将绘制视图树时，结束挂起
 */
internal suspend fun View.awaitPreDraw() {
    suspendCancellableCoroutine<Unit> { cont ->
        val listener = doOnPreDraw { cont.resume(Unit) }
        cont.invokeOnCancellation { listener.removeListener() }
    }
}

/**
 * 将Flow的执行上下文的调度器更改为主线程调度器
 */
internal fun <T> Flow<T>.flowOnMain(): Flow<T> = flowOn(Dispatchers.Main.immediate)

/**
 * 不检测执行上下文、异常透明性的Flow
 */
internal inline fun <T> unsafeFlow(
    crossinline block: suspend FlowCollector<T>.() -> Unit
): Flow<T> = object : Flow<T> {
    override suspend fun collect(collector: FlowCollector<T>) {
        collector.block()
    }
}