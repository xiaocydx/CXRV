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
    setViewCacheExtension(GetScrapOrCachedViewForTypeExtension(original))
    // resume original
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
    private val original: ViewCacheExtension?
) : ViewCacheExtension() {

    override fun getViewForPositionAndType(recycler: Recycler, position: Int, type: Int): View? {
        val holder = recycler.getScrapOrCachedViewForType(type)
        if (holder != null && holder.checkLayoutParams()) {
            holder.ensureNextStepBind()
            return holder.itemView
        }
        return original?.getViewForPositionAndType(recycler, position, type)
    }

    private fun Recycler.getScrapOrCachedViewForType(type: Int): ViewHolder? {
        for (index in (mAttachedScrap.size - 1) downTo 0) {
            val holder = mAttachedScrap[index]
            if (holder.itemViewType == type && !holder.wasReturnedFromScrap()) {
                holder.addFlags(ViewHolder.FLAG_RETURNED_FROM_SCRAP)
                return holder
            }
        }

        for (index in (mCachedViews.size - 1) downTo 0) {
            val holder = mCachedViews[index]
            if (holder.itemViewType == type && !holder.isAttachedToTransitionOverlay) {
                mCachedViews.removeAt(index)
                return holder
            }
        }
        return null
    }

    private fun ViewHolder.checkLayoutParams(): Boolean {
        return itemView.layoutParams is LayoutParams
    }

    private fun ViewHolder.ensureNextStepBind() {
        if (!isBound || needsUpdate() || isInvalid) return
        addFlags(ViewHolder.FLAG_UPDATE)
    }
}