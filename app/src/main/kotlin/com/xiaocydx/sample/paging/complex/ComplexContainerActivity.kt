package com.xiaocydx.sample.paging.complex

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xiaocydx.sample.transition.transform.TransformContainer

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
        // 第1种方式，将FragmentActivity作为TransformContainer的载体
        setContentView(ComplexListFragment::class)

        // 第2种方式，将Fragment作为TransformContainer的载体
        // ComplexContainerFragment.commitNow(
        //     id = android.R.id.content,
        //     fragmentManager = supportFragmentManager
        // )
    }
}