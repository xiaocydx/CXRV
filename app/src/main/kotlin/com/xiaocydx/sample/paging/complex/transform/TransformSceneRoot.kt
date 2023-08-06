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

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.transition.Transition
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.doOnAttach
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.Lifecycle.State.STARTED
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.xiaocydx.sample.R
import com.xiaocydx.sample.layoutParams
import com.xiaocydx.sample.matchParent
import java.lang.ref.WeakReference
import kotlin.reflect.KClass

/**
 * 变换过渡动画的SceneRoot
 *
 * @author xcc
 * @date 2023/8/1
 */
@SuppressLint("ViewConstructor")
internal class TransformSceneRoot(
    context: Context,
    private val fragmentManager: FragmentManager,
) : FrameLayout(context) {
    private var hasContentPendingTransaction = false
    private var hasTransformPendingTransaction = false
    private var transformViewRef: WeakReference<View>? = null
    private var transformCallbacks = TransformFragmentLifecycleCallbacks()
    private val contentFragment: Fragment?
        get() = fragmentManager.findFragmentByTag(TAG_CONTENT)
    private val transformFragment: Fragment?
        get() = fragmentManager.findFragmentByTag(TAG_TRANSFORM)

    init {
        layoutParams(matchParent, matchParent)
        id = R.id.transform_scene_root_fragment_container
    }

    // TODO: 防止创建两个
    fun installContentFragmentOnAttach(
        fragmentClass: KClass<out Fragment>,
        primaryNavigationFragment: Fragment? = null
    ): Boolean {
        if (hasContentPendingTransaction || contentFragment != null) return false
        hasContentPendingTransaction = true
        doOnAttach {
            require(contentFragment == null)
            hasContentPendingTransaction = false
            if (primaryNavigationFragment != null) {
                require(primaryNavigationFragment.view === this)
                primaryNavigationFragment.parentFragmentManager.beginTransaction()
                    .setPrimaryNavigationFragment(primaryNavigationFragment)
                    .commitNow()
            }
            fragmentManager.beginTransaction()
                .add(id, fragmentClass.java, null, TAG_CONTENT)
                .commitNow()
        }
        return true
    }

    fun installTransformFragment(
        fragmentClass: KClass<out Fragment>,
        args: Bundle? = null,
        allowStateLoss: Boolean = false
    ): Boolean {
        if (hasTransformPendingTransaction || transformFragment != null) return false
        hasTransformPendingTransaction = true
        setContentFragmentMaxLifecycle(STARTED)
        val transaction = fragmentManager
            .beginTransaction()
            .setReorderingAllowed(true)
            .addToBackStack(null)
            .add(id, fragmentClass.java, args, TAG_TRANSFORM)
        if (allowStateLoss) {
            transaction.commitAllowingStateLoss()
        } else {
            transaction.commit()
        }
        return true
    }

    fun setTransformView(view: View?) {
        if (transformViewRef?.get() === view) return
        transformViewRef = WeakReference(view)
    }

    fun createTransformTransition(fragment: Fragment, transform: MaterialContainerTransform): Transition {
        val senderView = { transformViewRef?.get() }
        val fragmentRef = WeakReference(fragment)
        return TransformTransition(sceneRootId = id, senderView, fragmentRef, transform)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        fragmentManager.registerFragmentLifecycleCallbacks(transformCallbacks, false)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        fragmentManager.unregisterFragmentLifecycleCallbacks(transformCallbacks)
    }

    private fun setContentFragmentMaxLifecycle(state: State) {
        val contentFragment = contentFragment ?: return
        fragmentManager.beginTransaction()
            .setMaxLifecycle(contentFragment, state)
            .commitNow()
    }

    private inner class TransformFragmentLifecycleCallbacks : FragmentLifecycleCallbacks() {
        override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
            // TODO: 补充更多判断条件
            if (f.tag === TAG_TRANSFORM) hasTransformPendingTransaction = false
        }

        override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
            // TODO: 补充更多判断条件
            // 在Destroyed才resume是为了避免f的过渡动画卡顿
            if (f.tag === TAG_TRANSFORM) setContentFragmentMaxLifecycle(RESUMED)
        }
    }

    private companion object {
        const val TAG_CONTENT = "com.xiaocydx.sample.paging.complex.transform.TAG_CONTENT"
        const val TAG_TRANSFORM = "com.xiaocydx.sample.paging.complex.transform.TAG_TRANSFORM"
    }
}