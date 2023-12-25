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

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.fragment.app.ActivitySystemBarController
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.fragment.app.FragmentSystemBarController
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.Lifecycle.State.INITIALIZED
import androidx.lifecycle.Lifecycle.State.RESUMED

/**
 * 对[FragmentActivity]和[Fragment]注入[SystemBar]的实现
 *
 * ```
 * class App : Application() {
 *
 *     override fun onCreate() {
 *         super.onCreate()
 *         SystemBar.install(this)
 *     }
 * }
 * ```
 */
fun SystemBar.Companion.install(application: Application) {
    ActivitySystemBarInstaller.register(application)
}

/**
 * ### [FragmentActivity]实现[SystemBar]
 * 1.应用默认配置
 * ```
 * class MainActivity : AppCompatActivity(), SystemBar
 * ```
 *
 * 2.构造声明配置
 * ```
 * class MainActivity : AppCompatActivity(), SystemBar {
 *     init {
 *         systemBarController {...}
 *     }
 * }
 * ```
 *
 * 3.后续修改配置
 * ```
 * class MainActivity : AppCompatActivity(), SystemBar {
 *      // 保留controller，后续修改配置
 *      private val controller = systemBarController {...}
 * }
 * ```
 *
 * ### [Fragment]实现[SystemBar]
 * [Fragment]实现[SystemBar]跟上述[FragmentActivity]一致，
 * 作为宿主的[FragmentActivity]，需要实现[SystemBar.Host]，
 * 宿主可以同时实现[SystemBar]和[SystemBar.Host]：
 * ```
 * class MainActivity : AppCompatActivity(), SystemBar, SystemBar.Host
 * ```
 *
 * ### 应用配置和恢复配置
 * 1. [FragmentActivity]应用配置的时机早于[Fragment]。
 * 2. 当`fragment.lifecycle`的状态转换为[RESUMED]时，应用当前的配置。
 * 3. 当`fragment.lifecycle`的状态转换为[DESTROYED]时，恢复之前的配置。
 *
 * 第3点是用于支持`fragment.lifecycle`的状态没有从[RESUMED]回退的场景，
 * 系统配置更改、进程被杀掉导致[FragmentActivity]重建，第3点也仍然满足。
 *
 * **注意**：第3点仅支持`A -> B -> C`的前进，以及`A <- B <- C`或`A <- C`（B被移除）的后退。
 */
interface SystemBar {

    interface Host

    fun <A> A.systemBarController(
        initializer: (SystemBarController.() -> Unit)? = null
    ): SystemBarController where A : FragmentActivity, A : SystemBar = run {
        require(window == null && lifecycle.currentState === INITIALIZED) {
            "只能在${javaClass.canonicalName}的构造阶段获取${SystemBarController.name}"
        }
        ActivitySystemBarController(this, repeatThrow = true).attach(initializer)
    }

    fun <F> F.systemBarController(
        initializer: (SystemBarController.() -> Unit)? = null
    ): SystemBarController where F : Fragment, F : SystemBar = run {
        require(activity == null && lifecycle.currentState === INITIALIZED) {
            "只能在${javaClass.canonicalName}的构造阶段获取${SystemBarController.name}"
        }
        FragmentSystemBarController(this, repeatThrow = true).attach(initializer)
    }

    companion object
}

internal val SystemBar.Companion.name: String
    get() = SystemBar::class.java.simpleName

internal val SystemBar.Companion.hostName: String
    get() = "${name}.${SystemBar.Host::class.java.simpleName}"

private object ActivitySystemBarInstaller : ActivityLifecycleCallbacks {

    fun register(application: Application) {
        application.unregisterActivityLifecycleCallbacks(this)
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity !is SystemBar && activity !is SystemBar.Host) return
        require(activity is FragmentActivity) {
            val activityName = activity.javaClass.canonicalName
            val componentName = FragmentActivity::class.java.canonicalName
            val systemBarName = when (activity) {
                is SystemBar -> SystemBar.name
                is SystemBar.Host -> SystemBar.hostName
                else -> throw AssertionError()
            }
            "${activityName}需要是${componentName}，才能实现${systemBarName}"
        }
        val window = activity.window
        window.recordSystemBarInitialColor()
        window.disabledDecorFitsSystemWindows()
        if (activity is SystemBar) {
            ActivitySystemBarController(activity, repeatThrow = false).attach()
        }
        if (activity is SystemBar.Host) {
            FragmentSystemBarInstaller.register(activity)
        }
    }

    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}

private object FragmentSystemBarInstaller : FragmentLifecycleCallbacks() {

    fun register(activity: FragmentActivity) {
        val fm = activity.supportFragmentManager
        fm.unregisterFragmentLifecycleCallbacks(this)
        fm.registerFragmentLifecycleCallbacks(this, true)
    }

    override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        if (f !is SystemBar) return
        require(f !is DialogFragment) {
            val fragmentName = f.javaClass.canonicalName
            "${fragmentName}为DialogFragment，${SystemBar.name}未支持DialogFragment"
        }
        FragmentSystemBarController(f, repeatThrow = false).attach()
    }
}