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

import com.xiaocydx.cxrv.paging.LoadType.APPEND
import com.xiaocydx.cxrv.paging.LoadType.REFRESH

/**
 * 提供分页相关的访问属性、执行函数
 *
 * @author xcc
 * @date 2021/9/15
 */
internal interface PagingMediator {
    /**
     * 加载状态集合
     */
    val loadStates: LoadStates

    /**
     * 当[REFRESH]加载开始时，若该属性为`true`，则列表滚动到首位。
     */
    val refreshStartScrollToFirst: Boolean

    /**
     * 当[APPEND]加载失败时，若该属性为`true`，则自动重试[APPEND]加载，
     * 自动重试的含义是不需要点击重试，时机是再次满足[APPEND]加载的条件，
     * 例如滚动列表，若最后一个item再次可视，则自动重试。
     */
    val appendFailureAutToRetry: Boolean

    /**
     * 末尾加载的预取策略
     */
    val appendPrefetch: PagingPrefetch

    /**
     * 刷新加载，获取新的[PagingData]
     */
    fun refresh()

    /**
     * 末尾加载，该函数会对加载状态做判断，避免冗余请求
     */
    fun append()

    /**
     * 重新加载，该函数会对加载状态做判断，避免冗余请求
     */
    fun retry()

    /**
     * 从当前[PagingMediator]获取[PagingListMediator]，仅由[PagingCollector]调用，
     * 当前[PagingMediator]可能是转换操作符的委托类，例如[PagingConfigMediator]。
     *
     * **注意**：该函数仅被[PagingListMediator]重写，转换操作符的委托类不进行重写，
     * [PagingListMediator]的构建流程确保[T]类型跟[PagingCollector]的[T]类型一致。
     */
    fun <T : Any> getListMediator(): PagingListMediator<T>? = null
}