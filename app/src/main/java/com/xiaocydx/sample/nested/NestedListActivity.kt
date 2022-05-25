package com.xiaocydx.sample.nested

import android.os.Bundle
import android.view.View
import android.view.View.OVER_SCROLL_NEVER
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.extension.fixedSize
import com.xiaocydx.cxrv.extension.linear
import com.xiaocydx.cxrv.list.submitList

/**
 * 嵌套列表示例代码
 *
 * @author xcc
 * @date 2022/4/6
 */
class NestedListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
    }

    private fun contentView(): View = RecyclerView(this).apply {
        linear().fixedSize()
        overScrollMode = OVER_SCROLL_NEVER
        layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        adapter = VerticalAdapter().apply {
            submitList(getItems(15))
        }
    }

    private fun getItems(
        size: Int
    ): List<VerticalItem> = (1..size).map {
        VerticalItem(
            id = "Vertical-$it",
            title = "List-$it",
            data = (1..size).map { value ->
                HorizontalItem(id = "Horizontal-$value", title = "$it-${value}")
            }
        )
    }
}