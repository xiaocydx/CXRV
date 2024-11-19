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

import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.core.view.doOnDetach
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.savedstate.SavedStateRegistry
import androidx.transition.Transition
import androidx.transition.TransitionSet
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.target.ViewTarget
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform
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

    // 滚动停止才发送事件让sender申请重新布局，避免滚动恢复期间出现卡顿
    viewPager2.registerOnPageChangeCallback(
        onScrollStateChanged = changed@{
            if (it != SCROLL_STATE_IDLE) return@changed
            val id = receiverId() ?: return@changed
            emitEvent(ReceiverEvent.Select(token, id))
        }
    )
    // 当从Window上分离时，尝试发送事件，兼容滚动未停止就退出receiver的情况
    viewPager2.doOnDetach {
        val id = receiverId() ?: return@doOnDetach
        emitEvent(ReceiverEvent.Return(token, id))
    }

    // 页面重建后主动发送事件让sender申请重新布局
    ReceiverEventEmitter.register(receiver)
    if (ReceiverEventEmitter.consume(receiver) && currentId != null) {
        state.emitReceiverEvent(ReceiverEvent.Select(token, currentId!!))
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
        setDefaultConfig()
    }
}

/**
 * 推迟[receiver]的进入过渡动画
 *
 * @param receiver Receiver页面
 * @param canStartEnterTransition 返回`true`，开始进入过渡动画
 */
fun Transform.postponeEnterTransition(
    receiver: Fragment,
    requestManager: RequestManager,
    transitionProvider: TransitionProvider,
    canStartEnterTransition: (target: View) -> Boolean
) {
    TransformPostpone(
        receiver, requestManager,
        transitionProvider,
        canStartEnterTransition
    ).postponeEnterTransition()
}

/**
 * 设置[MaterialContainerTransform]
 *
 * 使用Sender设置的`root`跟Receiver的`view`完成过渡
 */
fun Transform.setTransformTransition(
    receiver: Fragment,
    initializer: (MaterialContainerTransform.() -> Unit)? = null
): Transition {
    val state = receiver.requireActivity().transformState
    val transform = MaterialContainerTransform()
    initializer?.invoke(transform)
    val transition = TransformTransition(transform) { start ->
        if (receiver.isAdded) {
            if (start) state.getSenderRoot() else receiver.view
        } else {
            if (start) receiver.view else state.getSenderRoot()
        }
    }
    transition.setDefaultConfig()
    receiver.enterTransition = transition
    receiver.returnTransition = transition
    return transition
}

private fun Transition.setDefaultConfig() {
    // 设置运动路径
    setPathMotion(MaterialArcMotion())
    duration = 250
    interpolator = AccelerateDecelerateInterpolator()
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

private object ReceiverEventEmitter : SavedStateRegistry.SavedStateProvider {
    private const val KEY = "com.xiaocydx.accompanist.transition.transform.ReceiverEventEmitter"

    override fun saveState() = Bundle(1)

    fun register(receiver: Fragment) {
        receiver.savedStateRegistry.unregisterSavedStateProvider(KEY)
        receiver.savedStateRegistry.registerSavedStateProvider(KEY, this)
    }

    fun consume(receiver: Fragment): Boolean {
        return receiver.savedStateRegistry.consumeRestoredStateForKey(KEY) != null
    }
}

private class TransformPostpone(
    private var fragment: Fragment?,
    private var requestManager: RequestManager?,
    private var transitionProvider: TransitionProvider?,
    private var canStartEnterTransition: ((target: View) -> Boolean)?
) {

    fun postponeEnterTransition() {
        fragment?.apply {
            enterTransition = transitionProvider?.create(true)
            returnTransition = transitionProvider?.create(false)
            postponeEnterTransition()
        }
        requestManager?.addDefaultRequestListener(object : RequestCompleteListener() {
            override fun onComplete(target: Target<Any>?) {
                if (fragment == null || target !is ViewTarget<*, *>) return
                if (canStartEnterTransition?.invoke(target.view) != true) return
                startPostponedEnterTransition()
            }
        })
    }

    private fun startPostponedEnterTransition() {
        fragment?.startPostponedEnterTransition()
        fragment = null
        requestManager = null
        transitionProvider = null
        transitionProvider = null
        canStartEnterTransition = null
    }

    private abstract class RequestCompleteListener : RequestListener<Any> {
        protected abstract fun onComplete(target: Target<Any>?)

        override fun onLoadFailed(
            e: GlideException?, model: Any?,
            target: Target<Any>?, isFirstResource: Boolean
        ): Boolean {
            onComplete(target)
            return false
        }

        override fun onResourceReady(
            resource: Any?, model: Any?, target:
            Target<Any>?, dataSource: DataSource?,
            isFirstResource: Boolean
        ): Boolean {
            onComplete(target)
            return false
        }
    }
}