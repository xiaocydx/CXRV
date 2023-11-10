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

package com.xiaocydx.cxrv.recycle.prepare

import android.content.Context
import android.os.Looper
import android.view.LayoutInflater
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.Scrap
import com.xiaocydx.cxrv.internal.ChoreographerContext
import com.xiaocydx.cxrv.internal.LooperContext
import com.xiaocydx.cxrv.internal.trace
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainCoroutineDispatcher
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

/**
 * 当[PrepareFlow]被收集时，才开始预创建工作，直到取消收集或者[deadline]到达。
 *
 * @author xcc
 * @date 2023/11/10
 */
class PrepareFlow<T : Any> internal constructor(
    private val rv: RecyclerView,
    private val deadline: PrepareDeadline?,
    private val inflaterProvider: (Context) -> LayoutInflater,
    private val prepareDispatcher: CoroutineDispatcher,
    private val mainDispatcher: MainCoroutineDispatcher = Dispatchers.Main.immediate,
    private val scrapInfoList: List<ScrapInfo<T>> = emptyList(),
    private val putScrapToRecycledViewPool: Boolean = false
) : PrepareFusible<T>(), Flow<Scrap<T>> {

    internal fun recreate(
        rv: RecyclerView = this.rv,
        deadline: PrepareDeadline? = this.deadline,
        inflaterProvider: (Context) -> LayoutInflater = this.inflaterProvider,
        prepareDispatcher: CoroutineDispatcher = this.prepareDispatcher,
        mainDispatcher: MainCoroutineDispatcher = this.mainDispatcher,
        scrapInfoList: List<ScrapInfo<T>> = this.scrapInfoList,
        putScrapToRecycledViewPool: Boolean = this.putScrapToRecycledViewPool
    ) = PrepareFlow(
        rv, deadline, inflaterProvider,
        prepareDispatcher, mainDispatcher,
        scrapInfoList, putScrapToRecycledViewPool
    )

    override fun fusion(scrapInfo: ScrapInfo<T>): PrepareFlow<T> {
        return recreate(scrapInfoList = scrapInfoList + scrapInfo)
    }

    override suspend fun collect(collector: FlowCollector<Scrap<T>>) = channelFlow {
        @Suppress("INVISIBLE_MEMBER")
        val channelCapacity: Int = scrapInfoList.sumOf {
            it.count.coerceAtLeast(0)
        }.coerceAtMost(Channel.CHANNEL_DEFAULT_CAPACITY)
        if (channelCapacity <= 0) return@channelFlow

        // 通过Handler.post()发送的消息可能被doFrame消息按时间顺序插队，
        // 这会导致处理完doFrame消息后，才往RecycledViewPool放入scrap，
        // 调整为在doFrame消息的Animation回调下往RecycledViewPool放入scrap。
        // MainHandlerContext()是一个可行的方案，但目前还不确定其产生的影响。
        withContext(ChoreographerContext()) {
            var isDeadline = false
            var prepareJob: Job? = null
            val deadlineJob = deadline?.let {
                launch(start = UNDISPATCHED) {
                    deadline.awaitDeadlineNs()
                    prepareJob?.cancelAndJoin()
                    trace(TRACE_DEADLINE_TAG) { isDeadline = true }
                }
            }

            // LooperContext对dispatcher调度的线程设置主线程Looper，
            // 构建View时能获取到主线程Looper，例如GestureDetector。
            val pool = rv.recycledViewPool
            val channel = Channel<Scrap<T>>(channelCapacity)
            val realInflater = inflaterProvider(rv.context)
            prepareJob = launch(context = prepareDispatcher + LooperContext(Looper.myLooper()!!)) {
                coroutineContext.job.invokeOnCompletion { channel.close() }
                scrapInfoList.forEach { info ->
                    var num = 0
                    var count = info.count
                    val inflater = ScrapInflater(rv, realInflater, info.viewType)
                    while (count > 0) {
                        // 在调用info.provider()之前检查状态
                        ensureActive()
                        num++
                        count--
                        val scrap = info.provider(inflater, num)
                        channel.send(scrap)
                    }
                }
            }

            for (scrap in channel) {
                // isDeadline = true，丢弃channel的scrap
                if (isDeadline) break
                scrap.takeIf { putScrapToRecycledViewPool }?.tryPutToRecycledViewPool(pool)
                trace(TRACE_RECEIVE_TAG) { send(scrap) }
            }
            deadlineJob?.cancelAndJoin()
        }
    }.buffer(RENDEZVOUS).flowOn(mainDispatcher).collect(collector)

    internal class ScrapInfo<T : Any>(
        val viewType: Int,
        val count: Int,
        val provider: (inflater: ScrapInflater, num: Int) -> Scrap<T>
    )

    private companion object {
        const val TRACE_DEADLINE_TAG = "PrepareFlow Deadline"
        const val TRACE_RECEIVE_TAG = "PrepareFlow Receive"
    }
}