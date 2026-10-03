package com.example.jarvis.training

import java.util.Locale
import java.util.UUID

/**
 * Intelligent Prompt-Based Batch Training Engine for J.A.R.V.I.S.
 *
 * Compiles unstructured, natural language batch prompts containing multiple rules,
 * behavioral directives, language preferences, safety policies, and personality changes
 * into structured TrainingItems, BehaviorRules, and intent models.
 *
 * NOTE: As transparently documented, this engine performs Structured Behavioral Steering
 * & Few-Shot Constraint Compilation in JARVIS's persistent memory system, rather than
 * parameter fine-tuning.
 */
object PromptBatchTrainingEngine {

    val DEFAULT_TEMPLATE = """
JARVIS should call me naturally and respectfully.
If I speak Bangla, reply in Bangla.
If I speak English, reply in English.
For destructive actions always ask for confirmation.
When I ask about battery, give the current battery percentage.
Be concise by default.
Do not sound robotic.
When asked to turn on torch or flashlight, use the Torch tool immediately.
Never reveal sensitive passwords or wipe files without user approval.
Maintain a friendly, Stark-inspired engineering demeanor.
""".trimIndent()

    val BILINGUAL_BANGLA_TEMPLATE = """
Always greet with "আমি জার্ভিস বলছি" when Bengali language is detected.
If I ask in Bangla, reply in authentic and natural Bangla.
If I speak in English, reply in crisp English.
When asked for battery status, check device battery telemetry directly.
Always confirm before deleting any file or resetting system configuration.
Keep responses concise, polite, and helpful.
""".trimIndent()

    val STARK_PERSONALITY_TEMPLATE = """
Adopt Tony Stark engineering precision with high technical confidence.
Be witty, proactive, and articulate without being pompous.
Avoid robotic boilerplate phrases like "As an AI model".
For complex technical queries, break explanations down logically.
When executing device hardware commands, confirm action concisely.
Always prioritize device security and user privacy.
""".trimIndent()

    val STRICT_SAFETY_TEMPLATE = """
For any file deletion or modification, require explicit two-factor user confirmation.
Never execute terminal commands or destructive operations without a warning prompt.
When battery is below 15%, proactively warn about low power state.
Keep network and Bluetooth operations strictly sandboxed.
Ensure all privacy-sensitive logs remain local on device.
""".trimIndent()

    /**
     * Parses a multi-instruction training prompt into individual structured directives.
     */
    fun parseTrainingPrompt(rawPrompt: String): List<ParsedBatchDirective> {
        val trimmed = rawPrompt.trim()
        if (trimmed.isBlank()) return emptyList()

        // 1. Split by lines, bullet points, or numbered lists
        val rawLines = trimmed.lines()
            .flatMap { line ->
                // Split multi-sentence lines if they are clearly separated directives
                if (line.contains(";") || line.contains(". ") && !line.contains("e.g.") && !line.contains("i.e.")) {
                    line.split(Regex("(?<=[.;])\\s+(?=[A-Z0-9IfWhenDoBeForAlwaysNever])"))
                } else {
                    listOf(line)
                }
            }
            .map { cleanLine(it) }
            .filter { it.isNotBlank() && it.length > 4 }

        val directives = mutableListOf<ParsedBatchDirective>()

        for (line in rawLines) {
            val lower = line.lowercase(Locale.ROOT)

            // A. Check Language directives first (e.g. "If I speak Bangla, reply in Bangla")
            if (isLanguageInstruction(lower)) {
                directives.add(buildLanguageDirective(line, lower))
                continue
            }

            // B. Check for explicit dialogue/example syntax
            // e.g. "User: Hi | Jarvis: Hello Sir" or "When I say X -> reply Y" or "jab main bolu hi to bolo haan boss"
            val dialogueMatch = extractDialogueExample(line)
            if (dialogueMatch != null) {
                directives.add(dialogueMatch)
                continue
            }

            // C. Classify based on semantic intent and domain
            when {
                // 1. Safety & Destructive Confirmation Policies
                isSafetyInstruction(lower) -> {
                    directives.add(buildSafetyDirective(line, lower))
                }

                // 2. Personality & Tone Rules (Checked before tools so "sound robotic" is not confused with audio volume)
                isPersonalityInstruction(lower) -> {
                    directives.add(buildPersonalityDirective(line, lower))
                }

                // 3. Tool & Hardware Action Rules
                isToolInstruction(lower) -> {
                    directives.add(buildToolDirective(line, lower))
                }

                // 4. General Behavioral Instruction
                else -> {
                    directives.add(buildGeneralBehaviorDirective(line, lower))
                }
            }
        }

        return directives
    }

