package com.xiaocydx.cxrv.internal

import android.os.Trace
import android.util.Log

internal const val DEBUG_TAG = "CXRV"
internal const val DEBUG_LOG = false
internal const val DEBUG_TRACE = false

internal inline fun log(message: () -> String) {
    if (DEBUG_LOG) Log.d(DEBUG_TAG, message())
}

internal inline fun trace(name: String, action: () -> Unit) {
    trace(lazyName = { name }, action)
}

internal inline fun trace(lazyName: () -> String, action: () -> Unit) {
    if (DEBUG_TRACE) {
        Trace.beginSection(lazyName())
        action()
        Trace.endSection()
    }
}