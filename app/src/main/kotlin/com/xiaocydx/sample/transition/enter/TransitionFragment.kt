package com.xiaocydx.sample.transition.enter

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Slide
import com.xiaocydx.cxrv.concat.Concat
import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.grid
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.layoutParams
import com.xiaocydx.sample.matchParent

/**
 * @author xcc
 * @date 2023/5/21
 */
abstract class TransitionFragment : Fragment() {
    protected val viewModel: TransitionViewModel by viewModels()
    protected val loadingAdapter = LoadingAdapter()
    protected val contentAdapter = ContentAdapter()
    protected var recyclerView: RecyclerView? = null
        private set

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = RecyclerView(requireContext()).apply {
        id = viewModel.rvId
        recyclerView = this
        setBackgroundColor(0xFFE5E5E5.toInt())
        layoutParams(matchParent, matchParent)
        grid(spanCount = 4).fixedSize()
        divider(5.dp, 5.dp) { edge(Edge.all()) }
        adapter(Concat.header(loadingAdapter).content(contentAdapter).concat())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (arguments?.getBoolean(CUSTOM_ANIMATION) == true) return
        enterTransition = Slide(Gravity.RIGHT).apply {
            addTarget(view)
            duration = 300L
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView?.adapter = null
        recyclerView = null
    }

    companion object {
        private const val CUSTOM_ANIMATION = "CUSTOM_ANIMATION"

        fun createArgs(customAnimation: Boolean) =
                Bundle(1).apply { putBoolean(CUSTOM_ANIMATION, customAnimation) }
    }
}

