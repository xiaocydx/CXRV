package com.xiaocydx.sample.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore

class ViewModelLazy<VM : ViewModel>(
    private val viewModelClass: Class<VM>,
    viewModelKey: () -> String,
    storeProducer: () -> ViewModelStore,
    factoryProducer: () -> ViewModelProvider.Factory
) : Lazy<VM> {
    private var cached: VM? = null
    private var viewModelKey: (() -> String)? = viewModelKey
    private var storeProducer: (() -> ViewModelStore)? = storeProducer
    private var factoryProducer: (() -> ViewModelProvider.Factory)? = factoryProducer

    override val value: VM
        get() {
            val viewModel = cached
            return if (viewModel == null) {
                val key = viewModelKey!!()
                val store = storeProducer!!()
                val factory = factoryProducer!!()
                ViewModelProvider(store, factory)
                    .get(key, viewModelClass).also {
                        viewModelKey = null
                        storeProducer = null
                        factoryProducer = null
                        cached = it
                    }
            } else {
                viewModel
            }
        }

    override fun isInitialized(): Boolean = cached != null
}