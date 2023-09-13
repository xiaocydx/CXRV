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

package com.xiaocydx.sample.paging.complex.transform

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.coroutineScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.optimizeNextFrameScroll
import com.xiaocydx.sample.viewLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * 变换过渡动画的Sender，[FragmentActivity]或[Fragment]实现该接口完成页面跳转
 *
 * @author xcc
 * @date 2023/8/7
 */
interface TransformSender {

    /**
     * 当退出实现[TransformReceiver]的Fragment时，构建变换过渡动画的过程会发射退出事件，
     * 收集事件，在下一帧布局完成之前，都可以调用[setTransformView]设置`transformView`。
     */
    val <S> S.transformReturn: SharedFlow<Unit> where S : Fragment, S : TransformSender
        get() = requireTransformSceneRoot().transformReturn

    /**
     * 设置参与变换过渡动画的[View]，内部弱引用持有[View]
     *
     * **注意**：若未设置参与变换过渡动画的[View]，则不会运行动画。
     */
    fun <S> S.setTransformView(view: View?) where S : Fragment, S : TransformSender {
        findTransformSceneRoot()?.setTransformView(view)
    }

    /**
     * 跳转至实现[TransformReceiver]的Fragment，运行变换过渡动画
     *
     * @param transformView 参与变换过渡动画的[View]，内部弱引用持有[View]
     * @param fragmentClass 实现[TransformReceiver]的Fragment的[Class]
     * @return 若当前已跳转至实现[TransformReceiver]的Fragment，则返回`false`，表示跳转失败。
     */
    fun <S, R> S.forwardTransform(
        transformView: View,
        fragmentClass: KClass<R>,
        args: Bundle? = null,
        allowStateLoss: Boolean = false
    ): Boolean where S : Fragment, S : TransformSender,
                     R : Fragment, R : TransformReceiver {
        val root = findTransformSceneRoot() ?: return false
        root.setTransformView(transformView)
        return root.forwardTransform(fragmentClass, args, allowStateLoss)
    }

    /**
     * 启动协程，处理[TransformSender]的同步位置
     *
     * @param recyclerView   滚动到同步位置的[RecyclerView]
     * @param contentAdapter 内容区域的[Adapter]，用于查找[ViewHolder]
     * @param transformView  参与变换过渡动画的[View]，内部弱引用持有[View]
     */
    fun <S, VH> S.launchTransformSync(
        recyclerView: RecyclerView,
        contentAdapter: Adapter<VH>,
        position: TransformSenderPosition,
        transformView: (holder: VH) -> View?
    ): Job where S : Fragment, S : TransformSender, VH : ViewHolder {
        return viewLifecycle.coroutineScope.launch {
            position.syncEvent.onEach {
                recyclerView.scrollToPosition(contentAdapter, it)
            }.launchIn(this)

            // 消费同步位置，Fragment重新创建不需要恢复同步位置
            transformReturn
                .map { position.consume() }
                .map { recyclerView.findViewHolder(contentAdapter, it) }
                .onEach { setTransformView(it?.let(transformView)) }
                .launchIn(this)
        }
    }

    /**
     * 查找匹配[adapter]和[position]的[ViewHolder]，若查找不到，则返回`null`
     *
     * **注意**：该函数以[ConcatAdapter]实现HeaderFooter为前提，匹配[ViewHolder]。
     *
     * @param adapter  目标`holder.bindingAdapter`
     * @param position 目标`holder.bindingAdapterPosition`
     */
    fun <VH : ViewHolder> RecyclerView.findViewHolder(adapter: Adapter<VH>, position: Int): VH? {
        // 处理TransformSender的同步位置，不需要匹配正在运行remove动画的ViewHolder，
        // 因此通过LayoutManager.childCount和LayoutManager.getChildAt()进行匹配。
        val lm = layoutManager ?: return null
        for (i in 0 until lm.childCount) {
            val holder = lm.getChildAt(i)?.let(::getChildViewHolder)
            if (holder != null && holder.bindingAdapter === adapter
                    && holder.bindingAdapterPosition == position) {
                // bindingAdapter一致，确保holder类型安全，直接类型转换即可
                @Suppress("UNCHECKED_CAST")
                return holder as VH
            }
        }
        return null
    }

    /**
     * 非平滑滚动到匹配[adapter]和[position]的位置
     *
     * **注意**：该函数以[ConcatAdapter]实现HeaderFooter为前提，匹配位置。
     *
     * @param adapter  目标`holder.bindingAdapter`
     * @param position 目标`holder.bindingAdapterPosition`
     */
    fun RecyclerView.scrollToPosition(adapter: Adapter<*>, position: Int) {
        var offset = 0
        val concatAdapter = this.adapter as? ConcatAdapter
        if (concatAdapter != null) {
            // 先尝试快路径，尽可能避免调用concatAdapter.adapters创建集合对象
            val holder = findViewHolder(adapter, position)
            if (holder != null) {
                offset = holder.layoutPosition - holder.bindingAdapterPosition
            } else {
                val adapters = concatAdapter.adapters
                for (i in adapters.indices) {
                    if (adapters[i] === adapter) break
                    offset += adapters[i].itemCount
                }
            }
        }
        scrollToPosition(offset + position)
        // 非平滑滚动布局流程的优化方案，可用于替代增加缓存上限的方案
        optimizeNextFrameScroll()
    }
}