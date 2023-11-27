package com.xiaocydx.sample.paging.complex

import android.os.Looper
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import com.xiaocydx.cxrv.paging.LoadParams
import com.xiaocydx.cxrv.paging.LoadResult
import com.xiaocydx.cxrv.paging.LoadStates
import com.xiaocydx.cxrv.paging.Pager
import com.xiaocydx.cxrv.paging.PagingConfig
import com.xiaocydx.cxrv.paging.PagingData
import com.xiaocydx.cxrv.paging.PagingSource
import com.xiaocydx.cxrv.paging.broadcastIn
import com.xiaocydx.cxrv.paging.dataMap
import com.xiaocydx.cxrv.paging.flowMap
import com.xiaocydx.sample.transition.transform.SyncTransformSenderId
import com.xiaocydx.sample.transition.transform.TransformReceiver
import com.xiaocydx.sample.transition.transform.TransformSender
import com.xiaocydx.sample.transition.transform.TransformSenderId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * [VideoStream]仅在Fragment和Fragment之间、Activity和Activity之间起到传递作用，
 * 为了让实现足够简单，[VideoStream]和[VideoStreamShared]的函数仅支持主线程调用。
 *
 * @author xcc
 * @date 2023/11/22
 */
@MainThread
object VideoStream {
    private val sharedStore = mutableMapOf<String, SharedHolder<*>>()
    const val KEY_SHARED_TOKEN = "com.xiaocydx.sample.paging.complex.KEY_SHARED_TOKEN"

    /**
     * 创建[VideoStreamShared]
     *
     * 通过该函数创建的[VideoStreamShared]，共享计数会+1，当[scope]被取消时，共享计数-1，
     * 若共享计数归0，则移除[VideoStreamShared]，[VideoStreamShared.isActive]发射`false`。
     */
    fun <T : Any> makeShared(
        source: SharedSource<*, T>,
        scope: CoroutineScope
    ): VideoStreamShared<T> {
        assertMainThread()
        val holder = SharedHolder(VideoStreamSharedImpl(source))
        sharedStore[holder.token] = holder
        holder.sharedIn(scope)
        return holder.shared
    }

    /**
     * 通过[token]获取[VideoStreamShared]
     *
     * 通过该函数获取的[VideoStreamShared]，共享计数会+1，当[scope]被取消时，共享计数-1，
     * 若共享计数归0，则移除[VideoStreamShared]，[VideoStreamShared.isActive]发射`false`。
     */
    fun sharedFrom(
        token: String,
        scope: CoroutineScope
    ): VideoStreamShared<*> {
        assertMainThread()
        val holder = sharedStore[token]
        holder?.sharedIn(scope)
        return holder?.shared ?: EmptyShared
    }

    /**
     * 当[VideoStreamShared.isActive]发射`false`时，弹出栈顶的[fragment]
     *
     * 该函数用于处理进程重启后，[sharedFrom]返回[EmptyShared]导致无法交互的情况，
     * 按home键退到后台，输入adb shell am kill com.xiaocydx.sample命令可杀掉进程。
     */
    fun popWhenInactive(fragment: Fragment, sharedActive: StateFlow<Boolean>) {
        assertMainThread()
        fragment.lifecycleScope.launch {
            sharedActive.firstOrNull { !it } ?: return@launch
            fragment.lifecycle.withResumed { fragment.parentFragmentManager.popBackStack() }
        }
    }

    private fun SharedHolder<*>.sharedIn(scope: CoroutineScope) {
        if (increment() == 0) return
        scope.launch(Dispatchers.Main.immediate) {
            awaitCancellation()
        }.invokeOnCompletion {
            assertMainThread()
            if (decrement() == 0) {
                sharedStore.remove(shared.token)
                cancel()
            }
        }
    }

    private class SharedHolder<T : Any>(val shared: VideoStreamSharedImpl<T>) {
        private var count = 0
        val token = shared.token

        fun increment(): Int {
            if (shared.isActive.value) count++
            return count
        }

        fun decrement(): Int {
            count = (count - 1).coerceAtLeast(0)
            return count
        }

        fun cancel() {
            assert(count == 0)
            shared.cancel()
        }
    }
}

/**
 * [TransformSender]和[TransformReceiver]的共享资源，
 * 属性和函数用`Sender`和`Receiver`前缀演示数据的走向。
 *
 * 当[isActive]发射`false`时，[senderId]、[senderFlow]、[receiverFlow]会被取消，停止发射值，
 * 收集它们的协程也会正常结束挂起，正常结束指的是[Flow.collect]不会抛出[CancellationException]。
 */
