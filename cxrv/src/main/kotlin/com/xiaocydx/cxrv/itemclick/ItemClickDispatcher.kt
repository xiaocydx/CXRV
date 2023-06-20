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
 * 1.当RecyclerView的[onInterceptTouchEvent]的触摸事件为[ACTION_DOWN]时，
 * 找到可能触发点击或者长按的[DispatchTarget]，对其设置点击、长按监听，并添加到待处理集合中。
 * 2.若[onClick]或者[onLongClick]被调用，则说明第1步的待处理集合中有符合条件的[DispatchTarget]，
 * 因此遍历待处理集合，尝试执行点击或者长按的处理程序。
 */
@PublishedApi
internal class ItemClickDispatcher(
    private val rv: RecyclerView
) : SimpleOnItemTouchListener(), OnClickListener, OnLongClickListener {
    private var dispatchTargets = InlineList<DispatchTarget>()
    private var pendingClickTargets = InlineList<ClickDispatchTarget>()
    private var pendingLongClickTargets = InlineList<LongClickDispatchTarget>()

    init {
        rv.addOnItemTouchListener(this)
    }

    /**
     * 添加点击分发目标
     *
     * @param intervalMs   执行[clickHandler]的间隔时间
     * @param targetView   返回需要触发点击的目标视图
     * @param clickHandler 触发目标视图点击时的执行程序
     * @return 调用[Disposable.dispose]移除添加的点击分发目标
     */
    fun addItemClick(
        intervalMs: Long,
        targetView: (itemView: View, event: MotionEvent) -> View?,
        clickHandler: (itemView: View) -> Unit
    ): Disposable = DispatchTargetDisposable().attach(
        dispatcher = this,
        target = ClickDispatchTarget(intervalMs, targetView, clickHandler)
    )

    /**
     * 添加长按分发目标
     *
     * @param targetView       返回需要触发长按的目标视图
     * @param longClickHandler 触发目标视图长按时的执行程序，返回`true`表示消费了长按，松手时不会触发点击
     * @return 调用[Disposable.dispose]移除添加的长按分发目标
     */
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
        clearPendingClickTargets()
        clearPendingLongClickTargets()

        val itemView = rv.findChildViewUnder(event.x, event.y) ?: return false
        dispatchTargets.accessEach action@{
            if (!it.setCurrentTargetView(itemView, event)) return@action
            when {
                it is ClickDispatchTarget && it.setOnClickListener(this) -> {
                    addPendingClickTarget(it)
                }
                it is LongClickDispatchTarget && it.setOnLongClickListener(this) -> {
                    addPendingLongClickTarget(it)
                }
            }
        }
        return false
    }

    /**
     * 触发了点击，说明[pendingClickTargets]中有符合条件的[ClickDispatchTarget]
     */
    override fun onClick(view: View) {
        val itemView = rv.findContainingItemView(view) ?: return
        pendingClickTargets.accessEach { it.tryPerformClickHandler(view, itemView) }
        clearPendingClickTargets()
    }

    /**
     * 触发了长按，说明[pendingLongClickTargets]中有符合条件的[LongClickDispatchTarget]
     */
    override fun onLongClick(view: View): Boolean {
        val itemView = rv.findContainingItemView(view) ?: return false
        var consumed = false
        pendingLongClickTargets.accessEach {
            if (it.tryPerformLongClickHandler(view, itemView)) {
                consumed = true
            }
        }
        clearPendingLongClickTargets()
        return consumed
    }

    @VisibleForTesting
    fun getDispatchTargets() = dispatchTargets.toList()

    private fun addDispatchTarget(target: DispatchTarget) {
        dispatchTargets += target
    }

    private fun removeDispatchTarget(target: DispatchTarget) {
        dispatchTargets -= target
    }

    private fun addPendingClickTarget(target: ClickDispatchTarget) {
        pendingClickTargets += target
    }

    private fun addPendingLongClickTarget(target: LongClickDispatchTarget) {
        pendingLongClickTargets += target
    }

    private fun clearPendingClickTargets() {
        pendingClickTargets = pendingClickTargets.clear()
    }

    private fun clearPendingLongClickTargets() {
        pendingClickTargets = pendingClickTargets.clear()
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