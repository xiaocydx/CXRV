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

@file:Suppress("SpellCheckingInspection")

package com.xiaocydx.cxrv.animatable

import android.graphics.drawable.Animatable
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.cxrv.list.Disposable

/**
 * 设置控制[Animatable]的中间者
 *
 * ```
 * recyclerView.setAnimatableMediator {
 *     controlledByScroll() // 受RecyclerView滚动控制
 *     controlledByParentViewPager2(viewPager2) // 受父级ViewPager2滚动控制
 *     controlledByLifecycle(lifecycle, Lifecycle.State.RESUMED) // 受Lifecycle控制
 *     registerImageView(adapter) { imageView } // 提供ImageView
 * }
 * ```
 */
inline fun RecyclerView.setAnimatableMediator(initialize: AnimatableMediator.() -> Unit): Disposable {
    return AnimatableMediatorImpl(this).apply(initialize).attach()
}

/**
 * 负责[AnimatableProvider]和[AnimatableController]之间的调度
 *
 * 在视图树draw之前，[AnimatableMediator]会判断[canStartAnimatable]，
 * 若[canStartAnimatable]为`false`，则调用[stopAllAnimatable]停止全部。
 */
interface AnimatableMediator : Disposable {

    /**
     * 关联的[RecyclerView]
     */
    val recyclerView: RecyclerView

    /**
     * 是否允许执行[startAllAnimatable]
     *
     * 该属性的结果，由全部的[AnimatableController.canStartAnimatable]决定。
     */
    val canStartAnimatable: Boolean

    /**
     * 遍历[recyclerView]的全部子View，调用[Animatable.start]
     *
     * **注意**：当[canStartAnimatable]为`true`时，该函数才会生效。
     */
    fun startAllAnimatable()

    /**
     * 遍历[recyclerView]的全部子View，调用[Animatable.stop]
     */
    fun stopAllAnimatable()

    /**
     * 添加[AnimatableProvider]
     *
     * 当[startAllAnimatable]和[stopAllAnimatable]被调用时，
     * 会调用`provider.getAnimatableOrNull()`获取[Animatable]。
     */
    fun addAnimatableProvider(provider: AnimatableProvider)

    /**
     * 移除[AnimatableProvider]
     */
    fun removeAnimatableProvider(provider: AnimatableProvider)

    /**
     * 添加[AnimatableController]
     *
     * [controller]可以根据情况，调用[startAllAnimatable]和[stopAllAnimatable]。
     */
    fun addAnimatableController(controller: AnimatableController)

    /**
     * 移除[AnimatableController]
     */
    fun removeAnimatableController(controller: AnimatableController)

    /**
     * 查找父类或者自身等于[clazz]的[AnimatableController]
     */
    fun <T : AnimatableController> findAnimatableController(clazz: Class<T>): T?
}

/**
 * 查找父类或者自身等于`T::class.java`的[AnimatableController]
 */
inline fun <reified T : AnimatableController> AnimatableMediator.findAnimatableController(): T? {
    return findAnimatableController(T::class.java)
}

/**
 * [Animatable]的提供者
 *
 * 常见的动图加载场景，提供的[Animatable]是[ImageView.getDrawable]，
 * 可以对[ViewHolder]实现[Animatable]，统一控制内部多个[Animatable]。
 */
interface AnimatableProvider : Disposable {

    /**
     * 获取[Animatable]
     *
     * @return 返回`null`通常是[holder]的属性不符合匹配条件，或者动图还未加载完成，
     * [AnimatableMediator]会调用返回结果的[Animatable.start]和[Animatable.stop]。
     */
    fun getAnimatableOrNull(holder: ViewHolder): Animatable?

    /**
     * 是否可以执行[Animatable.start]，属于个体控制条件
     *
     * @param animatable [getAnimatableOrNull]返回的非`null`结果
     */
    fun canStartAnimatable(holder: ViewHolder, animatable: Animatable): Boolean
}

/**
 * [Animatable]的控制器
 *
 * 常见的控制场景：
 * 1. 滚动容器滚动时控制[Animatable]，例如RecyclerView。
 * 2. 父级滚动容器滚动时控制[Animatable]，例如ViewPager2嵌套RecyclerView。
 * 3. Lifecycle状态更改时控制[Animatable]，例如Fragment的`viewLifecycle`。
 */
interface AnimatableController : Disposable {

    /**
     * 是否可以执行[Animatable.start]，属于整体控制条件
     *
     * 1. 该属性会参与[AnimatableMediator.startAllAnimatable]的逻辑判断，
     * 当[canStartAnimatable]为`true`时，`startAllAnimatable()`才会生效。
     *
     * 2. [AnimatableMediator]会在视图树draw之前判断该属性，
     * 当[canStartAnimatable]为`false`时，调用[AnimatableMediator.stopAllAnimatable]。
     * 这种处理方式可以简化大部分场景的实现，例如RecyclerView滚动过程或者动图加载完成时，
     * 会申请下一帧重绘，在下一帧视图树draw之前判断该属性。
     */
    val canStartAnimatable: Boolean
}