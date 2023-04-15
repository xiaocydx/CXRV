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

package com.xiaocydx.cxrv.list

import androidx.recyclerview.widget.RecyclerView.ViewHolder

/**
 * ViewHolder绑定监听
 *
 * @author xcc
 * @date 2021/11/26
 */
internal fun interface ViewHolderListener<VH : ViewHolder> {

    fun onBindViewHolder(holder: VH, position: Int, payloads: List<Any>)
}