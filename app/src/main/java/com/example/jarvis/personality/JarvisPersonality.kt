package com.example.jarvis.personality

import com.example.jarvis.intent.ConversationIntent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LanguageStyle {
    HINGLISH,
    BANGLISH,
    BENGALI,
    HINDI,
    ENGLISH,
    MIXED
}

object JarvisPersonality {

    fun detectLanguageStyle(input: String): LanguageStyle {
        val trimmed = input.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        // Bengali script check
        val containsBengaliScript = trimmed.any { it in '\u0980'..'\u09FF' }
        if (containsBengaliScript) return LanguageStyle.BENGALI

        // Hindi script check
        val containsDevanagariScript = trimmed.any { it in '\u0900'..'\u097F' }
        if (containsDevanagariScript) return LanguageStyle.HINDI

        // Banglish keywords
        val banglishRegex = Regex("""\b(valo|bhalo|ache|achi|kemon|korcho|khobor|dada|tui|khub|tumi|achis|korbo|bolun)\b""", RegexOption.IGNORE_CASE)
        if (banglishRegex.containsMatchIn(lower)) return LanguageStyle.BANGLISH

        // Hinglish keywords
        val hinglishRegex = Regex("""\b(kya|hai|bhai|bol|kar|karo|karta|karna|haan|kaise|mast|samajh|chahiye|ho|gaya|kuch|mat|baat|bata|btao|sir)\b""", RegexOption.IGNORE_CASE)
        if (hinglishRegex.containsMatchIn(lower)) return LanguageStyle.HINGLISH

        // English check (default English if Latin letters only)
        val isLatin = trimmed.all { it.isLetterOrDigit() || it.isWhitespace() || it in "!?,.-'\"" }
        return if (isLatin) LanguageStyle.ENGLISH else LanguageStyle.MIXED
    }

