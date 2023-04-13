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
import android.view.Choreographer
import androidx.annotation.IntRange
import androidx.annotation.MainThread
import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.cxrv.internal.*
import com.xiaocydx.cxrv.recycle.ChoreographerDispatcher
import com.xiaocydx.cxrv.recycle.LooperElement
import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.*
import kotlin.coroutines.resume

/**
 * 按[PrepareScrapScope.add]的添加顺序，预创建ViewHolder并放入[RecycledViewPool]
 *
 * **注意**：此函数应当在初始化视图时调用，并确保RecyclerView已设置Adapter，
 * 预创建流程会按[prepareDeadline]进行停止，避免产生不必要的创建开销。
 *
 * @param prepareAdapter    调用[Adapter.createViewHolder]创建ViewHolder
 * @param prepareDeadline   预创建的截止时间，默认为[PrepareDeadline.FOREVER_NS]
 * @param prepareDispatcher 用于预创建的协程调度器，默认为[Dispatchers.IO]
 * @param block             调用[PrepareScrapScope.add]，添加预创建的键值对
 */
@MainThread
@ExperimentalFeature
suspend fun RecyclerView.prepareScrap(
    prepareAdapter: Adapter<*>,
    prepareDeadline: PrepareDeadline = PrepareDeadline.FOREVER_NS,
    prepareDispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: PrepareScrapScope.() -> Unit
): List<ViewHolder> {
    assertMainThread()
    require(adapter != null) { "请先对RecyclerView设置Adapter" }
    val rv = this
    val preparePairs = ArrayList(PrepareScrapScope().apply(block).pairs)
    val scrapList = ArrayList<ViewHolder>(preparePairs.sumOf { it.count })

    // 通过Handler发送的消息可能会被doFrame消息按时间顺序插队，
    // 也就导致处理完doFrame消息后，才往RecycledViewPool放入scrap，
    // 调整为在doFrame消息的Animation回调下往RecycledViewPool放入scrap。
    withContext(ChoreographerDispatcher()) {
        val deadlineNs = DeadlineNs()
        val deadlineJob = when (prepareDeadline) {
            PrepareDeadline.FOREVER_NS -> null
            PrepareDeadline.FRAME_NS -> launch(start = UNDISPATCHED) {
                // 将视图树首帧Vsync时间或者更新时下一帧Vsync时间，作为预创建的截止时间
                deadlineNs.set(prepareAdapter.awaitDeadlineNs())
            }
        }

        val channel = Channel<ViewHolder>(BUFFERED)
        // 对prepareDispatcher调度的线程临时设置主线程Looper，
        // 确保View的构造过程获取到主线程Looper，例如GestureDetector。
        val prepareContext = prepareDispatcher + LooperElement(Looper.myLooper()!!)
        launch(prepareContext) {
            preparePairs.accessEach { (viewType, count) ->
                var remainingCount = count
                while (remainingCount > 0 && deadlineNs.checkVolatile()) {
                    remainingCount--
                    // FIXME: createViewHolder()执行异常反馈出去
                    channel.send(prepareAdapter.createViewHolder(rv, viewType))
                }
            }
        }.invokeOnCompletion {
            channel.close()
        }

        for (scrap in channel) {
            if (!deadlineNs.checkPlain()) break
            // FIXME: putScrap()执行异常反馈出去
            rv.putScrap(scrap)
            scrapList.add(scrap)
        }
        deadlineJob?.cancelAndJoin()
    }
    return scrapList
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
 * 调用[PrepareScrapScope.add]，添加预创建的键值对
 */
class PrepareScrapScope @PublishedApi internal constructor() {
    @PublishedApi
    internal val pairs = mutableListOf<Pair>()

    fun add(viewType: Int, @IntRange(from = 1) count: Int) {
        pairs.add(Pair(viewType, count.coerceAtLeast(1)))
    }

    /**
     * 不使用[kotlin.Pair]，有装箱拆箱的开销
     */
    @PublishedApi
    internal data class Pair(val viewType: Int, val count: Int)
}

@MainThread
private fun RecyclerView.putScrap(scrap: ViewHolder) {
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

private class DeadlineNs {
    private var plainValue = Long.MAX_VALUE
    @Volatile private var volatileValue = Long.MAX_VALUE

    fun set(deadlineNs: Long) {
        plainValue = deadlineNs
        volatileValue = deadlineNs
    }

    fun checkPlain() = System.nanoTime() < plainValue

    fun checkVolatile() = System.nanoTime() < plainValue
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
}