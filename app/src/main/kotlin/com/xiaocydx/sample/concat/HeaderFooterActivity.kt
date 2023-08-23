package com.xiaocydx.sample.concat

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.HeaderFooterRemovedChecker
import com.xiaocydx.cxrv.concat.ViewAdapter
import com.xiaocydx.cxrv.concat.addFooter
import com.xiaocydx.cxrv.concat.addHeader
import com.xiaocydx.cxrv.concat.removeFooter
import com.xiaocydx.cxrv.concat.removeHeader
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.databinding.ActivityHeaderFooterBinding
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.foo.Foo
import com.xiaocydx.sample.foo.FooListAdapter
import com.xiaocydx.sample.layoutParams
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.onClick

/**
 * HeaderFooter示例代码
 *
 * @author xcc
 * @date 2022/12/14
 */
class HeaderFooterActivity : AppCompatActivity() {
    private lateinit var header: View
    private lateinit var footer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
    }

    private fun contentView() = ActivityHeaderFooterBinding
        .inflate(layoutInflater).apply {
            // initHeader
            header = createView(isHeader = true)
            btnAddHeader.onClick { rvFoo.addHeader(header) }
            btnRemoveHeader.onClick { rvFoo.removeHeader(header)?.let { checkRemoved(it) } }

            // initFooter
            footer = createView(isHeader = false)
            btnAddFooter.onClick { rvFoo.addFooter(footer) }
            btnRemoveFooter.onClick { rvFoo.removeFooter(footer)?.let { checkRemoved(it) } }

            // initFoo
            val fooAdapter = FooListAdapter().apply {
                submitList((1..3).map(::createFoo))
            }
            // 注意：先设置fooAdapter确定内容区，再添加header和footer
            rvFoo.linear().divider(height = 5.dp).adapter(fooAdapter)

            // 对初始化阶段不添加header和footer，而是后续动态添加和移除的场景而言，
            // 先设置HeaderFooterConcatAdapter，能让首次添加有动画效果且性能更高。
            // rvFoo.adapter(HeaderFooterConcatAdapter(fooAdapter))
            rvFoo.addHeader(header)
            rvFoo.addFooter(footer)
        }.root

    /**
     * HeaderFooter的实现确保从RecyclerView的缓存中清除已移除的Header和Footer，
     * 移除Header和Footer后，用[HeaderFooterRemovedChecker]检查是否从缓存中清除，
     * 若Header和Footer还在缓存中，则抛出[IllegalStateException]异常。
     *
     * @param adapter 添加Header和Footer最终都是转换成item多类型，
     * [addHeader]和[addFooter]会将[View.hashCode]作为`viewType`。
     */
    private fun ActivityHeaderFooterBinding.checkRemoved(adapter: ViewAdapter<*>) {
        val viewType = adapter.getItemViewType()
        val checker = HeaderFooterRemovedChecker(rvFoo, viewType)
        // 由于remove动画结束后，Header和Footer才被回收进缓存，因此在动画结束后检查
        rvFoo.itemAnimator?.isRunning(checker::check) ?: run(checker::check)
    }

    private fun createView(isHeader: Boolean) = View(this).apply {
        layoutParams(matchParent, 100.dp)
        setBackgroundColor(if (isHeader) 0xFF92C3FF.toInt() else 0xFF958CFF.toInt())
    }

    private fun createFoo(id: Int): Foo = Foo(id = id.toString(), name = "Foo-$id", num = id)
}