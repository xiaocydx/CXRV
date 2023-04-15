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
 * [PagingSource]的加载类型
 *
 * ### [LoadType]和[LoadParams]
 * [PagingSource.load]的加载参数通过[LoadParams.create]创建而来。
 *
 * ### [LoadType]和[LoadState]
 * [PagingEvent]包含[LoadType]和与之对应的[LoadState]，
 * 视图控制器接收[PagingEvent]，根据[LoadType]和[LoadState]更新加载视图。
 *
 * @author xcc
 * @date 2021/9/13
 */
enum class LoadType {
    /**
     * [PagingSource]的刷新加载
     *
     * 表示初始化加载或者主动触发的刷新加载
     */
    REFRESH,

    /**
     * [PagingSource]的末尾加载
     */
    APPEND
}