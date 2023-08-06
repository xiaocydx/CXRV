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
import android.transition.Transition
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.transition.platform.MaterialContainerTransform
import kotlin.reflect.KClass

/**
 * 变换过渡动画的Container，[FragmentActivity]或者[Fragment]实现该接口完成初始化配置
 */
interface TransformContainer {

    fun <C> C.setContentView(
        fragmentClass: KClass<out Fragment>
    ) where C : FragmentActivity, C : TransformContainer {
        setContentView(createContentView(fragmentClass))
    }

    fun <C> C.createContentView(
        fragmentClass: KClass<out Fragment>
    ): View where C : FragmentActivity, C : TransformContainer {
        return TransformSceneRoot(this, supportFragmentManager)
            .apply { installContentFragmentOnAttach(fragmentClass) }
    }

    fun <C> C.createContentView(
        fragmentClass: KClass<out Fragment>
    ): View where C : Fragment, C : TransformContainer {
        val primaryNavigationFragment = this
        return TransformSceneRoot(requireContext(), childFragmentManager)
            .apply { installContentFragmentOnAttach(fragmentClass, primaryNavigationFragment) }
    }

    fun <C> C.disableDecorFitsSystemWindows() where C : FragmentActivity, C : TransformContainer {
        SystemBarsContainer.disableDecorFitsSystemWindows(window)
    }
}

/**
 * 变换过渡动画的Sender，[FragmentActivity]或[Fragment]实现该接口完成页面跳转
 */
interface TransformSender {

    /**
     * 设置参与变换过渡动画的[View]，内部弱引用持有[View]
     *
     * **注意**：若未设置参与变换过渡动画的[View]，则不会运行动画。
     */
    fun <S> S.setTransformView(view: View?) where S : Fragment, S : TransformSender {
        findTransformSceneRoot()?.setTransformView(view)
    }

    /**
     * 跳转至实现了[TransformReceiver]的Fragment，运行变换过渡动画
     *
     * @param transformView 参与变换过渡动画的[View]，内部弱引用持有[View]
     * @param fragmentClass 实现了[TransformReceiver]的Fragment的[Class]
     * @return 若当前已跳转至实现了[TransformReceiver]的Fragment，则返回`false`，表示跳转失败。
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
        return root.installTransformFragment(fragmentClass, args, allowStateLoss)
    }
}

/**
 * 变换过渡动画的Receiver，[Fragment]实现该接口完成完成初始化配置
 */
interface TransformReceiver {

    /**
     * 在`Fragment.activity != null`时，设置`Fragment.enterTransition`
     *
     * @param block 可以调用[MaterialContainerTransform]声明的函数完成初始化配置
     * @return 返回设置为`enterTransition`的[Transition]，可以对其修改属性和添加监听。
     */
    fun <R> R.setTransformEnterTransition(
        block: (MaterialContainerTransform.() -> Unit)? = null
    ): Transition where R : Fragment, R : TransformReceiver {
        val root = requireTransformSceneRoot()
        val transform = MaterialContainerTransform()
        transform.interpolator = AccelerateDecelerateInterpolator()
        block?.invoke(transform)
        val enterTransition = root.createTransformTransition(this, transform)
        this.enterTransition = enterTransition
        return enterTransition
    }
}

private val FragmentActivity.contentParent: ViewGroup
    get() = findViewById(android.R.id.content)

private fun Fragment.requireTransformSceneRoot(): TransformSceneRoot {
    return requireNotNull(findTransformSceneRoot()) {
        "请先调用TransformContainer提供的FragmentActivity.setContentView()，" +
                "或者FragmentActivity.createView()，又或者Fragment.createView()。"
    }
}

private fun View.findTransformSceneRoot(): TransformSceneRoot? = when (this) {
    is TransformSceneRoot -> this
    is ViewGroup -> {
        var view: TransformSceneRoot? = null
        val childCount = childCount
        for (i in 0 until childCount) {
            view = getChildAt(i).findTransformSceneRoot()
            if (view != null) break
        }
        view
    }
    else -> null
}

private fun Fragment.findTransformSceneRoot(): TransformSceneRoot? {
    var parent = parentFragment
    while (parent != null && parent.view !is TransformSceneRoot) {
        parent = parent.parentFragment
    }
    val root = parent?.view as? TransformSceneRoot
    return root ?: activity?.contentParent?.findTransformSceneRoot()
}