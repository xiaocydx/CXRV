package com.xiaocydx.sample.paging.complex

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xiaocydx.sample.paging.complex.transform.TransformContainer

/**
 * 视频流的过渡动画和分页加载示例
 *
 * @author xcc
 * @date 2023/7/30
 */
class ComplexContainerActivity : AppCompatActivity(), TransformContainer {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        disableDecorFitsSystemWindows()
        // setContentView(ComplexListFragment::class)

        ComplexContainerFragment.commitNow(
            id = android.R.id.content,
            fragmentManager = supportFragmentManager
        )
    }
}