package com.xiaocydx.sample.paging.config

import android.content.Context
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.xiaocydx.recycler.extension.pagingCollector
import com.xiaocydx.recycler.list.ListAdapter
import com.xiaocydx.recycler.paging.LoadStates
import com.xiaocydx.recycler.paging.LoadStatesListener
import com.xiaocydx.recycler.paging.PagingCollector
import com.xiaocydx.recycler.paging.refreshToComplete

/**
 * 将RecyclerView的添加到[SwipeRefreshLayout]
 *
 * ```
 * val adapter: ListAdapter<*, *> = ...
 * val parent = recyclerView.withSwipeRefresh(adapter)
 * ```
 */
fun RecyclerView.withSwipeRefresh(adapter: ListAdapter<*, *>): SwipeRefreshLayout {
    return withSwipeRefresh().apply { setAdapter(adapter) }
}

/**
 * 将RecyclerView的父级替换为[SwipeRefreshLayout]
 *
 * ```
 * val adapter: ListAdapter<*, *> = ...
 * val newParent = recyclerView.replaceWithSwipeRefresh(adapter)
 * ```
 */
fun RecyclerView.replaceWithSwipeRefresh(adapter: ListAdapter<*, *>): SwipeRefreshLayout {
    return replaceWithSwipeRefresh().apply { setAdapter(adapter) }
}

/**
 * 将RecyclerView的添加到[SwipeRefreshLayout]
 */
internal fun RecyclerView.withSwipeRefresh(): DefaultSwipeRefreshLayout {
    require(parent == null) {
        "RecyclerView的父级不为空，无法添加到SwipeRefreshLayout。"
    }
    return DefaultSwipeRefreshLayout(context)
        .also { it.addView(this, MATCH_PARENT, MATCH_PARENT) }
}

/**
 * 将RecyclerView的父级替换为[SwipeRefreshLayout]
 */
internal fun RecyclerView.replaceWithSwipeRefresh(): DefaultSwipeRefreshLayout {
    val oldParent = requireNotNull(parent) {
        "RecyclerView的父级为空，无法进行父级替换，" +
                "请改为调用`RecyclerView.withSwipeRefresh(ListAdapter<*, *>)`创建刷新容器。"
    }
    require(oldParent is ViewGroup) {
        "RecyclerView的父级需要是ViewGroup。"
    }
    findSwipeRefresh()?.let { return it }

    val oldIndex = oldParent.indexOfChild(this)
    val oldLp = this.layoutParams
    if (oldParent.isAttachedToWindow) {
        oldParent.removeView(this)
    } else {
        oldParent.removeViewInLayout(this)
    }
    return withSwipeRefresh().also {
        oldParent.addView(it, oldIndex, oldLp)
    }
}

/**
 * 查找RecyclerView的[SwipeRefreshLayout]父级
 */
private fun RecyclerView.findSwipeRefresh(): DefaultSwipeRefreshLayout? {
    var parent = this.parent
    while (parent != null) {
        if (parent is DefaultSwipeRefreshLayout) {
            return parent
        } else {
            parent = parent.parent
        }
    }
    return null
}

internal class DefaultSwipeRefreshLayout(
    context: Context
) : SwipeRefreshLayout(context), LoadStatesListener {
    private var collector: PagingCollector<*>? = null

    init {
        setOnRefreshListener {
            collector?.refreshAtLeast(duration = 300)
        }
    }

    fun setAdapter(adapter: ListAdapter<*, *>?) {
        val collector: PagingCollector<*>? = adapter?.pagingCollector
        if (this.collector == collector) {
            return
        }
        this.collector?.removeLoadStatesListener(this)
        this.collector = collector
        collector?.addLoadStatesListener(this)
    }

    override fun onLoadStatesChanged(previous: LoadStates, current: LoadStates) {
        if (previous.refreshToComplete(current)) {
            isRefreshing = false
        }
    }
}