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

package com.xiaocydx.accompanist.insets

import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.list.enableBoundCheckCompat
import com.xiaocydx.insets.handleGestureNavBarEdgeToEdgeOnApply

fun RecyclerView.enableGestureNavBarEdgeToEdge() {
    layoutManager?.enableBoundCheckCompat()
    handleGestureNavBarEdgeToEdgeOnApply()
}