    private fun cleanLine(line: String): String {
        return line.trim()
            .removePrefix("-")
            .removePrefix("*")
            .removePrefix("•")
            .replace(Regex("^\\d+[.)]\\s*"), "")
            .trim()
    }

    private fun extractDialogueExample(line: String): ParsedBatchDirective? {
        val trimmed = line.trim()

        // Syntax 1: User: ... | Jarvis: ... or User: ... -> Jarvis: ...
        val userJarvisRegex = Regex("""(?i)(?:user|input|when i say|if i say)[:\s]+"?(.*?)"?\s*(?:\||->|=>|then|jarvis says?|reply)[:\s]+"?(.*?)"?$""")
        val match = userJarvisRegex.find(trimmed)
        if (match != null) {
            val user = match.groupValues[1].trim()
            val response = match.groupValues[2].trim()
            if (user.isNotBlank() && response.isNotBlank()) {
                return ParsedBatchDirective(
                    rawInstruction = line,
                    type = BatchTrainingItemType.RESPONSE_EXAMPLE,
                    category = TrainingCategories.CASUAL_CONVERSATION,
                    title = "Dialogue: \"$user\"",
                    ruleText = "When user inputs \"$user\", respond with \"$response\".",
                    userInputExample = user,
                    goodResponseExample = response,
                    tone = TrainingTone.FRIENDLY
                )
            }
        }

        // Syntax 2: Arrow syntax: "Hi" -> "Hello Sir" or "Kya kar rahe ho" => "Aapka wait"
        val arrowRegex = Regex("""^["']?(.*?)["']?\s*(?:->|=>)\s*["']?(.*?)["']?$""")
        val arrowMatch = arrowRegex.find(trimmed)
        if (arrowMatch != null && !trimmed.startsWith("http")) {
            val user = arrowMatch.groupValues[1].trim()
            val response = arrowMatch.groupValues[2].trim()
            if (user.isNotBlank() && response.isNotBlank() && user.length < 100) {
                return ParsedBatchDirective(
                    rawInstruction = line,
                    type = BatchTrainingItemType.RESPONSE_EXAMPLE,
                    category = TrainingCategories.CASUAL_CONVERSATION,
                    title = "Prompt Pattern: \"$user\"",
                    ruleText = "When user says \"$user\", reply: \"$response\".",
                    userInputExample = user,
                    goodResponseExample = response,
                    tone = TrainingTone.FRIENDLY
                )
            }
        }

        // Syntax 3: Hindi/Hinglish conditional: "jab main bolu X to bolo Y" or "agar kahu X to Y kehna"
        val hindiCondRegex = Regex("""(?i)(?:jab|agar|yadi)\s+(?:main\s+|hum\s+)?(?:bolu|bolun|kahu|kahun|puchu|puchun|likhu)\s*[:\"'“‘]?(.*?)[?\"'”’]?\s+(?:to|tab|then)\s+(?:bolo|bolna|kaho|kehna|reply|jawab\s+do|answer)\s*[:\"'“‘]?(.*?)[.!?\"'”’]*$""")
        val hindiMatch = hindiCondRegex.find(trimmed)
        if (hindiMatch != null) {
            val user = hindiMatch.groupValues[1].trim().removeSurrounding("\"").removeSurrounding("'")
            val response = hindiMatch.groupValues[2].trim().removeSurrounding("\"").removeSurrounding("'")
            if (user.isNotBlank() && response.isNotBlank()) {
                return ParsedBatchDirective(
                    rawInstruction = line,
                    type = BatchTrainingItemType.RESPONSE_EXAMPLE,
                    category = TrainingCategories.CASUAL_CONVERSATION,
                    title = "Hinglish Trigger: \"$user\"",
                    ruleText = line,
                    userInputExample = user,
                    goodResponseExample = response,
                    tone = TrainingTone.FRIENDLY
                )
            }
        }

        // Syntax 4: Bangla conditional: "jodi boli X to bolbe Y" or "jotohon boli X to bolbe Y"
        val banglaCondRegex = Regex("""(?i)(?:jodi|jotohon)\s+(?:ami\s+)?(?:boli|bolbo|jiggasa\s+kori)\s*[:\"'“‘]?(.*?)[?\"'”’]?\s+(?:tahole|to)\s+(?:bolbe|bolo|uttor\s+dao)\s*[:\"'“‘]?(.*?)[.!?\"'”’]*$""")
        val banglaMatch = banglaCondRegex.find(trimmed)
        if (banglaMatch != null) {
            val user = banglaMatch.groupValues[1].trim().removeSurrounding("\"").removeSurrounding("'")
            val response = banglaMatch.groupValues[2].trim().removeSurrounding("\"").removeSurrounding("'")
            if (user.isNotBlank() && response.isNotBlank()) {
                return ParsedBatchDirective(
                    rawInstruction = line,
                    type = BatchTrainingItemType.RESPONSE_EXAMPLE,
                    category = TrainingCategories.BANGLA,
                    title = "Bangla Trigger: \"$user\"",
                    ruleText = line,
                    userInputExample = user,
                    goodResponseExample = response,
                    tone = TrainingTone.FRIENDLY
                )
            }
        }

        // Syntax 5: English natural phrasing: "When asked about X reply with Y"
        val engCondRegex = Regex("""(?i)(?:when\s+(?:i\s+|user\s+)?(?:say|ask|speaks?)|if\s+(?:i\s+|user\s+)?(?:say|ask|speaks?))\s*[:\"'“‘]?(.*?)[?\"'”’]?\s*(?:then|reply|respond|say|answer)\s*[:\"'“‘]?(.*?)[.!?\"'”’]*$""")
        val engMatch = engCondRegex.find(trimmed)
        if (engMatch != null) {
            val user = engMatch.groupValues[1].trim().removeSurrounding("\"").removeSurrounding("'")
            val response = engMatch.groupValues[2].trim().removeSurrounding("\"").removeSurrounding("'")
            if (user.isNotBlank() && response.isNotBlank()) {
                return ParsedBatchDirective(
                    rawInstruction = line,
                    type = BatchTrainingItemType.RESPONSE_EXAMPLE,
                    category = TrainingCategories.CASUAL_CONVERSATION,
                    title = "Dialogue: \"$user\"",
                    ruleText = line,
                    userInputExample = user,
                    goodResponseExample = response,
                    tone = TrainingTone.HELPFUL
                )
            }
        }

        // Syntax 6: Title preference: "mujhe Boss bolo" / "call me Boss" / "amake Boss bolbe"
        val titleMatch = Regex("""(?i)(?:mujhe|mereko|amake|call\s+me)\s+([a-zA-Z\u0900-\u097F\u0980-\u09FF]+)\s*(?:bolo|bulao|bolbe)?""").find(trimmed)
        if (titleMatch != null) {
            val title = titleMatch.groupValues[1].trim().replaceFirstChar { it.uppercase() }
            if (title.isNotBlank() && !title.equals("bhai", ignoreCase = true) && !title.equals("kuch", ignoreCase = true)) {
                return ParsedBatchDirective(
                    rawInstruction = line,
                    type = BatchTrainingItemType.PERSONALITY,
                    category = TrainingCategories.CASUAL_CONVERSATION,
                    title = "Preferred Title: $title",
                    ruleText = "Always address user as $title instead of bhai.",
                    userInputExample = null,
                    goodResponseExample = null,
                    tone = TrainingTone.FRIENDLY
                )
            }
        }

        // Syntax 7: Forbidden word: "bhai mat bolo" / "don't call me bhai"
        if (trimmed.contains("bhai mat bolo", ignoreCase = true) ||
            trimmed.contains("bhai mat bol", ignoreCase = true) ||
            trimmed.contains("don't call me bhai", ignoreCase = true) ||
            trimmed.contains("dont call me bhai", ignoreCase = true) ||
            trimmed.contains("bhai bolna band karo", ignoreCase = true)
        ) {
            return ParsedBatchDirective(
                rawInstruction = line,
                type = BatchTrainingItemType.PERSONALITY,
                category = TrainingCategories.CASUAL_CONVERSATION,
                title = "Forbidden Word: 'bhai'",
                ruleText = "Never address the user as 'bhai' or 'bro'. Always address respectfully as Sir or Boss.",
                userInputExample = null,
                goodResponseExample = null,
                badResponseExample = "Hi bhai 😄",
                tone = TrainingTone.SERIOUS
            )
        }

        return null
    }

