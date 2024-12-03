package com.xiaocydx.sample.transition

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.accompanist.transition.EnterTransitionController
import com.xiaocydx.cxrv.itemclick.reduce.doOnItemClick
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.R
import com.xiaocydx.sample.common.Action
import com.xiaocydx.sample.common.initActionList
import com.xiaocydx.sample.databinding.ActionContainerBinding
import com.xiaocydx.sample.transition.EnterTransitionActivity.Companion.CUSTOM_ANIMATION
import kotlin.reflect.KClass

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
 * **注意**：[EnterTransitionController]不只是优化`Fragment.enterTransition`，
 * 将[CUSTOM_ANIMATION]设为`true`，可以验证[setCustomAnimations]的优化效果。
 *
 * @author xcc
 * @date 2023/5/21
 */
class EnterTransitionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
    }

    private fun contentView() = ActionContainerBinding
        .inflate(layoutInflater).initActionList {
            submitList(TransitionAction.entries.toList())
            doOnItemClick { performTransitionAction(it) }
        }.root

    private fun performTransitionAction(action: TransitionAction) {
        val args = TransitionFragment.createArgs(CUSTOM_ANIMATION)
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            addToBackStack(null)
            if (CUSTOM_ANIMATION) {
                setCustomAnimations()
                replace(R.id.container, action.clazz.java, args)
            } else {
                add(R.id.container, action.clazz.java, args)
            }
        }
    }

    private fun FragmentTransaction.setCustomAnimations() {
        setCustomAnimations(
            /* enter */ R.anim.slide_in,
            /* exit */ R.anim.fade_out,
            /* popEnter */ R.anim.fade_in,
            /* popExit */ R.anim.slide_out
        )
    }

    private enum class TransitionAction(val clazz: KClass<out Fragment>) : Action {
        Jank(JankFragment::class),
        Prepare(PrepareFragment::class),
        WaitEnd(WaitEndFragment::class),
        NotWaitEnd(NotWaitEndFragment::class);

        override val text = name
    }

    private companion object {
        const val CUSTOM_ANIMATION = false
    }
}