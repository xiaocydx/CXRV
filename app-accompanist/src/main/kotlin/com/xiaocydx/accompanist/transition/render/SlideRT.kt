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

package com.xiaocydx.accompanist.transition.render

import android.animation.Animator
import android.view.View
import android.view.ViewGroup
import androidx.transition.Slide
import androidx.transition.TransitionValues
import com.xiaocydx.accompanist.transition.render.AnimatorRT.Property

/**
 * 在RenderThread上运行的[Slide]过渡动画
 *
 * @author xcc
 * @date 2024/12/29
 */
class SlideRT(slideEdge: Int) : Slide(slideEdge) {

    override fun onAppear(
        sceneRoot: ViewGroup,
        view: View,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator? {
        if (endValues == null) return null
        val slideCalculator = mSlideCalculator.get(this)
        val endY = view.translationY
        val endX = view.translationX
        val startX = getGoneX.invoke(slideCalculator, sceneRoot, view) as Float
        val startY = getGoneY.invoke(slideCalculator, sceneRoot, view) as Float
        val animatorRT = when {
            startX == endX && startY == endY -> null
            startX != endX -> {
                view.translationX = startX
                AnimatorRT.create(view, Property.TRANSLATION_X, endX)
            }
            else -> {
                view.translationY = startY
                AnimatorRT.create(view, Property.TRANSLATION_Y, endX)
            }
        }
        return animatorRT?.toSafeAnimator()
    }

    override fun onDisappear(
        sceneRoot: ViewGroup,
        view: View,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator? {
        // FIXME: disappear改用AnimatorRT，没有运行动画，不符合预期
        return super.onDisappear(sceneRoot, view, startValues, endValues)
    }

    companion object {
        private val calculateSlideClazz = Class.forName("androidx.transition.Slide\$CalculateSlide")

        private val mSlideCalculator = Slide::class.java.getDeclaredField("mSlideCalculator")
            .apply { isAccessible = true }

        private val getGoneX = calculateSlideClazz.getDeclaredMethod(
            "getGoneX", ViewGroup::class.java, View::class.java
        ).apply { isAccessible = true }

        private val getGoneY = calculateSlideClazz.getDeclaredMethod(
            "getGoneY", ViewGroup::class.java, View::class.java
        ).apply { isAccessible = true }
    }
}