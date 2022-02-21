package com.xiaocydx.sample.viewpager

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import androidx.recyclerview.widget.tryRecycleAllChild

/**
 * @author xcc
 * @date 2022/2/21
 */
abstract class SharedRecycledFragment : Fragment() {
    protected abstract val recyclerView: RecyclerView

    open fun onAttachSharedPool(sharedPool: RecycledViewPool) {
        recyclerView.setRecycledViewPool(sharedPool)
    }

    abstract fun initObserve()

    open fun onRecycleToSharedPool(sharedPool: RecycledViewPool) {
        recyclerView.tryRecycleAllChild()
    }
}