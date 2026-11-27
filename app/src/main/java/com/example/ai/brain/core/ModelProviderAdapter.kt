package com.example.ai.brain.core

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

/**
 * Standard Model Provider Adapter Interface
 * Allows switching AI model providers (Gemini 3.8 Flash, Gemini 3.5 Flash-Lite, Custom Edge/Enterprise)
 * without modifying the core application code.
 */
interface ModelProviderAdapter {
  val providerId: String
  val providerName: String
  val activeModelName: String

  suspend fun generateResponse(
    prompt: String,
    systemInstruction: String,
    history: List<Pair<String, String>> = emptyList()
  ): Result<String>

  fun streamResponse(
    prompt: String,
    systemInstruction: String,
    history: List<Pair<String, String>> = emptyList()
  ): Flow<String>

  suspend fun checkHealth(): Boolean
}

/**
 * Official Firebase AI Logic Adapter (Gemini 3.8 Flash & Gemini 3.5 Flash-Lite)
 */
class FirebaseAiModelAdapter(
  override val activeModelName: String = "gemini-3.8-flash"
) : ModelProviderAdapter {

  override val providerId = "FIREBASE_AI_LOGIC"
  override val providerName = "Google Firebase AI Logic (Gemini Engine)"

  private fun getModel(systemInstruction: String) = Firebase.ai.generativeModel(
    modelName = activeModelName,
    systemInstruction = content { text(systemInstruction) }
  )

  override suspend fun generateResponse(
    prompt: String,
    systemInstruction: String,
    history: List<Pair<String, String>>
  ): Result<String> {
    return try {
      withTimeout(60_000L) {
        val model = getModel(systemInstruction)
        val chat = model.startChat(
          history = history.map { (role, msg) ->
            content(if (role == "user") "user" else "model") { text(msg) }
          }
        )
        val response = chat.sendMessage(prompt)
        val text = response.text
        if (!text.isNullOrBlank()) {
          Result.success(text.trim())
        } else {
          Result.failure(IllegalStateException("استجاب النموذج بمحتوى فارغ"))
        }
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override fun streamResponse(
    prompt: String,
    systemInstruction: String,
    history: List<Pair<String, String>>
  ): Flow<String> = flow {
    withTimeout(60_000L) {
      val model = getModel(systemInstruction)
      val chat = model.startChat(
        history = history.map { (role, msg) ->
          content(if (role == "user") "user" else "model") { text(msg) }
        }
      )
      chat.sendMessageStream(prompt).collect { chunk ->
        val text = chunk.text
        if (!text.isNullOrEmpty()) {
          emit(text)
        }
      }
    }
  }

  override suspend fun checkHealth(): Boolean {
    return try {
      val res = generateResponse("ping", "Answer with 'pong'")
      res.isSuccess
    } catch (e: Exception) {
      false
    }
  }
}

/**
 * High-performance fallback model adapter for offline, localized, or degraded connectivity situations.
 */
class LocalFallbackModelAdapter : ModelProviderAdapter {
  override val providerId = "LOCAL_FALLBACK"
  override val providerName = "Social Pay Local Intelligence Fallback"
  override val activeModelName = "offline-smart-rule-engine-v1"

  override suspend fun generateResponse(
    prompt: String,
    systemInstruction: String,
    history: List<Pair<String, String>>
  ): Result<String> {
    val response = synthesizeResponse(prompt)
    return Result.success(response)
  }

  override fun streamResponse(
    prompt: String,
    systemInstruction: String,
    history: List<Pair<String, String>>
  ): Flow<String> = flow {
    val fullResponse = synthesizeResponse(prompt)
    val tokens = fullResponse.split(" ")
    for (token in tokens) {
      emit("$token ")
      kotlinx.coroutines.delay(25)
    }
  }

  override suspend fun checkHealth(): Boolean = true

  private fun synthesizeResponse(prompt: String): String {
    val lower = prompt.lowercase()
    return when {
      lower.contains("رسوم") || lower.contains("fee") || lower.contains("شحال") -> {
        "رسوم Social Pay ثابتة ومحددة بدقة: 0.80$ فقط لكل عملية تحويل، وعند وصول حجم المعاملات التراكمي إلى مليون دولار ترتفع إلى 0.90$ ثابتة للأبد لتمويل صندوق الأمان والأيردروبات، بلا أي فوائد أو ربا."
      }
      lower.contains("أمان") || lower.contains("تثليث") || lower.contains("triangulation") || lower.contains("escrow") -> {
        "نظام الأمان في Social Pay يعتمد على 3 ركائز: 1) حجز الأمانة المشفرة بالعقد الذكي 0% ربا، 2) درع منع التثليث بمطابقة اسم صاحب الحساب مع الهوية المعتمدة، 3) ختم مكافحة التصيد السري المائي."
      }
      lower.contains("واش راك") || lower.contains("لاباس") || lower.contains("صباح الخير") || lower.contains("سلام") -> {
        "مرحباً بك! أنا SOCIAL PAY GENIUS BRAIN، مساعدك الذكي المالي والتقني. كيف أقدر نعاونك اليوم في المعاملات أو الفواتير أو الاستفسارات؟"
      }
      else -> {
        "أنا SOCIAL PAY GENIUS BRAIN. فهمت استفسارك بخصوص: \"$prompt\". كمنصة معاملات اجتماعية غير حافظة (Non-Custodial)، تتيح لك المنصة إنشاء روابط وفواتير ومشاركتها عبر واتساب وتيليجرام بأمان تام ورسوم 0.80$ ثابتة."
      }
    }
  }
}
