@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.view.View
import androidx.recyclerview.widget.RecyclerView.LayoutParams
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.cxrv.concat.ViewAdapter

internal val View.holder: ViewHolder?
    get() = (layoutParams as? LayoutParams)?.mViewHolder

internal val ViewHolder.payloads: List<Any>
    get() = unmodifiedPayloads ?: emptyList()

internal val RecyclerView.isPreLayout: Boolean
    get() = mState?.isPreLayout == true

internal val RecyclerView.cacheViews: List<ViewHolder>
    get() = mRecycler?.mCachedViews ?: emptyList()

internal fun RecyclerView.isHeaderOrFooterOrRemoved(child: View): Boolean {
    val holder = getChildViewHolder(child) ?: return false
    return holder.bindingAdapter is ViewAdapter<*> || holder.isRemoved
}

internal fun RecyclerView.isViewHolderRemoved(child: View): Boolean {
    return getChildViewHolder(child)?.isRemoved ?: return false
}

@Suppress("FunctionName")
internal fun SimpleViewHolder(itemView: View): ViewHolder {
    return object : ViewHolder(itemView) {}
}