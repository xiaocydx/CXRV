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

import com.xiaocydx.cxrv.list.ListOwner
import com.xiaocydx.cxrv.list.ListState
import com.xiaocydx.cxrv.list.UpdateOp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * ### 函数作用
 * 1. 将`Flow<PagingData<T>>`发射的事件的列表数据保存到[state]中。
 * 2. [state]和视图控制器建立基于[ListOwner]的双向通信。
 * 3. 将`Flow<PagingData<T>>`转换为热流，
 * 热流可以被重复收集，但同时只能被一个收集器收集，
 * 热流被首次收集时，才会开始收集它的上游，直到[scope]被取消。
 *
 * ### 调用顺序
 * 不允许在[storeIn]之后，调用[broadcastIn]或[flowMap]转换[PagingData.flow]，
 * [state]和视图控制器会建立双向通信，需要确保[state]跟视图控制器的数据一致。
 *
 * 在ViewModel中使用[storeIn]：
 * ```
 * class FooViewModel : ViewModel(private val repository: FooRepository) {
 *     private val state = ListState<Foo>()
 *     val flow = repository.flow
 *         .flowMap {...} // 转换分页事件流的分页数据
 *         .storeIn(state, viewModelScope)
 * }
 * ```
 */
fun <T : Any> Flow<PagingData<T>>.storeIn(
    state: ListState<T>,
    scope: CoroutineScope
): Flow<PagingData<T>> {
    if (this is StoreInPagingDataStateFlow) return this
    var previous: StoreInPagingEventSharedFlow<T>? = null
    val upstream: Flow<PagingData<T>> = map { data ->
        previous?.cancel()
        val mediator = PagingListMediator(data, state)
        val flow = StoreInPagingEventSharedFlow(scope, mediator.flow, mediator)
        previous = flow
        PagingData(flow, mediator)
    }
    return StoreInPagingDataStateFlow(scope, upstream)
}

/**
 * [storeIn]的简化函数，适用于只需要重建恢复列表状态，不需要主动更新列表状态的场景
 */
fun <T : Any> Flow<PagingData<T>>.storeIn(scope: CoroutineScope) = storeIn(ListState(), scope)

@PublishedApi
internal val PagingData<*>.isFromStoreInOperator: Boolean
    get() = mediator.asListMediator<Any>() != null

@PublishedApi
internal inline fun PagingData<*>.ensureBeforeStoreInOperator(lazyFunctionName: () -> String) {
    check(!isFromStoreInOperator) { "${lazyFunctionName()}必须在Flow<PagingData<T>>.storeIn()之前调用" }
}

private class StoreInPagingDataStateFlow<T : Any>(
    scope: CoroutineScope,
    upstream: Flow<PagingData<T>>
) : PagingSharedFlow<PagingData<T>>(scope, upstream, limitCollectorCount = 1) {
    private var state: PagingData<T>? = null

    override fun onActive(): PagingData<T>? = state

    override fun onReceive(value: PagingData<T>) {
        state = value
    }
}

private class StoreInPagingEventSharedFlow<T : Any>(
    scope: CoroutineScope,
    upstream: Flow<PagingEvent<T>>,
    private val mediator: PagingListMediator<T>
) : PagingSharedFlow<PagingEvent<T>>(scope, upstream, limitCollectorCount = 1) {
    private var isFirstActive = true

    override fun onActive(): PagingEvent<T>? = mediator.run {
        val isFirstActive = consumeFirstActive()
        if (isFirstActive && version == 0
                && loadStates.refresh.isIncomplete
                && loadStates.append.isIncomplete) {
            return@run null
        }
        val op: UpdateOp<T> = UpdateOp.SubmitList(currentList)
        PagingEvent.ListStateUpdate(op, loadStates).fusion(version)
    }

    private fun consumeFirstActive() = isFirstActive.also { isFirstActive = false }
}