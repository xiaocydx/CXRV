@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView.*
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool.ScrapData

/**
 * 从[RecycledViewPool]中清除ViewHolder的控制器
 *
 * ### [onDetachedFromRecyclerView]
 * Adapter从RecyclerView上分离时，尝试清除已回收的ViewHolder，
 * 若Adapter是从[ConcatAdapter]中被移除，则需要阻止回收ViewHolder。
 *
 * ### [onViewDetachedFromWindow]
 * RecyclerView从Window上分离时，尝试清除已回收的ViewHolder，
 * 避免共享[RecycledViewPool]的场景回收无用的ViewHolder。
 *
 * @author xcc
 * @date 2021/10/15
 */
internal class ViewController : View.OnAttachStateChangeListener {
    private var itemViewType = INVALID_TYPE
    private var viewHolder: ViewHolder? = null
    private var View.hasTransientState: Boolean
        get() = ViewCompat.hasTransientState(this)
        set(value) = ViewCompat.setHasTransientState(this, value)
    private val isRemovedAdapter: Boolean
        get() = recyclerView == null
    var recyclerView: RecyclerView? = null
        private set

    /**
     * 当[ConcatAdapter]使用隔离ViewType配置时，[Adapter.getItemViewType]获取到的是本地ViewType，
     * 并不是[RecycledViewPool]中的ViewType，因此通过[ViewHolder.getItemViewType]获取准确的ViewType。
     */
    fun onBindViewHolder(holder: ViewHolder) {
        itemViewType = holder.itemViewType
        viewHolder = holder
        recyclerView?.trySetMaxRecycledViews()
    }

    /**
     * 当[ConcatAdapter]使用隔离ViewType配置时，[Adapter.getItemViewType]获取到的是本地ViewType，
     * 并不是[RecycledViewPool]中的ViewType，因此通过[ViewHolder.getItemViewType]获取准确的ViewType。
     */
    fun onViewRecycled(holder: ViewHolder) {
        itemViewType = holder.itemViewType
        viewHolder = null
    }

    /**
     * 若是[trySetViewHolderNotRecyclable]触发的[holder]回收失败，则对状态进行处理。
     *
     * [holder]回收失败的详细流程[Recycler.recycleViewHolderInternal]。
     */
    fun onFailedToRecycleView(holder: ViewHolder): Boolean {
        if (!isRemovedAdapter) {
            return false
        }
        holder.itemView.apply {
            if (hasTransientState) {
                hasTransientState = false
            }
        }
        // 将holder设为不可回收，是为了防止hasTransientState重置为false后，
        // holder.isRecyclable表示可回收，导致仍然回收viewHolder。
        holder.setIsRecyclable(false)
        return false
    }

    fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        recyclerView.addOnAttachStateChangeListener(this)
    }

    fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
        recyclerView.also {
            it.removeOnAttachStateChangeListener(this)
            it.tryClearRecycledViewHolder()
            it.trySetViewHolderNotRecyclable()
        }
    }

    override fun onViewAttachedToWindow(view: View) {
    }

    override fun onViewDetachedFromWindow(view: View) {
        (view as? RecyclerView)?.tryClearRecycledViewHolder()
    }

    /**
     * 尝试设置回收池保留的最大ViewHolder数
     *
     * 将最大数设为1，避免保存多余的ViewHolder。
     */
    private fun RecyclerView.trySetMaxRecycledViews() {
        recycledViewPool.setMaxRecycledViews(itemViewType, 1)
    }

    /**
     * 尝试清除回收池保留的ViewHolder
     *
     * 因为调用[RecycledViewPool.setMaxRecycledViews]，将max设为0无法清除[ScrapData]，
     * 所以直接访问[RecycledViewPool.mScrap]，清除[itemViewType]对应的[ScrapData]。
     */
    private fun RecyclerView.tryClearRecycledViewHolder() {
        recycledViewPool.mScrap.remove(itemViewType)
    }

    /**
     * 尝试将[viewHolder]设为不可回收状态
     *
     * 若[viewHolder]被设为不可回收状态，则会在[onFailedToRecycleView]中对状态进行处理。
     */
    private fun RecyclerView.trySetViewHolderNotRecyclable() {
        if (!isRemovedAdapter || viewHolder == null) {
            // viewHolder为空表示已被回收，不需要再将holder设为不可回收状态
            return
        }
        val holder = viewHolder!!
        if (mRecycler.mCachedViews.remove(holder)) {
            // 尝试从mCachedViews中移除holder，防止被回收
            viewHolder = null
            return
        }
        // 若此处调用holder.setIsRecyclable(false)，
        // 将holder设为不可回收，则RecyclerView将不会移除itemView。
        holder.itemView.apply {
            if (hasTransientState) {
                // 可能正在执行属性动画，先取消动画
                animate().cancel()
            }
            if (!hasTransientState) {
                // animate()动画结束时，会把hasTransientState设为false，
                // 将hasTransientState重新设为true，让ViewHolder回收失败。
                hasTransientState = true
            }
        }
        viewHolder = null
    }

    inline fun withoutAnim(block: () -> Unit) {
        block()
        val itemAnimator = recyclerView?.itemAnimator ?: return
        recyclerView?.post { viewHolder?.let(itemAnimator::endAnimation) }
    }

    @VisibleForTesting
    fun getRecycledViewHolder(): ViewHolder? {
        val mScrapHeap = recyclerView
            ?.recycledViewPool?.mScrap?.get(itemViewType)
            ?.mScrapHeap.takeIf { !it.isNullOrEmpty() } ?: return null
        require(mScrapHeap.size == 1)
        return mScrapHeap.first()
    }
}