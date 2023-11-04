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

import android.os.Parcelable
import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.cxrv.itemvisible.findFirstCompletelyVisibleItemPosition
import com.xiaocydx.cxrv.layout.callback.LayoutManagerCallback

/**
 * 往列表首位插入或移动item时，若当前首位完全可见，则滚动到更新后的首位
 *
 * @author xcc
 * @date 2022/8/12
 */
internal class ScrollToFirstOnUpdateHelper : LayoutManagerCallback {
    private var previousItemCount = 0
    private var adapter: Adapter<*>? = null
    private var layout: LayoutManager? = null
    private val view: RecyclerView?
        get() = layout?.mRecyclerView

    var isEnabled = true

    override fun onPreAttachedToWindow(view: RecyclerView) {
        val layout = view.layoutManager ?: return
        onPreAdapterChanged(layout, oldAdapter = adapter, newAdapter = view.adapter)
    }

    override fun onPreAdapterChanged(layout: LayoutManager, oldAdapter: Adapter<*>?, newAdapter: Adapter<*>?) {
        adapter = newAdapter
        this.layout = layout
        previousItemCount = layout.itemCount
    }

    override fun onPreItemsAdded(recyclerView: RecyclerView, positionStart: Int, itemCount: Int) {
        val view = view
        val layout = layout
        if (!isEnabled || view == null || layout == null) return
        if (positionStart == 0 && previousItemCount != 0
                && view.findFirstCompletelyVisibleItemPosition() == itemCount
                && !layout.hasPendingScrollPositionOrSavedState()) {
            view.scrollToPosition(0)
        }
    }

    @Suppress("KotlinConstantConditions")
    override fun onPreItemsMoved(recyclerView: RecyclerView, from: Int, to: Int, itemCount: Int) {
        val view = view
        val layout = layout
        if (!isEnabled || view == null || layout == null) return
        val isFirstCompletelyVisible = when {
            from == 0 && to > from -> view.findFirstCompletelyVisibleItemPosition() == to - from
            to == 0 && from > to -> view.findFirstCompletelyVisibleItemPosition() == itemCount
            else -> false
        }
        if (isFirstCompletelyVisible && !layout.hasPendingScrollPositionOrSavedState()) {
            view.scrollToPosition(0)
        }
    }

    override fun onPreLayoutCompleted(layout: LayoutManager, state: State) {
        previousItemCount = state.itemCount
    }

    override fun onCleared() {
        adapter = null
        layout = null
    }

    private fun LayoutManager.hasPendingScrollPositionOrSavedState() = when (this) {
        is LinearLayoutManager -> mPendingScrollPosition != NO_POSITION || mPendingSavedState != null
        is StaggeredGridLayoutManager -> mPendingScrollPosition != NO_POSITION || mPendingSavedState != null
        else -> true
    }

    private companion object {
        val mPendingSavedStateField = runCatching {
            StaggeredGridLayoutManager::class.java.getDeclaredField("mPendingSavedState")
        }.onSuccess { it.isAccessible = true }.getOrNull()

        val StaggeredGridLayoutManager.mPendingSavedState: Parcelable?
            get() = mPendingSavedStateField?.get(this) as? Parcelable
    }
}