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

import androidx.annotation.CallSuper
import androidx.annotation.VisibleForTesting
import androidx.core.view.OneShotPreDrawListener
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.ViewController
import com.xiaocydx.cxrv.internal.doOnPreDraw

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
    private var endAnimationAction: OneShotPreDrawListener? = null
    protected val viewHolder: ViewHolder?
        get() = controller.viewHolder
    protected val recyclerView: RecyclerView?
        get() = controller.recyclerView

    var currentAsItem = currentAsItem
        private set

    final override fun getItemCount(): Int = if (currentAsItem) controller.itemCount else 0

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
        removeEndAnimationAction()
        controller.onDetachedFromRecyclerView(recyclerView)
    }

    /**
     * 显示Item，[anim]为`false`表示不需要动画
     */
    fun show(anim: Boolean = true) {
        if (currentAsItem) return
        currentAsItem = true
        notifyItemInserted(0)
        if (!anim) setEndAnimationAction()
    }

    /**
     * 隐藏Item，[anim]为`false`表示不需要Item动画
     */
    fun hide(anim: Boolean = true) {
        if (!currentAsItem) return
        currentAsItem = false
        notifyItemRemoved(0)
        if (!anim) setEndAnimationAction()
    }

    /**
     * 更新Item，[anim]为`false`表示不需要Item动画
     */
    fun update(anim: Boolean = true) {
        if (!currentAsItem) return
        if (anim) {
            notifyItemChanged(0)
        } else {
            // 重用controller.viewHolder，默认不执行动画
            notifyItemChanged(0, false)
        }
        removeEndAnimationAction()
    }

    /**
     * 显示/隐藏/更新Item，[anim]为`false`表示不需要Item动画
     */
    fun showOrHideOrUpdate(show: Boolean, anim: Boolean = true) {
        when {
            !show -> hide(anim)
            !currentAsItem -> show(anim)
            else -> update(anim)
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
            else -> update(anim = anim == NeedAnim.ALL)
        }
    }

    private fun setEndAnimationAction() {
        removeEndAnimationAction()
        val itemAnimator = recyclerView?.itemAnimator ?: return
        endAnimationAction = recyclerView?.doOnPreDraw {
            endAnimationAction = null
            controller.viewHolder?.let(itemAnimator::endAnimation)
        }
    }

    private fun removeEndAnimationAction() {
        endAnimationAction?.removeListener()
        endAnimationAction = null
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