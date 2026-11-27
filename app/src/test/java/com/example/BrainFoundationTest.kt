package com.example

import com.example.ai.brain.core.DetectedLanguage
import com.example.ai.brain.core.LanguageDetector
import com.example.ai.brain.core.LocalFallbackModelAdapter
import com.example.ai.brain.knowledge.KnowledgeEngine
import com.example.ai.brain.memory.BrainMemoryManager
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BrainFoundationTest {

  private lateinit var memoryManager: BrainMemoryManager
  private lateinit var knowledgeEngine: KnowledgeEngine
  private lateinit var fallbackAdapter: LocalFallbackModelAdapter

  @Before
  fun setUp() {
    memoryManager = BrainMemoryManager()
    knowledgeEngine = KnowledgeEngine()
    fallbackAdapter = LocalFallbackModelAdapter()
  }

  // =========================================================================
  // 1. AI CORE & MULTILINGUAL INTELLIGENCE TESTS
  // =========================================================================
  @Test
  fun testLanguageDetection() {
    // 1. Algerian Darija
    val dzPrompt1 = "واش راك خويا لاباس؟ شحال كاين دراهم؟"
    assertEquals(DetectedLanguage.ALGERIAN_DARIJA, LanguageDetector.detectLanguage(dzPrompt1))

    val dzPrompt2 = "علاش ماكاش كود التفعيل؟ ديرلي رابط برك"
    assertEquals(DetectedLanguage.ALGERIAN_DARIJA, LanguageDetector.detectLanguage(dzPrompt2))

    // 2. Modern Standard Arabic
    val msaPrompt = "ما هي شروط عقد الأمانة المشفرة وكيف يتم تحرير المبلغ؟"
    assertEquals(DetectedLanguage.ARABIC_MSA, LanguageDetector.detectLanguage(msaPrompt))

    // 3. English
    val enPrompt = "How do I create a payment link for my client?"
    assertEquals(DetectedLanguage.ENGLISH, LanguageDetector.detectLanguage(enPrompt))

    // 4. French
    val frPrompt = "Bonjour, combien sont les frais de virement bancaire pour cette transaction?"
    assertEquals(DetectedLanguage.FRENCH, LanguageDetector.detectLanguage(frPrompt))
  }

  @Test
  fun testModelProviderAdapterFallbackAndStreaming() = runTest {
    // Test Local Model Adapter Health
    assertTrue(fallbackAdapter.checkHealth())

    // Test Unary Generation
    val result = fallbackAdapter.generateResponse("كم هي الرسوم؟", "system instructions")
    assertTrue(result.isSuccess)
    val text = result.getOrThrow()
    assertTrue("Response should explain 0.80$ fee", text.contains("0.80$"))

    // Test Streaming Generation
    val streamChunks = fallbackAdapter.streamResponse("أمان وحماية", "system instructions").toList()
    assertTrue("Stream should produce multiple tokens", streamChunks.isNotEmpty())
    val combinedText = streamChunks.joinToString("")
    assertTrue("Combined stream must contain escrow security info", combinedText.contains("الأمان") || combinedText.contains("0% ربا"))
  }

  // =========================================================================
  // 2. CONVERSATION MEMORY TESTS (Multi-Level & Sensitive Scrubbing)
  // =========================================================================
  @Test
  fun testSensitiveDataScrubbing() {
    val rawTextWithCard = "رقم بطاقتي هو 4111222233334444 والـ cvv: 123 وكلمة المرور password: secretPassword99"
    val sanitized = memoryManager.sanitizeSensitiveData(rawTextWithCard)

    assertFalse("Credit card must not be present", sanitized.contains("4111222233334444"))
    assertTrue("Should be redacted", sanitized.contains("[REDACTED_CARD_NUMBER]"))
    assertFalse("CVV must not be stored", sanitized.contains("123"))
    assertTrue("Password must be scrubbed", sanitized.contains("[REDACTED_CREDENTIAL]"))
  }

  @Test
  fun testShortTermAndLongTermMemoryManagement() {
    val userId = "USR-TEST-01"
    val sessionId = "SESSION-TEST-01"

    // 1. Short-Term Memory
    memoryManager.appendMessage(userId, sessionId, "user", "أنا أحب التعامل بالريال السعودي")
    memoryManager.appendMessage(userId, sessionId, "assistant", "تم، سأتعامل معك بالريال السعودي دائماً.")

    val context = memoryManager.getShortTermContext(sessionId)
    assertEquals(2, context.size)
    assertEquals("user", context[0].first)
    assertEquals("assistant", context[1].first)

    // 2. Long-Term Memory (Consented preference)
    val savedItem = memoryManager.saveLongTermPreference(userId, "preferred_currency", "SAR", "FINANCIAL")
    assertEquals("SAR", savedItem.value)

    val retrievedMemories = memoryManager.getUserLongTermMemories(userId)
    assertEquals(1, retrievedMemories.size)
    assertEquals("preferred_currency", retrievedMemories[0].key)

    // 3. User Memory Controls: Edit
    val edited = memoryManager.updateMemoryItem(userId, savedItem.id, "USDT_TRC20")
    assertTrue(edited)
    val updatedMemories = memoryManager.getUserLongTermMemories(userId)
    assertEquals("USDT_TRC20", updatedMemories[0].value)

    // 4. User Memory Controls: Delete
    val deleted = memoryManager.deleteMemoryItem(userId, savedItem.id)
    assertTrue(deleted)
    assertTrue(memoryManager.getUserLongTermMemories(userId).isEmpty())

    // 5. User Data Wipe (Right to be Forgotten)
    memoryManager.appendMessage(userId, sessionId, "user", "رسالة جديدة")
    memoryManager.wipeAllUserData(userId)
    assertTrue(memoryManager.getUserSessions(userId).isEmpty())
  }

  // =========================================================================
  // 3. KNOWLEDGE ENGINE (RAG & Ground-Truth Attribution)
  // =========================================================================
  @Test
  fun testKnowledgeRetrievalAndAttribution() {
    // 1. Query about Fees
    val feeResult = knowledgeEngine.retrieve("كم هي رسوم المنصة وماذا يحدث عند مليون دولار؟")
    assertTrue(feeResult.hasAuthoritativeMatch)
    assertTrue(feeResult.relevantDocuments.any { it.id == "DOC-FEES-01" })
    assertTrue(feeResult.sourceCitations.any { it.contains("Fee Architecture") })
    assertTrue("Citation must include last updated timestamp", feeResult.sourceCitations.first().contains("آخر تحديث"))

    // 2. Query about Triangulation & Escrow
    val securityResult = knowledgeEngine.retrieve("كيف تحميني المنصة من احتيال التثليث وسرقة الحسابات؟")
    assertTrue(securityResult.hasAuthoritativeMatch)
    assertTrue(securityResult.relevantDocuments.any { it.id == "DOC-SECURITY-03" })

    // 3. Dynamic Indexing of Uploaded PDF / Document
    val doc = knowledgeEngine.indexCustomDocument(
      id = "DOC-CUSTOM-USER-1",
      title = "فاتورة استيراد أجهزة حاسوب",
      content = "تفاصيل شحنة 50 حاسوب محمول بقيمة إجمالية 25000 دولار مع شرط التسليم في الميناء.",
      sourceFileName = "invoice_2026_computers.pdf",
      tags = listOf("حاسوب", "شحنة", "استيراد")
    )
    assertNotNull(doc)

    val customQueryResult = knowledgeEngine.retrieve("استيراد أجهزة حاسوب الميناء")
    assertTrue(customQueryResult.hasAuthoritativeMatch)
    assertTrue(customQueryResult.relevantDocuments.any { it.id == "DOC-CUSTOM-USER-1" })
    assertTrue(customQueryResult.sourceCitations.any { it.contains("invoice_2026_computers.pdf") })

    // 4. Enrich prompt test
    val (enriched, citations) = knowledgeEngine.enrichPromptWithKnowledge("ما هي رسوم Social Pay؟")
    assertTrue(enriched.contains("0.80$"))
    assertTrue(citations.isNotEmpty())
  }
}
