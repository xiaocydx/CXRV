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

@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.os.Looper
import android.view.Choreographer
import androidx.annotation.IntRange
import androidx.annotation.MainThread
import androidx.core.os.HandlerCompat
import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.sample.extension.LooperElement
import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume

/**
 * 按[PrepareScrapPairs.add]的添加顺序，预创建ViewHolder并放入[RecycledViewPool]
 *
 * **注意**：此函数应当在初始化视图时调用，并确保RecyclerView有父级、已设置Adapter，
 * 预创建流程会按[prepareDeadline]进行停止，避免产生不必要的创建开销。
 *
 * ```
 * recyclerView.prepareScrap(
 *     prepareAdapter = adapter,
 *     prepareDeadline = PrepareDeadline.FOREVER_NS,
 *     prepareDispatcher = DefaultIoDispatcher
 * ) {
 *     add(viewType = viewType1, count = 10)
 *     add(viewType = viewType2, count = 10)
 * }.launchIn(coroutineScope)
 * ```
 *
 * @param prepareAdapter    调用[Adapter.createViewHolder]创建ViewHolder
 * @param prepareDeadline   预创建的截止时间，默认为[PrepareDeadline.FOREVER_NS]
 * @param prepareDispatcher 用于预创建的协程调度器，默认为[DefaultIoDispatcher]
 * @param block             调用[PrepareScrapPairs.add]，添加预创建的键值对
 *
 * @return 收集返回的`Flow<ViewHolder>`，才开始执行预创建流程，
 * 预创建执行过程发射创建成功的ViewHolder，供收集处统计或测试。
 */
@MainThread
fun RecyclerView.prepareScrap(
    prepareAdapter: Adapter<*>,
    prepareDeadline: PrepareDeadline = PrepareDeadline.FOREVER_NS,
    prepareDispatcher: CoroutineDispatcher = DefaultIoDispatcher,
    block: PrepareScrapPairs.() -> Unit
): Flow<ViewHolder> {
    require(adapter != null) { "请先对RecyclerView设置Adapter" }
    require(parent != null) { "请先将RecyclerView添加到父级中" }
    val recyclerView = this
    val preparePairs = PrepareScrapPairs().apply(block).complete()
    return unsafeFlow<ViewHolder> {
        coroutineScope {
            val deadlineNs = AtomicLong(Long.MAX_VALUE)
            val deadlineJob = when (prepareDeadline) {
                PrepareDeadline.FOREVER_NS -> null
                PrepareDeadline.FRAME_NS -> launch(start = UNDISPATCHED) {
                    // 将视图树首帧Vsync时间或者更新时下一帧Vsync时间，作为预创建的截止时间
                    deadlineNs.set(prepareAdapter.awaitDeadlineNs())
                }
            }

            val choreographer = Choreographer.getInstance()
            val prepareContext = LooperElement(Looper.myLooper()!!) + prepareDispatcher
            val prepareFlow = unsafeFlow<ViewHolder> {
                preparePairs.forEach { prepareViewType, prepareCount ->
                    var count = prepareCount
                    while (count > 0 && System.nanoTime() < deadlineNs.get()) {
                        // 由于使用unsafeFlow不检测执行上下文、异常透明性（提高发射性能），
                        // 因此emit()没有检查Job已取消的处理，需要补充判断以响应Job取消。
                        ensureActive()
                        count--
                        val scrap = runCatching {
                            prepareAdapter.createViewHolder(recyclerView, prepareViewType)
                        }.getOrNull() ?: continue
                        // 通过Handler发送的消息可能会被首帧的doFrame消息按时间顺序插队，
                        // 也就导致处理完首帧doFrame消息后，才往RecycledViewPool放入scrap，
                        // 因此调整为在doFrame消息的Animation回调下往RecycledViewPool放入scrap。
                        choreographer.postFrameCallback { recyclerView.putPrepareScrap(scrap) }
                        emit(scrap)
                    }
                }
            }.flowOn(prepareContext)

            emitAll(prepareFlow)
            deadlineJob?.cancel()
        }
    }.flowOn(Dispatchers.Main.immediate)
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
 * 调用[PrepareScrapPairs.add]，添加预创建的键值对
 */
class PrepareScrapPairs @PublishedApi internal constructor() {
    @PublishedApi
    internal val pairs = mutableListOf<Pair>()
    private var isComplete = false

    fun add(viewType: Int, @IntRange(from = 1) count: Int) {
        check(!isComplete) { "已完成添加" }
        pairs.add(Pair(viewType, count.coerceAtLeast(0)))
    }

    internal inline fun forEach(action: (viewType: Int, count: Int) -> Unit) {
        pairs.forEach { action(it.viewType, it.count) }
    }

    @PublishedApi
    internal fun complete() = apply { isComplete = true }

    /**
     * 不使用[kotlin.Pair]，有装箱拆箱的开销
     */
    @PublishedApi
    internal class Pair(val viewType: Int, val count: Int)
}

/**
 * 默认串行创建ViewHolder
 */
@OptIn(ExperimentalCoroutinesApi::class)
private val DefaultIoDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)

@MainThread
private fun RecyclerView.putPrepareScrap(scrap: ViewHolder) {
    val pool = recycledViewPool
    val mScrap = pool.mScrap
    val viewType = scrap.itemViewType
    val scrapData = mScrap[viewType] ?: kotlin.run {
        // 触发内部逻辑创建scrapData
        pool.getRecycledViewCount(viewType)
        mScrap.get(viewType)!!
    }
    scrapData.mScrapHeap.add(scrap)
}

@MainThread
private suspend fun Adapter<*>.awaitDeadlineNs(): Long {
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
        Choreographer.getInstance().postFrameCallback { frameTimeNs ->
            unregisterAdapterDataObserver()
            cont.resume(frameTimeNs)
        }
    }

    private inline fun runOnMainThread(crossinline action: () -> Unit) {
        if (Thread.currentThread() === Looper.getMainLooper().thread) {
            action()
        } else {
            HandlerCompat.createAsync(Looper.getMainLooper()).post { action() }
        }
    }
}

/**
 * 不检测执行上下文、异常透明性的Flow
 */
private inline fun <T> unsafeFlow(
    crossinline block: suspend FlowCollector<T>.() -> Unit
): Flow<T> = object : Flow<T> {
    override suspend fun collect(collector: FlowCollector<T>) {
        collector.block()
    }
}