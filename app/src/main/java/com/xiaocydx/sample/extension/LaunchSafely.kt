@file:Suppress("PackageDirectoryMismatch")

package com.xiaocydx.sample

import android.util.Log
import androidx.annotation.CheckResult
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 协程启动方式
 *
 * **注意**：
 * 1. 项目早期引入协程框架层时，由于不是所有同事都清楚协程异常机制，
 * 因此添加该扩展函数，提供一种保护措施，尽可能避免早期的协程代码导致线上崩溃，
 * 如果已经清楚了协程异常机制，做好了异常捕获，则无需关注该扩展函数。
 *
 * 2. [launchSafely]的意图是提供整体的异常处理方式，因此应当在最开始处调用[launchSafely]作为父级协程，
 * 不要在官方的协程构建器，例如[launch]的作用域下调用[launchSafely]启动协程，这可能会因为异常传递机制，
 * 导致[launchSafely]的[DefaultCoroutineExceptionHandler]得不到处理异常的机会，进而交由当前线程处理异常。
 *
 * 3. 可以通过[invokeOnException]添加异常处理逻辑：
 * ```
 *  scope.launchSafely {
 *      ...
 *  }.invokeOnException {
 *      // 异常处理逻辑
 *  }
 * ```
 */
fun CoroutineScope.launchSafely(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job = launch(combineCoroutineContext(context), start, block)

/**
 * 出现异常时，调用[handler]
 */
inline fun Job.invokeOnException(
    crossinline handler: (exception: Throwable) -> Unit
): DisposableHandle = invokeOnCompletion { exception ->
    exception?.takeIf { it !is CancellationException }?.let(handler)
}

/**
 * 合并上下文，添加[DefaultCoroutineExceptionHandler]，
 * [context]中[CoroutineExceptionHandler]的优先级最高。
 */
@CheckResult
private fun CoroutineScope.combineCoroutineContext(context: CoroutineContext): CoroutineContext {
    return DefaultCoroutineExceptionHandler + coroutineContext + context
}

private object DefaultCoroutineExceptionHandler : CoroutineExceptionHandler {
    override val key: CoroutineContext.Key<*> = CoroutineExceptionHandler
    private val TAG = javaClass.simpleName

    /**
     * 可以添加默认处理，例如异常的堆栈信息打印
     */
    override fun handleException(context: CoroutineContext, exception: Throwable) {
        Log.e(TAG, Log.getStackTraceString(exception))
    }
}