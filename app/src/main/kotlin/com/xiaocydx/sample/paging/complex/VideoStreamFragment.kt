package com.xiaocydx.sample.paging.complex

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.itemclick.doOnLongItemClick
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.ListState
import com.xiaocydx.cxrv.paging.Pager
import com.xiaocydx.cxrv.paging.broadcastIn
import com.xiaocydx.cxrv.paging.isSuccess
import com.xiaocydx.cxrv.paging.onEach
import com.xiaocydx.cxrv.paging.pagingCollector
import com.xiaocydx.cxrv.paging.storeIn
import com.xiaocydx.sample.databinding.FragmetVideoStreamBinding
import com.xiaocydx.sample.databinding.ItemVideoStreamBinding
import com.xiaocydx.sample.doOnApplyWindowInsets
import com.xiaocydx.sample.doOnStateChanged
import com.xiaocydx.sample.launchSafely
import com.xiaocydx.sample.paging.complex.transform.SystemBarsContainer
import com.xiaocydx.sample.paging.complex.transform.TransformReceiver
import com.xiaocydx.sample.paging.complex.transform.doOnEnd
import com.xiaocydx.sample.paging.complex.transform.setDarkStatusBarOnResume
import com.xiaocydx.sample.paging.complex.transform.setWindowNavigationBarColor
import com.xiaocydx.sample.paging.config.loadStatesFlow
import com.xiaocydx.sample.paging.config.replaceWithSwipeRefresh
import com.xiaocydx.sample.registerOnPageChangeCallback
import com.xiaocydx.sample.repeatOnLifecycle
import com.xiaocydx.sample.showToast
import com.xiaocydx.sample.viewLifecycle
import com.xiaocydx.sample.viewLifecycleScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * 视频流页面
 *
 * 在实际场景中，视频流页面可能会供多处业务复用，视频流的数据通过其他业务数据过滤、转换而来，
 * 分页加载是处理起来比较麻烦的场景，需要调用处页面和视频流页面共享分页数据来源和加载状态，
 * 共享分页数据来源和加载状态的做法，能彻底解决两个页面分开加载再进行同步而产生的一致性问题。
 *
 * 示例代码是构建一个分页数据流，它会发射分页数据容器，其中包含分页初始配置和分页事件流，
 * 分页事件流发射加载过程产生的事件，分页事件携带加载状态和列表数据，加载状态保存在[Pager]，
 * 列表数据保存在[ListState]。
 *
 * [Pager]提供原始分页数据流和保存加载状态，原始分页数据流通过[broadcastIn]转换为可共享，
 * 可共享的分页数据流通过[storeIn]转换可保存，可共享的分页数据流保留在两个页面的父级作用域，
 * 可保存的分页数据流在各自的作用域内构建，有单独的[ListState]。
 *
 * 若两个页面的[ListState]还需要同步，例如在视频流页面的删除操作，需要同步到调用处页面，
 * 则通过发送事件完成[ListState]的同步即可，这种不涉及分页加载的同步需求，并不难处理。
 *
 * 视频流的数据通过其他业务数据过滤、转换而来，因此存在一页数据过滤完后，没有视频流数据的问题，
 * `AppendTrigger`的实现能解决这个问题，当视频流页面收集到一页空数据时，会自动触发下一页加载。
 *
 * @author xcc
 * @date 2023/7/30
 */
