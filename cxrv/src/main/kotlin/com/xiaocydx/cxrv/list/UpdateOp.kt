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

import kotlinx.coroutines.Deferred
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

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
    val isCompleted: Boolean

    /**
     * 若未更新完成，则挂起等待更新完成，响应调用处协程的取消
     *
     * @return `true`-更新成功，`false`-更新失败
     */
    suspend fun await(): Boolean
}

/**
 * 获取结果
 *
 * 1. `null`-未更新完成，此时`isCompleted = false`
 * 2. `true`-更新成功，此时`isCompleted = true`
 * 3. `false`-更新失败，此时`isCompleted = true`
 */
fun UpdateResult.get(): Boolean? {
    if (!isCompleted) return null
    return await(this, NoOpContinuation) as? Boolean
}

@Suppress("UNCHECKED_CAST")
private val await = UpdateResult::await as Function2<UpdateResult, Continuation<Boolean>, *>

private object NoOpContinuation : Continuation<Any?> {
    override val context: CoroutineContext = EmptyCoroutineContext
    override fun resumeWith(result: Result<Any?>) = Unit
}

/**
 * 兼容`complete`参数的更新结果
 */
internal interface CompleteCompat {
    fun invokeOnCompletion(complete: ((exception: Throwable?) -> Unit)? = null)
}

/**
 * 执行[UpdateOp]未产生挂起，更新成功
 */
internal object SuccessResult : UpdateResult {
    override val isCompleted = true
    override suspend fun await() = true
}

/**
 * 执行[UpdateOp]未产生挂起，更新失败
 */
internal object FailureResult : UpdateResult {
    override val isCompleted = true
    override suspend fun await() = false
}

/**
 * 执行[UpdateOp]产生挂起，通过[deferred]等待更新结果
 */
internal class DeferredResult(
    private val deferred: Deferred<Boolean>
) : UpdateResult, CompleteCompat {
    override val isCompleted: Boolean
        get() = deferred.isCompleted

    override suspend fun await(): Boolean {
        // 不直接调用deferred.await()返回结果，是为了规避deferred被取消抛出CancellationException,
        // deferred被取消，deferred.join()只会正常完成，并且deferred.join()能响应当前协程的取消。
        deferred.join()
        if (deferred.isCancelled) return false
        return deferred.await()
    }

    override fun invokeOnCompletion(complete: ((exception: Throwable?) -> Unit)?) {
        complete?.let(deferred::invokeOnCompletion)
    }
}