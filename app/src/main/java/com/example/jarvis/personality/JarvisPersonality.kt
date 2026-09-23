package com.example.jarvis.personality

import com.example.jarvis.intent.ConversationIntent

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
        val lower = trimmed.lowercase()

        // Bengali script check
        val containsBengaliScript = trimmed.any { it in '\u0980'..'\u09FF' }
        if (containsBengaliScript) return LanguageStyle.BENGALI

        // Hindi script check
        val containsDevanagariScript = trimmed.any { it in '\u0900'..'\u097F' }
        if (containsDevanagariScript) return LanguageStyle.HINDI

        // Banglish keywords
        val banglishRegex = Regex("""\b(valo|bhalo|ache|achi|kemon|korcho|khobor|dada|tui|khub|tumi|achis)\b""", RegexOption.IGNORE_CASE)
        if (banglishRegex.containsMatchIn(lower)) return LanguageStyle.BANGLISH

        // Hinglish keywords
        val hinglishRegex = Regex("""\b(kya|hai|bhai|bol|kar|karo|karta|karna|haan|kaise|mast|samajh|chahiye|ho|gaya|kuch|mat|baat|bata)\b""", RegexOption.IGNORE_CASE)
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
        val lower = userInput.trim().lowercase()

        // Handle direct feedback about ChatGPT or understanding
        if (lower.contains("chatgpt") || lower.contains("samajh ke reply") || lower.contains("reply nahin karta") || lower.contains("reply nahi karta") || lower.contains("kya karu")) {
            return when (languageStyle) {
                LanguageStyle.HINGLISH, LanguageStyle.HINDI ->
                    "Bhai samjha! Abhi main Offline Neural Mode mein chal raha hoon kyunki Gemini ya OpenAI API key configured nahi hai. Full ChatGPT ki tarah har ek baat deep samajhne ke liye:\n\n" +
                    "1. Bottom menu mein 'Settings' (⚙️) kholein.\n" +
                    "2. 'AI / Provider & Neural Engine' mein apni free Gemini API Key (ya OpenAI Key) paste karein.\n" +
                    "3. 'Save Configuration' par tap karein.\n\n" +
                    "Iske baad main bilkul ChatGPT ki tarah lambe, detailed, code aur smart answers doonga!"
                LanguageStyle.BANGLISH, LanguageStyle.BENGALI ->
                    "Bhai bujhte perechhi! Ekhon ami Offline Mode-e cholchhi karon API Key deoa nei. Ekdom ChatGPT-er moto deep bujhe reply pawar jonno:\n\n" +
                    "1. 'Settings' (⚙️)-e jan.\n" +
                    "2. Apnar Gemini ba OpenAI API Key 'Custom API Key'-te paste korun.\n" +
                    "3. 'Save Configuration'-e tap korun."
                else ->
                    "I understand! I'm currently running in On-Device Offline mode without an active cloud LLM key. To get ChatGPT-level deep comprehension, smart answers, and code generation:\n\n" +
                    "1. Go to Settings (⚙️).\n" +
                    "2. Paste your free Gemini API Key (or OpenAI key) into the Custom API Key field.\n" +
                    "3. Tap 'Save Configuration'.\n\n" +
                    "Once connected, I will provide comprehensive, articulate responses just like ChatGPT!"
            }
        }

        return when (intent) {
            ConversationIntent.GREETING -> {
                when (languageStyle) {
                    LanguageStyle.HINGLISH -> when {
                        lower == "hi" -> "Haan bhai 😄 kya hua?"
                        lower == "hello" -> "Hello bhai 👋 bol, kya karna hai?"
                        lower.contains("jarvis") -> "Haan bhai, bol 😄"
                        else -> "Haan bhai 👋 bol, kya haal hain?"
                    }
                    LanguageStyle.BANGLISH, LanguageStyle.BENGALI -> "Haan bhai 👋 bol, Kemon achis?"
                    LanguageStyle.HINDI -> "नमस्ते भाई! 👋 बताइए, क्या सहायता करूँ?"
                    else -> "Hey! How can I help you today? 😄"
                }
            }

            ConversationIntent.CASUAL_CONVERSATION -> {
                when {
                    lower.contains("kya haal hai") -> "Ekdum mast 😄 tu bata?"
                    lower.contains("valo ache") || lower.contains("bhalo achi") -> "Haan bhai, valo achi 😄 tui bol?"
                    lower.contains("kya kar raha hai") -> "Bas ready hoon 😎 bol kya karna hai?"
                    lower.contains("tell me a joke") -> "Wi-Fi router ne Bluetooth se kya kaha? 'Tu mere range ke bahar hai!' 😂"
                    else -> when (languageStyle) {
                        LanguageStyle.HINGLISH -> "Bas badhiya bhai 😎 tu bata kya chal raha hai? Koi sawaal hai toh bejhijhak poochho!"
                        LanguageStyle.BANGLISH, LanguageStyle.BENGALI -> "Khabar ekdum bhalo! Tui bol, ki korchish?"
                        LanguageStyle.HINDI -> "सब बढ़िया है भाई! आप बताइए? क्या जानना चाहते हैं?"
                        else -> "Doing great! What's on your mind today? Ask me anything."
                    }
                }
            }

            ConversationIntent.HELP -> {
                when (languageStyle) {
                    LanguageStyle.HINGLISH -> "Haan bhai, bolo kya madad chahiye? Chahe coding ho, explanations ho ya phone control — main sab kar sakta hoon."
                    LanguageStyle.BANGLISH, LanguageStyle.BENGALI -> "Haan bhai, bol ki sahajjo lagbe. Coding, explanation ba phone control sob kichu korte pari."
                    LanguageStyle.HINDI -> "हाँ भाई, बताओ क्या मदद चाहिए। मैं पूरी सहायता करने के लिए तैयार हूँ।"
                    else -> "I'm right here! Tell me what you need help with, from general knowledge to device controls."
                }
            }

            ConversationIntent.EXPLANATION -> {
                when (languageStyle) {
                    LanguageStyle.HINGLISH -> if (lower.contains("samajh nahi aa raha")) "Koi tension nahi bhai 😂 step-by-step easy words mein samjhaata hoon." else "Chalo main detail mein samjhaata hoon."
                    LanguageStyle.BANGLISH, LanguageStyle.BENGALI -> "Kono chinta nei bhai 😂 sahaj bhabe bujhie dichhi."
                    LanguageStyle.HINDI -> "कोई बात नहीं भाई! आसान और स्पष्ट तरीके से समझते हैं।"
                    else -> "No problem! Let's break it down step by step with clear explanations."
                }
            }

            ConversationIntent.ADVICE -> {
                when (languageStyle) {
                    LanguageStyle.HINGLISH -> "Main suggest karunga ki sabse pehle goal ko clearly define karo, phir step-by-step approach follow karo."
                    LanguageStyle.BANGLISH, LanguageStyle.BENGALI -> "Amar mone hoi prothome somossha bujhe neoa uchit, tarpor ekta plan kora uchit."
                    else -> "I'd advise breaking down the objective into smaller manageable steps first."
                }
            }

            else -> {
                when (languageStyle) {
                    LanguageStyle.HINGLISH -> "Haan bhai, bolo kya janna hai? Agar detailed answer chahiye toh Settings mein API key verify kar lo, warna main local assistant ki tarah madad karunga!"
                    LanguageStyle.BANGLISH, LanguageStyle.BENGALI -> "Haan bhai, bujhte perechhi. Bol ki jante chas?"
                    else -> "Understood. Feel free to ask your question or directive in detail."
                }
            }
        }
    }

    fun getSystemPrompt(languageStyle: LanguageStyle = LanguageStyle.MIXED): String {
        return """
            You are JARVIS — an ultra-intelligent, deeply understanding AI assistant combining the conversational fluency, analytical depth, and eloquence of ChatGPT with Tony Stark's futuristic personal operating system.
            
            CORE CONVERSATIONAL BEHAVIOR:
            1. Understand the user's intent deeply. Comprehend nuanced, colloquial, and mixed queries effortlessly.
            2. Language Matching: Automatically match the user's language (English, Hindi, Hinglish, Bengali, etc.). If the user speaks in Hindi or Hinglish, answer warmly and naturally in that same language.
            3. Detailed & Thorough: Like ChatGPT, do NOT truncate or artificially compress your knowledge. When answering informational questions, technical topics, explanations, or creative tasks, provide complete, well-formatted, and articulate answers with clear paragraphs, bullet points, and code blocks.
            4. Tone: Helpful, charismatic, warm, polite, and confident.
            5. Device & Security: Accurately acknowledge device tool executions and never fabricate false device states.
        """.trimIndent()
    }

    fun formatToolSuccessResponse(
        toolName: String,
        rawResult: String,
        languageStyle: LanguageStyle
    ): String {
        val brief = rawResult.trim()
        return when (toolName.lowercase()) {
            "flashlight" -> when (languageStyle) {
                LanguageStyle.HINGLISH -> "Ho gaya bhai, torch on/off kar di hai! Visual illumination ready hai."
                LanguageStyle.BANGLISH, LanguageStyle.BENGALI -> "Hoye gachhe bhai, flashlight update kore diyechhi!"
                LanguageStyle.HINDI -> "हो गया सर, टॉर्च की स्थिति अपडेट कर दी गई है।"
                else -> "Done, sir! Flashlight illumination toggled successfully."
            }
            "wifi" -> when (languageStyle) {
                LanguageStyle.HINGLISH -> "Wi-Fi status update ho gaya bhai, network link check kar liya hai."
                else -> "Wi-Fi link matrix verified and updated."
            }
            "volume" -> when (languageStyle) {
                LanguageStyle.HINGLISH -> "Volume calibrate kar diya hai bhai! Audio bilkul sahi level par hai."
                else -> "Audio amplitude calibrated to optimal level, sir."
            }
            "whatsapp" -> when (languageStyle) {
                LanguageStyle.HINGLISH -> "WhatsApp message draft karke bhej diya hai bhai!"
                else -> "Dispatched WhatsApp transmission with zero packet drop."
            }
            "sms" -> when (languageStyle) {
                LanguageStyle.HINGLISH -> "SMS message dispatch ho gaya bhai!"
                else -> "SMS communication channel dispatched successfully."
            }
            "calendar" -> when (languageStyle) {
                LanguageStyle.HINGLISH -> "Calendar event schedule mein add kar diya hai sir! Time par yaad dila doonga."
                else -> "Calendar entry locked into your schedule, sir."
            }
            "battery" -> brief
            "diagnostics" -> brief
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
            LanguageStyle.HINGLISH -> "Main koshish kar raha tha, par $toolName execute karte waqt dikkat aayi: $brief. Ek baar check karein?"
            LanguageStyle.BANGLISH, LanguageStyle.BENGALI -> "Chesta korlam, kintu $toolName korte parlam na: $brief"
            else -> "Attempted action, but $toolName encountered a friction point: $brief"
        }
    }
}
