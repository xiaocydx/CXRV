package com.xiaocydx.sample.viewmodel

import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@MainThread
inline fun <reified VM : ViewModel> ComponentActivity.viewModels(
    key: String,
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> {
    val factoryPromise = factoryProducer ?: {
        defaultViewModelProviderFactory
    }

    return ViewModelLazy(key, VM::class.java, { viewModelStore }, factoryPromise)
}