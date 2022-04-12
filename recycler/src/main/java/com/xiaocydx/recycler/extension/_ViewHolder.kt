@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView.LayoutParams
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.recycler.concat.ViewAdapter

internal val View.holder: ViewHolder?
    get() = (layoutParams as? LayoutParams)?.mViewHolder

internal val ViewHolder.payloads: List<Any>
    get() = unmodifiedPayloads

internal val RecyclerView.isPreLayout: Boolean
    get() = mState.isPreLayout

internal val RecyclerView.cacheViews: List<ViewHolder>
    get() = mRecycler.mCachedViews

internal fun RecyclerView.isHeaderOrFooterOrRemoved(child: View): Boolean {
    val holder = getChildViewHolder(child) ?: return false
    return holder.bindingAdapter is ViewAdapter<*> || holder.isRemoved
}

internal fun RecyclerView.isViewHolderRemoved(child: View): Boolean {
    return getChildViewHolder(child)?.isRemoved ?: return false
}

internal fun View.isTouched(rawX: Float, rawY: Float): Boolean {
    if (!isVisible) {
        return false
    }
    val location = IntArray(2)
    this.getLocationOnScreen(location)
    val left = location[0]
    val top = location[1]
    val right = left + this.width
    val bottom = top + this.height
    val isContainX = rawX in left.toFloat()..right.toFloat()
    val isContainY = rawY in top.toFloat()..bottom.toFloat()
    return isContainX && isContainY
}