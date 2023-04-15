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

package com.xiaocydx.cxrv.internal

import android.os.Trace
import android.util.Log

internal const val DEBUG_TAG = "CXRV"
internal const val DEBUG_ENABLED = false
internal const val DEBUG_LOG = DEBUG_ENABLED
internal const val DEBUG_TRACE = DEBUG_ENABLED

internal inline fun log(message: () -> String) {
    if (DEBUG_LOG) Log.d(DEBUG_TAG, message())
}

internal inline fun trace(name: String, action: () -> Unit) {
    trace(lazyName = { name }, action)
}

internal inline fun trace(lazyName: () -> String, action: () -> Unit) {
    if (DEBUG_TRACE) Trace.beginSection(lazyName())
    action()
    if (DEBUG_TRACE) Trace.endSection()
}