package com.example.jarvis.training

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SyncState {
    IDLE,
    PENDING,
    SYNCED,
    FAILED
}

data class SyncResult(
    val state: SyncState,
    val version: Int,
    val message: String
)

class TrainingIpcClient(private val context: Context) {

    companion object {
        private const val TAG = "TrainingIpcClient"
        private const val PROVIDER_AUTHORITY = JarvisTrainingContentProvider.AUTHORITY
    }

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _acknowledgedVersion = MutableStateFlow(1)
    val acknowledgedVersion: StateFlow<Int> = _acknowledgedVersion.asStateFlow()

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private var syncServiceBinder: JarvisTrainingSyncService.LocalBinder? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Connected to JarvisTrainingSyncService")
            if (service is JarvisTrainingSyncService.LocalBinder) {
                syncServiceBinder = service
                _isConnected.value = true
                _acknowledgedVersion.value = service.getActiveVersion()
            } else {
                _isConnected.value = true
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Disconnected from JarvisTrainingSyncService")
            syncServiceBinder = null
            _isConnected.value = false
        }
    }

    fun startServiceBinding() {
        try {
            val intent = Intent(context, JarvisTrainingSyncService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Log.w(TAG, "Could not bind to JarvisTrainingSyncService", e)
        }
        checkConnectionViaProvider()
    }

    fun stopServiceBinding() {
        try {
            if (_isConnected.value) {
                context.unbindService(serviceConnection)
                _isConnected.value = false
                syncServiceBinder = null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error unbinding", e)
        }
    }

    fun checkConnectionViaProvider(): Boolean {
        return try {
            val uri = Uri.parse("content://$PROVIDER_AUTHORITY/bundle")
            val bundle = context.contentResolver.call(uri, JarvisTrainingContentProvider.METHOD_GET_STATUS, null, null)
            val success = bundle?.getBoolean(JarvisTrainingContentProvider.KEY_SUCCESS, false) ?: false
            if (success) {
                val version = bundle?.getInt(JarvisTrainingContentProvider.KEY_VERSION, 1) ?: 1
                _acknowledgedVersion.value = version
                _isConnected.value = true
            }
            success
        } catch (e: Exception) {
            Log.d(TAG, "ContentProvider ping failed: ${e.message}")
            false
        }
    }

    fun saveAndApply(bundle: TrainingBundle): SyncResult {
        _syncState.value = SyncState.PENDING
        var appliedViaProvider = false
        var appliedViaBinder = false

        // 1. Send via ContentProvider
        try {
            val uri = Uri.parse("content://$PROVIDER_AUTHORITY/bundle")
            val args = Bundle().apply {
                putString(JarvisTrainingContentProvider.KEY_BUNDLE_JSON, bundle.toJsonString())
            }
            val result = context.contentResolver.call(
                uri,
                JarvisTrainingContentProvider.METHOD_SAVE_AND_APPLY,
                bundle.toJsonString(),
                args
            )
            appliedViaProvider = result?.getBoolean(JarvisTrainingContentProvider.KEY_SUCCESS, false) ?: false
            if (appliedViaProvider) {
                _acknowledgedVersion.value = result?.getInt(JarvisTrainingContentProvider.KEY_VERSION, bundle.version) ?: bundle.version
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying via ContentProvider", e)
        }

        // 2. Send via Bound Service Binder if available
        try {
            val binder = syncServiceBinder
            if (binder != null) {
                appliedViaBinder = binder.applyBundle(bundle.toJsonString())
                if (appliedViaBinder) {
                    _acknowledgedVersion.value = binder.getActiveVersion()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying via Service Binder", e)
        }

        // 3. Send explicit broadcast to ensure background receivers wake up
        try {
            val intent = Intent(TrainingBroadcastReceiver.ACTION_TRAINING_APPLIED).apply {
                putExtra(TrainingBroadcastReceiver.EXTRA_BUNDLE_JSON, bundle.toJsonString())
                putExtra(TrainingBroadcastReceiver.EXTRA_VERSION, bundle.version)
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Error sending broadcast", e)
        }

        val success = appliedViaProvider || appliedViaBinder
        return if (success) {
            _syncState.value = SyncState.SYNCED
            _isConnected.value = true
            SyncResult(SyncState.SYNCED, bundle.version, "Training Applied ✓ (v${bundle.version})")
        } else {
            // If neither IPC call responded synchronously (e.g. JARVIS process is cold),
            // still persist in TrainingEngine if accessible, marking as PENDING.
            TrainingEngine.initialize(context)
            val fallback = TrainingEngine.applyNewTrainingBundle(bundle, persist = true)
            if (fallback) {
                _syncState.value = SyncState.PENDING
                SyncResult(SyncState.PENDING, bundle.version, "Saved persistently (Pending JARVIS link v${bundle.version})")
            } else {
                _syncState.value = SyncState.FAILED
                SyncResult(SyncState.FAILED, bundle.version, "Failed to validate or persist training.")
            }
        }
    }
}
