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

/**
 * 添加受父级[viewPager2]滚动控制的[AnimatableController]
 */
fun AnimatableMediator.controlledByParentViewPager2(viewPager2: ViewPager2): Disposable {
    return controlledByParentViewPager2 { viewPager2 }
}

/**
 * 添加受父级[ViewPager2]滚动控制的[AnimatableController]
 *
 * @param provider 提供[ViewPager2]，默认查找最接近的父级[ViewPager2]
 */
fun AnimatableMediator.controlledByParentViewPager2(
    provider: ViewPager2Provider = defaultViewPager2Provider
): Disposable {
    findAnimatableController<ParentViewPager2Controller>()?.dispose()
    return ParentViewPager2Controller().attach(this, provider)
}

typealias ViewPager2Provider = (view: View) -> ViewPager2?

private val defaultViewPager2Provider = { view: View ->
    var parent: View? = view.parent as? View
    // parent是ViewPager2，表示当前View是ViewPager2.mRecyclerView
    if (parent is ViewPager2) parent = parent.parent as? View
    while (parent != null && parent !is ViewPager2) {
        parent = parent.parent as? View
    }
    parent as? ViewPager2
}

private class ParentViewPager2Controller : OnPageChangeCallback(),
        OnAttachStateChangeListener, AnimatableController {
    private var mediator: AnimatableMediator? = null
    private var provider: ViewPager2Provider? = null
    private var viewPager2: ViewPager2? = null
    override val isDisposed: Boolean
        get() = mediator == null && provider == null
    override val canStartAnimatable: Boolean
        get() = viewPager2?.let { it.scrollState == SCROLL_STATE_IDLE } ?: true

    fun attach(
        mediator: AnimatableMediator,
        provider: ViewPager2Provider
    ): Disposable {
        this.mediator = mediator
        this.provider = provider
        mediator.also {
            it.addAnimatableController(this)
            it.recyclerView.addOnAttachStateChangeListener(this)
        }
        if (mediator.recyclerView.isAttachedToWindow) {
            registerOnPageChangeCallback()
        }
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
        if (viewPager2 != null) return
        val rvChild = mediator?.recyclerView
        viewPager2 = rvChild?.let { provider?.invoke(it) }
        viewPager2?.registerOnPageChangeCallback(this)
    }

    private fun unregisterOnPageChangeCallback() {
        if (viewPager2 == null) return
        viewPager2?.unregisterOnPageChangeCallback(this)
        viewPager2 = null
    }

    override fun dispose() {
        mediator?.also {
            it.removeAnimatableController(this)
            it.recyclerView.removeOnAttachStateChangeListener(this)
        }
        unregisterOnPageChangeCallback()
        mediator = null
        provider = null
    }
}