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

@file:Suppress("SpellCheckingInspection", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")

package com.xiaocydx.cxrv.animatable

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.internal.PreDrawListener
import com.xiaocydx.cxrv.list.Disposable
import com.xiaocydx.cxrv.list.InlineList
import com.xiaocydx.cxrv.list.accessEach
import com.xiaocydx.cxrv.list.toList

/**
 * [AnimatableMediator]的实现类
 *
 * @author xcc
 * @date 2022/7/23
 */
@PublishedApi
internal class AnimatableMediatorImpl(override val recyclerView: RecyclerView) : AnimatableMediator {
    private var providers = InlineList<AnimatableProvider>()
    private var controllers = InlineList<AnimatableController>()
    private var preDrawListener: PreDrawListener? = null
    override var isDisposed: Boolean = false
    override val canStartAnimatable: Boolean
        get() {
            controllers.accessEach { if (!it.canStartAnimatable) return false }
            return true
        }

    fun attach(): Disposable {
        preDrawListener = object : PreDrawListener(recyclerView) {
            override fun onPreDraw(): Boolean {
                if (!canStartAnimatable) {
                    stopAllAnimatable()
                } else {
                    val childCount = recyclerView.childCount
                    for (index in 0 until childCount) {
                        stopAnimatableOnPreDraw(recyclerView.getChildAt(index))
                    }
                }
                return super.onPreDraw()
            }
        }
        isDisposed = false
        return this
    }

    override fun startAllAnimatable() {
        if (!canStartAnimatable) return
        val childCount = recyclerView.childCount
        for (index in 0 until childCount) {
            startAnimatable(recyclerView.getChildAt(index))
        }
    }

    override fun stopAllAnimatable() {
        val childCount = recyclerView.childCount
        for (index in 0 until childCount) {
            stopAnimatable(recyclerView.getChildAt(index))
        }
    }

    override fun addAnimatableProvider(provider: AnimatableProvider) {
        providers += provider
    }

    override fun removeAnimatableProvider(provider: AnimatableProvider) {
        providers -= provider
    }

    override fun addAnimatableController(controller: AnimatableController) {
        controllers += controller
    }

    override fun removeAnimatableController(controller: AnimatableController) {
        controllers -= controller
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : AnimatableController> findAnimatableController(clazz: Class<T>): T? {
        controllers.accessEach { if (clazz.isAssignableFrom(it.javaClass)) return it as T }
        return null
    }

    private fun startAnimatable(child: View) {
        val holder = recyclerView.getChildViewHolder(child) ?: return
        providers.accessEach action@{
            val animatable = it.getAnimatableOrNull(holder) ?: return@action
            if (animatable.isRunning || !it.canStartAnimatable(holder, animatable)) return@action
            animatable.start()
        }
    }

    private fun stopAnimatable(child: View) {
        val holder = recyclerView.getChildViewHolder(child) ?: return
        providers.accessEach action@{
            val animatable = it.getAnimatableOrNull(holder) ?: return@action
            if (animatable.isRunning) animatable.stop()
        }
    }

    private fun stopAnimatableOnPreDraw(child: View) {
        val holder = recyclerView.getChildViewHolder(child) ?: return
        providers.accessEach action@{
            val animatable = it.getAnimatableOrNull(holder) ?: return@action
            if (!animatable.isRunning || it.canStartAnimatable(holder, animatable)) return@action
            animatable.stop()
        }
    }

    override fun dispose() {
        providers.toList().forEach { it.dispose() }
        controllers.toList().forEach { it.dispose() }
        preDrawListener?.removeListener()
        providers = InlineList()
        controllers = InlineList()
        preDrawListener = null
        isDisposed = true
    }
}