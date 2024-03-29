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

@file:OptIn(InternalizationApi::class)

package com.xiaocydx.cxrv.list

import androidx.annotation.MainThread
import com.xiaocydx.cxrv.internal.*
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

/**
 * 列表状态，和视图控制器建立基于[ListOwner]的双向通信
 *
 * 1.在ViewModel下创建`listState`，对外提供`flow`
 * ```
 * class FooViewModel : ViewModel() {
 *     private val listState = ListState<Foo>()
 *     val flow = listState.asFlow()
 * }
 * ```
 *
 * 2.在视图控制器下收集`viewModel.flow`
 * ```
 * class FooActivity : AppCompatActivity() {
 *     private val viewModel: FooViewModel by viewModels()
 *     private val adapter: ListAdapter<Foo, *> = ...
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *          super.onCreate(savedInstanceState)
 *          viewModel.flow
 *                 .onEach(adapter.listCollector)
 *                 .launchIn(lifecycleScope)
 *     }
 * }
 * ```
 *
 * 3.仅在视图控制器活跃期间收集`viewModel.flow`
 * ```
 * class FooActivity : AppCompatActivity() {
 *     private val viewModel: FooViewModel by viewModels()
 *     private val adapter: ListAdapter<Foo, *> = ...
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *          super.onCreate(savedInstanceState)
 *          // 注意：flowWithLifecycle()在onEach(adapter.listCollector)之后调用
 *          viewModel.flow
 *               .onEach(adapter.listCollector)
 *               .flowWithLifecycle(lifecycle)
 *               .launchIn(lifecycleScope)
 *
 *          // 或者直接通过repeatOnLifecycle()进行收集，选中其中一种写法即可
 *          lifecycleScope.launch {
 *              repeatOnLifecycle(Lifecycle.State.STARTED) {
 *                  viewModel.flow.onEach(adapter.listCollector).collect()
 *              }
 *          }
 *     }
 * }
 * ```
 *
 * @author xcc
 * @date 2022/2/17
 */
@InternalizationApi
class ListState<T : Any> : ListOwner<T> {
    private var updatedListeners = InlineList<(UpdateOp<T>) -> Unit>()
    private var dispatchListeners = InlineList<(UpdateOp<T>) -> Unit>()
    private val sourceList: ArrayList<T> = arrayListOf()
    override val currentList: List<T> = sourceList.toUnmodifiableList()
    internal var version: Int = 0
        private set

    @MainThread
    override fun updateList(op: UpdateOp<T>): UpdateResult {
        return updateList(op, dispatch = true)
    }

    @MainThread
    internal fun updateList(op: UpdateOp<T>, dispatch: Boolean): UpdateResult {
        assertMainThread()
        val succeed = when (op) {
            is UpdateOp.SubmitList -> submitList(op.newList)
            is UpdateOp.SetItem -> setItem(op.position, op.item)
            is UpdateOp.SetItems -> setItems(op.position, op.items)
            is UpdateOp.AddItem -> addItem(op.position, op.item)
            is UpdateOp.AddItems -> addItems(op.position, op.items)
            is UpdateOp.RemoveItems -> removeItems(op.position, op.itemCount)
            is UpdateOp.MoveItem -> moveItem(op.fromPosition, op.toPosition)
        }
        if (succeed) {
            version++
            updatedListeners.reverseAccessEach { it(op) }
            if (dispatch) dispatchListeners.reverseAccessEach { it(op) }
        }
        return if (succeed) SuccessResult else FailureResult
    }

    @MainThread
    internal fun addUpdatedListener(listener: (UpdateOp<T>) -> Unit) {
        assertMainThread()
        updatedListeners += listener
    }

    @MainThread
    internal fun removeUpdatedListener(listener: (UpdateOp<T>) -> Unit) {
        assertMainThread()
        updatedListeners -= listener
    }

    @MainThread
    internal fun addDispatchListener(listener: (UpdateOp<T>) -> Unit) {
        assertMainThread()
        dispatchListeners += listener
    }

    @MainThread
    internal fun removeDispatchListener(listener: (UpdateOp<T>) -> Unit) {
        assertMainThread()
        dispatchListeners -= listener
    }

