package com.xiaocydx.recycler.paging

import com.xiaocydx.recycler.list.UpdateOp

/**
 * 分页事件
 *
 * [PagingData]触发[PagingSource]加载，加载过程会发送[PagingEvent]，
 * 视图控制器接收[PagingEvent]，根据事件类型执行对应的更新操作。
 *
 * @author xcc
 * @date 2021/9/13
 */
sealed class PagingEvent<T : Any>(
    open val loadType: LoadType?,
    open val loadStates: LoadStates
) {
    /**
     * 加载状态更新事件
     */
    data class LoadStateUpdate<T : Any>(
        override val loadType: LoadType?,
        override val loadStates: LoadStates
    ) : PagingEvent<T>(loadType, loadStates)

    /**
     * 加载数据成功事件
     */
    data class LoadDataSuccess<T : Any>(
        val data: List<T>,
        override val loadType: LoadType,
        override val loadStates: LoadStates
    ) : PagingEvent<T>(loadType, loadStates)

    /**
     * 列表状态更新事件
     */
    class ListStateUpdate<T : Any> internal constructor(
        internal val op: UpdateOp<T>,
        override val loadStates: LoadStates
    ) : PagingEvent<T>(loadType = null, loadStates)
}