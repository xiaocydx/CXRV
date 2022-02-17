package com.xiaocydx.recycler.extension

import android.os.Looper
import android.view.View
import androidx.core.os.HandlerCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.lang.Runnable
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 主线程异步消息Handler
 */
private val mainHandler = HandlerCompat.createAsync(Looper.getMainLooper())

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
        mainHandler.post { action() }
    } else {
        action()
    }
}

/**
 * 断言当前为主线程
 */
internal fun assertMainThread() {
    check(isMainThread) { "只能在主线程中调用当前函数" }
}

/**
 * 发送同步消息，并等待被执行
 */
internal suspend fun View.postAwait() {
    suspendCancellableCoroutine<Unit> suspend@{
        val action = Runnable { it.resume(Unit) }
        if (!post(action)) {
            it.resumeWithException(CancellationException("发送消息失败。"))
            return@suspend
        }
        it.invokeOnCancellation { removeCallbacks(action) }
    }
}

internal fun <T> Flow<T>.flowOnMain(
    mainDispatcher: MainCoroutineDispatcher = Dispatchers.Main.immediate
): Flow<T> = flow {
    val flow = when (currentCoroutineContext()[ContinuationInterceptor]) {
        is MainCoroutineDispatcher -> this@flowOnMain
        else -> this@flowOnMain.flowOn(mainDispatcher)
    }
    flow.collect { emit(it) }
}