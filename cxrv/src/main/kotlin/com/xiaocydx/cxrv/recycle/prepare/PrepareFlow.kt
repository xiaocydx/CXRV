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
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.LazyThreadSafetyMode.NONE

/**
 * 当[PrepareFlow]被收集时，才开始预创建工作，直到取消收集或者[deadline]到达
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
    private val scrapInfoList: List<ScrapInfo<T>> = emptyList()
) : PrepareFusible<T>(), Flow<Scrap<T>> {

    internal fun recreate(
        rv: RecyclerView = this.rv,
        deadline: PrepareDeadline? = this.deadline,
        inflaterProvider: (Context) -> LayoutInflater = this.inflaterProvider,
        prepareDispatcher: CoroutineDispatcher = this.prepareDispatcher,
        mainDispatcher: MainCoroutineDispatcher = this.mainDispatcher,
        scrapInfoList: List<ScrapInfo<T>> = this.scrapInfoList
    ) = PrepareFlow(
        rv, deadline, inflaterProvider,
        prepareDispatcher, mainDispatcher, scrapInfoList
    )

    override fun fusion(scrapInfo: ScrapInfo<T>): PrepareFlow<T> {
        return recreate(scrapInfoList = scrapInfoList + scrapInfo)
    }

    /**
     * [channelFlow]为桥接作用，实现`Context preservation`约束，
     * 调用[collector]开始收集，协程上下文的调度器元素变化过程为：
     * `mainDispatcher -> ChoreographerContext -> prepareDispatcher`，
     * [prepareDispatcher]回到[ChoreographerContext]，通过Choreographer调度一次，
     * [ChoreographerContext]回到[mainDispatcher]，都是主线程Looper，不产生调度。
     */
    @Suppress("INVISIBLE_MEMBER")
    override suspend fun collect(collector: FlowCollector<Scrap<T>>) = channelFlow {
        val scrapInfoList = scrapInfoList.filter { it.count > 0 }
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
            prepareJob = launch(context = prepareDispatcher + LooperContext(Looper.myLooper()!!)) {
                val scrapContext = ScrapContext(rv.context)
                val scrapParent = lazy(NONE) { ScrapParent(rv, scrapContext) }
                try {
                    val inflater = inflaterProvider(scrapContext)
                    scrapContext.setInflater(inflater)
                    scrapInfoList.forEach { (viewType, count, scrapProvider) ->
                        var num = 0
                        val scrapInflater = ScrapInflater(rv, inflater, scrapContext, scrapParent, viewType)
                        while (num < count) {
                            // 在scrapProvider()之前检查状态
                            ensureActive()
                            num++
                            val scrap = scrapProvider(num, scrapInflater, pool)
                            trace(TRACE_PREPARE_TAG) { channel.send(scrap) }
                        }
                    }
                } finally {
                    scrapContext.clearInflater()
                    channel.close()
                }
            }

            for (scrap in channel) {
                // isDeadline = true，丢弃channel的scrap
                if (isDeadline) break
                trace(TRACE_FORWARD_TAG) { send(scrap) }
            }
            deadlineJob?.cancelAndJoin()
        }
    }.buffer(RENDEZVOUS).flowOn(mainDispatcher).collect(collector)

    internal data class ScrapInfo<T : Any>(
        val viewType: Int,
        val count: Int,
        val scrapProvider: (Int, ScrapInflater, RecycledViewPool) -> Scrap<T>
    )

    private companion object {
        const val TRACE_DEADLINE_TAG = "PrepareFlow Deadline"
        const val TRACE_PREPARE_TAG = "PrepareFlow Prepare"
        const val TRACE_FORWARD_TAG = "PrepareFlow Forward"
    }
}