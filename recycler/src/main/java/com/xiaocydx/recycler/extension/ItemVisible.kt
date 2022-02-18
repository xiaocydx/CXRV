package com.xiaocydx.recycler.extension

import androidx.annotation.MainThread
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.xiaocydx.recycler.helper.ItemVisibleHelper
import com.google.android.flexbox.FlexboxLayoutManager

/**
 * 第一个item是否可视
 *
 * 监听RecyclerView滚动，频繁判断item是否可视的场景，
 * 请使用[ItemVisibleHelper]，或者使用[doOnFirstItemVisible]。
 */
val RecyclerView.isFirstItemVisible: Boolean
    get() = when (layoutManager) {
        null -> false
        else -> findFirstVisibleItemPosition() == 0
    }

/**
 * 第一个item是否完全可视
 *
 * 监听RecyclerView滚动，频繁判断item是否可视的场景，
 * 请使用[ItemVisibleHelper]，或者使用[doOnFirstItemCompletelyVisible]。
 */
val RecyclerView.isFirstItemCompletelyVisible: Boolean
    get() = when (layoutManager) {
        null -> false
        else -> findFirstCompletelyVisibleItemPosition() == 0
    }

/**
 * 最后一个item是否可视
 *
 * 监听RecyclerView滚动，频繁判断item是否可视的场景，
 * 请使用[ItemVisibleHelper]，或者使用[doOnLastItemVisible]。
 */
val RecyclerView.isLastItemVisible: Boolean
    get() = when (val lm: LayoutManager? = layoutManager) {
        null -> false
        else -> findLastVisibleItemPosition() == lm.itemCount - 1
    }

/**
 * 最后一个item是否完全可视
 *
 * 监听RecyclerView滚动，频繁判断item是否可视的场景，
 * 请使用[ItemVisibleHelper]，或者使用[doOnLastItemCompletelyVisible]。
 */
val RecyclerView.isLastItemCompletelyVisible: Boolean
    get() = when (val lm: LayoutManager? = layoutManager) {
        null -> false
        else -> findLastCompletelyVisibleItemPosition() == lm.itemCount - 1
    }

/**
 * 查找第一个可视item的position
 *
 * 监听RecyclerView滚动，频繁获取可视item位置的场景，请使用[ItemVisibleHelper]。
 */
fun RecyclerView.findFirstVisibleItemPosition(): Int =
        when (val lm: LayoutManager? = layoutManager) {
            is LinearLayoutManager -> lm.findFirstVisibleItemPosition()
            is StaggeredGridLayoutManager -> lm.findFirstVisibleItemPosition()
            is FlexboxLayoutManager -> lm.findFirstVisibleItemPosition()
            else -> RecyclerView.NO_POSITION
        }

/**
 * 查找第一个完全可视item的position
 *
 * 监听RecyclerView滚动，频繁获取可视item位置的场景，请使用[ItemVisibleHelper]。
 */
fun RecyclerView.findFirstCompletelyVisibleItemPosition(): Int =
        when (val lm: LayoutManager? = layoutManager) {
            is LinearLayoutManager -> lm.findFirstCompletelyVisibleItemPosition()
            is StaggeredGridLayoutManager -> lm.findFirstCompletelyVisibleItemPosition()
            is FlexboxLayoutManager -> lm.findFirstCompletelyVisibleItemPosition()
            else -> RecyclerView.NO_POSITION
        }

/**
 * 查找最后一个可视item的position
 *
 * 监听RecyclerView滚动，频繁获取可视item位置的场景，请使用[ItemVisibleHelper]。
 */
fun RecyclerView.findLastVisibleItemPosition(): Int =
        when (val lm: LayoutManager? = layoutManager) {
            is LinearLayoutManager -> lm.findLastVisibleItemPosition()
            is StaggeredGridLayoutManager -> lm.findLastVisibleItemPosition()
            is FlexboxLayoutManager -> lm.findLastVisibleItemPosition()
            else -> RecyclerView.NO_POSITION
        }

/**
 * 查找最后一个完全可视item的position
 *
 * 监听RecyclerView滚动，频繁获取可视item位置的场景，请使用[ItemVisibleHelper]。
 */
fun RecyclerView.findLastCompletelyVisibleItemPosition(): Int =
        when (val lm: LayoutManager? = layoutManager) {
            is LinearLayoutManager -> lm.findLastCompletelyVisibleItemPosition()
            is StaggeredGridLayoutManager -> lm.findLastCompletelyVisibleItemPosition()
            is FlexboxLayoutManager -> lm.findLastCompletelyVisibleItemPosition()
            else -> RecyclerView.NO_POSITION
        }

/**
 * 查找第一个可视item的position
 *
 * @param into 各个跨度空间所对应的position
 */
fun StaggeredGridLayoutManager.findFirstVisibleItemPosition(
    into: IntArray? = null
): Int = findFirstVisibleItemPositions(into).minPosition()

