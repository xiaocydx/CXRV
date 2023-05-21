package com.xiaocydx.sample.transition

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.ViewCompat
import androidx.core.view.isInvisible
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
import com.xiaocydx.cxrv.list.grid
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.withLayoutParams
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
        enterTransition = Slide().apply {
            slideEdge = Gravity.END
            duration = TRANSITION_DURATION
        }
    }

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = RecyclerView(requireContext()).apply {
        id = viewModel.rvId
        setBackgroundColor(0xFFE5E5E5.toInt())
        grid(spanCount = 4)
        divider(5.dp, 5.dp) { edge(Edge.all()) }
        adapter(contentAdapter.withHeader(loadingAdapter))
        withLayoutParams(matchParent, matchParent)
        recyclerView = this
    }
}

class SlideViewModel : ViewModel() {
    val rvId = ViewCompat.generateViewId()
    val state = flow {
        kotlinx.coroutines.delay(CONTENT_DURATION)
        emit(SlideState.CONTENT)
    }.stateIn(viewModelScope, Lazily, SlideState.LOADING)
}

const val TRANSITION_DURATION = 500L
const val CONTENT_DURATION = 100L

enum class SlideState {
    LOADING, CONTENT
}

class SlideContentAdapter : RecyclerView.Adapter<ViewHolder>() {
    private var count = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = AppCompatTextView(parent.context).apply {
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF8AA1D5.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
            withLayoutParams(matchParent, 50.dp)
        }
        view.isInvisible = true
        return object : ViewHolder(view) {}
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder.itemView as TextView).text = position.toString()
    }

    override fun getItemCount(): Int = count

    fun insertItems() {
        count = 100
        notifyItemRangeInserted(0, count)
    }
}

class SlideLoadingAdapter : ViewAdapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = FrameLayout(parent.context).apply { withLayoutParams(matchParent, matchParent) }
        val processBar = ProgressBar(parent.context).apply { layoutParams = LayoutParams(80.dp, 80.dp, Gravity.CENTER) }
        layout.addView(processBar)
        return object : ViewHolder(layout) {}
    }

    override fun getItemViewType(): Int = javaClass.hashCode()

    fun showLoading() = updateItem(show = true, NeedAnim.NOT_ALL)

    fun hideLoading() = updateItem(show = false, NeedAnim.NOT_ALL)
}