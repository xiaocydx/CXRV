package com.xiaocydx.cxrv.paging

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 合并事件，后发送的事件会覆盖前面的事件
 *
 * @author xcc
 * @date 2021/9/15
 */
internal class ConflatedEvent<T> {
    private val sharedFlow: MutableSharedFlow<T> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * 该属性仅对内部可见，因此不调用[asSharedFlow]转换为只可读
     */
    val flow: Flow<T> = sharedFlow

    fun send(event: T) {
        sharedFlow.tryEmit(event)
    }
}