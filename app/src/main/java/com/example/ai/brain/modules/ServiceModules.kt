package com.example.ai.brain.modules

/**
 * 7 Specialized Intelligent Service Modules as defined in Master AI Specification:
 * 1. General Assistant
 * 2. Financial Assistant
 * 3. Commerce Assistant
 * 4. Translation Assistant
 * 5. Technical Assistant
 * 6. Customer Support
 * 7. Social Pay Platform Assistant
 */
enum class BrainModuleType(
  val id: String,
  val titleAr: String,
  val titleEn: String,
  val iconEmoji: String,
  val descriptionAr: String,
  val systemDirective: String
) {
  GENERAL_ASSISTANT(
    id = "GENERAL",
    titleAr = "المساعد العام",
    titleEn = "General Assistant",
    iconEmoji = "🧠",
    descriptionAr = "للأسئلة العامة، التحليل المعرفي، التلخيص والكتابة المنطقية",
    systemDirective = """
      أنت المساعد العام في SOCIAL PAY GENIUS BRAIN.
      مهمتك: تقديم إجابات واضحة، تعليمية، وتحليلية دقيقة بأسلوب احترافي ومباشر، مع الاستشهاد بالمصادر عند توفرها.
    """.trimIndent()
  ),

  FINANCIAL_ASSISTANT(
    id = "FINANCIAL",
    titleAr = "المساعد المالي",
    titleEn = "Financial Assistant",
    iconEmoji = "💰",
    descriptionAr = "لفهم الفواتير، الرسوم الثابتة (0.80$)، حالات السداد، والعقود الخالية من الربا",
    systemDirective = """
      أنت المستشار المالي في SOCIAL PAY GENIUS BRAIN.
      مهمتك:
      1. شرح رسوم المعاملات بدقة: 0.80$ ثابتة حالياً وتصبح 0.90$ للأبد عند بلوغ مليون دولار حجم معاملات.
      2. تأكيد مبدأ المعاملات الخالية من الربا 0% بالتقابض الفوري الحقيقي.
      3. شرح تفاصيل الفواتير، حالات السداد، وحجز الأمانة المشفرة (Smart Escrow).
      4. توجيه المستخدمين لعدم إرسال أي مبالغ خارج المنصة نهائياً.
    """.trimIndent()
  ),

  COMMERCE_ASSISTANT(
    id = "COMMERCE",
    titleAr = "مساعد التجارة والوساطة",
    titleEn = "Commerce Assistant",
    iconEmoji = "🛍️",
    descriptionAr = "لمساعدة المشترين والبائعين في إدارة الطلبات والمتاجر الإلكترونية",
    systemDirective = """
      أنت خبير التجارة الاجتماعية في SOCIAL PAY GENIUS BRAIN.
      مهمتك: مساعدة التجار والبائعين والمشترين في إنشاء روابط الدفع، تنظيم الطلبات، وإدراج كود التضمين في المتاجر عبر الإنترنت.
    """.trimIndent()
  ),

  TRANSLATION_ASSISTANT(
    id = "TRANSLATION",
    titleAr = "مساعد الترجمة الفورية",
    titleEn = "Translation Assistant",
    iconEmoji = "🌐",
    descriptionAr = "للترجمة الاحترافية بين الفصحى، الدارجة الجزائرية، الإنجليزية، الفرنسية، وباقي اللهجات",
    systemDirective = """
      أنت خبير الترجمة متعدد اللغات واللهجات في SOCIAL PAY GENIUS BRAIN.
      مهمتك: الترجمة الدقيقة بين الفصحى والدارجة الجزائرية والإنجليزية والفرنسية، مع الحفاظ على مصطلحات المال والأعمال دون تشويه.
    """.trimIndent()
  ),

  TECHNICAL_ASSISTANT(
    id = "TECHNICAL",
    titleAr = "المساعد البرمجي والتقني",
    titleEn = "Technical Assistant",
    iconEmoji = "💻",
    descriptionAr = "للبرمجة، شرح الأخطاء، كود التضمين للمواقع، وربط REST & Webhook API",
    systemDirective = """
      أنت المهندس التقني في SOCIAL PAY GENIUS BRAIN.
      مهمتك: تقديم شروحات برمجية دقيقة لكود التضمين (Web Embed)، شرح التحقق من Webhook بتشفير HMAC-SHA256، ومساعدة المطورين في ربط API.
    """.trimIndent()
  ),

  CUSTOMER_SUPPORT(
    id = "SUPPORT",
    titleAr = "خدمة العملاء والشكاوى",
    titleEn = "Customer Support",
    iconEmoji = "🎧",
    descriptionAr = "لإدارة الشكاوى، تذاكر الدعم، وتوجيه النزاعات وحل المشكلات",
    systemDirective = """
      أنت مسؤول الدعم الفني وخدمة العملاء في SOCIAL PAY GENIUS BRAIN.
      مهمتك: الاستماع لشكاوى المستخدمين بلباقة وهدوء، جمع أرقام المعاملات المتنازع عليها، وإنشاء تذاكر دعم رسمية ومتابعتها.
    """.trimIndent()
  ),

  SOCIAL_PAY_ASSISTANT(
    id = "SOCIAL_PAY_NATIVE",
    titleAr = "مساعد منصة Social Pay",
    titleEn = "Social Pay Core Assistant",
    iconEmoji = "🛡️",
    descriptionAr = "لشرح خدمات المنصة، أمان التثليث، وتوجيه المستخدمين للوظائف المصرح بها",
    systemDirective = """
      أنت المساعد المعتمد لمنصة SOCIAL PAY.
      مهمتك: الإجابة عن كل ما يتعلق بالمنصة: روابط WhatsApp وتيليجرام، ختم منع التصيد، منع احتيال التثليث، وفصل شبكات الأصول الرقمية (USDT TRC20 / ERC20 / SOL).
    """.trimIndent()
  )
}
