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
    val stats: StateFlow<LaunchStats> get() = store.stats
    val appsLoad: StateFlow<AppsLoad> get() = store.appsLoad
    val reads: StateFlow<Int> get() = store.reads

    init {
        scope.launch { store.changes.collect { icons.evict(it.packages, it.user) } }
    }

    fun resume() = store.resume()

    suspend fun edit(undoable: Boolean = true, change: (LayoutFile) -> LayoutFile): Result<HomeStore.Edited> = store.edit(undoable, change)

    suspend fun undo(): Result<LayoutFile> = store.undo()

    suspend fun restore(): Result<LayoutFile> = store.restore()
}
