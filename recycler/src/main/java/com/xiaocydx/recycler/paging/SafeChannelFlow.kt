package com.xiaocydx.recycler.paging

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

/**
 * 安全的[channelFlow]
 *
 * 对[SendChannel.send]捕获[ClosedSendChannelException]异常，
 * 包装为[CancellationException]重新抛出，目的是只取消当前所处协程。
 */
internal inline fun <E> safeChannelFlow(
    crossinline block: suspend CoroutineScope.(SendChannel<E>) -> Unit
): Flow<E> = channelFlow {
    block(SafeSendChannel(this))
}

internal class SafeSendChannel<E>(
    private val channel: SendChannel<E>
) : SendChannel<E> by channel {

    override suspend fun send(element: E) {
        try {
            channel.send(element)
        } catch (e: ClosedSendChannelException) {
            throw CancellationException(e.message, e)
        }
    }
}