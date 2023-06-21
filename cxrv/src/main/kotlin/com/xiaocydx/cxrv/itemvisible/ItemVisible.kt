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

package com.xiaocydx.cxrv.itemvisible

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.xiaocydx.cxrv.itemvisible.VisibleTarget.FIRST_ITEM
import com.xiaocydx.cxrv.itemvisible.VisibleTarget.FIRST_ITEM_COMPLETELY
import com.xiaocydx.cxrv.itemvisible.VisibleTarget.LAST_ITEM
import com.xiaocydx.cxrv.itemvisible.VisibleTarget.LAST_ITEM_COMPLETELY
import com.xiaocydx.cxrv.layout.runExtensionsPrimitive
import com.xiaocydx.cxrv.list.Disposable

/**
 * 第一个item是否可视
 *
 * 1. 默认仅支持[LinearLayoutManager]、[StaggeredGridLayoutManager]。
 * 2. 监听RecyclerView滚动，频繁判断item是否可视的场景，
 * 可以使用[ItemVisibleHelper]，或者使用[doOnFirstItemVisible]。
 */
val RecyclerView.isFirstItemVisible: Boolean
    get() = findFirstVisibleItemPosition() == 0

/**
 * 第一个item是否完全可视
 *
 * 1. 默认仅支持[LinearLayoutManager]、[StaggeredGridLayoutManager]。
 * 2. 监听RecyclerView滚动，频繁判断item是否可视的场景，
 * 可以使用[ItemVisibleHelper]，或者使用[doOnFirstItemCompletelyVisible]。
 */
val RecyclerView.isFirstItemCompletelyVisible: Boolean
    get() = findFirstCompletelyVisibleItemPosition() == 0

/**
 * 最后一个item是否可视
 *
 * 1. 默认仅支持[LinearLayoutManager]、[StaggeredGridLayoutManager]。
 * 2. 监听RecyclerView滚动，频繁判断item是否可视的场景，
 * 可以使用[ItemVisibleHelper]，或者使用[doOnLastItemVisible]。
 */
val RecyclerView.isLastItemVisible: Boolean
    get() = when (val lm: LayoutManager? = layoutManager) {
        null -> false
        else -> findLastVisibleItemPosition() == lm.itemCount - 1
    }

/**
 * 最后一个item是否完全可视
 *
 * 1. 默认仅支持[LinearLayoutManager]、[StaggeredGridLayoutManager]。
 * 2. 监听RecyclerView滚动，频繁判断item是否可视的场景，
 * 可以使用[ItemVisibleHelper]，或者使用[doOnLastItemCompletelyVisible]。
 */
val RecyclerView.isLastItemCompletelyVisible: Boolean
    get() = when (val lm: LayoutManager? = layoutManager) {
        null -> false
        else -> findLastCompletelyVisibleItemPosition() == lm.itemCount - 1
    }

/**
 * 查找第一个可视item的position
 *
 * 1. 默认仅支持[LinearLayoutManager]、[StaggeredGridLayoutManager]。
 * 2. 监听RecyclerView滚动，频繁获取可视item位置的场景，可以使用[ItemVisibleHelper]。
 */
fun RecyclerView.findFirstVisibleItemPosition(): Int = when (val lm: LayoutManager? = layoutManager) {
    null -> NO_POSITION
    is LinearLayoutManager -> lm.findFirstVisibleItemPosition()
    is StaggeredGridLayoutManager -> lm.findFirstVisibleItemPosition()
    else -> lm.runExtensionsPrimitive(NO_POSITION) { findFirstVisibleItemPosition(lm) }
}

/**
 * 查找第一个完全可视item的position
 *
 * 1. 默认仅支持[LinearLayoutManager]、[StaggeredGridLayoutManager]。
 * 2. 监听RecyclerView滚动，频繁获取可视item位置的场景，可以使用[ItemVisibleHelper]。
 */
