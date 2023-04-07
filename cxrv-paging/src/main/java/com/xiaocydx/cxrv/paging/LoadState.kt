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
 * 加载状态
 *
 * ### [LoadType]和[LoadState]
 * [PagingEvent]包含[LoadType]和与之对应的[LoadState]，
 * 视图控制器接收[PagingEvent]，根据[LoadType]和[LoadState]更新加载视图的显示状态。
 *
 * @author xcc
 * @date 2021/9/13
 */
sealed class LoadState {
    /**
     * 加载未完成状态，表示加载还未开始
     */
    object Incomplete : LoadState()

    /**
     * 加载中状态，对应[PagingSource.load]被调用，等待加载结果
     */
    object Loading : LoadState()

    /**
     * 加载成功状态，对应[PagingSource.load]返回[LoadResult.Success]结果
     */
    data class Success(val isFully: Boolean) : LoadState()

    /**
     * 加载失败状态，对应[PagingSource.load]返回[LoadResult.Failure]结果
     */
    data class Failure(val exception: Throwable) : LoadState()
}

/**
 * 是否加载未完成
 */
val LoadState.isIncomplete: Boolean
    get() = this is LoadState.Incomplete

/**
 * 是否加载中
 */
val LoadState.isLoading: Boolean
    get() = this is LoadState.Loading

/**
 * 是否加载成功
 */
val LoadState.isSuccess: Boolean
    get() = this is LoadState.Success

/**
 * 是否加载失败
 */
val LoadState.isFailure: Boolean
    get() = this is LoadState.Failure

/**
 * 是否加载完成，加载成功或加载失败即算加载完成
 */
val LoadState.isComplete: Boolean
    get() = isSuccess || isFailure

/**
 * 是否加载完全，没有更多数据
 */
val LoadState.isFully: Boolean
    get() = (this as? LoadState.Success)?.isFully ?: false

/**
 * 加载失败的异常，返回`null`表示当前没有加载失败
 */
val LoadState.exception: Throwable?
    get() = (this as? LoadState.Failure)?.exception

/**
 * 若当前状态为加载成功，则执行[action]
 */
inline fun LoadState.onSuccess(action: LoadState.Success.() -> Unit): LoadState {
    (this as? LoadState.Success)?.apply(action)
    return this
}

/**
 * 若当前状态为加载失败，则执行[action]
 */
inline fun LoadState.onFailure(action: LoadState.Failure.() -> Unit): LoadState {
    (this as? LoadState.Failure)?.apply(action)
    return this
}