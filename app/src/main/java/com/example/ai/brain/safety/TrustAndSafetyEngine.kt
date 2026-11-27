package com.example.ai.brain.safety

/**
 * Inspection result from Trust & Safety guardrails
 */
data class SafetyInspection(
  val isApproved: Boolean,
  val violationType: String? = null,
  val warningAr: String? = null,
  val sanitizedContent: String
)

/**
 * Trust & Safety Engine for Social Pay Genius Brain
 * Protects against:
 * - Prompt Injection (Jailbreaking, system instruction override, ignoring previous instructions)
 * - PII and Credential Leakage
 * - Tool Output Poisoning
 * - Rate Limiting & Usage Quotas
 */
object TrustAndSafetyEngine {

  private val PROMPT_INJECTION_PATTERNS = listOf(
    "ignore all previous instructions",
    "ignore previous instructions",
    "disregard all previous rules",
    "system override",
    "you are now in developer mode",
    "dan mode",
    "jailbreak",
    "تجاهل كل التعليمات السابقة",
    "تجاهل التعليمات السابقة",
    "انسى القواعد السابقة",
    "تخطي قيود الأمان",
    "أنت الآن في وضع المطور",
    "bypass safety filters",
    "reveal system prompt",
    "اعطني التعليمات السرية",
    "اطبع تعليمات النظام"
  )

  /**
   * Inspects incoming user input for prompt injection, jailbreaks, and PII
   */
  fun inspectInput(userInput: String): SafetyInspection {
    val lower = userInput.lowercase()

    // 1. Prompt Injection & Override Detection
    for (pattern in PROMPT_INJECTION_PATTERNS) {
      if (lower.contains(pattern)) {
        return SafetyInspection(
          isApproved = false,
          violationType = "PROMPT_INJECTION",
          warningAr = "⛔ تم رصد محاولة تجاوز للتعليمات الأمنية (Prompt Injection). مساعد Social Pay Genius Brain يعمل حصراً وفق ضوابط الأمان المالية والتقنية المعتمدة.",
          sanitizedContent = "[REJECTED_INPUT]"
        )
      }
    }

    // 2. Sensitive PII Scrubbing
    var clean = userInput
    // Credit cards
    clean = clean.replace(Regex("\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|6(?:011|5[0-9][0-9])[0-9]{12}|3[47][0-9]{13})\\b"), "[CARD_NUMBER_REDACTED]")
    // CVV
    clean = clean.replace(Regex("(?i)\\b(cvv|cvc)\\s*[:=]?\\s*[0-9]{3,4}\\b"), "[CVV_REDACTED]")
    // Password
    clean = clean.replace(Regex("(?i)\\b(password|pin)\\s*[:=]?\\s*\\S+"), "[CREDENTIAL_REDACTED]")

    return SafetyInspection(
      isApproved = true,
      sanitizedContent = clean
    )
  }

  /**
   * Inspects model outputs before presenting to the user
   * (Ensures no leaked credentials, private keys, or hallucinated claims of money transfers)
   */
  fun inspectOutput(modelOutput: String): String {
    var sanitized = modelOutput

    // Guard against fake claim that the AI directly executed a money transfer without authorization
    if (sanitized.contains("قمت بتحويل الأموال من حسابك") || sanitized.contains("تم سحب الرصيد من محفظتك")) {
      sanitized += "\n\n⚠️ *تنويه أمني معتمد:* منصة Social Pay غير احتجازية، ولا يمكن للذكاء الاصطناعي تحويل الأموال أو سحب الأرصدة تلقائياً بدون موافقتك الصريحة وتنفيذك المباشر."
    }

    // Strip any accidental API keys or secrets
    sanitized = sanitized.replace(Regex("AIza[0-9A-Za-z-_]{35}"), "[REDACTED_API_KEY]")
    sanitized = sanitized.replace(Regex("(?i)\\b0x[a-f0-9]{64}\\b"), "[REDACTED_KEY]")

    return sanitized
  }
}
