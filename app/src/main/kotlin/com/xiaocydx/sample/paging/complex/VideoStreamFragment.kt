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
import com.xiaocydx.cxrv.paging.isSuccess
import com.xiaocydx.cxrv.paging.onEach
import com.xiaocydx.cxrv.paging.pagingCollector
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
 * @author xcc
 * @date 2023/7/30
 */
class VideoStreamFragment : Fragment(), TransformReceiver {
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
        val requestManager = Glide.with(this)
        setupEnterTransition(requestManager)
        binding = FragmetVideoStreamBinding.inflate(
            inflater, container, false
        )
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
        viewLifecycle.doOnStateChanged { source, event ->
            val currentState = source.lifecycle.currentState
            Log.d("VideoStreamFragment", "currentState = ${currentState}, event = $event")
        }

        viewLifecycleScope.launchSafely {
            // Fragment首次创建，同步初始状态，下面的selectPosition是同步后的结果
            val initialState = complexViewModel.consumePendingInitialState()
            initialState?.let(videoViewModel::syncInitialState)

            // 首次刷新完成后，再选中位置和注册页面回调，这个处理对Fragment重建流程同样适用
            videoAdapter.pagingCollector.loadStatesFlow().first { it.refresh.isSuccess }
            binding.viewPager2.setCurrentItem(videoViewModel.selectPosition.value, false)
            binding.viewPager2.registerOnPageChangeCallback(onSelected = videoViewModel::selectVideo)

            // Fragment首次创建，需要丢弃一次选中值，避免冗余的同步，
            // Fragment重建流程，需要根据最新选中值，同步选中的位置。
            var selectVideoId = videoViewModel.selectVideoId
            if (initialState != null) selectVideoId = selectVideoId.drop(count = 1)
            selectVideoId.collect(complexViewModel::syncSelectVideo)
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

    private fun setupEnterTransition(requestManager: RequestManager) {
        // 1. 初始化enterTransition
        val enterTransition = setTransformEnterTransition()
        enterTransition.duration = 200

        // 2. 推迟过渡动画，直至图片加载结束
        EnterTransitionListener(this, requestManager).postpone()
        enterTransition.doOnEnd(once = true) {
            // 过渡动画结束时，才将viewPager2.offscreenPageLimit修改为1，
            // 确保startPostponedEnterTransition()不受两侧加载图片影响。
            binding.viewPager2.offscreenPageLimit = 1
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