package com.xiaocydx.sample.paging.complex

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.transition.Transition
import androidx.transition.TransitionListenerAdapter
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
import com.xiaocydx.sample.launchSafely
import com.xiaocydx.sample.paging.complex.transform.TransformReceiver
import com.xiaocydx.sample.paging.config.loadStatesFlow
import com.xiaocydx.sample.paging.config.replaceWithSwipeRefresh
import com.xiaocydx.sample.registerOnPageChangeCallback
import com.xiaocydx.sample.repeatOnLifecycle
import com.xiaocydx.sample.viewLifecycle
import com.xiaocydx.sample.viewLifecycleScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import java.lang.ref.WeakReference

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
        binding = FragmetVideoStreamBinding.inflate(
            inflater, container, false
        )
        videoAdapter = bindingAdapter(
            uniqueId = VideoStreamItem::id,
            inflate = ItemVideoStreamBinding::inflate
        ) {
            // FIXME: 修复fragment退出过程自动清除图片的问题
            // val requestManager = Glide.with(this@VideoStreamFragment)
            val requestManager = Glide.with(requireActivity())
            initEnterTransition(requestManager)
            onBindView {
                requestManager.load(it.coverUrl)
                    .centerCrop().into(ivCover)
                tvTitle.text = it.title
            }
        }
        binding.viewPager2.apply {
            adapter = videoAdapter
            orientation = ORIENTATION_VERTICAL
            replaceWithSwipeRefresh(videoAdapter)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleScope.launchSafely {
            val initialState = complexViewModel.consumePendingInitialState()
            if (initialState != null) {
                videoViewModel.syncInitialState(initialState)
                videoAdapter.pagingCollector.loadStatesFlow().first { it.refresh.isSuccess }
                binding.viewPager2.setCurrentItem(initialState.position, false)
                binding.viewPager2.registerOnPageChangeCallback(onSelected = videoViewModel::selectVideo)
                videoViewModel.selectId.drop(count = 1).collect(complexViewModel::syncSelectVideo)
            } else {
                videoViewModel.selectId.collect(complexViewModel::syncSelectVideo)
            }
        }

        videoViewModel.videoFlow
            .onEach(videoAdapter.pagingCollector)
            .repeatOnLifecycle(viewLifecycle)
            .launchInLifecycleScope()

        // TODO: 自行处理WindowInsets
        binding.headerMask.updateLayoutParams { height = 0 }
        // ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, inset ->
        //     val systemBars = inset.getInsets(WindowInsetsCompat.Type.systemBars())
        //     root.updatePadding(bottom = systemBars.bottom)
        //     headerMask.updateLayoutParams { height = systemBars.top }
        //     WindowInsetsCompat.CONSUMED
        // }
    }

    private fun initEnterTransition(requestManager: RequestManager) {
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

    private class StartPostponedEnterTransitionListener(fragment: Fragment) : RequestListener<Any> {
        private var fragmentRef: WeakReference<Fragment>? = WeakReference(fragment)

        override fun onResourceReady(
            resource: Any?, model: Any?,
            target: Target<Any>?, dataSource: DataSource?, isFirstResource: Boolean
        ): Boolean = startPostponedEnterTransition()

        override fun onLoadFailed(
            e: GlideException?, model: Any?,
            target: Target<Any>?, isFirstResource: Boolean
        ): Boolean = startPostponedEnterTransition()

        private fun startPostponedEnterTransition(): Boolean {
            fragmentRef?.get()?.startPostponedEnterTransition()
            fragmentRef = null
            return false
        }
    }
}