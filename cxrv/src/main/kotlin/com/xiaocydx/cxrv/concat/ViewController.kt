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

@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView.*
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool.ScrapData
import androidx.recyclerview.widget.ViewController.Tracker
import com.xiaocydx.cxrv.R
import java.lang.ref.WeakReference

/**
 * 从[Recycler]中清除ViewHolder的控制器
 *
 * ### [ViewController.onDetachedFromRecyclerView]
 * 当Adapter从RecyclerView分离时，清除缓存中已分离的[Tracker.viewHolder]，
 * 若Adapter是从[ConcatAdapter]移除，则拦截将被回收的[Tracker.viewHolder]，
 * 拦截流程为[makeViewHolderRecycleFailed]和[onFailedToRecycleView]。
 *
 * ### [Tracker.onViewDetachedFromWindow]
 * 当RecyclerView从Window分离时，清除已分离的[Tracker.viewHolder]，
 * 避免共享[RecycledViewPool]的场景回收无用的[Tracker.viewHolder]。
 *
 * @author xcc
 * @date 2021/10/15
 */
internal class ViewController {
    private val tracker = Tracker()
    val viewHolder: ViewHolder?
        get() = tracker.viewHolder
    val recyclerView: RecyclerView?
        get() = tracker.recyclerView
    val itemCount = 1

    fun onBindViewHolder(holder: ViewHolder) {
        tracker.track(holder)
    }

    fun onViewRecycled(holder: ViewHolder) {
        tracker.untrack(holder)
        tracker.recyclerView?.let(tracker::limitMaxScrap)
    }

    fun onFailedToRecycleView(holder: ViewHolder): Boolean {
        tracker.untrack(holder)
        if (clearViewHolderRecycleFailed(holder)) {
            // 避免hasTransientState重置为false后，holder仍被回收
            holder.setIsRecyclable(false)
        }
        // holder.itemView.animate()导致的回收失败，不做任何处理
        return false
    }

    fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        tracker.track(recyclerView)
    }

    fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        recyclerView.clearDetachedViewHolder()
        tracker.viewHolder?.let(::makeViewHolderRecycleFailed)
        tracker.untrack(recyclerView)
        tracker.viewHolder?.let(tracker::untrack)
    }

    /**
     * 对[holder]设置回收失败标志，使得回收流程无法回收[holder]
     *
     * 在[onFailedToRecycleView]中对[holder]的状态做进一步处理。
     *
     * **注意**：在[Recycler.addViewHolderToRecycledViewPool]的分发过程中，
     * 虽然可以通过[RecyclerListener]或者[onViewRecycled]，将回收上限设为0，
     * 避免[holder]被回收，但是这种处理方式会创建[ScrapData]，清除的不够彻底。
     */
    private fun makeViewHolderRecycleFailed(holder: ViewHolder) {
        // 注意：ViewHolder.setIsRecyclable()是计数逻辑，
        // 若此处调用ViewHolder.setIsRecyclable(false)，
        // 则移除动画结束时，RecyclerView不会移除itemView。
        if (!hasTransientState(holder)) setHasTransientState(holder, true)
    }

    /**
     * 清除[holder]的回收失败标志
     *
     * @return 若调用过[makeViewHolderRecycleFailed]，则返回`true`
     */
    private fun clearViewHolderRecycleFailed(holder: ViewHolder): Boolean {
        val hasTransientState = hasTransientState(holder)
        if (hasTransientState) setHasTransientState(holder, false)
        return hasTransientState
    }

    /**
     * 清除已分离的[Tracker.viewType]和[Tracker.viewHolder]
     *
     * **注意**：调用[RecycledViewPool.setMaxRecycledViews]，
     * 将参数`max`设为0无法清除[RecycledViewPool.ScrapData]，
     * 因此直接访问[RecycledViewPool.mScrap]进行清除。
     */
    private fun RecyclerView.clearDetachedViewHolder() {
        recycledViewPool.mScrap.remove(tracker.viewType)
        val holder = tracker.viewHolder
        if (holder == null || holder.itemView.isAttachedToWindow) {
            return
        }
        val views: ArrayList<ViewHolder> = when {
            !holder.isScrap -> mRecycler.mCachedViews
            !holder.mInChangeScrap -> mRecycler.mAttachedScrap
            else -> mRecycler.mChangedScrap
        } ?: return
        views.remove(holder)
    }

    private fun hasTransientState(holder: ViewHolder): Boolean {
        val hasTransientState = holder.itemView.getTag(R.id.tag_view_has_transient_state)
        return (hasTransientState as? Boolean) == true
    }

    private fun setHasTransientState(holder: ViewHolder, value: Boolean) {
        holder.itemView.setTag(R.id.tag_view_has_transient_state, value)
        // 注意：View.setHasTransientState()是计数逻辑
        ViewCompat.setHasTransientState(holder.itemView, value)
    }

    @VisibleForTesting
    fun getRecycledViewHolder(): ViewHolder? {
        val mScrapHeap = tracker.recyclerView
            ?.recycledViewPool?.mScrap?.get(tracker.viewType)
            ?.mScrapHeap.takeIf { !it.isNullOrEmpty() } ?: return null
        require(mScrapHeap.size == 1)
        return mScrapHeap.first()
    }

    @Suppress("SpellCheckingInspection")
    private inner class Tracker : View.OnAttachStateChangeListener {
        private var viewHolderRef: WeakReference<ViewHolder>? = null

        /**
         * 当[ConcatAdapter]使用隔离ViewType配置时，[Adapter.getItemViewType]获取到的是本地ViewType，
         * 并不是[RecycledViewPool]中的ViewType，因此通过[ViewHolder.getItemViewType]获取全局ViewType。
         */
        var viewType = INVALID_TYPE
            private set
        var recyclerView: RecyclerView? = null
            private set
        val viewHolder: ViewHolder?
            get() = viewHolderRef?.get()

        fun track(holder: ViewHolder) {
            viewType = holder.itemViewType
            viewHolderRef = WeakReference(holder)
        }

        fun untrack(holder: ViewHolder) {
            // 保留viewType，用于后续清除Scrap
            viewType = holder.itemViewType
            viewHolderRef = null
        }

        fun track(recyclerView: RecyclerView) {
            assert(this.recyclerView == null)
            this.recyclerView = recyclerView
            recyclerView.addOnAttachStateChangeListener(this)
        }

        fun untrack(recyclerView: RecyclerView) {
            assert(this.recyclerView != null)
            this.recyclerView = null
            recyclerView.removeOnAttachStateChangeListener(this)
        }

        fun limitMaxScrap(rv: RecyclerView) {
            // 将viewType的回收上限设为1，避免回收多余的ViewHolder
            if (viewType == INVALID_TYPE) return
            rv.recycledViewPool.setMaxRecycledViews(viewType, /*max*/1)
        }

        override fun onViewAttachedToWindow(view: View) {
            // 若在RecyclerView分离期间，viewHolder没有被回收，则清除回收失败标志
            viewHolder?.let(::clearViewHolderRecycleFailed)
        }

        override fun onViewDetachedFromWindow(view: View) {
            // 当RecyclerView从Window分离时，例如ViewPager2嵌套RecyclerView的滚动过程，
            // 清除已分离的viewHolder，或者让viewHolder在RecyclerView分离期间回收失败。
            (view as? RecyclerView)?.clearDetachedViewHolder()
            viewHolder?.let(::makeViewHolderRecycleFailed)
        }
    }
}