package com.xiaocydx.cxrv.paging

import androidx.annotation.CheckResult
import androidx.annotation.MainThread
import com.xiaocydx.cxrv.internal.flowOnMain
import com.xiaocydx.cxrv.list.ListMediator
import com.xiaocydx.cxrv.list.ListState
import com.xiaocydx.cxrv.list.UpdateOp
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * 提供分页列表相关的访问属性、执行函数
 *
 * @author xcc
 * @date 2022/3/8
 */
internal class PagingListMediator<T : Any>(
    data: PagingData<T>,
    private val listState: ListState<T>
) : ListMediator<T>, PagingMediator by data.mediator {
    override val version: Int
        get() = listState.version
    override val currentList: List<T>
        get() = listState.currentList

    val flow: Flow<PagingEvent<T>> = callbackFlow {
        launch {
            data.flow.collect { event ->
                if (event is PagingEvent.LoadDataSuccess) {
                    updateList(event.toUpdateOp())
                }
                send(event.fusion(version))
            }
        }

        val listener: (UpdateOp<T>) -> Unit = {
            trySend(PagingEvent.ListStateUpdate(it, loadStates).fusion(version))
        }
        listState.addUpdatedListener(listener)
        awaitClose { listState.removeUpdatedListener(listener) }
    }.buffer(UNLIMITED).flowOnMain()

    override fun updateList(op: UpdateOp<T>) {
        listState.updateList(op, dispatch = false)
    }

    @MainThread
    private fun PagingEvent.LoadDataSuccess<T>.toUpdateOp(): UpdateOp<T> {
        return when (loadType) {
            LoadType.REFRESH -> UpdateOp.SubmitList(data)
            LoadType.APPEND -> UpdateOp.AddItems(currentList.size, data)
        }
    }
}

@Suppress("UNCHECKED_CAST")
internal fun <T : Any> PagingMediator.asListMediator(): PagingListMediator<T>? {
    return this as? PagingListMediator<T>
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