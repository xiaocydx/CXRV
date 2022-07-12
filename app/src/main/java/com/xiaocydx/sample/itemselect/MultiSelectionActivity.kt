package com.xiaocydx.sample.itemselect

import android.os.Bundle
import android.view.View
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
import com.xiaocydx.cxrv.itemselect.isSelected
import com.xiaocydx.cxrv.itemselect.multiSelection
import com.xiaocydx.cxrv.itemselect.toggleSelect
import com.xiaocydx.cxrv.list.*
import com.xiaocydx.sample.databinding.ItemSelectionBinding
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.overScrollNever
import com.xiaocydx.sample.withLayoutParams

/**
 * [multiSelection]示例代码
 *
 * * 配置多项选择功能。
 * * 页面配置发生变更时（例如旋转屏幕），保留选择状态。
 *
 * @author xcc
 * @date 2022/2/18
 */
class MultiSelectionActivity : AppCompatActivity() {
    private val viewModel: SelectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
    }

    private fun contentView(): View = RecyclerView(this).apply {
        id = viewModel.rvId
        adapter = MultiSelectionBindingAdapter()
        // adapter = MultiSelectionAdapter()
        linear().fixedSize().divider {
            height = 0.5f.dp
            color = 0xFF7E7AAA.toInt()
        }
        overScrollNever()
        withLayoutParams(matchParent, matchParent)
    }

    @Suppress("FunctionName")
    private fun MultiSelectionBindingAdapter(): ListAdapter<*, *> {
        return bindingAdapter(
            uniqueId = { item: String -> item },
            inflate = ItemSelectionBinding::inflate
        ) {
            val selection = multiSelection(
                itemKey = { item: String -> item },
                itemAccess = { getItem(it) }
            ).initSelected(viewModel)

            onBindViewPayloads { item, _ ->
                viewSelect.isVisible = selection.isSelected(holder)
                if (selection.hasPayload(holder)) {
                    return@onBindViewPayloads
                }
                tvSelection.text = item
            }

            doOnItemClick { holder, item ->
                // selection.select(holder)
                selection.toggleSelect(holder)
            }

            submitList((1..20).map { "Selection-$it" })
        }
    }

    private inner class MultiSelectionAdapter : BindingAdapter<String, ItemSelectionBinding>() {
        private val selection = multiSelection(
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

        override fun ItemSelectionBinding.onBindView(item: String, payloads: List<Any>) {
            viewSelect.isVisible = selection.isSelected(holder)
            if (selection.hasPayload(holder)) {
                return
            }
            tvSelection.text = item
        }
    }
}