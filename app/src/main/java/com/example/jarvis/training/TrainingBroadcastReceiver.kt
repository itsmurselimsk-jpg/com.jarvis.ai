package com.example.jarvis.training

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class TrainingBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRAINING_APPLIED = "com.jarvis.ai.ACTION_TRAINING_APPLIED"
        const val ACTION_TRAINING_ACKNOWLEDGED = "com.jarvis.ai.ACTION_TRAINING_ACKNOWLEDGED"
        const val EXTRA_BUNDLE_JSON = "extra_bundle_json"
        const val EXTRA_VERSION = "extra_version"
        private const val TAG = "TrainingReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TRAINING_APPLIED) {
            val json = intent.getStringExtra(EXTRA_BUNDLE_JSON)
            val version = intent.getIntExtra(EXTRA_VERSION, -1)
            Log.d(TAG, "Received ACTION_TRAINING_APPLIED for v$version")

            if (!json.isNullOrBlank()) {
                try {
                    val bundle = TrainingBundle.fromJsonString(json)
                    TrainingEngine.initialize(context)
                    val applied = TrainingEngine.applyNewTrainingBundle(bundle, persist = true)
                    Log.i(TAG, "Applied training bundle v${bundle.version} result=$applied")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed parsing training bundle from broadcast", e)
                }
            } else {
                // If JSON not in intent, trigger load from persisted storage / provider
                TrainingEngine.initialize(context)
                TrainingEngine.loadPersistedTraining()
            }
        }
    }
}
