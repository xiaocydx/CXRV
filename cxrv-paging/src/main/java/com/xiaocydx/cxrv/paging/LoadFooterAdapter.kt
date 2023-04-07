@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")

package com.xiaocydx.cxrv.paging

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.SimpleViewHolder
import com.xiaocydx.cxrv.concat.ViewAdapter
import com.xiaocydx.cxrv.internal.PreDrawListener
import com.xiaocydx.cxrv.internal.hasDisplayItem
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.ListChangedListener
import com.xiaocydx.cxrv.paging.LoadFooterAdapter.Visible.*

/**
 * 加载尾部适配器
 *
 * @author xcc
 * @date 2021/9/17
 */
@PublishedApi
internal class LoadFooterAdapter(
    private val config: LoadFooterConfig,
    private val adapter: ListAdapter<*, *>
) : ViewAdapter<ViewHolder>(), LoadStatesListener, ListChangedListener<Any> {
    private val bounds = Rect()
    private var visible: Visible = NONE
    private var isPostponeHandleFullyVisible = false
    private var loadStates: LoadStates = LoadStates.Incomplete
    private var preDrawListener: PreDrawListenerImpl? = null

    init {
        val collector = adapter.pagingCollector
        config.complete(
            retry = collector::retry,
            exception = { collector.loadStates.exception }
        )
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        adapter.addListChangedListener(this)
        adapter.pagingCollector.addLoadStatesListener(this)
        preDrawListener = PreDrawListenerImpl(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        adapter.removeListChangedListener(this)
        adapter.pagingCollector.removeLoadStatesListener(this)
        preDrawListener?.removeListener()
        preDrawListener = null
    }

    override fun getItemViewType(): Int = hashCode()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LoadViewLayout(
            context = parent.context,
            width = config.width,
            height = config.height,
            loadingItem = config.loadingScope?.getViewItem(),
            successItem = config.fullyScope?.getViewItem(),
            failureItem = config.failureScope?.getViewItem()
        )
        return SimpleViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder) {
        val itemView = holder.itemView as LoadViewLayout
        when (visible) {
            LOADING -> itemView.loadingVisible()
            FAILURE -> itemView.failureVisible()
            FULLY -> itemView.successVisible()
            NONE -> throw AssertionError("局部刷新出现断言异常")
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
            // 此时FULLY视图未显示，并且列表不为空
            visible == NONE && hasDisplayItem -> postponeHandleFullyVisible()
            // 此时FULLY视图已显示，并且列表已更改
            visible == FULLY && hasDisplayItem -> postponeHandleFullyVisible()
            // 此时FULLY视图已显示，并且列表已空
            visible == FULLY && !hasDisplayItem -> updateLoadFooter(NONE)
        }
    }

    override fun onLoadStatesChanged(previous: LoadStates, current: LoadStates) {
        loadStates = current
        val visible = current.toVisible()
        if (visible == FULLY) {
            var removeFooter = true
            if (previous.refresh.isIncomplete) {
                // 由于当前加载状态的流转过程可能是RecyclerView的重建恢复流程，
                // 因此假设重建之前FULLY视图已显示，此次先显示FULLY视图，参与下一帧的测量和布局，
                // 确保正常恢复RecyclerView的滚动位置，在下一帧布局完成后，再判断是否需要显示FULLY视图。
                removeFooter = false
                updateLoadFooter(FULLY)
            }
            postponeHandleFullyVisible(removeFooter)
        } else {
            updateLoadFooter(visible)
        }
    }

    private fun LoadStates.toVisible(): Visible = when {
        !adapter.hasDisplayItem -> NONE
        this.isFully -> if (config.fullyScope != null) FULLY else NONE
        append.isIncomplete -> NONE
        append.isLoading -> if (config.loadingScope != null) LOADING else NONE
        append.isFailure -> if (config.failureScope != null) FAILURE else NONE
        append.isSuccess -> NONE
        else -> visible
    }

    private fun updateLoadFooter(visible: Visible) {
        if (this.visible != visible) {
            this.visible = visible
            updateItem(show = visible != NONE, anim = NeedAnim.NOT_ALL)
        }
    }

    /**
     * 在[handleFullyVisibleWhilePreDraw]被调用时判断是否显示FULLY视图
     *
     * [removeFooter] == `true`满足的场景：
     * 末尾加载完全时，会在FULLY视图前面插入item，
     * 若此时有item动画，则FULLY视图看起来像是被“挤下去”，体验并不好，
     * 因此先移除`loadFooter`，在RV布局流程完成后，判断是否显示FULLY视图。
     */
    private fun postponeHandleFullyVisible(removeFooter: Boolean = false) {
        if (removeFooter) updateLoadFooter(NONE)
        isPostponeHandleFullyVisible = true
    }

    /**
     * 该函数在视图树draw之前被调用，即RV布局流程完成后被调用
     */
    private fun handleFullyVisibleWhilePreDraw() {
        if (!isPostponeHandleFullyVisible) return
        isPostponeHandleFullyVisible = false
        if (!loadStates.isFully) return
        val rv = recyclerView ?: return
        when {
            !adapter.hasDisplayItem -> updateLoadFooter(NONE)
            !config.isFullyVisibleWhileExceed -> updateLoadFooter(FULLY)
            visible == NONE -> {
                if (rv.isExceedVisibleRange()) updateLoadFooter(FULLY)
            }
            visible == FULLY -> {
                val holder = viewHolder ?: return
                val lm = rv.layoutManager ?: return
                lm.getDecoratedBoundsWithMargins(holder.itemView, bounds)
                val isExceedVisibleRange = if (lm.canScrollHorizontally()) {
                    rv.isExceedVisibleRange(diffScrollRange = -bounds.width())
                } else if (lm.canScrollVertically()) {
                    rv.isExceedVisibleRange(diffScrollRange = -bounds.height())
                } else false
                if (!isExceedVisibleRange) updateLoadFooter(NONE)
            }
        }
    }

    /**
     * RecyclerView的滚动范围是否超过可视范围
     *
     * @param diffScrollRange 差异滚动范围，总的滚动范围加上该值后再进行判断
     */
    private fun RecyclerView.isExceedVisibleRange(diffScrollRange: Int = 0): Boolean {
        val lm = layoutManager ?: return false
        return if (lm.canScrollHorizontally()) {
            computeHorizontalScrollRange() + diffScrollRange > computeHorizontalScrollExtent()
        } else if (lm.canScrollVertically()) {
            computeVerticalScrollRange() + diffScrollRange > computeVerticalScrollExtent()
        } else false
    }

    private enum class Visible {
        NONE, LOADING, FAILURE, FULLY
    }

    private inner class PreDrawListenerImpl(rv: RecyclerView) : PreDrawListener(rv) {
        override fun onPreDraw(): Boolean {
            handleFullyVisibleWhilePreDraw()
            return super.onPreDraw()
        }

        override fun onViewAttachedToWindow(view: View) {
            super.onViewAttachedToWindow(view)
            postponeHandleFullyVisible()
        }
    }
}