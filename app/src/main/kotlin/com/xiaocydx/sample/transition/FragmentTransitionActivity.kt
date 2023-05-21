package com.xiaocydx.sample.transition

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.doOnSimpleItemClick
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.R
import com.xiaocydx.sample.databinding.ActivityFragmentTransitionBinding
import com.xiaocydx.sample.databinding.ItemButtonBinding
import com.xiaocydx.sample.dp

/**
 * @author xcc
 * @date 2023/5/21
 */
class FragmentTransitionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityFragmentTransitionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.rvAction
            .linear(HORIZONTAL)
            .divider(10.dp, 10.dp) {
                edge(Edge.all())
            }
            .adapter(bindingAdapter(
                uniqueId = TransitionAction::ordinal,
                inflate = ItemButtonBinding::inflate
            ) {
                initTransitionAction()
                onBindView { root.text = it.text }
            })
    }

    private fun ListAdapter<TransitionAction, *>.initTransitionAction() {
        doOnSimpleItemClick { item ->
            when (item) {
                TransitionAction.JANK -> addFragment(JankSlideFragment())
                TransitionAction.TIMEOUT -> addFragment(TimeoutSlideFragment())
                TransitionAction.NOT_TIMEOUT -> addFragment(NotTimeoutSlideFragment())
                TransitionAction.PREPARE_SCRAP -> addFragment(PrepareScrapSlideFragment())
            }
        }
        submitList(TransitionAction.values().toList())
    }

    private fun addFragment(fragment: SlideFragment) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            addToBackStack(null)
            add(R.id.container, fragment, fragment.javaClass.canonicalName)
        }
    }

    private enum class TransitionAction(val text: String) {
        JANK("Jank"),
        TIMEOUT("Timeout"),
        NOT_TIMEOUT(" NotTimeout"),
        PREPARE_SCRAP("PrepareScrap")
    }
}