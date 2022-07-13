package com.xiaocydx.sample.viewpager2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnAttach
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle.State.*
import androidx.lifecycle.flowWithLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.destroyRecycleViews
import androidx.viewpager2.widget.ViewPager2
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.paging.onEach
import com.xiaocydx.cxrv.paging.pagingCollector
import com.xiaocydx.sample.*
import com.xiaocydx.sample.paging.FooAdapter
import com.xiaocydx.sample.paging.FooListViewModel
import com.xiaocydx.sample.paging.config.paging
import com.xiaocydx.sample.paging.config.withSwipeRefresh
import com.xiaocydx.sample.viewpager2.nested.isVp2NestedScrollable
import com.xiaocydx.sample.viewpager2.shared.findParentViewPager2
import com.xiaocydx.sample.viewpager2.shared.sharedRecycledViewPool
import kotlinx.coroutines.flow.launchIn

/**
 * @author xcc
 * @date 2022/2/21
 */
class FooListFragment : Fragment() {
    @Suppress("PrivatePropertyName")
    private val TAG = javaClass.simpleName
    private val sharedViewModel: FooCategoryViewModel by activityViewModels()
    private lateinit var fooAdapter: FooAdapter
    private lateinit var listViewModel: FooListViewModel
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
        fooAdapter = FooAdapter()
        listViewModel = sharedViewModel.getListViewModel(categoryId)
        id = listViewModel.rvId
        linear().fixedSize().divider {
            height = 2.dp
            color = 0xFF9DAA8F.toInt()
        }
        paging(fooAdapter)
        overScrollNever()
        withLayoutParams(matchParent, matchParent)
        doOnAttachToViewPager2()
    }.withSwipeRefresh(fooAdapter)

    /**
     * 在[RecyclerView.isAttachedToWindow] = `true`之后，向上查找[ViewPager2]父级：
     * 1. 处理[ViewPager2]嵌套[RecyclerView]的滚动冲突。
     * 2. 对[RecyclerView]设置[ViewPager2.sharedRecycledViewPool]。
     * 3. Fragment视图销毁时，将[RecyclerView]的子View、`Scrap`、离屏缓存，
     * 回收进`sharedRecycledViewPool`。
     */
    private fun RecyclerView.doOnAttachToViewPager2() = doOnAttach {
        val vp2 = findParentViewPager2() ?: return@doOnAttach
        isVp2NestedScrollable = true
        setRecycledViewPool(vp2.sharedRecycledViewPool)
        viewLifecycle.doOnTargetState(DESTROYED) {
            // 回收进sharedRecycledViewPool的上限，是当前子View数量的2倍，
            // 这是一种简易策略，意图是最多回收2页满数量的View，供重建复用。
            val maxScrap = childCount * 2
            destroyRecycleViews { _, _ -> maxScrap }
        }
    }

    /**
     * 若[FooListViewModel.isLoaded] = `false`，则当[viewLifecycle]状态为[RESUMED]时，
     * 才收集[FooListViewModel.flow]，开始列表分页加载，达到首次懒加载的目的。
     * 否则当[viewLifecycle]状态为[STARTED]时，收集[FooListViewModel.flow]，
     * 确保[ViewPager2]滚动过程[RecyclerView]及时添加`itemView`。
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycle.doOnTargetState(
            state = if (listViewModel.isLoaded) STARTED else RESUMED
        ) {
            listViewModel.flow
                .onEach(fooAdapter.pagingCollector)
                .flowWithLifecycle(viewLifecycle)
                .launchIn(viewLifecycleScope)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG, "onDestroy: categoryId = $categoryId")
    }

    companion object {
        private const val KEY_CATEGORY_ID = "KEY_CATEGORY_ID"

        fun newInstance(categoryId: Long): FooListFragment = FooListFragment().apply {
            arguments = Bundle(1).apply { putLong(KEY_CATEGORY_ID, categoryId) }
        }
    }
}