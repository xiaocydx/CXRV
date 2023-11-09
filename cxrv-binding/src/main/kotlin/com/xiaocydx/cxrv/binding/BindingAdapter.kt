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

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.xiaocydx.cxrv.binding

import android.view.ViewGroup
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.viewbinding.ViewBinding
import com.xiaocydx.cxrv.internal.InternalVisibleApi
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.recycle.prepare.ScrapInflater

/**
 * 使用[ViewBinding]完成视图绑定的[ListAdapter]模板类
 *
 * @author xcc
 * @date 2021/12/5
 */
abstract class BindingAdapter<ITEM : Any, VB : ViewBinding> :
        ListAdapter<ITEM, BindingHolder<VB>>(), BindingScrapProvider<VB> {
    /**
     * Kotlin中类的函数引用，若不是直接作为内联函数的实参，则会编译为单例，
     * 但此处仍然用属性保存函数引用，不重复获取，即使编译规则改了也不受影响。
     */
    private var inflate: Inflate<VB>? = null

    /**
     * 通过[VB]获取[BindingHolder]
     */
    val VB.holder: BindingHolder<VB>
        get() = BindingHolder.getHolder(this)

    @get:AnyThread
    @property:InternalVisibleApi
    final override val scrapType: Int
        get() = getItemViewType(0)

    @get:WorkerThread
    @property:InternalVisibleApi
    final override val scrapInflate: Inflate<VB>
        get() = ensureInflate()

    /**
     * 函数引用`VB::inflate`
     */
    @AnyThread
    protected abstract fun inflate(): Inflate<VB>

    /**
     * 对应[Adapter.onCreateViewHolder]
     *
     * 可以在该函数中完成初始化工作，例如设置点击监听。
     */
    protected open fun VB.onCreateView() = Unit

    /**
     * 对应Adapter.onBindViewHolder(holder, position, payloads)
     */
    protected open fun VB.onBindView(item: ITEM, payloads: List<Any>) = onBindView(item)

    /**
     * 对应[Adapter.onBindViewHolder]
     */
    protected open fun VB.onBindView(item: ITEM) = Unit

    /**
     * 对应[Adapter.onViewRecycled]
     */
    protected open fun VB.onViewRecycled() = Unit

    /**
     * 对应[Adapter.onFailedToRecycleView]
     */
    protected open fun VB.onFailedToRecycleView(): Boolean = false

    /**
     * 对应[Adapter.onViewAttachedToWindow]
     */
    protected open fun VB.onViewAttachedToWindow() = Unit

    /**
     * 对应[Adapter.onViewDetachedFromWindow]
     */
    protected open fun VB.onViewDetachedFromWindow() = Unit

    @AnyThread
    private fun ensureInflate(): Inflate<VB> {
        // 调用多次inflate()，对inflate重复赋值没有影响
        if (inflate == null) inflate = inflate()
        return inflate!!
    }

    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingHolder<VB> {
        val binding = ensureInflate().invoke(parent.inflater, parent, false)
        val holder = BindingHolder(binding)
        binding.onCreateView()
        return holder
    }

    final override fun onCreateScrap(inflater: ScrapInflater): BindingHolder<VB> {
        return super.onCreateScrap(inflater)
    }

    final override fun getItemViewType(position: Int): Int {
        return super.getItemViewType(position)
    }

    final override fun onBindViewHolder(holder: BindingHolder<VB>, item: ITEM, payloads: List<Any>) {
        holder.binding.onBindView(item, payloads)
    }

    final override fun onBindViewHolder(holder: BindingHolder<VB>, item: ITEM) {
        holder.binding.onBindView(item)
    }

    final override fun onViewRecycled(holder: BindingHolder<VB>) {
        holder.binding.onViewRecycled()
    }

    final override fun onFailedToRecycleView(holder: BindingHolder<VB>): Boolean {
        return holder.binding.onFailedToRecycleView()
    }

    final override fun onViewAttachedToWindow(holder: BindingHolder<VB>) {
        super.onViewAttachedToWindow(holder)
        holder.binding.onViewAttachedToWindow()
    }

    final override fun onViewDetachedFromWindow(holder: BindingHolder<VB>) {
        holder.binding.onViewDetachedFromWindow()
    }
}