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
    if (!isVisible) {
        return false
    }
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

internal inline fun View.doOnAttach(crossinline action: (view: View) -> Unit) {
    if (ViewCompat.isAttachedToWindow(this)) {
        action(this)
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                removeOnAttachStateChangeListener(this)
                action(view)
            }

            override fun onViewDetachedFromWindow(view: View) {}
        })
    }
}

internal inline fun View.doOnDetach(crossinline action: (view: View) -> Unit) {
    if (!ViewCompat.isAttachedToWindow(this)) {
        action(this)
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {}

            override fun onViewDetachedFromWindow(view: View) {
                removeOnAttachStateChangeListener(this)
                action(view)
            }
        })
    }
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