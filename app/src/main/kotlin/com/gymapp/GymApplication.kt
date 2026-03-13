package com.gymapp

import android.app.Application
import dagger.hilt.EntryPoint
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class GymApplication : Application() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SeederEntryPoint {
        fun debugDataSeeder(): DebugDataSeeder
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            EntryPointAccessors
                .fromApplication(this@GymApplication, SeederEntryPoint::class.java)
                .debugDataSeeder()
                .seedIfEmpty()
        }
    }
}
