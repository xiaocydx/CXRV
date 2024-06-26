package com.xiaocydx.sample.itemtouch

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.accompanist.view.dp
import com.xiaocydx.accompanist.view.layoutParams
import com.xiaocydx.accompanist.view.matchParent
import com.xiaocydx.cxrv.binding.BindingHolder
import com.xiaocydx.cxrv.concat.Concat
import com.xiaocydx.cxrv.concat.toAdapter
import com.xiaocydx.cxrv.itemtouch.addItemTouchCallback
import com.xiaocydx.cxrv.itemtouch.itemTouch
import com.xiaocydx.cxrv.itemtouch.onDragMoveItem
import com.xiaocydx.cxrv.itemtouch.onSwipeRemoveItem
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.multitype.listAdapter
import com.xiaocydx.cxrv.multitype.register
import com.xiaocydx.sample.common.TextItem
import com.xiaocydx.sample.common.TextType1Delegate
import com.xiaocydx.sample.common.TextType2Delegate
import com.xiaocydx.sample.common.initMultiTypeTextItems
import com.xiaocydx.sample.databinding.ItemTextType1Binding
import com.xiaocydx.sample.databinding.ItemTextType2Binding

/**
 * ItemTouch示例代码
 *
 * @author xcc
 * @date 2023/3/5
 */
class ItemTouchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
    }

    /**
     * 连接Header和Footer，拖动时移动item不会跟Header和Footer交换位置
     */
    private fun contentView() = RecyclerView(this)
        .layoutParams(matchParent, matchParent)
        .linear().adapter(Concat.content(createListAdapter())
            .header(createView(isHeader = true).toAdapter())
            .footer(createView(isHeader = false).toAdapter())
            .concat())

    /**
     * ItemTouch的配置方式有三种：
     * 1. [RecyclerView.addItemTouchCallback]。
     * 2. [RecyclerView.itemTouch]。
     * 3. [ListAdapter.itemTouch]。
     *
     * [RecyclerView.addItemTouchCallback]是ItemTouch的基础配置方式，
     * [RecyclerView.itemTouch]在[RecyclerView.addItemTouchCallback]的基础上简化配置代码，
     * [ListAdapter.itemTouch]在[RecyclerView.itemTouch]的基础上进一步简化配置代码。
     *
     * 该函数展示了如何通过[ListAdapter.itemTouch]完成ItemTouch的配置。
     */
    private fun createListAdapter() = listAdapter<TextItem> {
        register(TextType1Delegate())
        register(TextType2Delegate())
        initMultiTypeTextItems()
        itemTouch {
            // 拖动时移动item，结合ListAdapter自身特性的简化函数
            onDragMoveItem()
            // 侧滑时移除item，结合ListAdapter自身特性的简化函数
            onSwipeRemoveItem()
            // ACTION_DOWN触摸到targetView就能开始拖动，
            // withLongPress = true表示继续启用长按itemView拖动。
            startDragView(withLongPress = true) { it.targetView }
            // 拖动开始时放大itemView
            onSelected {
                it.itemView.scaleX = 1.1f
                it.itemView.scaleY = 1.1f
            }
            // 拖动结束时恢复itemView
            clearView {
                it.itemView.scaleX = 1.0f
                it.itemView.scaleY = 1.0f
            }
        }
    }

    private val RecyclerView.ViewHolder.targetView: View
        get() {
            this as BindingHolder<*>
            return when (val binding = binding) {
                is ItemTextType1Binding -> binding.targetView
                is ItemTextType2Binding -> binding.targetView
                else -> throw IllegalArgumentException()
            }
        }

    private fun createView(isHeader: Boolean) = AppCompatTextView(this).apply {
        gravity = Gravity.CENTER
        text = if (isHeader) "Header" else "Footer"
        layoutParams(matchParent, 100.dp)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, 18.dp.toFloat())
        setBackgroundColor(if (isHeader) 0xFF92C3FF.toInt() else 0xFF958CFF.toInt())
    }
}