package com.xiaocydx.sample.paging.complex

import android.content.Context
import android.view.View
import android.view.Window
import android.widget.LinearLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.xiaocydx.sample.doOnApplyWindowInsets
import com.xiaocydx.sample.isGestureNavigationBar
import com.xiaocydx.sample.matchParent

/**
 * @author xcc
 * @date 2023/8/4
 */
class SystemBarsContainer(context: Context) : LinearLayout(context) {
    private val statusBarView = View(context)
    private val navigationBarView = View(context)
    private var contentView: View? = null

    fun init(window: Window, view: View) = apply {
        // 确保decorView创建流程先执行，才能拿到颜色值
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.decorView.doOnApplyWindowInsets { _, _, _ -> }

        removeAllViews()
        contentView = view
        orientation = VERTICAL
        addView(statusBarView, matchParent, 0)
        addView(contentView, matchParent, 0)
        addView(navigationBarView, matchParent, 0)
        contentView!!.updateLayoutParams<LayoutParams> { weight = 1f }

        statusBarView.setBackgroundColor(window.statusBarColor)
        navigationBarView.setBackgroundColor(window.navigationBarColor)
        doOnApplyWindowInsets { _, insets, _ ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val isGestureNavigationBar = insets.isGestureNavigationBar(resources)
            statusBarView.updateLayoutParams { height = systemBars.top }
            navigationBarView.updateLayoutParams {
                height = if (isGestureNavigationBar) 0 else systemBars.bottom
            }
        }
    }
}