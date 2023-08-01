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
        if (startValues?.view == null) return null
        var finalEndValues = endValues
        if (finalEndValues == null) {
            val view = (if (fragment.isAdded) fragment.view else lazyView()) ?: return null
            finalEndValues = TransitionValues(view)
            transform.captureEndValues(finalEndValues)
        }
        transform.dependsOn(this, sceneRoot)
        return transform.createAnimator(sceneRoot, startValues, finalEndValues)
    }
}