package com.xiaocydx.cxrv.itemvisible

import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager

/**
 * @author xcc
 * @date 2022/11/22
 */
internal class TestLayoutManager(private val fakePosition: Int) : LayoutManager() {
    fun findFirstVisibleItemPosition() = fakePosition

    fun findFirstCompletelyVisibleItemPosition() = fakePosition

    fun findLastVisibleItemPosition() = fakePosition

    fun findLastCompletelyVisibleItemPosition() = fakePosition

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
    }
}