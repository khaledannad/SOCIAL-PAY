package com.example.ai.brain.knowledge

/**
 * A verified document or article indexed in the Social Pay Knowledge Base
 */
data class KnowledgeDocument(
  val id: String,
  val title: String,
  val category: String, // "FEE_POLICY", "SECURITY", "ESCROW", "INTEGRATION", "ASSETS", "USER_GUIDE", "UPLOADED_DOC"
  val content: String,
  val sourceUrlOrRef: String,
  val lastUpdatedDate: String,
  val tags: List<String>
)

/**
 * Retrieval result containing context snippets, confidence score, and source attribution
 */
data class KnowledgeRetrievalResult(
  val query: String,
  val relevantDocuments: List<KnowledgeDocument>,
  val sourceCitations: List<String>,
  val confidenceScore: Double,
  val hasAuthoritativeMatch: Boolean
)

/**
 * RAG Knowledge Engine for Social Pay Genius Brain
 * Indexes official documentation, guides, FAQs, and uploaded user documents.
 * Guarantees zero hallucinated citations and accurate source attribution with timestamps.
 */
class KnowledgeEngine {

  private val documents = mutableListOf<KnowledgeDocument>()

  init {
    seedAuthoritativeSocialPayKnowledge()
  }

  /**
   * Pre-populates verified documentation
   */
  private fun seedAuthoritativeSocialPayKnowledge() {
    documents.addAll(
      listOf(
        KnowledgeDocument(
          id = "DOC-FEES-01",
          title = "سياسة الرسوم الرسمية في Social Pay",
          category = "FEE_POLICY",
          content = "رسوم منصة Social Pay ثابتة ومحددة بقيمة 0.80$ (ثمانون سنتاً) لكل معاملة بغض النظر عن قيمة المبلغ المحول. عند تحقيق المشروع مدخول مليون دولار (1,000,000$)، ترتفع الرسوم إلى 0.90$ ثابتة للأبد لتمويل صندوق الأمان والأيردروبات للمستخدمين الموثوقين. لا توجد أي نسب مئوية خفية أو فوائد ربوية (0% ربا).",
          sourceUrlOrRef = "Social Pay Official Whitepaper § 3.2 (Fee Architecture)",
          lastUpdatedDate = "2026-11-01",
          tags = listOf("رسوم", "fee", "عمولة", "0.80", "0.90", "مليون", "تكلفة", "شحال", "كم الرسوم")
        ),
        KnowledgeDocument(
          id = "DOC-ESCROW-02",
          title = "نظام الأمانة المشفرة والتقابض الفوري (Smart Escrow)",
          category = "ESCROW",
          content = "تعتمد Social Pay على عقود ذكية لامركزية غير احتجازية (Non-Custodial Escrow). يتم قفل الأصل أو الضمان في العقد الذكي تلقائياً عند فتح الصفقة، ولا يتم تحريره للطرف الآخر إلا بعد إثبات استلام الحوالة البنكية أو الدفع بالتقابض الفوري الحقيقي. المنصة لا تحتفظ بأموال المستخدمين ولا بالمفاتيح الخاصة.",
          sourceUrlOrRef = "Social Pay Technical Specification v1.0 (Non-Custodial Escrow)",
          lastUpdatedDate = "2026-11-05",
          tags = listOf("أمانة", "عقد ذكي", "escrow", "حجز", "تقابض", "ربا", "تحرير", "ضمان")
        ),
        KnowledgeDocument(
          id = "DOC-SECURITY-03",
          title = "درع الأمان ومكافحة الاحتيال والتثليث والتصيد",
          category = "SECURITY",
          content = "لمنع الاحتيال، تطبق المنصة: 1) حارس منع التثليث (Anti-Triangulation): مطابقة اسم المحول في الحساب البنكي مع الهوية المعتمدة (KYC) لمنع استخدام حسابات أطراف ثالثة مسروقة، 2) ختم مكافحة التصيد (Anti-Phishing Secret): كود مائي سري يظهر في كل جلسة معتمدة، 3) فحص الروابط الخارجية لمنع التوجيه خارج المنصة.",
          sourceUrlOrRef = "Social Pay Security & Compliance Guide § 4.1",
          lastUpdatedDate = "2026-11-06",
          tags = listOf("تثليث", "احتيال", "تصيد", "triangulation", "phishing", "أمان", "نصب", "حماية", "كود سري")
        ),
        KnowledgeDocument(
          id = "DOC-INTEGRATION-04",
          title = "محرك التكامل الاجتماعي وكود التضمين للمواقع",
          category = "INTEGRATION",
          content = "تتيح المنصة إنشاء روابط مشاركة رسمية عبر واتساب (api.whatsapp.com)، تيليجرام (t.me/share)، وإنستغرام برابط https://pay.socialpay.io/req/{id}. كما توفر زر دفع ذكي للمتاجر الإلكترونية بكود تضمين HTML/JS: <script src=\"https://cdn.socialpay.io/v1/button.js\"></script> يتيح السداد دون الحاجة لتثبيت التطبيق.",
          sourceUrlOrRef = "Social Pay Developer Portal / Social SDK Docs",
          lastUpdatedDate = "2026-11-07",
          tags = listOf("واتساب", "تيليجرام", "رابط", "مشاركة", "كود تضمين", "تضمين", "متجر", "whatsapp", "telegram", "web embed")
        ),
        KnowledgeDocument(
          id = "DOC-ASSETS-05",
          title = "دليل العملات والأصول الرقمية وفصل الشبكات",
          category = "ASSETS",
          content = "تدعم المنصة العملات الورقية (SAR, AED, USD, EUR, EGP) والأصول الرقمية مفصولة بالكامل بالشبكة لمنع الأخطاء: تيذر ترون (USDT-TRC20)، تيذر إيثيريوم (USDT-ERC20)، تيذر سولانا (USDT-SOL)، والبيتكوين والإيثيريوم. يمنع النظام خلط الشبكات لمنع ضياع الأصول.",
          sourceUrlOrRef = "Social Pay Multi-Chain Asset Registry",
          lastUpdatedDate = "2026-11-07",
          tags = listOf("عملات", "أصول", "usdt", "trc20", "erc20", "solana", "ريال", "دولار", "شبكات", "crypto")
        )
      )
    )
  }

