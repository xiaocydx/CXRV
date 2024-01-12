package com.xiaocydx.sample.paging.complex

import android.os.Bundle
import android.os.Looper
import androidx.annotation.CheckResult
import androidx.annotation.MainThread
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import com.xiaocydx.accompanist.transition.transform.SyncTransformSenderId
import com.xiaocydx.accompanist.transition.transform.TransformSenderId
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
import com.xiaocydx.sample.paging.complex.VideoStream.KEY_ID
import com.xiaocydx.sample.paging.complex.VideoStream.KEY_NAME
import com.xiaocydx.sample.paging.complex.VideoStream.Receiver
import com.xiaocydx.sample.paging.complex.VideoStream.Sender
import com.xiaocydx.sample.paging.complex.VideoStream.Shared
import com.xiaocydx.sample.paging.complex.VideoStream.Source
import com.xiaocydx.sample.paging.complex.VideoStream.StatefulSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.reflect.KClass

/**
 * [VideoStream]仅在Activity和Activity之间、Fragment和Fragment之间起到传递作用，
 * 为了让实现足够简单，[VideoStream]、[Sender]、[Receiver]的函数仅支持主线程调用。
 *
 * Fragment和Fragment之间不通过ParentFragment的ViewModel共享[Shared]对象，
 * 因为在实际场景中，两个Fragment不一定有相同的ParentFragment，以视频流页面为例，
 * [VideoStreamFragment]可能作为通用页面供多处业务复用，实现为Activity的直接Fragment，
 * 此时两个Fragment没有相同的ParentFragment，如果通过Activity的ViewModel实现共享需求，
 * 那么当两个Fragment都退出回退栈时，该如何清除Activity的ViewModel的[Shared]对象？
 *
 * 不能在其中一个Fragment退出回退栈时，就清除[Shared]对象，因为另一个Fragment可能还在使用，
 * [VideoStream]提供了简易的解决方案，能否利用依赖注入框架解决上述问题，不在当前讨论范围内。
 *
 * **注意**：作为Receiver的Activity，其主题应当包含`windowIsTranslucent= true`，
 * 这能让作为Sender的Activity跟Receiver一起重建，以及处理事件时可以进行重新布局。
 *
 * @author xcc
 * @date 2023/11/22
 */
@MainThread
object VideoStream {
    private val store = mutableMapOf<String, SharedHolder>()
    const val KEY_ID = "com.xiaocydx.sample.paging.complex.KEY_ID"
    const val KEY_NAME = "com.xiaocydx.sample.paging.complex.KEY_NAME"

    /**
     * 获取面向[Sender]的[Shared]对象，通过[handle]保存内部状态
     *
     * 通过该函数获取的[Shared]对象，共享计数会+1，当[scope]被取消时，共享计数-1，
     * 若共享计数归0，则移除[Shared]对象，[Shared.isActive]发射`false`，结束共享。
     *
     * 以`SenderFragment`和`ReceiverFragment`之间共享[Shared]对象为例，
     * `SenderViewModel`和`ReceiverViewModel`分别为两个Fragment的ViewModel，
     * 当因系统内存不足开始释放内存时，[Sender]和[Receiver]已通过`handle`保存内部状态，
     * 当页面重新创建时，[sender]或[receiver]都可以根据保存的状态重新创建[Shared]对象，
     * 页面重建过程，即使先调用[receiver]，后调用[sender]，[Shared]对象也会是同一个。
     * ```
     * class SenderViewModel(handle: SavedStateHandle) : ViewModel() {
     *     private val shared = VideoStream.sender(Source::class, handle, viewModelScope)
     * }
     *
     * class ReceiverViewModel(handle: SavedStateHandle) : ViewModel() {
     *     private val shared = VideoStream.receiver(handle, viewModelScope)
     * }
     * ```
     *
     * 当系统内存不足时，通过销毁Activity（Android 11起，单Task也可销毁Activity）或者杀掉进程释放内存，
     * 模拟杀掉进程，可按home键将应用退到后台，输入adb shell am kill com.xiaocydx.sample命令杀掉进程。
     */
    @CheckResult
    fun <T, S> sender(
        clazz: KClass<S>,
        handle: SavedStateHandle,
        scope: CoroutineScope
    ): Sender<T, S> where T : Any, S : Source<*, T> {
        assertMainThread()
        var id = handle.get<String>(KEY_ID)
        if (id.isNullOrEmpty()) {
            id = UUID.randomUUID().toString()
            handle[KEY_ID] = id
        }
        @Suppress("UNCHECKED_CAST")
        return shared(id, clazz.java, handle, scope) as Sender<T, S>
    }

