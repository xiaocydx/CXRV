package com.xiaocydx.sample.viewpager2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.doOnAttach
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.flowWithLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.setRecycleAllViewsOnDetach
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.paging.onEach
import com.xiaocydx.cxrv.paging.pagingCollector
import com.xiaocydx.sample.*
import com.xiaocydx.sample.databinding.ItemFooBinding
import com.xiaocydx.sample.paging.Foo
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
    /**
     * 通过日志观察[FooListFragment]的创建和销毁，以及[ViewHolder]的创建
     */
    @Suppress("PrivatePropertyName")
    private val TAG = javaClass.simpleName
    private val sharedViewModel: FooCategoryViewModel by activityViewModels()
    private lateinit var fooAdapter: ListAdapter<Foo, *>
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
        listViewModel = sharedViewModel.getListViewModel(categoryId)
        id = listViewModel.rvId
        linear().fixedSize().divider {
            width = 10.dp
            height = 10.dp
            topEdge = true
            horizontalEdge = true
        }
        initFooAdapter()
        overScrollNever()
        withLayoutParams(matchParent, matchParent)
        doOnAttachToViewPager2()
    }.withSwipeRefresh(fooAdapter)

    /**
     * 初始化适配器
     *
     * `sharedRecycledViewPool`场景的一些注意事项：
     * 1. 除了有视图设置逻辑，还要有视图重置逻辑，逻辑对称才能避免内存泄漏问题。
     * 2. 若使用[Glide]对[ImageView]加载图片，则需要和父级关联或者做另外处理。
     */
    private fun RecyclerView.initFooAdapter() {
        fooAdapter = bindingAdapter(
            uniqueId = Foo::id,
            inflate = ItemFooBinding::inflate
        ) {
            // 对itemView设置的OnClickListener会捕获外部FooListFragment（访问了TAG），
            // 当RecyclerView从Window上分离时，itemView会被回收进sharedRecycledViewPool,
            // 这会间接导致已销毁的FooListFragment无法被GC，即出现内存泄漏问题。
            // 注释doOnItemClick()，恢复setOnClickListener()，再滚动ViewPager2，
            // 让FooListFragment被销毁，等待一段时间后，就能看到LeakCanary的内存泄漏提示。
            onCreateView {
                Log.e(TAG, "onCreateView：${System.currentTimeMillis()}")
                // root.setOnClickListener {
                //     val context = holder.itemView.context
                //     context.showToast("setOnClickListener ${TAG}-${holder.item.name}")
                // }
            }

            // doOnItemClick()会在合适的时机会清除状态，
            // 避免sharedRecycledViewPool场景出现内存泄漏问题。
            // 或者不使用doOnItemClick()，而是在onBindView()时设置OnClickListener，
            // 在onViewRecycled()时置空OnClickListener，确保逻辑对称，避免内存泄漏问题。
            doOnItemClick { holder, item ->
                val context = holder.itemView.context
                context.showToast("doOnItemClick ${TAG}-${item.name}")
            }

            // 当前FooListFragment会复用其它FooListFragment回收进sharedRecycledViewPool的视图，
            // 由于Glide源码对被复用的视图再次加载图片，未清除上一个RequestManager记录的Target，
            // 当上一个RequestManager销毁时，通过未清除的Target，对已被复用的视图设置占位图。
            // 将下面的代码修改为：val manager = Glide.with(this@FooListFragment)，
            // 将ViewPager2从1滚动到6，再滚动回2，就能观察到已被复用的视图设置为占位图的现象。
            val manager = when (parentFragment) {
                null -> Glide.with(requireActivity())
                else -> Glide.with(requireParentFragment())
            }

            // 解决方案1：将RequestManager跟FooListFragment的父级关联，规避未清除Target造成的影响。
            // 解决方案2：通过反射替换GlideContext的imageViewTargetFactory，修改Target实现，
            // 或者不通过反射，而是调用into(Target)，目的都是为了重写Target的equals()、hashCode()，
            // 让Glide源码能够及时清除上一个RequestManager记录的Target。
            // https://github.com/bumptech/glide/issues/4598，
            // Glide官方认为重写Target的equals()、hashCode()是一种可行的方案。

            // 此处示例代码选择是的解决方案1，因为它足够简单，并且从Glide源码角度来看，
            // 解决方案2在调用into(ImageView)时，仍然会有遍历全局RequestManager的性能损耗。
            onBindView { item ->
                manager.load(item.url)
                    .placeholder(R.color.placeholder_color)
                    .centerCrop().into(imageView)
                textView.text = item.name
            }
        }

        // 连接LoadHeaderAdapter、fooAdapter、LoadFooterAdapter，
        // 将连接后的ConcatAdapter设置给RecyclerView，完成分页初始化。
        paging(fooAdapter)
    }

    /**
     * 在[RecyclerView.isAttachedToWindow] = `true`之后，向上查找[ViewPager2]父级：
     * 1. 处理[ViewPager2]嵌套[RecyclerView]的滚动冲突。
     * 2. 对[RecyclerView]设置[ViewPager2.sharedRecycledViewPool]。
     * 3. 当[RecyclerView]从Window上分离时，保存[LayoutManager]的状态，
     * 将子View和离屏缓存回收进[ViewPager2.sharedRecycledViewPool]。
     */
    private fun RecyclerView.doOnAttachToViewPager2() = doOnAttach {
        val vp2 = findParentViewPager2() ?: return@doOnAttach
        isVp2NestedScrollable = true
        setRecycledViewPool(vp2.sharedRecycledViewPool)
        setRecycleAllViewsOnDetach { _, _, initialState ->
            // 回收进sharedRecycledViewPool的上限，是当前子View数量的2倍，
            // 这是一种简易策略，意图是最多回收2页满数量的View，供重建复用。
            2 * initialState.childCount
        }
    }

    /**
     * 1. 若[FooListViewModel.isLoaded] = `false`，则当[viewLifecycle]状态为[RESUMED]时，
     * 才收集[FooListViewModel.flow]，开始列表分页加载，达到首次懒加载的目的。
     * 2. 否则当[viewLifecycle]状态为[STARTED]时，收集[FooListViewModel.flow]，
     * 确保[ViewPager2]的滚动过程能及时对[RecyclerView]添加`itemView`。
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