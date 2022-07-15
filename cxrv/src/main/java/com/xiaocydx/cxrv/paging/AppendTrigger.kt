package com.xiaocydx.cxrv.paging

import android.view.View
import android.view.View.OnAttachStateChangeListener
import androidx.core.view.OneShotPreDrawListener
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.isPreLayout
import com.xiaocydx.cxrv.internal.doOnPreDraw
import com.xiaocydx.cxrv.internal.hasDisplayItem
import com.xiaocydx.cxrv.internal.isLastDisplayItem
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
    private var postAppendListener: OneShotPreDrawListener? = null
    private val appendIfLastItemVisible = AppendIfLastItemVisible()
    private val appendIfLastItemAttached = AppendIfLastItemAttached()
    private val loadStates: LoadStates
        get() = collector.loadStates

    init {
        adapter.also {
            it.addViewHolderListener(this)
            it.addListChangedListener(this)
            it.addAdapterAttachCallback(this)
        }
        collector.addLoadStatesListener(this)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        if (!loadStates.isAllowAppend || !adapter.isLastDisplayItem(position)) {
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

    /**
     * [onListChanged]在[onLoadStatesChanged]之前被调用，
     * 当刷新加载成功时，`lm.itemCount`已更新，`loadStates.refresh`未更新，
     * 因此不会调用[postAppend]，由[onLoadStatesChanged]主动触发末尾加载。
     */
    override fun onListChanged(current: List<Any>) {
        val lm = rv?.layoutManager ?: return
        if (loadStates.isAllowAppend && lm.itemCount < lm.childCount) {
            postAppend()
        }
    }

    /**
     * 当刷新加载成功时，加载结果可能和之前第一页结果一致，
     * 此时[onBindViewHolder]不会被调用，因此需要主动触发末尾加载。
     */
    override fun onLoadStatesChanged(previous: LoadStates, current: LoadStates) {
        if (previous.refreshToSuccess(current)
                || (previous.refresh.isIncomplete && current.refresh.isSuccess)) {
            // refresh从Incomplete直接流转至Success，可能是RecyclerView的重建恢复流程
            postAppend()
        }
    }

    private fun postAppend() {
        removeAppend()
        postAppendListener = rv?.doOnPreDraw(appendIfLastItemVisible)
        appendIfLastItemAttached.keepEnabled()
    }

    private fun removeAppend() {
        postAppendListener?.removeListener()
        postAppendListener = null
        appendIfLastItemAttached.failureEnabled()
    }

    private fun append() {
        removeAppend()
        collector.append()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.rv = recyclerView
        recyclerView.addOnAttachStateChangeListener(appendIfLastItemVisible)
        recyclerView.addOnChildAttachStateChangeListener(appendIfLastItemAttached)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.rv = null
        removeAppend()
        recyclerView.removeOnAttachStateChangeListener(appendIfLastItemVisible)
        recyclerView.removeOnChildAttachStateChangeListener(appendIfLastItemAttached)
    }

    /**
     * 若最后一个item可视，则触发末尾加载，
     * 最后一个item可能是loadFooter或者[adapter]的最后一个item。
     */
    private inner class AppendIfLastItemVisible : Runnable, OnAttachStateChangeListener {
        override fun run() {
            if (!loadStates.isAllowAppend) return
            val lm = rv?.layoutManager ?: return
            val position = lm.itemCount - 1
            if (!adapter.hasDisplayItem
                    || rv?.findViewHolderForLayoutPosition(position) != null) {
                append()
            }
        }

        override fun onViewAttachedToWindow(view: View): Unit = run()

        override fun onViewDetachedFromWindow(view: View): Unit = Unit
    }

    /**
     * 若最后一个item添加为子View，则触发末尾加载，
     * 最后一个item可能是加载Footer或者[adapter]的最后一个item。
     */
    private inner class AppendIfLastItemAttached : OnChildAttachStateChangeListener {
        private var enabled = false

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
            if (enabled || loadStates.append.isFailure) {
                val lm = rv?.layoutManager ?: return
                val holder = rv?.getChildViewHolder(view) ?: return
                if (holder.layoutPosition != lm.itemCount - 1) return
                append()
            }
        }

        override fun onChildViewDetachedFromWindow(view: View): Unit = Unit
    }
}