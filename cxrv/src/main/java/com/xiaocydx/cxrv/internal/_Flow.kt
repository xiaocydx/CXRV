package com.xiaocydx.cxrv.internal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flowOn

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