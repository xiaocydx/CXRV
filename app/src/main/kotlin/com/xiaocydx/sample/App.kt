package com.xiaocydx.sample

import android.app.Application
import com.xiaocydx.accompanist.systembar.SystemBar
import com.xiaocydx.accompanist.systembar.install

/**
 * @author xcc
 * @date 2023/12/23
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        SystemBar.install(this)
    }
}