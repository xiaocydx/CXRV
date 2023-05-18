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

package com.xiaocydx.cxrv.viewpager2.loop

import androidx.recyclerview.widget.RecyclerView.*
import androidx.viewpager2.widget.ViewPager2

/**
 * [ViewPager2]循环页面的内容，提供`position`的计算函数
 *
 * 例如A、B、C是原始页面，带`*`的是附加页面，[extraPageLimit] = `2`：
 * ```
 * {B* ，C* ，A ，B ，C ，A* ，B*}
 *
 * currentCount = 3
 * supportLoop = true
 * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
 * layoutPositionRange = [0, 6]
 * headerLayoutPositionRange = [0, 1]
 * footerLayoutPositionRange = [5, 6]
 *
 * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
 * bindingAdapterPositionRange = [0, 2]
 * headerBindingAdapterPositionRange = [1, 2]
 * footerBindingAdapterPositionRange = [0, 1]
 * ```
 *
 * **注意**：此类的函数仅返回数值，不返回[IntRange]，
 * 因为这会创建对象，对频繁调用此类函数的场景不友好。
 *
 * @author xcc
 * @date 2023/5/11
 */
internal class LoopPagerContent private constructor(
    val viewPager2: ViewPager2,
    val adapter: Adapter<*>,
    val extraPageLimit: Int,
    private var observer: RecordDataObserver?
) {
    init {
        require(extraPageLimit == DEFAULT_EXTRA_PAGE_LIMIT
                || extraPageLimit == PADDING_EXTRA_PAGE_LIMIT)
        observer?.let(adapter::registerAdapterDataObserver)
    }

    constructor(viewPager2: ViewPager2, adapter: Adapter<*>, extraPageLimit: Int) :
            this(viewPager2, adapter, extraPageLimit, observer = null)

    /**
     * 之前的内容`item`数量
     *
     * [Adapter]对[AdapterDataObserver]是反向遍历分发，[RecordDataObserver]最先添加，最后分发，
     * `previous.count`仅在分发过程表示之前的内容`item`数量，分发完成后`previous,count`为最新值。
     */
    val previous = when (observer) {
        null -> LoopPagerContent(
            viewPager2,
            adapter,
            extraPageLimit,
            RecordDataObserver(adapter)
        )
        else -> this
    }

    /**
     * 内容`item`数量
     */
    val count: Int
        get() = observer?.lastItemCount ?: adapter.itemCount

    fun removeObserver() {
        if (previous !== this) previous.removeObserver()
        observer?.let(adapter::unregisterAdapterDataObserver)
        observer = null
    }

    /**
     * 是否支持循环
     */
    fun supportLoop() = count > 1

    /**
     * 将`bindingAdapterPosition`转换为`layoutPosition`
     *
     * @param bindingAdapterPosition 取值范围是[firstBindingAdapterPosition]到[lastBindingAdapterPosition]。
     * @return 当[bindingAdapterPosition]不处于取值范围时，返回[NO_POSITION]。
     */
    fun toLayoutPosition(bindingAdapterPosition: Int): Int {
        val first = firstBindingAdapterPosition()
        val last = lastBindingAdapterPosition()
        if (first == NO_POSITION || bindingAdapterPosition !in first..last) return NO_POSITION
        var layoutPosition = bindingAdapterPosition
        if (supportLoop()) layoutPosition += extraPageLimit
        return layoutPosition
    }

    /**
     * `layoutPosition`取值范围第一个值
     *
     * @return 若[count]小于等于0，则返回[NO_POSITION]。
     */
    fun firstLayoutPosition(): Int {
        return if (count <= 0) NO_POSITION else 0
    }

    /**
     * `layoutPosition`取值范围最后的值
     *
     * @return 若[count]小于等于0，则返回[NO_POSITION]。
     */
    fun lastLayoutPosition(): Int {
        if (count <= 0) return NO_POSITION
        val extraCount = if (!supportLoop()) 0 else extraPageLimit * 2
        return count + extraCount - 1
    }

    /**
     * 附加页面的`layoutPosition`取值范围第一个值
     *
     * @param isHeader `true`-头部，`false`-尾部
     * @return 若[count]小于等于0或不支持循环，则返回[NO_POSITION]。
     */
    fun firstExtraLayoutPosition(isHeader: Boolean): Int {
        if (count <= 0 || !supportLoop()) return NO_POSITION
        return if (isHeader) 0 else extraPageLimit + count
    }

    /**
     * 附加页面的`layoutPosition`取值范围最后的值
     *
     * @param isHeader `true`-头部，`false`-尾部
     * @return 若[count]小于等于0或不支持循环，则返回[NO_POSITION]。
     */
    fun lastExtraLayoutPosition(isHeader: Boolean): Int {
        if (count <= 0 || !supportLoop()) return NO_POSITION
        return if (isHeader) extraPageLimit - 1 else extraPageLimit + count + extraPageLimit - 1
    }

    /**
     * 将`layoutPosition`转换为`bindingAdapterPosition`
     *
     * @param layoutPosition 取值范围是[firstLayoutPosition]到[firstLayoutPosition]。
     * @return 当[layoutPosition]不处于取值范围时，返回[NO_POSITION]。
     */
    fun toBindingAdapterPosition(layoutPosition: Int): Int {
        val first = firstLayoutPosition()
        val last = lastLayoutPosition()
        if (first == NO_POSITION || layoutPosition !in first..last) return NO_POSITION
        if (!supportLoop()) return layoutPosition
        var bindingAdapterPosition = (layoutPosition - extraPageLimit) % count
        if (bindingAdapterPosition < 0) bindingAdapterPosition += count
        return bindingAdapterPosition
    }

    /**
     * `bindingAdapterPosition`取值范围第一个值
     *
     * @return 若[count]小于等于0，则返回[NO_POSITION]。
     */
    fun firstBindingAdapterPosition(): Int {
        return if (count <= 0) NO_POSITION else 0
    }

    /**
     * `bindingAdapterPosition`取值范围最后的值
     *
     * @return 若[count]小于等于0，则返回[NO_POSITION]。
     */
    fun lastBindingAdapterPosition(): Int {
        return if (count <= 0) NO_POSITION else count - 1
    }

    /**
     * 附加页面的`bindingAdapterPosition`取值范围第一个值
     *
     * @param isHeader `true`-头部，`false`-尾部
     * @return 若[count]小于等于0或不支持循环，则返回[NO_POSITION]。
     */
    fun firstExtraBindingAdapterPosition(isHeader: Boolean): Int {
        if (count <= 0 || !supportLoop()) return NO_POSITION
        return if (isHeader) count - extraPageLimit else 0
    }

    /**
     * 附加页面的`bindingAdapterPosition`取值范围最后的值
     *
     * @param isHeader `true`-头部，`false`-尾部
     * @return 若[count]小于等于0或不支持循环，则返回[NO_POSITION]。
     */
    fun lastExtraBindingAdapterPosition(isHeader: Boolean): Int {
        if (count <= 0 || !supportLoop()) return NO_POSITION
        return if (isHeader) count - 1 else extraPageLimit - 1
    }

    companion object {
        const val DEFAULT_EXTRA_PAGE_LIMIT = 1
        const val PADDING_EXTRA_PAGE_LIMIT = 2
    }
}