package com.example.jarvis.training

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class BehavioralContext(
    val matchedItem: TrainingItem? = null,
    val preferredTool: String? = null,
    val requiresConfirmation: Boolean = false,
    val preferredTone: String? = null,
    val relevantRules: List<String> = emptyList(),
    val goodExamples: List<Pair<String, String>> = emptyList(),
    val badExamples: List<Pair<String, String>> = emptyList(),
    val promptGuidance: String = ""
)

object TrainingEngine {
    private const val TAG = "TrainingEngine"
    private const val PREFS_NAME = "jarvis_training_active_prefs"
    private const val KEY_BUNDLE_JSON = "active_training_bundle_json"
    private const val KEY_ACK_VERSION = "acknowledged_version"

    private val _activeBundle = MutableStateFlow(TrainingBundle(version = 1))
    val activeBundle: StateFlow<TrainingBundle> = _activeBundle.asStateFlow()

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
        loadPersistedTraining()
    }

    @Synchronized
    fun loadPersistedTraining() {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_BUNDLE_JSON, null)
        if (!jsonStr.isNullOrBlank()) {
            try {
                val bundle = TrainingBundle.fromJsonString(jsonStr)
                _activeBundle.value = bundle
                Log.d(TAG, "Loaded active training bundle v${bundle.version} with ${bundle.items.size} items, ${bundle.rules.size} rules.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse persisted training bundle", e)
            }
        }
    }

    @Synchronized
    fun applyNewTrainingBundle(bundle: TrainingBundle, persist: Boolean = true): Boolean {
        val validation = TrainingValidation.validateBundle(bundle)
        if (!validation.isValid) {
            Log.w(TAG, "Rejected training bundle v${bundle.version}: ${validation.errorMessage}")
            return false
        }

        // Apply immediately in memory
        _activeBundle.value = bundle
        appContext?.let { ctx ->
            com.example.jarvis.voice.JarvisVoiceEngine.setVoice(bundle.voiceConfig, ctx)
        }
        Log.i(TAG, "Applied training bundle v${bundle.version} into live AgentLoop & VoiceEngine memory!")

        if (persist) {
            val ctx = appContext
            if (ctx != null) {
                val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putString(KEY_BUNDLE_JSON, bundle.toJsonString())
                    .putInt(KEY_ACK_VERSION, bundle.version)
                    .apply()

                // Broadcast acknowledgment to notify Trainer app
                try {
                    val ackIntent = Intent("com.jarvis.ai.ACTION_TRAINING_ACKNOWLEDGED").apply {
                        putExtra("version", bundle.version)
                        putExtra("timestamp", System.currentTimeMillis())
                        setPackage(ctx.packageName)
                    }
                    ctx.sendBroadcast(ackIntent)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed sending ack broadcast", e)
                }
            }
        }
        return true
    }

    fun getAcknowledgedVersion(): Int {
        val ctx = appContext ?: return _activeBundle.value.version
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_ACK_VERSION, _activeBundle.value.version)
    }

    /**
     * Pattern matching: Match relevant training rules, tone, and tool expectations
     * based on user input, intent, and language.
     * Crucially: This implements Pattern Learning, NOT fixed hardcoded replies!
     */
    fun matchRelevantTraining(
        input: String,
        intent: String,
        langStyle: String
    ): BehavioralContext {
        val bundle = _activeBundle.value
        val enabledItems = bundle.items.filter { it.enabled }
        val enabledRules = bundle.rules.filter { it.enabled }
        val trimmedInput = input.trim().lowercase(Locale.ROOT)

        var matchedItem: TrainingItem? = null
        var bestScore = 0

        // 1. Match specific training items by input similarity or semantic tokens
        val inputTokens = trimmedInput.split(Regex("[\\s,?.!]+")).filter { it.length > 1 }

        for (item in enabledItems) {
            val itemUser = item.userInput.trim().lowercase(Locale.ROOT)
            var score = 0

            if (itemUser.isNotBlank()) {
                if (trimmedInput == itemUser) {
                    score += 100
                } else if (trimmedInput.contains(itemUser) || itemUser.contains(trimmedInput)) {
                    score += 50
                } else {
                    val itemTokens = itemUser.split(Regex("[\\s,?.!]+")).filter { it.length > 1 }
                    val overlap = inputTokens.intersect(itemTokens.toSet()).size
                    if (overlap > 0) {
                        score += overlap * 20
                    }
                }
            }

            // Match intent & category bonus
            if (item.intent.equals(intent, ignoreCase = true)) {
                score += 15
            }
            if (item.category.equals(intent, ignoreCase = true) ||
                (intent.contains("GREETING", ignoreCase = true) && item.category.equals("Greeting", ignoreCase = true))
            ) {
                score += 15
            }

            if (score > bestScore && score >= 15) {
                bestScore = score
                matchedItem = item
            }
        }

        // 2. Collect relevant rules
        val relevantRulesList = mutableListOf<String>()
        // Category-specific or global rules
        for (rule in enabledRules) {
            if (rule.category.equals("General", ignoreCase = true) ||
                rule.category.equals(matchedItem?.category, ignoreCase = true) ||
                rule.category.equals(intent, ignoreCase = true)
            ) {
                relevantRulesList.add(rule.rule)
            }
        }
        if (matchedItem != null) {
            relevantRulesList.addAll(matchedItem.behaviorRules)
        }

        // 3. Collect few-shot good and bad examples
        val goodExamples = mutableListOf<Pair<String, String>>()
        val badExamples = mutableListOf<Pair<String, String>>()

        if (matchedItem != null) {
            matchedItem.goodResponses.forEach { resp ->
                goodExamples.add(Pair(matchedItem.userInput.ifBlank { input }, resp))
            }
            matchedItem.badResponses.forEach { resp ->
                badExamples.add(Pair(matchedItem.userInput.ifBlank { input }, resp))
            }
        }

        // Multi-turn training matches
        for (ct in bundle.conversationTrainings.filter { it.enabled }) {
            if (ct.intent.equals(intent, ignoreCase = true) && ct.expectedBehavior.isNotBlank()) {
                relevantRulesList.add("Multi-turn context: ${ct.expectedBehavior}")
            }
        }

        // 4. Construct behavioral guidance prompt injection
        val promptBuilder = StringBuilder()
        if (relevantRulesList.isNotEmpty() || goodExamples.isNotEmpty() || badExamples.isNotEmpty() || matchedItem != null) {
            promptBuilder.appendLine("BEHAVIORAL TRAINING GUIDANCE (Version v${bundle.version}):")
            if (matchedItem?.tone != null) {
                promptBuilder.appendLine("- Tone to adopt: ${matchedItem.tone.displayName}")
            }
            if (relevantRulesList.isNotEmpty()) {
                promptBuilder.appendLine("- Behavioral Rules:")
                relevantRulesList.distinct().forEach { rule ->
                    promptBuilder.appendLine("  * $rule")
                }
            }
            if (goodExamples.isNotEmpty()) {
                promptBuilder.appendLine("- Preferred Response Styles (Learn the tone/manner, do NOT blindly copy):")
                goodExamples.take(3).forEach { (u, g) ->
                    promptBuilder.appendLine("  * When user says '$u', prefer responses shaped like: \"$g\"")
                }
            }
            if (badExamples.isNotEmpty()) {
                promptBuilder.appendLine("- Prohibited / Anti-patterns (DO NOT respond like this):")
                badExamples.take(3).forEach { (u, b) ->
                    promptBuilder.appendLine("  * Avoid robotic/cold replies like: \"$b\"")
                }
            }
            if (matchedItem?.toolRequired != null) {
                promptBuilder.appendLine("- Tool requirement: Must use '${matchedItem.toolRequired}' tool.")
            }
            if (matchedItem?.confirmationRequired == true) {
                promptBuilder.appendLine("- Security confirmation required before action.")
            }
        }

        return BehavioralContext(
            matchedItem = matchedItem,
            preferredTool = matchedItem?.toolRequired,
            requiresConfirmation = matchedItem?.confirmationRequired ?: false,
            preferredTone = matchedItem?.tone?.displayName,
            relevantRules = relevantRulesList.distinct(),
            goodExamples = goodExamples,
            badExamples = badExamples,
            promptGuidance = promptBuilder.toString().trim()
        )
    }
}
