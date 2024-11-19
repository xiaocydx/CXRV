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

@file:Suppress("UnusedReceiverParameter")

package com.xiaocydx.accompanist.transition.transform

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.core.view.doOnDetach
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.transition.Transition
import androidx.transition.TransitionSet
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE
import com.google.android.material.transition.MaterialArcMotion
import com.xiaocydx.accompanist.view.setRoundRectOutlineProvider
import com.xiaocydx.accompanist.viewpager2.registerOnPageChangeCallback

/**
 * 设置[receiver]的事件发射器，适用于ViewPager2场景
 */
fun Transform.setReceiverEventEmitter(
    token: String,
    receiver: Fragment,
    viewPager2: ViewPager2,
    receiverId: () -> String?
) = viewPager2.doOnLayout {
    var currentId = receiverId()
    val state = receiver.requireActivity().transformState
    val emitEvent = fun(event: ReceiverEvent) {
        if (currentId == event.id) return
        currentId = event.id
        state.emitReceiverEvent(event)
    }
    viewPager2.registerOnPageChangeCallback(
        onScrollStateChanged = changed@{
            if (it != SCROLL_STATE_IDLE) return@changed
            val id = receiverId() ?: return@changed
            emitEvent(ReceiverEvent.Select(token, id))
        }
    )
    viewPager2.doOnDetach {
        val id = receiverId() ?: return@doOnDetach
        emitEvent(ReceiverEvent.Return(token, id))
    }
}

/**
 * 创建[Transition]的提供者
 *
 * 圆角值的捕获规则：
 * 1. 若`image`调用了[View.setRoundRectOutlineProvider]，则捕获设置的圆角值。
 * 2. 若`root`调用了[View.setRoundRectOutlineProvider]，并且`root`和`image`的尺寸一致，则捕获设置的圆角值。
 *
 * @param receiver Receiver页面
 * @param receiverRoot [receiverImage]的父级，当过渡动画开始时，`root.alpha = 0f`
 * @param receiverImage 进行`ImageView.imageMatrix`变换的[ImageView]
 * @param imageDecoration 绘制[receiverImage]之前和之后的绘制时机。
 */
fun Transform.createTransitionProvider(
    receiver: Fragment,
    receiverRoot: () -> View?,
    receiverImage: () -> ImageView?,
    imageDecoration: ImageTransform.Decoration? = null
): TransitionProvider = TransitionProvider { isEnter ->
    val state = receiver.requireActivity().transformState
    val senderRoot = { state.getSenderRoot() }
    val senderImage = { state.getSenderImage() }
    TransitionSet().apply {
        addTransition(FadeTransform(receiver))
        addTransition(ImageTransform(
            isEnter, senderRoot, senderImage,
            receiverRoot, receiverImage, imageDecoration))
        // 设置运动路径
        setPathMotion(MaterialArcMotion())
        duration = 250
        interpolator = AccelerateDecelerateInterpolator()
    }
}

sealed class ReceiverEvent(internal val token: String, val id: String) {
    class Select(token: String, id: String) : ReceiverEvent(token, id)
    class Return(token: String, id: String) : ReceiverEvent(token, id)
}

/**
 * [Transition]提供者
 */
fun interface TransitionProvider {

    /**
     * 创建[Transition]
     *
     * @param isEnter `true`-进入过渡动画，`false`-退出过渡动画
     */
    fun create(isEnter: Boolean): Transition
}

/**
 * 转换[TransitionProvider]，可用于修改[Transition]的属性
 *
 * ```
 * var provider: TransitionProvider = ...
 * provider = provider.toProvider { isEnter, transition ->
 *     transition.duration = 500
 *     transition.setPathMotion(null)
 * }
 * ```
 */
fun TransitionProvider.toProvider(
    block: (isEnter: Boolean, transition: Transition) -> Unit
) = TransitionProvider { isEnter ->
    val transition = create(isEnter)
    block(isEnter, transition)
    transition
}