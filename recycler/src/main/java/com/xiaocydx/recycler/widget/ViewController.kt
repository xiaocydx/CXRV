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
 * 当Adapter从RecyclerView上分离时，尝试清除已被回收的ViewHolder，
 * 若Adapter是被[ConcatAdapter]移除，则需要拦截要被回收的ViewHolder，
 * 拦截流程为[tryMakeViewHolderRecycleFailed]和[onFailedToRecycleView]。
 *
 * ### [onViewDetachedFromWindow]
 * 当RecyclerView从Window上分离时，尝试清除已被回收的ViewHolder，
 * 避免共享[RecycledViewPool]的场景回收无用的ViewHolder。
 *
 * @author xcc
 * @date 2021/10/15
 */
internal class ViewController : View.OnAttachStateChangeListener {
    private var viewType = INVALID_TYPE
    private var viewHolder: ViewHolder? = null
    private var View.hasTransientState: Boolean
        get() = ViewCompat.hasTransientState(this)
        set(value) = ViewCompat.setHasTransientState(this, value)
    private val isAttached: Boolean
        get() = recyclerView != null
    var recyclerView: RecyclerView? = null
        private set

    fun onBindViewHolder(holder: ViewHolder) {
        viewHolder = holder
        saveItemViewType(holder)
        recyclerView?.setMaxRecycledViews()
    }

    fun onViewRecycled(holder: ViewHolder) {
        viewHolder = null
        saveItemViewType(holder)
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
            it.tryMakeViewHolderRecycleFailed()
        }
    }

    override fun onViewAttachedToWindow(view: View) {
    }

    override fun onViewDetachedFromWindow(view: View) {
        (view as? RecyclerView)?.tryClearRecycledViewHolder()
    }

    /**
     * 当[ConcatAdapter]使用隔离ViewType配置时，[Adapter.getItemViewType]获取到的是本地ViewType，
     * 并不是[RecycledViewPool]中的ViewType，因此通过[ViewHolder.getItemViewType]获取全局ViewType。
     */
    private fun saveItemViewType(holder: ViewHolder) {
        viewType = holder.itemViewType
    }

    /**
     * 将ViewHolder回收上限设为1，防止回收多余的ViewHolder
     */
    private fun RecyclerView.setMaxRecycledViews() {
        recycledViewPool.setMaxRecycledViews(viewType, 1)
    }

    /**
     * 尝试清除已被回收的ViewHolder
     *
     * 因为调用[RecycledViewPool.setMaxRecycledViews]，将max设为0无法清除[ScrapData]，
     * 所以直接访问[RecycledViewPool.mScrap]，清除[viewType]对应的[ScrapData]。
     */
    private fun RecyclerView.tryClearRecycledViewHolder() {
        recycledViewPool.mScrap.remove(viewType)
    }

    /**
     * 尝试让ViewHolder回收失败
     *
     * 若尝试成功，则会在[onFailedToRecycleView]中对ViewHolder的状态做进一步处理。
     *
     * **注意**：在[Recycler.addViewHolderToRecycledViewPool]的流程中，
     * 虽然可以通过[RecyclerListener]或者[onViewRecycled]，将ViewHolder回收上限设为0，
     * 防止ViewHolder被回收，但是这种处理方式仍然会创建[ScrapData]，导致清除的不够干净。
     */
    private fun RecyclerView.tryMakeViewHolderRecycleFailed() {
        val holder = viewHolder
        if (isAttached || holder == null) {
            // holder为空表示已被回收，不需要进行拦截
            return
        }
        if (mRecycler.mCachedViews.remove(holder)) {
            // 尝试从mCachedViews中移除holder，防止被回收
            viewHolder = null
            return
        }
        // 注意：holder.setIsRecyclable()不是纯粹的状态设置，而是包含计数逻辑，
        // 若此处调用holder.setIsRecyclable(false)，则移除动画结束时，将不会移除itemView。
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
        viewHolder = null
    }

    /**
     * 若是[tryMakeViewHolderRecycleFailed]导致的回收失败，
     * 则对[holder]的状态做进一步处理，防止[holder]被回收。
     *
     * 回收失败的详细流程[Recycler.recycleViewHolderInternal]。
     */
    fun onFailedToRecycleView(holder: ViewHolder): Boolean {
        if (isAttached) {
            return false
        }
        holder.itemView.apply {
            if (hasTransientState) {
                hasTransientState = false
            }
        }
        // 将holder设为不可回收，是为了防止hasTransientState重置为false后，
        // holder.isRecyclable()为true，导致仍然可以回收viewHolder。
        holder.setIsRecyclable(false)
        return false
    }

    inline fun withoutAnim(block: () -> Unit) {
        block()
        val itemAnimator = recyclerView?.itemAnimator ?: return
        recyclerView?.post { viewHolder?.let(itemAnimator::endAnimation) }
    }

    @VisibleForTesting
    fun getRecycledViewHolder(): ViewHolder? {
        val mScrapHeap = recyclerView
            ?.recycledViewPool?.mScrap?.get(viewType)
            ?.mScrapHeap.takeIf { !it.isNullOrEmpty() } ?: return null
        require(mScrapHeap.size == 1)
        return mScrapHeap.first()
    }
}