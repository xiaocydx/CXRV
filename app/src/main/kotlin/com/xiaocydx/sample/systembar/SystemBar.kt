package com.xiaocydx.sample.systembar

import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import androidx.annotation.ColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat.Type.navigationBars
import androidx.core.view.WindowInsetsCompat.Type.statusBars
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.fragment.app.SystemBarController
import com.xiaocydx.sample.consume

/**
 * 去除`window.decorView`实现的间距和背景色，自行处理[WindowInsets]
 *
 * **注意**：该函数需要在`super.onCreate(savedInstanceState)`之前调用。
 */
fun SystemBar.Companion.init(activity: FragmentActivity) = with(activity) {
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
 * **注意**：需要调用`SystemBar.init()`完成初始化，该注解才会生效。
 *
 * 对Fragment添加该注解，即可关联[SystemBarController]，例如：
 * ```
 * @SystemBar(statusBarEdgeToEdge = true)
 * class FooFragment : Fragment()
 * ```
 *
 * 若需要动态修改属性，则直接使用[SystemBarController]，例如：
 * ```
 * class FooFragment : Fragment() {
 *     val controller = SystemBarController(this)
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SystemBar(
    val consumeStatusBar: Boolean = false,
    val consumeNavigationBar: Boolean = false,
    val statusBarEdgeToEdge: Boolean = false,
    val gestureNavBarEdgeToEdge: Boolean = false,
    @ColorInt val statusBarColor: Int = InitialColor,
    @ColorInt val navigationBarColor: Int = InitialColor,
    val appearanceLightStatusBar: Boolean = false,
    val appearanceLightNavigationBar: Boolean = false,
) {
    companion object {
        const val InitialColor = 0x00FFFFFF
    }
}

private object SystemBarControllerInstaller : FragmentLifecycleCallbacks() {

    fun registerToFragmentManager(fm: FragmentManager) {
        fm.unregisterFragmentLifecycleCallbacks(this)
        fm.registerFragmentLifecycleCallbacks(this, true)
    }

    override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        f.javaClass.getAnnotation(SystemBar::class.java)?.apply {
            SystemBarController(f)
                .setConsumeStatusBar(consumeStatusBar)
                .setConsumeNavigationBar(consumeNavigationBar)
                .setStatusBarEdgeToEdge(statusBarEdgeToEdge)
                .setGestureNavBarEdgeToEdge(gestureNavBarEdgeToEdge)
                .setStatusBarColor(statusBarColor)
                .setNavigationBarColor(navigationBarColor)
                .setAppearanceLightStatusBar(appearanceLightStatusBar)
                .setAppearanceLightNavigationBar(appearanceLightNavigationBar)
        }
    }
}