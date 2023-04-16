package com.xiaocydx.sample.itemclick

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.list.*
import com.xiaocydx.sample.databinding.ActivityItemClickBinding
import com.xiaocydx.sample.databinding.ItemButtonBinding
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.itemclick.scenes.*

/**
 * ItemClick示例代码
 *
 * @author xcc
 * @date 2022/2/18
 */
class ItemClickActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityItemClickBinding.inflate(layoutInflater).initView().root)
    }

    private fun ActivityItemClickBinding.initView() = apply {
        val list = ItemClickScenesList()
        var disposable = list.first().apply(rvClick)

        rvClick.linear()
        rvScenes
            .linear(orientation = HORIZONTAL)
            .divider {
                width = 5.dp
                height = 5.dp
                horizontalEdge = true
            }
            .adapter(bindingAdapter(
                uniqueId = ItemClickScenes::text,
                inflate = ItemButtonBinding::inflate
            ) {
                submitList(list)
                doOnItemClick { _, item ->
                    disposable.dispose()
                    disposable = item.apply(rvClick)
                }
                onBindView { root.text = it.text }
            })
    }

    /**
     * [ItemClickScenes]演示了从基础函数开始，逐步增强和简化的过程
     */
    @Suppress("FunctionName")
    private fun ItemClickScenesList() = listOf(
        AbsoluteScenes(),
        RelativeScenes(),
        ListAdapterScenes(),
        ViewTypeDelegateScenes()
    )
}