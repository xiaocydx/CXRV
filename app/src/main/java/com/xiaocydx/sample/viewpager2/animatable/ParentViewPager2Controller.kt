package com.xiaocydx.sample.viewpager2.animatable

import android.view.View
import android.view.View.OnAttachStateChangeListener
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.xiaocydx.cxrv.list.Disposable

/**
 * 添加受父级[viewPager2]控制的[AnimatableController]
 */
@Suppress("SpellCheckingInspection")
fun AnimatableMediator.controlledByParentViewPager2(viewPager2: ViewPager2): Disposable {
    findController(ParentViewPager2Controller::class.java)?.dispose()
    return ParentViewPager2Controller().attach(this, viewPager2)
}

private class ParentViewPager2Controller : OnPageChangeCallback(),
        OnAttachStateChangeListener, AnimatableController {
    private var mediator: AnimatableMediator? = null
    private var viewPager2: ViewPager2? = null
    private var realParent: RecyclerView? = null
    private var isRegisteredCallback = false
    override val isDisposed: Boolean
        get() = mediator == null && viewPager2 == null
    override val isAllowStart: Boolean
        get() = viewPager2 != null && viewPager2!!.scrollState == SCROLL_STATE_IDLE

    fun attach(
        mediator: AnimatableMediator,
        viewPager2: ViewPager2
    ): Disposable {
        this.mediator = mediator
        this.viewPager2 = viewPager2
        realParent = viewPager2.getChildAt(0) as? RecyclerView
        mediator.also {
            it.addController(this)
            it.recyclerView.addOnAttachStateChangeListener(this)
        }
        registerOnPageChangeCallback()
        return this
    }

    override fun onPageScrollStateChanged(state: Int) {
        if (state == SCROLL_STATE_IDLE) {
            startCurrentItem()
        } else {
            mediator?.stopAll()
        }
    }

    private fun startCurrentItem() {
        val rvChild = mediator?.recyclerView ?: return
        val viewPager2 = viewPager2 ?: return
        val realParent = realParent ?: return
        val holder: ViewHolder = realParent.findContainingViewHolder(rvChild) ?: return
        if (holder.layoutPosition == viewPager2.currentItem) {
            mediator?.startAll()
        }
    }

    override fun onViewAttachedToWindow(rvChild: View) {
        registerOnPageChangeCallback()
    }

    override fun onViewDetachedFromWindow(rvChild: View) {
        // 从父级ViewPager2中移除，避免出现内存泄漏问题
        unregisterOnPageChangeCallback()
    }

    private fun registerOnPageChangeCallback() {
        if (isRegisteredCallback) return
        isRegisteredCallback = true
        viewPager2?.registerOnPageChangeCallback(this)
    }

    private fun unregisterOnPageChangeCallback() {
        if (!isRegisteredCallback) return
        isRegisteredCallback = false
        viewPager2?.unregisterOnPageChangeCallback(this)
    }

    override fun dispose() {
        mediator?.also {
            it.removeController(this)
            it.recyclerView.removeOnAttachStateChangeListener(this)
        }
        unregisterOnPageChangeCallback()
        mediator = null
        viewPager2 = null
        realParent = null
    }
}