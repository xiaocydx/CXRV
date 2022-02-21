package com.xiaocydx.sample.viewmodel

import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@MainThread
inline fun <reified VM : ViewModel> ComponentActivity.viewModels(
    noinline key: () -> String,
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> = ViewModelLazy(
    viewModelClass = VM::class.java,
    viewModelKey = key,
    storeProducer = { viewModelStore },
    factoryProducer = factoryProducer ?: { defaultViewModelProviderFactory }
)