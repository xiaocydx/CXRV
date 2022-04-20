package com.xiaocydx.recycler.extension

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import android.view.View
import androidx.core.os.HandlerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
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
 * 在下一帧绘制完成后执行[action]
 */
internal fun View.doOnFrameComplete(
    action: () -> Unit
): Disposable = when {
    !isAttachedToWindow -> emptyDisposable()
    isLayoutRequested -> {
        // 已经申请重绘，发送同步消息，等待下一帧同步屏障解除后被执行
        FrameCompleteObserver(postFrame = false, handler, action)
    }
    else -> {
        // 下一帧doFrame()的执行过程可能会申请重绘，添加同步屏障，
        // 因此发送异步消息，避免被同步屏障影响，无法在当前帧被执行。
        FrameCompleteObserver(postFrame = true, asyncHandler, action)
    }
}

/**
 * 等待下一帧绘制完成
 */
internal suspend fun View.awaitFrameComplete() {
    suspendCancellableCoroutine<Unit> { continuation ->
        val disposable = doOnFrameComplete {
            continuation.resume(Unit)
        }
        continuation.invokeOnCancellation {
            disposable.dispose()
        }
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

private class FrameCompleteObserver(
    postFrame: Boolean,
    handler: Handler,
    action: () -> Unit
) : Disposable, Runnable {
    private var handler: Handler? = handler
    private var action: (() -> Unit)? = action
    override val isDisposed: Boolean
        get() = handler == null && action == null

    init {
        if (postFrame) {
            Choreographer.getInstance().postFrameCallback {
                handler.post(this)
            }
        } else {
            handler.post(this)
        }
    }

    override fun run() {
        action?.invoke()
        dispose()
    }

    override fun dispose() = runOnMainThread {
        handler?.removeCallbacks(this)
        handler = null
        action = null
    }
}