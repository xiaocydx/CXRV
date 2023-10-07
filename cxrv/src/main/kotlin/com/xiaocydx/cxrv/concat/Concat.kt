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

import androidx.annotation.CheckResult
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.ConcatAdapter.Config
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.xiaocydx.cxrv.list.InlineList
import com.xiaocydx.cxrv.list.accessEach
import com.xiaocydx.cxrv.list.isHeaderOrFooter

/**
 * [ConcatAdapter]的构建类，[header]、[content]、[footer]之间没有调用顺序限制
 *
 * **注意**：
 * 1. 此类不是[ConcatAdapter]构造函数的替代品，而是用于连接[headers]、[contents]、[footers]的场景。
 * 2. 此类不允许[ConcatAdapter]嵌套[ConcatAdapter]，无法统一隔离ViewType配置，不满足[concat]的意图，
 * 此外，[SpanSizeProvider]也不支持[ConcatAdapter]嵌套[ConcatAdapter]。
 * ```
 * val concatAdapter = Concat
 *     .header(headerAdapter)
 *     .content(contentAdapter1)
 *     .content(contentAdapter2)
 *     .footer(footerAdapter)
 *     .concat()
 * ```
 */
open class Concat private constructor() {
    private var headers = InlineList<ViewAdapter<*>>()
    private var contents = InlineList<Adapter<*>>()
    private var footers = InlineList<ViewAdapter<*>>()

    /**
     * 添加[header]，[concat]按添加顺序连接多个`header`
     *
     * @param header 类型约束为[ViewAdapter]，可用于[RecyclerView.isHeaderOrFooter]
     */
    @CheckResult
    open fun header(header: ViewAdapter<*>) = apply { headers += header }

    /**
     * 添加[content]，[concat]按添加顺序连接多个`content`
     *
     * @param content 若类型为[ConcatAdapter]，则抛出[IllegalArgumentException]，
     * [concat]不使用隔离ViewType配置，[content]需要确保不跟其他`content`产生冲突。
     */
    @CheckResult
    open fun content(content: Adapter<*>) = apply { contents += requireNotConcat(content) }

    /**
     * 添加[footer]，[concat]按添加顺序连接多个`footer`
     *
     * @param footer 类型约束为[ViewAdapter]，可用于[RecyclerView.isHeaderOrFooter]
     */
    @CheckResult
    open fun footer(footer: ViewAdapter<*>) = apply { footers += footer }

    /**
     * 连接[headers]、[contents]、[footers]，构建[ConcatAdapter]
     *
     * 该函数构建的[ConcatAdapter]，不使用隔离ViewType配置，
     * 在共享[RecycledViewPool]的场景下，隔离ViewType配置
     * 可能会导致获取已回收的ViewHolder，抛出类型转换异常。
     *
     * ### 场景描述
     * FragmentA和FragmentB共享[RecycledViewPool]，并且内容区Adapter相同。
     *
     * FragmentA有Header和内容区Adapter，使用隔离ViewType配置:
     * 1. Header的隔离ViewType = 0。
     * 2. 内容区itemView的隔离ViewType = 1。
     *
     * FragmentB只有内容区Adapter，itemView的ViewType = 0，
     * 从[RecycledViewPool]中可能会取出Header的ViewHolder，
     * 跟内容区的ViewHolder类型不一致，导致抛出类型转换异常。
     */
    fun concat(): ConcatAdapter {
        val config = Config.Builder().setIsolateViewTypes(false).build()
        return ConcatAdapter(config, getAdapters())
    }

    private fun requireNotConcat(content: Adapter<*>): Adapter<*> {
        require(content !is ConcatAdapter) { "content的类型不能为ConcatAdapter" }
        return content
    }

    private fun getAdapters(): List<Adapter<*>> {
        val size = headers.size + contents.size + footers.size
        val adapters = ArrayList<Adapter<*>>(size)
        headers.accessEach(adapters::add)
        contents.accessEach(adapters::add)
        footers.accessEach(adapters::add)
        return adapters
    }

    @CheckResult
    internal open fun headerOrNull(header: ViewAdapter<*>?) = apply { header?.let { headers += it } }

    @CheckResult
    internal open fun footerOrNull(footer: ViewAdapter<*>?) = apply { footer?.let { footers += it } }

    internal fun concatIfNecessary(): Adapter<*> {
        val adapters = getAdapters()
        if (adapters.size == 1) return adapters.first()
        val config = Config.Builder().setIsolateViewTypes(false).build()
        return ConcatAdapter(config, adapters)
    }

    companion object : Concat() {
        override fun header(header: ViewAdapter<*>) = Concat().header(header)
        override fun content(content: Adapter<*>) = Concat().content(content)
        override fun footer(footer: ViewAdapter<*>) = Concat().footer(footer)
        override fun headerOrNull(header: ViewAdapter<*>?) = Concat().headerOrNull(header)
        override fun footerOrNull(footer: ViewAdapter<*>?) = Concat().footerOrNull(footer)
    }
}