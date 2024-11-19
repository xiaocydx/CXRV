/*
 * Copyright 2022 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaocydx.accompanist.transition.transform

import android.view.View
import androidx.fragment.app.Fragment
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.target.ViewTarget

/**
 * 推迟[receiver]的进入过渡动画
 *
 * @param receiver Receiver页面
 * @param canStartEnterTransition 返回`true`，开始进入过渡动画
 */
@Suppress("UnusedReceiverParameter")
fun Transform.postponeEnterTransition(
    receiver: Fragment,
    requestManager: RequestManager,
    transitionProvider: TransitionProvider,
    canStartEnterTransition: (target: View) -> Boolean
) {
    TransformPostpone(
        receiver, requestManager,
        transitionProvider,
        canStartEnterTransition
    ).postponeEnterTransition()
}

private class TransformPostpone(
    fragment: Fragment,
    private val requestManager: RequestManager,
    private val transitionProvider: TransitionProvider,
    private val canStartEnterTransition: (target: View) -> Boolean
) {
    private var fragment: Fragment? = fragment

    fun postponeEnterTransition() {
        fragment?.enterTransition = transitionProvider.create(true)
        fragment?.returnTransition = transitionProvider.create(false)
        fragment?.postponeEnterTransition()
        requestManager.addDefaultRequestListener(object : RequestCompleteListener() {
            override fun onComplete(target: Target<Any>?) {
                if (fragment == null || target !is ViewTarget<*, *>) return
                if (!canStartEnterTransition(target.view)) return
                startPostponedEnterTransition()
            }
        })
    }

    private fun startPostponedEnterTransition() {
        fragment?.startPostponedEnterTransition()
        fragment = null
    }

    private abstract class RequestCompleteListener : RequestListener<Any> {
        protected abstract fun onComplete(target: Target<Any>?)

        override fun onLoadFailed(
            e: GlideException?, model: Any?,
            target: Target<Any>?, isFirstResource: Boolean
        ): Boolean {
            onComplete(target)
            return false
        }

        override fun onResourceReady(
            resource: Any?, model: Any?, target:
            Target<Any>?, dataSource: DataSource?,
            isFirstResource: Boolean
        ): Boolean {
            onComplete(target)
            return false
        }
    }
}