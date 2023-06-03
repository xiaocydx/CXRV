package com.xiaocydx.sample.transition

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.transition.Slide
import com.xiaocydx.cxrv.concat.ViewAdapter
import com.xiaocydx.cxrv.concat.withHeader
import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.grid
import com.xiaocydx.sample.databinding.ItemSlideContentBinding
import com.xiaocydx.sample.databinding.ItemSlideLoadingBinding
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.withLayoutParams
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted.Companion.Lazily
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/**
 * @author xcc
 * @date 2023/5/21
 */
abstract class SlideFragment : Fragment() {
    protected val viewModel: SlideViewModel by viewModels()
    protected val loadingAdapter = SlideLoadingAdapter()
    protected val contentAdapter = SlideContentAdapter()
    protected lateinit var recyclerView: RecyclerView
        private set

    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = Slide(Gravity.END).apply { duration = TRANSITION_DURATION }
    }

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = RecyclerView(requireContext()).apply {
        id = viewModel.rvId
        setBackgroundColor(0xFFE5E5E5.toInt())
        grid(spanCount = 4).fixedSize()
        divider(5.dp, 5.dp) { edge(Edge.all()) }
        adapter(contentAdapter.withHeader(loadingAdapter))
        withLayoutParams(matchParent, matchParent)
        recyclerView = this
    }
}

class SlideViewModel : ViewModel() {
    val rvId = ViewCompat.generateViewId()
    val state = flow {
        delay(LOADING_DURATION)
        emit(SlideState.CONTENT)
    }.stateIn(viewModelScope, Lazily, SlideState.LOADING)
}

/**
 * Fragment过渡动画时长
 */
const val TRANSITION_DURATION = 300L

/**
 * 列表数据加载时长
 */
const val LOADING_DURATION = 100L

enum class SlideState {
    LOADING, CONTENT
}

class SlideContentAdapter : RecyclerView.Adapter<SlideContentAdapter.Holder>() {
    private var count = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = ItemSlideContentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.binding.root.text = position.toString()
    }

    override fun getItemCount(): Int = count

    fun insertItems() {
        count = 100
        notifyItemRangeInserted(0, count)
    }

    class Holder(val binding: ItemSlideContentBinding) : ViewHolder(binding.root)
}

class SlideLoadingAdapter : ViewAdapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemSlideLoadingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return object : ViewHolder(view.root) {}
    }

    override fun getItemViewType(): Int = javaClass.hashCode()

    fun showLoading() = updateItem(show = true, NeedAnim.NOT_ALL)

    fun hideLoading() = updateItem(show = false, NeedAnim.NOT_ALL)
}