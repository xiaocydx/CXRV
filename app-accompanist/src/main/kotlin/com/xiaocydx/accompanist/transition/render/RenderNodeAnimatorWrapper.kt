package com.xiaocydx.accompanist.transition.render

import android.animation.Animator
import android.animation.TimeInterpolator
import android.annotation.SuppressLint
import android.view.View

/**
 * @author xcc
 * @date 2024/12/29
 */
internal class RenderNodeAnimatorWrapper private constructor(private val animator: Animator) {

    var duration: Long
        get() = animator.duration
        set(value) {
            animator.duration = value
        }

    var interpolator: TimeInterpolator?
        get() = animator.interpolator
        set(value) {
            animator.interpolator = value
        }

    fun setTarget(view: View) {
        setTarget.invoke(animator, view)
    }

    fun start() {
        animator.start()
    }

    fun get() = animator

    companion object : Reflection {
        private const val TRANSLATION_X = 0
        private const val TRANSLATION_Y = 1

        @SuppressLint("PrivateApi")
        private val clazz = Class.forName("android.view.RenderNodeAnimator")
        private val property_finalValue = clazz.declaredConstructor(
            Int::class.javaPrimitiveType!!,
            Float::class.javaPrimitiveType!!
        ).toCache()
        private val setTarget = clazz.declaredMethod(
            name = "setTarget", View::class.java
        ).toCache()

        fun createTranslationY(finalValue: Float): RenderNodeAnimatorWrapper {
            val animator = property_finalValue.newInstance(TRANSLATION_Y, finalValue)
            return RenderNodeAnimatorWrapper(animator as Animator)
        }

        fun createTranslationX(finalValue: Float): RenderNodeAnimatorWrapper {
            val animator = property_finalValue.newInstance(TRANSLATION_X, finalValue)
            return RenderNodeAnimatorWrapper(animator as Animator)
        }
    }
}