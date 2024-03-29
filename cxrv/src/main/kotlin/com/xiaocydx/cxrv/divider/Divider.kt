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

package com.xiaocydx.cxrv.divider

import android.content.Context
import androidx.annotation.Px
import androidx.recyclerview.widget.LayoutManagerCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.xiaocydx.cxrv.list.grid
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.staggered

/**
 * 添加通用间隔，可用于链式调用场景，默认排除Header和Footer
 *
 * @param width  水平方向的间隔size
 * @param height 垂直方向的间隔size
 */
@Deprecated("不需要其它含义的扩展函数", ReplaceWith("divider(width, height)"))
fun <T : RecyclerView> T.spacing(
    @Px width: Int = 0,
    @Px height: Int = 0
): T = divider(width, height)

/**
 * 添加通用分割线，可用于链式调用场景，默认排除Header和Footer
 *
 * ```
 * recyclerView.divider(5.dp, 5.dp) {
 *     edge(Edge.top().bottom())
 *     color(0xFF9DAA8F.toInt())
 * }
 * ```
 * 详细的属性描述[DividerItemDecoration.Config]
 *
 * **注意**：[DividerItemDecoration]仅支持实现了[LayoutManagerCompat]的兼容类，
 * 若LayoutManager不是兼容类，则[DividerItemDecoration]会在控制台打印警告信息，
 * 提示可以通过[linear]、[grid]、[staggered]使用兼容类。
 */
inline fun <T : RecyclerView> T.divider(
    @Px width: Int = 0,
    @Px height: Int = 0,
    block: DividerItemDecoration.Config.() -> Unit = {}
): T {
    setDividerItemDecoration(DividerItemDecoration(context) {
        width(width).height(height).apply(block)
    })
    return this
}

/**
 * 创建通用分割线，默认排除Header和Footer
 *
 * ```
 * DividerItemDecoration(context) {
 *     width(5.dp).height(5.dp)
 *     edge(Edge.top().bottom())
 *     color(0xFF9DAA8F.toInt())
 * }
 * ```
 * 详细的属性描述[DividerItemDecoration.Config]
 */
inline fun DividerItemDecoration(
    context: Context,
    block: DividerItemDecoration.Config.() -> Unit
) = DividerItemDecoration.Config(context).apply(block).build()

/**
 * 添加匹配[adapter]的通用分割线，跟[divider]不能同时调用
 *
 * ```
 * recyclerView.addDividerItemDecoration(subAdapter1) {
 *     size(5.dp).edge(Edge.all()).color(0xFF979EC4.toInt())
 * }
 * recyclerView.addDividerItemDecoration(subAdapter2) {
 *     size(5.dp).edge(Edge.all()).color(0xFFD77F7A.toInt())
 * }
 * ```
 */
inline fun RecyclerView.addDividerItemDecoration(
    adapter: Adapter<*>,
    block: DividerItemDecoration.Config.() -> Unit
) = DividerItemDecoration.Config(context)
    .adapter(adapter).apply(block)
    .build().also(::addItemDecoration)

@PublishedApi
internal fun RecyclerView.setDividerItemDecoration(decor: DividerItemDecoration) {
    ensureWithoutDividerItemDecoration()
    addItemDecoration(decor)
}

private fun RecyclerView.ensureWithoutDividerItemDecoration() {
    for (index in itemDecorationCount - 1 downTo 0) {
        val decor = getItemDecorationAt(index)
        if (decor !is DividerItemDecoration) continue
        removeItemDecorationAt(index)
    }
}