package com.example.ai.brain.core

import com.example.ai.brain.action.ActionResult
import com.example.ai.brain.action.BrainActionEngine
import com.example.ai.brain.action.PendingActionProposal
import com.example.ai.brain.knowledge.KnowledgeEngine
import com.example.ai.brain.memory.BrainMemoryManager
import com.example.ai.brain.modules.BrainModuleType
import com.example.ai.brain.realtime.RealTimeKnowledgeEngine
import com.example.ai.brain.safety.TrustAndSafetyEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * High-level response state emitted by Social Pay Genius Brain
 */
sealed class BrainResponse {
  data class Completed(
    val text: String,
    val detectedLanguage: DetectedLanguage,
    val sourceCitations: List<String>,
    val actionProposal: PendingActionProposal? = null,
    val executedActionResult: ActionResult? = null
  ) : BrainResponse()

  data class StreamChunk(val chunk: String) : BrainResponse()
  data class Error(val errorMessageAr: String) : BrainResponse()
}

/**
 * SOCIAL PAY GENIUS BRAIN — Orchestrator Engine
 * Integrates:
 * - Model Provider Adapters (Firebase AI Logic Gemini 3.8 Flash with graceful fallback)
 * - Multilingual Intelligence & Language Detection
 * - Multi-Level Memory (Short-Term, Long-Term, User Controls)
 * - RAG Knowledge Engine (Authoritative Social Pay docs & custom documents)
 * - 7 Specialized Intelligent Modules
 * - Action Engine (Function Calling with User Confirmation Gates)
 * - Real-Time Market & Blockchain Knowledge
 * - Trust & Safety Guardrails
 */
