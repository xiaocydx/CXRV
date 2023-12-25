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

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.WindowInsets
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.navigationBars
import androidx.core.view.WindowInsetsCompat.Type.statusBars
import androidx.core.view.updatePadding
import com.xiaocydx.accompanist.view.layoutParams
import com.xiaocydx.accompanist.view.matchParent
import com.xiaocydx.accompanist.windowinsets.isGestureNavigationBar

/**
 * [SystemBar]视图容器
 *
 * @author xcc
 * @date 2023/12/21
 */
internal class SystemBarContainer(context: Context) : FrameLayout(context) {
    private val statusBarDrawable = ColorDrawable()
    private val navigationBarDrawable = ColorDrawable()

    init {
        setWillNotDraw(false)
    }

    var statusBarColor: Int
        get() = statusBarDrawable.color
        set(value) {
            if (statusBarDrawable.color == value) return
            statusBarDrawable.color = value
            invalidateDrawable(statusBarDrawable)
        }

    var navigationBarColor: Int
        get() = navigationBarDrawable.color
        set(value) {
            if (navigationBarDrawable.color == value) return
            navigationBarDrawable.color = value
            invalidateDrawable(navigationBarDrawable)
        }

    var statusBarEdgeToEdge: EdgeToEdge = EdgeToEdge.Disabled
        set(value) {
            if (field == value) return
            field = value
            ViewCompat.requestApplyInsets(this)
        }

    var navigationBarEdgeToEdge: EdgeToEdge = EdgeToEdge.Disabled
        set(value) {
            if (field == value) return
            field = value
            ViewCompat.requestApplyInsets(this)
        }

    init {
        layoutParams(matchParent, matchParent)
    }

    override fun verifyDrawable(who: Drawable) = when (who) {
        statusBarDrawable, navigationBarDrawable -> true
        else -> super.verifyDrawable(who)
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val applyInsets = WindowInsetsCompat.toWindowInsetsCompat(insets, this)
        val statusBarHeight = applyInsets.getInsets(statusBars()).top
        val navigationBarHeight = applyInsets.getInsets(navigationBars()).bottom
        updatePadding(
            top = when (statusBarEdgeToEdge) {
                EdgeToEdge.Disabled -> statusBarHeight
                EdgeToEdge.Enabled, EdgeToEdge.Gesture -> 0
            },
            bottom = when (navigationBarEdgeToEdge) {
                EdgeToEdge.Disabled -> navigationBarHeight
                EdgeToEdge.Enabled -> 0
                EdgeToEdge.Gesture -> when {
                    applyInsets.isGestureNavigationBar(resources) -> 0
                    else -> navigationBarHeight
                }
            }
        )
        return super.onApplyWindowInsets(insets)
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        statusBarDrawable.setBounds(0, 0, width, paddingTop)
        navigationBarDrawable.setBounds(0, height - paddingBottom, width, height)
        statusBarDrawable.takeIf { it.bounds.height() > 0 }?.draw(canvas)
        navigationBarDrawable.takeIf { it.bounds.height() > 0 }?.draw(canvas)
    }

    companion object {
        const val InitialColor = 0x00FFFFFF
    }
}