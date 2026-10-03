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
    val preferredTitle: String? = null,
    val forbiddenWords: List<String> = emptyList(),
    val forcedLanguage: String? = null,
    val directTrainedResponse: String? = null,
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

        // 2. Collect all active user-configured rules
        val relevantRulesList = mutableListOf<String>()
        // All enabled rules from training must be strictly enforced
        for (rule in enabledRules) {
            relevantRulesList.add(rule.rule)
        }
        if (matchedItem != null) {
            relevantRulesList.addAll(matchedItem.behaviorRules)
        }

        // 3. Scan for user Title, Forbidden Words, and Direct Q&A in rules
        var preferredTitle: String? = null
        val forbiddenWordsList = mutableListOf<String>()
        var forcedLanguage: String? = null
        var directTrainedResponse: String? = null

        if (matchedItem != null && matchedItem.goodResponses.isNotEmpty()) {
            directTrainedResponse = matchedItem.goodResponses.first()
        }

        for (ruleStr in relevantRulesList) {
            val rLower = ruleStr.lowercase(Locale.ROOT)

            // Title check: "call me Boss", "mujhe boss bolo", "address user as Boss"
            val titleMatch = Regex("""(?i)(?:call\s+me|mujhe|mereko|amake|address\s+(?:the\s+)?user\s+as)\s+([a-zA-Z\u0900-\u097F\u0980-\u09FF]+)\s*(?:bolo|bulao|bolbe)?""").find(ruleStr)
            if (titleMatch != null) {
                val candidate = titleMatch.groupValues[1].trim().replaceFirstChar { it.uppercase() }
                if (!candidate.equals("bhai", ignoreCase = true) && !candidate.equals("sir", ignoreCase = true) && candidate.length > 1) {
                    preferredTitle = candidate
                }
            }

            // Forbidden words: "bhai mat bolo", "don't call me bhai", "never address the user as 'bhai'"
            if (rLower.contains("bhai mat") || rLower.contains("don't call me bhai") ||
                rLower.contains("dont call me bhai") || rLower.contains("bhai na bol") ||
                rLower.contains("as 'bhai'") || rLower.contains("as bhai") ||
                rLower.contains("forbidden word: 'bhai'") || rLower.contains("never address the user as 'bhai'")
            ) {
                forbiddenWordsList.add("bhai")
                forbiddenWordsList.add("bro")
                forbiddenWordsList.add("bhaiya")
            }

            // Language directives
            if (rLower.contains("hindi me") || rLower.contains("reply in hindi") || rLower.contains("speak in hindi")) {
                forcedLanguage = "HINDI"
            } else if (rLower.contains("bangla") || rLower.contains("bengali") || rLower.contains("বাংলা")) {
                forcedLanguage = "BENGALI"
            } else if (rLower.contains("english only") || rLower.contains("reply in english")) {
                forcedLanguage = "ENGLISH"
            }

            // Direct Q&A rule match: comprehensive patterns
            if (directTrainedResponse == null) {
                // Pattern A: "When user says 'X', reply: 'Y'" or "when i say X reply Y"
                val qaMatch = Regex("""(?i)(?:when (?:user|i)\s+(?:inputs|says?|asks?)|if (?:user|i)\s+(?:inputs|says?|asks?))\s*[:\"'“‘]?(.*?)[?\"'”’]?\s*(?:,|then)?\s*(?:respond with|reply:?|say)\s*[:\"'“‘]?(.*?)[.!?\"'”’]*$""").find(ruleStr)
                if (qaMatch != null) {
                    val q = qaMatch.groupValues[1].trim().lowercase(Locale.ROOT).removeSurrounding("\"").removeSurrounding("'")
                    val a = qaMatch.groupValues[2].trim().removeSurrounding("\"").removeSurrounding("'")
                    if (trimmedInput == q || (q.length > 1 && (trimmedInput.contains(q) || q.contains(trimmedInput)))) {
                        directTrainedResponse = a
                    }
                }
            }

            // Pattern B: Arrow syntax "X" -> "Y" or X => Y
            if (directTrainedResponse == null) {
                val arrowMatch = Regex("""^["']?(.*?)["']?\s*(?:->|=>)\s*["']?(.*?)["']?$""").find(ruleStr.trim())
                if (arrowMatch != null && !ruleStr.trim().startsWith("http")) {
                    val q = arrowMatch.groupValues[1].trim().lowercase(Locale.ROOT).removeSurrounding("\"").removeSurrounding("'")
                    val a = arrowMatch.groupValues[2].trim().removeSurrounding("\"").removeSurrounding("'")
                    if (trimmedInput == q || (q.length > 1 && (trimmedInput.contains(q) || q.contains(trimmedInput)))) {
                        directTrainedResponse = a
                    }
                }
            }

            // Pattern C: Hindi/Hinglish conditional: "jab main bolu X to bolo Y" or "agar bolu X to kehna Y"
            if (directTrainedResponse == null) {
                val hindiCondRegex = Regex("""(?i)(?:jab|agar|yadi)\s+(?:main\s+|hum\s+)?(?:bolu|bolun|kahu|kahun|puchu|puchun|likhu)\s*[:\"'“‘]?(.*?)[?\"'”’]?\s+(?:to|tab|then)\s+(?:bolo|bolna|kaho|kehna|reply|jawab\s+do|answer)\s*[:\"'“‘]?(.*?)[.!?\"'”’]*$""")
                val hindiMatch = hindiCondRegex.find(ruleStr.trim())
                if (hindiMatch != null) {
                    val q = hindiMatch.groupValues[1].trim().lowercase(Locale.ROOT).removeSurrounding("\"").removeSurrounding("'")
                    val a = hindiMatch.groupValues[2].trim().removeSurrounding("\"").removeSurrounding("'")
                    if (trimmedInput == q || (q.length > 1 && (trimmedInput.contains(q) || q.contains(trimmedInput)))) {
                        directTrainedResponse = a
                    }
                }
            }

            // Pattern D: Bangla conditional: "jodi boli X to bolbe Y"
            if (directTrainedResponse == null) {
                val banglaCondRegex = Regex("""(?i)(?:jodi|jotohon)\s+(?:ami\s+)?(?:boli|bolbo)\s*[:\"'“‘]?(.*?)[?\"'”’]?\s+(?:tahole|to)\s+(?:bolbe|bolo|uttor\s+dao)\s*[:\"'“‘]?(.*?)[.!?\"'”’]*$""")
                val banglaMatch = banglaCondRegex.find(ruleStr.trim())
                if (banglaMatch != null) {
                    val q = banglaMatch.groupValues[1].trim().lowercase(Locale.ROOT).removeSurrounding("\"").removeSurrounding("'")
                    val a = banglaMatch.groupValues[2].trim().removeSurrounding("\"").removeSurrounding("'")
                    if (trimmedInput == q || (q.length > 1 && (trimmedInput.contains(q) || q.contains(trimmedInput)))) {
                        directTrainedResponse = a
                    }
                }
            }
        }

        // 4. Collect few-shot good and bad examples
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

        // 5. Construct behavioral guidance prompt injection with strict priority
        val promptBuilder = StringBuilder()
        if (relevantRulesList.isNotEmpty() || goodExamples.isNotEmpty() || badExamples.isNotEmpty() || matchedItem != null) {
            promptBuilder.appendLine("==================================================")
            promptBuilder.appendLine("CRITICAL USER-CONFIGURED TRAINING RULES (Version v${bundle.version}):")
            promptBuilder.appendLine("You MUST strictly obey all rules below. They override any generic assistant defaults or boilerplate habits.")
            if (preferredTitle != null) {
                promptBuilder.appendLine("- Mandatory user title: Address user as \"$preferredTitle\" (never use 'bhai').")
            }
            if (forbiddenWordsList.isNotEmpty()) {
                promptBuilder.appendLine("- Forbidden words: Never say: ${forbiddenWordsList.distinct().joinToString(", ")}.")
            }
            if (matchedItem?.tone != null) {
                promptBuilder.appendLine("- Tone to adopt: ${matchedItem.tone.displayName}")
            }
            if (relevantRulesList.isNotEmpty()) {
                promptBuilder.appendLine("- MANDATORY RULES:")
                relevantRulesList.distinct().forEach { rule ->
                    promptBuilder.appendLine("  * $rule")
                }
            }
            if (goodExamples.isNotEmpty()) {
                promptBuilder.appendLine("- PREFERRED RESPONSE STYLES:")
                goodExamples.take(5).forEach { (u, g) ->
                    promptBuilder.appendLine("  * When user says '$u', answer like: \"$g\"")
                }
            }
            if (badExamples.isNotEmpty()) {
                promptBuilder.appendLine("- FORBIDDEN REPLIES (Do NOT respond like this):")
                badExamples.take(5).forEach { (u, b) ->
                    promptBuilder.appendLine("  * Avoid generic/robotic: \"$b\"")
                }
            }
            if (matchedItem?.toolRequired != null) {
                promptBuilder.appendLine("- Tool requirement: Must execute '${matchedItem.toolRequired}' tool.")
            }
            if (matchedItem?.confirmationRequired == true) {
                promptBuilder.appendLine("- Security confirmation required before action.")
            }
            promptBuilder.appendLine("==================================================")
        }

        return BehavioralContext(
            matchedItem = matchedItem,
            preferredTool = matchedItem?.toolRequired,
            requiresConfirmation = matchedItem?.confirmationRequired ?: false,
            preferredTone = matchedItem?.tone?.displayName,
            preferredTitle = preferredTitle,
            forbiddenWords = forbiddenWordsList.distinct(),
            forcedLanguage = forcedLanguage,
            directTrainedResponse = directTrainedResponse,
            relevantRules = relevantRulesList.distinct(),
            goodExamples = goodExamples,
            badExamples = badExamples,
            promptGuidance = promptBuilder.toString().trim()
        )
    }

    /**
     * Applies post-training text filters to guarantees that trained rules,
     * forbidden words (e.g. 'bhai'), and preferred titles (e.g. 'Boss')
     * are strictly enforced across all outputs.
     */
    fun applyPostTrainingTransformations(
        text: String,
        context: BehavioralContext
    ): String {
        var result = text

        // 1. Remove / replace forbidden words
        if (context.forbiddenWords.isNotEmpty()) {
            val replacement = context.preferredTitle ?: "Sir"
            for (forbidden in context.forbiddenWords) {
                result = result.replace(Regex("""(?i)\b${Regex.escape(forbidden)}\b"""), replacement)
            }
        }

        // 2. Enforce preferred title (if user asked to be called "Boss", replace generic "bhai")
        if (context.preferredTitle != null) {
            val title = context.preferredTitle
            result = result.replace(Regex("""(?i)\bbhai\b"""), title)
            result = result.replace(Regex("""(?i)\bbhaiya\b"""), title)
        }

        return result
    }
}
