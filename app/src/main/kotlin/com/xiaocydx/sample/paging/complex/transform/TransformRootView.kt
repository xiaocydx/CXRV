package com.xiaocydx.sample.paging.complex.transform

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.transition.Transition
import com.google.android.material.transition.MaterialContainerTransform
import com.xiaocydx.sample.R
import com.xiaocydx.sample.layoutParams
import com.xiaocydx.sample.matchParent

/**
 * @author xcc
 * @date 2023/8/1
 */
internal class TransformRootView(context: Context) : FrameLayout(context) {
    private var senderView: View? = null

    init {
        layoutParams(matchParent, matchParent)
        id = R.id.transform_root_view_fragment_container
    }

    fun setSenderView(view: View?) {
        senderView = view
    }

    fun createTransition(fragment: Fragment, transform: MaterialContainerTransform): Transition {
        return TransformTransition(fragment, ::senderView, transform)
    }
}