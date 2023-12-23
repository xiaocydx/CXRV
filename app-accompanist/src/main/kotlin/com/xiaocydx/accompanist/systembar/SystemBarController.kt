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

@file:SuppressLint("SupportAnnotationUsage")

package com.xiaocydx.accompanist.systembar

import android.annotation.SuppressLint
import androidx.annotation.ColorInt
import com.xiaocydx.accompanist.systembar.EdgeToEdge.Disabled
import com.xiaocydx.accompanist.systembar.EdgeToEdge.Enabled
import com.xiaocydx.accompanist.systembar.EdgeToEdge.Gesture

/**
 * SystemBar控制器
 *
 * @author xcc
 * @date 2023/12/21
 */
interface SystemBarController {

    /**
     * 状态栏背景色，默认为`window.statusBarColor`的初始值
     */
    @get:ColorInt
    @set:ColorInt
    var statusBarColor: Int

    /**
     * 导航栏背景色，默认为`window.navigationBarColor`的初始值
     */
    @get:ColorInt
    @set:ColorInt
    var navigationBarColor: Int

    /**
     * 设为[Enabled]可去除状态栏间距和[statusBarColor]，默认为[Disabled]，
     * [Gesture]等于[Enabled]，[Gesture]仅对[navigationBarEdgeToEdge]有区分。
     */
    var statusBarEdgeToEdge: EdgeToEdge

    /**
     * 设为[Enabled]可去除导航栏间距和[navigationBarColor]，默认为[Disabled]，
     * [Gesture]是当前为手势导航栏时，才去除导航栏间距和[navigationBarColor]。
     */
    var navigationBarEdgeToEdge: EdgeToEdge

    /**
     * 当状态栏的背景为浅色时，可以将该属性设为true，以便于清楚看到状态栏的图标
     *
     * 对应`WindowInsetsControllerCompat.isAppearanceLightStatusBars`，默认为false
     */
    var isAppearanceLightStatusBar: Boolean

    /**
     * 当导航栏的背景为浅色时，可以将该属性设为true，以便于清楚看到导航栏的图标
     *
     * 对应`WindowInsetsControllerCompat.isAppearanceLightNavigationBars`，默认为false
     */
    var isAppearanceLightNavigationBar: Boolean

    companion object
}

sealed class EdgeToEdge {
    object Disabled : EdgeToEdge()
    object Enabled : EdgeToEdge()
    object Gesture : EdgeToEdge()
}

internal val SystemBarController.Companion.name: String
    get() = SystemBarController::class.java.simpleName

internal fun SystemBarController.applyPendingSystemBarConfig() {
    statusBarColor = statusBarColor
    navigationBarColor = navigationBarColor
    statusBarEdgeToEdge = statusBarEdgeToEdge
    navigationBarEdgeToEdge = navigationBarEdgeToEdge
    isAppearanceLightStatusBar = isAppearanceLightStatusBar
    isAppearanceLightNavigationBar = isAppearanceLightNavigationBar
}