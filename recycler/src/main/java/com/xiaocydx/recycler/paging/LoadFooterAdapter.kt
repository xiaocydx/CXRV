package com.xiaocydx.recycler.paging

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.ViewHolder
import com.xiaocydx.recycler.concat.ViewAdapter
import com.xiaocydx.recycler.extension.findLastCompletelyVisibleItemPosition
import com.xiaocydx.recycler.extension.findLastVisibleItemPosition
import com.xiaocydx.recycler.extension.hasDisplayItem
import com.xiaocydx.recycler.extension.isFirstItemCompletelyVisible
import com.xiaocydx.recycler.list.ListAdapter
import com.xiaocydx.recycler.list.ListChangedListener

/**
 * 加载尾部适配器
 *
 * @author xcc
 * @date 2021/9/17
 */
internal class LoadFooterAdapter(
    private val config: LoadFooterConfig,
    private val adapter: ListAdapter<*, *>
) : ViewAdapter<ViewHolder>(), LoadStatesListener, ListChangedListener<Any> {
    private var visible: Visible = Visible.NONE
    private var isPostponeHandleFullyVisible = false
    private var loadStates: LoadStates = LoadStates.Incomplete
    private var preDrawListener: PreDrawListener? = null

    init {
        val collector = adapter.pagingCollector
        config.complete(
            retry = collector::retry,
            exception = { collector.loadStates.exception }
        )
        adapter.addListChangedListener(this)
        collector.addLoadStatesListener(this)
    }

    override fun getItemViewType(): Int = hashCode()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LoadViewLayout(
            context = parent.context,
            loadingItem = config.loadingScope?.getViewItem(),
            successItem = config.fullyScope?.getViewItem(),
            failureItem = config.failureScope?.getViewItem()
        )
        return ViewHolder(itemView, parent).apply {
            itemView.layoutParams.width = config.width
            itemView.layoutParams.height = config.height
        }
    }

    override fun onBindViewHolder(
        holder: ViewHolder
    ): Unit = with(holder.itemView as LoadViewLayout) {
        when (visible) {
            Visible.LOADING -> loadingVisible()
            Visible.FAILURE -> failureVisible()
            Visible.FULLY -> successVisible()
            Visible.NONE -> throw AssertionError("局部刷新出现断言异常")
        }
    }

    /**
     * 加载完全后，在列表更改时更新加载完全视图的显示情况
     *
     * **注意**：[onListChanged]在[onLoadStatesChanged]之前被调用。
     */
    override fun onListChanged(current: List<Any>) {
        val hasDisplayItem = adapter.hasDisplayItem
        when {
            !loadStates.isFully -> return
            visible == Visible.FULLY && hasDisplayItem
                    && config.isFullyVisibleWhileExceed -> {
                // 此时FULLY视图已显示，并且列表已更改
                postponeHandleFullyVisible()
            }
            visible == Visible.FULLY && !hasDisplayItem -> {
                // 此时FULLY视图已显示，并且列表已空
                updateLoadFooter(Visible.NONE)
            }
            visible == Visible.NONE && hasDisplayItem -> {
                // 此时FULLY视图未显示，并且列表不为空
                if (config.isFullyVisibleWhileExceed) {
                    postponeHandleFullyVisible()
                } else {
                    updateLoadFooter(Visible.FULLY)
                }
            }
        }
    }

    override fun onLoadStatesChanged(previous: LoadStates, current: LoadStates) {
        loadStates = current
        val visible = current.toVisible()
        if (visible == Visible.FULLY) {
            // 末尾加载完全时，会在FULLY视图前面插入item，
            // 若此时有item动画，则FULLY视图看起来像是被“挤下去”，体验并不好，
            // 因此先移除loadFooter，在RV布局流程完成后，判断是否显示FULLY视图。
            postponeHandleFullyVisible()
        } else {
            updateLoadFooter(visible)
        }
    }

    private fun LoadStates.toVisible(): Visible = when {
        !adapter.hasDisplayItem -> Visible.NONE
        this.isFully -> if (config.fullyScope != null) Visible.FULLY else Visible.NONE
        append.isIncomplete -> Visible.NONE
        append.isLoading -> if (config.loadingScope != null) Visible.LOADING else Visible.NONE
        append.isFailure -> if (config.failureScope != null) Visible.FAILURE else Visible.NONE
        append.isSuccess -> Visible.NONE
        else -> visible
    }

    private fun updateLoadFooter(visible: Visible) {
        if (this.visible != visible) {
            this.visible = visible
            updateItem(show = visible != Visible.NONE, anim = NeedAnim.NOT_ALL)
        }
    }

    /**
     * 在[handleFullyVisibleWhilePreDraw]被调用时判断是否显示FULLY视图
     *
     * 先移除`loadFooter`是为了满足两种场景：
     * 1. 当`config.isFullyVisibleWhileExceed = true`时，
     * 需要先移除`loadFooter`，才能在RV布局流程完成后，判断是否显示FULLY视图。
     * 2. 末尾加载完全时，会在FULLY视图前面插入item，
     * 若此时有item动画，则FULLY视图看起来像是被“挤下去”，体验并不好，
     * 因此先移除`loadFooter`，在RV布局流程完成后，判断是否显示FULLY视图。
     */
    private fun postponeHandleFullyVisible() {
        updateLoadFooter(Visible.NONE)
        isPostponeHandleFullyVisible = true
    }

    /**
     * 该函数在视图树draw之前被调用，即RV布局流程完成后被调用
     */
    private fun handleFullyVisibleWhilePreDraw() {
        if (!isPostponeHandleFullyVisible) return
        assert(visible == Visible.NONE) { "判断是否显示FULLY视图出现断言异常" }
        isPostponeHandleFullyVisible = false
        val rv = recyclerView ?: return
        if (!config.isFullyVisibleWhileExceed || !rv.isFirstItemCompletelyVisible) {
            updateLoadFooter(Visible.FULLY)
            return
        }

        val lastVisiblePosition = rv.findLastVisibleItemPosition()
        val lastCompletelyVisiblePosition = rv.findLastCompletelyVisibleItemPosition()
        val isExceed = lastVisiblePosition != lastCompletelyVisiblePosition
        updateLoadFooter(if (isExceed) Visible.FULLY else Visible.NONE)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        preDrawListener = PreDrawListener(recyclerView, ::handleFullyVisibleWhilePreDraw)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        preDrawListener?.removeListener()
        preDrawListener = null
    }

    private enum class Visible {
        NONE, LOADING, FAILURE, FULLY
    }
}