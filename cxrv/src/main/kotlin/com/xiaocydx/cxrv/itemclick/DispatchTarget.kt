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

package com.xiaocydx.cxrv.itemclick

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.View.OnAttachStateChangeListener

/**
 * Item点击分发目标
 *
 * @author xcc
 * @date 2022/4/14
 */
internal sealed class DispatchTarget(
    private val targetView: (itemView: View, event: MotionEvent) -> View?
) : OnAttachStateChangeListener {
    protected var dispatcher: ItemClickDispatcher? = null
        private set

    fun onAttachedToDispatcher(dispatcher: ItemClickDispatcher) {
        require(this.dispatcher == null)
        this.dispatcher = dispatcher
    }

    fun onDetachedFromDispatcher(dispatcher: ItemClickDispatcher) {
        require(this.dispatcher === dispatcher)
        this.dispatcher = null
    }

    fun bind(itemView: View, event: MotionEvent): Boolean {
        val targetView = targetView(itemView, event)?.takeIf { it.isAttachedToWindow }
        if (targetView != null) {
            // 避免重复添加
            unbind(targetView)
            setListener(targetView)
            targetView.addOnAttachStateChangeListener(this)
            dispatcher?.addPendingDispatchTarget(this, targetView)
        }
        return targetView != null
    }

    private fun unbind(targetView: View) {
        clearListener(targetView)
        targetView.removeOnAttachStateChangeListener(this)
        dispatcher?.removePendingDispatchTarget(this, targetView)
    }

    override fun onViewDetachedFromWindow(targetView: View) {
        // 避免共享RecycledViewPool场景出现内存泄漏问题
        unbind(targetView)
    }

    override fun onViewAttachedToWindow(v: View) = Unit

    protected abstract fun setListener(targetView: View)

    protected abstract fun clearListener(targetView: View)

    abstract fun perform(itemView: View): Boolean
}

internal class ClickDispatchTarget(
    private val intervalMs: Long,
    targetView: (itemView: View, event: MotionEvent) -> View?,
    private val clickHandler: (itemView: View) -> Unit,
) : DispatchTarget(targetView) {
    private var lastPerformUptimeMs = 0L

    override fun setListener(targetView: View) {
        targetView.setOnClickListener(dispatcher)
    }

    override fun clearListener(targetView: View) {
        targetView.setOnClickListener(null)
    }

    override fun perform(itemView: View): Boolean {
        val canPerform = checkPerformInterval()
        if (canPerform) clickHandler(itemView)
        return canPerform
    }

    private fun checkPerformInterval(): Boolean {
        val performUptimeMs = SystemClock.uptimeMillis()
        return if (lastPerformUptimeMs + intervalMs < performUptimeMs) {
            lastPerformUptimeMs = performUptimeMs
            true
        } else false
    }
}

internal class LongClickDispatchTarget(
    targetView: (itemView: View, event: MotionEvent) -> View?,
    private val longClickHandler: (itemView: View) -> Boolean,
) : DispatchTarget(targetView) {

    override fun setListener(targetView: View) {
        targetView.setOnLongClickListener(dispatcher)
    }

    override fun clearListener(targetView: View) {
        targetView.setOnLongClickListener(null)
    }

    override fun perform(itemView: View): Boolean {
        return longClickHandler(itemView)
    }
}