/*
 * Copyright 2022 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaocydx.cxrv.paging

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