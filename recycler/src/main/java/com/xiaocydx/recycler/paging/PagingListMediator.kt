package com.xiaocydx.recycler.paging

import androidx.annotation.MainThread
import com.xiaocydx.recycler.extension.flowOnMain
import com.xiaocydx.recycler.list.ListMediator
import com.xiaocydx.recycler.list.ListState
import com.xiaocydx.recycler.list.UpdateOp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.job
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

    val flow: Flow<PagingEvent<T>> = safeChannelFlow<PagingEvent<T>> { channel ->
        listState.setUpdatedListener(this) { op ->
            val event: PagingEvent<T> = PagingEvent.ListStateUpdate(op, loadStates)
            channel.trySend(event)
                .takeIf { result ->
                    result.isFailure && !result.isClosed
                }?.let {
                    // 同步发送失败，原因可能是buffer满了，
                    // 启动协程，调用send挂起等待buffer空位，
                    // 确保更新操作事件不会丢失。
                    launch { channel.send(event) }
                }
        }

        data.flow.collect { event ->
            if (event is PagingEvent.LoadDataSuccess) {
                updateList(event.toUpdateOp())
            }
            channel.send(event)
        }
    }.flowOnMain()

    override fun updateList(op: UpdateOp<T>) {
        listState.updateList(op, dispatch = false)
    }

    private fun ListState<T>.setUpdatedListener(
        scope: CoroutineScope,
        listener: (UpdateOp<T>) -> Unit
    ) {
        addUpdatedListener(listener)
        scope.coroutineContext.job.invokeOnCompletion {
            removeUpdatedListener(listener)
        }
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