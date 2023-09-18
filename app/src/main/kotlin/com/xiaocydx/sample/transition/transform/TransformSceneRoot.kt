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

package com.xiaocydx.sample.transition.transform

import android.content.Context
import android.os.Bundle
import android.transition.Transition
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnAttach
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.Lifecycle.State.STARTED
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.xiaocydx.sample.R
import com.xiaocydx.sample.layoutParams
import com.xiaocydx.sample.matchParent
import kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.lang.ref.WeakReference
import kotlin.reflect.KClass

/**
 * 变换过渡动画的SceneRoot
 *
 * @author xcc
 * @date 2023/8/1
 */
internal class TransformSceneRoot(context: Context, private val fragmentManager: FragmentManager) {
    private var hasContentPendingTransaction = false
    private var hasTransformPendingTransaction = false
    private var transformViewRef: WeakReference<View>? = null
    private val fragmentContainer = FragmentContainerView(context)
    private val contentFragment: Fragment?
        get() = fragmentManager.findFragmentByTag(TAG_CONTENT)
    private val transformFragment: Fragment?
        get() = fragmentManager.findFragmentByTag(TAG_TRANSFORM)
    private val _transformReturn = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1, onBufferOverflow = DROP_OLDEST
    )
    val transformReturn = _transformReturn.asSharedFlow()

    init {
        fragmentContainer.apply {
            layoutParams(matchParent, matchParent)
            id = R.id.transform_scene_root_fragment_container
            transformSceneRoot = this@TransformSceneRoot
            addOnAttachStateChangeListener(TransformFragmentLifecycleCallbacks())
        }
    }

    fun toContentView() = fragmentContainer

    fun installOnAttach(
        fragmentClass: KClass<out Fragment>,
        primaryNavigationFragment: Fragment? = null
    ): Boolean {
        if (hasContentPendingTransaction || contentFragment != null) return false
        hasContentPendingTransaction = true
        fragmentContainer.doOnAttach { container ->
            require(contentFragment == null)
            hasContentPendingTransaction = false
            if (primaryNavigationFragment != null) {
                require(primaryNavigationFragment.view === container)
                primaryNavigationFragment.parentFragmentManager.beginTransaction()
                    .setPrimaryNavigationFragment(primaryNavigationFragment)
                    .commitNow()
            }
            fragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .add(container.id, fragmentClass.java, null, TAG_CONTENT)
                .commitNow()
        }
        return true
    }

    fun forwardTransform(
        fragmentClass: KClass<out Fragment>,
        args: Bundle? = null,
        allowStateLoss: Boolean = false
    ): Boolean {
        if (hasTransformPendingTransaction || transformFragment != null) return false
        hasTransformPendingTransaction = true
        // transformFragment的生命周期状态，在过渡动画结束后才转换为RESUMED，
        // 此时就将contentFragment的生命周期状态回退至STARTED，是为了确保过渡动画流畅。
        setContentFragmentMaxLifecycle(STARTED)
        val transaction = fragmentManager
            .beginTransaction()
            .setReorderingAllowed(true)
            .addToBackStack(null)
            .add(fragmentContainer.id, fragmentClass.java, args, TAG_TRANSFORM)
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
        val fragmentRef = WeakReference(fragment)
        return TransformTransition(fragmentContainer.id, transform) { start ->
            val f = fragmentRef.get()
            val targetView = when {
                f == null -> null
                f.isAdded -> if (start) transformViewRef?.get() else f.view
                else -> if (start) f.view else transformViewRef?.get()
            }
            if (start && targetView === f?.view) {
                _transformReturn.tryEmit(Unit)
            }
            targetView
        }
    }

    private fun setContentFragmentMaxLifecycle(state: State) {
        val contentFragment = contentFragment ?: return
        fragmentManager.beginTransaction()
            .setMaxLifecycle(contentFragment, state)
            .commitNow()
    }

    private inner class TransformFragmentLifecycleCallbacks :
            FragmentLifecycleCallbacks(), View.OnAttachStateChangeListener {

        override fun onViewAttachedToWindow(v: View) {
            fragmentManager.registerFragmentLifecycleCallbacks(this, false)
        }

        override fun onViewDetachedFromWindow(v: View) {
            fragmentManager.unregisterFragmentLifecycleCallbacks(this)
        }

        override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
            if (f.tag === TAG_TRANSFORM) hasTransformPendingTransaction = false
        }

        override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
            // transformFragment的生命周期状态，在过渡动画结束后才转换为DESTROYED，
            // 此时才将contentFragment的生命周期状态恢复至RESUMED，是为了确保过渡动画流畅。
            if (f.tag === TAG_TRANSFORM) setContentFragmentMaxLifecycle(RESUMED)
        }
    }

    private companion object {
        const val TAG_CONTENT = "com.xiaocydx.sample.paging.complex.transform.TAG_CONTENT"
        const val TAG_TRANSFORM = "com.xiaocydx.sample.paging.complex.transform.TAG_TRANSFORM"
    }
}

internal fun Fragment.findTransformSceneRoot(): TransformSceneRoot? {
    var parent = parentFragment
    while (parent != null && parent.view?.transformSceneRoot == null) {
        parent = parent.parentFragment
    }
    val root = parent?.view?.transformSceneRoot
    return root ?: activity?.contentParent?.findTransformSceneRoot()
}

private val FragmentActivity.contentParent: ViewGroup
    get() = findViewById(android.R.id.content)

private var View.transformSceneRoot: TransformSceneRoot?
    get() = getTag(R.id.transform_scene_root) as? TransformSceneRoot
    set(value) {
        setTag(R.id.transform_scene_root, value)
    }

private fun View.findTransformSceneRoot(): TransformSceneRoot? = when {
    transformSceneRoot != null -> transformSceneRoot
    this is ViewGroup -> {
        var root: TransformSceneRoot? = null
        val childCount = childCount
        for (i in 0 until childCount) {
            root = getChildAt(i).findTransformSceneRoot()
            if (root != null) break
        }
        root
    }
    else -> null
}