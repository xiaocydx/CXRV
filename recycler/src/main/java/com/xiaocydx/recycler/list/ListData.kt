package com.xiaocydx.recycler.list

import kotlinx.coroutines.flow.Flow

/**
 * 列表更新数据的容器
 *
 * @author xcc
 * @date 2022/3/8
 */
class ListData<T : Any> internal constructor(
    internal val flow: Flow<UpdateOp<T>>,
    internal val mediator: ListMediator<T>
)