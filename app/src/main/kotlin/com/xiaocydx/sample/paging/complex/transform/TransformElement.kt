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
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.transition.Transition
import com.google.android.material.transition.MaterialContainerTransform
import com.xiaocydx.sample.matchParent
import kotlin.reflect.KClass

/**
 * @author xcc
 * @date 2023/8/1
 */
interface TransformContainer {

    fun FragmentActivity.setTransformContentView(child: View) {
        setContentView(child)
        installTransformContainer()
    }

    fun FragmentActivity.installTransformContainer() {
        val contentParent = contentParent
        if (contentParent.findTransformRootView() != null) return
        val rootView = TransformRootView(this)
        contentParent.addView(rootView, matchParent, matchParent)
    }
}

interface TransformSender {

    fun FragmentActivity.setTransformTargetView(view: View?) {
        requireTransformRootView().setSenderView(view)
    }

    // TODO: 处理防抖
    fun FragmentActivity.showTransformFragment(
        senderView: View,
        fragmentClass: KClass<out Fragment>,
        args: Bundle? = null,
        tag: String? = null,
        allowStateLoss: Boolean = false
    ) {
        val rootView = requireTransformRootView()
        rootView.setSenderView(senderView)
        supportFragmentManager.addTransformFragment(
            fragmentClass, rootView.id,
            args, tag, allowStateLoss
        )
    }

    fun Fragment.setTransformTargetView(view: View?) =
            requireActivity().setTransformTargetView(view)

    // TODO: 处理防抖
    fun Fragment.showTransformFragment(
        targetView: View,
        fragmentClass: KClass<out Fragment>,
        args: Bundle? = null,
        tag: String? = null,
        allowStateLoss: Boolean = false
    ) = requireActivity().showTransformFragment(
        targetView, fragmentClass,
        args, tag, allowStateLoss
    )
}

interface TransformReceiver {

    fun Fragment.setTransformEnterTransition(
        transform: MaterialContainerTransform = MaterialContainerTransform()
    ): Transition {
        val rootView = requireActivity().requireTransformRootView()
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

private fun FragmentManager.addTransformFragment(
    fragmentClass: KClass<out Fragment>,
    @IdRes containerViewId: Int,
    args: Bundle? = null,
    tag: String? = null,
    allowStateLoss: Boolean = false
) {
    val transaction = beginTransaction()
        .addToBackStack(null)
        .add(containerViewId, fragmentClass.java, args, tag)
    if (allowStateLoss) {
        transaction.commitAllowingStateLoss()
    } else {
        transaction.commit()
    }
}