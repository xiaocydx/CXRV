package com.xiaocydx.sample.viewpager.shared

import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import androidx.viewpager.widget.ViewPager

/**
 * @author xcc
 * @date 2022/2/21
 */
abstract class SharedRecycledPagerAdapter(
    fm: FragmentManager,
    sharedPool: RecycledViewPool = RecycledViewPool()
) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private val controller = SharedRecycledController(fm, sharedPool)

    abstract override fun getItem(position: Int): SharedRecycledFragment

    @CallSuper
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val viewPager = container as ViewPager
        if (!controller.isBindViewPager) {
            controller.bindViewPager(viewPager)
        }
        return super.instantiateItem(container, position).also {
            val isCurrent = viewPager.currentItem == position
            controller.instantiateItem(it as Fragment, isCurrent)
        }
    }

    @CallSuper
    override fun destroyItem(container: ViewGroup, position: Int, item: Any) {
        super.destroyItem(container, position, item)
        controller.destroyItem(item as Fragment)
    }
}