package com.xiaocydx.sample.concat

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.HeaderFooterRemovedChecker
import com.xiaocydx.cxrv.concat.*
import com.xiaocydx.cxrv.divider.spacing
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.databinding.ActivityHeaderFooterBinding
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.foo.Foo
import com.xiaocydx.sample.foo.FooListAdapter
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.onClick
import com.xiaocydx.sample.withLayoutParams

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
        val binding = ActivityHeaderFooterBinding.inflate(layoutInflater)
        setContentView(binding.initHeader().initFooter().initFoo().root)
    }

    private fun ActivityHeaderFooterBinding.initHeader() = apply {
        header = createView(isHeader = true)
        btnAddHeader.onClick { rvFoo.addHeader(header) }
        btnRemoveHeader.onClick { rvFoo.removeHeader(header)?.let { checkRemoved(it) } }
    }

    private fun ActivityHeaderFooterBinding.initFooter() = apply {
        footer = createView(isHeader = false)
        btnAddFooter.onClick { rvFoo.addFooter(footer) }
        btnRemoveFooter.onClick { rvFoo.removeFooter(footer)?.let { checkRemoved(it) } }
    }

    private fun ActivityHeaderFooterBinding.initFoo() = apply {
        val fooAdapter = FooListAdapter().apply {
            submitList((1..3).map(::createFoo))
        }
        // 注意：先设置footer，再添加header和footer，
        // 因为要先确定内容区，才能连接header和footer。
        rvFoo.linear().spacing(height = 5.dp).adapter(fooAdapter)

        // 对于初始化时不添加header和footer，而是后续动态添加/移除的场景，
        // 先设置HeaderFooterConcatAdapter，这能让首次添加有动画效果且性能更高。
        // rvFoo.adapter(HeaderFooterConcatAdapter(fooAdapter))
        rvFoo.addHeader(header)
        rvFoo.addFooter(footer)
    }

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
        withLayoutParams(matchParent, 100.dp)
        setBackgroundColor(if (isHeader) 0xFF92C3FF.toInt() else 0xFF958CFF.toInt())
    }

    private fun createFoo(id: Int): Foo = Foo(id = id.toString(), name = "Foo-$id", num = id)
}