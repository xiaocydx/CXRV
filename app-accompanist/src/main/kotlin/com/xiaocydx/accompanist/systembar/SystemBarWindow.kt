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

package com.xiaocydx.accompanist.systembar

import android.view.Window
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat.Type.navigationBars
import androidx.core.view.WindowInsetsCompat.Type.statusBars
import com.xiaocydx.accompanist.R
import com.xiaocydx.accompanist.windowinsets.consume

internal val Window.initialStatusBarColor: Int
    get() {
        val key = R.id.tag_window_initial_status_bar_color
        var color = decorView.getTag(key) as? Int
        if (color == null) {
            color = statusBarColor
            decorView.setTag(key, color)
        }
        return color
    }

internal val Window.initialNavigationBarColor: Int
    get() {
        val key = R.id.tag_window_initial_navigation_bar_color
        var color = decorView.getTag(key) as? Int
        if (color == null) {
            color = navigationBarColor
            decorView.setTag(key, color)
        }
        return color
    }

internal fun Window.recordSystemBarInitialColor() {
    // 记录StatusBar和NavigationBar的初始背景色，
    // 执行完decorView创建流程，才能获取到背景色。
    initialStatusBarColor
    initialNavigationBarColor
}

internal fun Window.disabledDecorFitsSystemWindows() {
    // 设置decorFitsSystemWindows = false的详细解释：
    // https://www.yuque.com/u12192380/khwdgb/kqx6tak191xz1zpv
    WindowCompat.setDecorFitsSystemWindows(this, false)

    // 设置softInputMode = SOFT_INPUT_ADJUST_RESIZE的详细解释：
    // https://www.yuque.com/u12192380/khwdgb/ifiu0ptqnm080gzl
    @Suppress("DEPRECATION")
    setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

    // Android 9.0以下decorView.onApplyWindowInsets()会返回新创建的WindowInsets，
    // 不会引用ViewRootImpl的成员属性mDispatchContentInsets和mDispatchStableInsets,
    // 若不返回decorView新创建的WindowInsets，则需要兼容WindowInsets可变引起的问题，
    // 详细解释：https://www.yuque.com/u12192380/khwdgb/yvtolsepi5kmz38i
    ViewCompat.setOnApplyWindowInsetsListener(decorView) { _, insets ->
        val applyInsets = insets.consume(statusBars() or navigationBars())
        ViewCompat.onApplyWindowInsets(decorView, applyInsets)
        insets
    }
}