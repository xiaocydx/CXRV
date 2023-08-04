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

import android.animation.Animator
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.transition.dependsOn
import com.google.android.material.transition.MaterialContainerTransform
import android.transition.Transition as AndroidTransition
import android.transition.TransitionValues as AndroidTransitionValues
import androidx.transition.TransitionValues as AndroidXTransitionValues

/**
 * @author xcc
 * @date 2023/8/1
 */
internal class TransformTransition(
    private val fragment: Fragment,
    private val lazyView: () -> View?,
    private val transform: MaterialContainerTransform
) : AndroidTransition() {

    override fun captureStartValues(transitionValues: AndroidTransitionValues) {
        val view = (if (fragment.isAdded) lazyView() else fragment.view) ?: return
        val values = AndroidXTransitionValues(view)
        transform.captureStartValues(values)
        transitionValues.view = view
        transitionValues.values.putAll(values.values)
    }

    override fun captureEndValues(transitionValues: AndroidTransitionValues) {
        val view = (if (fragment.isAdded) fragment.view else lazyView()) ?: return
        val values = AndroidXTransitionValues(view)
        transform.captureEndValues(values)
        transitionValues.view = view
        transitionValues.values.putAll(values.values)
    }

    override fun createAnimator(
        sceneRoot: ViewGroup,
        startValues: AndroidTransitionValues?,
        endValues: AndroidTransitionValues?
    ): Animator? {
        val startView = startValues?.view
        val endView = endValues?.view ?: (if (fragment.isAdded) fragment.view else lazyView())
        if (startView == null || endView == null
                || (startView.parent == null && endView.parent == null)) {
            // startView和endView的parent都为null，无法向上递归查找，会导致创建属性动画抛出异常
            return null
        }

        val endXValues = if (endValues?.view == null) {
            AndroidXTransitionValues(endView).also(transform::captureEndValues)
        } else {
            endValues.toAndroidX()
        }
        transform.dependsOn(this, sceneRoot)
        return transform.createAnimator(sceneRoot, startValues.toAndroidX(), endXValues)
    }

    @Suppress("DEPRECATION")
    private fun AndroidTransitionValues.toAndroidX(): AndroidXTransitionValues {
        val transitionValues = AndroidXTransitionValues()
        transitionValues.view = view
        transitionValues.values.putAll(values)
        return transitionValues
    }
}