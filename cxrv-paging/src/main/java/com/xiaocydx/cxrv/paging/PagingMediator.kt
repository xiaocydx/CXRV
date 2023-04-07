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

/**
 * 提供分页相关的访问属性、执行函数
 *
 * @author xcc
 * @date 2021/9/15
 */
interface PagingMediator {
    /**
     * 加载状态集合
     */
    val loadStates: LoadStates

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
}