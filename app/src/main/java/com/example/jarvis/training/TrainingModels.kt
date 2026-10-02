package com.example.jarvis.training

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class TrainingType {
    BEHAVIOR,
    INTENT,
    TOOL,
    CONFIRMATION,
    PERSONALITY,
    MULTI_TURN
}

enum class TrainingTone(val displayName: String) {
    CASUAL("Casual"),
    FRIENDLY("Friendly"),
    PROFESSIONAL("Professional"),
    SERIOUS("Serious"),
    HELPFUL("Helpful"),
    WARNING("Warning"),
    CONCISE("Concise")
}

object TrainingCategories {
    const val GREETING = "Greeting"
    const val CASUAL_CONVERSATION = "Casual Conversation"
    const val QUESTION = "Question"
    const val ADVICE = "Advice"
    const val EMOTIONAL_CONVERSATION = "Emotional Conversation"
    const val FOLLOW_UP = "Follow-up"
    const val CLARIFICATION = "Clarification"
    const val HINDI = "Hindi"
    const val HINGLISH = "Hinglish"
    const val BANGLA = "Bangla"
    const val BANGLISH = "Banglish"
    const val ENGLISH = "English"
    const val DEVICE_INFORMATION = "Device Information"
    const val DEVICE_ACTION = "Device Action"
    const val BATTERY = "Battery"
    const val WI_FI = "Wi-Fi"
    const val BLUETOOTH = "Bluetooth"
    const val TORCH = "Torch"
    const val VOLUME = "Volume"
    const val BRIGHTNESS = "Brightness"
    const val APP_OPENING = "App Opening"
    const val FILE_OPERATION = "File Operation"
    const val DOCUMENT_ANALYSIS = "Document Analysis"
    const val PDF = "PDF"
    const val WEB_RESEARCH = "Web Research"
    const val REMINDER = "Reminder"
    const val AUTOMATION = "Automation"
    const val MEMORY = "Memory"
    const val PRIVACY = "Privacy"
    const val SECURITY = "Security"
    const val CONFIRMATION = "Confirmation"
    const val ERROR_HANDLING = "Error Handling"
    const val UNKNOWN_REQUEST = "Unknown Request"
    const val UNSAFE_REQUEST = "Unsafe Request"
    const val HELP = "Help"

    val ALL = listOf(
        GREETING, CASUAL_CONVERSATION, QUESTION, ADVICE, EMOTIONAL_CONVERSATION,
        FOLLOW_UP, CLARIFICATION, HINDI, HINGLISH, BANGLA, BANGLISH, ENGLISH,
        DEVICE_INFORMATION, DEVICE_ACTION, BATTERY, WI_FI, BLUETOOTH, TORCH,
        VOLUME, BRIGHTNESS, APP_OPENING, FILE_OPERATION, DOCUMENT_ANALYSIS, PDF,
        WEB_RESEARCH, REMINDER, AUTOMATION, MEMORY, PRIVACY, SECURITY,
        CONFIRMATION, ERROR_HANDLING, UNKNOWN_REQUEST, UNSAFE_REQUEST, HELP
    )
}

