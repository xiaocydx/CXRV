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

package com.xiaocydx.accompanist.paging

import android.content.Context
import android.os.SystemClock
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.paging.HandleEventListener
import com.xiaocydx.cxrv.paging.LoadStates
import com.xiaocydx.cxrv.paging.LoadStatesListener
import com.xiaocydx.cxrv.paging.LoadType
import com.xiaocydx.cxrv.paging.PagingCollector
import com.xiaocydx.cxrv.paging.PagingEvent
import com.xiaocydx.cxrv.paging.isComplete
import com.xiaocydx.cxrv.paging.loadType
import com.xiaocydx.cxrv.paging.pagingCollector
import com.xiaocydx.cxrv.paging.refreshToComplete
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
 * 将ViewPager2的添加到[SwipeRefreshLayout]
 *
 * ```
 * val adapter: ListAdapter<*, *> = ...
 * val parent = viewPager2.withSwipeRefresh(adapter)
 * ```
 */
fun ViewPager2.withSwipeRefresh(adapter: ListAdapter<*, *>): SwipeRefreshLayout {
    return addToSwipeRefreshLayout().apply { setAdapter(adapter) }
}

/**
 * 将ViewPager2的父级替换为[SwipeRefreshLayout]
 *
 * ```
 * val adapter: ListAdapter<*, *> = ...
 * val newParent = viewPager2.replaceWithSwipeRefresh(adapter)
 * ```
 */
fun ViewPager2.replaceWithSwipeRefresh(adapter: ListAdapter<*, *>): SwipeRefreshLayout {
    return replaceParentToSwipeRefreshLayout().apply { setAdapter(adapter) }
}

private fun ViewGroup.addToSwipeRefreshLayout(): DefaultSwipeRefreshLayout {
    require(parent == null) { "父级不为空，无法添加到SwipeRefreshLayout" }
    return DefaultSwipeRefreshLayout(context)
        .also { it.addView(this, MATCH_PARENT, MATCH_PARENT) }
}

private fun ViewGroup.replaceParentToSwipeRefreshLayout(): DefaultSwipeRefreshLayout {
    val oldParent = requireNotNull(parent) { "父级为空，无法进行父级替换" }
    require(oldParent is ViewGroup) { "父级需要是ViewGroup" }
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

private fun ViewGroup.findSwipeRefresh(): DefaultSwipeRefreshLayout? {
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
            delay(timeMillis = refreshCompleteWhen - SystemClock.uptimeMillis())
        }

        override fun onLoadStatesChanged(previous: LoadStates, current: LoadStates) {
            if (previous.refreshToComplete(current)) isRefreshing = false
        }
    }
}