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

package com.xiaocydx.cxrv.concat

import android.view.Choreographer.FrameCallback
import androidx.annotation.CallSuper
import androidx.annotation.VisibleForTesting
import androidx.core.view.OneShotPreDrawListener
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.ViewController
import androidx.recyclerview.widget.assertNotInLayoutOrScroll
import com.xiaocydx.cxrv.internal.currentAnimationTimeNanos
import com.xiaocydx.cxrv.internal.doOnPreDraw
import com.xiaocydx.cxrv.internal.postTraversalCallback
import com.xiaocydx.cxrv.internal.removeTraversalCallback

/**
 * View适配器，用于构建HeaderFooter
 *
 * 当移除HeaderFooter或者RecyclerView从Window分离时，
 * [ViewController]会清除已分离的ViewHolder，拦截将被回收的ViewHolder，
 * 子类不用关注移除HeaderFooter和共享[RecycledViewPool]的场景，可能引起内存泄漏的问题。
 *
 * @author xcc
 * @date 2021/10/15
 */
abstract class ViewAdapter<VH : ViewHolder>(
    currentAsItem: Boolean = false
) : Adapter<VH>(), SpanSizeProvider {
    private val controller = ViewController()
    private val dispatcher = NotifyDispatcher()
    private var itemCount = if (currentAsItem) controller.itemCount else 0

    protected val viewHolder: ViewHolder?
        get() = controller.viewHolder
    protected val recyclerView: RecyclerView?
        get() = controller.recyclerView

    var currentAsItem = currentAsItem
        private set

    final override fun getItemCount(): Int = itemCount

    final override fun getItemViewType(position: Int): Int = getItemViewType()

    final override fun fullSpan(position: Int, holder: ViewHolder): Boolean = true

    final override fun getSpanSize(position: Int, spanCount: Int): Int = spanCount

    final override fun onBindViewHolder(holder: VH, position: Int, payloads: List<Any>) {
        controller.onBindViewHolder(holder)
        onBindViewHolder(holder, payloads)
    }

    final override fun onBindViewHolder(holder: VH, position: Int) {
        controller.onBindViewHolder(holder)
        onBindViewHolder(holder)
    }

    protected open fun onBindViewHolder(holder: VH, payloads: List<Any>) {
        onBindViewHolder(holder)
    }

    protected open fun onBindViewHolder(holder: VH) = Unit

    /**
     * 唯一ViewType，不能跟内容区ViewType产生冲突，默认为当前对象的[hashCode]
     *
     * **注意**：共享[RecycledViewPool]的场景，不能随意将[Class.hashCode]作为返回结果。
     */
    open fun getItemViewType(): Int = hashCode()

    @CallSuper
    override fun onViewRecycled(holder: VH) {
        controller.onViewRecycled(holder)
    }

    @CallSuper
    override fun onFailedToRecycleView(holder: VH): Boolean {
        return controller.onFailedToRecycleView(holder)
    }

    @CallSuper
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        controller.onAttachedToRecyclerView(recyclerView)
        spanSizeProvider.onAttachedToRecyclerView(recyclerView)
    }

    @CallSuper
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        dispatcher.cancel()
        controller.onDetachedFromRecyclerView(recyclerView)
    }

    /**
     * 显示Item，[anim]为`false`表示不运行动画
     */
    fun show(anim: Boolean = true) {
        if (currentAsItem) return
        dispatcher.dispatch(anim, current = true)
    }

    /**
     * 隐藏Item，[anim]为`false`表示不运行动画
     */
    fun hide(anim: Boolean = true) {
        if (!currentAsItem) return
        dispatcher.dispatch(anim, current = false)
    }

    /**
     * 更新Item，不运行动画
     */
    fun update() {
        if (!currentAsItem) return
        dispatcher.dispatch(anim = false, current = true)
    }

    /**
     * 显示/隐藏/更新Item，[anim]为`false`表示不运行动画
     */
    fun showOrHideOrUpdate(show: Boolean, anim: Boolean = true) {
        when {
            !show -> hide(anim)
            !currentAsItem -> show(anim)
            else -> update()
        }
    }

    /**
     * 更新item的显示情况
     *
     * @param show 是否显示item，`true`-添加或更新item，`false`-移除item
     * @param anim 支持的动画，详细描述[NeedAnim]
     */
    @Deprecated(
        message = "函数命名和函数参数存在歧义，拆分实现",
        replaceWith = ReplaceWith("showOrHideOrUpdate(show, anim)")
    )
    fun updateItem(show: Boolean, anim: NeedAnim = NeedAnim.ALL) {
        when {
            !show -> hide(anim = anim != NeedAnim.NOT_ALL)
            !currentAsItem -> show(anim = anim != NeedAnim.NOT_ALL)
            else -> update()
        }
    }

    private inner class NotifyDispatcher {
        private var anim = false
        private var notifyItemAction: FrameCallback? = null
        private var endAnimationAction: OneShotPreDrawListener? = null

        fun dispatch(anim: Boolean, current: Boolean) {
            // 保持notifyItemXXX()原有的断言，不在布局过程做调度
            controller.recyclerView?.assertNotInLayoutOrScroll()
            this.anim = anim
            val previousAsItem = currentAsItem
            currentAsItem = current
            if (notifyItemAction != null) return

            // notifyItemAction执行之前不更新itemCount，
            // 避免其它SubAdapter更新时计算出错误的位置。
            notifyItemAction = FrameCallback {
                notifyItemAction = null
                itemCount = if (currentAsItem) controller.itemCount else 0
                when {
                    !previousAsItem && currentAsItem -> {
                        notifyItemInserted(0)
                        endAnimationActionOnPreDraw()
                    }
                    previousAsItem && !currentAsItem -> {
                        notifyItemRemoved(0)
                        endAnimationActionOnPreDraw()
                    }
                    previousAsItem && currentAsItem -> {
                        // 重用controller.viewHolder，默认不执行动画
                        notifyItemChanged(0, false)
                    }
                }
            }
            // postTraversalCallback()会调用recyclerView.requestLayout()，
            // 即使当前执行在Input/Animation Callback，也能在当前帧完成布局。
            controller.recyclerView?.postTraversalCallback(notifyItemAction!!)
        }

        fun cancel() {
            // 直接执行notifyItemAction，更新itemCount
            notifyItemAction?.doFrame(currentAnimationTimeNanos)
            notifyItemAction?.let { controller.recyclerView?.removeTraversalCallback(it) }
            notifyItemAction = null
            endAnimationAction?.removeListener()
            endAnimationAction = null
        }

        private fun endAnimationActionOnPreDraw() {
            val rv = controller.recyclerView ?: return
            val itemAnimator = rv.itemAnimator ?: return
            if (anim || !rv.isAttachedToWindow) return
            endAnimationAction = rv.doOnPreDraw {
                endAnimationAction = null
                controller.viewHolder?.let(itemAnimator::endAnimation)
            }
        }
    }

    /**
     * 需要Item动画
     */
    @Deprecated("函数命名和函数参数存在歧义，拆分实现")
    enum class NeedAnim {
        /**
         * 需要全部类型的item动画
         */
        ALL,

        /**
         * 仅不需要Change类型的item动画
         */
        NOT_CHANGE,

        /**
         * 不需要全部类型的item动画
         */
        NOT_ALL
    }

    @VisibleForTesting
    internal fun getRecycledViewHolder(): ViewHolder? {
        return controller.getRecycledViewHolder()
    }
}