    @MainThread
    private fun submitList(newList: List<T>): Boolean {
        if (sourceList.isNotEmpty()) {
            sourceList.clear()
        }
        if (newList.isEmpty()) return true
        return sourceList.addAll(newList)
    }

    @MainThread
    private fun setItem(position: Int, newItem: T): Boolean {
        if (position !in sourceList.indices) return false
        sourceList[position] = newItem
        return true
    }

    @MainThread
    private fun setItems(position: Int, newItems: List<T>): Boolean {
        if (position !in sourceList.indices) return false
        val end = (position + newItems.size - 1).coerceAtMost(sourceList.lastIndex)
        var index = position
        while (index <= end) {
            sourceList[index] = newItems[index - position]
            index++
        }
        return true
    }

    @MainThread
    private fun addItem(position: Int, item: T): Boolean {
        if (position !in 0..sourceList.size) return false
        if (position == sourceList.size) {
            // ArrayList.add()相比于ArrayList.add(position, item)，
            // position等于size时，不会调用System.arraycopy()。
            sourceList.add(item)
        } else {
            sourceList.add(position, item)
        }
        return true
    }

    @MainThread
    private fun addItems(position: Int, items: List<T>): Boolean {
        if (position !in 0..sourceList.size) return false
        return sourceList.addAll(position, items)
    }

    @MainThread
    @Suppress("UnnecessaryVariable")
    private fun removeItems(position: Int, itemCount: Int): Boolean {
        if (position !in sourceList.indices || itemCount <= 0) return false
        if (itemCount == 1) {
            // ArrayList.removeAt()相比于ArrayList.removeRange()，
            // 移除的是最后一位元素时，不会调用System.arraycopy()。
            sourceList.removeAt(position)
            return true
        }
        val fromIndex = position
        val toIndex = (fromIndex + itemCount).coerceAtMost(sourceList.size)
        // 调用链SubList.clear() -> ArrayList.removeRange()
        sourceList.subList(fromIndex, toIndex).clear()
        return true
    }

    @MainThread
    private fun moveItem(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition !in sourceList.indices
                || toPosition !in sourceList.indices) {
            return false
        }
        // 先remove再add可能会造成两次较大范围的元素搬运
        // val item = sourceList.removeAt(fromPosition)
        // sourceList.add(toPosition, item)
        if (fromPosition < toPosition) {
            // 从小到大，fromPosition两两交换至toPosition
            for (i in fromPosition until toPosition) sourceList.swap(i, i + 1)
        } else {
            // 从大到小，fromPosition两两交换至toPosition
            for (i in fromPosition downTo toPosition + 1) sourceList.swap(i, i - 1)
        }
        return true
    }
}

/**
 * 将[ListState]转换为列表状态数据流
 */
@InternalizationApi
fun <T : Any> ListState<T>.asFlow(): Flow<ListData<T>> = unsafeFlow {
    val mediator = ListMediatorImpl(this@asFlow)
    emit(ListData(mediator.flow, mediator))
}

@PublishedApi
internal class ListMediatorImpl<T : Any>(
    private val listState: ListState<T>
) : ListMediator<T> {
    private var isCollected = false
    override val version: Int
        get() = listState.version
    override val currentList: List<T>
        get() = listState.currentList

    val flow: Flow<ListEvent<T>> = callbackFlow {
        assertMainThread()
        check(!isCollected) { "更新事件流Flow<ListEvent<T>>只能被收集一次" }
        isCollected = true
        // 先发射最新的列表数据和版本号，让下游判断是否需要更新
        send(ListEvent(UpdateOp.SubmitList(safeCurrentList()), version))
        val listener: (UpdateOp<T>) -> Unit = { trySend(ListEvent(it, version)) }
        listState.addDispatchListener(listener)
        awaitClose { listState.removeDispatchListener(listener) }
    }.buffer(UNLIMITED).flowOnMain()

    private fun safeCurrentList() = when {
        currentList.isEmpty() -> emptyList()
        else -> currentList.toSafeMutableList()
    }

    override fun isSameList(other: ListMediator<T>?): Boolean {
        if (other !is ListMediatorImpl) return false
        return listState === other.listState
    }

    override fun updateList(op: UpdateOp<T>) {
        listState.updateList(op)
    }
}