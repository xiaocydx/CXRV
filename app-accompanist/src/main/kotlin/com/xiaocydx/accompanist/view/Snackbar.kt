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

@file:Suppress("SpellCheckingInspection")

package com.xiaocydx.accompanist.view

import android.app.Activity
import android.view.View
import androidx.annotation.CheckResult
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

@CheckResult
fun View.snackbar() = Snackbar.make(
    this, "", Snackbar.LENGTH_SHORT
).setTextMaxLines(Int.MAX_VALUE).dismissAction()

@CheckResult
fun Snackbar.short() = setDuration(Snackbar.LENGTH_SHORT)

@CheckResult
fun Snackbar.long() = setDuration(Snackbar.LENGTH_LONG)

@CheckResult
fun Snackbar.indefinite() = setDuration(Snackbar.LENGTH_INDEFINITE)

@CheckResult
fun Snackbar.dismissAction(text: CharSequence = "dismiss") = setAction(text) { dismiss() }

@CheckResult
fun Activity.snackbar() = window.findViewById<View>(android.R.id.content).snackbar()

@CheckResult
fun Fragment.snackbar() = requireActivity().snackbar()