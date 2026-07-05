package com.multimediaplayer

import android.app.Application
import com.multimediaplayer.data.database.AppDatabase

class MediaPlayerApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: MediaPlayerApplication
            private set
    }
}