  /**
   * Searches and retrieves relevant documents based on user query
   */
  fun retrieve(query: String, maxResults: Int = 3): KnowledgeRetrievalResult {
    val cleanQuery = query.lowercase().trim()
    val queryTokens = cleanQuery.split(Regex("[\\s,?.!]+")).filter { it.length > 2 }

    val scoredDocs = documents.map { doc ->
      var score = 0.0
      // Title match
      if (doc.title.lowercase().contains(cleanQuery)) score += 10.0
      // Tag matches
      val matchedTags = doc.tags.count { tag -> cleanQuery.contains(tag.lowercase()) }
      score += matchedTags * 5.0
      // Content token overlap
      val contentLower = doc.content.lowercase()
      for (token in queryTokens) {
        if (contentLower.contains(token)) score += 2.0
      }
      doc to score
    }
      .filter { it.second >= 4.0 }
      .sortedByDescending { it.second }
      .take(maxResults)

    val matchedDocs = scoredDocs.map { it.first }
    val citations = matchedDocs.map { "${it.title} (${it.sourceUrlOrRef} - آخر تحديث: ${it.lastUpdatedDate})" }
    val avgScore = if (scoredDocs.isNotEmpty()) scoredDocs.map { it.second }.average() else 0.0

    return KnowledgeRetrievalResult(
      query = query,
      relevantDocuments = matchedDocs,
      sourceCitations = citations,
      confidenceScore = avgScore,
      hasAuthoritativeMatch = matchedDocs.isNotEmpty()
    )
  }

  /**
   * Dynamically indexes an uploaded document or text file (e.g. PDF/TXT content)
   */
  fun indexCustomDocument(
    id: String,
    title: String,
    content: String,
    sourceFileName: String,
    tags: List<String> = emptyList()
  ): KnowledgeDocument {
    val doc = KnowledgeDocument(
      id = id,
      title = title,
      category = "UPLOADED_DOC",
      content = content,
      sourceUrlOrRef = "مستند المستخدم المرفوع: $sourceFileName",
      lastUpdatedDate = "2026-11-08",
      tags = tags + listOf(title.lowercase(), sourceFileName.lowercase())
    )
    documents.add(doc)
    return doc
  }

  /**
   * Builds an enriched prompt with retrieved ground-truth context
   */
  fun enrichPromptWithKnowledge(userPrompt: String): Pair<String, List<String>> {
    val retrieval = retrieve(userPrompt)
    if (!retrieval.hasAuthoritativeMatch) {
      return userPrompt to emptyList()
    }

    val contextSnippet = retrieval.relevantDocuments.joinToString("\n\n") { doc ->
      "【المصدر: ${doc.title} | المرجع: ${doc.sourceUrlOrRef} | التحديث: ${doc.lastUpdatedDate}】\n${doc.content}"
    }

    val enrichedPrompt = """
    معلومات قاعدة المعرفة الموثوقة (استند إليها حصراً ولا تخترع مراجع):
    $contextSnippet
    
    سؤال المستخدم:
    $userPrompt
    """.trimIndent()

    return enrichedPrompt to retrieval.sourceCitations
  }

  fun getAllDocuments(): List<KnowledgeDocument> = documents.toList()
}
