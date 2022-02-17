package com.xiaocydx.recycler.paging

import com.xiaocydx.recycler.list.UpdateOp

/**
 * 分页事件
 *
 * [PagingData]触发[PagingSource]加载，加载过程会发送[PagingEvent]，
 * 视图层接收[PagingEvent]，根据事件类型执行对应的更新操作。
 *
 * @author xcc
 * @date 2021/9/13
 */
internal sealed class PagingEvent<T : Any>(
    /**
     * 加载类型，为null表示与加载流程无关，只是携带[loadStates]的事件
     */
    open val loadType: LoadType?,

    /**
     * 加载状态集合
     */
    open val loadStates: LoadStates
) {

    /**
     * 加载状态更新事件
     *
     * 视图层接收到该事件，需要更新加载视图的显示状态。
     */
    data class LoadUpdate<T : Any>(
        override val loadType: LoadType?,
        override val loadStates: LoadStates
    ) : PagingEvent<T>(loadType, loadStates)

    /**
     * 列表更新事件
     *
     * 视图层接收到该事件，需要更新数据源并刷新列表。
     */
    data class ListUpdate<T : Any>(
        override val loadType: LoadType?,
        override val loadStates: LoadStates,
        /**
         * 更新操作
         */
        val op: UpdateOp<T>
    ) : PagingEvent<T>(loadType, loadStates)
}