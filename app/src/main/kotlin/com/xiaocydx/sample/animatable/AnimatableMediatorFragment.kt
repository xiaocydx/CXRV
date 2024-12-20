package com.xiaocydx.sample.animatable

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.accompanist.view.dp
import com.xiaocydx.accompanist.view.layoutParams
import com.xiaocydx.accompanist.view.matchParent
import com.xiaocydx.accompanist.view.overScrollNever
import com.xiaocydx.cxrv.animatable.controlledByParentViewPager2
import com.xiaocydx.cxrv.animatable.controlledByScroll
import com.xiaocydx.cxrv.animatable.registerImageView
import com.xiaocydx.cxrv.animatable.setAnimatableMediator
import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.viewpager2.nested.isVp2NestedScrollable

/**
 * @author xcc
 * @date 2024/12/20
 */
class AnimatableMediatorFragment : Fragment() {
    private val animatableAdapter by lazy {
        val categoryId = arguments?.getLong(KEY_CATEGORY_ID) ?: 0L
        AnimatableMediatorAdapter(this, categoryId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = RecyclerView(requireContext()).apply {
        isVp2NestedScrollable = true
        layoutParams(matchParent, matchParent)
        linear().overScrollNever().adapter(animatableAdapter)
        divider(10.dp, 10.dp) { edge(Edge.top().horizontal()) }

        setAnimatableMediator {
            // 动图受RecyclerView滚动
            controlledByScroll()
            // 动图父级ViewPager2控制
            controlledByParentViewPager2()
            // 跳转至透明主题的Activity，可以启用该函数，动图受RESUMED状态控制
            // controlledByLifecycle(viewLifecycle, RESUMED)
            registerImageView(animatableAdapter, visiableRatio = 0.5f) { view.imageView }
        }
    }

    companion object {
        private const val KEY_CATEGORY_ID = "KEY_CATEGORY_ID"

        fun newInstance(categoryId: Long) = AnimatableMediatorFragment().apply {
            arguments = Bundle(1).apply { putLong(KEY_CATEGORY_ID, categoryId) }
        }
    }
}