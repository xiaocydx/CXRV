package com.xiaocydx.sample.paging.complex

import androidx.fragment.app.Fragment
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

/**
 * @author xcc
 * @date 2023/9/6
 */
class EnterTransitionListener(
    private var fragment: Fragment?,
    private val requestManager: RequestManager
) : RequestListener<Any> {

    fun postpone() {
        fragment?.postponeEnterTransition()
        requestManager.addDefaultRequestListener(this)
    }

    override fun onResourceReady(
        resource: Any?, model: Any?,
        target: Target<Any>?, dataSource: DataSource?, isFirstResource: Boolean
    ): Boolean = startPostponedEnterTransition()

    override fun onLoadFailed(
        e: GlideException?, model: Any?,
        target: Target<Any>?, isFirstResource: Boolean
    ): Boolean = startPostponedEnterTransition()

    private fun startPostponedEnterTransition(): Boolean {
        // RequestManager没提供removeDefaultRequestListener()，做置空处理
        fragment?.startPostponedEnterTransition()
        fragment = null
        return false
    }
}