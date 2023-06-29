package com.xiaocydx.sample.itemselect

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.binding.BindingAdapter
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.itemselect.hasPayload
import com.xiaocydx.cxrv.itemselect.singleSelection
import com.xiaocydx.cxrv.itemselect.toggleSelect
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.getItem
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.databinding.ItemSelectionBinding
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.layoutParams
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.overScrollNever

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

    /**
     * [BindingAdapter]的构建函数，适用于简单列表场景
     */
    private fun contentView() = RecyclerView(this)
        .apply { id = viewModel.rvId }
        .layoutParams(matchParent, matchParent)
        .overScrollNever().linear().fixedSize()
        .divider(height = 0.5f.dp) { color(0xFF7E7AAA.toInt()) }
        .adapter(bindingAdapter(
            uniqueId = SelectionItem::num,
            inflate = ItemSelectionBinding::inflate
        ) {
            val selection = singleSelection(
                itemKey = SelectionItem::num,
                itemAccess = { getItem(it) }
            ).initSelected(viewModel)

            submitList((1..20).map(::SelectionItem))
            doOnItemClick { holder, item ->
                // selection.select(holder)
                selection.toggleSelect(holder)
            }
            onBindView { item ->
                viewSelect.isVisible = selection.isSelected(holder)
                if (selection.hasPayload(holder)) return@onBindView
                tvSelection.text = item.text
            }
        })
}