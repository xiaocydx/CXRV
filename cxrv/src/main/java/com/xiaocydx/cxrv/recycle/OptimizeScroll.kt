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
    if (original is GetScrapOrCachedViewForTypeExtension) return
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

    override fun getViewForPositionAndType(recycler: Recycler, position: Int, type: Int): View? {
        val runAnimations = recyclerView.mState.willRunSimpleAnimations()
        if (!runAnimations && recycler.recycleScrapOrCachedViewForType(type)) return null
        return original?.getViewForPositionAndType(recycler, position, type)
    }

    /**
     * 判断逻辑参考自[Recycler.getScrapOrHiddenOrCachedHolderForPosition]
     */
    private fun Recycler.recycleScrapOrCachedViewForType(type: Int): Boolean {
        val attachedScrap = mAttachedScrap ?: emptyList<ViewHolder>()
        for (index in attachedScrap.indices) {
            val holder = attachedScrap[index]
            if (!holder.wasReturnedFromScrap()
                    && holder.itemViewType == type
                    && !holder.isInvalid && !holder.isRemoved) {
                recycleViewHolderToRecycledViewPool(holder)
                return true
            }
        }

        val cachedViews = mCachedViews ?: emptyList<ViewHolder>()
        for (index in cachedViews.indices) {
            val holder = cachedViews[index]
            if (!holder.isInvalid
                    && holder.itemViewType == type
                    && !holder.isAttachedToTransitionOverlay) {
                mCachedViews?.removeAt(index)
                recycleViewHolderToRecycledViewPool(holder)
                return true
            }
        }
        return false
    }

    private fun Recycler.recycleViewHolderToRecycledViewPool(holder: ViewHolder) {
        if (!holder.isInvalid) {
            // 对holder添加FLAG_INVALID，将无法回收进离屏缓存，
            // 确保holder在下面第3步只回收进RecycledViewPool。
            holder.addFlags(ViewHolder.FLAG_INVALID)
        }
        // 1. 清除FLAG_TMP_DETACHED
        // 2. 从mAttachedScrap中移除
        // 3. 回收进RecycledViewPool
        recycleView(holder.itemView)
    }
}