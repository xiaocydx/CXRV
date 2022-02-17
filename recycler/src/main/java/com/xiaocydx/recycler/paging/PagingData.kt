package com.xiaocydx.recycler.paging

import kotlinx.coroutines.flow.Flow

/**
 * 分页数据的容器
 *
 * @author xcc
 * @date 2021/9/14
 */
data class PagingData<T : Any> internal constructor(
    internal val flow: Flow<PagingEvent<T>>,
    internal val mediator: PagingMediator<T>
)