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

package com.xiaocydx.cxrv.paging

import androidx.annotation.MainThread
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Lazily
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 将[PagingData.flow]转换为热流，以广播的形式发射[PagingData]，用于多个页面间共享分页数据流和加载状态
 *
 * **注意**：若对`Flow<PagingData<T>>`先调用[storeIn]，后调用[multiple]，
 * 则会抛出[IllegalArgumentException]异常，详细原因可以看[storeIn]的注释。
 *
 * 在Activity内共享[multiple]的转换结果：
 * ```
 * // 跟Activity作用域关联的ActivityViewModel
 * class ActivityViewModel : ViewModel(private val repository: FooRepository) {
 *     private val state = ListState<Foo>()
 *     val pagingFlow = repository.flow.multiple(viewModelScope)
 *     val fooFlow = pagingFlow.storeIn(state, viewModelScope)
 * }
 *
 * // 将ActivityViewModel.pagingFlow传递给跟Fragment作用域关联的FragmentViewModel，
 * // ActivityViewModel和FragmentViewModel共享分页数据流和加载状态、分离列表状态。
 * class FragmentViewModel : ViewModel(pagingFlow: Flow<PagingData<Foo>>) {
 *     private val state = ListState<Foo>()
 *     val fooFlow = pagingFlow
 *         .flowMap { eventFlow ->
 *             eventFlow.itemMap { loadType, item -> ... }
 *         }
 *         .storeIn(state, viewModelScope)
 * }
 * ```
 */
fun <T : Any> Flow<PagingData<T>>.multiple(scope: CoroutineScope): Flow<PagingData<T>> {
    if (this is PagingDataStateFlow) return this
    var previous: PagingEventSharedFlow<T>? = null
    val upstream: Flow<PagingData<T>> = map { data ->
        data.ensureMultipleBeforeStoreIn()
        previous?.cancel()
        previous = PagingEventSharedFlow(scope, data.flow)
        PagingData(previous!!, data.mediator)
    }
    return PagingDataStateFlow(scope, upstream)
}

private fun PagingData<*>.ensureMultipleBeforeStoreIn() {
    check(mediator.asListMediator<Any>() == null) {
        "Flow<PagingData<T>>.multiple()必须在Flow<PagingData<T>>.storeIn()之前调用"
    }
}

private class PagingDataStateFlow<T : Any>(
    scope: CoroutineScope,
    upstream: Flow<PagingData<T>>,
) : Flow<PagingData<T>> {
    private val stateFlow = upstream
        .stateIn(scope, Lazily, null)
        .filterNotNull()

    override suspend fun collect(
        collector: FlowCollector<PagingData<T>>
    ) = collector.emitAll(stateFlow)
}

private class PagingEventSharedFlow<T : Any>(
    private val scope: CoroutineScope,
    private val upstream: Flow<PagingEvent<T>>,
    private val mainDispatcher: MainCoroutineDispatcher = Dispatchers.Main.immediate
) : Flow<PagingEvent<T>> {
    @Volatile private var collectJob: Job? = null
    private val sharedFlow = MutableSharedFlow<PagingEvent<T>?>()
    private val cancellableSharedFlow = sharedFlow.takeWhile { it != null }.mapNotNull { it }

    override suspend fun collect(collector: FlowCollector<PagingEvent<T>>) {
        withMainDispatcher { launchCollectJob() }
        collector.emitAll(cancellableSharedFlow)
    }

    @MainThread
    private fun launchCollectJob() {
        if (collectJob != null) return
        val coroutineName = CoroutineName(javaClass.simpleName)
        collectJob = scope.launch(coroutineName + mainDispatcher) {
            try {
                sharedFlow.emitAll(upstream)
            } finally {
                // 当前协程可能已被取消，用NonCancellable确保执行
                withContext(NonCancellable) { sharedFlow.emit(null) }
            }
        }
        collectJob!!.invokeOnCompletion {
            // MainThread
            collectJob = null
        }
    }

    private suspend inline fun <R> withMainDispatcher(
        context: CoroutineContext = EmptyCoroutineContext,
        crossinline block: suspend () -> R
    ): R {
        val dispatcher = mainDispatcher
        return if (dispatcher.isDispatchNeeded(EmptyCoroutineContext)) {
            withContext(dispatcher + context) { block() }
        } else {
            block()
        }
    }

    suspend fun cancel() {
        collectJob?.cancelAndJoin()
    }
}
