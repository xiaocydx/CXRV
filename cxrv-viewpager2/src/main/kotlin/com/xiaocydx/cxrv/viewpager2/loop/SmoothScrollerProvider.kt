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

package com.xiaocydx.cxrv.viewpager2.loop

import android.content.Context
import android.view.View
import android.view.animation.Interpolator
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import kotlin.math.sqrt

/**
 * [SmoothScroller]的提供者，可用于修改平滑滚动的时长和插值器
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

/**
 * [LinearSmoothScroller]的提供者
 *
 * @param durationMs   平滑滚动的时长
 * @param interpolator 平滑滚动的插值器，`null`表示使用默认的插值器
 */
@Suppress("FunctionName")
fun LinearSmoothScrollerProvider(
    durationMs: Long,
    interpolator: Interpolator? = null
) = SmoothScrollerProvider { context ->
    object : LinearSmoothScroller(context) {
        override fun onTargetFound(targetView: View, state: RecyclerView.State, action: Action) {
            val dx = calculateDxToMakeVisible(targetView, horizontalSnapPreference)
            val dy = calculateDyToMakeVisible(targetView, verticalSnapPreference)
            val distance = sqrt((dx * dx + dy * dy).toDouble()).toInt()
            var time = calculateTimeForDeceleration(distance)
            time = durationMs.toInt().coerceAtLeast(time)
            val finalInterpolator = interpolator ?: mDecelerateInterpolator
            if (time > 0) action.update(-dx, -dy, time, finalInterpolator)
        }
    }
}