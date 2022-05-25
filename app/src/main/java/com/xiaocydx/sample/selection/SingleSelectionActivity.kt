package com.xiaocydx.sample.selection

import android.os.Bundle
import android.view.View
import android.view.View.OVER_SCROLL_NEVER
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.binding.BindingAdapter
import com.xiaocydx.cxrv.binding.Inflate
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.extension.*
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.getItem
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.cxrv.selection.hasPayload
import com.xiaocydx.cxrv.selection.isSelected
import com.xiaocydx.cxrv.selection.toggleSelect
import com.xiaocydx.sample.databinding.ItemSelectionBinding
import com.xiaocydx.sample.dp

/**
 * [singleSelection]示例代码
 *
 * * 配置单项选择功能。
 * * 页面配置发生变更时（例如旋转屏幕），保留选择状态。
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

    private fun contentView(): View = RecyclerView(this).apply {
        id = viewModel.rvId
        linear().fixedSize().divider {
            height = 0.5.dp
            color = 0xFF7E7AAA.toInt()
        }
        adapter = SingleSelectionBindingAdapter()
        // adapter = SingleSelectionAdapter()
        layoutParams = LayoutParams(MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        overScrollMode = OVER_SCROLL_NEVER
    }

    @Suppress("FunctionName")
    private fun SingleSelectionBindingAdapter(): ListAdapter<*, *> {
        return bindingAdapter(
            uniqueId = { item: String -> item },
            inflate = ItemSelectionBinding::inflate
        ) {
            val selection = singleSelection(
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

        override fun ItemSelectionBinding.onBindView(item: String, payloads: List<Any>) {
            viewSelect.isVisible = selection.isSelected(holder)
            if (selection.hasPayload(holder)) {
                return
            }
            tvSelection.text = item
        }
    }
}