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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.transition.MaterialContainerTransform
import com.xiaocydx.sample.matchParent
import kotlin.reflect.KClass

/**
 * 变换过渡动画的Container，[FragmentActivity]实现该接口完成初始化配置
 */
interface TransformContainer {

    /**
     * 设置`contentView`的同时，调用[installTransformContainer]
     *
     * ```
     * class TransformContainerActivity : AppCompatActivity(), TransformContainer {
     *
     *     override fun onCreate(savedInstanceState: Bundle?) {
     *         super.onCreate(savedInstanceState)
     *         setTransformContentView(contentView)
     *     }
     * }
     * ```
     */
    fun <C> C.setTransformContentView(view: View) where C : FragmentActivity, C : TransformContainer {
        setContentView(view)
        installTransformContainer()
    }

    /**
     * 若调用者已对`contentView`进行单独的处理，则自行调用该函数
     *
     * ```
     * class TransformContainerActivity : AppCompatActivity(), TransformContainer {
     *
     *     override fun onCreate(savedInstanceState: Bundle?) {
     *         super.onCreate(savedInstanceState)
     *         // 已对contentView进行单独的处理
     *         installTransformContainer()
     *     }
     * }
     * ```
     */
    fun <C> C.installTransformContainer() where C : FragmentActivity, C : TransformContainer {
        val contentParent = contentParent
        if (contentParent.findTransformRootView() != null) return
        val rootView = TransformRootView(this, supportFragmentManager)
        contentParent.addView(rootView, matchParent, matchParent)
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
    fun <S> S.setTransformView(view: View?) where S : FragmentActivity, S : TransformSender {
        requireTransformRootView().setSenderView(view)
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
    ): Boolean where S : FragmentActivity, S : TransformSender,
                     R : Fragment, R : TransformReceiver {
        val rootView = requireTransformRootView()
        rootView.setSenderView(transformView)
        return rootView.showTransformFragment(fragmentClass, args, allowStateLoss)
    }

    /**
     * 设置参与变换过渡动画的[View]，内部弱引用持有[View]
     *
     * **注意**：若未设置参与变换过渡动画的[View]，则不会运行动画。
     */
    fun <S> S.setTransformView(view: View?) where S : Fragment, S : TransformSender {
        activity?.requireTransformRootView()?.setSenderView(view)
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
        val activity = activity ?: return false
        val rootView = activity.requireTransformRootView()
        rootView.setSenderView(transformView)
        return rootView.showTransformFragment(fragmentClass, args, allowStateLoss)
    }
}

/**
 * 变换过渡动画的Receiver，[Fragment]实现该接口完成完成初始化配置
 */
interface TransformReceiver {

    /**
     * 在`Fragment.activity != null`时，设置`Fragment.enterTransition`
     *
     * @param block 可以调整[MaterialContainerTransform]的属性，但不能对其添加监听
     * @return 返回设置为`enterTransition`的[Transition]，可以对其调整属性和添加监听。
     */
    fun <R> R.setTransformEnterTransition(
        block: (MaterialContainerTransform.() -> Unit)? = null
    ): Transition where R : Fragment, R : TransformReceiver {
        // 该函数属于初始化配置，应当调用requireActivity()确保调用时机
        val rootView = requireActivity().requireTransformRootView()
        val transform = MaterialContainerTransform()
        block?.invoke(transform)
        val enterTransition = rootView.createTransition(this, transform)
        this.enterTransition = enterTransition
        return enterTransition
    }
}

private val FragmentActivity.contentParent: ViewGroup
    get() = findViewById(android.R.id.content)

private fun FragmentActivity.requireTransformRootView(): TransformRootView {
    return requireNotNull(contentParent.findTransformRootView()) {
        "请先调用FragmentActivity.setTransformContentView()"
    }
}

private fun View.findTransformRootView(): TransformRootView? = when (this) {
    is TransformRootView -> this
    is ViewGroup -> {
        var view: TransformRootView? = null
        val childCount = childCount
        for (i in 0 until childCount) {
            view = getChildAt(i).findTransformRootView()
            if (view != null) break
        }
        view
    }
    else -> null
}