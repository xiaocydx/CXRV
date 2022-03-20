package com.xiaocydx.recycler.paging

import androidx.annotation.MainThread
import com.xiaocydx.recycler.extension.flowOnMain
import com.xiaocydx.recycler.list.ListMediator
import com.xiaocydx.recycler.list.ListState
import com.xiaocydx.recycler.list.UpdateOp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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
    override val updateVersion: Int
        get() = listState.updateVersion
    override val currentList: List<T>
        get() = listState.currentList

    val flow: Flow<PagingEvent<T>> = flow {
        coroutineScope {
            val channel = Channel<PagingEvent<T>>(UNLIMITED)
            launch {
                data.flow.collect { event ->
                    if (event is PagingEvent.LoadDataSuccess) {
                        updateList(event.toUpdateOp())
                    }
                    channel.send(event)
                }
            }

            val listener: (UpdateOp<T>) -> Unit = {
                channel.trySend(PagingEvent.ListStateUpdate(it, loadStates))
            }
            listState.addUpdatedListener(listener)
            try {
                emitAll(channel)
            } finally {
                listState.removeUpdatedListener(listener)
            }
        }
    }.flowOnMain()

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