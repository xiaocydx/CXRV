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

package com.xiaocydx.cxrv.recycle.scrap

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.LayoutInflater.Factory
import android.view.LayoutInflater.Factory2
import android.view.View

/**
 * 实现逻辑参考自PhoneLayoutInflater
 *
 * [LayoutInflater]不支持多线程，因此单独构建[LayoutInflater]用于预创建，
 * `LayoutInflater.from(context)`结果的[Factory]和[Factory2]无法沿用，
 * 因为[Factory]和[Factory2]的实现不一定支持多线程。
 *
 * @author xcc
 * @date 2023/11/8
 */
internal class PrepareLayoutInflater(context: Context) : LayoutInflater(context) {

    override fun cloneInContext(newContext: Context): LayoutInflater {
        return PrepareLayoutInflater(newContext)
    }

    override fun onCreateView(name: String?, attrs: AttributeSet?): View {
        for (prefix in classPrefixList) {
            try {
                val view = createView(name, prefix, attrs)
                if (view != null) return view
            } catch (e: ClassNotFoundException) {
            }
        }
        return super.onCreateView(name, attrs)
    }

    companion object {
        private val classPrefixList = arrayOf("android.widget.", "android.webkit.", "android.app.")
    }
}