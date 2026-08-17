package com.laumar.aninote

import android.app.Application
import com.laumar.aninote.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AniNoteApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Force Room DB init early — hides cold start behind app startup
        applicationScope.launch {
            AppDatabase.getInstance(applicationContext)
        }
    }
}
