package com.xiaocydx.sample.viewpager.shared

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import androidx.recyclerview.widget.destroyRecycleViews

/**
 * @author xcc
 * @date 2022/2/21
 */
abstract class SharedRecycledFragment : Fragment() {
    protected abstract val rv: RecyclerView

    open fun onAttachSharedPool(sharedPool: RecycledViewPool) {
        rv.setRecycledViewPool(sharedPool)
    }

    abstract fun onLazyInitialize()

    open fun onRecycleToSharedPool(sharedPool: RecycledViewPool) {
        val maxScrap = rv.childCount * 2
        rv.destroyRecycleViews { _, _ -> maxScrap }
    }
}