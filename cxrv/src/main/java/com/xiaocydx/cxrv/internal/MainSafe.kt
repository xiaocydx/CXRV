package com.xiaocydx.cxrv.internal

import android.os.Looper
import android.view.Choreographer
import android.view.Choreographer.FrameCallback
import androidx.core.os.HandlerCompat
import kotlinx.coroutines.android.awaitFrame
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
    if (isMainThread) action() else asyncHandler.post { action() }
}

/**
 * 断言当前为主线程
 */
internal fun assertMainThread() {
    assert(isMainThread) { "只能在主线程中调用当前函数" }
}

/**
 * 没有选择使用[awaitFrame]的原因是要增加[delayMillis]参数
 */
internal suspend fun Choreographer.awaitFrame(
    delayMillis: Long = 0L
): Long = suspendCancellableCoroutine { cont ->
    val callback = FrameCallback { cont.resume(it) }
    postFrameCallbackDelayed(callback, delayMillis)
    cont.invokeOnCancellation { removeFrameCallback(callback) }
}