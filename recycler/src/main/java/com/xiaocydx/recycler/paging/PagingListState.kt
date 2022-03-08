package com.xiaocydx.recycler.paging

import com.xiaocydx.recycler.list.ListOwner
import com.xiaocydx.recycler.list.ListUpdater
import com.xiaocydx.recycler.list.UpdateOp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * 分页列表状态
 *
 * // FIXME: 2022/3/8 考虑刷新加载期间，没有数据的临界情况
 *
 * @author xcc
 * @date 2022/3/8
 */
class PagingListState<T : Any> : ListOwner<T> {
    private val updater = ListUpdater<T>(mutableListOf())
    override val currentList: List<T>
        get() = updater.currentList

    override fun updateList(op: UpdateOp<T>) {
        updater.updateList(op, dispatch = true)
    }

    @PublishedApi
    internal fun transform(data: PagingData<T>): PagingData<T> {
        val mediator = PagingListMediatorImp(data, updater)
        return PagingData(mediator.flow, mediator)
    }

    private class PagingListMediatorImp<T : Any>(
        data: PagingData<T>,
        private val updater: ListUpdater<T>
    ) : PagingListMediator<T>, PagingMediator by data.mediator {
        override var updateVersion: Int = 0
        override val currentList: List<T>
            get() = updater.currentList

        val flow: Flow<PagingEvent<T>> = safeChannelFlow { channel ->
            updater.setUpdatedListener { op ->
                // FIXME: 2022/3/8 updateVersion自增的位置是否合理？
                updateVersion++
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
                if (event !is PagingEvent.LoadDataSuccess) {
                    channel.send(event)
                    return@collect
                }
                updater.updateList(
                    op = when (event.loadType) {
                        LoadType.REFRESH -> UpdateOp.SubmitList(event.data)
                        LoadType.APPEND -> UpdateOp.AddItems(currentList.size, event.data)
                    },
                    dispatch = true
                )
            }
        }

        override fun updateList(op: UpdateOp<T>) {
            updater.updateList(op, dispatch = false)
        }
    }
}

@Suppress("UNCHECKED_CAST")
internal fun <T : Any> PagingMediator.asListMediator(): PagingListMediator<T>? {
    return this as? PagingListMediator<T>
}

/**
 * 提供分页列表状态相关的访问属性、执行函数
 */
internal interface PagingListMediator<T : Any> : PagingMediator {
    /**
     * 列表更新版本号
     */
    val updateVersion: Int

    /**
     * 当前列表
     */
    val currentList: List<T>

    /**
     * 更新列表
     */
    fun updateList(op: UpdateOp<T>)
}