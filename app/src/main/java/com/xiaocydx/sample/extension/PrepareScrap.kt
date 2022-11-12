@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.view.Choreographer
import androidx.annotation.MainThread
import androidx.recyclerview.widget.RecyclerView.*
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool.ScrapData
import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume

/**
 * 往[RecycledViewPool]预创建[prepareCount]个ViewHolder
 *
 * **注意**：
 * 此函数应当在初始化视图时调用，并且确保RecyclerView有父级、已设置Adapter和自己的[RecycledViewPool]，
 * 预创建流程会尽量在主线程执行RecyclerView布局流程之前停止，避免继续创建ViewHolder，产生不必要的开销。
 *
 * @param prepareCount      预创建的ViewHolder数量
 * @param prepareAdapter    调用[Adapter.createViewHolder]创建ViewHolder
 * @param prepareDispatcher 用于预创建的协程调度器，默认为[DefaultIoDispatcher]
 * @param prepareViewType   预创建的ViewType类型
 *
 * @return 收集返回的`Flow<ViewHolder>`，才开始执行预创建流程，
 * 预创建执行过程发射创建成功的ViewHolder，供收集处统计或测试。
 */
fun RecyclerView.prepareScrap(
    prepareCount: Int,
    prepareAdapter: Adapter<*>,
    prepareDispatcher: CoroutineDispatcher = DefaultIoDispatcher,
    prepareViewType: () -> Int
): Flow<ViewHolder> {
    require(adapter != null) { "请先对RecyclerView设置Adapter" }
    require(parent != null) { "请先将RecyclerView添加到父级中" }
    return unsafeFlow<ViewHolder> {
        val scrapData = getScrapDataForType(prepareViewType())
        val finalPrepareCount = prepareCount - scrapData.mScrapHeap.size
        if (finalPrepareCount <= 0) {
            // RecyclerViewPool中的ViewHolder数量已满足，结束预创建流程。
            return@unsafeFlow
        }

        coroutineScope {
            val deadlineNs = AtomicLong(Long.MAX_VALUE)
            val deadlineJob = launch(start = UNDISPATCHED) {
                // 将视图树首帧Vsync时间或者更新时下一帧Vsync时间，作为预创建的截止时间
                deadlineNs.set(prepareAdapter.awaitDeadlineNs())
            }

            // MainThread -> Choreographer
            val choreographer = Choreographer.getInstance()
            val recyclerView = this@prepareScrap
            val prepareFlow = unsafeFlow<ViewHolder> {
                var count = finalPrepareCount
                while (count > 0 && System.nanoTime() < deadlineNs.get()) {
                    // 由于使用unsafeFlow不检测执行上下文、异常透明性（提高发射性能），
                    // 因此emit()没有检查Job已取消的处理，需要补充判断以响应Job取消。
                    ensureActive()
                    count--
                    val scrap = prepareAdapter.createViewHolder(recyclerView, prepareViewType())
                    // 虽然协程主线程调度器默认发送异步消息，不受同步屏障影响，
                    // 但是异步消息可能会被首帧的doFrame消息按时间顺序插队，
                    // 也就导致处理完首帧doFrame消息后，才往RecycledViewPool添加scrap，
                    // 因此调整为在doFrame消息的Animation回调下往RecycledViewPool添加scrap。
                    // MainThread -> scrapData.mScrapHeap.add(scrap)
                    choreographer.postFrameCallback { scrapData.mScrapHeap.add(scrap) }
                    emit(scrap)
                }
            }.flowOn(prepareDispatcher)

            emitAll(prepareFlow)
            deadlineJob.cancel()
        }
    }.flowOn(Dispatchers.Main.immediate)
}

/**
 * 默认串行创建ViewHolder
 */
@OptIn(ExperimentalCoroutinesApi::class)
private val DefaultIoDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)

@MainThread
private fun RecyclerView.getScrapDataForType(viewType: Int): ScrapData {
    val pool = recycledViewPool
    val mScrap = pool.mScrap
    return mScrap[viewType] ?: kotlin.run {
        // 触发内部逻辑创建scrapData
        pool.getRecycledViewCount(viewType)
        mScrap.get(viewType)!!
    }
}

@MainThread
private suspend fun Adapter<*>.awaitDeadlineNs(): Long {
    return suspendCancellableCoroutine { cont ->
        DeadlineNsObserver(this, cont).attach()
    }
}

private class DeadlineNsObserver(
    private val adapter: Adapter<*>,
    private val cont: CancellableContinuation<Long>
) : AdapterDataObserver() {
    private var isRegister = false
    private var isResumed = false

    fun attach() {
        if (adapter.itemCount > 0) return tryResume()
        isRegister = true
        adapter.registerAdapterDataObserver(this)
        cont.invokeOnCancellation { adapter.unregisterAdapterDataObserver(this) }
    }

    override fun onChanged() = tryResume()

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = tryResume()

    private fun tryResume() {
        if (isResumed) return
        isResumed = true
        Choreographer.getInstance().postFrameCallback(::resume)
    }

    private fun resume(deadlineNs: Long) {
        if (isRegister) adapter.unregisterAdapterDataObserver(this)
        cont.resume(deadlineNs)
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