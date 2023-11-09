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

package com.xiaocydx.cxrv.recycle.scrap

import android.content.Context
import android.os.Looper
import android.view.LayoutInflater
import androidx.annotation.CheckResult
import androidx.recyclerview.widget.PrepareDeadline
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.Scrap
import androidx.recyclerview.widget.awaitDeadlineNs
import com.xiaocydx.cxrv.internal.ChoreographerContext
import com.xiaocydx.cxrv.internal.LooperContext
import com.xiaocydx.cxrv.internal.trace
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TRACE_DEADLINE_TAG = "PrepareScrap Deadline"
private const val TRACE_RECEIVE_TAG = "PrepareScrap Receive"

internal class PrepareScrapInfo<T : Any>(
    val viewType: Int,
    val count: Int,
    val scrapProvider: PrepareScrapProvider<T>
)

internal typealias PrepareScrapProvider<T> = (inflater: ScrapInflater, num: Int) -> Scrap<T>

class PrepareScrapFlow<T : Any> internal constructor(
    private val rv: RecyclerView,
    private val adapter: Adapter<*>,
    private val deadline: PrepareDeadline,
    private val dispatcher: CoroutineDispatcher,
    private val inflaterProvider: (Context) -> LayoutInflater,
    private val scrapInfoList: List<PrepareScrapInfo<T>> = emptyList()
) : Flow<Scrap<T>> {

    @CheckResult
    internal fun fusion(
        viewType: Int,
        count: Int,
        scrapProvider: PrepareScrapProvider<T>
    ) = PrepareScrapFlow(
        rv = rv,
        adapter = adapter,
        deadline = deadline,
        dispatcher = dispatcher,
        inflaterProvider = inflaterProvider,
        scrapInfoList = scrapInfoList + PrepareScrapInfo(viewType, count, scrapProvider)
    )

    override suspend fun collect(collector: FlowCollector<Scrap<T>>) = channelFlow {
        @Suppress("INVISIBLE_MEMBER")
        val channelCapacity: Int = scrapInfoList.sumOf { it.count }
            .coerceAtMost(Channel.CHANNEL_DEFAULT_CAPACITY)
        if (channelCapacity <= 0) return@channelFlow

        // 通过Handler.post()发送的消息可能被doFrame消息按时间顺序插队，
        // 这会导致处理完doFrame消息后，才往RecycledViewPool放入scrap，
        // 调整为在doFrame消息的Animation回调下往RecycledViewPool放入scrap。
        // MainHandlerContext()是一个可行的方案，但目前还不确定其产生的影响。
        withContext(ChoreographerContext()) {
            var isDeadline = false
            var prepareJob: Job? = null
            val deadlineJob = when (deadline) {
                PrepareDeadline.FOREVER_NS -> null
                PrepareDeadline.FRAME_NS -> launch(start = UNDISPATCHED) {
                    // 将视图树首帧Vsync时间或者更新时下一帧Vsync时间，作为预创建的截止时间
                    adapter.awaitDeadlineNs()
                    prepareJob?.cancelAndJoin()
                    trace(TRACE_DEADLINE_TAG) { isDeadline = true }
                }
            }

            // LooperContext对dispatcher调度的线程设置主线程Looper，
            // 构建View时能获取到主线程Looper，例如GestureDetector。
            val context = dispatcher + LooperContext(Looper.myLooper()!!)
            val channel = Channel<Scrap<T>>(channelCapacity)
            val pool = rv.recycledViewPool
            prepareJob = launch(context) {
                coroutineContext.job.invokeOnCompletion { channel.close() }
                scrapInfoList.forEach { info ->
                    var num = 0
                    var count = info.count
                    val inflater = ScrapInflater(rv, pool, inflaterProvider(rv.context), info.viewType)
                    while (count > 0) {
                        ensureActive()
                        num++
                        count--
                        channel.send(info.scrapProvider(inflater, num))
                    }
                }
            }

            for (scrap in channel) {
                // isDeadline = true，丢弃channel的scrap
                if (isDeadline) break
                scrap.putToPoolIfNecessary()
                trace(TRACE_RECEIVE_TAG) { send(scrap) }
            }
            deadlineJob?.cancelAndJoin()
        }
    }.buffer(RENDEZVOUS).flowOn(Dispatchers.Main.immediate).collect(collector)
}