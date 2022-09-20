@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.view.View
import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.cxrv.internal.doOnPreDraw
import java.lang.reflect.Field

/**
 * 调用[RecyclerView.scrollToPosition]之后，调用该函数，
 * 减少下一帧[RecyclerView]布局的耗时，适用于联动性交互场景。
 */
fun RecyclerView.optimizeNextFrameScroll() {
    // reflect < 1ms
    val original = getViewCacheExtensionOrNull()
    setViewCacheExtension(GetScrapOrCachedViewForTypeExtension(this, original))
    doOnPreDraw { setViewCacheExtension(original) }
}

private fun RecyclerView.getViewCacheExtensionOrNull(): ViewCacheExtension? {
    val mViewCacheExtensionField: Field = try {
        mRecycler.javaClass.getDeclaredField("mViewCacheExtension")
    } catch (e: NoSuchFieldException) {
        return null
    }
    mViewCacheExtensionField.isAccessible = true
    return mViewCacheExtensionField.get(mRecycler) as? ViewCacheExtension
}

private class GetScrapOrCachedViewForTypeExtension(
    private val recyclerView: RecyclerView,
    private val original: ViewCacheExtension?
) : ViewCacheExtension() {
    /**
     * 若存在preLayout，则不做处理，避免影响更新流程
     */
    private var hasPreLayout = false

    override fun getViewForPositionAndType(recycler: Recycler, position: Int, type: Int): View? {
        hasPreLayout = hasPreLayout || recycler.isScrapInPreLayout()
        val holder = if (!hasPreLayout) recycler.getScrapOrCachedViewForType(type) else null
        holder?.ensureCallTryBindViewHolderByDeadline()
        return holder?.itemView ?: original?.getViewForPositionAndType(recycler, position, type)
    }

    /**
     * scrap是否有存在于preLayout的[ViewHolder]
     */
    private fun Recycler.isScrapInPreLayout(): Boolean {
        val changedScrap = mChangedScrap ?: emptyList<ViewHolder>()
        for (index in changedScrap.indices) {
            val holder = changedScrap[index]
            if (recyclerView.mViewInfoStore.isInPreLayout(holder)) return true
        }

        val attachedScrap = mAttachedScrap ?: emptyList<ViewHolder>()
        for (index in attachedScrap.indices) {
            val holder = attachedScrap[index]
            if (recyclerView.mViewInfoStore.isInPreLayout(holder)) return true
        }
        return false
    }

    /**
     * 实现逻辑参考自[Recycler.getScrapOrHiddenOrCachedHolderForPosition]
     */
    private fun Recycler.getScrapOrCachedViewForType(type: Int): ViewHolder? {
        val attachedScrap = mAttachedScrap ?: emptyList<ViewHolder>()
        for (index in attachedScrap.indices) {
            val holder = attachedScrap[index]
            if (holder.checkLayoutParams()
                    && !holder.wasReturnedFromScrap()
                    && holder.itemViewType == type
                    && !holder.isInvalid && !holder.isRemoved) {
                holder.addFlags(ViewHolder.FLAG_RETURNED_FROM_SCRAP)
                return holder
            }
        }

        val cachedViews = mCachedViews ?: emptyList<ViewHolder>()
        for (index in cachedViews.indices) {
            val holder = cachedViews[index]
            if (holder.checkLayoutParams()
                    && !holder.isInvalid
                    && holder.itemViewType == type
                    && !holder.isAttachedToTransitionOverlay) {
                mCachedViews?.removeAt(index)
                return holder
            }
        }
        return null
    }

    /**
     * 确保有[LayoutParams]，避免后续流程抛出异常
     */
    private fun ViewHolder.checkLayoutParams(): Boolean {
        return itemView.layoutParams is LayoutParams
    }

    /**
     * 确保后续流程调用[Recycler.tryBindViewHolderByDeadline]
     */
    private fun ViewHolder.ensureCallTryBindViewHolderByDeadline() {
        if (!isBound || needsUpdate() || isInvalid) return
        addFlags(ViewHolder.FLAG_UPDATE)
    }
}