    /**
     * 获取面向[Receiver]的[Shared]对象，通过[handle]保存内部状态
     *
     * 通过该函数获取的[Shared]对象，共享计数会+1，当[scope]被取消时，共享计数-1，
     * 若共享计数归0，则移除[Shared]对象，[Shared.isActive]发射`false`，结束共享。
     */
    @CheckResult
    fun receiver(
        handle: SavedStateHandle,
        scope: CoroutineScope
    ): Receiver {
        assertMainThread()
        val id = handle.get<String>(KEY_ID)
        val name = handle.get<String>(KEY_NAME)
        require(!id.isNullOrEmpty())
        require(!name.isNullOrEmpty())
        val clazz = Class.forName(
            name, false,
            VideoStream::class.java.classLoader
        ).asSubclass(Source::class.java)
        return shared(id, clazz, handle, scope)
    }

    @CheckResult
    private fun shared(
        id: String,
        clazz: Class<out Source<*, *>>,
        handle: SavedStateHandle,
        scope: CoroutineScope
    ): VideoStreamShared<*, *> {
        var holder = store[id]
        if (holder == null) {
            @Suppress("UNCHECKED_CAST")
            val source = clazz.newInstance() as Source<Any, *>
            if (source is StatefulSource<Any, *>) source.init(handle)
            holder = SharedHolder(VideoStreamShared(id, source))
            store[holder.id] = holder
        }
        holder.sharedIn(scope)
        return holder.shared
    }

    /**
     * 视频流的共享类，子接口有且仅有[Sender]和[Receiver]
     */
    @MainThread
    sealed interface Shared {
        /**
         * 是否处于活跃状态，当发射`false`时，表示[Sender]和[Receiver]结束共享
         */
        val isActive: StateFlow<Boolean>

        /**
         * 对应[Pager.loadStates]
         */
        val loadStates: LoadStates

        /**
         * 对应[Pager.refresh]
         */
        fun refresh()

        /**
         * 对应[Pager.append]
         */
        fun append()

        /**
         * 对应[Pager.retry]
         */
        fun retry()
    }

    /**
     * [Shared]的发送端
     */
    @MainThread
    interface Sender<T : Any, S : Source<*, T>> : Shared {
        /**
         * 分页数据源，为[senderFlow]提供分页数据和转换处理
         */
        val source: S

        /**
         * 同步id，可用于同步滚动位置
         */
        val senderId: TransformSenderId

        /**
         * 分页数据流，是[Receiver.receiverFlow]的上游
         */
        val senderFlow: Flow<PagingData<T>>

        /**
         * 设置[Receiver.Initial]，跟[Receiver.consumeReceiverState]是对称逻辑
         */
        fun setReceiverState(id: String, list: List<T>?)

        /**
         * 将内部状态转换为[Bundle]，作为[Receiver]的页面传参
         */
        fun toBundle(): Bundle
    }

    /**
     * [Shared]的接收端
     */
    @MainThread
    interface Receiver : Shared {
        /**
         * 分页数据流，是[Sender.senderFlow]的下游
         */
        val receiverFlow: Flow<PagingData<VideoStreamItem>>

        /**
         * 同步[Sender.senderId]
         */
        fun syncSenderId(id: String)

        /**
         * 消费[Sender.setReceiverState]设置的[Initial]
         */
        fun consumeReceiverState(): Initial?

        /**
         * [Receiver]的初始状态，用于初始化[Receiver]的选中位置和列表状态
         */
        data class Initial(val position: Int, val list: List<VideoStreamItem>)
    }

    /**
     * [Sender]和[Receiver]共享的分页数据源
     *
     * **注意**：实现类不应当有状态，因为页面重建后，新创建的[Source]会缺失状态，
     * 如果实现类确实要有状态，那么实现[StatefulSource]，并在[Sender]修改状态，
     * 详细的描述可以看[StatefulSource]的注释。
     */
    @MainThread
    interface Source<K : Any, T : Any> : PagingSource<K, T> {
        /**
         * 对应[Pager.initKey]
         */
        val initKey: K

        /**
         * 对应[Pager.config]
         */
        val config: PagingConfig

        /**
         * 加载成功的结果，会通过[toViewStreamList]转换为[VideoStreamItem]
         */
        override suspend fun load(params: LoadParams<K>): LoadResult<K, T>

