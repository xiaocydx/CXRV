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

import androidx.annotation.MainThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val LIST_COLLECTOR_KEY = "com.xiaocydx.cxrv.list.LIST_COLLECTOR_KEY"

/**
 * 列表数据收集器，负责收集指定流的[ListData]
 */
val <T : Any> ListAdapter<T, *>.listCollector: ListCollector<T>
    get() {
        var collector =
                getTag<ListCollector<T>>(LIST_COLLECTOR_KEY)
        if (collector == null) {
            collector = ListCollector(this)
            setTag(LIST_COLLECTOR_KEY, collector)
        }
        return collector
    }

/**
 * `Flow<ListData<T>>`的值发射给[listCollector]，是一种简化写法
 *
 * ```
 * val adapter: ListAdapter<Foo, *> = ...
 * flow.onEach { adapter.listCollector.emit(it) }
 *
 * // 简化上面的写法
 * flow.onEach(adapter.listCollector)
 * ```
 */
fun <T : Any> Flow<ListData<T>>.onEach(
    collector: ListCollector<T>
): Flow<ListData<T>> = onEach(collector::emit)

/**
 * `Flow<ListData<T>>`的值发射给[listCollector]，是一种简化写法
 *
 * ```
 * val adapter: ListAdapter<Foo, *> = ...
 * flow.onEach { adapter.listCollector.emit(it) }
 *
 * // 简化上面的写法
 * flow.onEach(adapter)
 * ```
 */
@Deprecated(
    message = "虽然简化了代码，但是降低了可读性",
    replaceWith = ReplaceWith(expression = "onEach(adapter.listCollector)")
)
fun <T : Any> Flow<ListData<T>>.onEach(
    adapter: ListAdapter<T, *>
): Flow<ListData<T>> = onEach(adapter.listCollector::emit)

/**
 * `Flow<ListData<T>>`的值发射给[listCollector]，是一种简化写法
 *
 * ```
 * val adapter: ListAdapter<Foo, *> = ...
 * flow.collect { value ->
 *     adapter.listCollector.emit(value)
 * }
 *
 * // 简化上面的写法
 * flow.collect(adapter)
 * ```
 */
@Deprecated(
    message = "虽然简化了代码，但是降低了可读性",
    replaceWith = ReplaceWith(expression = "collect(adapter.listCollector)")
)
suspend fun <T : Any> Flow<ListData<T>>.collect(
    adapter: ListAdapter<T, *>
): Unit = collect(adapter.listCollector)

/**
 * 列表数据收集器，负责收集指定流的[ListData]
 */
class ListCollector<T : Any> internal constructor(
    private val adapter: ListAdapter<T, *>,
    private val mainDispatcher: MainCoroutineDispatcher = Dispatchers.Main.immediate
) : FlowCollector<ListData<T>> {
    private var version = 0
    private var mediator: ListMediator<T>? = null

    init {
        adapter.addListExecuteListener { op ->
            // 先得到期望的version，用于拦截同步发送的更新事件
            version++
            mediator?.updateList(op)
            // 再得到实际的version，确保不会因为失败而增加version
            version = mediator?.version ?: 0
        }
    }

    override suspend fun emit(
        value: ListData<T>
    ) = withContext(mainDispatcher.immediate) {
        setMediator(value.mediator)

        // 使用Channel的原因：
        // 当op是SubmitList时，adapter会清除更新队列，并取消差异计算，
        // 正在等待的result会结束挂起，保存在Channel的result不会挂起，
        // for循环快速处理完Channel的result后，挂起等待新的result完成。
        /*
           listState.submitList(newList1) // op1
           listState.addItem(0, item1)    // op2
           listState.addItem(0, item2)    // op3
           listState.submitList(newList2) // 清除op2和op3，取消op1
         */
        val channel = Channel<ListEventResult>(UNLIMITED)
        launch {
            value.flow.collect { event ->
                val newVersion = event.version
                if (newVersion <= version) return@collect
                val result = adapter.updateList(event.op, dispatch = false)
                channel.send(ListEventResult(newVersion, result))
            }
            channel.close()
        }

        for (result in channel) {
            if (!result.await()) continue
            val newVersion = result.newVersion
            if (newVersion <= version) continue
            version = newVersion
        }
    }

    @MainThread
    private fun setMediator(mediator: ListMediator<T>?) {
        if (this.mediator === mediator) return
        val previousMediator = this.mediator
        val currentMediator = mediator
        if (previousMediator == null || currentMediator == null
                || !previousMediator.isSameList(currentMediator)) {
            version = 0
        }
        this.mediator = mediator
    }

    private class ListEventResult(
        val newVersion: Int,
        private val result: UpdateResult
    ) {
        suspend fun await() = result.await()
    }
}