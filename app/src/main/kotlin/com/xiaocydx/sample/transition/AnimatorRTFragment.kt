package com.xiaocydx.sample.transition

import android.os.Bundle
import android.view.Gravity
import android.view.View
import com.xiaocydx.accompanist.transition.render.SlideRT

/**
 * 在RenderThread上运行Fragment过渡动画，解决[JankFragment]的卡顿问题
 *
 * @author xcc
 * @date 2025/3/3
 */
class AnimatorRTFragment : JankFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // RT后缀：在RenderThread上运行的Slide过渡动画
        setEnterTransition(view, SlideRT(Gravity.RIGHT))
    }
}