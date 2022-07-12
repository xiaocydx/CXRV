package com.xiaocydx.sample.viewpager2.nested

import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.xiaocydx.sample.R

/**
 * 是否处理[ViewPager2]嵌套[RecyclerView]的滚动冲突
 */
var RecyclerView.isVp2NestedScrollable: Boolean
    get() = getTag(R.id.tag_vp2_nested_scrollable) != null
    set(value) {
        val key = R.id.tag_vp2_nested_scrollable
        var listener = getTag(key) as? Vp2NestedScrollableListener
        if (value && listener == null) {
            listener = Vp2NestedScrollableListener()
            setTag(key, listener)
            addOnItemTouchListener(listener)
        } else if (!value && listener != null) {
            setTag(key, null)
            removeOnItemTouchListener(listener)
        }
    }

private class Vp2NestedScrollableListener : RecyclerView.OnItemTouchListener {
    private val handler = Vp2NestedScrollableHandler()

    /**
     * 调用时机
     * 1. [RecyclerView.onInterceptTouchEvent]。
     * 2. [RecyclerView.onTouchEvent]。
     */
    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        handler.handleInterceptTouchEvent(rv, e)
        return false
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent): Unit = Unit

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean): Unit = Unit
}