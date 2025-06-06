package com.xiaocydx.sample.viewpager2.loop

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import androidx.viewpager2.widget.MarginPageTransformer
import com.xiaocydx.accompanist.lifecycle.launchRepeatOnLifecycle
import com.xiaocydx.accompanist.view.dp
import com.xiaocydx.accompanist.view.snackbar
import com.xiaocydx.accompanist.viewpager2.launchBanner
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.itemclick.reduce.doOnItemClick
import com.xiaocydx.cxrv.list.doOnListChanged
import com.xiaocydx.cxrv.list.listCollector
import com.xiaocydx.cxrv.list.onEach
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.cxrv.viewpager2.loop.LookupDirection
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerController
import com.xiaocydx.cxrv.viewpager2.loop.setPageTransformer
import com.xiaocydx.sample.common.Action
import com.xiaocydx.sample.common.actionList
import com.xiaocydx.sample.databinding.ActivityLoopPagerBinding
import com.xiaocydx.sample.viewpager2.loop.LoopPagerActivity.LoopPagerAction.Append
import com.xiaocydx.sample.viewpager2.loop.LoopPagerActivity.LoopPagerAction.CancelBanner
import com.xiaocydx.sample.viewpager2.loop.LoopPagerActivity.LoopPagerAction.LaunchBanner
import com.xiaocydx.sample.viewpager2.loop.LoopPagerActivity.LoopPagerAction.Refresh
import com.xiaocydx.sample.viewpager2.loop.LoopPagerActivity.LoopPagerAction.Scroll
import com.xiaocydx.sample.viewpager2.loop.LoopPagerActivity.LoopPagerAction.SmoothScroll
import kotlinx.coroutines.Job

/**
 * [LoopPagerController]的示例代码
 *
 * @author xcc
 * @date 2023/5/11
 */
class LoopPagerActivity : AppCompatActivity() {
    private val viewModel: ContentViewModel by viewModels()
    private lateinit var adapter: ContentListAdapter
    private lateinit var controller: LoopPagerController
    private var bannerJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityLoopPagerBinding.inflate(layoutInflater)
        setContentView(binding.initView().initCollect().root)
    }

    private fun ActivityLoopPagerBinding.initView() = apply {
        adapter = ContentListAdapter().apply {
            doOnListChanged {
                // 列表已更改，在下一帧布局完成后，触发requestTransform()修正间距
                viewPager2.doOnPreDraw { viewPager2.requestTransform() }
            }

            doOnItemClick { holder, item ->
                snackbar().setText("""
                |   item.text = ${item.text}
                |   layoutPosition = ${holder.layoutPosition}
                |   bindingAdapterPosition = ${holder.bindingAdapterPosition}
            """.trimMargin()).show()
            }
        }

        controller = LoopPagerController(viewPager2).apply {
            setAdapter(adapter)
            setPadding(left = 50.dp, right = 50.dp)
            // 注意：LoopPagerController支持全部局部更新操作和RecyclerView.ItemAnimator，
            // 而示例代码不会运行动画，是因为设置PageTransformer会将ItemAnimator置为null。
            setPageTransformer(ScaleInTransformer(), MarginPageTransformer(10.dp))
        }

        rvAction.actionList {
            submitList(LoopPagerAction.entries.toList())
            doOnItemClick(action = ::performLoopPagerAction)
        }
    }

    private fun ActivityLoopPagerBinding.initCollect() = apply {
        adapter.doOnListChanged changed@{
            if (!viewModel.consumeScrollToFirst()) return@changed
            controller.scrollToPosition(0)
        }
        viewModel.flow
            .onEach(adapter.listCollector)
            .launchRepeatOnLifecycle(lifecycle)
    }

    private fun performLoopPagerAction(action: LoopPagerAction) {
        val position = 0
        val timeMillis = 1000L
        val text = when (action) {
            Refresh -> {
                viewModel.refresh(timeMillis)
                "${timeMillis / 1000}s后刷新"
            }
            Append -> {
                viewModel.append(timeMillis)
                "${timeMillis / 1000}s后添加"
            }
            Scroll -> {
                controller.scrollToPosition(position)
                "非平滑滚动至\nbindingAdapterPosition = $position"
            }
            SmoothScroll -> {
                controller.smoothScrollToPosition(position, LookupDirection.START)
                "平滑滚动至\nbindingAdapterPosition = $position"
            }
            LaunchBanner -> {
                bannerJob?.cancel()
                bannerJob = controller.launchBanner(adapter, lifecycle, durationMs = 500)
                "启动Banner轮播交互"
            }
            CancelBanner -> {
                bannerJob?.cancel()
                bannerJob = null
                "取消Banner轮播交互"
            }
        }
        snackbar().setText(text).show()
    }

    private enum class LoopPagerAction : Action {
        Refresh,
        Append,
        Scroll,
        SmoothScroll,
        LaunchBanner,
        CancelBanner;

        override val text = name
    }
}