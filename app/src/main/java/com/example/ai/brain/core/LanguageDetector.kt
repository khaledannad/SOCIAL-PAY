package com.example.ai.brain.core

/**
 * Recognized languages & dialects in Social Pay Genius Brain
 */
enum class DetectedLanguage(
  val code: String,
  val displayNameAr: String,
  val displayNameEn: String,
  val systemPromptTag: String
) {
  ARABIC_MSA("ar_msa", "العربية الفصحى", "Modern Standard Arabic", "اللغة العربية الفصحى الأنيقة والدقيقة"),
  ALGERIAN_DARIJA("ar_dz", "الدارجة الجزائرية", "Algerian Darija", "الدارجة الجزائرية الأصيلة والمفهومة"),
  ENGLISH("en", "الإنجليزية", "English", "English language with precise financial clarity"),
  FRENCH("fr", "الفرنسية", "French", "Langue française avec clarté financière et technique"),
  AUTO_DETECT("auto", "اكتشاف تلقائي", "Auto Detected", "تلقائي حسب لغة المدخلات")
}

object LanguageDetector {

  private val algerianDarijaKeywords = setOf(
    "واش", "راك", "لاباس", "شحال", "بزاف", "كاش", "نحب", "نقدر", "برك", "دير", "كاين", "ماكاش",
    "مليح", "صحيت", "يعطيك الصحة", "هدرة", "علاش", "وقتاش", "وين", "حبيت", "شريت", "بعت", "سواردي",
    "دراهم", "حسابي", "كيفاش", "بلا مزية", "ماشي", "قاع", "هكذا", "رايح"
  )

  private val frenchKeywords = setOf(
    "bonjour", "salut", "merci", "combien", "frais", "facture", "paiement", "compte",
    "transaction", "argent", "comment", "pourquoi", "quand", "virement", "sécurité",
    "quels", "quel", "quelle", "quelles", "sont", "réseaux", "réseau", "pris", "charge",
    "pouvez", "système", "séquestre", "sans", "conservation", "contre", "fraude"
  )

  private val englishKeywords = setOf(
    "hello", "hi", "how", "what", "fee", "fees", "invoice", "payment", "escrow", "crypto",
    "bank", "transfer", "status", "cancel", "help", "support", "rate", "usdt", "smart"
  )

  /**
   * Detects the language/dialect of user input text with high precision.
   */
  fun detectLanguage(text: String): DetectedLanguage {
    val clean = text.trim().lowercase()
    if (clean.isEmpty()) return DetectedLanguage.ARABIC_MSA

    val words = clean.split(Regex("[\\s,?.!:;()\"'-]+")).filter { it.isNotBlank() }

    // Check for French (including French specific diacritics: é, è, ê, à, â, ç, etc.)
    val hasFrenchAccents = clean.any { it in "éèêëàâçùûîïô" }
    val frenchMatchCount = words.count { frenchKeywords.contains(it) }
    val frenchCommonWords = listOf("je", "tu", "il", "elle", "nous", "vous", "le", "la", "les", "des", "du", "sur", "par", "est", "sont", "pour", "avec", "dans", "une", "un", "ce", "cette", "en")
    val hasFrenchArticles = words.any { it in frenchCommonWords }
    val isLatinAlphabet = clean.any { it in 'a'..'z' || it in 'é'..'ë' || it in 'à'..'ù' }
    val hasArabicLetters = clean.any { it in '\u0600'..'\u06FF' }

    if (!hasArabicLetters && isLatinAlphabet) {
      if (hasFrenchAccents || (frenchMatchCount >= 1 && hasFrenchArticles) || frenchMatchCount >= 2) {
        return DetectedLanguage.FRENCH
      }

      // Check for English
      val englishMatchCount = words.count { englishKeywords.contains(it) }
      val isEnglishSentence = words.any { it in listOf("the", "is", "are", "can", "you", "my", "please", "i", "to", "for", "what", "how", "what's") }
      if (englishMatchCount >= 1 || isEnglishSentence) {
        return DetectedLanguage.ENGLISH
      }

      return DetectedLanguage.ENGLISH
    }

    // Check for Algerian Darija
    val hasDarijaKeyword = algerianDarijaKeywords.any { kw ->
      words.contains(kw) || clean.contains(" $kw ") || clean.startsWith("$kw ") || clean.endsWith(" $kw") || clean.contains(kw)
    }
    if (hasDarijaKeyword) {
      return DetectedLanguage.ALGERIAN_DARIJA
    }

    if (hasArabicLetters) {
      return DetectedLanguage.ARABIC_MSA
    }

    return DetectedLanguage.ENGLISH
  }


  /**
   * Preserves financial & technical glossary definitions regardless of language
   */
  fun formatSystemPromptLanguageDirective(detected: DetectedLanguage): String {
    return """
    أنت تتحدث بـ ${detected.systemPromptTag}.
    قواعد اللغة الصارمة:
    1. رد حصراً بنفس لغة المستخدم المكتشفة (${detected.displayNameAr} / ${detected.displayNameEn}).
    2. لا تخلط اللغات في نفس الإجابة إلا إذا طلب المستخدم صراحة الترجمة.
    3. حافظ على دقة المصطلحات المالية:
       - Escrow = أمانة العقد الذكي / Escrow
       - Non-Custodial = غير احتجازية (أموالك بيدك)
       - Flat Fee = رسوم ثابتة 0.80$
       - Anti-Triangulation = مكافحة احتيال التثليث
       - Web Embed = كود التضمين للمواقع
    """.trimIndent()
  }
}