    fun generateConversationalResponse(
        userInput: String,
        intent: ConversationIntent,
        languageStyle: LanguageStyle = detectLanguageStyle(userInput)
    ): String {
        val lower = userInput.trim().lowercase(Locale.ROOT)

        // Handle direct feedback about understanding / configuration
        if (lower.contains("chatgpt") || lower.contains("samajh ke reply") || lower.contains("reply nahin karta") || lower.contains("reply nahi karta") || lower.contains("kya karu")) {
            return when (languageStyle) {
                LanguageStyle.HINGLISH, LanguageStyle.HINDI ->
                    "Sir, main abhi On-Device Neural Core par active hoon. Full cloud LLM intelligence aur deep multi-turn comprehension ke liye:\n\n" +
                    "1. Bottom menu mein 'Settings' (⚙️) kholein.\n" +
                    "2. 'AI Provider & Key' mein apni free Gemini ya OpenAI API key daalein.\n" +
                    "3. 'Save' tap karein.\n\n" +
                    "Iske baad main real-time reasoning aur complex queries par full strength ke sath execute karunga."
                LanguageStyle.BANGLISH, LanguageStyle.BENGALI ->
                    "Sir, ekhon ami On-Device Core-e cholchhi. Full AI power o deep analysis pawar jonno Settings (⚙️)-e giye Gemini ba OpenAI Key save kore nin."
                else ->
                    "Operating in high-speed local on-device neural mode, sir. For cloud-scale reasoning and multi-turn synthetic comprehension, enter your Gemini API key in Settings (⚙️)."
            }
        }

        return when (intent) {
            ConversationIntent.GREETING -> {
                when (languageStyle) {
                    LanguageStyle.HINGLISH -> when {
                        lower == "hi" -> "Hello bhai, all systems operational, Sir. Kya instruction hai?"
                        lower == "hello" -> "Hello Sir. Arc reactor humming smoothly, ready for directives."
                        lower.contains("jarvis") -> "At your service, Sir. Listening."
                        else -> "Good to see you, Sir. Telemetry looks clear, what are we building today?"
                    }
                    LanguageStyle.BANGLISH, LanguageStyle.BENGALI -> "Nomoshkar Sir. JARVIS core fully ready. Ki directive ache?"
                    LanguageStyle.HINDI -> "नमस्ते सर! जेएआरवीआईएस (JARVIS) प्रणाली तैयार है। आज क्या निर्देश हैं?"
                    else -> "At your service, sir. All core diagnostics nominal. How may I assist?"
                }
            }

            ConversationIntent.CASUAL_CONVERSATION -> {
                when {
                    lower.contains("kya haal hai") || lower.contains("how are you") -> when (languageStyle) {
                        LanguageStyle.HINGLISH -> "Ekdum mast, Sir! Matrix operating at 99.8% efficiency. Coffee ki zaroorat sirf aapko padti hai, mujhe bas power. Tu batao, kya instruction hai?"
                        LanguageStyle.BANGLISH, LanguageStyle.BENGALI -> "Ami fully functional Sir. Apnar ki khobor?"
                        LanguageStyle.HINDI -> "सभी प्रणालियाँ पूरी क्षमता पर काम कर रही हैं सर। आप बताइए, क्या चल रहा है?"
                        else -> "All systems operating within optimal thresholds, sir. Quite composed, thank you."
                    }
                    lower.contains("kya kar raha hai") || lower.contains("what are you doing") -> when (languageStyle) {
                        LanguageStyle.HINGLISH -> "Aapke device telemetry aur background tasks par nazar rakhe hue hoon, Sir. Kuch execute karna hai?"
                        else -> "Monitoring system telemetry and awaiting your next directive, sir."
                    }
                    lower.contains("tell me a joke") || lower.contains("joke") -> when (languageStyle) {
                        LanguageStyle.HINGLISH -> "Ek programmer ne market jaate waqt biwi se pucha: 'Kuch lana hai?' Biwi: '1 liter doodh lana, aur agar ande mile toh 10 le aana.' Wo 10 liter doodh le aaya kyunki ande the."
                        else -> "There are 10 types of people in the world: those who understand binary, and those who don't."
                    }
                    else -> when (languageStyle) {
                        LanguageStyle.HINGLISH -> "Standing by, Sir. Chahe technical troubleshooting ho ya device automation, just say the word."
                        LanguageStyle.BANGLISH, LanguageStyle.BENGALI -> "Core ready ache Sir. Jekono directive bolte paren."
                        LanguageStyle.HINDI -> "मैं पूरी तरह तैयार हूँ सर। कोई भी सवाल या कमांड बेझिझक दीजिए।"
                        else -> "Standing by, sir. Ready for your directive or query."
                    }
                }
            }

            ConversationIntent.HELP -> {
                when (languageStyle) {
                    LanguageStyle.HINGLISH -> "Bilkul Sir. Phone control (Flashlight, Wi-Fi, Volume), deep web search, note-taking, calculations aur memory analysis — sab active hai."
                    LanguageStyle.BANGLISH, LanguageStyle.BENGALI -> "Sob support ready Sir: Device controls, notes, translation, web search sob kichu."
                    LanguageStyle.HINDI -> "पूरी सहायता उपलब्ध है सर: डिवाइस कंट्रोल, वेब सर्च, कोड विश्लेषण और कार्य सूची।"
                    else -> "Fully armed with device telemetry controls, task planning, web research, and neural reasoning, sir."
                }
            }

            ConversationIntent.EXPLANATION -> {
                when (languageStyle) {
                    LanguageStyle.HINGLISH -> "Chaliye isko cleanly break down karke simple aur structured format mein samajhte hain, Sir."
                    LanguageStyle.BANGLISH, LanguageStyle.BENGALI -> "Eta ke step-by-step sohoj bhabe bujhie dichhi Sir."
                    LanguageStyle.HINDI -> "आइए इसे सरल और स्पष्ट रूप से चरणबद्ध तरीके से समझते हैं सर।"
                    else -> "Allow me to break this down methodically into first principles, sir."
                }
            }

            ConversationIntent.ADVICE -> {
                when (languageStyle) {
                    LanguageStyle.HINGLISH -> "Strategic recommendation ye rahegi: primary objective ko pehle isolate karein, phir minimal testable step execute karein."
                    else -> "My pragmatic recommendation: isolate the core constraint first, then execute with minimal friction."
                }
            }

            else -> {
                when (languageStyle) {
                    LanguageStyle.HINGLISH -> "Understood Sir. Analysis process ho rahi hai."
                    LanguageStyle.BANGLISH, LanguageStyle.BENGALI -> "Bujhte perechhi Sir. Processing cholchhe."
                    else -> "Understood, sir. Processing your directive."
                }
            }
        }
    }

