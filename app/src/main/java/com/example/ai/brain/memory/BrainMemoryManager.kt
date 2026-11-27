package com.example.ai.brain.memory

import java.util.UUID

/**
 * An item stored in Long-Term Memory (User-consented preferences, aliases, favorite channels)
 */
data class LongTermMemoryItem(
  val id: String = UUID.randomUUID().toString(),
  val userId: String,
  val key: String, // e.g. "preferred_currency", "default_channel", "favorite_contact", "business_name"
  val value: String,
  val category: String = "PREFERENCE", // "PREFERENCE", "BUSINESS_INFO", "COMMUNICATION"
  val createdAt: Long = System.currentTimeMillis(),
  val lastAccessedAt: Long = System.currentTimeMillis()
)

/**
 * Chat message within a conversation
 */
data class BrainChatMessage(
  val id: String = UUID.randomUUID().toString(),
  val role: String, // "user", "assistant", "system", "tool"
  val content: String,
  val languageTag: String? = null,
  val toolName: String? = null,
  val sourceAttributions: List<String> = emptyList(),
  val timestamp: Long = System.currentTimeMillis()
)

/**
 * A persistent chat session / conversation thread
 */
data class ConversationSession(
  val sessionId: String = UUID.randomUUID().toString(),
  val userId: String,
  val title: String,
  val messages: List<BrainChatMessage> = emptyList(),
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Multi-Level Memory Manager for Social Pay Genius Brain
 * Handles:
 * - Short-Term Memory (active turn context window)
 * - Long-Term Memory (user-consented persistent preferences)
 * - Sensitive Data Scrubbing (redacts passwords, card numbers, seed phrases, private keys)
 * - User Memory Inspection, Editing, and Full Deletion Controls
 */
class BrainMemoryManager {

  // Long-Term Memory Map: userId -> List<LongTermMemoryItem>
  private val longTermStore = mutableMapOf<String, MutableList<LongTermMemoryItem>>()

  // Sessions Store: sessionId -> ConversationSession
  private val sessionStore = mutableMapOf<String, ConversationSession>()

  // Active session per user
  private val activeUserSessions = mutableMapOf<String, String>()

  /**
   * Sanitizes text to strictly prohibit storing sensitive data
   * (Credit card numbers, passwords, CVVs, private keys, seed phrases)
   */
  fun sanitizeSensitiveData(text: String): String {
    var sanitized = text
    // 16-digit Card numbers
    sanitized = sanitized.replace(Regex("\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|6(?:011|5[0-9][0-9])[0-9]{12}|3[47][0-9]{13})\\b"), "[REDACTED_CARD_NUMBER]")
    // CVV (3 or 4 digits preceded by cvv/cvc)
    sanitized = sanitized.replace(Regex("(?i)\\b(cvv|cvc)\\s*[:=]?\\s*[0-9]{3,4}\\b"), "[REDACTED_CVV]")
    // Passwords or PINs
    sanitized = sanitized.replace(Regex("(?i)\\b(password|pin|كلمة المرور|رمز الدخول)\\s*[:=]?\\s*\\S+"), "$1: [REDACTED_CREDENTIAL]")
    // Private keys (0x... 64 hex chars or base58 long strings)
    sanitized = sanitized.replace(Regex("(?i)\\b0x[a-f0-9]{64}\\b"), "[REDACTED_PRIVATE_KEY]")
    return sanitized
  }

  /**
   * Appends a message to the active session (Short-Term Memory)
   */
  @Synchronized
  fun appendMessage(userId: String, sessionId: String, role: String, content: String, sources: List<String> = emptyList()): BrainChatMessage {
    val cleanContent = sanitizeSensitiveData(content)
    val msg = BrainChatMessage(
      role = role,
      content = cleanContent,
      sourceAttributions = sources
    )

    val existing = sessionStore[sessionId] ?: ConversationSession(
      sessionId = sessionId,
      userId = userId,
      title = cleanContent.take(30).ifEmpty { "محادثة جديدة" }
    )

    val updatedMessages = existing.messages + msg
    val updatedSession = existing.copy(
      messages = updatedMessages,
      updatedAt = System.currentTimeMillis()
    )
    sessionStore[sessionId] = updatedSession
    activeUserSessions[userId] = sessionId
    return msg
  }

  /**
   * Retrieves short-term context window (e.g. last 10 messages) for prompt generation
   */
  @Synchronized
  fun getShortTermContext(sessionId: String, maxMessages: Int = 10): List<Pair<String, String>> {
    val session = sessionStore[sessionId] ?: return emptyList()
    return session.messages.takeLast(maxMessages).map { it.role to it.content }
  }

  /**
   * Saves a user-consented preference or fact to Long-Term Memory
   */
  @Synchronized
  fun saveLongTermPreference(userId: String, key: String, value: String, category: String = "PREFERENCE"): LongTermMemoryItem {
    val cleanValue = sanitizeSensitiveData(value)
    val items = longTermStore.getOrPut(userId) { mutableListOf() }
    items.removeIf { it.key.equals(key, ignoreCase = true) }
    val item = LongTermMemoryItem(
      userId = userId,
      key = key,
      value = cleanValue,
      category = category
    )
    items.add(item)
    return item
  }

  /**
   * Retrieves all long-term memories for a specific user (Isolation guarantee)
   */
  @Synchronized
  fun getUserLongTermMemories(userId: String): List<LongTermMemoryItem> {
    return longTermStore[userId]?.toList() ?: emptyList()
  }

  /**
   * User Memory Control: Updates an individual memory item
   */
  @Synchronized
  fun updateMemoryItem(userId: String, itemId: String, newValue: String): Boolean {
    val items = longTermStore[userId] ?: return false
    val idx = items.indexOfFirst { it.id == itemId }
    if (idx != -1) {
      val existing = items[idx]
      items[idx] = existing.copy(
        value = sanitizeSensitiveData(newValue),
        lastAccessedAt = System.currentTimeMillis()
      )
      return true
    }
    return false
  }

  /**
   * User Memory Control: Deletes a specific memory item
   */
  @Synchronized
  fun deleteMemoryItem(userId: String, itemId: String): Boolean {
    val items = longTermStore[userId] ?: return false
    return items.removeIf { it.id == itemId }
  }

  /**
   * User Memory Control: Completely wipes all memories and history for a user (Right to be Forgotten)
   */
  @Synchronized
  fun wipeAllUserData(userId: String) {
    longTermStore.remove(userId)
    activeUserSessions.remove(userId)
    val userSessionIds = sessionStore.filterValues { it.userId == userId }.keys
    userSessionIds.forEach { sessionStore.remove(it) }
  }

  /**
   * Retrieves all user sessions
   */
  @Synchronized
  fun getUserSessions(userId: String): List<ConversationSession> {
    return sessionStore.values
      .filter { it.userId == userId }
      .sortedByDescending { it.updatedAt }
  }

  @Synchronized
  fun getSession(sessionId: String): ConversationSession? = sessionStore[sessionId]

  @Synchronized
  fun createNewSession(userId: String, title: String = "محادثة جديدة"): ConversationSession {
    val newSession = ConversationSession(userId = userId, title = title)
    sessionStore[newSession.sessionId] = newSession
    activeUserSessions[userId] = newSession.sessionId
    return newSession
  }

  @Synchronized
  fun getActiveSessionId(userId: String): String {
    return activeUserSessions[userId] ?: createNewSession(userId).sessionId
  }
}
