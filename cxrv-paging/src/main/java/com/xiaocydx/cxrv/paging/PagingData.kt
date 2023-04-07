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
 * 分页数据的容器
 *
 * @author xcc
 * @date 2021/9/14
 */
class PagingData<out T : Any>
@PublishedApi internal constructor(
    @PublishedApi
    internal val flow: Flow<PagingEvent<T>>,
    @PublishedApi
    internal val mediator: PagingMediator
)