class VideoStreamFragment : Fragment(), TransformReceiver {
    private lateinit var requestManager: RequestManager
    private lateinit var binding: FragmetVideoStreamBinding
    private lateinit var videoAdapter: ListAdapter<VideoStreamItem, *>
    private val complexViewModel: ComplexListViewModel by viewModels(
        ownerProducer = { parentFragment ?: requireActivity() }
    )
    private val videoViewModel: VideoStreamViewModel by viewModels(
        factoryProducer = { VideoStreamViewModel.Factory(complexViewModel.videoStreamFlow()) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requestManager = Glide.with(this)
        binding = FragmetVideoStreamBinding
            .inflate(inflater, container, false)
        videoAdapter = bindingAdapter(
            uniqueId = VideoStreamItem::id,
            inflate = ItemVideoStreamBinding::inflate
        ) {
            onBindView {
                requestManager.load(it.coverUrl)
                    .centerCrop().into(ivCover)
            }
            doOnLongItemClick { _, _ ->
                binding.viewPager2.currentItem = 0
                showToast("长按平滑滚动至首位")
                false
            }
        }
        binding.viewPager2.apply {
            adapter = videoAdapter
            orientation = ORIENTATION_VERTICAL
            replaceWithSwipeRefresh(videoAdapter)
        }
        return SystemBarsContainer(requireContext())
            .setDarkStatusBarOnResume(this)
            .setStatusBarEdgeToEdge(true)
            .setGestureNavBarEdgeToEdge(true)
            .setWindowNavigationBarColor(this)
            .attach(binding.root)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupDebugLog()
        val isFirstCreate = savedInstanceState == null
        val enterTransition = setTransformEnterTransition()
        enterTransition.duration = 200
        val viewPager2 = binding.viewPager2
        if (isFirstCreate) {
            // Fragment首次创建，推迟过渡动画，直到选中位置的图片加载结束，
            // 过渡动画结束时，才将viewPager2.offscreenPageLimit修改为1，
            // 确保startPostponedEnterTransition()不受两侧加载图片影响。
            EnterTransitionListener(this, requestManager).postpone()
            enterTransition.doOnEnd(once = true) { viewPager2.offscreenPageLimit = 1 }
        } else {
            // Fragment重新创建，直接将viewPager2.offscreenPageLimit修改为1
            viewPager2.offscreenPageLimit = 1
        }

        viewLifecycleScope.launchSafely {
            // Fragment首次创建，同步初始状态，下面的selectPosition是同步后的结果
            complexViewModel.consumePendingInitialState()?.let(videoViewModel::syncInitialState)

            // 首次刷新完成后，再选中位置和注册页面回调，这个处理对Fragment重新创建同样适用
            videoAdapter.pagingCollector.loadStatesFlow().first { it.refresh.isSuccess }
            viewPager2.setCurrentItem(videoViewModel.selectPosition.value, false)
            viewPager2.registerOnPageChangeCallback(onScrollStateChanged = callback@{ state ->
                // 不依靠onSelected()更新选中位置，因为该函数被调用时仍在进行平滑滚动，
                // 状态更改为IDLE时才更新选中位置，避免平滑滚动期间同步申请布局造成卡顿。
                if (state != ViewPager2.SCROLL_STATE_IDLE) return@callback
                videoViewModel.selectVideo(viewPager2.currentItem)
            })

            // Fragment首次创建，需要丢弃一次选中值，避免冗余的同步，
            // Fragment重新创建，需要根据最新选中值，同步选中的位置。
            var selectVideoId = videoViewModel.selectVideoId
            if (isFirstCreate) selectVideoId = selectVideoId.drop(count = 1)
            selectVideoId.collect(complexViewModel::syncSelectId)
        }

        binding.tvTitle.doOnApplyWindowInsets { v, insets, initialState ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.updatePadding(top = initialState.paddings.top + statusBars.top)
        }

        videoViewModel.selectVideoTitle
            .flowWithLifecycle(viewLifecycle)
            .distinctUntilChanged()
            .onEach(binding.tvTitle::setText)
            .launchIn(viewLifecycleScope)

        videoViewModel.videoFlow
            .onEach(videoAdapter.pagingCollector)
            .repeatOnLifecycle(viewLifecycle)
            .launchInLifecycleScope()
    }

    private fun setupDebugLog() {
        viewLifecycle.doOnStateChanged { source, event ->
            val currentState = source.lifecycle.currentState
            Log.d("VideoStreamFragment", "currentState = ${currentState}, event = $event")
        }
        videoAdapter.pagingCollector.addLoadStatesListener { _, current ->
            Log.d("VideoStreamFragment", "loadStates = $current")
        }
    }

    private class EnterTransitionListener(
        private var fragment: Fragment?,
        private val requestManager: RequestManager
    ) : RequestListener<Any> {

        fun postpone() {
            fragment?.postponeEnterTransition()
            requestManager.addDefaultRequestListener(this)
        }

        override fun onResourceReady(
            resource: Any?, model: Any?,
            target: Target<Any>?, dataSource: DataSource?, isFirstResource: Boolean
        ): Boolean = startPostponedEnterTransition()

        override fun onLoadFailed(
            e: GlideException?, model: Any?,
            target: Target<Any>?, isFirstResource: Boolean
        ): Boolean = startPostponedEnterTransition()

        private fun startPostponedEnterTransition(): Boolean {
            // RequestManager没提供removeDefaultRequestListener()，做置空处理
            fragment?.startPostponedEnterTransition()
            fragment = null
            return false
        }
    }
}