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

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")
@file:OptIn(InternalizationApi::class)

package com.xiaocydx.cxrv.paging

import androidx.annotation.CheckResult
import androidx.annotation.MainThread
import com.xiaocydx.cxrv.internal.InternalizationApi
import com.xiaocydx.cxrv.internal.assertMainThread
import com.xiaocydx.cxrv.internal.flowOnMain
import com.xiaocydx.cxrv.list.ListMediator
import com.xiaocydx.cxrv.list.ListState
import com.xiaocydx.cxrv.list.UpdateOp
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

/**
 * 提供分页列表相关的访问属性、执行函数
 *
 * @author xcc
 * @date 2022/3/8
 */
internal open class PagingListMediator<T : Any>(
    data: PagingData<T>,
    private val listState: ListState<T>
) : PagingMediator by data.mediator, ListMediator<T> {
    override val version: Int
        get() = listState.version
    override val currentList: List<T>
        get() = listState.currentList

    open val flow: Flow<PagingEvent<T>> = channelFlow {
        assertMainThread()
        launch(start = UNDISPATCHED) {
            data.flow.collect { event ->
                if (event is PagingEvent.LoadDataSuccess) {
                    val op = event.toUpdateOp()
                    listState.updateList(op, dispatch = false)
                }
                send(event.fusion(version))
            }
        }

        val listener: (UpdateOp<T>) -> Unit = {
            trySend(PagingEvent.ListStateUpdate(it, loadStates).fusion(version))
        }
        listState.addDispatchListener(listener)
        awaitClose { listState.removeDispatchListener(listener) }
    }.buffer(UNLIMITED).flowOnMain()

    override fun updateList(op: UpdateOp<T>) {
        listState.updateList(op)
    }

    override fun isSameList(other: ListMediator<T>?): Boolean {
        if (other !is PagingListMediator) return false
        return listState === other.listState
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getListMediator() = this as PagingListMediator<T>

    @MainThread
    private fun PagingEvent.LoadDataSuccess<T>.toUpdateOp(): UpdateOp<T> {
        return when (loadType) {
            LoadType.REFRESH -> UpdateOp.SubmitList(data)
            LoadType.APPEND -> UpdateOp.AddItems(currentList.size, data)
        }
    }
}

@CheckResult
internal fun <T : Any> PagingEvent<T>.fusion(version: Int): PagingEvent<T> = when (this) {
    is PagingEvent.LoadStateUpdate -> FusionLoadStateUpdate(loadType, loadStates, version)
    is PagingEvent.LoadDataSuccess -> FusionLoadDataSuccess(data, loadType, loadStates, version)
    is PagingEvent.ListStateUpdate -> FusionListStateUpdate(op, loadStates, version)
}

internal fun PagingEvent<*>.getVersionOrZero(): Int = when (this) {
    is FusionLoadStateUpdate -> version
    is FusionLoadDataSuccess -> version
    is FusionListStateUpdate -> version
    else -> 0
}

private class FusionLoadStateUpdate<T : Any>(
    loadType: LoadType,
    loadStates: LoadStates,
    val version: Int
) : PagingEvent.LoadStateUpdate<T>(loadType, loadStates)

private class FusionLoadDataSuccess<T : Any>(
    data: List<T>,
    loadType: LoadType,
    loadStates: LoadStates,
    val version: Int
) : PagingEvent.LoadDataSuccess<T>(data, loadType, loadStates)

private class FusionListStateUpdate<T : Any>(
    op: UpdateOp<T>,
    loadStates: LoadStates,
    val version: Int
) : PagingEvent.ListStateUpdate<T>(op, loadStates)