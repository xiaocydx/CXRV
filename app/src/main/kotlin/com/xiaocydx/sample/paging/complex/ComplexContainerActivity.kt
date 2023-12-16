package com.xiaocydx.sample.paging.complex

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.SystemBarsController
import androidx.fragment.app.commit
import androidx.fragment.app.disableDecorFitsSystemWindows

/**
 * 视频流的过渡动画和分页加载示例
 *
 * @author xcc
 * @date 2023/7/30
 */
class ComplexContainerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemBarsController.disableDecorFitsSystemWindows(window)
        if (savedInstanceState != null) return
        supportFragmentManager.commit {
            add(android.R.id.content, ComplexListFragment::class.java, null)
        }
    }
}