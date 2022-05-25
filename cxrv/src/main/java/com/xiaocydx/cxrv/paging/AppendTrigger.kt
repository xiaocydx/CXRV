package com.xiaocydx.cxrv.paging

import android.view.View
import androidx.core.view.OneShotPreDrawListener
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.isPreLayout
import com.xiaocydx.cxrv.internal.doOnPreDraw
import com.xiaocydx.cxrv.internal.hasDisplayItem
import com.xiaocydx.cxrv.internal.isLastDisplayItem
import com.xiaocydx.cxrv.itemvisible.isLastItemVisible
import com.xiaocydx.cxrv.list.*

/**
 * [PagingSource]的末尾加载触发器
 *
 * ### [postAppend]
 * 刷新加载结果可能和之前第一页结果一致，此时[onBindViewHolder]不会被调用，
 * 也就不会尝试触发末尾加载，因此调用[postAppend]主动触发末尾加载。
 *
 * ### [append]
 * 调用[append]，会通过[removeAppend]移除[postAppend]，
 * 表示末尾加载已触发，不用靠[postAppend]主动触发末尾加载。
 *
 * ### 执行时序
 * ```
 *  +----------------+        +------------+         +---------------+
 *  | RefreshSuccess | -----> | postAppend | ------> | AppendLoading |
 *  +----------------+        +------------+         +---------------+
 *          |                       ∧
 *          |                       | remove
 *          V                       |
 *     +--------+            +--------------+        +---------------+
 *     | append | ---------> | removeAppend | -----> | AppendLoading |
 *     +--------+            +--------------+        +---------------+
 * ```
 * @author xcc
 * @date 2021/9/19
 */
internal class AppendTrigger(
    private val adapter: ListAdapter<*, *>,
    private val collector: PagingCollector<*>
) : LoadStatesListener, ViewHolderListener<ViewHolder>,
        ListChangedListener<Any>, AdapterAttachCallback {
    private var rv: RecyclerView? = null
    private var previousNotEmpty = adapter.hasDisplayItem
    private var postAppendDisposable: OneShotPreDrawListener? = null
    private val appendIfLastItemVisible = AppendIfLastItemVisible()
    private val appendIfLastItemAttached = AppendIfLastItemAttached()
    private val isAllowAppend: Boolean
        get() = collector.loadStates.isAllowAppend

    init {
        adapter.also {
            it.addViewHolderListener(this)
            it.addListChangedListener(this)
            it.addAdapterAttachCallback(this)
        }
        collector.addLoadStatesListener(this)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        if (!isAllowAppend || !adapter.isLastDisplayItem(position)) {
            return
        }
        if (rv?.isPreLayout == true) {
            // 此时可能是调用了notifyItemRangeChanged()，额外触发了onBindViewHolder()，
            // 这种情况不符合滑动绑定触发末尾加载的条件，因此替换为LastItemAttached触发方案。
            appendIfLastItemAttached.keepEnabled()
            return
        }
        append()
    }

    override fun onLoadStatesChanged(previous: LoadStates, current: LoadStates) {
        if (!adapter.hasDisplayItem || !previous.refreshToSuccess(current)) {
            return
        }
        // 刷新加载结果可能和之前第一页结果一致，
        // 此时onBindViewHolder()不会被调用，需要主动触发末尾加载
        postAppend()
        appendIfLastItemAttached.keepEnabled()
    }

    override fun onListChanged(current: List<Any>) {
        val currentNotEmpty = adapter.hasDisplayItem
        if (previousNotEmpty
                && !currentNotEmpty
                && !collector.loadStates.isFully) {
            // 当前列表被清空，但是还未加载完全，则主动触发末尾加载
            append()
        }
        previousNotEmpty = currentNotEmpty
    }

    private fun postAppend() {
        removeAppend()
        postAppendDisposable = rv?.doOnPreDraw {
            appendIfLastItemVisible()
        }
    }

    private fun removeAppend() {
        postAppendDisposable?.removeListener()
        postAppendDisposable = null
    }

    private fun append() {
        removeAppend()
        collector.append()
        appendIfLastItemAttached.failureEnabled()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.rv = recyclerView
        recyclerView.addOnChildAttachStateChangeListener(appendIfLastItemAttached)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.rv = null
        removeAppend()
        recyclerView.removeOnChildAttachStateChangeListener(appendIfLastItemAttached)
    }

    /**
     * 若最后一个item可视，则触发末尾加载，
     * 最后一个item可能是加载Footer或者[adapter]的最后一个item。
     */
    private inner class AppendIfLastItemVisible : () -> Unit {
        override fun invoke() {
            if (isAllowAppend && rv?.isLastItemVisible == true) {
                append()
            }
        }
    }

    /**
     * 若最后一个item添加为子View，则触发末尾加载，
     * 最后一个item可能是加载Footer或者[adapter]的最后一个item。
     */
    private inner class AppendIfLastItemAttached : OnChildAttachStateChangeListener {
        private var enabled = false
        private val isAppendFailure: Boolean
            get() = collector.loadStates.append.isFailure

        fun keepEnabled() {
            enabled = true
        }

        fun failureEnabled() {
            enabled = false
        }

        override fun onChildViewAttachedToWindow(view: View) {
            if (rv?.isPreLayout == true) {
                return
            }
            if (enabled || isAppendFailure) {
                val lm = rv?.layoutManager ?: return
                val holder = rv?.getChildViewHolder(view) ?: return
                if (holder.layoutPosition == lm.itemCount - 1) {
                    append()
                }
            }
        }

        override fun onChildViewDetachedFromWindow(view: View): Unit = Unit
    }
}