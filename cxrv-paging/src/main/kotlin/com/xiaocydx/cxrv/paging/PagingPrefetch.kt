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

import androidx.annotation.IntRange

/**
 * 分页数据的预取策略
 *
 * @author xcc
 * @date 2022/11/30
 */
sealed class PagingPrefetch {
    /**
     * 不需要预取分页数据
     */
    object None : PagingPrefetch()

    /**
     * 依靠RecyclerView的预取机制预取分页数据
     *
     * **注意**：对RecyclerView禁用预取机制（默认启用），该策略会无效。
     */
    object Default : PagingPrefetch()

    /**
     * 在[Default]的基础上，提前[value]个item预取分页数据
     *
     * **注意**：对RecyclerView禁用预取机制（默认启用），该策略仍有效。
     */
    data class ItemCount(@IntRange(from = 1) val value: Int) : PagingPrefetch()
}