package com.xiaocydx.sample.concat

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.HeaderFooterRemovedChecker
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.accompanist.view.dp
import com.xiaocydx.accompanist.view.layoutParams
import com.xiaocydx.accompanist.view.matchParent
import com.xiaocydx.accompanist.view.wrapContent
import com.xiaocydx.cxrv.concat.ViewAdapter
import com.xiaocydx.cxrv.concat.addFooter
import com.xiaocydx.cxrv.concat.addHeader
import com.xiaocydx.cxrv.concat.removeFooter
import com.xiaocydx.cxrv.concat.removeHeader
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.reduce.doOnItemClick
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.doOnAttach
import com.xiaocydx.cxrv.list.grid
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.common.Action
import com.xiaocydx.sample.common.Foo
import com.xiaocydx.sample.common.FooListAdapter
import com.xiaocydx.sample.common.actionList
import com.xiaocydx.sample.concat.HeaderFooterActivity.HeaderFooterAction.AddFooter
import com.xiaocydx.sample.concat.HeaderFooterActivity.HeaderFooterAction.AddHeader
import com.xiaocydx.sample.concat.HeaderFooterActivity.HeaderFooterAction.RemoveFooter
import com.xiaocydx.sample.concat.HeaderFooterActivity.HeaderFooterAction.RemoveHeader
import com.xiaocydx.sample.databinding.ActionContentBinding

/**
 * HeaderFooter示例代码
 *
 * @author xcc
 * @date 2022/12/14
 */
class HeaderFooterActivity : AppCompatActivity() {
    private lateinit var header: View
    private lateinit var footer: View
    private lateinit var rvContent: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
    }

    private fun contentView() = ActionContentBinding
        .inflate(layoutInflater).apply {
            // 注意：先设置fooAdapter确定内容区，再添加header和footer，
            // 若在初始化时不添加header和footer，而是后续根据情况动态添加header和footer，
            // 先设置Concat.content(fooAdapter).concat()，后续添加有动画效果且性能更高。
            // rvFoo.adapter(Concat.content(fooAdapter).concat())
            val fooAdapter = createFooAdapter()
            rvContent.linear().divider(height = 5.dp).adapter(fooAdapter)
            this@HeaderFooterActivity.rvContent = rvContent

            header = createView(isHeader = true)
            footer = createView(isHeader = false)
            rvContent.addHeader(header)
            rvContent.addFooter(footer)

            rvAction.actionList {
                submitList(HeaderFooterAction.entries.toList())
                doOnItemClick { performHeaderFooterAction(it) }
                doOnAttach { rv -> rv.grid(spanCount = 2) }
                onCreateView { root.layoutParams(matchParent, wrapContent) }
            }
        }.root

    private fun performHeaderFooterAction(action: HeaderFooterAction) {
        when (action) {
            AddHeader -> rvContent.addHeader(header)
            RemoveHeader -> rvContent.removeHeader(header)?.let(::checkRemoved)
            AddFooter -> rvContent.addFooter(footer)
            RemoveFooter -> rvContent.removeFooter(footer)?.let(::checkRemoved)
        }
    }

    private fun createFooAdapter() = FooListAdapter().apply {
        submitList((1..3).map { id ->
            Foo(id = id.toString(), name = "Foo-$id", num = id)
        })
    }

    private fun createView(isHeader: Boolean) = AppCompatTextView(this).apply {
        gravity = Gravity.CENTER
        text = if (isHeader) "Header" else "Footer"
        layoutParams(matchParent, 100.dp)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, 18.dp.toFloat())
        setBackgroundColor(if (isHeader) 0xFF92C3FF.toInt() else 0xFF958CFF.toInt())
    }

    private fun checkRemoved(viewAdapter: ViewAdapter<*>) {
        HeaderFooterRemovedChecker(rvContent, viewAdapter).check()
    }

    private enum class HeaderFooterAction(override val text: String) : Action {
        AddHeader("添加Header"),
        RemoveHeader("移除Header"),
        AddFooter("添加Footer"),
        RemoveFooter("移除Footer")
    }
}