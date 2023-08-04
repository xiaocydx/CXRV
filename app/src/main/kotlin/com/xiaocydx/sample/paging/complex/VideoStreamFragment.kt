package com.xiaocydx.sample.paging.complex

import android.os.Bundle
import android.transition.Transition
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
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.paging.isSuccess
import com.xiaocydx.cxrv.paging.onEach
import com.xiaocydx.cxrv.paging.pagingCollector
import com.xiaocydx.sample.databinding.FragmetVideoStreamBinding
import com.xiaocydx.sample.databinding.ItemVideoStreamBinding
import com.xiaocydx.sample.doOnApplyWindowInsets
import com.xiaocydx.sample.launchSafely
import com.xiaocydx.sample.paging.complex.transform.TransformReceiver
import com.xiaocydx.sample.paging.complex.transform.TransitionListenerAdapter
import com.xiaocydx.sample.paging.config.loadStatesFlow
import com.xiaocydx.sample.paging.config.replaceWithSwipeRefresh
import com.xiaocydx.sample.registerOnPageChangeCallback
import com.xiaocydx.sample.repeatOnLifecycle
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
            onBindView { requestManager.load(it.coverUrl).centerCrop().into(ivCover) }
        }
        binding.viewPager2.apply {
            adapter = videoAdapter
            orientation = ORIENTATION_VERTICAL
            replaceWithSwipeRefresh(videoAdapter)
        }
        return SystemBarsContainer(requireContext())
            .setStatusBarEdgeToEdge(true)
            .setGestureNavBarEdgeToEdge(true)
            .attach(binding.root)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleScope.launchSafely {
            // Fragment首次创建，同步初始状态，下面的selectPosition是同步后的结果
            val initialState = complexViewModel.consumePendingInitialState()
            initialState?.let(videoViewModel::syncInitialState)

            // 首次刷新完成后，再选中位置和注册页面回调，这个处理对Fragment重建流程同样适用
            videoAdapter.pagingCollector.loadStatesFlow().first { it.refresh.isSuccess }
            binding.viewPager2.setCurrentItem(videoViewModel.selectPosition.value, false)
            binding.viewPager2.registerOnPageChangeCallback(onSelected = videoViewModel::selectVideo)

            // Fragment首次创建，需要丢弃一次选中值，避免冗余的同步处理，
            // Fragment重建流程，需要根据最新的选中值，同步选中的位置。
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
            .onEach { binding.tvTitle.text = it }
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
        postponeEnterTransition()
        requestManager.addDefaultRequestListener(StartPostponedEnterTransitionListener(this))
        enterTransition.addListener(object : TransitionListenerAdapter() {
            override fun onTransitionEnd(transition: Transition) {
                // 过渡动画结束时，才将viewPager2.offscreenPageLimit修改为1，
                // 确保startPostponedEnterTransition()不受两侧加载图片影响。
                transition.removeListener(this)
                binding.viewPager2.offscreenPageLimit = 1
            }
        })
    }

    private class StartPostponedEnterTransitionListener(
        private var fragment: Fragment?
    ) : RequestListener<Any> {

        override fun onResourceReady(
            resource: Any?, model: Any?,
            target: Target<Any>?, dataSource: DataSource?, isFirstResource: Boolean
        ): Boolean = startPostponedEnterTransition()

        override fun onLoadFailed(
            e: GlideException?, model: Any?,
            target: Target<Any>?, isFirstResource: Boolean
        ): Boolean = startPostponedEnterTransition()

        private fun startPostponedEnterTransition(): Boolean {
            // RequestManager没提供removeDefaultRequestListener()函数，做置空处理
            fragment?.startPostponedEnterTransition()
            fragment = null
            return false
        }
    }
}