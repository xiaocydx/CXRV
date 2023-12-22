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

package com.xiaocydx.sample.systembar

import android.view.Window
import com.xiaocydx.sample.R

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

internal fun Window.recordSystemBarColor() {
    // 记录StatusBar和NavigationBar的初始背景色，
    // 执行完decorView创建流程，才能获取到背景色。
    initialStatusBarColor
    initialNavigationBarColor
}