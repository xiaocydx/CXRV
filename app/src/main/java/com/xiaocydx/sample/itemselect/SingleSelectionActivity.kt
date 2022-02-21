package com.xiaocydx.sample.itemselect

import android.os.Bundle
import android.view.View
import android.view.View.OVER_SCROLL_NEVER
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.recycler.binding.BindingAdapter
import com.xiaocydx.recycler.binding.Inflate
import com.xiaocydx.recycler.binding.bindingAdapter
import com.xiaocydx.recycler.extension.*
import com.xiaocydx.recycler.list.ListAdapter
import com.xiaocydx.recycler.list.getItem
import com.xiaocydx.recycler.list.submitList
import com.xiaocydx.sample.databinding.ItemSelectionBinding
import com.xiaocydx.sample.dp

/**
 * [SingleSelection]示例代码
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

    private fun contentView(): View {
        return RecyclerView(this).apply {
            id = viewModel.rvId
            linear()
            divider {
                height = 0.5.dp
                color = 0xFF7E7AAA.toInt()
            }
            adapter = SingleSelectionBindingAdapter()
            // adapter = SingleSelectionAdapter()
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
            overScrollMode = OVER_SCROLL_NEVER
        }
    }

    @Suppress("FunctionName")
    private fun SingleSelectionBindingAdapter(): ListAdapter<*, *> {
        return bindingAdapter(
            uniqueId = { item: String -> item },
            inflate = ItemSelectionBinding::inflate
        ) {
            val selection = SingleSelection(
                adapter = this,
                itemKey = { item: String -> item },
                itemAccess = { getItem(it) }
            ).apply { saveToViewModel(viewModel) }

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
        private val selection = SingleSelection(
            adapter = this,
            itemKey = { item: String -> item },
            itemAccess = { getItem(it) }
        ).apply { saveToViewModel(viewModel) }

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