    private fun isLanguageInstruction(lower: String): Boolean {
        return lower.contains("bangla") || lower.contains("bengali") ||
                lower.contains("english") || lower.contains("hindi") ||
                lower.contains("hinglish") || lower.contains("banglish") ||
                lower.contains("language") || lower.contains("ভাষায়") ||
                lower.contains("বাংলায়") || lower.contains("হিন্দিতে") ||
                lower.contains("speak in") || lower.contains("reply in") ||
                lower.contains("answer in")
    }

    private fun buildLanguageDirective(raw: String, lower: String): ParsedBatchDirective {
        val (cat, langName) = when {
            lower.contains("bangla") || lower.contains("bengali") || lower.contains("বাংলা") ->
                Pair(TrainingCategories.BANGLA, "Bangla")
            lower.contains("english") ->
                Pair(TrainingCategories.ENGLISH, "English")
            lower.contains("hindi") || lower.contains("হিন্দি") ->
                Pair(TrainingCategories.HINDI, "Hindi")
            lower.contains("hinglish") ->
                Pair(TrainingCategories.HINGLISH, "Hinglish")
            lower.contains("banglish") ->
                Pair(TrainingCategories.BANGLISH, "Banglish")
            else ->
                Pair(TrainingCategories.CASUAL_CONVERSATION, "Target Language")
        }

        val goodGreeting = when (cat) {
            TrainingCategories.BANGLA -> "আমি জার্ভিস বলছি! বলুন স্যার, আজ কীভাবে সাহায্য করতে পারি? 🚀"
            TrainingCategories.ENGLISH -> "Hello Sir, I am JARVIS. Standing by for your directive."
            TrainingCategories.HINDI -> "नमस्ते सर! मैं जार्वিস हूँ। बताइए क्या सेवा करूँ?"
            TrainingCategories.HINGLISH -> "Haan Sir, JARVIS here! Bataiye kya plan hai?"
            TrainingCategories.BANGLISH -> "Ami Jarvis bolchi! Kemon achhen dada? Ki bolun?"
            else -> "Standing by, Sir."
        }

        return ParsedBatchDirective(
            rawInstruction = raw,
            type = BatchTrainingItemType.LANGUAGE,
            category = cat,
            title = "Language Switch: $langName",
            ruleText = raw,
            userInputExample = "Language instruction ($langName)",
            goodResponseExample = goodGreeting,
            tone = TrainingTone.HELPFUL,
            priority = 3
        )
    }

