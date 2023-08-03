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
import androidx.transition.Transition
import androidx.transition.TransitionValues
import androidx.transition.dependsOn
import com.google.android.material.transition.MaterialContainerTransform

/**
 * @author xcc
 * @date 2023/8/1
 */
internal class TransformTransition(
    private val fragment: Fragment,
    private val lazyView: () -> View?,
    private val transform: MaterialContainerTransform
) : Transition() {

    override fun captureStartValues(transitionValues: TransitionValues) {
        transitionValues.view = if (fragment.isAdded) lazyView() else fragment.view
        transitionValues.takeIf { it.view != null }?.let(transform::captureStartValues)
    }

    override fun captureEndValues(transitionValues: TransitionValues) {
        transitionValues.view = if (fragment.isAdded) fragment.view else lazyView()
        transitionValues.takeIf { it.view != null }?.let(transform::captureEndValues)
    }

    override fun createAnimator(
        sceneRoot: ViewGroup,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator? {
        val startView = startValues?.view
        val endView = endValues?.view ?: (if (fragment.isAdded) fragment.view else lazyView())
        if (startView == null || endView == null
                || (startView.parent == null && endView.parent == null)) {
            // startView和endView的parent都为null，无法向上递归查找，会导致创建属性动画抛出异常
            return null
        }

        var finalEndValues = endValues
        if (finalEndValues?.view == null) {
            finalEndValues = TransitionValues(endView)
            transform.captureEndValues(finalEndValues)
        }
        transform.dependsOn(this, sceneRoot)
        return transform.createAnimator(sceneRoot, startValues, finalEndValues)
    }
}