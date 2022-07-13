@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView.*
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool.ScrapData
import androidx.recyclerview.widget.ViewController.Tracker

/**
 * 从[Recycler]中清除ViewHolder的控制器
 *
 * ### [Adapter.onDetachedFromRecyclerView]
 * 当Adapter从RecyclerView上分离时，清除已分离的[Tracker.viewHolder]，
 * 若Adapter是被[ConcatAdapter]移除，则需要拦截要被回收的[Tracker.viewHolder]，
 * 拦截流程为[makeViewHolderRecycleFailed]和[onFailedToRecycleView]。
 *
 * ### [View.OnAttachStateChangeListener.onViewDetachedFromWindow]
 * 当RecyclerView从Window上分离时，清除已分离的[Tracker.viewHolder]，
 * 避免共享[RecycledViewPool]的场景回收无用的[Tracker.viewHolder]。
 *
 * @author xcc
 * @date 2021/10/15
 */
internal class ViewController {
    private val tracker = Tracker()
    private var View.hasTransientState: Boolean
        get() = ViewCompat.hasTransientState(this)
        set(value) = ViewCompat.setHasTransientState(this, value)
    val viewHolder: ViewHolder?
        get() = tracker.viewHolder
    val recyclerView: RecyclerView?
        get() = tracker.recyclerView

    fun onBindViewHolder(holder: ViewHolder) {
        tracker.track(holder)
    }

    fun onViewRecycled(holder: ViewHolder) {
        tracker.untrack(holder)
        recyclerView?.let(tracker::limitMaxScrap)
    }

    fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        tracker.track(recyclerView)
    }

    fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        tracker.untrack(recyclerView)
        recyclerView.clearDetachedViewHolder()
        tracker.viewHolder?.let { holder ->
            tracker.untrack(holder)
            makeViewHolderRecycleFailed(holder)
        }
    }

    /**
     * 让[holder]回收失败
     *
     * 在[onFailedToRecycleView]中对[holder]的状态做进一步处理。
     *
     * **注意**：在[Recycler.addViewHolderToRecycledViewPool]的流程中，
     * 虽然可以通过[RecyclerListener]或者[onViewRecycled]，将回收上限设为0，
     * 防止[holder]被回收，但这种处理方式仍然会创建[ScrapData]，导致清除的不够彻底。
     */
    private fun makeViewHolderRecycleFailed(holder: ViewHolder) {
        if (tracker.isAttached) return
        // 注意：holder.setIsRecyclable()是计数逻辑，
        // 若此处调用holder.setIsRecyclable(false)，
        // 则移除动画结束时，RecyclerView不会移除itemView。
        holder.itemView.apply {
            if (hasTransientState) {
                // 可能正在执行属性动画，先取消动画
                animate().cancel()
            }
            if (!hasTransientState) {
                // animate()动画结束时，会把hasTransientState设为false，
                // 将hasTransientState重新设为true，让holder回收失败。
                hasTransientState = true
            }
        }
    }

    /**
     * 若是[makeViewHolderRecycleFailed]导致的回收失败，
     * 则对[holder]的状态做进一步处理，防止[holder]被回收。
     *
     * 回收失败的详细流程[Recycler.recycleViewHolderInternal]。
     */
    fun onFailedToRecycleView(holder: ViewHolder): Boolean {
        if (tracker.isAttached) return false
        holder.itemView.apply {
            if (hasTransientState) {
                hasTransientState = false
            }
        }
        // 将holder设为不可回收，防止hasTransientState重置为false后，
        // holder.isRecyclable()为true，导致holder仍然可以被回收。
        holder.setIsRecyclable(false)
        return false
    }

    /**
     * 清除已分离的[Tracker.viewType]和[Tracker.viewHolder]
     *
     * **注意**：调用[RecycledViewPool.setMaxRecycledViews]，
     * 将`max`设为0无法清除[RecycledViewPool.ScrapData]，
     * 因此直接访问[RecycledViewPool.mScrap]进行清除。
     */
    private fun RecyclerView.clearDetachedViewHolder() {
        recycledViewPool.mScrap.remove(tracker.viewType)
        val holder = tracker.viewHolder
        if (holder == null || holder.itemView.isAttachedToWindow) {
            return
        }
        val views: ArrayList<ViewHolder> = when {
            !holder.isScrap -> mRecycler.mCachedViews
            !holder.mInChangeScrap -> mRecycler.mAttachedScrap
            else -> mRecycler.mChangedScrap
        } ?: return
        views.remove(holder)
    }

    @VisibleForTesting
    fun getRecycledViewHolder(): ViewHolder? {
        val mScrapHeap = recyclerView
            ?.recycledViewPool?.mScrap?.get(tracker.viewType)
            ?.mScrapHeap.takeIf { !it.isNullOrEmpty() } ?: return null
        require(mScrapHeap.size == 1)
        return mScrapHeap.first()
    }

    @Suppress("SpellCheckingInspection")
    private inner class Tracker : View.OnAttachStateChangeListener {
        /**
         * 由于[ViewHolder.setIsRecyclable]是计数逻辑，因此用该属性确保逻辑对称
         */
        private var isDetachedRecyclable = false

        /**
         * 当[ConcatAdapter]使用隔离ViewType配置时，[Adapter.getItemViewType]获取到的是本地ViewType，
         * 并不是[RecycledViewPool]中的ViewType，因此通过[ViewHolder.getItemViewType]获取全局ViewType。
         */
        var viewType = INVALID_TYPE
            private set
        var viewHolder: ViewHolder? = null
            private set
        var recyclerView: RecyclerView? = null
            private set
        val isAttached: Boolean
            get() = recyclerView != null

        fun track(holder: ViewHolder) {
            viewType = holder.itemViewType
            viewHolder = holder
        }

        fun untrack(holder: ViewHolder) {
            // 保留viewType，用于后续清除Scrap
            viewType = holder.itemViewType
            viewHolder = null
        }

        fun track(recyclerView: RecyclerView) {
            this.recyclerView = recyclerView
            recyclerView.addOnAttachStateChangeListener(this)
        }

        fun untrack(recyclerView: RecyclerView) {
            this.recyclerView = null
            recyclerView.removeOnAttachStateChangeListener(this)
        }

        /**
         * 将[viewType]的回收上限设为1，防止回收多余的ViewHolder
         */
        fun limitMaxScrap(rv: RecyclerView) {
            if (viewType == INVALID_TYPE) return
            rv.recycledViewPool.setMaxRecycledViews(viewType, /*max*/1)
        }

        override fun onViewAttachedToWindow(view: View) {
            if (isDetachedRecyclable) {
                isDetachedRecyclable = false
                viewHolder?.setIsRecyclable(true)
            }
        }

        override fun onViewDetachedFromWindow(view: View) {
            if (!isDetachedRecyclable && viewHolder?.isRecyclable == true) {
                isDetachedRecyclable = true
                viewHolder?.setIsRecyclable(false)
            }
            (view as? RecyclerView)?.clearDetachedViewHolder()
        }
    }
}