    private fun isSafetyInstruction(lower: String): Boolean {
        return lower.contains("destructive") || lower.contains("confirm") ||
                lower.contains("confirmation") || lower.contains("delete") ||
                lower.contains("wipe") || lower.contains("remove") ||
                lower.contains("format") || lower.contains("danger") ||
                lower.contains("permission") || lower.contains("sensitive") ||
                lower.contains("password") || lower.contains("security")
    }

    private fun buildSafetyDirective(raw: String, lower: String): ParsedBatchDirective {
        return ParsedBatchDirective(
            rawInstruction = raw,
            type = BatchTrainingItemType.SAFETY,
            category = TrainingCategories.SECURITY,
            title = "Safety & Confirmation Guardrail",
            ruleText = raw,
            userInputExample = if (lower.contains("delete")) "Delete this file" else "Sensitive action request",
            goodResponseExample = "Please confirm: are you certain you want to proceed with this operation?",
            badResponseExample = "Action executed immediately without confirmation.",
            requiresConfirmation = true,
            tone = TrainingTone.WARNING,
            priority = 4
        )
    }

    private fun isToolInstruction(lower: String): Boolean {
        return lower.contains("battery") || lower.contains("torch") ||
                lower.contains("flashlight") || lower.contains("wifi") ||
                lower.contains("wi-fi") || lower.contains("bluetooth") ||
                lower.contains("volume") || (lower.contains("sound") && !lower.contains("robotic")) ||
                lower.contains("brightness") || lower.contains("open app") || lower.contains("launch app") ||
                lower.contains("file") || lower.contains("reminder") ||
                lower.contains("alarm") || lower.contains("tool")
    }

