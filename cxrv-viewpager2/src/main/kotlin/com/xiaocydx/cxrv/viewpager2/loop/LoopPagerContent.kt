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

import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
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
internal class LoopPagerContent(
    val viewPager2: ViewPager2,
    val adapter: Adapter<*>,
    val extraPageLimit: Int
) {

    init {
        require(extraPageLimit == DEFAULT_EXTRA_PAGE_LIMIT
                || extraPageLimit == PADDING_EXTRA_PAGE_LIMIT)
    }

    /**
     * 当前的内容`item`数量
     */
    val currentCount: Int
        get() = adapter.itemCount

    /**
     * 是否支持循环
     *
     * @param contentCount 默认按[currentCount]进行判断
     */
    fun supportLoop(contentCount: Int = currentCount) = contentCount > 1

    /**
     * 将`bindingAdapterPosition`转换为`layoutPosition`
     *
     * @param bindingAdapterPosition 取值范围是[firstBindingAdapterPosition]到[lastBindingAdapterPosition]。
     * @param contentCount           默认按[currentCount]进行转换。
     * @return 当[bindingAdapterPosition]不处于取值范围时，返回[NO_POSITION]。
     */
    fun toLayoutPosition(bindingAdapterPosition: Int, contentCount: Int = currentCount): Int {
        val first = firstBindingAdapterPosition(contentCount)
        val last = lastBindingAdapterPosition(contentCount)
        if (first == NO_POSITION || bindingAdapterPosition !in first..last) return NO_POSITION
        var layoutPosition = bindingAdapterPosition
        if (supportLoop(contentCount)) layoutPosition += extraPageLimit
        return layoutPosition
    }

    /**
     * `layoutPosition`取值范围第一个值
     *
     * @param contentCount 默认按[currentCount]计算取值范围。
     * @return 若[contentCount]小于等于0，则返回[NO_POSITION]。
     */
    fun firstLayoutPosition(contentCount: Int = currentCount): Int {
        return if (contentCount <= 0) NO_POSITION else 0
    }

    /**
     * `layoutPosition`取值范围最后的值
     *
     * @param contentCount 默认按[currentCount]计算取值范围。
     * @return 若[contentCount]小于等于0，则返回[NO_POSITION]。
     */
    fun lastLayoutPosition(contentCount: Int = currentCount): Int {
        if (contentCount <= 0) return NO_POSITION
        val extraCount = if (!supportLoop(contentCount)) 0 else extraPageLimit * 2
        return contentCount + extraCount - 1
    }

    /**
     * 附加页面的`layoutPosition`取值范围第一个值
     *
     * @param isHeader     `true`-头部，`false`-尾部
     * @param contentCount 默认按[currentCount]计算取值范围。
     * @return 若[contentCount]小于等于0或不支持循环，则返回[NO_POSITION]。
     */
    fun firstExtraLayoutPosition(isHeader: Boolean, contentCount: Int = currentCount): Int {
        if (contentCount <= 0 || !supportLoop(contentCount)) return NO_POSITION
        return if (isHeader) 0 else extraPageLimit + contentCount
    }

    /**
     * 附加页面的`layoutPosition`取值范围最后的值
     *
     * @param isHeader     `true`-头部，`false`-尾部
     * @param contentCount 默认按[currentCount]计算取值范围。
     * @return 若[contentCount]小于等于0或不支持循环，则返回[NO_POSITION]。
     */
    fun lastExtraLayoutPosition(isHeader: Boolean, contentCount: Int = currentCount): Int {
        if (contentCount <= 0 || !supportLoop(contentCount)) return NO_POSITION
        return if (isHeader) extraPageLimit - 1 else extraPageLimit + contentCount + extraPageLimit - 1
    }

    /**
     * 将`layoutPosition`转换为`bindingAdapterPosition`
     *
     * @param layoutPosition 取值范围是[firstLayoutPosition]到[firstLayoutPosition]。
     * @param contentCount   默认按[currentCount]进行转换。
     * @return 当[layoutPosition]不处于取值范围时，返回[NO_POSITION]。
     */
    fun toBindingAdapterPosition(layoutPosition: Int, contentCount: Int = currentCount): Int {
        val first = firstLayoutPosition(contentCount)
        val last = lastLayoutPosition(contentCount)
        if (first == NO_POSITION || layoutPosition !in first..last) return NO_POSITION
        if (!supportLoop(contentCount)) return layoutPosition
        var bindingAdapterPosition = (layoutPosition - extraPageLimit) % currentCount
        if (bindingAdapterPosition < 0) bindingAdapterPosition += currentCount
        return bindingAdapterPosition
    }

    /**
     * `bindingAdapterPosition`取值范围第一个值
     *
     * @param contentCount 默认按[currentCount]计算取值范围。
     * @return 若[contentCount]小于等于0，则返回[NO_POSITION]。
     */
    fun firstBindingAdapterPosition(contentCount: Int = currentCount): Int {
        return if (contentCount <= 0) NO_POSITION else 0
    }

    /**
     * `bindingAdapterPosition`取值范围最后的值
     *
     * @param contentCount 默认按[currentCount]计算取值范围。
     * @return 若[contentCount]小于等于0，则返回[NO_POSITION]。
     */
    fun lastBindingAdapterPosition(contentCount: Int = currentCount): Int {
        return if (contentCount <= 0) NO_POSITION else contentCount - 1
    }

    /**
     * 附加页面的`bindingAdapterPosition`取值范围第一个值
     *
     * @param isHeader     `true`-头部，`false`-尾部
     * @param contentCount 默认按[currentCount]计算取值范围。
     * @return 若[contentCount]小于等于0或不支持循环，则返回[NO_POSITION]。
     */
    fun firstExtraBindingAdapterPosition(isHeader: Boolean, contentCount: Int = currentCount): Int {
        if (contentCount <= 0 || !supportLoop(contentCount)) return NO_POSITION
        return if (isHeader) contentCount - extraPageLimit else 0
    }

    /**
     * 附加页面的`bindingAdapterPosition`取值范围最后的值
     *
     * @param isHeader     `true`-头部，`false`-尾部
     * @param contentCount 默认按[currentCount]计算取值范围。
     * @return 若[contentCount]小于等于0或不支持循环，则返回[NO_POSITION]。
     */
    fun lastExtraBindingAdapterPosition(isHeader: Boolean, contentCount: Int = currentCount): Int {
        if (contentCount <= 0 || !supportLoop(contentCount)) return NO_POSITION
        return if (isHeader) contentCount - 1 else extraPageLimit - 1
    }

    companion object {
        const val DEFAULT_EXTRA_PAGE_LIMIT = 1
        const val PADDING_EXTRA_PAGE_LIMIT = 2
    }
}