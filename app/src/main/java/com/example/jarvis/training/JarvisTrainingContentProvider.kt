package com.example.jarvis.training

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.util.Log

class JarvisTrainingContentProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.jarvis.ai.training.provider"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/bundle")
        const val METHOD_SAVE_AND_APPLY = "save_and_apply"
        const val METHOD_GET_BUNDLE = "get_bundle"
        const val METHOD_GET_STATUS = "get_status"
        const val METHOD_PING = "ping"

        const val KEY_BUNDLE_JSON = "bundle_json"
        const val KEY_VERSION = "version"
        const val KEY_SUCCESS = "success"
        const val KEY_STATUS = "status"
        const val KEY_ITEM_COUNT = "item_count"
    }

    override fun onCreate(): Boolean {
        context?.let { ctx ->
            TrainingEngine.initialize(ctx)
        }
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val result = Bundle()
        val ctx = context ?: return result

        when (method) {
            METHOD_SAVE_AND_APPLY -> {
                val json = arg ?: extras?.getString(KEY_BUNDLE_JSON)
                if (json.isNullOrBlank()) {
                    result.putBoolean(KEY_SUCCESS, false)
                    result.putString("error", "Empty training bundle JSON")
                    return result
                }

                try {
                    val bundle = TrainingBundle.fromJsonString(json)
                    val applied = TrainingEngine.applyNewTrainingBundle(bundle, persist = true)
                    result.putBoolean(KEY_SUCCESS, applied)
                    result.putInt(KEY_VERSION, bundle.version)
                    result.putInt(KEY_ITEM_COUNT, bundle.items.size)
                    Log.d("TrainingContentProvider", "save_and_apply v${bundle.version} result=$applied")
                } catch (e: Exception) {
                    result.putBoolean(KEY_SUCCESS, false)
                    result.putString("error", e.message)
                    Log.e("TrainingContentProvider", "Error saving training bundle", e)
                }
            }

            METHOD_GET_BUNDLE -> {
                val bundle = TrainingEngine.activeBundle.value
                result.putBoolean(KEY_SUCCESS, true)
                result.putString(KEY_BUNDLE_JSON, bundle.toJsonString())
                result.putInt(KEY_VERSION, bundle.version)
            }

            METHOD_GET_STATUS -> {
                val bundle = TrainingEngine.activeBundle.value
                result.putBoolean(KEY_SUCCESS, true)
                result.putString(KEY_STATUS, "ACTIVE")
                result.putInt(KEY_VERSION, bundle.version)
                result.putInt(KEY_ITEM_COUNT, bundle.items.size)
            }

            METHOD_PING -> {
                result.putBoolean(KEY_SUCCESS, true)
                result.putString("response", "PONG_JARVIS_ACTIVE")
            }
        }

        return result
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val cursor = MatrixCursor(arrayOf("version", "items_count", "rules_count", "bundle_json"))
        val bundle = TrainingEngine.activeBundle.value
        cursor.addRow(arrayOf(bundle.version, bundle.items.size, bundle.rules.size, bundle.toJsonString()))
        return cursor
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.dir/vnd.com.jarvis.ai.training"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
