@file:JvmName("_ViewHolderInternalKt")
@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.view.View
import android.view.ViewGroup
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

internal class SimpleViewHolder(itemView: View) : ViewHolder(itemView) {

    fun isNotReuseUpdatedViewHolder(parent: ViewGroup): Boolean {
        val rv = parent as? RecyclerView ?: return false
        val changedScrap = rv.mRecycler?.mChangedScrap
        // changedScrap.isEmpty()为true时，不创建迭代器
        if (changedScrap.isNullOrEmpty()) return false
        return changedScrap.firstOrNull { it.itemView === itemView } != null
    }
}