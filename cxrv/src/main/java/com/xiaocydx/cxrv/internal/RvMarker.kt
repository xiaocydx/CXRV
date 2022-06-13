package com.xiaocydx.cxrv.internal

/**
 * 通用不可见版本标记
 */
internal const val RV_HIDE_MARKER = "999.9"

/**
 * RecyclerView通用Dsl标记
 */
@DslMarker
internal annotation class RvDslMarker

/**
 * 该注解用于表示功能处于实验性阶段
 */
@RequiresOptIn(
    message = "功能处于实验性阶段，将来改动的可能性很大",
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalFeature