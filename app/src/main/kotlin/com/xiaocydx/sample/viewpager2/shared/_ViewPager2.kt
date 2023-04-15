@file:JvmName("ViewPager2Internal")

package com.xiaocydx.sample.viewpager2.shared

import android.view.View
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
 * 向上查找[ViewPager2]父级
 */
fun RecyclerView.findParentViewPager2(): ViewPager2? {
    var parent: View? = parent as? View
    while (parent != null && parent !is ViewPager2) {
        parent = parent.parent as? View
    }
    return parent as? ViewPager2
}