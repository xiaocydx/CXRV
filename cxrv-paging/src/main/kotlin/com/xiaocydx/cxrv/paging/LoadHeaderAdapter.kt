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

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.xiaocydx.cxrv.paging

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.SimpleViewHolder
import com.xiaocydx.cxrv.concat.ViewAdapter
import com.xiaocydx.cxrv.internal.hasDisplayItem
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.ListChangedListener
import com.xiaocydx.cxrv.paging.LoadHeaderAdapter.Visible.EMPTY
import com.xiaocydx.cxrv.paging.LoadHeaderAdapter.Visible.FAILURE
import com.xiaocydx.cxrv.paging.LoadHeaderAdapter.Visible.LOADING
import com.xiaocydx.cxrv.paging.LoadHeaderAdapter.Visible.NONE

/**
 * 加载头部适配器
 *
 * @author xcc
 * @date 2021/9/17
 */
@PublishedApi
internal class LoadHeaderAdapter(
    private val config: LoadHeaderConfig,
    private val adapter: ListAdapter<*, *>
) : ViewAdapter<ViewHolder>(), LoadStatesListener, ListChangedListener<Any> {
    private var visible: Visible = NONE
    private var loadStates: LoadStates = LoadStates.Incomplete
    private val collector = adapter.pagingCollector

    init {
        config.complete(
            retry = collector::retry,
            exception = { collector.loadStates.exception }
        )
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        adapter.addListChangedListener(this)
        collector.addLoadStatesListener(this)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        adapter.removeListChangedListener(this)
        collector.removeLoadStatesListener(this)
    }

    override fun getItemViewType(): Int = hashCode()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LoadViewLayout(
            context = parent.context,
            width = config.width,
            height = config.height,
            loadingItem = config.loadingScope?.getViewItem(),
            successItem = config.emptyScope?.getViewItem(),
            failureItem = config.failureScope?.getViewItem()
        )
        return SimpleViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder) {
        val itemView = holder.itemView as LoadViewLayout
        when (visible) {
            LOADING -> itemView.loadingVisible()
            FAILURE -> itemView.failureVisible()
            EMPTY -> itemView.successVisible()
            NONE -> throw AssertionError("局部刷新出现断言异常")
        }
    }

    /**
     * 加载完毕后，在列表更改时更新空视图的显示情况
     *
     * **注意**：[onListChanged]在[onLoadStatesChanged]之前被调用。
     */
    override fun onListChanged(current: List<Any>) {
        when {
            !loadStates.isFully -> updateLoadHeader(loadStates.toVisible())
            // 此时EMPTY视图已显示，并且列表不为空
            visible == EMPTY && adapter.hasDisplayItem -> updateLoadHeader(NONE)
            // 此时EMPTY视图未显示，并且列表为空
            visible == NONE && !adapter.hasDisplayItem -> updateLoadHeader(EMPTY)
        }
    }

    override fun onLoadStatesChanged(previous: LoadStates, current: LoadStates) {
        loadStates = collector.displayLoadStates
        updateLoadHeader(loadStates.toVisible())
    }

    private fun LoadStates.toVisible(): Visible = when {
        adapter.hasDisplayItem -> NONE
        this.isIncomplete -> NONE
        this.isLoading -> if (config.loadingScope != null) LOADING else NONE
        this.isFailure -> if (config.failureScope != null) FAILURE else NONE
        this.isFully -> if (config.emptyScope != null) EMPTY else NONE
        else -> visible
    }

    private fun updateLoadHeader(visible: Visible) {
        if (this.visible != visible) {
            this.visible = visible
            showOrHideOrUpdate(show = visible != NONE, anim = false)
        }
    }

    internal fun toHandle() = LoadAdapterHandle { update() }

    private enum class Visible {
        NONE, LOADING, FAILURE, EMPTY
    }
}