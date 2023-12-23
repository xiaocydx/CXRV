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
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.fragment.app.FragmentSystemBarController
import androidx.lifecycle.Lifecycle.State.INITIALIZED

fun SystemBar.Companion.install(application: Application) {
    ActivitySystemBarInstaller.register(application)
}

/**
 * **注意**：调用`SystemBar.install()`完成初始化，该接口才会生效
 */
interface SystemBar {

    fun <F> F.systemBarController(
        initializer: (SystemBarController.() -> Unit)? = null
    ): SystemBarController where F : Fragment, F : SystemBar = run {
        require(activity == null && lifecycle.currentState === INITIALIZED) {
            "只能在构造阶段获取${SystemBarController::class.java.simpleName}"
        }
        FragmentSystemBarController(this, repeatThrow = true).also { initializer?.invoke(it) }
    }

    companion object
}

internal val SystemBar.Companion.name: String
    get() = SystemBar::class.java.simpleName

private object ActivitySystemBarInstaller : ActivityLifecycleCallbacks {

    fun register(application: Application) {
        application.unregisterActivityLifecycleCallbacks(this)
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity !is SystemBar) return
        val window = activity.window
        window.recordSystemBarInitialColor()
        window.disabledDecorFitsSystemWindows()
        FragmentSystemBarInstaller.register(activity)
    }

    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}

private object FragmentSystemBarInstaller : FragmentLifecycleCallbacks() {

    fun register(activity: Activity) {
        if (activity !is FragmentActivity) return
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
        FragmentSystemBarController(f, repeatThrow = false)
    }
}