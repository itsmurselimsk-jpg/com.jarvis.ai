package com.example.jarvis.voice

sealed class VoiceError(val userMessage: String, val technicalDetail: String = "") {
    class AuthenticationError(detail: String = "ElevenLabs authentication failed. Verify API key.") :
        VoiceError("Voice authentication issue. Response displayed in text.", detail)

    class RateLimitError(detail: String = "ElevenLabs request rate limit or character quota exceeded.") :
        VoiceError("Voice rate limit reached. Response displayed in text.", detail)

    class InvalidVoiceIdError(voiceId: String) :
        VoiceError("Voice profile unavailable. Response displayed in text.", "Invalid Voice ID: $voiceId")

    class NetworkError(detail: String = "Network connection failed.") :
        VoiceError("Voice network unavailable. Response displayed in text.", detail)

    class SynthesisError(detail: String = "Speech synthesis failed.") :
        VoiceError("Voice synthesis error. Response displayed in text.", detail)

    class PlaybackError(detail: String = "Audio playback failed.") :
        VoiceError("Audio playback issue. Response displayed in text.", detail)

    class Cancelled(detail: String = "Speech cancelled.") :
        VoiceError("Speech interrupted.", detail)
}
