package com.xiaocydx.sample.transition

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.doOnSimpleItemClick
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.R
import com.xiaocydx.sample.databinding.ActivityEnterTransitionBinding
import com.xiaocydx.sample.databinding.ItemButtonBinding
import com.xiaocydx.sample.dp

/**
 * 对页面导航场景而言，Fragment过渡动画卡顿的主要原因是动画运行期间，
 * 消息耗时较长（例如doFrame）或者消息堆积较多，导致动画进度跨度较大，
 * 动画运行期间，列表数据加载完成，[RecyclerView]申请下一帧重新布局，
 * 这是`doFrame`消息耗时较长的常见场景。
 *
 * 1. [JankFragment]模拟加载列表数据的场景，复现Fragment过渡动画卡顿问题。
 *
 * 2. [PrepareFragment]尝试在加载列表数据期间，预创建[ViewHolder]以解决卡顿问题，但结果是失败的，
 * 原因是预创建[ViewHolder]虽然能解决创建View的耗时问题，但解决不了[RecyclerView]布局本身的耗时，
 * 当在一帧内填充大量的View时，`onBindViewHolder()`、`measureChild()`、`layoutChild()`等等函数，
 * 其执行时长按View的填充个数累积起来，就是耗时较长的`doFrame`消息，导致Fragment过渡动画卡顿。
 *
 * 3. [WaitEndFragment]推迟Fragment过渡动画，推迟时间达到，开始Fragment过渡动画，
 * 动画运行期间，列表数据加载完成，等待动画结束再申请重新布局，避免造成动画卡顿。
 *
 * 4. [NotWaitEndFragment]推迟Fragment过渡动画，推迟时间未到达，列表数据加载完成，申请重新布局，
 * 并开始Fragment过渡动画，此时的交互体验接近Activity的窗口动画，即看到Fragment页面就有列表内容，
 * 而不是先显示Loading，再看到列表内容。
 *
 * @author xcc
 * @date 2023/5/21
 */
class EnterTransitionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
    }

    private fun contentView() = ActivityEnterTransitionBinding
        .inflate(layoutInflater).apply {
            rvAction
                .linear(HORIZONTAL)
                .divider(10.dp, 10.dp) {
                    edge(Edge.all())
                }
                .adapter(bindingAdapter(
                    uniqueId = TransitionAction::ordinal,
                    inflate = ItemButtonBinding::inflate
                ) {
                    submitList(TransitionAction.values().toList())
                    doOnSimpleItemClick(::performTransitionAction)
                    onBindView { root.text = it.text }
                })
        }.root

    private fun performTransitionAction(action: TransitionAction) {
        when (action) {
            TransitionAction.JANK -> addFragment(JankFragment())
            TransitionAction.PREPARE -> addFragment(PrepareFragment())
            TransitionAction.WAIT_END -> addFragment(WaitEndFragment())
            TransitionAction.NOT_WAIT_END -> addFragment(NotWaitEndFragment())
        }
    }

    private fun addFragment(fragment: TransitionFragment) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            addToBackStack(null)
            add(R.id.container, fragment, fragment.javaClass.canonicalName)
        }
    }

    private enum class TransitionAction(val text: String) {
        JANK("Jank"),
        PREPARE("Prepare"),
        WAIT_END("WaitEnd"),
        NOT_WAIT_END(" NotWaitEnd")
    }
}