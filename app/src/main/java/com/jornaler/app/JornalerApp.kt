package com.jornaler.app

import android.app.Application
import android.content.Context
import android.util.Log
import com.jornaler.app.data.EntryRepository
import com.jornaler.app.data.JornalerDatabase
import com.jornaler.app.data.ThemeStore
import androidx.glance.appwidget.updateAll
import com.jornaler.app.widget.JornalerWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

class AppGraph(context: Context) {
    private val appContext = context.applicationContext
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val repository = EntryRepository(JornalerDatabase.get(appContext).entries())
    val themes = ThemeStore(appContext)

    @OptIn(FlowPreview::class)
    fun start() {
        appScope.launch {
            repository.changes.debounce(300).collect {
                runCatching { JornalerWidget().updateAll(appContext) }
                    .onFailure { error -> Log.w(TAG, "Widget update failed", error) }
            }
        }
    }

    private companion object {
        const val TAG = "JORNaler"
    }
}

class JornalerApp : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        graph.start()
    }
}

fun jornalerGraph(context: Context): AppGraph = (context.applicationContext as JornalerApp).graph
