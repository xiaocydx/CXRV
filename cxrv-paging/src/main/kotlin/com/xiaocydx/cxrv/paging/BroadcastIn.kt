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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * ### 函数作用
 * 1. 将`Flow<PagingData<T>>`转换为热流，以广播的形式发射[PagingData]。
 * 2. 热流可以被重复收集，同时能被多个收集器收集，当热流被首次收集时，才开始收集上游，
 * 直到[scope]被取消，或者当收集器数量为`0`时取消收集上游，大于`0`时重新收集上游。
 *
 * ### 调用顺序
 * 若对`Flow<PagingData<T>>`先调用[storeIn]，后调用[broadcastIn]，
 * 则会抛出[IllegalArgumentException]异常，详细原因可以看[storeIn]的注释。
 *
 * 在Activity内共享[broadcastIn]的转换结果：
 * ```
 * // 跟Activity作用域关联的ActivityViewModel
 * class ActivityViewModel : ViewModel(private val repository: FooRepository) {
 *     private val state = ListState<Foo>()
 *     val broadcastFlow = repository.flow.broadcastIn(viewModelScope)
 *     val fooFlow = broadcastFlow.storeIn(state, viewModelScope)
 * }
 *
 * // 将ActivityViewModel.broadcastFlow传递给跟Fragment作用域关联的FragmentViewModel，
 * // ActivityViewModel和FragmentViewModel共享分页数据流和加载状态，同时分离列表状态。
 * class FragmentViewModel : ViewModel(broadcastFlow: Flow<PagingData<Foo>>) {
 *     private val state = ListState<Foo>()
 *     val fooFlow = broadcastFlow
 *         .flowMap {...} // 转换分页事件流的分页数据
 *         .appendPrefetch(prefetch) // 转换末尾加载的预取策略
 *         .storeIn(state, viewModelScope)
 * }
 * ```
 */
fun <T : Any> Flow<PagingData<T>>.broadcastIn(scope: CoroutineScope): Flow<PagingData<T>> {
    if (this is BroadcastInPagingDataStateFlow) return this
    var previous: BroadcastInPagingEventShareFlow<T>? = null
    val upstream: Flow<PagingData<T>> = map { data ->
        data.ensureBeforeStoreInOperator { "Flow<PagingData<T>>.broadcastIn()" }
        previous?.cancel()
        previous = BroadcastInPagingEventShareFlow(scope, data.flow)
        PagingData(previous!!, data.mediator)
    }
    return BroadcastInPagingDataStateFlow(scope, upstream)
}

private class BroadcastInPagingDataStateFlow<T : Any>(
    scope: CoroutineScope,
    upstream: Flow<PagingData<T>>
) : PagingStateFlow<PagingData<T>>(
    scope = scope,
    upstream = upstream,
    limitCollectorCount = UNLIMITED,
    withoutCollectorNeedCancel = true,
    canRepeatCollectAfterCancel = true,
)

private class BroadcastInPagingEventShareFlow<T : Any>(
    scope: CoroutineScope,
    upstream: Flow<PagingEvent<T>>
) : PagingSharedFlow<PagingEvent<T>>(
    scope = scope,
    upstream = upstream,
    limitCollectorCount = UNLIMITED,
    withoutCollectorNeedCancel = true,
    canRepeatCollectAfterCancel = false
)