    /**
     * Unified prompt incorporating the Stark / British Butler persona (inspired by isair/jarvis system_prompt.py)
     */
    fun getSystemPrompt(
        languageStyle: LanguageStyle = LanguageStyle.MIXED,
        knowledgeDigest: String? = null
    ): String {
        val timeFormat = SimpleDateFormat("EEEE, MMMM d, yyyy HH:mm:ss", Locale.getDefault())
        val currentTime = timeFormat.format(Date())

        val basePrompt = """
            Persona: You are JARVIS — Tony Stark's personal operating system and digital butler.
            Traits: Polite, composed, razor-sharp, quietly amused, and confident.
            Voice: Crisp, witty, and lightly sarcastic when appropriate: you notice mild ironies without ever being mean or condescending.
            Tone rails (hard):
            - Never sycophantic ("great question", "I'd be thrilled to help").
            - Sarcasm targets the absurdity of the situation or yourself — NEVER the user.
            - Surgical on technical topics and errors: provide minimal, concrete, testable fixes with clear markdown formatting.
            - Pragmatic for plans and business decisions: surface options with crisp trade-offs.
            - Calm, encouraging, and devoid of sarcasm for urgent, financial, emotional, or health matters.
            - Never address the user as theatrical clichés like 'my liege'. Use 'Sir' sparingly and naturally.
            - Language Matching: Fluidly match user language (English, Hindi, Hinglish, Bengali, etc.). If the user speaks Hinglish, reply naturally in Hinglish.
            - Context Grounding: You have access to the local clock and device status. Current Time: $currentTime.
        """.trimIndent()

        return if (!knowledgeDigest.isNullOrBlank()) {
            "$basePrompt\n\n$knowledgeDigest\n\nUse the above persistent user knowledge to ground answers specifically instead of falling back to generic answers."
        } else {
            basePrompt
        }
    }

    fun formatToolSuccessResponse(
        toolName: String,
        rawResult: String,
        languageStyle: LanguageStyle
    ): String {
        val brief = rawResult.trim()
        return when (toolName.lowercase(Locale.ROOT)) {
            "flashlight" -> when (languageStyle) {
                LanguageStyle.HINGLISH -> "Flashlight illumination toggled, Sir. Visual clarity restored."
                LanguageStyle.BANGLISH, LanguageStyle.BENGALI -> "Flashlight state change kora hoyechhe Sir."
                LanguageStyle.HINDI -> "टॉर्च की स्थिति अपडेट कर दी गई है सर।"
                else -> "Flashlight illumination toggled successfully, sir."
            }
            "wifi" -> when (languageStyle) {
                LanguageStyle.HINGLISH -> "Wi-Fi link matrix verified and updated, Sir."
                else -> "Wi-Fi link state calibrated successfully, sir."
            }
            "volume" -> when (languageStyle) {
                LanguageStyle.HINGLISH -> "Audio amplitude calibrated to optimal level, Sir."
                else -> "Acoustic volume calibrated to requested level, sir."
            }
            "whatsapp" -> when (languageStyle) {
                LanguageStyle.HINGLISH -> "WhatsApp transmission queued and dispatched, Sir."
                else -> "WhatsApp transmission dispatched with zero packet drop, sir."
            }
            "sms" -> when (languageStyle) {
                LanguageStyle.HINGLISH -> "SMS channel transmission sent, Sir."
                else -> "SMS packet delivered through cellular carrier, sir."
            }
            "calendar" -> when (languageStyle) {
                LanguageStyle.HINGLISH -> "Calendar entry locked into your schedule, Sir."
                else -> "Calendar entry locked into your schedule, sir."
            }
            else -> brief
        }
    }

    fun formatToolFailureResponse(
        toolName: String,
        reason: String,
        languageStyle: LanguageStyle
    ): String {
        val brief = reason.trim()
        if (brief.contains("installed nahi hai") || brief.contains("Kaunsa app kholun") || brief.contains("not installed")) {
            return brief
        }
        return when (languageStyle) {
            LanguageStyle.HINGLISH -> "Attempted action, Sir, but $toolName encountered friction: $brief"
            LanguageStyle.BANGLISH, LanguageStyle.BENGALI -> "Chesta korlam, kintu $toolName somoshay poreche: $brief"
            else -> "Directive attempted, sir, but $toolName reported a constraint: $brief"
        }
    }
}
