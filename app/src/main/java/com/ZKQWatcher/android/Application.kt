package com.ZKQWatcher.android

import android.app.Application
import java.util.concurrent.Executors

/**
 * Gives easy access to a single‑thread executor & global context.
 * (Useful if you later need background work outside the Service.)
 */
class Application : Application() {

    companion object {
        lateinit var instance: Application
            private set

        // lightweight single thread – avoid ANRs when doing small tasks
        val ioExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
