package com.xiaocydx.recycler.concat

import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.VisibleForTesting
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import androidx.recyclerview.widget.ViewController

/**
 * View适配器，用于构建HeaderFooter
 *
 * 当移除HeaderFooter或者RecyclerView从Window上分离时，
 * [ViewController]会清除已分离的ViewHolder，拦截要被回收的ViewHolder，
 * 子类不用关注移除HeaderFooter和共享[RecycledViewPool]的场景下，可能引起内存泄漏的问题。
 *
 * @author xcc
 * @date 2021/10/15
 */
abstract class ViewAdapter<VH : ViewHolder>(
    private var currentAsItem: Boolean = false
) : Adapter<VH>(), SpanSizeProvider {
    private val controller = ViewController()
    private var previousAsItem = currentAsItem
    protected val recyclerView: RecyclerView?
        get() = controller.recyclerView

    final override fun getItemCount(): Int = if (currentAsItem) 1 else 0

    final override fun getItemViewType(position: Int): Int = getItemViewType()

    final override fun fullSpan(position: Int, holder: RecyclerView.ViewHolder): Boolean = true

    final override fun getSpanSize(position: Int, spanCount: Int): Int = spanCount

    final override fun onBindViewHolder(holder: VH, position: Int, payloads: List<Any>) {
        controller.onBindViewHolder(holder)
        onBindViewHolder(holder, payloads)
    }

    final override fun onBindViewHolder(holder: VH, position: Int) {
        controller.onBindViewHolder(holder)
        onBindViewHolder(holder)
    }

    protected open fun onBindViewHolder(holder: VH, payloads: List<Any>) {
        onBindViewHolder(holder)
    }

    protected open fun onBindViewHolder(holder: VH) {
    }

    /**
     * 返回唯一的ViewType值，例如[View.hashCode]
     */
    protected abstract fun getItemViewType(): Int

    @CallSuper
    override fun onViewRecycled(holder: VH) {
        controller.onViewRecycled(holder)
    }

    @CallSuper
    override fun onFailedToRecycleView(holder: VH): Boolean {
        return controller.onFailedToRecycleView(holder)
    }

    @CallSuper
    override fun onViewAttachedToWindow(holder: VH) {
        spanSizeProvider.onViewAttachedToWindow(holder)
    }

    @CallSuper
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        controller.onAttachedToRecyclerView(recyclerView)
        spanSizeProvider.onAttachedToRecyclerView(recyclerView)
    }

    @CallSuper
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        controller.onDetachedFromRecyclerView(recyclerView)
    }

    /**
     * 更新item的显示情况
     *
     * @param show 是否显示item，true-添加或更新item，false-移除item
     * @param anim 支持的动画，详细描述[NeedAnim]
     */
    protected fun updateItem(show: Boolean, anim: NeedAnim = NeedAnim.ALL) {
        currentAsItem = show
        when {
            !previousAsItem && currentAsItem -> when (anim) {
                NeedAnim.ALL,
                NeedAnim.NOT_CHANGE -> notifyItemInserted(0)
                NeedAnim.NOT_ALL -> withoutAnim { notifyItemInserted(0) }
            }
            previousAsItem && !currentAsItem -> when (anim) {
                NeedAnim.ALL,
                NeedAnim.NOT_CHANGE -> notifyItemRemoved(0)
                NeedAnim.NOT_ALL -> withoutAnim { notifyItemRemoved(0) }
            }
            previousAsItem && currentAsItem -> when (anim) {
                NeedAnim.ALL -> notifyItemChanged(0)
                NeedAnim.NOT_CHANGE,
                NeedAnim.NOT_ALL -> notifyItemChanged(0, this)
            }
        }
        previousAsItem = currentAsItem
    }

    private inline fun withoutAnim(block: () -> Unit) {
        block()
        val itemAnimator = recyclerView?.itemAnimator ?: return
        recyclerView?.doOnPreDraw {
            controller.viewHolder?.let(itemAnimator::endAnimation)
        }
    }

    /**
     * 需要Item动画
     */
    protected enum class NeedAnim {
        /**
         * 需要全部类型的item动画
         */
        ALL,

        /**
         * 仅不需要Change类型的item动画
         */
        NOT_CHANGE,

        /**
         * 不需要全部类型的item动画
         */
        NOT_ALL
    }

    @VisibleForTesting
    internal fun getRecycledViewHolder(): RecyclerView.ViewHolder? {
        return controller.getRecycledViewHolder()
    }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)
}