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

@file:JvmName("OptimizeScrollInternalKt")
@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.view.View
import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.cxrv.internal.doOnPreDraw

/**
 * 调用[RecyclerView.scrollToPosition]之后，调用该函数，
 * 减少下一帧[RecyclerView]布局的耗时，适用于连续绘制场景。
 */
fun RecyclerView.optimizeNextFrameScroll() {
    val original = getViewCacheExtensionOrNull()
    if (original is GetScrapOrCachedViewForType) return
    setViewCacheExtension(GetScrapOrCachedViewForType(this, original))
    doOnPreDraw { setViewCacheExtension(original) }
}

private class GetScrapOrCachedViewForType(
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