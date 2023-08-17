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
 * 2. 转换的热流可以被多个收集器收集，当热流被首次收集时，才开始收集上游，
 * 直到[scope]被取消，或者当收集器数量为`0`时取消收集上游，大于`0`时重新收集上游。
 *
 * ### 调用顺序
 * 不允许在[storeIn]之后，调用[broadcastIn]转换`Flow<PagingData<T>>`，
 * 这会抛出[IllegalArgumentException]，详细原因可以看[storeIn]的注释。
 *
 * 在Activity内共享[broadcastIn]的转换结果（[Pager]的注释解释了如何收集`flow`）：
 * ```
 * // FooViewModel跟Activity作用域关联
 * class FooViewModel : ViewModel(repository: FooRepository) {
 *     private val pager = repository.pager
 *     val broadcastFlow = pager.flow.broadcastIn(viewModelScope)
 * }
 *
 * // 将FooViewModel.broadcastFlow传递给Fragment的ViewModel，
 * // Fragment1和Fragment2共享分页数据流和加载状态，分离列表状态。
 * class Fragment1ViewModel : ViewModel(broadcastFlow: Flow<PagingData<Foo>>) {
 *     private val state = ListState<Foo>()
 *     val flow = broadcastFlow
 *         .flowMap {...} // 转换分页事件流的列表数据
 *         .appendPrefetch(prefetch) // 转换末尾加载的预取策略
 *         .storeIn(state, viewModelScope)
 * }
 *
 * class Fragment2ViewModel : ViewModel(broadcastFlow: Flow<PagingData<Foo>>) {
 *     private val state = ListState<Foo>()
 *     val flow = broadcastFlow
 *         .flowMap {...} // 转换分页事件流的列表数据
 *         .appendPrefetch(prefetch) // 转换末尾加载的预取策略
 *         .storeIn(state, viewModelScope)
 * }
 * ```
 */
fun <T : Any> Flow<PagingData<T>>.broadcastIn(scope: CoroutineScope): Flow<PagingData<T>> {
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
    withoutCollectorNeedCancel = true,
    canRepeatCollectAfterCancel = true,
)

private class BroadcastInPagingEventShareFlow<T : Any>(
    scope: CoroutineScope,
    upstream: Flow<PagingEvent<T>>
) : PagingSharedFlow<PagingEvent<T>>(
    scope = scope,
    upstream = upstream,
    withoutCollectorNeedCancel = true,
    canRepeatCollectAfterCancel = false
)