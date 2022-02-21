package com.xiaocydx.sample.viewmodel

import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

@MainThread
inline fun <reified VM : ViewModel> Fragment.viewModels(
    noinline key: () -> String,
    noinline ownerProducer: () -> ViewModelStoreOwner = { this },
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> = createViewModelLazy(
    viewModelClass = VM::class.java,
    viewModelKey = key,
    storeProducer = { ownerProducer().viewModelStore },
    factoryProducer = factoryProducer
)

@MainThread
inline fun <reified VM : ViewModel> Fragment.activityViewModels(
    noinline key: () -> String,
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> = createViewModelLazy(
    viewModelClass = VM::class.java,
    viewModelKey = key,
    storeProducer = { requireActivity().viewModelStore },
    factoryProducer = factoryProducer ?: { requireActivity().defaultViewModelProviderFactory }
)

@MainThread fun <VM : ViewModel> Fragment.createViewModelLazy(
    viewModelClass: Class<VM>,
    viewModelKey: () -> String,
    storeProducer: () -> ViewModelStore,
    factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> {
    val factoryPromise = factoryProducer ?: {
        defaultViewModelProviderFactory
    }
    return ViewModelLazy(viewModelClass, viewModelKey, storeProducer, factoryPromise)
}