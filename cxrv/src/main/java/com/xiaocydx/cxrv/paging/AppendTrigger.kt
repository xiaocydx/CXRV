package com.xiaocydx.cxrv.paging

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import androidx.recyclerview.widget.isPreLayout
import com.xiaocydx.cxrv.internal.PreDrawListener
import com.xiaocydx.cxrv.internal.hasDisplayItem
import com.xiaocydx.cxrv.internal.isLastDisplayItem
import com.xiaocydx.cxrv.list.*

/**
 * [PagingSource]的末尾加载触发器
 *
 * ### [preAppend]
 * 刷新加载结果可能和之前第一页结果一致，此时[onBindViewHolder]不会被调用，
 * 也就不会尝试触发末尾加载，因此调用[preAppend]推迟尝试触发末尾加载。
 *
 * ### [append]
 * 调用[append]时，通过[removePreAppend]移除[preAppend]，
 * 表示末尾加载已触发，不用靠[preAppend]推迟尝试触发末尾加载。
 *
 * ### 刷新流程执行时序
 * ```
 *  +----------------+        +-----------------+        +---------------+
 *  | RefreshSuccess | -----> |    preAppend    | -----> | AppendLoading |
 *  +----------------+        +-----------------+        +---------------+
 *          |                        ∧
 *          |                        | remove
 *          V                        |
 *  +----------------+        +-----------------+        +---------------+
 *  |     append     | -----> | removePreAppend | -----> | AppendLoading |
 *  +----------------+        +-----------------+        +---------------+
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
    private var retryListener: AppendRetryListener? = null
    private var scrollListener: AppendScrollListener? = null
    private var preDrawListener: AppendPreDrawListener? = null
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

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.rv = recyclerView
        retryListener = AppendRetryListener(recyclerView)
        scrollListener = AppendScrollListener(recyclerView)
        preDrawListener = AppendPreDrawListener(recyclerView)
        retryListener?.isEnabled = true
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.rv = null
        retryListener?.removeListener()
        scrollListener?.removeListener()
        preDrawListener?.removeListener()
        retryListener = null
        scrollListener = null
        preDrawListener = null
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        if (!loadStates.isAllowAppend || !adapter.isLastDisplayItem(position)) {
            return
        }
        if (rv?.isPreLayout == true) {
            // 此时处于预测性布局流程，额外调用了onBindViewHolder()，
            // 例如列表全选功能，调用notifyItemRangeChanged()做全选更新，
            // 这种情况不启用滚动绑定触发方案，而是启用滚动监听触发方案。
            scrollListener?.isEnabled = true
            return
        }
        append()
    }

    /**
     * [onListChanged]在[onLoadStatesChanged]之前被调用，
     * 当刷新加载成功时，`lm.itemCount`已更新，`loadStates.refresh`未更新，
     * 因此不会调用[preAppend]，由[onLoadStatesChanged]主动触发末尾加载。
     */
    override fun onListChanged(current: List<Any>) {
        val lm = rv?.layoutManager ?: return
        if (loadStates.isAllowAppend && lm.itemCount < lm.childCount) {
            preAppend()
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
            preAppend()
        }
    }

    private fun preAppend() {
        scrollListener?.isEnabled = true
        preDrawListener?.isEnabled = true
    }

    private fun removePreAppend() {
        scrollListener?.isEnabled = false
        preDrawListener?.isEnabled = false
    }

    private fun append() {
        removePreAppend()
        collector.append()
    }

    /**
     * 若最后一个item可视，则触发末尾加载，
     * 最后一个item可能是loadFooter或者[adapter]的最后一个item。
     */
    private fun appendIfLastItemVisible() {
        // 当append.isFailure = true时，由AppendRetryListener处理
        if (!loadStates.isAllowAppend || loadStates.append.isFailure) return
        val lm = rv?.layoutManager ?: return
        val position = lm.itemCount - 1
        if (!adapter.hasDisplayItem || rv?.findViewHolderForLayoutPosition(position) != null) {
            append()
        }
    }

    /**
     * 末尾加载失败后，若最后一个item再次可视，则触发末尾加载，
     * 最后一个item可能是loadFooter或者[adapter]的最后一个item。
     */
    private inner class AppendRetryListener(
        private val rv: RecyclerView
    ) : OnChildAttachStateChangeListener {
        var isEnabled = false

        init {
            rv.addOnChildAttachStateChangeListener(this)
        }

        override fun onChildViewAttachedToWindow(view: View) {
            if (!isEnabled || !loadStates.append.isFailure) return
            val lm = rv.layoutManager ?: return
            val holder = rv.getChildViewHolder(view) ?: return
            if (holder.layoutPosition != lm.itemCount - 1) return
            append()
        }

        override fun onChildViewDetachedFromWindow(view: View): Unit = Unit

        fun removeListener() {
            rv.removeOnChildAttachStateChangeListener(this)
        }
    }

    /**
     * [onBindViewHolder]的替补方案，在[onScrolled]被调用时尝试触发末尾加载
     */
    private inner class AppendScrollListener(private val rv: RecyclerView) : OnScrollListener() {
        var isEnabled = false

        init {
            rv.addOnScrollListener(this)
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (isEnabled) appendIfLastItemVisible()
        }

        fun removeListener() {
            rv.removeOnScrollListener(this)
        }
    }

    /**
     * [AppendScrollListener]的替补方案，在[onPreDraw]被调用时尝试触发末尾加载
     */
    private inner class AppendPreDrawListener(rv: RecyclerView) : PreDrawListener(rv) {
        var isEnabled = false

        override fun onPreDraw(): Boolean {
            if (isEnabled) appendIfLastItemVisible()
            return super.onPreDraw()
        }

        /**
         * ViewPager2滚动时会将嵌套的RecyclerView从Window上分离，
         * 保存在离屏缓存中，滚动回来时触发该函数，此时数据可能发生变化，
         * 需要尝试触发末尾加载。
         */
        override fun onViewAttachedToWindow(view: View) {
            super.onViewAttachedToWindow(view)
            preAppend()
        }
    }
}