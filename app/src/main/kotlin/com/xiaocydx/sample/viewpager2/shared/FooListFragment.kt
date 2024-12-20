package com.xiaocydx.sample.viewpager2.shared

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.setRecycleAllViewsOnDetach
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.xiaocydx.accompanist.lifecycle.doOnTargetState
import com.xiaocydx.accompanist.lifecycle.launchRepeatOnLifecycle
import com.xiaocydx.accompanist.lifecycle.viewLifecycle
import com.xiaocydx.accompanist.paging.withPaging
import com.xiaocydx.accompanist.paging.withSwipeRefresh
import com.xiaocydx.accompanist.view.dp
import com.xiaocydx.accompanist.view.layoutParams
import com.xiaocydx.accompanist.view.matchParent
import com.xiaocydx.accompanist.view.overScrollNever
import com.xiaocydx.accompanist.viewpager2.setVp2SharedRecycledViewPoolOnAttach
import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.paging.onEach
import com.xiaocydx.cxrv.paging.pagingCollector
import com.xiaocydx.cxrv.viewpager2.nested.isVp2NestedScrollable
import com.xiaocydx.sample.common.Foo
import com.xiaocydx.sample.common.FooListViewModel

/**
 * @author xcc
 * @date 2022/2/21
 */
class FooListFragment : Fragment() {
    /**
     * 通过日志观察[FooListFragment]的创建和销毁，以及[ViewHolder]的创建
     */
    @Suppress("PropertyName", "HasPlatformType")
    val TAG = javaClass.simpleName
    private val sharedViewModel: FooCategoryViewModel by viewModels(
        ownerProducer = { parentFragment ?: requireActivity() }
    )
    private lateinit var fooAdapter: ListAdapter<Foo, FooViewHolder>
    private lateinit var fooViewModel: FooListViewModel
    private val categoryId: Long
        get() = arguments?.getLong(KEY_CATEGORY_ID) ?: 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(TAG, "onCreate: categoryId = $categoryId")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = RecyclerView(requireContext()).apply {
        fooViewModel = sharedViewModel.getFooViewModel(categoryId)
        id = fooViewModel.rvId
        layoutParams(matchParent, matchParent)
        overScrollNever().linear().fixedSize()
        divider(10.dp, 10.dp) { edge(Edge.top().horizontal()) }
        initFooAdapter(useFixed = true)
        initParentViewPager2()
    }.withSwipeRefresh(fooAdapter)

    /**
     * [FooListAdapterFixed]讲述了`sharedRecycledViewPool`场景的一些注意事项：
     * 1. 除了有视图设置逻辑，还要有视图重置逻辑，逻辑对称才能避免内存泄漏问题。
     * 2. 若使用[Glide]对[ImageView]加载图片，则需要和父级关联或者做另外处理。
     */
    private fun RecyclerView.initFooAdapter(useFixed: Boolean) {
        fooAdapter = if (!useFixed) {
            // FooListAdapterError列举了问题点
            FooListAdapterError(this@FooListFragment)
        } else {
            // FooListAdapterFixed修复FooListAdapterError列举的问题
            FooListAdapterFixed(this@FooListFragment)
        }

        // 连接LoadHeaderAdapter、fooAdapter、LoadFooterAdapter，
        // 将连接后的ConcatAdapter设置给RecyclerView，完成初始化。
        adapter = fooAdapter.withPaging()
    }

    private fun RecyclerView.initParentViewPager2() {
        // 1. 处理ViewPager2嵌套RecyclerView的滚动冲突
        isVp2NestedScrollable = true

        // 2. 对RecyclerView设置ViewPager2.sharedRecycledViewPool
        setVp2SharedRecycledViewPoolOnAttach()

        // 3. 当RecyclerView从Window分离时，保存LayoutManager的状态，
        // 将子View和离屏缓存回收进ViewPager2.sharedRecycledViewPool。
        setRecycleAllViewsOnDetach { _, _, initialState ->
            // 回收进sharedRecycledViewPool的上限，是当前子View数量的3倍，
            // 这是一种简易策略，意图是最多回收3页满数量的View，供重建复用。
            3 * initialState.childCount
        }
    }

    /**
     * 1. 若`fooViewModel.isLoaded = false`，则当[viewLifecycle]状态为[RESUMED]时，
     * 才收集[FooListViewModel.pagingFlow]，开始列表分页加载，达到首次懒加载的目的。
     *
     * 2. 否则当[viewLifecycle]状态为[STARTED]时，收集[FooListViewModel.pagingFlow]，
     * 确保[ViewPager2]的滚动过程能及时对[RecyclerView]添加`itemView`。
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycle.doOnTargetState(if (fooViewModel.isLoaded) STARTED else RESUMED) {
            fooViewModel.pagingFlow
                .onEach(fooAdapter.pagingCollector)
                .launchRepeatOnLifecycle(viewLifecycle)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG, "onDestroy: categoryId = $categoryId")
    }

    companion object {
        private const val KEY_CATEGORY_ID = "KEY_CATEGORY_ID"

        fun newInstance(categoryId: Long) = FooListFragment().apply {
            arguments = Bundle(1).apply { putLong(KEY_CATEGORY_ID, categoryId) }
        }
    }
}