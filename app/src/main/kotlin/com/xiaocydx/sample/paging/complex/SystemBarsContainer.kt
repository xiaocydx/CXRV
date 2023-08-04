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

package com.xiaocydx.sample.paging.complex

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.xiaocydx.sample.doOnApplyWindowInsets
import com.xiaocydx.sample.isGestureNavigationBar
import com.xiaocydx.sample.matchParent

/**
 * 简易的SystemBars容器
 *
 * @author xcc
 * @date 2023/8/4
 */
class SystemBarsContainer(context: Context) : FrameLayout(context) {
    private var statusBarHeight = 0
    private var navigationBarHeight = 0
    private var statusBarEdgeToEdge = false
    private var gestureNavBarEdgeToEdge = false
    private val statusBarDrawable = ColorDrawable()
    private val navigationBarDrawable = ColorDrawable()
    private var contentView: View? = null

    /**
     * 该函数只需要在Activity中调用
     */
    @Suppress("DEPRECATION")
    fun disableDecorFitsSystemWindows(window: Window) = apply {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        // 拦截decorView.onApplyWindowInsets()的逻辑
        window.decorView.doOnApplyWindowInsets { _, _, _ -> }
    }

    fun setStatusBarEdgeToEdge(edgeToEdge: Boolean) = apply {
        if (statusBarEdgeToEdge != edgeToEdge) {
            statusBarEdgeToEdge = edgeToEdge
            ViewCompat.requestApplyInsets(this)
        }
    }

    fun setGestureNavBarEdgeToEdge(edgeToEdge: Boolean) = apply {
        if (gestureNavBarEdgeToEdge != edgeToEdge) {
            gestureNavBarEdgeToEdge = edgeToEdge
            ViewCompat.requestApplyInsets(this)
        }
    }

    fun setWindowSystemBarsColor(window: Window) = apply {
        // 确保decorView创建流程先执行，才能拿到颜色值
        window.decorView
        setStatusBarColor(window.statusBarColor)
        setNavigationBarColor(window.navigationBarColor)
    }

    fun setStatusBarColor(@ColorInt color: Int) = apply {
        if (statusBarDrawable.color != color) {
            statusBarDrawable.color = color
            invalidate()
        }
    }

    fun setNavigationBarColor(@ColorInt color: Int) = apply {
        if (navigationBarDrawable.color != color) {
            navigationBarDrawable.color = color
            invalidate()
        }
    }

    fun attach(view: View) = apply {
        setWillNotDraw(false)
        removeAllViews()
        contentView = view
        addView(contentView, matchParent, matchParent)
        doOnApplyWindowInsets { _, insets, _ ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            updateSystemBars(
                statusBarHeight = if (statusBarEdgeToEdge) 0 else systemBars.top,
                navigationBarHeight = when {
                    !gestureNavBarEdgeToEdge -> systemBars.bottom
                    else -> if (insets.isGestureNavigationBar(resources)) 0 else systemBars.bottom
                }
            )
        }
    }

    private fun updateSystemBars(statusBarHeight: Int, navigationBarHeight: Int) {
        val statusBarChanged = this.statusBarHeight != statusBarHeight
        val navigationBarChanged = this.navigationBarHeight != navigationBarHeight
        if (statusBarChanged || navigationBarChanged) {
            contentView?.updateLayoutParams<LayoutParams> {
                topMargin = statusBarHeight
                bottomMargin = navigationBarHeight
            }
            this.statusBarHeight = statusBarHeight
            this.navigationBarHeight = navigationBarHeight
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        checkOnlyContentView()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        checkOnlyContentView()
        super.onLayout(changed, left, top, right, bottom)
    }

    override fun draw(canvas: Canvas) {
        checkOnlyContentView()
        super.draw(canvas)
        statusBarDrawable.setBounds(0, 0, width, statusBarHeight)
        navigationBarDrawable.setBounds(0, height - navigationBarHeight, width, height)
        statusBarDrawable.takeIf { it.bounds.height() > 0 }?.draw(canvas)
        navigationBarDrawable.takeIf { it.bounds.height() > 0 }?.draw(canvas)
    }

    private fun checkOnlyContentView() {
        check(childCount <= 1)
        check(getChildAt(0) === contentView)
    }
}