data class TrainingItem(
    val id: String = UUID.randomUUID().toString(),
    val type: TrainingType = TrainingType.BEHAVIOR,
    val category: String = TrainingCategories.GREETING,
    val intent: String = "GREETING",
    val userInput: String = "",
    val context: String = "",
    val goodResponses: List<String> = emptyList(),
    val badResponses: List<String> = emptyList(),
    val behaviorRules: List<String> = emptyList(),
    val language: String = "auto",
    val tone: TrainingTone = TrainingTone.FRIENDLY,
    val toolRequired: String? = null,
    val confirmationRequired: Boolean = false,
    val priority: Int = 1,
    val enabled: Boolean = true,
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("type", type.name)
        json.put("category", category)
        json.put("intent", intent)
        json.put("userInput", userInput)
        json.put("context", context)
        json.put("goodResponses", JSONArray(goodResponses))
        json.put("badResponses", JSONArray(badResponses))
        json.put("behaviorRules", JSONArray(behaviorRules))
        json.put("language", language)
        json.put("tone", tone.name)
        if (toolRequired != null) json.put("toolRequired", toolRequired)
        json.put("confirmationRequired", confirmationRequired)
        json.put("priority", priority)
        json.put("enabled", enabled)
        json.put("version", version)
        json.put("createdAt", createdAt)
        json.put("updatedAt", updatedAt)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): TrainingItem {
            val goodList = mutableListOf<String>()
            val goodArr = json.optJSONArray("goodResponses")
            if (goodArr != null) {
                for (i in 0 until goodArr.length()) goodList.add(goodArr.getString(i))
            }
            val badList = mutableListOf<String>()
            val badArr = json.optJSONArray("badResponses")
            if (badArr != null) {
                for (i in 0 until badArr.length()) badList.add(badArr.getString(i))
            }
            val rulesList = mutableListOf<String>()
            val rulesArr = json.optJSONArray("behaviorRules") ?: json.optJSONArray("rules")
            if (rulesArr != null) {
                for (i in 0 until rulesArr.length()) rulesList.add(rulesArr.getString(i))
            }

            return TrainingItem(
                id = json.optString("id", UUID.randomUUID().toString()),
                type = try { TrainingType.valueOf(json.optString("type", TrainingType.BEHAVIOR.name)) } catch (e: Exception) { TrainingType.BEHAVIOR },
                category = json.optString("category", TrainingCategories.GREETING),
                intent = json.optString("intent", "GREETING"),
                userInput = json.optString("userInput", ""),
                context = json.optString("context", ""),
                goodResponses = goodList,
                badResponses = badList,
                behaviorRules = rulesList,
                language = json.optString("language", "auto"),
                tone = try { TrainingTone.valueOf(json.optString("tone", TrainingTone.FRIENDLY.name)) } catch (e: Exception) { TrainingTone.FRIENDLY },
                toolRequired = if (json.has("toolRequired") && !json.isNull("toolRequired") && json.optString("toolRequired").isNotBlank()) json.getString("toolRequired") else null,
                confirmationRequired = json.optBoolean("confirmationRequired", false),
                priority = json.optInt("priority", 1),
                enabled = json.optBoolean("enabled", true),
                version = json.optInt("version", 1),
                createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
            )
        }
    }
}

data class ConversationTurn(
    val role: String, // "user" or "jarvis"
    val text: String,
    val toolUsed: String? = null,
    val confirmationRequired: Boolean = false
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("role", role)
        json.put("text", text)
        if (toolUsed != null) json.put("toolUsed", toolUsed)
        json.put("confirmationRequired", confirmationRequired)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): ConversationTurn {
            return ConversationTurn(
                role = json.optString("role", "user"),
                text = json.optString("text", ""),
                toolUsed = if (json.has("toolUsed") && !json.isNull("toolUsed")) json.getString("toolUsed") else null,
                confirmationRequired = json.optBoolean("confirmationRequired", false)
            )
        }
    }
}

data class ConversationTraining(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Multi-turn Dialogue",
    val turns: List<ConversationTurn> = emptyList(),
    val expectedBehavior: String = "",
    val intent: String = "CASUAL_CONVERSATION",
    val language: String = "auto",
    val toolRequired: String? = null,
    val confirmationRequired: Boolean = false,
    val enabled: Boolean = true,
    val version: Int = 1
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("title", title)
        val turnsArr = JSONArray()
        turns.forEach { turnsArr.put(it.toJson()) }
        json.put("turns", turnsArr)
        json.put("expectedBehavior", expectedBehavior)
        json.put("intent", intent)
        json.put("language", language)
        if (toolRequired != null) json.put("toolRequired", toolRequired)
        json.put("confirmationRequired", confirmationRequired)
        json.put("enabled", enabled)
        json.put("version", version)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): ConversationTraining {
            val turnsList = mutableListOf<ConversationTurn>()
            val turnsArr = json.optJSONArray("turns")
            if (turnsArr != null) {
                for (i in 0 until turnsArr.length()) {
                    turnsList.add(ConversationTurn.fromJson(turnsArr.getJSONObject(i)))
                }
            }
            return ConversationTraining(
                id = json.optString("id", UUID.randomUUID().toString()),
                title = json.optString("title", "Multi-turn Dialogue"),
                turns = turnsList,
                expectedBehavior = json.optString("expectedBehavior", ""),
                intent = json.optString("intent", "CASUAL_CONVERSATION"),
                language = json.optString("language", "auto"),
                toolRequired = if (json.has("toolRequired") && !json.isNull("toolRequired")) json.getString("toolRequired") else null,
                confirmationRequired = json.optBoolean("confirmationRequired", false),
                enabled = json.optBoolean("enabled", true),
                version = json.optInt("version", 1)
            )
        }
    }
}

