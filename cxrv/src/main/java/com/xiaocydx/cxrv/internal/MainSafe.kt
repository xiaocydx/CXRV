package com.xiaocydx.cxrv.internal

import android.os.Looper
import androidx.core.os.HandlerCompat

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