fun RecyclerView.findFirstCompletelyVisibleItemPosition(): Int = when (val lm: LayoutManager? = layoutManager) {
    null -> NO_POSITION
    is LinearLayoutManager -> lm.findFirstCompletelyVisibleItemPosition()
    is StaggeredGridLayoutManager -> lm.findFirstCompletelyVisibleItemPosition()
    else -> lm.runExtensionsPrimitive(NO_POSITION) { findFirstCompletelyVisibleItemPosition(lm) }
}

/**
 * 查找最后一个可视item的position
 *
 * 1. 默认仅支持[LinearLayoutManager]、[StaggeredGridLayoutManager]。
 * 2. 监听RecyclerView滚动，频繁获取可视item位置的场景，可以使用[ItemVisibleHelper]。
 */
fun RecyclerView.findLastVisibleItemPosition(): Int = when (val lm: LayoutManager? = layoutManager) {
    null -> NO_POSITION
    is LinearLayoutManager -> lm.findLastVisibleItemPosition()
    is StaggeredGridLayoutManager -> lm.findLastVisibleItemPosition()
    else -> lm.runExtensionsPrimitive(NO_POSITION) { findLastVisibleItemPosition(lm) }
}

/**
 * 查找最后一个完全可视item的position
 *
 * 1. 默认仅支持[LinearLayoutManager]、[StaggeredGridLayoutManager]。
 * 2. 监听RecyclerView滚动，频繁获取可视item位置的场景，可以使用[ItemVisibleHelper]。
 */
fun RecyclerView.findLastCompletelyVisibleItemPosition(): Int = when (val lm: LayoutManager? = layoutManager) {
    null -> NO_POSITION
    is LinearLayoutManager -> lm.findLastCompletelyVisibleItemPosition()
    is StaggeredGridLayoutManager -> lm.findLastCompletelyVisibleItemPosition()
    else -> lm.runExtensionsPrimitive(NO_POSITION) { findLastCompletelyVisibleItemPosition(lm) }
}

/**
 * 查找第一个可视item的position
 *
 * @param into 各个跨度空间所对应的position
 */
fun StaggeredGridLayoutManager.findFirstVisibleItemPosition(into: IntArray? = null): Int {
    return findFirstVisibleItemPositions(into).minPosition()
}

/**
 * 查找第一个完全可视item的position
 *
 * @param into 各个跨度空间所对应的position，若传`null`，则创建新的数组对象。
 */
fun StaggeredGridLayoutManager.findFirstCompletelyVisibleItemPosition(into: IntArray? = null): Int {
    return findFirstCompletelyVisibleItemPositions(into).minPosition()
}

/**
 * 查找最后一个可视item的position
 *
 * @param into 各个跨度空间所对应的position，若传`null`，则创建新的数组对象。
 */
fun StaggeredGridLayoutManager.findLastVisibleItemPosition(into: IntArray? = null): Int {
    return findLastVisibleItemPositions(into).maxPosition()
}

/**
 * 查找最后一个完全可视item的position
 *
 * @param into 各个跨度空间所对应的position，若传`null`，则创建新的数组对象。
 */
fun StaggeredGridLayoutManager.findLastCompletelyVisibleItemPosition(into: IntArray? = null): Int {
    return findLastCompletelyVisibleItemPositions(into).maxPosition()
}

/**
 * 不能使用[IntArray.minOrNull]简化代码，
 * 因为返回值是可空类型，调用处会对返回值执行装箱操作。
 */
private fun IntArray.minPosition(): Int {
    var minPosition = NO_POSITION
    for (index in indices) {
        val spanPosition = this[index]
        minPosition = when {
            spanPosition <= NO_POSITION -> continue
            minPosition == NO_POSITION -> spanPosition
            else -> spanPosition.coerceAtMost(minPosition)
        }
    }
    return minPosition
}