        /**
         * 将[load]的加载结果转换为视频流页面的[VideoStreamItem]
         *
         * 若加载结果是复合数据，即包含视频以外的`item`，则需要进行过滤，
         * 过滤后视频流加载结果可能为空，视频流页面会自动触发下一页加载，
         * 因此实现[load]时，不必循环加载补充[VideoStreamItem]。
         */
        fun toVideoStreamList(list: List<T>): List<VideoStreamItem>
    }

    /**
     * [Sender]和[Receiver]共享的分页数据源，支持保存轻量的状态
     *
     * ```
     * class SenderViewModel(private val handle: SavedStateHandle) : ViewModel() {
     *     private val shared = VideoStream.sender(SearchSource::class, handle, viewModelScope)
     *
     *     fun setKeyword(keyword: String) {
     *         shared.source.setKeyword(handle, keyword)
     *         // 修改了source的keyword，刷新重新加载
     *         shared.refresh()
     *     }
     * }
     *
     * class SearchSource : VideoStream.StatefulSource<Int, ComplexItem>() {
     *     // keyword用于加载过程，即使进程被杀掉，重建后也会恢复keyword
     *     private val keyword: String
     *         get() = get<String>(KEY_KEYWORD) ?: ""
     *
     *     fun setKeyword(handle: SavedStateHandle, keyword: String) {
     *         set(handle, KEY_KEYWORD, keyword)
     *     }
     *
     *     private companion object {
     *         const val KEY_KEYWORD = "KEY_KEYWORD"
     *     }
     * }
     * ```
     */
    @MainThread
    abstract class StatefulSource<K : Any, T : Any> : Source<K, T> {
        private val regular = mutableMapOf<String, Any?>()

        internal fun init(handle: SavedStateHandle) {
            assertMainThread()
            handle.keys().forEach { regular[it] = handle[it] }
        }

        protected fun <V> get(key: String): V? {
            assertMainThread()
            @Suppress("UNCHECKED_CAST")
            return regular[key] as V?
        }

        protected fun <V> set(handle: SavedStateHandle, key: String, value: V?) {
            assertMainThread()
            handle[key] = value
            regular[key] = value
        }

        internal fun toBundle(): Bundle {
            assertMainThread()
            return bundleOf(*regular.map { it.key to it.value }.toTypedArray())
        }
    }

    private fun SharedHolder.sharedIn(scope: CoroutineScope) {
        if (increment() == 0) return
        scope.launch(Dispatchers.Main.immediate) {
            awaitCancellation()
        }.invokeOnCompletion {
            assertMainThread()
            if (decrement() == 0) {
                store.remove(id)
                cancel()
            }
        }
    }

    private class SharedHolder(val shared: VideoStreamShared<*, *>) {
        private var count = 0
        val id = shared.id

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

private fun assertMainThread() {
    assert(Thread.currentThread() === Looper.getMainLooper().thread)
}

private class VideoStreamShared<T, S>(
    val id: String, override val source: S
) : Sender<T, S>, Receiver where T : Any, S : Source<Any, T> {
    private val pager = Pager(source.initKey, source.config, source)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _isActive = MutableStateFlow(true)
    private val _senderId = SyncTransformSenderId()
    private var receiverState: Receiver.Initial? = null

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
            receiverState = null
            _senderId.close()
            _isActive.value = false
        }
    }

    fun cancel() = scope.cancel()

    override fun refresh() = pager.refresh()

    override fun append() = pager.append()

    override fun retry() = pager.retry()

    override fun syncSenderId(id: String) {
        assertMainThread()
        if (!_isActive.value) return
        _senderId.sync(id)
    }

    override fun setReceiverState(id: String, list: List<T>?) {
        assertMainThread()
        if (!_isActive.value) return
        _senderId.record(id)
        receiverState = null
        if (list.isNullOrEmpty()) return
        val videoList = source.toVideoStreamList(list)
        val position = videoList.indexOfFirst { it.id == id }.coerceAtLeast(0)
        receiverState = Receiver.Initial(position, videoList)
    }

    override fun consumeReceiverState(): Receiver.Initial? {
        assertMainThread()
        return receiverState?.also { receiverState = null }
    }

    override fun toBundle(): Bundle {
        assertMainThread()
        if (!_isActive.value) return Bundle()
        val bundle = source.let {
            it as? StatefulSource<*, *>
        }?.toBundle() ?: Bundle(2)
        bundle.putString(KEY_ID, id)
        bundle.putString(KEY_NAME, source.javaClass.name)
        return bundle
    }
}