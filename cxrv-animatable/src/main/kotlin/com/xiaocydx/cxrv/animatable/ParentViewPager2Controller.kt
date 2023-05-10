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

@file:Suppress("SpellCheckingInspection")

package com.xiaocydx.cxrv.animatable

import android.view.View
import android.view.View.OnAttachStateChangeListener
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.xiaocydx.cxrv.list.Disposable
import java.lang.ref.WeakReference

/**
 * 添加受父级[viewPager2]滚动控制的[AnimatableController]
 */
fun AnimatableMediator.controlledByParentViewPager2(viewPager2: ViewPager2): Disposable {
    findAnimatableController<ParentViewPager2Controller>()?.dispose()
    return ParentViewPager2Controller().attach(this, viewPager2)
}

private class ParentViewPager2Controller : OnPageChangeCallback(),
        OnAttachStateChangeListener, AnimatableController {
    private var mediator: AnimatableMediator? = null
    private var viewPager2Ref: WeakReference<ViewPager2>? = null
    private var isRegisteredCallback = false
    private val viewPager2: ViewPager2?
        get() = viewPager2Ref?.get()
    override val isDisposed: Boolean
        get() = mediator == null && viewPager2 == null
    override val canStartAnimatable: Boolean
        get() = viewPager2?.let { it.scrollState == SCROLL_STATE_IDLE } ?: true

    fun attach(
        mediator: AnimatableMediator,
        viewPager2: ViewPager2
    ): Disposable {
        this.mediator = mediator
        this.viewPager2Ref = WeakReference(viewPager2)
        mediator.also {
            it.addAnimatableController(this)
            it.recyclerView.addOnAttachStateChangeListener(this)
        }
        registerOnPageChangeCallback()
        return this
    }

    override fun onPageScrollStateChanged(state: Int) {
        if (state == SCROLL_STATE_IDLE) {
            startCurrentItem()
        } else {
            mediator?.stopAllAnimatable()
        }
    }

    private fun startCurrentItem() {
        val rvChild = mediator?.recyclerView ?: return
        val viewPager2 = viewPager2 ?: return
        val realParent = viewPager2.getChildAt(0) as? RecyclerView ?: return
        val holder = realParent.findContainingViewHolder(rvChild) ?: return
        if (holder.layoutPosition == viewPager2.currentItem) {
            mediator?.startAllAnimatable()
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
            it.removeAnimatableController(this)
            it.recyclerView.removeOnAttachStateChangeListener(this)
        }
        unregisterOnPageChangeCallback()
        mediator = null
        viewPager2Ref = null
    }
}