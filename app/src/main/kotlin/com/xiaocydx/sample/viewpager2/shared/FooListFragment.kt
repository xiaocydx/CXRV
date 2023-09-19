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
import com.xiaocydx.cxrv.animatable.controlledByParentViewPager2
import com.xiaocydx.cxrv.animatable.controlledByScroll
import com.xiaocydx.cxrv.animatable.registerImageView
import com.xiaocydx.cxrv.animatable.setAnimatableMediator
import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.autoDispose
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.paging.onEach
import com.xiaocydx.cxrv.paging.pagingCollector
import com.xiaocydx.cxrv.viewpager2.nested.isVp2NestedScrollable
import com.xiaocydx.sample.doOnTargetState
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.foo.Foo
import com.xiaocydx.sample.foo.FooListViewModel
import com.xiaocydx.sample.launchRepeatOnLifecycle
import com.xiaocydx.sample.layoutParams
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.overScrollNever
import com.xiaocydx.sample.paging.config.withPaging
import com.xiaocydx.sample.paging.config.withSwipeRefresh
import com.xiaocydx.sample.viewLifecycle

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
        initFooAdapter()
        initParentViewPager2()
    }.withSwipeRefresh(fooAdapter)

    /**
     * [FooListAdapterFixed]中讲述了`sharedRecycledViewPool`场景的一些注意事项：
     * 1. 除了有视图设置逻辑，还要有视图重置逻辑，逻辑对称才能避免内存泄漏问题。
     * 2. 若使用[Glide]对[ImageView]加载图片，则需要和父级关联或者做另外处理。
     */
    private fun RecyclerView.initFooAdapter() {
        // FooListAdapterError列举了问题点
        // fooAdapter = FooListAdapterError(this@FooListFragment)

        // FooListAdapterFixed修复FooListAdapterError列举的问题
        fooAdapter = FooListAdapterFixed(this@FooListFragment, categoryId)

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
        }.autoDispose(viewLifecycle)

        // 4. 设置动图受RecyclerView滚动、父级ViewPager2控制
        setAnimatableMediator {
            controlledByScroll()
            controlledByParentViewPager2()
            // 若跳转至透明主题的Activity，则可以启用该函数，动图受RESUMED状态控制
            // controlledByLifecycle(viewLifecycle, RESUMED)
            registerImageView(fooAdapter, visiableRatio = 0.5f) { view.imageView }
        }
    }

    /**
     * 1. 若[FooListViewModel.isLoaded] = `false`，则当[viewLifecycle]状态为[RESUMED]时，
     * 才收集[FooListViewModel.flow]，开始列表分页加载，达到首次懒加载的目的。
     * 2. 否则当[viewLifecycle]状态为[STARTED]时，收集[FooListViewModel.flow]，
     * 确保[ViewPager2]的滚动过程能及时对[RecyclerView]添加`itemView`。
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycle.doOnTargetState(if (fooViewModel.isLoaded) STARTED else RESUMED) {
            fooViewModel.flow
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