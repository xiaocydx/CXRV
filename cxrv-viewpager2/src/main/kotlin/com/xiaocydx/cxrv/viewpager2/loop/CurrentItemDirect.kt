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

@file:JvmName("CurrentItemDirectInternalKt")
@file:Suppress("PackageDirectoryMismatch")

package androidx.viewpager2.widget

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*

/**
 * ### 优化初衷
 * 当[RecyclerView.onInterceptTouchEvent]将滚动状态设为[SCROLL_STATE_DRAGGING]时，
 * 会对[OnScrollListener]分发新的滚动状态，若此时调用[ViewPager2.setCurrentItem]，
 * 则会调用至[RecyclerView.stopScroll]，将滚动状态设为[SCROLL_STATE_IDLE]，
 * 导致[RecyclerView.onInterceptTouchEvent]返回`false`，在实际场景中，
 * 手势滚动可能会因为这次触摸事件拦截失败，而出现交互不流畅的问题。
 *
 * ### 优化方案
 * [ViewPager2.mCurrentItem]直接设置为[item]，并通过[LayoutManager]非平滑滚动，
 * 以此避免调用至[RecyclerView.stopScroll]，将滚动状态设为[SCROLL_STATE_IDLE]。
 */
internal fun ViewPager2.setCurrentItemDirect(item: Int) {
    mCurrentItem = item
    scrollToPositionDirect(item)
}

/**
 * [ViewPager2]的LayoutManager是[LinearLayoutManager]，调用`scrollToPosition()`是为了确保下一帧布局流程，
 * `onLayoutChildren()`通过`updateAnchorFromPendingData()`更新锚点，而不是`updateAnchorFromChildren()`，
 * 在快速滚动的过程中，`updateAnchorFromChildren()`不能达到预期的效果。
 */
internal fun ViewPager2.scrollToPositionDirect(position: Int) {
    mRecyclerView.layoutManager?.scrollToPosition(position)
}