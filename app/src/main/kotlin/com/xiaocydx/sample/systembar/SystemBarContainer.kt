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

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.navigationBars
import androidx.core.view.WindowInsetsCompat.Type.statusBars
import androidx.core.view.doOnAttach
import androidx.core.view.updatePadding
import com.xiaocydx.sample.isGestureNavigationBar
import com.xiaocydx.sample.matchParent

/**
 * @author xcc
 * @date 2023/12/21
 */
internal class SystemBarContainer(context: Context) : FrameLayout(context) {
    private val statusBarDrawable = ColorDrawable()
    private val navigationBarDrawable = ColorDrawable()
    private var contentView: View? = null

    var statusBarEdgeToEdge: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            ViewCompat.requestApplyInsets(this)
        }

    var gestureNavBarEdgeToEdge: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            ViewCompat.requestApplyInsets(this)
        }

    var statusBarColor: Int
        get() = statusBarDrawable.color
        set(value) {
            if (statusBarDrawable.color == value) return
            statusBarDrawable.color = value
            invalidate()
        }

    var navigationBarColor: Int
        get() = navigationBarDrawable.color
        set(value) {
            if (navigationBarDrawable.color == value) return
            navigationBarDrawable.color = value
            invalidate()
        }

    fun attach(view: View) = apply {
        setWillNotDraw(false)
        removeAllViews()
        contentView = view
        addView(contentView, matchParent, matchParent)
        doOnAttach(ViewCompat::requestApplyInsets)
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val applyInsets = WindowInsetsCompat.toWindowInsetsCompat(insets, this)
        val statusBars = applyInsets.getInsets(statusBars())
        val navigationBars = applyInsets.getInsets(navigationBars())
        updatePadding(
            top = if (statusBarEdgeToEdge) 0 else statusBars.top,
            bottom = when {
                !gestureNavBarEdgeToEdge -> navigationBars.bottom
                else -> if (applyInsets.isGestureNavigationBar(resources)) 0 else navigationBars.bottom
            }
        )
        return super.onApplyWindowInsets(insets)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        checkContentView()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        checkContentView()
        super.onLayout(changed, left, top, right, bottom)
    }

    override fun draw(canvas: Canvas) {
        checkContentView()
        super.draw(canvas)
        statusBarDrawable.setBounds(0, 0, width, paddingTop)
        navigationBarDrawable.setBounds(0, height - paddingBottom, width, height)
        statusBarDrawable.takeIf { it.bounds.height() > 0 }?.draw(canvas)
        navigationBarDrawable.takeIf { it.bounds.height() > 0 }?.draw(canvas)
    }

    private fun checkContentView() {
        check(childCount <= 1)
        if (childCount == 1) check(getChildAt(0) === contentView)
    }

    companion object {
        const val InitialColor = 0x00FFFFFF
    }
}