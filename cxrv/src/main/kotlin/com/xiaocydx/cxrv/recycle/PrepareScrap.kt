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
@file:Suppress("PackageDirectoryMismatch", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package androidx.recyclerview.widget

import android.os.Looper
import android.view.Choreographer
import androidx.annotation.IntRange
import androidx.annotation.MainThread
import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.cxrv.internal.*
import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CHANNEL_DEFAULT_CAPACITY
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.*
import kotlin.coroutines.resume

/**
 * 按[PrepareScope.add]的添加顺序，预创建ViewHolder并放入[RecycledViewPool]
 *
 * **注意**：该函数需要在视图初始化阶段调用，如果有自定义[RecycledViewPool]，
 * 那么在调用该函数之前，先对RecyclerView设置自定义[RecycledViewPool]，例如：
 * ```
 * lifecycleScope.launch {
 *     recyclerView.setRecycledViewPool(CustomRecycledViewPool())
 *     val result = recyclerView.prepareScrap(
 *         prepareAdapter = adapter,
 *         prepareDeadline = PrepareDeadline.FOREVER_NS,
 *         prepareDispatcher = Dispatchers.IO
 *     ) {
 *         add(viewType1, 20)
 *         add(viewType2, 10)
 *     }
 *     result.getRecycledScrapCount()
 *     result.getPreparedScrapCount()
 * }
 * ```
 *
 * @param prepareAdapter 对RecyclerView设置的Adapter可能是[ConcatAdapter]，
 * 因此需要主动传入跟列表数据关联的Adapter，用于观察数据变更和创建ViewHolder，
 * 若调用[Adapter.createViewHolder]抛出异常，则结束预创建流程，并传递异常。
 *
 * @param prepareDeadline 预创建的截止时间，默认为[PrepareDeadline.FOREVER_NS]，
 * 预创建流程会按[prepareDeadline]进行停止，尽可能避免创建多余的ViewHolder。
 *
 * @param prepareDispatcher 用于预创建的协程调度器，默认为[Dispatchers.IO]，
 * 可以通过`Dispatchers.IO.limitedParallelism()`创建一个并行度受限的调度器，
 * 供多处调用该函数的地方使用。
 *
 * @param block 调用[PrepareScope.add]，添加预创建的键值对，添加顺序决定创建顺序，
 * 如果对RecyclerView设置的[RecycledViewPool]，已经存在ViewHolder（共享池场景）
 * 那么可以先减去[RecycledViewPool.getRecycledViewCount]，再添加预创建的键值对。
 *
 * @return [PrepareResult]提供数量查询函数，可用于数据统计或者单元测试。
 */
@MainThread
suspend fun RecyclerView.prepareScrap(
    prepareAdapter: Adapter<*>,
    prepareDeadline: PrepareDeadline = PrepareDeadline.FOREVER_NS,
    prepareDispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: PrepareScope.() -> Unit
): PrepareResult {
    assertMainThread()
    val rv = this
    val pairs = PrepareScope().apply(block).getPairs()
    val initialCapacity = pairs.sumOf { it.count }
    val result = PrepareResult(initialCapacity, recycledViewPool)
    if (initialCapacity <= 0) return result

    // 通过Handler.post()发送的消息可能被doFrame消息按时间顺序插队，
    // 这会导致处理完doFrame消息后，才往RecycledViewPool放入scrap，
    // 调整为在doFrame消息的Animation回调下往RecycledViewPool放入scrap。
    // MainHandlerContext()是一个可行的方案，但目前还不确定其产生的影响。
    withContext(ChoreographerContext()) {
        var prepareJob: Job? = null
        var deadline = false
        val deadlineJob = when (prepareDeadline) {
            PrepareDeadline.FOREVER_NS -> null
            PrepareDeadline.FRAME_NS -> launch(start = UNDISPATCHED) {
                // 将视图树首帧Vsync时间或者更新时下一帧Vsync时间，作为预创建的截止时间
                prepareAdapter.awaitDeadlineNs()
                prepareJob?.cancelAndJoin()
                trace(TRACE_DEADLINE_TAG) { deadline = true }
            }
        }

        // LooperContext对prepareDispatcher调度的线程设置主线程Looper，
        // 确保View的构造过程能获取到主线程Looper，例如GestureDetector。
        val prepareContext = prepareDispatcher + LooperContext(Looper.myLooper()!!)
        val channel = Channel<ViewHolder>(result.channelCapacity)
        prepareJob = launch(prepareContext) {
            coroutineContext.job.invokeOnCompletion { channel.close() }
            pairs.forEach { (viewType, count) ->
                var remainingCount = count
                while (remainingCount > 0) {
                    ensureActive()
                    remainingCount--
                    channel.send(prepareAdapter.createViewHolder(rv, viewType))
                }
            }
        }

        for (scrap in channel) {
            if (deadline) break
            trace(TRACE_PUT_SCRAP_TAG) { result.putScrapToRecycledViewPool(scrap) }
        }
        deadlineJob?.cancelAndJoin()
    }
    return result
}

/**
 * 预创建的截止时间
 */
enum class PrepareDeadline {
    /**
     * 没有截止时间
     */
    FOREVER_NS,

    /**
     * 将视图树首帧Vsync时间或者更新时下一帧Vsync时间，作为预创建的截止时间
     */
    FRAME_NS
}

/**
 * 调用[PrepareScope.add]，添加预创建的键值对
 */
class PrepareScope private constructor() {
    private val pairs = mutableListOf<Pair>()

    fun add(viewType: Int, @IntRange(from = 1) count: Int) {
        if (count < 1) return
        pairs.add(Pair(viewType, count))
    }

    internal fun getPairs(): List<Pair> = ArrayList(pairs)

    /**
     * 不使用[kotlin.Pair]，有装箱拆箱的开销
     */
    internal data class Pair(val viewType: Int, val count: Int)
}

/**
 * [RecyclerView.prepareScrap]的结果，提供数量查询函数
 */
@MainThread
class PrepareResult private constructor(
    initialCapacity: Int,
    private val recycledViewPool: RecycledViewPool
) {
    private val preparedScrapList: List<ViewHolder>
    internal val channelCapacity: Int

    init {
        var capacity = CHANNEL_DEFAULT_CAPACITY
        if (initialCapacity > capacity) capacity = UNLIMITED
        channelCapacity = capacity
        preparedScrapList = if (initialCapacity > 0) ArrayList(initialCapacity) else emptyList()
    }

    /**
     * 获取[viewType]在[RecycledViewPool]的回收数量
     *
     * 回收数量不一定等于[getPreparedScrapCount]，可能布局流程已取走一部分进行填充，
     * 又或者在执行预创建流程之前，[RecyclerView]已存在ViewHolder，例如共享池场景。
     */
    fun getRecycledScrapCount(viewType: Int): Int {
        assertMainThread()
        return recycledViewPool.getRecycledViewCount(viewType)
    }

    /**
     * 获取[viewType]在[RecycledViewPool]的预创建数量
     */
    fun getPreparedScrapCount(viewType: Int): Int {
        assertMainThread()
        return preparedScrapList.fold(0) { acc, scrap ->
            if (scrap.itemViewType == viewType) acc + 1 else acc
        }
    }

    internal fun putScrapToRecycledViewPool(scrap: ViewHolder) {
        assertMainThread()
        recycledViewPool.putScrap(scrap)
        preparedScrapList.let { it as? MutableList<ViewHolder> }?.add(scrap)
    }

    private fun RecycledViewPool.putScrap(scrap: ViewHolder) {
        val viewType = scrap.itemViewType
        val scrapData = mScrap[viewType] ?: kotlin.run {
            // 触发内部逻辑创建scrapData
            getRecycledViewCount(viewType)
            mScrap.get(viewType)!!
        }
        scrapData.mScrapHeap.add(scrap)
    }
}

private const val TRACE_DEADLINE_TAG = "PrepareScrap Deadline"
private const val TRACE_PUT_SCRAP_TAG = "PrepareScrap PutScrap"

@MainThread
private suspend fun Adapter<*>.awaitDeadlineNs(): Long {
    assertMainThread()
    return suspendCancellableCoroutine { cont ->
        DeadlineNsObserver(this, cont).attach()
    }
}

@MainThread
private class DeadlineNsObserver(
    private val adapter: Adapter<*>,
    private val cont: CancellableContinuation<Long>
) : AdapterDataObserver() {
    private var isRegister = false
    private var isResume = false

    fun attach() {
        if (adapter.itemCount > 0) return resume()
        registerAdapterDataObserver()
        cont.invokeOnCancellation { runOnMainThread(::unregisterAdapterDataObserver) }
    }

    override fun onChanged() = resume()

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = resume()

    private fun registerAdapterDataObserver() {
        isRegister = true
        adapter.registerAdapterDataObserver(this)
    }

    private fun unregisterAdapterDataObserver() {
        if (!isRegister) return
        isRegister = false
        adapter.unregisterAdapterDataObserver(this)
    }

    private fun resume() {
        if (isResume) return
        isResume = true
        Choreographer.getInstance().postFrameCallbackDelayed({ frameTimeNs ->
            unregisterAdapterDataObserver()
            cont.resume(frameTimeNs)
        }, Long.MIN_VALUE)
    }
}