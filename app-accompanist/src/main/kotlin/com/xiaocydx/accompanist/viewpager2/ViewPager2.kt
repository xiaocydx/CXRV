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

package com.xiaocydx.accompanist.viewpager2

import android.view.View
import androidx.core.view.doOnAttach
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import androidx.viewpager2.widget.ViewPager2
import com.xiaocydx.accompanist.R

/**
 * 共享[RecycledViewPool]，可用于分类场景
 */
val ViewPager2.sharedRecycledViewPool: RecycledViewPool
    get() {
        val key = R.id.tag_vp2_shared_recycled_view_pool
        var pool = getTag(key) as? RecycledViewPool
        if (pool == null) {
            pool = RecycledViewPool()
            setTag(key, pool)
        }
        return pool
    }

/**
 * 查找最接近的父级[ViewPager2]
 */
fun RecyclerView.findParentViewPager2(): ViewPager2? {
    var parent: View? = parent as? View
    // parent是ViewPager2，表示当前View是ViewPager2.mRecyclerView
    if (parent is ViewPager2) parent = parent.parent as? View
    while (parent != null && parent !is ViewPager2) {
        parent = parent.parent as? View
    }
    return parent as? ViewPager2
}

/**
 * 当RecyclerView添加到Window时，设置[ViewPager2.sharedRecycledViewPool]
 */
fun RecyclerView.setVp2SharedRecycledViewPoolOnAttach() {
    doOnAttach { findParentViewPager2()?.sharedRecycledViewPool?.let(::setRecycledViewPool) }
}

inline fun ViewPager2.registerOnPageChangeCallback(
    crossinline onScrolled: (position: Int, positionOffset: Float, positionOffsetPixels: Int) -> Unit = { _, _, _ -> },
    crossinline onSelected: (position: Int) -> Unit = {},
    crossinline onScrollStateChanged: (state: Int) -> Unit = {}
): ViewPager2.OnPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
    override fun onPageScrolled(
        position: Int,
        positionOffset: Float,
        positionOffsetPixels: Int
    ) = onScrolled(position, positionOffset, positionOffsetPixels)

    override fun onPageSelected(position: Int) = onSelected(position)

    override fun onPageScrollStateChanged(state: Int) = onScrollStateChanged(state)
}.also(::registerOnPageChangeCallback)