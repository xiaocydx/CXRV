package com.xiaocydx.accompanist.transition.render

import android.animation.Animator
import android.view.View
import android.view.ViewGroup
import androidx.transition.Slide
import androidx.transition.TransitionValues

/**
 * @author xcc
 * @date 2024/12/29
 */
class RenderNodeSlide(slideEdge: Int) : Slide(slideEdge) {

    override fun onAppear(
        sceneRoot: ViewGroup,
        view: View?,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator? {
        if (endValues == null) return null
        val position = endValues.values[PROPNAME_SCREEN_POSITION] as IntArray?
        val endX = view!!.translationX
        val startX = view.translationX + sceneRoot.width
        view.translationX = startX
        val wrapper = RenderNodeAnimatorWrapper.createTranslationX(endX)
        wrapper.setTarget(view)
        // FIXME: 2024/12/29
        //  1. 不支持pause()和resume()
        //  2. view的内部元素不能正常绘制？
        return wrapper.get()
    }

    private companion object {
        const val PROPNAME_SCREEN_POSITION: String = "android:slide:screenPosition"
    }
}