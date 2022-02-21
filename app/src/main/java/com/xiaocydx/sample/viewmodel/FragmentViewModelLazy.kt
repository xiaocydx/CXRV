package com.xiaocydx.sample.viewmodel

import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.*

@MainThread
inline fun <reified VM : ViewModel> Fragment.viewModels(
    key: String,
    noinline ownerProducer: () -> ViewModelStoreOwner = { this },
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> = createViewModelLazy(key, VM::class.java, { ownerProducer().viewModelStore }, factoryProducer)

@MainThread
inline fun <reified VM : ViewModel> Fragment.activityViewModels(
    key: String,
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> = createViewModelLazy(
    key, VM::class.java, { requireActivity().viewModelStore },
    factoryProducer ?: { requireActivity().defaultViewModelProviderFactory }
)

@MainThread fun <VM : ViewModel> Fragment.createViewModelLazy(
    viewModelKey: String,
    viewModelClass: Class<VM>,
    storeProducer: () -> ViewModelStore,
    factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> {
    val factoryPromise = factoryProducer ?: {
        defaultViewModelProviderFactory
    }
    return ViewModelLazy(viewModelKey, viewModelClass, storeProducer, factoryPromise)
}