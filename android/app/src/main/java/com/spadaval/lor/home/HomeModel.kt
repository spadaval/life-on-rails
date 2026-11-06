package com.spadaval.lor.home

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Home's view of [HomeStore] for one activity: the store's app list and layout, and this
 * activity's icon cache, which drops a package's icons when the store says it changed.
 */
class HomeModel(private val store: HomeStore, scope: CoroutineScope, val icons: Icons) {
    val apps: StateFlow<List<HomeApp>> get() = store.apps
    val layout: StateFlow<LayoutState> get() = store.layout

    init {
        scope.launch { store.changes.collect { icons.evict(it.packages, it.user) } }
    }

    fun resume() = store.resume()

    suspend fun edit(change: (LayoutFile) -> LayoutFile): Result<LayoutFile> = store.edit(change)
}
