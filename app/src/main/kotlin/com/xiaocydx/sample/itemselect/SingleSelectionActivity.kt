package com.xiaocydx.sample.itemselect

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.binding.BindingAdapter
import com.xiaocydx.cxrv.binding.Inflate
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.itemselect.hasPayload
import com.xiaocydx.cxrv.itemselect.singleSelection
import com.xiaocydx.cxrv.itemselect.toggleSelect
import com.xiaocydx.cxrv.list.*
import com.xiaocydx.sample.databinding.ItemSelectionBinding
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.overScrollNever
import com.xiaocydx.sample.withLayoutParams

/**
 * [singleSelection]示例代码
 *
 * 1. 配置单项选择功能。
 * 2. 页面配置发生变更时（例如旋转屏幕），保留选择状态。
 *
 * @author xcc
 * @date 2022/2/18
 */
class SingleSelectionActivity : AppCompatActivity() {
    private val viewModel: SelectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
    }

    private fun contentView() = RecyclerView(this).apply {
        id = viewModel.rvId
        linear().fixedSize()
        divider(height = 0.5f.dp) { color(0xFF7E7AAA.toInt()) }
        adapter(SingleSelectionBindingAdapter())
        // adapter(SingleSelectionAdapter())
        overScrollNever()
        withLayoutParams(matchParent, matchParent)
    }

    /**
     * [BindingAdapter]的构建函数，适用于简单列表场景
     */
    @Suppress("FunctionName")
    private fun SingleSelectionBindingAdapter() = bindingAdapter(
        uniqueId = { item: String -> item },
        inflate = ItemSelectionBinding::inflate
    ) {
        val selection = singleSelection(
            itemKey = { item: String -> item },
            itemAccess = { getItem(it) }
        ).initSelected(viewModel)

        doOnItemClick { holder, item ->
            // selection.select(holder)
            selection.toggleSelect(holder)
        }
        submitList((1..20).map { "Selection-$it" })

        onBindView { item ->
            viewSelect.isVisible = selection.isSelected(holder)
            if (selection.hasPayload(holder)) return@onBindView
            tvSelection.text = item
        }
    }

    private inner class SingleSelectionAdapter : BindingAdapter<String, ItemSelectionBinding>() {
        private val selection = singleSelection(
            itemKey = { item: String -> item },
            itemAccess = { getItem(it) }
        ).initSelected(viewModel)

        init {
            doOnItemClick { holder, item ->
                // selection.select(holder)
                selection.toggleSelect(holder)
            }
            submitList((1..20).map { "Selection-$it" })
        }

        override fun inflate(): Inflate<ItemSelectionBinding> {
            return ItemSelectionBinding::inflate
        }

        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun ItemSelectionBinding.onBindView(item: String) {
            viewSelect.isVisible = selection.isSelected(holder)
            if (selection.hasPayload(holder)) return
            tvSelection.text = item
        }
    }
}