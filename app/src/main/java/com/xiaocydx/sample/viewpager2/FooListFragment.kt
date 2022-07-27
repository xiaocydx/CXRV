package com.xiaocydx.sample.viewpager2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.doOnAttach
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.setRecycleAllViewsOnDetach
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.xiaocydx.cxrv.binding.BindingHolder
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.autoDispose
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.paging.onEach
import com.xiaocydx.cxrv.paging.pagingCollector
import com.xiaocydx.sample.*
import com.xiaocydx.sample.databinding.ItemFooBinding
import com.xiaocydx.sample.paging.Foo
import com.xiaocydx.sample.paging.FooListViewModel
import com.xiaocydx.sample.paging.config.withPaging
import com.xiaocydx.sample.paging.config.withSwipeRefresh
import com.xiaocydx.sample.viewpager2.nested.isVp2NestedScrollable
import com.xiaocydx.sample.viewpager2.shared.findParentViewPager2
import com.xiaocydx.sample.viewpager2.shared.sharedRecycledViewPool

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
    private val sharedViewModel: FooCategoryViewModel by viewModels(
        ownerProducer = { parentFragment ?: requireActivity() }
    )
    private lateinit var listViewModel: FooListViewModel
    private lateinit var fooAdapter: ListAdapter<Foo, BindingHolder<ItemFooBinding>>
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
        listViewModel = sharedViewModel
            .getListViewModel(categoryId)
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

            // 当前FooListFragment会复用其它FooListFragment回收进sharedRecycledViewPool的ImageView，
            // Glide对被共享复用的ImageView再次加载图片时，未及时移除上一个RequestManager记录的Target，
            // 当上一个RequestManager.onDestroy()被调用时，clear(Target)对已被共享的ImageView设置占位图。

            // 解决方案1：
            // 将RequestManager跟FooListFragment的父级关联，规避未及时移除Target造成的问题。
            // 因为此处示例代码比较简单，没有加载GIF、WEBP动图，所以选择解决方案1。
            // 注意：跟父级关联后，FooListFragment销毁时不会取消请求，而是在onBindView()中，
            // 对被共享复用的ImageView再次加载图片时，若请求不一致，则取消上一次的请求，
            // 若业务场景不能接受这一点，则可以选择解决方案2。
            // val requestManager = Glide.with(this@FooListFragment)
            val requestManager = when (parentFragment) {
                null -> Glide.with(requireActivity())
                else -> Glide.with(requireParentFragment())
            }
            // 将上面的代码修改为：val requestManager = Glide.with(this@FooListFragment)，
            // 将ViewPager2从1滚动到6，再滚动回2，能观察到已被共享复用的ImageView设置占位图的现象。

            onBindView { item ->
                textView.text = item.name
                // 将into(imageView)替换为intoIsolate(imageView, categoryId)，
                // 能解决调用GifDrawable.stop()、WebpDrawable.stop()停止动图，
                // 出现相同动图url的ImageView内容绘制混乱的问题。
                // intoIsolate(imageView, categoryId)做的事：
                // 1.对缓存键混入附带categoryId的signature。
                // 2.继承ImageViewTarget，重写equals()和hashCode()。
                // 因为对缓存键混入了signature，所以对被共享复用的ImageView再次加载图片时，
                // 即使url跟之前的一致，也不会看作是同一请求, 这在一定程度上降低了资源重用率。
                // 注意：由于对缓存键混入了signature，并且重写了ImageViewTarget的equals()和hashCode(),
                // 因此intoIsolate(imageView, categoryId)也能解决上面提到的未及时清除Target造成的问题，
                // 可作为解决方案2。
                requestManager
                    .load(item.url).centerCrop()
                    .placeholder(R.color.placeholder_color)
                    .into(imageView)
                    // .intoIsolate(imageView, categoryId)
            }
        }

        // 连接LoadHeaderAdapter、fooAdapter、LoadFooterAdapter，
        // 将连接后的ConcatAdapter设置给RecyclerView，完成初始化。
        adapter = fooAdapter.withPaging()
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
        }.autoDispose(viewLifecycle)
        // Activity直接退出的流程，即ActivityThread.handleDestroyActivity()，
        // 是先更改Lifecycle的状态，再执行视图树的dispatchDetachedFromWindow()。
        // autoDispose(viewLifecycle)在DESTROYED状态时自动废弃，
        // 避免Activity直接退出时，执行冗余的视图回收处理。
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
                .repeatOnLifecycle(viewLifecycle)
                .launchInLifecycleScope()
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