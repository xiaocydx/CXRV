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

import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat.Type.navigationBars
import androidx.core.view.WindowInsetsCompat.Type.statusBars
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.fragment.app.SystemBarController
import androidx.lifecycle.Lifecycle.State.INITIALIZED
import com.xiaocydx.sample.consume

/**
 * 去除`window.decorView`实现的间距和背景色，自行处理[WindowInsets]
 *
 * **注意**：该函数需要在`super.onCreate(savedInstanceState)`之前调用。
 */
fun SystemBar.Companion.init(activity: FragmentActivity) = with(activity) {
    window.recordSystemBarColor()

    // 设置decorFitsSystemWindows = false的详细解释：
    // https://www.yuque.com/u12192380/khwdgb/kqx6tak191xz1zpv
    WindowCompat.setDecorFitsSystemWindows(window, false)

    // 设置softInputMode = SOFT_INPUT_ADJUST_RESIZE的详细解释：
    // https://www.yuque.com/u12192380/khwdgb/ifiu0ptqnm080gzl
    @Suppress("DEPRECATION")
    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

    // Android 9.0以下decorView.onApplyWindowInsets()会返回新创建的WindowInsets，
    // 不会引用ViewRootImpl的成员属性mDispatchContentInsets和mDispatchStableInsets,
    // 若不返回decorView新创建的WindowInsets，则需要兼容WindowInsets可变引起的问题，
    // 详细解释：https://www.yuque.com/u12192380/khwdgb/yvtolsepi5kmz38i
    ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
        val decorInsets = insets.consume(statusBars() or navigationBars())
        ViewCompat.onApplyWindowInsets(window.decorView, decorInsets)
        insets
    }

    SystemBarControllerInstaller.registerToFragmentManager(supportFragmentManager)
}

/**
 * **注意**：需要调用`SystemBar.init()`完成初始化，该接口才会生效。
 */
interface SystemBar {

    fun <F> F.systemBarController(
        initializer: (SystemBarController.() -> Unit)? = null
    ): SystemBarController where F : Fragment, F : SystemBar {
        require(activity == null && lifecycle.currentState === INITIALIZED) {
            "只能在Fragment构造阶段调用systemBarController()获取SystemBarController"
        }
        return SystemBarController(this, repeatThrow = true).also { initializer?.invoke(it) }
    }

    companion object
}

private object SystemBarControllerInstaller : FragmentLifecycleCallbacks() {

    fun registerToFragmentManager(fm: FragmentManager) {
        fm.unregisterFragmentLifecycleCallbacks(this)
        fm.registerFragmentLifecycleCallbacks(this, true)
    }

    override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        if (f is SystemBar) SystemBarController(f, repeatThrow = false)
    }
}