data class BehaviorRule(
    val id: String = UUID.randomUUID().toString(),
    val rule: String = "",
    val category: String = TrainingCategories.GREETING,
    val priority: Int = 1,
    val enabled: Boolean = true,
    val version: Int = 1
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("rule", rule)
        json.put("category", category)
        json.put("priority", priority)
        json.put("enabled", enabled)
        json.put("version", version)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): BehaviorRule {
            return BehaviorRule(
                id = json.optString("id", UUID.randomUUID().toString()),
                rule = json.optString("rule", ""),
                category = json.optString("category", TrainingCategories.GREETING),
                priority = json.optInt("priority", 1),
                enabled = json.optBoolean("enabled", true),
                version = json.optInt("version", 1)
            )
        }
    }
}

data class TrainingBundle(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val items: List<TrainingItem> = emptyList(),
    val conversationTrainings: List<ConversationTraining> = emptyList(),
    val rules: List<BehaviorRule> = emptyList(),
    val voiceConfig: com.example.jarvis.voice.JarvisVoiceConfig = com.example.jarvis.voice.JarvisVoiceConfig(),
    val appliedBy: String = "JARVIS Trainer"
) {
    fun toJsonString(): String {
        val root = JSONObject()
        root.put("trainingVersion", version)
        root.put("timestamp", timestamp)
        root.put("appliedBy", appliedBy)
        root.put("voiceConfig", voiceConfig.toJson())

        val itemsArr = JSONArray()
        items.forEach { itemsArr.put(it.toJson()) }
        root.put("items", itemsArr)

        val convArr = JSONArray()
        conversationTrainings.forEach { convArr.put(it.toJson()) }
        root.put("conversationTrainings", convArr)

        val rulesArr = JSONArray()
        rules.forEach { rulesArr.put(it.toJson()) }
        root.put("rules", rulesArr)

        return root.toString(2)
    }

    companion object {
        fun fromJsonString(jsonStr: String): TrainingBundle {
            val root = JSONObject(jsonStr)
            val version = root.optInt("trainingVersion", root.optInt("version", 1))
            val timestamp = root.optLong("timestamp", System.currentTimeMillis())
            val appliedBy = root.optString("appliedBy", "JARVIS Trainer")

            val voiceConfig = if (root.has("voiceConfig")) {
                com.example.jarvis.voice.JarvisVoiceConfig.fromJson(root.getJSONObject("voiceConfig"))
            } else {
                com.example.jarvis.voice.JarvisVoiceConfig()
            }

            val items = mutableListOf<TrainingItem>()
            val itemsArr = root.optJSONArray("items")
            if (itemsArr != null) {
                for (i in 0 until itemsArr.length()) {
                    items.add(TrainingItem.fromJson(itemsArr.getJSONObject(i)))
                }
            }

            val convs = mutableListOf<ConversationTraining>()
            val convArr = root.optJSONArray("conversationTrainings")
            if (convArr != null) {
                for (i in 0 until convArr.length()) {
                    convs.add(ConversationTraining.fromJson(convArr.getJSONObject(i)))
                }
            }

            val rules = mutableListOf<BehaviorRule>()
            val rulesArr = root.optJSONArray("rules")
            if (rulesArr != null) {
                for (i in 0 until rulesArr.length()) {
                    rules.add(BehaviorRule.fromJson(rulesArr.getJSONObject(i)))
                }
            }

            return TrainingBundle(
                version = version,
                timestamp = timestamp,
                items = items,
                conversationTrainings = convs,
                rules = rules,
                voiceConfig = voiceConfig,
                appliedBy = appliedBy
            )
        }
    }
}

