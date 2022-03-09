package com.xiaocydx.recycler.paging

import kotlinx.coroutines.flow.Flow

/**
 * 分页数据的容器
 *
 * @author xcc
 * @date 2021/9/14
 */
class PagingData<T : Any>
@PublishedApi internal constructor(
    @PublishedApi
    internal val flow: Flow<PagingEvent<T>>,
    @PublishedApi
    internal val mediator: PagingMediator
)