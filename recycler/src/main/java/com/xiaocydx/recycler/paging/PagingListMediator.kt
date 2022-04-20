package com.xiaocydx.recycler.paging

import androidx.annotation.MainThread
import com.xiaocydx.recycler.extension.flowOnMain
import com.xiaocydx.recycler.list.ListMediator
import com.xiaocydx.recycler.list.ListState
import com.xiaocydx.recycler.list.UpdateOp
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
                send(event)
            }
        }

        val listener: (UpdateOp<T>) -> Unit = {
            trySend(PagingEvent.ListStateUpdate(it, loadStates))
        }
        listState.addUpdatedListener(listener)
        awaitClose {
            listState.removeUpdatedListener(listener)
        }
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