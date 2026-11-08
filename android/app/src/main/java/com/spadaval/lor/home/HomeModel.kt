package com.spadaval.lor.home

import kotlinx.coroutines.flow.StateFlow

/** Home's view of [HomeStore] for one activity: the store's app list, layout and icons. */
class HomeModel(private val store: HomeStore) {
    val apps: StateFlow<List<HomeApp>> get() = store.apps
    val layout: StateFlow<LayoutState> get() = store.layout
    val stats: StateFlow<LaunchStats> get() = store.stats
    val appsLoad: StateFlow<AppsLoad> get() = store.appsLoad
    val reads: StateFlow<Int> get() = store.reads
    val search: StateFlow<SearchSources> get() = store.search
    val icons: Icons get() = store.icons

    fun resume() = store.resume()

    suspend fun edit(undoable: Boolean = true, change: (LayoutFile) -> LayoutFile): Result<HomeStore.Edited> = store.edit(undoable, change)

    suspend fun undo(): Result<LayoutFile> = store.undo()

    suspend fun restore(): Result<LayoutFile> = store.restore()
}
