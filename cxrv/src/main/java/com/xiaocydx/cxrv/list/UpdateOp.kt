package com.xiaocydx.cxrv.list

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume

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
 * 执行[UpdateOp]产生挂起，通过[deferred]等待更新结果
 */
internal class DeferredResult(
    private val deferred: Deferred<Unit>
) : UpdateResult, CompleteCompat {
    override val isComplete: Boolean
        get() = deferred.isCompleted

    override suspend fun await(): Unit = deferred.await()

    override fun invokeOnCompletion(complete: ((exception: Throwable?) -> Unit)?) {
        complete?.let(deferred::invokeOnCompletion)
    }
}

/**
 * 实现逻辑很粗糙，但对单元测试来说已经足够
 */
@VisibleForTesting
internal class TestUpdateResult(
    dispatcher: CoroutineDispatcher,
    private val block: () -> UpdateResult
) : UpdateResult, CompleteCompat {
    @Volatile private var result: UpdateResult? = null
    @Volatile private var continuation: Continuation<Unit>? = null
    @Volatile private var complete: ((exception: Throwable?) -> Unit)? = null
    override val isComplete: Boolean
        get() = result?.isComplete ?: false

    init {
        dispatcher.dispatch(EmptyCoroutineContext) {
            result = block()
            continuation?.resume(Unit)
            complete?.invoke(null)
        }
    }

    override suspend fun await() {
        assert(result == null)
        suspendCancellableCoroutine { continuation = it }
        result?.await()
    }

    override fun invokeOnCompletion(complete: ((exception: Throwable?) -> Unit)?) {
        assert(result == null)
        this.complete = complete
    }
}

/**
 * 对单元测试跳过主线程断言
 */
@VisibleForTesting
internal abstract class TestMainCoroutineDispatcher : MainCoroutineDispatcher()