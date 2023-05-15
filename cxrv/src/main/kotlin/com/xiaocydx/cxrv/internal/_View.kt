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

package com.xiaocydx.cxrv.internal

import android.view.View
import android.view.ViewGroup
import androidx.core.view.OneShotPreDrawListener
import androidx.core.view.ViewCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal inline var View.isVisible: Boolean
    get() = visibility == View.VISIBLE
    set(value) {
        visibility = if (value) View.VISIBLE else View.GONE
    }

internal inline var View.isInvisible: Boolean
    get() = visibility == View.INVISIBLE
    set(value) {
        visibility = if (value) View.INVISIBLE else View.VISIBLE
    }

internal inline var View.isGone: Boolean
    get() = visibility == View.GONE
    set(value) {
        visibility = if (value) View.GONE else View.VISIBLE
    }

internal fun View.isTouched(rawX: Float, rawY: Float): Boolean {
    if (!isVisible) return false
    val location = IntArray(2)
    this.getLocationOnScreen(location)
    val left = location[0]
    val top = location[1]
    val right = left + this.width
    val bottom = top + this.height
    val isContainX = rawX in left.toFloat()..right.toFloat()
    val isContainY = rawY in top.toFloat()..bottom.toFloat()
    return isContainX && isContainY
}

internal suspend fun View.awaitPreDraw() {
    suspendCancellableCoroutine<Unit> { cont ->
        val listener = doOnPreDraw { cont.resume(Unit) }
        cont.invokeOnCancellation { listener.removeListener() }
    }
}

internal fun View.doOnPreDraw(action: Runnable): OneShotPreDrawListener {
    return OneShotPreDrawListener.add(this, action)
}

internal inline fun View.doOnPreDraw(crossinline action: (view: View) -> Unit): OneShotPreDrawListener {
    return OneShotPreDrawListener.add(this) { action(this) }
}

internal inline fun View.doOnAttach(crossinline action: (view: View) -> Unit): OneShotAttachStateListener? {
    return if (ViewCompat.isAttachedToWindow(this)) {
        action(this)
        null
    } else {
        OneShotAttachStateListener(this, isAttach = true) { action(it) }
    }
}

internal inline fun View.doOnDetach(crossinline action: (view: View) -> Unit): OneShotAttachStateListener? {
    return if (!ViewCompat.isAttachedToWindow(this)) {
        action(this)
        null
    } else {
        OneShotAttachStateListener(this, isAttach = false) { action(it) }
    }
}

internal class OneShotAttachStateListener(
    private val view: View,
    private val isAttach: Boolean,
    private val action: (view: View) -> Unit
) : View.OnAttachStateChangeListener {

    init {
        view.addOnAttachStateChangeListener(this)
    }

    override fun onViewAttachedToWindow(view: View) {
        if (isAttach) complete()
    }

    override fun onViewDetachedFromWindow(view: View) {
        if (!isAttach) complete()
    }

    private fun complete() {
        removeListener()
        action(view)
    }

    fun removeListener() {
        view.removeOnAttachStateChangeListener(this)
    }
}

internal suspend fun View.awaitNextLayout() = suspendCancellableCoroutine<Unit> { cont ->
    val listener = object : View.OnLayoutChangeListener {
        override fun onLayoutChange(
            view: View, left: Int, top: Int, right: Int, bottom: Int,
            oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
        ) {
            view.removeOnLayoutChangeListener(this)
            cont.resume(Unit)
        }
    }
    addOnLayoutChangeListener(listener)
    cont.invokeOnCancellation { removeOnLayoutChangeListener(listener) }
}

internal inline fun View.doOnNextLayout(crossinline action: (View) -> Unit) {
    addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
        override fun onLayoutChange(
            view: View, left: Int, top: Int, right: Int, bottom: Int,
            oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
        ) {
            view.removeOnLayoutChangeListener(this)
            action(view)
        }
    })
}

/**
 * 用于频繁遍历访问子View的场景，减少迭代器对象的创建
 */
internal inline fun ViewGroup.childEach(action: (View) -> Unit) {
    val childCount = childCount
    for (index in 0 until childCount) {
        action(getChildAt(index))
    }
}