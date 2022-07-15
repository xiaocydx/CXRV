package com.xiaocydx.cxrv.internal

import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.CallSuper
import androidx.core.view.OneShotPreDrawListener

/**
 * 实现逻辑改造自[OneShotPreDrawListener]
 *
 * @author xcc
 * @date 2022/7/15
 */
internal open class PreDrawListener(
    private val view: View
) : ViewTreeObserver.OnPreDrawListener, View.OnAttachStateChangeListener {
    private var isAddedPreDrawListener = false
    private var viewTreeObserver: ViewTreeObserver = view.viewTreeObserver
    private var action: (() -> Unit)? = null

    constructor(view: View, action: () -> Unit) : this(view) {
        this.action = action
    }

    init {
        addOnPreDrawListener()
        view.addOnAttachStateChangeListener(this)
    }

    @CallSuper
    override fun onPreDraw(): Boolean {
        action?.invoke()
        return true
    }

    @CallSuper
    override fun onViewAttachedToWindow(view: View) {
        viewTreeObserver = view.viewTreeObserver
        if (action == null) addOnPreDrawListener()
    }

    @CallSuper
    override fun onViewDetachedFromWindow(view: View) {
        // 从视图树中移除监听，避免出现内存泄漏
        if (action != null) {
            action = null
            removeListener()
        } else {
            removeOnPreDrawListener()
        }
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