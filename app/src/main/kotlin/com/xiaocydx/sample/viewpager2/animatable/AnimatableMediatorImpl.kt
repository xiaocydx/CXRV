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

package com.xiaocydx.sample.viewpager2.animatable

import android.view.View
import android.view.ViewTreeObserver
import androidx.core.view.OneShotPreDrawListener
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.list.Disposable

/**
 * [AnimatableMediator]的实现类
 *
 * @author xcc
 * @date 2022/7/23
 */
@PublishedApi
@Suppress("SpellCheckingInspection")
internal class AnimatableMediatorImpl(
    override val recyclerView: RecyclerView
) : AnimatableMediator, () -> Unit {
    private var providers: ArrayList<AnimatableProvider>? = null
    private var controllers: ArrayList<AnimatableController>? = null
    private var preDrawListener: PreDrawListener? = null
    override var isDisposed: Boolean = false
    override val canStartAnimatable: Boolean
        get() {
            controllers?.accessEach { if (!it.canStartAnimatable) return false }
            return true
        }

    fun attach(): Disposable {
        preDrawListener = PreDrawListener(recyclerView, action = this)
        isDisposed = false
        return this
    }

    // checkOnPreDraw()
    override fun invoke() {
        if (!canStartAnimatable) {
            stopAllAnimatable()
        } else {
            val childCount = recyclerView.childCount
            for (index in 0 until childCount) {
                stopAnimatableOnPreDraw(recyclerView.getChildAt(index))
            }
        }
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
        if (providers == null) {
            providers = ArrayList(2)
        }
        if (!providers!!.contains(provider)) {
            providers!!.add(provider)
        }
    }

    override fun removeAnimatableProvider(provider: AnimatableProvider) {
        providers?.remove(provider)
    }

    override fun addAnimatableController(controller: AnimatableController) {
        if (controllers == null) {
            controllers = ArrayList(2)
        }
        if (!controllers!!.contains(controller)) {
            controllers!!.add(controller)
        }
    }

    override fun removeAnimatableController(controller: AnimatableController) {
        controllers?.remove(controller)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : AnimatableController> findAnimatableController(clazz: Class<T>): T? {
        controllers?.accessEach { if (clazz.isAssignableFrom(it.javaClass)) return it as T }
        return null
    }

    private fun startAnimatable(child: View) {
        val holder = recyclerView.getChildViewHolder(child) ?: return
        providers?.accessEach action@{
            val animatable = it.getAnimatableOrNull(holder) ?: return@action
            if (animatable.isRunning || !it.canStartAnimatable(holder, animatable)) return@action
            animatable.start()
        }
    }

    private fun stopAnimatable(child: View) {
        val holder = recyclerView.getChildViewHolder(child) ?: return
        providers?.accessEach action@{
            val animatable = it.getAnimatableOrNull(holder) ?: return@action
            if (animatable.isRunning) animatable.stop()
        }
    }

    private fun stopAnimatableOnPreDraw(child: View) {
        val holder = recyclerView.getChildViewHolder(child) ?: return
        providers?.accessEach action@{
            val animatable = it.getAnimatableOrNull(holder) ?: return@action
            if (!animatable.isRunning || it.canStartAnimatable(holder, animatable)) return@action
            animatable.stop()
        }
    }

    private inline fun <T> ArrayList<T>.accessEach(action: (T) -> Unit) {
        for (index in this.indices) action(get(index))
    }

    override fun dispose() {
        providers?.accessEach { it.dispose() }
        controllers?.accessEach { it.dispose() }
        preDrawListener?.removeListener()
        providers = null
        controllers = null
        preDrawListener = null
        isDisposed = true
    }
}

/**
 * 实现逻辑改造自[OneShotPreDrawListener]
 */
private class PreDrawListener(
    private val view: View,
    private val action: (() -> Unit)? = null
) : ViewTreeObserver.OnPreDrawListener, View.OnAttachStateChangeListener {
    private var isAddedPreDrawListener = false
    private var viewTreeObserver: ViewTreeObserver = view.viewTreeObserver

    init {
        addOnPreDrawListener()
        view.addOnAttachStateChangeListener(this)
    }

    override fun onPreDraw(): Boolean {
        action?.invoke()
        return true
    }

    override fun onViewAttachedToWindow(view: View) {
        viewTreeObserver = view.viewTreeObserver
        addOnPreDrawListener()
    }

    override fun onViewDetachedFromWindow(view: View) {
        // 从视图树中移除监听，避免出现内存泄漏问题
        removeOnPreDrawListener()
    }

    fun removeListener() {
        removeOnPreDrawListener()
        view.removeOnAttachStateChangeListener(this)
    }

    private fun addOnPreDrawListener() {
        if (isAddedPreDrawListener) return
        isAddedPreDrawListener = true
        viewTreeObserver.addOnPreDrawListener(this)
    }

    private fun removeOnPreDrawListener() {
        if (!isAddedPreDrawListener) return
        isAddedPreDrawListener = false
        if (viewTreeObserver.isAlive) {
            viewTreeObserver.removeOnPreDrawListener(this)
        } else {
            view.viewTreeObserver.removeOnPreDrawListener(this)
        }
    }
}