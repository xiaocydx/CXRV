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

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.xiaocydx.cxrv.paging

import com.xiaocydx.cxrv.internal.flowOnMain
import com.xiaocydx.cxrv.internal.unsafeFlow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.job

/**
 * 分页的主要入口，提供[PagingData]数据流，调用[refresh]，
 * 会发射新的[PagingData]并取消旧的[PagingData]的事件流。
 *
 * 1.在Repository下创建[Pager]，对外提供[Pager]或者[Pager.flow]
 * ```
 * class FooRepository {
 *     val pager: Pager<String, Foo> = ...
 *     val flow = pager.flow
 * }
 * ```
 *
 * 2.对ViewModel注入Repository
 * ```
 * class FooViewModel(repository: FooRepository) : ViewModel() {
 *     val flow = repository.flow
 * }
 * ```
 *
 * 3.在视图控制器下收集`viewModel.flow`
 * ```
 * class FooActivity : AppCompatActivity() {
 *     private val viewModel: FooViewModel by viewModels()
 *     private val adapter: ListAdapter<Foo, *> = ...
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *          super.onCreate(savedInstanceState)
 *          viewModel.flow
 *               .onEach(adapter.pagingCollector)
 *               .launchIn(lifecycleScope)
 *     }
 * }
 * ```
 *
 * 4.仅在视图控制器活跃期间收集`viewModel.flow`，使用操作符[storeIn]
 * ```
 * class FooViewModel(repository: FooRepository) : ViewModel() {
 *     val flow = repository.flow.storeIn(viewModelScope)
 * }
 *
 * class FooActivity : AppCompatActivity() {
 *     private val viewModel: FooViewModel by viewModels()
 *     private val adapter: ListAdapter<Foo, *> = ...
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *          super.onCreate(savedInstanceState)
 *          // 注意：flowWithLifecycle()在onEach(adapter.pagingCollector)之后调用
 *          viewModel.flow
 *               .onEach(adapter.pagingCollector)
 *               .flowWithLifecycle(lifecycle)
 *               .launchIn(lifecycleScope)
 *
 *          // 或者直接通过repeatOnLifecycle()进行收集，选中其中一种写法即可
 *          lifecycleScope.launch {
 *              repeatOnLifecycle(Lifecycle.State.STARTED) {
 *                  viewModel.flow.onEach(adapter.pagingCollector).collect()
 *              }
 *          }
 *     }
 * }
 * ```
 *
 * @author xcc
 * @date 2021/9/13
 */
class Pager<K : Any, T : Any>(
    private val initKey: K,
    private val config: PagingConfig,
    private val source: PagingSource<K, T>
) {
    private val refreshEvent = ConflatedEvent<Unit>()
    private val appendEvent = ConflatedEvent<Unit>()
    private val retryEvent = ConflatedEvent<Unit>()
    private var isCollected = false
    @Volatile private var fetcher: PagingFetcher<K, T>? = null
    val loadStates: LoadStates
        get() = fetcher?.loadStates ?: LoadStates.Incomplete

    val flow: Flow<PagingData<T>> = unsafeFlow {
        coroutineScope {
            check(!isCollected) { "分页数据流Flow<PagingData<T>>只能被1个收集器收集" }
            isCollected = true
            val scope = currentCoroutineContext().job
            scope.invokeOnCompletion { isCollected = false }
            refreshEvent.flow.onStart {
                // 触发初始化加载
                emit(Unit)
            }.collect {
                fetcher?.close()
                fetcher = PagingFetcher(initKey, config, source, appendEvent, retryEvent)
                emit(PagingData(fetcher!!.flow, mediator = PagingMediatorImpl(fetcher!!)))
            }
        }
    }.conflate().flowOnMain()

    fun refresh() {
        refreshEvent.send(Unit)
    }

    fun append() {
        appendEvent.send(Unit)
    }

    fun retry() {
        retryEvent.send(Unit)
    }

    private inner class PagingMediatorImpl(
        private val fetcher: PagingFetcher<*, *>
    ) : PagingMediator {
        override val loadStates: LoadStates
            get() = fetcher.loadStates

        override val refreshStartScrollToFirst: Boolean
            get() = config.refreshStartScrollToFirst

        override val appendFailureAutToRetry: Boolean
            get() = config.appendFailureAutToRetry

        override val appendPrefetch: PagingPrefetch
            get() = config.appendPrefetch

        override fun refresh() = this@Pager.refresh()

        override fun append() = this@Pager.append()

        override fun retry() = this@Pager.retry()
    }
}