package com.xiaocydx.sample.viewpager2.loop

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.MarginPageTransformer
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.list.doOnListChanged
import com.xiaocydx.cxrv.list.listCollector
import com.xiaocydx.cxrv.list.onEach
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerController
import com.xiaocydx.sample.databinding.ActivityLoopPagerBinding
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.onClick
import com.xiaocydx.sample.repeatOnLifecycle
import com.xiaocydx.sample.showToast
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * [LoopPagerController]的示例代码
 *
 * @author xcc
 * @date 2023/5/11
 */
class LoopPagerActivity : AppCompatActivity() {
    private val viewModel: ContentViewModel by viewModels()
    private lateinit var binding: ActivityLoopPagerBinding
    private lateinit var adapter: ContentListAdapter
    private lateinit var controller: LoopPagerController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoopPagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        initCollect()
    }

    private fun initView() = with(binding) {
        val timeMillis = 1000L
        btnRefresh.onClick {
            viewModel.refresh(timeMillis)
            showToast("点击刷新，等待${timeMillis / 1000}s")
        }
        btnAppend.onClick {
            viewModel.append(timeMillis)
            showToast("点击添加，等待${timeMillis / 1000}s")
        }

        adapter = ContentListAdapter()
        adapter.doOnItemClick { holder, item ->
            showToast("item.text = ${item.text}\n" +
                    "layoutPosition = ${holder.layoutPosition}\n" +
                    "bindingAdapterPosition = ${holder.bindingAdapterPosition}")
        }
        adapter.doOnListChanged {
            // 列表已更改，在下一帧布局完成后，触发requestTransform()修正间距
            viewPager2.doOnPreDraw { viewPager2.requestTransform() }
        }

        viewPager2.setPageTransformer(MarginPageTransformer(10.dp))
        controller = LoopPagerController(viewPager2)
        controller.setAdapter(adapter)
        controller.setPadding(left = 40.dp, right = 40.dp)
    }

    private fun initCollect() {
        viewModel.refreshEvent
            .onEach { controller.scrollToPosition(0) }
            .launchIn(lifecycleScope)

        viewModel.flow
            .onEach(adapter.listCollector)
            .repeatOnLifecycle(lifecycle)
            .launchInLifecycleScope()
    }
}