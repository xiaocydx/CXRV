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
@file:OptIn(InternalizationApi::class)

package com.xiaocydx.cxrv.paging

import androidx.annotation.VisibleForTesting
import com.xiaocydx.cxrv.internal.InternalizationApi
import com.xiaocydx.cxrv.list.ListOwner
import com.xiaocydx.cxrv.list.ListState
import com.xiaocydx.cxrv.list.MutableStateList
import com.xiaocydx.cxrv.list.UpdateOp
import com.xiaocydx.cxrv.list.toSafeMutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * ### 函数作用
 * 1. 将`Flow<PagingData<T>>`发射的事件的列表数据保存到[list]。
 * 2. [list]和视图控制器建立基于[ListOwner]的双向通信。
 * 3. 将`Flow<PagingData<T>>`转换为热流，热流可以被多个收集器收集，
 * 当热流被首次收集时，才开始收集上游，直到[scope]被取消。
 *
 * ### 调用顺序
 * 不允许在[storeIn]之后，调用[broadcastIn]或[flowMap]转换`Flow<PagingData<T>>`，
 * 因为[list]和视图控制器会建立双向通信，需要确保[list]跟视图控制器的数据一致。
 *
 * 在ViewModel中使用[storeIn]（[Pager]的注释解释了如何收集`flow`）：
 * ```
 * class FooViewModel : ViewModel(repository: FooRepository) {
 *     private val list = MutableStateList<Foo>()
 *     private val pager = repository.pager
 *     val flow = pager.flow
 *         .flowMap {...} // 转换分页事件流的列表数据
 *         .storeIn(list, viewModelScope)
 * }
 * ```
 *
 * 在Activity内共享[storeIn]的转换结果（[Pager]的注释解释了如何收集`flow`）：
 * ```
 * // FooViewModel跟Activity作用域关联，Fragment1和Fragment2共享分页数据流、加载状态、列表状态
 * class Fragment1 : Fragment(R.layout.fragment1) {
 *     private val viewModel: FooViewModel by activityViewModels()
 * }
 *
 * class Fragment2 : Fragment(R.layout.fragment2) {
 *     private val viewModel: FooViewModel by activityViewModels()
 * }
 * ```
 */
fun <T : Any> Flow<PagingData<T>>.storeIn(
    list: MutableStateList<T>,
    scope: CoroutineScope
): Flow<PagingData<T>> = storeIn(list.state, scope)

/**
 * [storeIn]的简化函数，适用于只需要重建恢复列表状态，不需要主动更新列表状态的场景
 */
fun <T : Any> Flow<PagingData<T>>.storeIn(scope: CoroutineScope) = storeIn(MutableStateList(), scope)

/**
 * [storeIn]的实现函数，[ListState]降级为内部API，[MutableStateList]替代[ListState]
 */
fun <T : Any> Flow<PagingData<T>>.storeIn(
    state: ListState<T>,
    scope: CoroutineScope
): Flow<PagingData<T>> = storeInInternal(state, scope, ::PagingListMediator)

@PublishedApi
internal val PagingData<*>.isFromStoreInOperator: Boolean
    get() = flow is StoreInPagingEventSharedFlow

@PublishedApi
internal inline fun PagingData<*>.ensureBeforeStoreInOperator(lazyFunctionName: () -> String) {
    check(!isFromStoreInOperator) { "${lazyFunctionName()}必须在Flow<PagingData<T>>.storeIn()之前调用" }
}

private fun PagingData<*>.ensureSingleStoreInOperator() {
    check(!isFromStoreInOperator) { "已调用Flow<PagingData<T>>.storeIn()构建最终分页数据流" }
}

@VisibleForTesting
internal inline fun <T : Any> Flow<PagingData<T>>.storeInInternal(
    state: ListState<T>,
    scope: CoroutineScope,
    crossinline transform: (PagingData<T>, ListState<T>) -> PagingListMediator<T>
): Flow<PagingData<T>> {
    var previous: StoreInPagingEventSharedFlow<T>? = null
    val upstream: Flow<PagingData<T>> = map { data ->
        data.ensureSingleStoreInOperator()
        previous?.cancel()
        val mediator = transform(data, state)
        val flow = StoreInPagingEventSharedFlow(scope, mediator.flow, mediator)
        previous = flow
        PagingData(flow, mediator)
    }
    return StoreInPagingDataStateFlow(scope, upstream)
}

@VisibleForTesting
internal class StoreInPagingDataStateFlow<T : Any>(
    scope: CoroutineScope,
    upstream: Flow<PagingData<T>>
) : PagingStateFlow<PagingData<T>>(
    scope = scope,
    upstream = upstream,
    withoutCollectorNeedCancel = false,
    canRepeatCollectAfterCancel = false
)

@VisibleForTesting
internal class StoreInPagingEventSharedFlow<T : Any>(
    scope: CoroutineScope,
    upstream: Flow<PagingEvent<T>>,
    private val mediator: PagingListMediator<T>,
    private val mainDispatcher: MainCoroutineDispatcher = Dispatchers.Main.immediate
) : PagingSharedFlow<PagingEvent<T>>(
    scope = scope,
    upstream = upstream,
    withoutCollectorNeedCancel = false,
    canRepeatCollectAfterCancel = false
) {

    override fun CoroutineScope.beforeCollect(collectorId: Int) {
        launch {
            // 确保先activeValue后upstream的发射顺序
            emitSharedFlow(collectorId, getActiveValue())
        }
    }

    /**
     * 丢弃非活跃状态期间[upstream]发射的事件，当恢复活跃状态时，主动发射事件进行差异更新
     */
    private suspend fun getActiveValue(): PagingEvent<T> = withMainDispatcher {
        // 对于共享分页数据流和加载状态的场景，初始加载状态可能不是Incomplete，
        // 此时需要发射事件，同步列表状态和加载状态，同步完成后才进入共享状态。
        val op: UpdateOp<T> = UpdateOp.SubmitList(safeCurrentList())
        PagingEvent.ListStateUpdate(op, mediator.loadStates).fusion(mediator.version)
    }

    private fun safeCurrentList() = when {
        mediator.currentList.isEmpty() -> emptyList()
        else -> mediator.currentList.toSafeMutableList()
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
}