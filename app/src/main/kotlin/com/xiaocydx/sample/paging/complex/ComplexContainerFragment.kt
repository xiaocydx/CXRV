package com.xiaocydx.sample.paging.complex

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commitNow
import com.xiaocydx.sample.transition.transform.TransformContainer

/**
 * 视频流的过渡动画和分页加载示例
 *
 * @author xcc
 * @date 2023/8/5
 */
class ComplexContainerFragment : Fragment(), TransformContainer {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = createContentView(ComplexListFragment::class)

    companion object {
        fun commitNow(id: Int, fragmentManager: FragmentManager) {
            val tag = ComplexListFragment::class.java.simpleName
            if (fragmentManager.findFragmentByTag(tag) != null) return
            fragmentManager.commitNow { add(id, ComplexContainerFragment(), tag) }
        }
    }
}