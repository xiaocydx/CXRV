package com.xiaocydx.recycler.paging

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.isPreLayout
import com.xiaocydx.recycler.extension.hasDisplayItem
import com.xiaocydx.recycler.extension.isLastDisplayItem
import com.xiaocydx.recycler.helper.ItemVisibleHelper
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
 * 因此调用[postAppend]延迟执行[appendIfLastItemVisible]，触发末尾加载。
 *
 * ### [append]
 * 若在[postAppend]的延迟任务执行之前，[append]被调用了，
 * 则会通过[removeAppend]移除[postAppend]的延迟任务，
 * 表示末尾加载已触发，不用靠[postAppend]触发末尾加载。
 *
 * ### 执行时序
 * ```
 *  +----------------+        +------------+  delay  +---------------+
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
    private val collector: PagingCollector<*>
) : LoadStatesListener, ViewHolderListener<ViewHolder>,
    ListChangedListener<Any>, AdapterAttachCallback {
    private var isPostAppend = false
    private val adapter: ListAdapter<*, *> = collector.adapter
    private var previousNotEmpty = adapter.hasDisplayItem
    private val helper = ItemVisibleHelper()
    private val recyclerView: RecyclerView?
        get() = helper.recyclerView
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

    override fun onLoadStatesChanged(previous: LoadStates, current: LoadStates) {
        if (!previous.refreshToSuccess(current)) {
            return
        }
        if (adapter.hasDisplayItem) {
            // 刷新加载结果可能和之前第一页结果一致，
            // 此时onBindViewHolder()不会被调用，需要主动触发末尾加载
            postAppend()
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        if (isAllowAppend && adapter.isLastDisplayItem(position)) {
            if (recyclerView?.isPreLayout == true) {
                // 此时可能是调用了notifyItemRangeChanged()，额外触发了onBindViewHolder()，
                // 这种情况不符合滑动触发末尾加载的条件，因此替换为postAppend()触发末尾加载。
                postAppend()
                return
            }
            append()
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

    /**
     * 发送消息、添加滚动监听执行[run]
     *
     * 第一页数据可能超过一屏，此时仅靠消息执行[appendIfLastItemVisible]，
     * 并不会触发末尾加载，因此添加滚动监听，滚动过程执行[appendIfLastItemVisible]。
     */
    private fun postAppend() {
        if (!isPostAppend) {
            isPostAppend = true
            recyclerView?.post(appendIfLastItemVisible)
            recyclerView?.addOnScrollListener(appendIfLastItemVisible)
        }
    }

    /**
     * 移除[postAppend]发送的消息、添加的滚动监听
     */
    private fun removeAppend() {
        if (isPostAppend) {
            isPostAppend = false
            recyclerView?.removeCallbacks(appendIfLastItemVisible)
            recyclerView?.removeOnScrollListener(appendIfLastItemVisible)
        }
    }

    /**
     * 若在[postAppend]发送的消息被执行之前，该函数被调用，
     * 则会通过[removeAppend]移除[postAppend]发送的消息，
     * 表示末尾加载已触发，不需要靠[postAppend]进行触发。
     */
    private fun append() {
        removeAppend()
        collector.append()
    }

    /**
     * 分页场景下Footer一般只有一个，即加载Footer，
     * 因此判断最后一个item是否可视，若可视则触发末尾加载，
     * 该item可能是加载Footer或者[adapter]的最后一个item。
     */
    private val appendIfLastItemVisible = object : OnScrollListener(), Runnable {
        override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
            run()
        }

        override fun run() {
            if (isAllowAppend && helper.isLastItemVisible) {
                append()
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        helper.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        removeAppend()
        helper.recyclerView = null
    }
}