/**
 * 不能使用[IntArray.maxOrNull]简化代码，
 * 因为返回值是可空类型，调用处会对返回值执行装箱操作。
 */
private fun IntArray.maxPosition(): Int {
    var maxPosition = NO_POSITION
    for (index in indices) {
        val spanPosition = this[index]
        maxPosition = spanPosition.coerceAtLeast(maxPosition)
    }
    return maxPosition
}

/**
 * 添加第一个item可视时的处理程序
 *
 * 默认仅支持[LinearLayoutManager]、[StaggeredGridLayoutManager]。
 *
 * @param once 为true表示调用一次[handler]后就移除。
 * @return 调用[Disposable.dispose]可以移除[handler]。
 */
fun RecyclerView.doOnFirstItemVisible(once: Boolean = false, handler: () -> Unit): Disposable {
    return ItemVisibleDisposable(once, FIRST_ITEM).attach(this, handler)
}

/**
 * 添加第一个item完全可视时的处理程序
 *
 * 默认仅支持[LinearLayoutManager]、[StaggeredGridLayoutManager]。
 *
 * @param once 为true表示调用一次[handler]后就移除。
 * @return 调用[Disposable.dispose]可以移除[handler]。
 */
fun RecyclerView.doOnFirstItemCompletelyVisible(once: Boolean = false, handler: () -> Unit): Disposable {
    return ItemVisibleDisposable(once, FIRST_ITEM_COMPLETELY).attach(this, handler)
}

/**
 * 添加最后一个item可视时的处理程序
 *
 * 默认仅支持[LinearLayoutManager]、[StaggeredGridLayoutManager]。
 *
 * @param once 为true表示调用一次[handler]后就移除。
 * @return 调用[Disposable.dispose]可以移除[handler]。
 */
fun RecyclerView.doOnLastItemVisible(once: Boolean = false, handler: () -> Unit): Disposable {
    return ItemVisibleDisposable(once, LAST_ITEM).attach(this, handler)
}

/**
 * 添加最后一个item完全可视时的处理程序
 *
 * 默认仅支持[LinearLayoutManager]、[StaggeredGridLayoutManager]。
 *
 * @param once 为true表示调用一次[handler]后就移除。
 * @return 调用[Disposable.dispose]可以移除[handler]。
 */
fun RecyclerView.doOnLastItemCompletelyVisible(once: Boolean = false, handler: () -> Unit): Disposable {
    return ItemVisibleDisposable(once, LAST_ITEM_COMPLETELY).attach(this, handler)
}

/**
 * 可废弃的item可视观察者
 */
private class ItemVisibleDisposable(
    private val once: Boolean,
    private val target: VisibleTarget
) : RecyclerView.OnScrollListener(), Disposable {
    private var helper = ItemVisibleHelper()
    private var handler: (() -> Unit)? = null
    private var previousVisible = false
    override val isDisposed: Boolean
        get() = helper.recyclerView == null && handler == null

    fun attach(
        rv: RecyclerView,
        handler: () -> Unit,
    ): Disposable {
        helper.recyclerView = rv
        this.handler = handler
        rv.addOnScrollListener(this)
        return this
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        val visible = when (target) {
            FIRST_ITEM -> helper.isFirstItemVisible
            FIRST_ITEM_COMPLETELY -> helper.isFirstItemCompletelyVisible
            LAST_ITEM -> helper.isLastItemVisible
            LAST_ITEM_COMPLETELY -> helper.isLastItemCompletelyVisible
        }
        if (visible && visible != previousVisible) {
            handler?.let {
                if (once) dispose()
                it.invoke()
            }
        }
        previousVisible = visible
    }

    override fun dispose() {
        helper.recyclerView?.addOnScrollListener(this)
        helper.recyclerView = null
        handler = null
    }
}

private enum class VisibleTarget {
    FIRST_ITEM,
    FIRST_ITEM_COMPLETELY,
    LAST_ITEM,
    LAST_ITEM_COMPLETELY
}