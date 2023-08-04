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
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.transition.Transition
import com.google.android.material.transition.MaterialContainerTransform
import com.xiaocydx.sample.R
import com.xiaocydx.sample.layoutParams
import com.xiaocydx.sample.matchParent
import java.lang.ref.WeakReference
import kotlin.reflect.KClass

/**
 * @author xcc
 * @date 2023/8/1
 */
@SuppressLint("ViewConstructor")
internal class TransformRootView(
    context: Context,
    private val fragmentManager: FragmentManager,
) : FrameLayout(context) {
    private var hasPendingTransaction = false
    private var senderViewRef: WeakReference<View>? = null
    private var callbacks = TransformFragmentLifecycleCallbacks()

    init {
        layoutParams(matchParent, matchParent)
        id = R.id.transform_root_view_fragment_container
    }

    fun setSenderView(view: View?) {
        if (senderViewRef?.get() === view) return
        senderViewRef = WeakReference(view)
    }

    fun createTransition(fragment: Fragment, transform: MaterialContainerTransform): Transition {
        return TransformTransition(fragment, lazyView = { senderViewRef?.get() }, transform)
    }

    fun showTransformFragment(
        fragmentClass: KClass<out Fragment>,
        args: Bundle? = null,
        allowStateLoss: Boolean = false
    ): Boolean {
        if (hasPendingTransaction) return false
        if (fragmentManager.findFragmentByTag(FRAGMENT_TAG) != null) return false
        hasPendingTransaction = true
        val transaction = fragmentManager.beginTransaction()
            .addToBackStack(null)
            .add(id, fragmentClass.java, args, FRAGMENT_TAG)
        if (allowStateLoss) {
            transaction.commitAllowingStateLoss()
        } else {
            transaction.commit()
        }
        return false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        fragmentManager.registerFragmentLifecycleCallbacks(callbacks, false)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        fragmentManager.unregisterFragmentLifecycleCallbacks(callbacks)
    }

    private inner class TransformFragmentLifecycleCallbacks : FragmentLifecycleCallbacks() {

        override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
            if (f.tag == FRAGMENT_TAG) hasPendingTransaction = false
        }

        override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) = Unit
    }

    private companion object {
        const val FRAGMENT_TAG = "com.xiaocydx.sample.paging.complex.transform.FRAGMENT_TAG"
    }
}