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

package com.xiaocydx.cxrv.list

import kotlinx.coroutines.Job

/**
 * 列表更新操作
 *
 * @author xcc
 * @date 2021/11/2
 */
sealed class UpdateOp<in T : Any> {
    internal data class SubmitList<T : Any>(val newList: List<T>) : UpdateOp<T>()

    internal data class SetItem<T : Any>(val position: Int, val item: T) : UpdateOp<T>()

    internal data class SetItems<T : Any>(val position: Int, val items: List<T>) : UpdateOp<T>()

    internal data class AddItem<T : Any>(val position: Int, val item: T) : UpdateOp<T>()

    internal data class AddItems<T : Any>(val position: Int, val items: List<T>) : UpdateOp<T>()

    internal data class RemoveItems<T : Any>(val position: Int, val itemCount: Int) : UpdateOp<T>()

    internal data class MoveItem<T : Any>(val fromPosition: Int, val toPosition: Int) : UpdateOp<T>()
}

/**
 * [UpdateOp]的更新结果
 */
interface UpdateResult {
    /**
     * 是否更新完成
     */
    val isComplete: Boolean

    /**
     * 若未更新完成，则挂起等待更新完成，响应调用处协程的取消
     */
    suspend fun await()
}

/**
 * 兼容`complete`参数的更新结果
 */
internal interface CompleteCompat {
    fun invokeOnCompletion(complete: ((exception: Throwable?) -> Unit)? = null)
}

/**
 * 执行[UpdateOp]未产生挂起，更新完成
 */
internal object CompleteResult : UpdateResult {
    override val isComplete: Boolean = true
    override suspend fun await() = Unit
}

/**
 * 执行[UpdateOp]产生挂起，通过[job]等待更新完成
 */
internal class DeferredResult(
    private val job: Job
) : UpdateResult, CompleteCompat {
    override val isComplete: Boolean
        get() = job.isCompleted

    override suspend fun await(): Unit = job.join()

    override fun invokeOnCompletion(complete: ((exception: Throwable?) -> Unit)?) {
        complete?.let(job::invokeOnCompletion)
    }
}