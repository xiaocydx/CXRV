package com.xiaocydx.cxrv.viewpager2.loop

import android.content.Context
import androidx.recyclerview.widget.RecyclerView.SmoothScroller

/**
 * [SmoothScroller]的提供者
 *
 * @author xcc
 * @date 2023/5/23
 */
fun interface SmoothScrollerProvider {

    /**
     * 以修改平滑滚动的时长为例：
     * ```
     * class SmoothScrollerImpl(
     *     context: Context,
     *     private val durationMs: Int
     * ) : LinearSmoothScroller(context) {
     *
     *     override fun onTargetFound(targetView: View, state: State, action: Action) {
     *         val dx = calculateDxToMakeVisible(targetView, horizontalSnapPreference)
     *         val dy = calculateDyToMakeVisible(targetView, verticalSnapPreference)
     *         // 将平滑滚动的时长修改为durationMs
     *         action.update(-dx, -dy, durationMs, mDecelerateInterpolator)
     *     }
     * }
     * ```
     */
    fun create(context: Context): SmoothScroller
}