package com.example.jarvis.training

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log

class JarvisTrainingSyncService : Service() {

    companion object {
        const val ACTION_BIND_TRAINER = "com.jarvis.ai.training.BIND_TRAINER"
        private const val TAG = "JarvisTrainingSyncService"
    }

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): JarvisTrainingSyncService = this@JarvisTrainingSyncService

        fun getActiveVersion(): Int = TrainingEngine.activeBundle.value.version

        fun isJarvisActive(): Boolean = true

        fun applyBundle(bundleJson: String): Boolean {
            return try {
                val bundle = TrainingBundle.fromJsonString(bundleJson)
                TrainingEngine.applyNewTrainingBundle(bundle, persist = true)
            } catch (e: Exception) {
                Log.e(TAG, "Error applying bundle via binder", e)
                false
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        TrainingEngine.initialize(applicationContext)
        Log.d(TAG, "JarvisTrainingSyncService created.")
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "Trainer bound to JarvisTrainingSyncService.")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "Trainer unbound from JarvisTrainingSyncService.")
        return super.onUnbind(intent)
    }
}