@MainThread
interface VideoStreamShared<T : Any> {
    val token: String
    val isActive: StateFlow<Boolean>
    val senderId: TransformSenderId
    val senderFlow: Flow<PagingData<T>>
    val receiverFlow: Flow<PagingData<VideoStreamItem>>
    val loadStates: LoadStates
    fun refresh()
    fun append()
    fun retry()
    fun syncSenderId(id: String)
    fun setReceiverState(id: String, list: List<T>?)
    fun consumeReceiverState(): VideoStreamInitial?
}

data class VideoStreamInitial(val position: Int, val list: List<VideoStreamItem>)

/**
 * [TransformSender]和[TransformReceiver]共享的分页数据源
 *
 * **注意**：子类需要确保对象存储在[VideoStream]中，不会出现内存泄漏问题。
 */
abstract class SharedSource<K : Any, T : Any>(
    val initKey: K,
    val config: PagingConfig
) : PagingSource<K, T> {

    /**
     * 加载成功的结果，会通过[toViewStreamList]转换为[VideoStreamItem]
     */
    abstract override suspend fun load(params: LoadParams<K>): LoadResult<K, T>

    abstract fun toVideoStreamList(list: List<T>): List<VideoStreamItem>
}

private fun assertMainThread() {
    assert(Thread.currentThread() === Looper.getMainLooper().thread)
}

private class VideoStreamSharedImpl<T : Any>(
    private val source: SharedSource<*, T>
) : VideoStreamShared<T> {
    @Suppress("UNCHECKED_CAST")
    private val pager = Pager(source.initKey, source.config, source as PagingSource<Any, T>)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _isActive = MutableStateFlow(true)
    private val _senderId = SyncTransformSenderId()
    private var receiverState: VideoStreamInitial? = null

    override val token = UUID.randomUUID().toString()
    override val isActive = _isActive.asStateFlow()
    override val senderId = _senderId.asSenderId()
    override val senderFlow = pager.flow.broadcastIn(scope)
    override val receiverFlow = senderFlow.flowMap { flow ->
        flow.dataMap { _, data -> source.toVideoStreamList(data) }
    }
    override val loadStates: LoadStates
        get() = pager.loadStates

    init {
        scope.launch {
            awaitCancellation()
        }.invokeOnCompletion {
            _isActive.value = false
            _senderId.close()
        }
    }

    fun cancel() = scope.cancel()

    override fun refresh() = pager.refresh()

    override fun append() = pager.append()

    override fun retry() = pager.retry()

    override fun syncSenderId(id: String) {
        if (!_isActive.value) return
        _senderId.sync(id)
    }

    override fun setReceiverState(id: String, list: List<T>?) {
        if (!_isActive.value) return
        _senderId.record(id)
        receiverState = null
        if (list.isNullOrEmpty()) return
        val videoList = source.toVideoStreamList(list)
        val position = videoList.indexOfFirst { it.id == id }.coerceAtLeast(0)
        receiverState = VideoStreamInitial(position, videoList)
    }

    override fun consumeReceiverState() = receiverState?.also { receiverState = null }
}

@Suppress("UNCHECKED_CAST")
private object EmptyShared : VideoStreamShared<Any>, Flow<Any> {
    override val token = "EmptyShared"
    override val isActive = emptyStateFlow()
    override val senderId = emptySenderId()
    override val senderFlow = emptyFlow<PagingData<Any>>()
    override val receiverFlow = emptyFlow<PagingData<VideoStreamItem>>()
    override val loadStates = LoadStates.Incomplete

    override fun refresh() = Unit
    override fun append() = Unit
    override fun retry() = Unit
    override fun syncSenderId(id: String) = Unit
    override fun setReceiverState(id: String, list: List<Any>?) = Unit
    override fun consumeReceiverState() = null

    private fun <T> emptyFlow() = this as Flow<T>

    private fun emptyStateFlow() = object : StateFlow<Boolean> {
        override val replayCache = emptyList<Boolean>()
        override val value = false
        override suspend fun collect(collector: FlowCollector<Boolean>): Nothing {
            collector.emit(false)
            awaitCancellation()
        }
    }

    private fun emptySenderId() = object : TransformSenderId {
        override val syncEvent = EmptyShared as Flow<String>
        override fun consume() = null
    }

    override suspend fun collect(collector: FlowCollector<Any>) = Unit
}