/*
 * Copyright 2022 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaocydx.cxrv.paging

import com.xiaocydx.cxrv.list.UpdateOp

/**
 * 分页事件
 *
 * [PagingData]触发[PagingSource]加载，加载过程会发送[PagingEvent]，
 * 视图控制器接收[PagingEvent]，根据事件类型执行对应的更新操作。
 *
 * @author xcc
 * @date 2021/9/13
 */
sealed class PagingEvent<out T : Any>(val loadStates: LoadStates) {
    /**
     * 加载状态更新事件
     */
    open class LoadStateUpdate<T : Any>
    @PublishedApi internal constructor(
        val loadType: LoadType,
        loadStates: LoadStates
    ) : PagingEvent<T>(loadStates)

    /**
     * 加载数据成功事件
     */
    open class LoadDataSuccess<T : Any>
    @PublishedApi internal constructor(
        val data: List<T>,
        val loadType: LoadType,
        loadStates: LoadStates
    ) : PagingEvent<T>(loadStates)

    /**
     * 列表状态更新事件
     */
    open class ListStateUpdate<T : Any>
    @PublishedApi internal constructor(
        internal val op: UpdateOp<T>,
        loadStates: LoadStates
    ) : PagingEvent<T>(loadStates)
}

val PagingEvent<*>.loadType: LoadType?
    get() = when (this) {
        is PagingEvent.LoadStateUpdate -> loadType
        is PagingEvent.LoadDataSuccess -> loadType
        is PagingEvent.ListStateUpdate -> null
    }