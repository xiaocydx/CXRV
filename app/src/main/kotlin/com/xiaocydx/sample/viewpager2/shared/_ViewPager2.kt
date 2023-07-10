@file:JvmName("ViewPager2Internal")

package com.xiaocydx.sample.viewpager2.shared

import android.view.View
import androidx.core.view.doOnAttach
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import androidx.viewpager2.widget.ViewPager2
import com.xiaocydx.sample.R

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