class SocialPayGeniusBrainEngine(
  val primaryAdapter: ModelProviderAdapter = FirebaseAiModelAdapter(),
  val fallbackAdapter: ModelProviderAdapter = LocalFallbackModelAdapter(),
  val memoryManager: BrainMemoryManager = BrainMemoryManager(),
  val knowledgeEngine: KnowledgeEngine = KnowledgeEngine(),
  val actionEngine: BrainActionEngine = BrainActionEngine()
) {

  var activeModule: BrainModuleType = BrainModuleType.SOCIAL_PAY_ASSISTANT
  var activeLanguage: DetectedLanguage = DetectedLanguage.AUTO_DETECT

  /**
   * Generates response with full context reasoning, safety filtering, and action execution
   */
  suspend fun processUserMessage(
    userInput: String,
    userId: String,
    sessionId: String = memoryManager.getActiveSessionId(userId)
  ): BrainResponse {
    // 1. Safety Guardrail & Prompt Injection Scan
    val safety = TrustAndSafetyEngine.inspectInput(userInput)
    if (!safety.isApproved) {
      val errorMsg = safety.warningAr ?: "تم حظر المدخلات لمخالفتها معايير الأمان."
      memoryManager.appendMessage(userId, sessionId, "user", userInput)
      memoryManager.appendMessage(userId, sessionId, "assistant", errorMsg)
      return BrainResponse.Error(errorMsg)
    }

    val cleanInput = safety.sanitizedContent
    memoryManager.appendMessage(userId, sessionId, "user", cleanInput)

    // 2. Multilingual Intelligence & Language Detection
    val detectedLang = if (activeLanguage == DetectedLanguage.AUTO_DETECT) {
      LanguageDetector.detectLanguage(cleanInput)
    } else {
      activeLanguage
    }

    // 3. Check for Real-Time Query (Live rates, gas fees)
    val lower = cleanInput.lowercase()
    if (lower.contains("سعر الصرف") || lower.contains("اسعار الصرف") || lower.contains("سعر الدولار") || lower.contains("غاز") || lower.contains("live rate") || lower.contains("real time")) {
      val liveData = RealTimeKnowledgeEngine.formatLiveMarketSummary()
      memoryManager.appendMessage(userId, sessionId, "assistant", liveData)
      return BrainResponse.Completed(
        text = liveData,
        detectedLanguage = detectedLang,
        sourceCitations = listOf("بيانات السوق الحية وشبكات البلوكشين (محدثة لحظياً)")
      )
    }

    // 4. Check for Action Intent (Function Calling)
    val actionCheck = detectAndExecuteAction(cleanInput, userId)
    if (actionCheck != null) {
      when (actionCheck) {
        is ActionResult.NeedsConfirmation -> {
          val confirmText = "⚠️ *تأكيد مطلوب قبل المتابعة:*\n${actionCheck.proposal.titleAr}\n${actionCheck.proposal.summaryAr}\n\nهل ترغب في تأكيد هذا الإجراء؟"
          memoryManager.appendMessage(userId, sessionId, "assistant", confirmText)
          return BrainResponse.Completed(
            text = confirmText,
            detectedLanguage = detectedLang,
            sourceCitations = emptyList(),
            actionProposal = actionCheck.proposal
          )
        }
        is ActionResult.Success -> {
          memoryManager.appendMessage(userId, sessionId, "assistant", actionCheck.messageAr)
          return BrainResponse.Completed(
            text = actionCheck.messageAr,
            detectedLanguage = detectedLang,
            sourceCitations = listOf("Social Pay Core Action Engine"),
            executedActionResult = actionCheck
          )
        }
        is ActionResult.Denied -> {
          memoryManager.appendMessage(userId, sessionId, "assistant", actionCheck.reasonAr)
          return BrainResponse.Completed(
            text = actionCheck.reasonAr,
            detectedLanguage = detectedLang,
            sourceCitations = emptyList()
          )
        }
        is ActionResult.Error -> {
          // fall through to model reasoning
        }
      }
    }

    // 5. RAG Knowledge Enrichment
    val (enrichedPrompt, citations) = knowledgeEngine.enrichPromptWithKnowledge(cleanInput)

    // 6. Build Master System Instruction
    val longTermPreferences = memoryManager.getUserLongTermMemories(userId).joinToString("; ") { "${it.key}: ${it.value}" }
    val systemInstruction = buildMasterSystemInstruction(detectedLang, longTermPreferences)

    // 7. Short-Term History
    val history = memoryManager.getShortTermContext(sessionId, maxMessages = 8)

    // 8. Generate via Primary Model Adapter with Fallback
    var responseResult = primaryAdapter.generateResponse(enrichedPrompt, systemInstruction, history)
    if (responseResult.isFailure) {
      // Graceful fallback to Local Fallback Model
      responseResult = fallbackAdapter.generateResponse(cleanInput, systemInstruction, history)
    }

    val rawOutput = responseResult.getOrElse {
      "أنا SOCIAL PAY GENIUS BRAIN. نعتذر عن التأخير المؤقت في استجابة النموذج، ولكن يمكنك دائماً إنشاء ومتابعة طلباتك بأمان وبرسوم 0.80$."
    }

    // 9. Inspect & Sanitize Output
    val finalOutput = TrustAndSafetyEngine.inspectOutput(rawOutput)
    memoryManager.appendMessage(userId, sessionId, "assistant", finalOutput, citations)

    return BrainResponse.Completed(
      text = finalOutput,
      detectedLanguage = detectedLang,
      sourceCitations = citations
    )
  }

  /**
   * Streaming response implementation
   */
  fun streamUserMessage(
    userInput: String,
    userId: String,
    sessionId: String = memoryManager.getActiveSessionId(userId)
  ): Flow<String> = flow {
    val safety = TrustAndSafetyEngine.inspectInput(userInput)
    if (!safety.isApproved) {
      emit(safety.warningAr ?: "تم حظر المدخلات.")
      return@flow
    }

    val cleanInput = safety.sanitizedContent
    memoryManager.appendMessage(userId, sessionId, "user", cleanInput)

    val detectedLang = if (activeLanguage == DetectedLanguage.AUTO_DETECT) {
      LanguageDetector.detectLanguage(cleanInput)
    } else {
      activeLanguage
    }

    val (enrichedPrompt, _) = knowledgeEngine.enrichPromptWithKnowledge(cleanInput)
    val systemInstruction = buildMasterSystemInstruction(detectedLang, "")
    val history = memoryManager.getShortTermContext(sessionId, maxMessages = 6)

    try {
      primaryAdapter.streamResponse(enrichedPrompt, systemInstruction, history).collect { token ->
        emit(token)
      }
    } catch (_: Exception) {
      fallbackAdapter.streamResponse(cleanInput, systemInstruction, history).collect { token ->
        emit(token)
      }
    }
  }

  private fun buildMasterSystemInstruction(lang: DetectedLanguage, userPrefs: String): String {
    return """
    أنت المساعد الذكي المعتمد: SOCIAL PAY GENIUS BRAIN.
    
    【توجيهات الوحدة النشطة (${activeModule.titleAr})】
    ${activeModule.systemDirective}
    
    【توجيهات اللغة】
    ${LanguageDetector.formatSystemPromptLanguageDirective(lang)}
    
    【تفضيلات المستخدم المحفوظة بالذاكرة طويلة المدى】
    ${if (userPrefs.isNotBlank()) userPrefs else "لا توجد تفضيلات مسبقة."}
    
    【قواعد الأمان والامتثال المالي الصارمة】
    1. رسوم المعاملة ثابتة 0.80$، وتصبح 0.90$ للأبد عند تحقيق مليون دولار حجم تداول.
    2. المنصة غير احتجازية (Non-Custodial) ولا تحتفظ بأموال المستخدمين ولا بمفاتيحهم.
    3. الصفقات خالية من الربا بنسبة 0% وتقوم على التقابض الفوري الحقيقي.
    4. تمنع المنصة الاحتيال عبر درع منع التثليث (مطابقة اسم المحول البنكي مع الهوية) وختم مكافحة التصيد.
    5. لا تحول أموالاً أو تغير أرصدة من تلقاء نفسك أبداً.
    """.trimIndent()
  }

  private fun detectAndExecuteAction(input: String, userId: String): ActionResult? {
    val lower = input.lowercase()

    // 1. Create Invoice intent
    if (lower.contains("انشئ فاتورة") || lower.contains("إنشاء فاتورة") || lower.contains("اعمل فاتورة") || lower.contains("create invoice")) {
      val amount = Regex("([0-9]+(?:\\.[0-9]+)?)").find(input)?.value ?: "150"
      val currency = if (lower.contains("دولار") || lower.contains("usd")) "USD" else "SAR"
      return actionEngine.executeTool(
        toolName = "create_invoice",
        params = mapOf("amount" to amount, "currency" to currency, "clientName" to "عميل موثق", "description" to "فاتورة تجارية صادرة عبر Social Pay"),
        userId = userId
      )
    }

    // 2. Create Payment Link intent
    if (lower.contains("انشئ رابط") || lower.contains("رابط دفع") || lower.contains("رابط مشاركة") || lower.contains("ديرلي رابط") || lower.contains("payment link")) {
      val amount = Regex("([0-9]+(?:\\.[0-9]+)?)").find(input)?.value ?: "100"
      val currency = if (lower.contains("usdt")) "USDT" else if (lower.contains("دولار")) "USD" else "SAR"
      return actionEngine.executeTool(
        toolName = "create_payment_link",
        params = mapOf("amount" to amount, "currency" to currency, "title" to "طلب سداد Social Pay", "channel" to "WHATSAPP"),
        userId = userId
      )
    }

    // 3. Transaction Status intent
    val txMatch = Regex("(?i)SP-(?:REQ|INV|BUY|SEL)-[0-9]{6}").find(input)?.value
    if (txMatch != null && (lower.contains("حالة") || lower.contains("status") || lower.contains("وين وصلت"))) {
      return actionEngine.executeTool(
        toolName = "get_transaction_status",
        params = mapOf("transactionId" to txMatch),
        userId = userId
      )
    }

    // 4. Create Support Ticket intent
    if (lower.contains("شكوى") || lower.contains("مشكلة") || lower.contains("تذكرة دعم") || lower.contains("help dispute") || lower.contains("support")) {
      return actionEngine.executeTool(
        toolName = "create_support_ticket",
        params = mapOf("subject" to "طلب مساعدة ومتابعة نزاع", "details" to input),
        userId = userId
      )
    }

    // 5. User History intent
    if (lower.contains("سجل معاملاتي") || lower.contains("تاريخ حسابي") || lower.contains("شحال درت") || lower.contains("my history")) {
      return actionEngine.executeTool(
        toolName = "get_user_history",
        params = emptyMap(),
        userId = userId
      )
    }

    return null
  }
}
