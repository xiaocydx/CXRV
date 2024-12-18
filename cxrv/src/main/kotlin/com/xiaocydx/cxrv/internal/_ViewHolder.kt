/*
 * Copyright 2022 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JvmName("_ViewHolderInternalKt")
@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.*
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

internal fun RecyclerView.clearPendingUpdates() {
    mAdapterHelper.takeIf { it.hasPendingUpdates() }?.reset()
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

/**
 * ```
 * public final class Recycler {
 *     private ViewCacheExtension mViewCacheExtension;
 * }
 * ```
 */
private val mViewCacheExtensionField by lazy {
    runCatching {
        val clazz = Class.forName("androidx.recyclerview.widget.RecyclerView\$Recycler")
        clazz.getDeclaredField("mViewCacheExtension").apply { isAccessible = true }
    }.getOrNull()
}

internal fun RecyclerView.getViewCacheExtensionOrNull(): ViewCacheExtension? {
    return runCatching { mViewCacheExtensionField?.get(mRecycler) }.getOrNull() as? ViewCacheExtension
}