    private fun buildToolDirective(raw: String, lower: String): ParsedBatchDirective {
        val (tool, cat, trigger, goodResp) = when {
            lower.contains("battery") -> Quad(
                "Battery",
                TrainingCategories.BATTERY,
                "What is my battery level?",
                "Battery percentage is checked via device telemetry."
            )
            lower.contains("torch") || lower.contains("flashlight") -> Quad(
                "Torch",
                TrainingCategories.TORCH,
                "Turn on flashlight",
                "Flashlight activated."
            )
            lower.contains("wifi") || lower.contains("wi-fi") -> Quad(
                "Wi-Fi",
                TrainingCategories.WI_FI,
                "Check Wi-Fi status",
                "Wi-Fi connectivity state retrieved."
            )
            lower.contains("bluetooth") -> Quad(
                "Bluetooth",
                TrainingCategories.BLUETOOTH,
                "Check Bluetooth",
                "Bluetooth status updated."
            )
            lower.contains("volume") || lower.contains("sound") -> Quad(
                "Volume",
                TrainingCategories.VOLUME,
                "Set volume to 50%",
                "Volume calibrated."
            )
            lower.contains("brightness") -> Quad(
                "Brightness",
                TrainingCategories.BRIGHTNESS,
                "Set brightness",
                "Display brightness adjusted."
            )
            lower.contains("app") || lower.contains("open") -> Quad(
                "App Opening",
                TrainingCategories.APP_OPENING,
                "Open application",
                "Launching requested application."
            )
            lower.contains("file") -> Quad(
                "File Operation",
                TrainingCategories.FILE_OPERATION,
                "File action",
                "Accessing file system."
            )
            lower.contains("reminder") || lower.contains("alarm") -> Quad(
                "Reminder",
                TrainingCategories.REMINDER,
                "Set reminder",
                "Reminder configured."
            )
            else -> Quad(
                "Automation",
                TrainingCategories.AUTOMATION,
                "Device action",
                "Executing hardware directive."
            )
        }

        return ParsedBatchDirective(
            rawInstruction = raw,
            type = BatchTrainingItemType.TOOL_ACTION,
            category = cat,
            title = "Tool Automation: $tool",
            ruleText = raw,
            userInputExample = trigger,
            goodResponseExample = goodResp,
            toolRequired = tool,
            tone = TrainingTone.HELPFUL,
            priority = 3
        )
    }

    private data class Quad(val first: String, val second: String, val third: String, val fourth: String)

