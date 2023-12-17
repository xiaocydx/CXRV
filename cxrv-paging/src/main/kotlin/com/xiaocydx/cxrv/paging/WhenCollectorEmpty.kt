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

import kotlinx.coroutines.flow.Flow

/**
 * [PagingStateFlow]和[PagingSharedFlow]的配置属性
 *
 * @author xcc
 * @date 2023/12/17
 */
internal enum class WhenCollectorEmpty {
    /**
     * 当`collector`数量为`0`时，不做任何处理
     */
    NONE,

    /**
     * 当`collector`数量为`0`时，取消收集`upstream`，关闭[Flow]
     */
    CLOSE,

    /**
     * 当`collector`数量为`0`时，取消收集`upstream`，大于`0`时重新收集`upstream`
     */
    REPEAT
}

/**
 * [PagingStateFlow]和[PagingSharedFlow]的关闭标志
 */
internal val Closed = Any()