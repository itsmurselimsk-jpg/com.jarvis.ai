package com.example.jarvis.training

object TrainingValidation {

    private val FORBIDDEN_EXEC_PATTERNS = listOf(
        Regex("""(?i)\b(rm\s+-rf|sudo\s+|chmod\s+|chown\s+|mkfs|dd\s+if=|;\s*sh\b|;\s*bash\b)"""),
        Regex("""(?i)\b(su\s+-c|eval\(|exec\(|Runtime\.getRuntime)"""),
        Regex("""(?i)(<script>|javascript:|base64_decode)""")
    )

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )

    fun validateItem(item: TrainingItem): ValidationResult {
        if (item.userInput.isBlank() && item.type != TrainingType.BEHAVIOR) {
            return ValidationResult(false, "User input pattern cannot be empty for non-behavior types.")
        }

        // Check for disallowed execution injections
        for (pattern in FORBIDDEN_EXEC_PATTERNS) {
            if (pattern.containsMatchIn(item.userInput) ||
                item.goodResponses.any { pattern.containsMatchIn(it) } ||
                item.behaviorRules.any { pattern.containsMatchIn(it) }
            ) {
                return ValidationResult(false, "Security violation: disallowed execution payload detected in training item.")
            }
        }

        return ValidationResult(true)
    }

    fun validateBundle(bundle: TrainingBundle): ValidationResult {
        if (bundle.version < 1) {
            return ValidationResult(false, "Training version must be greater than or equal to 1.")
        }

        for (item in bundle.items) {
            val res = validateItem(item)
            if (!res.isValid) return res
        }

        for (rule in bundle.rules) {
            for (pattern in FORBIDDEN_EXEC_PATTERNS) {
                if (pattern.containsMatchIn(rule.rule)) {
                    return ValidationResult(false, "Security violation: rule contains forbidden syntax.")
                }
            }
        }

        return ValidationResult(true)
    }
}