/**
 * 查找第一个完全可视item的position
 *
 * @param into 各个跨度空间所对应的position，若传`null`，则创建新的数组对象。
 */
fun StaggeredGridLayoutManager.findFirstCompletelyVisibleItemPosition(
    into: IntArray? = null
): Int = findFirstCompletelyVisibleItemPositions(into).minPosition()

/**
 * 查找最后一个可视item的position
 *
 * @param into 各个跨度空间所对应的position，若传`null`，则创建新的数组对象。
 */
fun StaggeredGridLayoutManager.findLastVisibleItemPosition(
    into: IntArray? = null
): Int = findLastVisibleItemPositions(into).maxPosition()

/**
 * 查找最后一个完全可视item的position
 *
 * @param into 各个跨度空间所对应的position，若传`null`，则创建新的数组对象。
 */
fun StaggeredGridLayoutManager.findLastCompletelyVisibleItemPosition(
    into: IntArray? = null
): Int = findLastCompletelyVisibleItemPositions(into).maxPosition()

/**
 * 不能使用[IntArray.minOrNull]简化代码，
 * 因为返回值是可空类型，调用处会对返回值执行装箱操作。
 */
private fun IntArray.minPosition(): Int {
    if (isEmpty()) {
        return RecyclerView.NO_POSITION
    }
    var minPosition = this[0]
    for (i in 1..lastIndex) {
        val spanPosition = this[i]
        if (minPosition > spanPosition) {
            minPosition = spanPosition
        }
    }
    return minPosition
}

/**
 * 不能使用[IntArray.maxOrNull]简化代码，
 * 因为返回值是可空类型，调用处会对返回值执行装箱操作。
 */
private fun IntArray.maxPosition(): Int {
    if (isEmpty()) {
        return RecyclerView.NO_POSITION
    }
    var maxPosition = this[0]
    for (index in 1..lastIndex) {
        val spanPosition = this[index]
        if (maxPosition < spanPosition) {
            maxPosition = spanPosition
        }
    }
    return maxPosition
}

/**
 * 添加第一个item可视时的处理程序
 *
 * @param once 为true表示调用一次[handler]后就移除。
 * @return 调用[Disposable.dispose]可以移除[handler]。
 */
@MainThread
fun RecyclerView.doOnFirstItemVisible(
    once: Boolean = false,
    handler: () -> Unit
): Disposable = ItemVisibleObserver(this, handler, once, VisibleTarget.FIRST_ITEM)

/**
 * 添加第一个item完全可视时的处理程序
 *
 * @param once 为true表示调用一次[handler]后就移除。
 * @return 调用[Disposable.dispose]可以移除[handler]。
 */
@MainThread
fun RecyclerView.doOnFirstItemCompletelyVisible(
    once: Boolean = false,
    handler: () -> Unit
): Disposable = ItemVisibleObserver(this, handler, once, VisibleTarget.FIRST_ITEM_COMPLETELY)

/**
 * 添加最后一个item可视时的处理程序
 *
 * @param once 为true表示调用一次[handler]后就移除。
 * @return 调用[Disposable.dispose]可以移除[handler]。
 */
@MainThread
fun RecyclerView.doOnLastItemVisible(
    once: Boolean = false,
    handler: () -> Unit
): Disposable = ItemVisibleObserver(this, handler, once, VisibleTarget.LAST_ITEM)

/**
 * 添加最后一个item完全可视时的处理程序
 *
 * @param once 为true表示调用一次[handler]后就移除。
 * @return 调用[Disposable.dispose]可以移除[handler]。
 */
@MainThread
fun RecyclerView.doOnLastItemCompletelyVisible(
    once: Boolean = false,
    handler: () -> Unit
): Disposable = ItemVisibleObserver(this, handler, once, VisibleTarget.LAST_ITEM_COMPLETELY)

/**
 * 可废弃的item可视观察者
 */
private class ItemVisibleObserver(
    recyclerView: RecyclerView,
    handler: () -> Unit,
    private val once: Boolean,
    private val target: VisibleTarget
) : RecyclerView.OnScrollListener(), Disposable {
    private var handler: (() -> Unit)? = handler
    private var helper = ItemVisibleHelper(recyclerView)
    private var previousVisible = false
    override val isDisposed: Boolean
        get() = helper.recyclerView == null && handler == null

    init {
        assertMainThread()
        helper.recyclerView?.addOnScrollListener(this)
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        val visible = when (target) {
            VisibleTarget.FIRST_ITEM -> helper.isFirstItemVisible
            VisibleTarget.FIRST_ITEM_COMPLETELY -> helper.isFirstItemCompletelyVisible
            VisibleTarget.LAST_ITEM -> helper.isLastItemVisible
            VisibleTarget.LAST_ITEM_COMPLETELY -> helper.isLastItemCompletelyVisible
        }
        if (visible && visible != previousVisible) {
            handler?.let {
                if (once) {
                    dispose()
                }
                it.invoke()
            }
        }
        previousVisible = visible
    }

    override fun dispose() = runOnMainThread {
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