enum class BatchTrainingItemType(val displayName: String) {
    BEHAVIOR("Behavior Instruction"),
    PERSONALITY("Personality Instruction"),
    LANGUAGE("Language Rule"),
    SAFETY("Safety Policy"),
    TOOL_ACTION("Tool & Action"),
    RESPONSE_EXAMPLE("Response Example")
}

data class ParsedBatchDirective(
    val id: String = UUID.randomUUID().toString(),
    val rawInstruction: String,
    val type: BatchTrainingItemType = BatchTrainingItemType.BEHAVIOR,
    val category: String = TrainingCategories.CASUAL_CONVERSATION,
    val title: String = "",
    val ruleText: String = "",
    val userInputExample: String? = null,
    val goodResponseExample: String? = null,
    val badResponseExample: String? = null,
    val requiresConfirmation: Boolean = false,
    val toolRequired: String? = null,
    val tone: TrainingTone = TrainingTone.FRIENDLY,
    val priority: Int = 2,
    val isApproved: Boolean = true
) {
    fun toBehaviorRule(version: Int = 1): BehaviorRule {
        return BehaviorRule(
            id = id,
            rule = ruleText.ifBlank { rawInstruction },
            category = category,
            priority = priority,
            enabled = true,
            version = version
        )
    }

    fun toTrainingItem(version: Int = 1): TrainingItem? {
        val input = userInputExample ?: if (toolRequired != null) "Check $toolRequired" else ""
        if (input.isBlank() && goodResponseExample.isNullOrBlank() && toolRequired == null) {
            return null
        }
        return TrainingItem(
            id = id,
            type = when (type) {
                BatchTrainingItemType.TOOL_ACTION -> TrainingType.TOOL
                BatchTrainingItemType.SAFETY -> TrainingType.CONFIRMATION
                BatchTrainingItemType.PERSONALITY -> TrainingType.PERSONALITY
                else -> TrainingType.BEHAVIOR
            },
            category = category,
            intent = when (type) {
                BatchTrainingItemType.TOOL_ACTION -> "DEVICE_ACTION"
                BatchTrainingItemType.SAFETY -> "CONFIRMATION_CHECK"
                BatchTrainingItemType.LANGUAGE -> "LANGUAGE_SWITCH"
                else -> "BEHAVIORAL_DIRECTIVE"
            },
            userInput = input,
            goodResponses = if (!goodResponseExample.isNullOrBlank()) listOf(goodResponseExample) else emptyList(),
            badResponses = if (!badResponseExample.isNullOrBlank()) listOf(badResponseExample) else emptyList(),
            behaviorRules = listOf(ruleText.ifBlank { rawInstruction }),
            language = if (type == BatchTrainingItemType.LANGUAGE) category.lowercase() else "auto",
            tone = tone,
            toolRequired = toolRequired,
            confirmationRequired = requiresConfirmation,
            priority = priority,
            enabled = true,
            version = version
        )
    }
}

data class BatchAnalysisSummary(
    val totalDirectives: Int = 0,
    val rulesCount: Int = 0,
    val examplesCount: Int = 0,
    val personalityCount: Int = 0,
    val languageCount: Int = 0,
    val safetyCount: Int = 0,
    val toolCount: Int = 0
)

data class BatchApplyResult(
    val rulesCount: Int,
    val examplesCount: Int,
    val personalityCount: Int,
    val languageCount: Int,
    val safetyCount: Int,
    val toolCount: Int,
    val isSynced: Boolean,
    val version: Int,
    val message: String
)

