package com.xiaocydx.sample.paging.complex

import android.app.Activity
import android.app.Instrumentation
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.paging.isSuccess
import com.xiaocydx.cxrv.paging.onEach
import com.xiaocydx.cxrv.paging.pagingCollector
import com.xiaocydx.sample.databinding.ItemVideoStreamBinding
import com.xiaocydx.sample.doOnStateChanged
import com.xiaocydx.sample.extensions.doOnEnd
import com.xiaocydx.sample.launchSafely
import com.xiaocydx.sample.layoutParams
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.paging.config.loadStatesFlow
import com.xiaocydx.sample.paging.config.replaceWithSwipeRefresh
import com.xiaocydx.sample.registerOnPageChangeCallback
import com.xiaocydx.sample.repeatOnLifecycle
import com.xiaocydx.sample.wrapContent
import kotlinx.coroutines.flow.first
import java.lang.ref.WeakReference

/**
 * @author xcc
 * @date 2023/7/30
 */
class VideoStreamActivity : AppCompatActivity() {
    private lateinit var root: ViewGroup
    private lateinit var headerMask: View
    private lateinit var viewPager2: ViewPager2
    private lateinit var videoAdapter: ListAdapter<ComplexItem, *>
    private val viewModel: VideoStreamViewModel by viewModels(
        factoryProducer = { VideoStreamViewModel.Factory(this) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initCollect()
        initEdgeToEdge()
    }

    private fun initView() {
        root = FrameLayout(this).layoutParams(matchParent, matchParent)
        headerMask = View(this).layoutParams(matchParent, wrapContent)
        viewPager2 = ViewPager2(this).layoutParams(matchParent, matchParent)
        root.addView(viewPager2)
        root.addView(headerMask)

        root.setBackgroundColor(Color.BLACK)
        headerMask.setBackgroundColor(0x4DFFFFFF)
        videoAdapter = bindingAdapter(
            uniqueId = ComplexItem::id,
            inflate = ItemVideoStreamBinding::inflate
        ) {
            val requestManager = Glide.with(this@VideoStreamActivity)
            initEnterSharedElement(requestManager)
            onBindView { requestManager.load(it.coverUrl).centerCrop().into(ivCover) }
        }
        viewPager2.apply {
            id = viewModel.vpId
            adapter = videoAdapter
            orientation = ORIENTATION_VERTICAL
            replaceWithSwipeRefresh(videoAdapter)
        }

        setContentView(root)
    }

    private fun initCollect() {
        lifecycleScope.launchSafely {
            videoAdapter.pagingCollector.loadStatesFlow().first { it.refresh.isSuccess }
            viewPager2.setCurrentItem(viewModel.consumePosition(), false)
        }

        viewModel.flow
            .onEach(videoAdapter.pagingCollector)
            .repeatOnLifecycle(lifecycle)
            .launchInLifecycleScope()
    }

    private fun initEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, inset ->
            val systemBars = inset.getInsets(WindowInsetsCompat.Type.systemBars())
            root.updatePadding(bottom = systemBars.bottom)
            headerMask.updateLayoutParams { height = systemBars.top }
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun initEnterSharedElement(requestManager: RequestManager) {
        // 1. 初始化sharedElementEnterTransition
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        root.transitionName = viewModel.sharedName
        window.sharedElementEnterTransition = MaterialContainerTransform()
            .apply { duration = 250L }.apply { addTarget(root) }
        setEnterSharedElementCallback(MaterialContainerTransformSharedElementCallback())

        // 2. 推迟过渡动画，直至图片加载结束
        postponeEnterTransition()
        requestManager.addDefaultRequestListener(StartPostponedEnterTransitionListener(this))
        window.sharedElementEnterTransition.doOnEnd(once = true) {
            // 过渡动画结束时，才将viewPager2.offscreenPageLimit修改为1，
            // 确保startPostponedEnterTransition()不受两侧加载图片影响。
            viewPager2.offscreenPageLimit = 1
        }

        // 3. 发送事件同步当前位置的videoId
        viewPager2.registerOnPageChangeCallback(onSelected = viewModel::selectVideo)

        // 4. 兼容Android 10及以上，执行onStop()后再恢复，退出时无过渡动画的问题
        lifecycle.doOnStateChanged { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE
                    && !isFinishing && Build.VERSION.SDK_INT >= 29) {
                Instrumentation().callActivityOnSaveInstanceState(this, Bundle())
            }
        }
    }

    private class StartPostponedEnterTransitionListener(activity: Activity) : RequestListener<Any> {
        private var activityRef: WeakReference<Activity>? = WeakReference(activity)

        override fun onResourceReady(
            resource: Any?, model: Any?,
            target: Target<Any>?, dataSource: DataSource?, isFirstResource: Boolean
        ): Boolean = startPostponedEnterTransition()

        override fun onLoadFailed(
            e: GlideException?, model: Any?,
            target: Target<Any>?, isFirstResource: Boolean
        ): Boolean = startPostponedEnterTransition()

        private fun startPostponedEnterTransition(): Boolean {
            activityRef?.get()?.startPostponedEnterTransition()
            activityRef = null
            return false
        }
    }
}