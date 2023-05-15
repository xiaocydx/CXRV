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

package com.xiaocydx.cxrv.viewpager2

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

/**
 * @author xcc
 * @date 2023/5/14
 */
internal class TestAdapter(var count: Int) : RecyclerView.Adapter<ViewHolder>() {
    var callback: AdapterCallback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = View(parent.context)
        view.layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        return object : ViewHolder(view) {}
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = error("")

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        callback?.onBindViewHolder(holder, position, payloads)
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        callback?.onViewAttachedToWindow(holder)
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        callback?.onViewDetachedFromWindow(holder)
    }

    override fun getItemCount(): Int = count
}

internal interface AdapterCallback {
    fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>)
    fun onViewAttachedToWindow(holder: ViewHolder)
    fun onViewDetachedFromWindow(holder: ViewHolder)
}