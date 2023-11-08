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

@file:JvmName("PrepareScrapInternalKt")
@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.os.Looper
import androidx.annotation.MainThread
import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.cxrv.internal.*
import com.xiaocydx.cxrv.recycle.scrap.awaitDeadlineNs
import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

/**
 * 按[PrepareScope.scrap]的调用顺序，预创建ViewHolder并放入[RecycledViewPool]
 *
 * **注意**：该函数需要在视图初始化阶段调用，如果有自定义[RecycledViewPool]，
 * 那么在调用该函数之前，先对RecyclerView设置自定义[RecycledViewPool]，例如：
 * ```
 * lifecycleScope.launch {
 *     recyclerView.setRecycledViewPool(CustomRecycledViewPool())
 *     val result = recyclerView.prepareScrap(adapter) {
 *         deadline(PrepareDeadline.FOREVER_NS) // 默认为PrepareDeadline.FOREVER_NS
 *         dispatcher(Dispatchers.IO) // 默认为Dispatchers.IO
 *         scrap(viewType1, 20, provider)
 *         scrap(viewType2, 10, provider)
 *     }
 *     val recycledCount = result.getRecycledScrapCount(viewType1)
 *     val preparedCount = result.getPreparedScrapCount(viewType1)
 * }
 * ```
 *
 * @param adapter 对RecyclerView设置的Adapter可能是[ConcatAdapter]，
 * 因此需要主动传入跟列表数据关联的Adapter，用于观察数据变更和创建ViewHolder。
 * @param block   初始化预创建的属性，详细描述可以看[PrepareScope]的注释。
 * @return [PrepareResult]提供数量查询函数，可用于数据统计或者单元测试。
 */
@MainThread
suspend fun RecyclerView.prepareScrap(
    adapter: Adapter<*>,
    block: PrepareScope.() -> Unit
): PrepareResult {
    assertMainThread()
    val rv = this
    val (deadline, dispatcher, infoList, inflaterProvider) =
            PrepareScope(rv, adapter).apply(block).createConfig()
    val initialCapacity = infoList.sumOf { it.count }
    val result = PrepareResult(initialCapacity, recycledViewPool)
    if (initialCapacity <= 0) return result

    // 通过Handler.post()发送的消息可能被doFrame消息按时间顺序插队，
    // 这会导致处理完doFrame消息后，才往RecycledViewPool放入scrap，
    // 调整为在doFrame消息的Animation回调下往RecycledViewPool放入scrap。
    // MainHandlerContext()是一个可行的方案，但目前还不确定其产生的影响。
    withContext(ChoreographerContext()) {
        var prepareJob: Job? = null
        var isDeadline = false
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
        val channel = Channel<ViewHolder>(result.channelCapacity)
        val inflater = Inflater(rv, inflaterProvider(rv.context))
        prepareJob = launch(context) {
            coroutineContext.job.invokeOnCompletion { channel.close() }
            infoList.forEach { (viewType, count, provider) ->
                var remainingCount = count
                while (remainingCount > 0) {
                    ensureActive()
                    remainingCount--
                    channel.send(provider.createViewHolder(inflater, viewType))
                }
            }
        }

        for (scrap in channel) {
            if (isDeadline) break
            trace(TRACE_PUT_SCRAP_TAG) { result.putScrapToRecycledViewPool(scrap) }
        }
        deadlineJob?.cancelAndJoin()
    }
    return result
}

/**
 * 按[PrepareScope.add]的添加顺序，预创建ViewHolder并放入[RecycledViewPool]
 */
@MainThread
@Deprecated("部分函数参数已聚集到PrepareScope")
suspend fun RecyclerView.prepareScrap(
    prepareAdapter: Adapter<*>,
    prepareDeadline: PrepareDeadline = PrepareDeadline.FOREVER_NS,
    prepareDispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: PrepareScope.() -> Unit
): PrepareResult {
    return prepareScrap(prepareAdapter) {
        deadline(prepareDeadline)
        dispatcher(prepareDispatcher)
        block()
    }
}

private const val TRACE_DEADLINE_TAG = "PrepareScrap Deadline"
private const val TRACE_PUT_SCRAP_TAG = "PrepareScrap PutScrap"