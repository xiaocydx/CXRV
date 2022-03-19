package com.xiaocydx.recycler.paging

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.isPreLayout
import com.xiaocydx.recycler.extension.*
import com.xiaocydx.recycler.list.AdapterAttachCallback
import com.xiaocydx.recycler.list.ListAdapter
import com.xiaocydx.recycler.list.ListChangedListener
import com.xiaocydx.recycler.list.ViewHolderListener

/**
 * [PagingSource]的末尾加载触发器
 *
 * ### [postAppend]
 * 刷新加载结果可能和之前第一页结果一致，
 * 此时[onBindViewHolder]不会被调用，也就不会尝试触发末尾加载，
 * 因此调用[postAppend]发送消息执行[appendIfLastItemVisible]，触发末尾加载。
 *
 * ### [append]
 * 若在[postAppend]发送的消息被执行之前，已调用[append]，
 * 则会通过[removeAppend]移除[postAppend]发送的消息，
 * 表示末尾加载已触发，不用靠[postAppend]触发末尾加载。
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
    private var recyclerView: RecyclerView? = null
    private var previousNotEmpty = adapter.hasDisplayItem
    private var postAppendDisposable: Disposable? = null
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
        if (isAllowAppend && adapter.isLastDisplayItem(position)) {
            if (recyclerView?.isPreLayout == true) {
                // 此时可能是调用了notifyItemRangeChanged()，额外触发了onBindViewHolder()，
                // 这种情况不符合滑动绑定触发末尾加载的条件，因此替换为LastItemAttached触发方案。
                appendIfLastItemAttached.keepEnabled()
                return
            }
            append()
        }
    }

    override fun onLoadStatesChanged(previous: LoadStates, current: LoadStates) {
        if (!previous.refreshToSuccess(current)) {
            return
        }
        if (adapter.hasDisplayItem) {
            // 刷新加载结果可能和之前第一页结果一致，
            // 此时onBindViewHolder()不会被调用，需要主动触发末尾加载
            postAppend()
            appendIfLastItemAttached.keepEnabled()
            return
        }
        current.refresh.onSuccess {
            if (dataSize == 0 && !isFully) {
                // 若刷新加载的结果为空，且没有加载完全，则主动触发末尾加载
                // 注意：列表在加载之前可能预设了item，因此当前列表不为空不代表加载的第一页不为空。
                append()
            }
        }
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
        postAppendDisposable = recyclerView
            ?.doOnFrameComplete(appendIfLastItemVisible)
    }

    private fun removeAppend() {
        postAppendDisposable?.dispose()
        postAppendDisposable = null
    }

    private fun append() {
        removeAppend()
        collector.append()
        appendIfLastItemAttached.failureEnabled()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        recyclerView.addOnChildAttachStateChangeListener(appendIfLastItemAttached)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        recyclerView.removeOnChildAttachStateChangeListener(appendIfLastItemAttached)
        removeAppend()
        this.recyclerView = null
    }

    /**
     * 若最后一个item可视，则触发末尾加载，
     * 最后一个item可能是加载Footer或者[adapter]的最后一个item。
     */
    private inner class AppendIfLastItemVisible : () -> Unit {
        override fun invoke() {
            if (isAllowAppend && recyclerView?.isLastItemVisible == true) {
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
            if (recyclerView?.isPreLayout == true) {
                return
            }
            if (enabled || isAppendFailure) {
                val lm = recyclerView?.layoutManager ?: return
                val holder = recyclerView?.getChildViewHolder(view) ?: return
                if (holder.layoutPosition == lm.itemCount - 1) {
                    append()
                }
            }
        }

        override fun onChildViewDetachedFromWindow(view: View): Unit = Unit
    }
}