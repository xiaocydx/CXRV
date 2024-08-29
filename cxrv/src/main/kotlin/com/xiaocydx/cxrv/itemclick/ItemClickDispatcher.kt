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

import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import androidx.annotation.VisibleForTesting
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SimpleOnItemTouchListener
import com.xiaocydx.cxrv.R
import com.xiaocydx.cxrv.list.Disposable
import com.xiaocydx.cxrv.list.InlineList
import com.xiaocydx.cxrv.list.accessEach
import com.xiaocydx.cxrv.list.getOrNull
import com.xiaocydx.cxrv.list.toList

/**
 * Item点击分发器，支持`itemView`及其子view的点击、长按
 */
@PublishedApi
internal val RecyclerView.itemClickDispatcher: ItemClickDispatcher
    get() {
        var dispatcher: ItemClickDispatcher? =
                getTag(R.id.tag_item_click_dispatcher) as? ItemClickDispatcher
        if (dispatcher == null) {
            dispatcher = ItemClickDispatcher(this)
            setTag(R.id.tag_item_click_dispatcher, dispatcher)
        }
        return dispatcher
    }

/**
 * Item点击分发器，支持`itemView`及其子view的点击、长按
 *
 * 1. 当RecyclerView的[onInterceptTouchEvent]的触摸事件为[ACTION_DOWN]时，
 * 找到可能触发点击或者长按的[DispatchTarget]，对其设置点击、长按监听，并添加到待处理集合中。
 * 2. 若[onClick]或者[onLongClick]被调用，则说明第1步的待处理集合中有符合条件的[DispatchTarget]，
 * 因此遍历待处理集合，执行点击或者长按的处理程序。
 */
@PublishedApi
internal class ItemClickDispatcher(
    private val rv: RecyclerView
) : SimpleOnItemTouchListener(), OnClickListener, OnLongClickListener {
    private var dispatchTargets = InlineList<DispatchTarget>()
    private val pendingClickTargets = PendingDispatchTargets<ClickDispatchTarget>()
    private val pendingLongClickTargets = PendingDispatchTargets<LongClickDispatchTarget>()

    init {
        rv.addOnItemTouchListener(this)
    }

    fun addItemClick(
        intervalMs: Long,
        targetView: (itemView: View, event: MotionEvent) -> View?,
        clickHandler: (itemView: View) -> Unit
    ): Disposable = DispatchTargetDisposable().attach(
        dispatcher = this,
        target = ClickDispatchTarget(intervalMs, targetView, clickHandler)
    )

    fun addLongItemClick(
        targetView: (itemView: View, event: MotionEvent) -> View?,
        longClickHandler: (itemView: View) -> Boolean
    ): Disposable = DispatchTargetDisposable().attach(
        dispatcher = this,
        target = LongClickDispatchTarget(targetView, longClickHandler)
    )

    /**
     * 找到可能触发点击或者长按的[DispatchTarget]，对其设置点击、长按监听，并添加到待处理集合中
     */
    override fun onInterceptTouchEvent(rv: RecyclerView, event: MotionEvent): Boolean {
        if (event.actionMasked != ACTION_DOWN) return false
        pendingClickTargets.clear()
        pendingLongClickTargets.clear()
        rv.findItemView(event) { itemView ->
            dispatchTargets.accessEach { it.bind(itemView, event) }
        }
        return false
    }

    /**
     * 触发了点击，说明[pendingClickTargets]中有符合条件的[ClickDispatchTarget]
     */
    override fun onClick(targtView: View) {
        val itemView = rv.findContainingItemView(targtView) ?: return
        pendingClickTargets.perform(itemView, targtView)
        pendingClickTargets.clear()
    }

    /**
     * 触发了长按，说明[pendingLongClickTargets]中有符合条件的[LongClickDispatchTarget]
     */
    override fun onLongClick(targetView: View): Boolean {
        val itemView = rv.findContainingItemView(targetView) ?: return false
        val consumed = pendingLongClickTargets.perform(itemView, targetView)
        pendingLongClickTargets.clear()
        return consumed
    }

    @VisibleForTesting
    fun getDispatchTargets() = dispatchTargets.toList()

    private fun addDispatchTarget(target: DispatchTarget) {
        dispatchTargets += target
        target.onAttachedToDispatcher(this)
    }

    private fun removeDispatchTarget(target: DispatchTarget) {
        dispatchTargets -= target
        target.onDetachedFromDispatcher(this)
    }

    fun addPendingDispatchTarget(target: DispatchTarget, targetView: View) {
        when (target) {
            is ClickDispatchTarget -> pendingClickTargets.add(target, targetView)
            is LongClickDispatchTarget -> pendingLongClickTargets.add(target, targetView)
        }
    }

    fun removePendingDispatchTarget(target: DispatchTarget, targetView: View) {
        when (target) {
            is ClickDispatchTarget -> pendingClickTargets.remove(target, targetView)
            is LongClickDispatchTarget -> pendingLongClickTargets.remove(target, targetView)
        }
    }

    private inline fun RecyclerView.findItemView(event: MotionEvent, action: (itemView: View) -> Unit) {
        val lm = layoutManager ?: return
        val x = event.x
        val y = event.y
        val count = lm.childCount
        for (i in count - 1 downTo 0) {
            val child = lm.getChildAt(i)!!
            val translationX = child.translationX
            val translationY = child.translationY
            if (x >= child.left + translationX
                    && x <= child.right + translationX
                    && y >= child.top + translationY
                    && y <= child.bottom + translationY) {
                action(child)
            }
        }
    }

    private class PendingDispatchTargets<T : DispatchTarget> {
        private var targets = InlineList<T>()
        private var targetViews = InlineList<View>()

        fun add(target: T, targetView: View) {
            assert(targets.size == targetViews.size)
            targets = targets.add(target)
            targetViews = targetViews.add(targetView)
        }

        fun remove(target: T, targetView: View) {
            assert(targets.size == targetViews.size)
            val index = targets.indexOf(target)
            if (targetViews.getOrNull(index) !== targetView) return
            targets = targets.removeAt(index)
            targetViews = targetViews.removeAt(index)
        }

        fun clear() {
            targets = targets.clear()
            targetViews = targetViews.clear()
        }

        fun perform(itemView: View, targetView: View): Boolean {
            assert(targets.size == targetViews.size)
            var outcome = false
            for (i in 0 until targetViews.size) {
                if (targetViews[i] !== targetView) continue
                if (targets[i].perform(itemView)) outcome = true
            }
            return outcome
        }
    }

    private class DispatchTargetDisposable : Disposable {
        private var target: DispatchTarget? = null
        private var dispatcher: ItemClickDispatcher? = null
        override val isDisposed: Boolean
            get() = target == null && dispatcher == null

        fun attach(
            target: DispatchTarget,
            dispatcher: ItemClickDispatcher
        ): Disposable {
            this.target = target
            this.dispatcher = dispatcher
            dispatcher.addDispatchTarget(target)
            return this
        }

        override fun dispose() {
            dispatcher?.removeDispatchTarget(target!!)
            target = null
            dispatcher = null
        }
    }
}