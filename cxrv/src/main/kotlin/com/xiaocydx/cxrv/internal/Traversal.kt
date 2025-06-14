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

import android.content.Context
import android.graphics.Canvas
import android.view.Choreographer
import android.view.Choreographer.FrameCallback
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import com.xiaocydx.cxrv.R
import com.xiaocydx.cxrv.list.InlineList
import com.xiaocydx.cxrv.list.reverseAccessEach

/**
 * 若当前在doFrame消息中，则结果表示当前帧的Vsync时间
 */
internal val currentAnimationTimeNanos: Long
    get() = AnimationUtils.currentAnimationTimeMillis() * 1_000_000

/**
 * 添加[callback]到[BroadcastProxy]，调用[View.requestLayout]申请重新布局，
 * 实现确保[callback]在`doFrame`消息中、[View.onMeasure]重新布局之前执行。
 *
 * 若添加[callback]失败、[BroadcastProxy]或[BroadcastFrame]被移除，则同步执行[callback]。
 *
 * 该函数用于跟[View]的布局严格同步，[Choreographer.postFrameCallbackDelayed]设置负延时的做法，
 * 在Animation Callback下无效，因为添加的负延时[FrameCallback]是在下一帧执行，不是在当前帧执行。
 */
internal fun View.postTraversalCallback(callback: FrameCallback) {
    broadcastProxy.postTraversalCallback(callback)
}

/**
 * 移除[postTraversalCallback]添加的[callback]
 */
internal fun View.removeTraversalCallback(callback: FrameCallback) {
    broadcastProxy.removeTraversalCallback(callback)
}

private val View.broadcastProxy: BroadcastProxy
    get() {
        var proxy = getTag(R.id.tag_view_broadcast_proxy) as? BroadcastProxy
        if (proxy == null) {
            proxy = BroadcastProxy(this)
            setTag(R.id.tag_view_broadcast_proxy, proxy)
        }
        return proxy
    }

private class BroadcastProxy(private val view: View) {
    private var broadcastFrame: BroadcastFrame? = null
    private var callbacks = InlineList<FrameCallback>()

    init {
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) = unregisterFromBroadcastFrame()
        })
    }

    fun postTraversalCallback(callback: FrameCallback) {
        if (broadcastFrame == null && view.isAttachedToWindow) {
            // broadcastFrame未获取或被移除, 尝试获取
            registerToBroadcastFrame()
        }
        if (broadcastFrame == null) {
            // 添加callback失败，同步执行callback
            callback.doFrame(currentAnimationTimeNanos)
            return
        }
        callbacks += callback
        broadcastFrame?.scheduleTraversal()
    }

    fun requestLayoutIfNecessary() {
        if (callbacks.size > 0) view.requestLayout()
    }

    fun removeTraversalCallback(callback: FrameCallback) {
        callbacks -= callback
    }

    fun registerToBroadcastFrame() {
        broadcastFrame = BroadcastFrame.get(view)
        broadcastFrame?.register(this)
    }

    fun unregisterFromBroadcastFrame() {
        broadcastFrame?.unregister(this)
        broadcastFrame = null
        // this或broadcastFrame被移除，同步执行callback
        consumeTraversalCallbacks()
    }

    fun consumeTraversalCallbacks() {
        val frameTimeNanos = currentAnimationTimeNanos
        callbacks.reverseAccessEach { it.doFrame(frameTimeNanos) }
        callbacks = callbacks.clear()
    }
}

private class BroadcastFrame private constructor(context: Context) : View(context) {
    private val scheduleTraversalRunner = Runnable { scheduleTraversal() }
    private var proxyList = InlineList<BroadcastProxy>()
    private var canUnregisterOnDetached = true
    private var isScheduled = false

    fun register(proxy: BroadcastProxy) {
        proxyList += proxy
    }

    fun unregister(proxy: BroadcastProxy) {
        proxyList -= proxy
    }

    fun scheduleTraversal() {
        removeCallbacks(scheduleTraversalRunner)
        proxyList.reverseAccessEach { it.requestLayoutIfNecessary() }

        ensureFirstMeasure()
        requestLayout()
        if (parent !is FrameLayout) {
            // parent通常是DecorView，DecorView继承自FrameLayout，
            // FrameLayout.onMeasure()按照child顺序进行测量，BroadcastFrame是第0位child。
            // requestApplyInsets()是替补方案，尽可能让BroadcastFrame在其它child之前执行。
            requestApplyInsets()
        }

        // 调用scheduleTraversal()时，当前可能处于measure、layout阶段，
        // 此时调用requestLayout()会被parent的flag拦截，不会有下一帧。
        // 因此调用invalidate()，在onDraw()判断isScheduled是否被重置，
        // 若isScheduled未被重置，则表示被拦截，需要重新调度。
        invalidate()
        isScheduled = true
    }

    override fun dispatchApplyWindowInsets(insets: WindowInsets): WindowInsets {
        consumeTraversalCallbacks()
        return insets
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        consumeTraversalCallbacks()
        setMeasuredDimension(0, 0)
    }

    override fun onDraw(canvas: Canvas) {
        if (isScheduled) {
            isScheduled = false
            // 前置流程可能会添加同步屏障，因此不使用post()
            postOnAnimation(scheduleTraversalRunner)
        }
    }

    private fun consumeTraversalCallbacks() {
        isScheduled = false
        proxyList.reverseAccessEach { it.consumeTraversalCallbacks() }
    }

    private fun ensureFirstMeasure() {
        val parent = parent as? ViewGroup ?: return
        val first = parent.getChildAt(0)
        if (first !== this) {
            canUnregisterOnDetached = false
            parent.removeView(this)
            parent.addView(this, 0)
            canUnregisterOnDetached = true
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (!canUnregisterOnDetached) return
        removeCallbacks(scheduleTraversalRunner)
        proxyList.reverseAccessEach { it.unregisterFromBroadcastFrame() }
        proxyList = proxyList.clear()
    }

    override fun dispatchTouchEvent(event: MotionEvent) = false

    override fun dispatchKeyEvent(event: KeyEvent) = false

    companion object {

        fun get(view: View): BroadcastFrame? {
            if (view is BroadcastFrame) return view
            val rootView = view.rootView as? ViewGroup ?: return null
            require(rootView !== view) { "view.rootView不能是view自身" }
            rootView.childEach { if (it is BroadcastFrame) return it }
            val broadcastFrame = BroadcastFrame(rootView.context)
            rootView.addView(broadcastFrame, 0)
            return broadcastFrame
        }
    }
}