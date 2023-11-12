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

package com.xiaocydx.cxrv.recycle.prepare

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.WorkerThread
import androidx.recyclerview.widget.RecyclerView
import java.lang.ref.WeakReference

/**
 * 虚假的`parent`，提供[ScrapParent]和[RecyclerView.generateLayoutParams]
 *
 * @author xcc
 * @date 2023/11/11
 */
@WorkerThread
@SuppressLint("ViewConstructor")
internal class ScrapParent(rv: RecyclerView, context: ScrapContext) : ViewGroup(context) {
    private val rvRef = WeakReference(rv)

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return rvRef.get()?.generateLayoutParams(attrs) ?: super.generateLayoutParams(attrs)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) = Unit
}

@WorkerThread
internal class ScrapContext(base: Context) : ContextWrapper(base) {
    private var inflater: LayoutInflater? = null
    private val threadId = Thread.currentThread().id

    fun setInflater(inflater: LayoutInflater) {
        assert(checkThread())
        this.inflater = inflater
    }

    fun clearInflater() {
        assert(checkThread())
        this.inflater = null
    }

    /**
     * 预创建的工作线程执行期间，[inflater]不为空，非[threadId]线程访问[inflater]，
     * 返回`super.getSystemService(name)`，确保只在[threadId]线程访问[inflater]。
     */
    override fun getSystemService(name: String): Any {
        if (name == LAYOUT_INFLATER_SERVICE && checkThread()) {
            inflater?.let { return it }
        }
        return super.getSystemService(name)
    }

    private fun checkThread() = Thread.currentThread().id == threadId
}