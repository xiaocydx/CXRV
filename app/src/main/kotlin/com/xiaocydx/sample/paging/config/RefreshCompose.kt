package com.xiaocydx.sample.paging.config

import android.content.Context
import android.os.SystemClock
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.paging.*
import kotlinx.coroutines.delay

/**
 * 将RecyclerView的添加到[SwipeRefreshLayout]
 *
 * ```
 * val adapter: ListAdapter<*, *> = ...
 * val parent = recyclerView.withSwipeRefresh(adapter)
 * ```
 */
fun RecyclerView.withSwipeRefresh(adapter: ListAdapter<*, *>): SwipeRefreshLayout {
    return addToSwipeRefreshLayout().apply { setAdapter(adapter) }
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
    return replaceParentToSwipeRefreshLayout().apply { setAdapter(adapter) }
}

/**
 * 将RecyclerView的添加到[SwipeRefreshLayout]
 */
private fun RecyclerView.addToSwipeRefreshLayout(): DefaultSwipeRefreshLayout {
    require(parent == null) {
        "RecyclerView的父级不为空，无法添加到SwipeRefreshLayout。"
    }
    return DefaultSwipeRefreshLayout(context)
        .also { it.addView(this, MATCH_PARENT, MATCH_PARENT) }
}

/**
 * 将RecyclerView的父级替换为[SwipeRefreshLayout]
 */
private fun RecyclerView.replaceParentToSwipeRefreshLayout(): DefaultSwipeRefreshLayout {
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
    return addToSwipeRefreshLayout().also {
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

internal class DefaultSwipeRefreshLayout(context: Context) : SwipeRefreshLayout(context) {
    private var collector: PagingCollector<*>? = null
    private val controller = RefreshCompleteController()

    init {
        setOnRefreshListener {
            controller.refreshAtLeast(duration = 300)
        }
    }

    fun setAdapter(adapter: ListAdapter<*, *>?) {
        val collector: PagingCollector<*>? = adapter?.pagingCollector
        if (this.collector == collector) return
        val previous = this.collector
        previous?.removeLoadStatesListener(controller)
        previous?.removeHandleEventListener(controller)
        this.collector = collector
        collector?.addLoadStatesListener(controller)
        collector?.addHandleEventListener(controller)
    }

    private inner class RefreshCompleteController : HandleEventListener<Any>, LoadStatesListener {
        private var refreshCompleteWhen = 0L

        /**
         * 下拉刷新动画至少持续[duration]时间，避免刷新加载太快完成，导致动画很快结束
         */
        fun refreshAtLeast(duration: Long) {
            refreshCompleteWhen = SystemClock.uptimeMillis() + duration
            collector?.refresh()
        }

        override suspend fun handleEvent(rv: RecyclerView, event: PagingEvent<Any>) {
            val loadType = event.loadType
            val loadState = event.loadStates.refresh
            if (loadType != LoadType.REFRESH || !loadState.isComplete) return
            val timeMillis = refreshCompleteWhen - SystemClock.uptimeMillis()
            if (timeMillis <= 0) return
            delay(timeMillis)
        }

        override fun onLoadStatesChanged(previous: LoadStates, current: LoadStates) {
            if (previous.refreshToComplete(current)) isRefreshing = false
        }
    }
}