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
import com.xiaocydx.sample.transition.transform.SenderId
import com.xiaocydx.sample.transition.transform.TransformReceiver
import com.xiaocydx.sample.transition.transform.TransformSender
import com.xiaocydx.sample.transition.transform.TransformSenderId
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * @author xcc
 * @date 2023/11/22
 */
object VideoStream {
    private val sharedStore = mutableMapOf<String, SharedHolder<*>>()
    const val KEY_SHARED_TOKEN = "com.xiaocydx.sample.paging.complex.KEY_SHARED_TOKEN"

    /**
     * 创建[VideoStreamShared]
     *
     * 通过该函数创建的[VideoStreamShared]，共享计数会+1，当[scope]被取消时，共享计数-1，
     * 若共享计数归0，则取消[VideoStreamShared]，[VideoStreamShared.isActive]发射`false`。
     */
    @MainThread
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
     * 若共享计数归0，则取消[VideoStreamShared]，[VideoStreamShared.isActive]发射`false`。
     */
    @MainThread
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
     * 该函数用于处理进程重启后，[sharedFrom]返回[EmptyShared]无法进行交互的情况，
     * 按home键退到后台，输入adb shell am kill com.xiaocydx.sample命令可杀掉进程。
     */
    @MainThread
    fun popWhenInactive(fragment: Fragment, sharedActive: StateFlow<Boolean>) {
        assertMainThread()
        fragment.lifecycleScope.launch {
            sharedActive.firstOrNull { !it } ?: return@launch
            fragment.lifecycle.withResumed { fragment.parentFragmentManager.popBackStack() }
        }
    }

    private fun assertMainThread() {
        assert(Thread.currentThread() === Looper.getMainLooper().thread)
    }

    private fun SharedHolder<*>.sharedIn(scope: CoroutineScope) {
        if (incrementAndGet() == 0) return
        scope.launch(Dispatchers.Main.immediate) {
            awaitCancellation()
        }.invokeOnCompletion {
            assertMainThread()
            if (decrementAndGet() == 0) {
                sharedStore.remove(shared.token)
                release()
            }
        }
    }

    private class SharedHolder<T : Any>(val shared: VideoStreamSharedImpl<T>) {
        private var count = 0
        val token = shared.token

        fun incrementAndGet(): Int {
            if (shared.isActive.value) count++
            return count
        }

        fun decrementAndGet(): Int {
            count = (count - 1).coerceAtLeast(0)
            return count
        }

        fun release() {
            assert(count == 0)
            shared.cancel()
        }
    }
}

/**
 * [TransformSender]和[TransformReceiver]共享的分页数据源
 *
 * **注意**：子类需要确保对象存储在单例中，不会出现内存泄漏问题。
 */
abstract class SharedSource<K : Any, T : Any>(
    val initKey: K,
    val config: PagingConfig
) : PagingSource<K, T> {

    /**
     * 加载成功的结果，会通过[toViewStreamList]转换为[VideoStreamItem]
     */
    abstract override suspend fun load(params: LoadParams<K>): LoadResult<K, T>

    abstract fun toVideoStreamList(data: List<T>): List<VideoStreamItem>
}

/**
 * [TransformSender]和[TransformReceiver]的共享资源，
 * 属性和函数用`Sender`和`Receiver`前缀演示数据的走向。
 */
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
    fun consumeReceiverState(): VideoStreamInitialState?
}

data class VideoStreamInitialState(val position: Int, val videoList: List<VideoStreamItem>)

private class VideoStreamSharedImpl<T : Any>(
    private val source: SharedSource<*, T>
) : VideoStreamShared<T> {
    @Suppress("UNCHECKED_CAST")
    private val pager = Pager(source.initKey, source.config, source as PagingSource<Any, T>)
    private val _isActive = MutableStateFlow(true)
    private val _senderId = SenderId()
    private var receiverState: VideoStreamInitialState? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override val token = UUID.randomUUID().toString()
    override val isActive = _isActive.asStateFlow()
    override val senderId = _senderId.asTransform()
    override val senderFlow = pager.flow.broadcastIn(scope)
    override val receiverFlow = senderFlow.flowMap {
        it.dataMap { _, data -> source.toVideoStreamList(data) }
    }
    override val loadStates: LoadStates
        get() = pager.loadStates

    init {
        scope.launch {
            awaitCancellation()
        }.invokeOnCompletion {
            // TODO: senderId需要完善cancel
            _isActive.value = false
        }
    }

    fun cancel() {
        scope.cancel()
    }

    override fun refresh() = pager.refresh()

    override fun append() = pager.append()

    override fun retry() = pager.retry()

    override fun syncSenderId(id: String) = _senderId.sync(id)

    override fun setReceiverState(id: String, list: List<T>?) {
        _senderId.record(id)
        receiverState = null
        if (list.isNullOrEmpty()) return
        val videoList = source.toVideoStreamList(list)
        val position = videoList.indexOfFirst { it.id == id }.coerceAtLeast(0)
        receiverState = VideoStreamInitialState(position, videoList)
    }

    override fun consumeReceiverState() = receiverState?.also { receiverState = null }
}

private object EmptyShared : VideoStreamShared<Any> {
    override val token = "EmptyShared"
    override val isActive = inactive()
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

    private fun <T> emptyFlow() = flow<T> { }

    private fun inactive() = object : StateFlow<Boolean> {
        override val replayCache = emptyList<Boolean>()
        override val value = false
        override suspend fun collect(collector: FlowCollector<Boolean>): Nothing {
            collector.emit(false)
            awaitCancellation()
        }
    }

    private fun emptySenderId() = object : TransformSenderId {
        override val syncEvent = flow<String> {}
        override fun consume() = null
    }
}