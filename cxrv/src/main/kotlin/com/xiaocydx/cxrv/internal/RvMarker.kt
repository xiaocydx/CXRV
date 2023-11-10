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

import kotlin.annotation.AnnotationTarget.*

/**
 * 通用不可见版本标记
 */
internal const val RV_HIDE_MARKER = "999.9"

/**
 * RecyclerView通用Dsl标记
 */
@DslMarker
internal annotation class RvDslMarker

@RequiresOptIn(
    message = "内部API，外部不该调用，不提供兼容性保证",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(value = AnnotationRetention.BINARY)
@Target(CLASS, FUNCTION, TYPEALIAS, PROPERTY)
annotation class VisibleForInternal

@RequiresOptIn(
    message = "功能处于实验性阶段，将来改动的可能性很大",
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
@Target(CLASS, FUNCTION, TYPEALIAS, PROPERTY)
annotation class ExperimentalFeature