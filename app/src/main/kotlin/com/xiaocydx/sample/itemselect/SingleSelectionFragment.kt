package com.xiaocydx.sample.itemselect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.accompanist.view.dp
import com.xiaocydx.accompanist.view.layoutParams
import com.xiaocydx.accompanist.view.matchParent
import com.xiaocydx.cxrv.binding.binding
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.itemselect.hasPayload
import com.xiaocydx.cxrv.itemselect.singleSelection
import com.xiaocydx.cxrv.itemselect.toggleSelect
import com.xiaocydx.cxrv.list.getItem
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.databinding.ItemSelectionBinding

/**
 * [singleSelection]示例代码
 *
 * 1. 配置单项选择功能。
 * 2. 页面配置发生变更时（例如旋转屏幕），保留选择状态。
 *
 * @author xcc
 * @date 2022/2/18
 */
class SingleSelectionFragment : Fragment() {
    private val viewModel: SelectionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = RecyclerView(requireContext())
        .apply { id = viewModel.rvId }
        .layoutParams(matchParent, matchParent).linear()
        .divider { height(0.5f.dp).color(0xFF7E7AAA.toInt()) }
        .binding(
            uniqueId = SelectionItem::num,
            inflate = ItemSelectionBinding::inflate
        ) {
            submitList((1..20).map(::SelectionItem))

            // 第一步：初始化SingleSelection
            val selection = singleSelection(
                itemKey = SelectionItem::num,
                itemAccess = { getItem(it) }
            ).initSelected(viewModel)

            // 第二步：点击itemView时调用单项选择
            doOnItemClick { holder, item ->
                // selection.select(holder)
                selection.toggleSelect(holder)
            }

            // 第三步：添加选择视图的更新逻辑
            onBindView { item ->
                viewSelect.isVisible = selection.isSelected(holder)
                if (selection.hasPayload(holder)) return@onBindView
                tvSelection.text = item.text
            }
        }
}