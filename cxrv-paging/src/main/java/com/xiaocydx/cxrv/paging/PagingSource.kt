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
 * 分页数据源
 *
 * @author xcc
 * @date 2021/9/13
 */
fun interface PagingSource<K: Any, T : Any> {

    /**
     * 加载页面结果
     *
     * **注意**：内部逻辑调用[PagingSource.load]时，也会进行异常捕获。
     * ```
     * class FooPagingSource : PagingSource<Int, Foo>() {
     *
     *      suspend fun load(params: LoadParams<Int, Foo>): LoadResult<Int, Foo> {
     *          return try {
     *              ...
     *              LoadResult.Success(data, nextKey)
     *          } catch (exception: Throwable) {
     *              LoadResult.Failure(exception)
     *          }
     *      }
     * }
     * ```
     */
    suspend fun load(params: LoadParams<K>): LoadResult<K, T>
}

/**
 * [PagingSource.load]的加载参数
 */
sealed class LoadParams<K: Any>(
    /**
     * 页面对应的key
     */
    open val key: K,

    /**
     * 页面大小
     *
     * 若[pageSize] == [PagingConfig.Invalid]，则表示调用者不使用[LoadParams.pageSize]。
     */
    open val pageSize: Int
) {

    /**
     * 刷新加载的参数
     *
     * [pageSize]此时的值是[PagingConfig.initPageSize]。
     */
    data class Refresh<K: Any>(
        override val key: K,
        override val pageSize: Int
    ) : LoadParams<K>(key, pageSize)

    /**
     * 在末尾加载的参数
     *
     * [pageSize]此时的值是[PagingConfig.pageSize]。
     */
    data class Append<K: Any>(
        override val key: K,
        override val pageSize: Int
    ) : LoadParams<K>(key, pageSize)

    companion object {
        fun <K: Any> create(
            loadType: LoadType,
            key: K,
            pageSize: Int
        ): LoadParams<K> = when (loadType) {
            LoadType.REFRESH -> Refresh(key, pageSize)
            LoadType.APPEND -> Append(key, pageSize)
        }
    }
}

/**
 * [PagingSource.load]的加载结果
 */
sealed class LoadResult<K, T : Any> {

    /**
     * 加载成功的结果
     *
     * [nextKey]为加载下一页的key，若[nextKey] == `null`，则表示加载完全
     */
    data class Success<K, T : Any>(
        val data: List<T>,
        val nextKey: K?,
    ) : LoadResult<K, T>()

    /**
     * 加载失败的结果
     */
    data class Failure<K, T : Any>(
        val exception: Throwable
    ) : LoadResult<K, T>()
}