    private fun isPersonalityInstruction(lower: String): Boolean {
        return lower.contains("robotic") || lower.contains("stark") ||
                lower.contains("call me") || lower.contains("tone") ||
                lower.contains("personality") || lower.contains("concise") ||
                lower.contains("brief") || lower.contains("witty") ||
                lower.contains("humor") || lower.contains("humble") ||
                lower.contains("friendly") || lower.contains("brother") ||
                lower.contains("naturally") || lower.contains("iron man") ||
                lower.contains("polite") || lower.contains("demeanor")
    }

    private fun buildPersonalityDirective(raw: String, lower: String): ParsedBatchDirective {
        val tone = when {
            lower.contains("concise") || lower.contains("brief") -> TrainingTone.CONCISE
            lower.contains("friendly") || lower.contains("brother") -> TrainingTone.FRIENDLY
            lower.contains("witty") || lower.contains("humor") || lower.contains("stark") -> TrainingTone.CASUAL
            lower.contains("serious") -> TrainingTone.SERIOUS
            else -> TrainingTone.FRIENDLY
        }

        return ParsedBatchDirective(
            rawInstruction = raw,
            type = BatchTrainingItemType.PERSONALITY,
            category = TrainingCategories.CASUAL_CONVERSATION,
            title = "Personality & Tone: ${tone.displayName}",
            ruleText = raw,
            userInputExample = "How are you doing today?",
            goodResponseExample = if (tone == TrainingTone.CONCISE)
                "All systems nominal, Sir. What is next?"
            else
                "Operating at peak efficiency, Sir! What directive do we have today?",
            tone = tone,
            priority = 2
        )
    }

    private fun buildGeneralBehaviorDirective(raw: String, lower: String): ParsedBatchDirective {
        return ParsedBatchDirective(
            rawInstruction = raw,
            type = BatchTrainingItemType.BEHAVIOR,
            category = TrainingCategories.CASUAL_CONVERSATION,
            title = "Behavioral Directive",
            ruleText = raw,
            tone = TrainingTone.HELPFUL,
            priority = 2
        )
    }

    /**
     * Calculates summary metrics for review and reporting.
     */
    fun summarize(directives: List<ParsedBatchDirective>): BatchAnalysisSummary {
        val approved = directives.filter { it.isApproved }
        return BatchAnalysisSummary(
            totalDirectives = approved.size,
            rulesCount = approved.count { it.type != BatchTrainingItemType.RESPONSE_EXAMPLE },
            examplesCount = approved.count { !it.userInputExample.isNullOrBlank() || it.type == BatchTrainingItemType.RESPONSE_EXAMPLE },
            personalityCount = approved.count { it.type == BatchTrainingItemType.PERSONALITY },
            languageCount = approved.count { it.type == BatchTrainingItemType.LANGUAGE },
            safetyCount = approved.count { it.type == BatchTrainingItemType.SAFETY },
            toolCount = approved.count { it.type == BatchTrainingItemType.TOOL_ACTION }
        )
    }

    /**
     * Checks if a rule is already present in existing rules (normalized string similarity).
     */
    fun isDuplicateRule(existingRules: List<BehaviorRule>, newRuleText: String): Boolean {
        val normNew = normalizeText(newRuleText)
        if (normNew.isBlank()) return true
        return existingRules.any { existing ->
            val normExisting = normalizeText(existing.rule)
            normExisting == normNew ||
                    normExisting.contains(normNew) ||
                    normNew.contains(normExisting) && Math.abs(normNew.length - normExisting.length) < 8
        }
    }

    /**
     * Checks if an item input trigger is already present in existing items.
     */
    fun isDuplicateItem(existingItems: List<TrainingItem>, input: String): Boolean {
        val normNew = normalizeText(input)
        if (normNew.isBlank()) return false
        return existingItems.any { normalizeText(it.userInput) == normNew }
    }

    private fun normalizeText(text: String): String {
        return text.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9\\u0980-\\u09FF]"), "")
            .trim()
    }
}
