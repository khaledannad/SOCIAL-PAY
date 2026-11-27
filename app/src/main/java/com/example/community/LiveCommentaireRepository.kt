package com.example.community

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Repository for LIVE COMMENTAIRE 🌍 — Worldwide Social Space.
 *
 * Strict non-negotiable architectural rule:
 * Fully isolated from the financial ledger, wallet balances, payment rails,
 * and merchant/partner operations.
 */
class LiveCommentaireRepository private constructor(context: Context) {

  private val prefs: SharedPreferences = context.getSharedPreferences("live_commentaire_wall_cache", Context.MODE_PRIVATE)
  private val scope = CoroutineScope(Dispatchers.Default)

  private val _comments = MutableStateFlow<List<CommunityComment>>(emptyList())
  val comments: StateFlow<List<CommunityComment>> = _comments.asStateFlow()

  private val _onlineUsersCount = MutableStateFlow(14852)
  val onlineUsersCount: StateFlow<Int> = _onlineUsersCount.asStateFlow()

  init {
    loadCachedCommentsOrSeed()
    startLiveFluctuationHeartbeat()
  }

  private fun startLiveFluctuationHeartbeat() {
    scope.launch {
      while (true) {
        delay(4000)
        val delta = (-5..7).random()
        _onlineUsersCount.update { (it + delta).coerceIn(14500, 15300) }
      }
    }
  }

  private fun loadCachedCommentsOrSeed() {
    val rawJson = prefs.getString(KEY_POSTS, null)
    if (!rawJson.isNullOrBlank()) {
      try {
        val array = JSONArray(rawJson)
        val loaded = mutableListOf<CommunityComment>()
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          val repliesJson = obj.optJSONArray("replies") ?: JSONArray()
          val replies = mutableListOf<CommunityReply>()
          for (j in 0 until repliesJson.length()) {
            val r = repliesJson.getJSONObject(j)
            replies.add(
              CommunityReply(
                id = r.optString("id", UUID.randomUUID().toString()),
                authorName = r.optString("authorName", "Global Friend"),
                countryFlag = r.optString("countryFlag", "🌍"),
                message = r.optString("message", ""),
                timestampMillis = r.optLong("timestampMillis", System.currentTimeMillis())
              )
            )
          }

          val aiReplyObj = obj.optJSONObject("aiReply")
          val aiReply = aiReplyObj?.let {
            CommunityAiReply(
              botName = it.optString("botName", "SOCIAL PAY AI"),
              message = it.optString("message", ""),
              timestampMillis = it.optLong("timestampMillis", System.currentTimeMillis()),
              languageCode = it.optString("languageCode", "ar")
            )
          }

          loaded.add(
            CommunityComment(
              id = obj.optString("id", UUID.randomUUID().toString()),
              authorName = obj.optString("authorName", "Anonymous"),
              isAnonymous = obj.optBoolean("isAnonymous", false),
              countryCode = obj.optString("countryCode", "GLOBAL"),
              countryFlag = obj.optString("countryFlag", "🌍"),
              cityOrRegion = obj.optString("cityOrRegion", ""),
              message = obj.optString("message", ""),
              languageCode = obj.optString("languageCode", "ar"),
              timestampMillis = obj.optLong("timestampMillis", System.currentTimeMillis()),
              likesCount = obj.optInt("likesCount", 0),
              isLikedByMe = obj.optBoolean("isLikedByMe", false),
              replies = replies,
              aiReply = aiReply
            )
          )
        }
        if (loaded.isNotEmpty()) {
          _comments.value = loaded
          return
        }
      } catch (_: Exception) {
        // Fall back to seed
      }
    }
    _comments.value = createSeedComments()
    saveToPrefs()
  }

  private fun createSeedComments(): List<CommunityComment> {
    val now = System.currentTimeMillis()
    return listOf(
      CommunityComment(
        id = "comm_1",
        authorName = "سلطان الشمري",
        isAnonymous = false,
        countryCode = "SA",
        countryFlag = "🇸🇦",
        cityOrRegion = "الرياض",
        message = "فكرة ممتازة جداً وربط ذكي بين التواصل المباشر والمدفوعات الدولية! تحياتي لكل المطورين والمستخدمين من السعودية 🇸🇦✨",
        languageCode = "ar",
        timestampMillis = now - 180_000,
        likesCount = 42,
        replies = listOf(
          CommunityReply(
            authorName = "فاطمة الزهراء",
            countryFlag = "🇩🇿",
            message = "بالتأكيد، تجربة استخدام فائقة الجمال والسلاسة!",
            timestampMillis = now - 120_000
          )
        ),
        aiReply = CommunityAiReply(
          botName = "SOCIAL PAY AI",
          message = "أهلاً بك يا سلطان في مساحة SOCIAL PAY العالمية! يسعدنا جداً تواجدك معنا ونسعد بتواصل جميع الأصدقاء حول العالم 🌍🤝",
          timestampMillis = now - 175_000,
          languageCode = "ar"
        )
      ),
      CommunityComment(
        id = "comm_2",
        authorName = "Alexandre Dupont",
        isAnonymous = false,
        countryCode = "FR",
        countryFlag = "🇫🇷",
        cityOrRegion = "Paris",
        message = "Une interface absolument magnifique, sombre et futuriste. Salutations chaleureuses depuis Paris à toute la communauté mondiale ! 🥐🇫🇷",
        languageCode = "fr",
        timestampMillis = now - 360_000,
        likesCount = 38,
        aiReply = CommunityAiReply(
          botName = "SOCIAL PAY AI",
          message = "Bienvenue dans la communauté mondiale SOCIAL PAY Alexandre ! Ravis de vous compter parmi nous depuis Paris ✨🤝",
          timestampMillis = now - 350_000,
          languageCode = "fr"
        )
      ),
      CommunityComment(
        id = "comm_3",
        authorName = "Dmitry Ivanov",
        isAnonymous = false,
        countryCode = "RU",
        countryFlag = "🇷🇺",
        cityOrRegion = "Москва",
        message = "Отличная концепция глобальной финансовой связи без границ! Очень стильный интерфейс и удобная работа. Привет из Москвы! 🇷🇺🚀",
        languageCode = "ru",
        timestampMillis = now - 540_000,
        likesCount = 29,
        aiReply = CommunityAiReply(
          botName = "SOCIAL PAY AI",
          message = "Добро пожаловать в глобальное сообщество SOCIAL PAY, Дмитрий! Очень рады приветствовать вас из Москвы! 🌍✨",
          timestampMillis = now - 530_000,
          languageCode = "ru"
        )
      ),
      CommunityComment(
        id = "comm_4",
        authorName = "王伟 (Wang Wei)",
        isAnonymous = false,
        countryCode = "CN",
        countryFlag = "🇨🇳",
        cityOrRegion = "北京 (Beijing)",
        message = "全球互联金融网络的愿景非常宏大，设计也很现代！很高兴在这里结识全球各地的新朋友 🇨🇳🏮",
        languageCode = "zh",
        timestampMillis = now - 720_000,
        likesCount = 51,
        aiReply = CommunityAiReply(
          botName = "SOCIAL PAY AI",
          message = "热烈欢迎加入 SOCIAL PAY 全球社区！很高兴能与来自北京的您在此相聚，祝您交流愉快 🌐✨",
          timestampMillis = now - 710_000,
          languageCode = "zh"
        )
      ),
      CommunityComment(
        id = "comm_5",
        authorName = "Liam Gallagher",
        isAnonymous = false,
        countryCode = "GB",
        countryFlag = "🇬🇧",
        cityOrRegion = "London",
        message = "Real-time borderless connectivity is truly the future. Love the privacy-first approach and social feed. Big up from the UK!",
        languageCode = "en",
        timestampMillis = now - 900_000,
        likesCount = 34,
        aiReply = CommunityAiReply(
          botName = "SOCIAL PAY AI",
          message = "Welcome to the global SOCIAL PAY space, Liam! Wonderful to have you connecting with people from every corner of the world 💫",
          timestampMillis = now - 890_000,
          languageCode = "en"
        )
      ),
      CommunityComment(
        id = "comm_6",
        authorName = "زائر عالمي",
        isAnonymous = true,
        countryCode = "AE",
        countryFlag = "🇦🇪",
        cityOrRegion = "دبي",
        message = "سلام من دبي لكل المبدعين! ميزة التعليقات الحرة رائعة وتفتح مجالاً حقيقياً للتعارف الإنساني دون تعقيدات 👍",
        languageCode = "ar",
        timestampMillis = now - 1_200_000,
        likesCount = 27,
        aiReply = CommunityAiReply(
          botName = "SOCIAL PAY AI",
          message = "مرحباً بأهل دبي الأعزاء وبجميع الزوار الكرام! أهلاً وسهلاً بكم في المساحة الحرة لـ SOCIAL PAY 🌐💙",
          timestampMillis = now - 1_190_000,
          languageCode = "ar"
        )
      ),
      CommunityComment(
        id = "comm_7",
        authorName = "Elena Petrova",
        isAnonymous = false,
        countryCode = "RU",
        countryFlag = "🇷🇺",
        cityOrRegion = "Санкт-Петербург",
        message = "Удивительное сочетание технологичности и человеческого общения! Всем мира и процветания 🇷🇺❤️",
        languageCode = "ru",
        timestampMillis = now - 1_500_000,
        likesCount = 22,
        aiReply = CommunityAiReply(
          botName = "SOCIAL PAY AI",
          message = "Приветствуем вас, Елена! Спасибо за теплые слова и участие в нашем открытом сообществе! 🤝✨",
          timestampMillis = now - 1_490_000,
          languageCode = "ru"
        )
      ),
      CommunityComment(
        id = "comm_8",
        authorName = "李娜 (Li Na)",
        isAnonymous = false,
        countryCode = "CN",
        countryFlag = "🇨🇳",
        cityOrRegion = "上海 (Shanghai)",
        message = "支持创新的去中心化与社交理念，期待平台越来越好！来自上海的问候 ✨",
        languageCode = "zh",
        timestampMillis = now - 1_800_000,
        likesCount = 31,
        aiReply = CommunityAiReply(
          botName = "SOCIAL PAY AI",
          message = "感谢李娜的热情支持与问候！欢迎在 SOCIAL PAY 自由空间常来交流 🌸🤝",
          timestampMillis = now - 1_790_000,
          languageCode = "zh"
        )
      )
    )
  }

  fun postComment(
    authorNameInput: String?,
    messageInput: String,
    countryCode: String,
    countryFlag: String,
    languageCode: String
  ): Result<CommunityComment> {
    val cleanMsg = messageInput.trim()
    if (cleanMsg.length < 2) {
      return Result.failure(IllegalArgumentException("Comment text is too short"))
    }
    val cleanAuthor = authorNameInput?.trim().orEmpty()
    val isAnon = cleanAuthor.isBlank()
    val finalAuthor = if (isAnon) {
      when (languageCode) {
        "ar" -> "زائر عالمي"
        "fr" -> "Visiteur Mondial"
        "ru" -> "Мировой Гость"
        "zh" -> "全球访客"
        else -> "Global Visitor"
      }
    } else {
      cleanAuthor.take(60)
    }

    val aiWelcomeMessage = generateAiWelcome(languageCode, finalAuthor)
    val now = System.currentTimeMillis()

    val newComment = CommunityComment(
      id = "comm_" + UUID.randomUUID().toString().take(8),
      authorName = finalAuthor,
      isAnonymous = isAnon,
      countryCode = countryCode,
      countryFlag = countryFlag,
      cityOrRegion = "",
      message = cleanMsg.take(1000),
      languageCode = languageCode,
      timestampMillis = now,
      likesCount = 1,
      isLikedByMe = true,
      replies = emptyList(),
      aiReply = CommunityAiReply(
        botName = "SOCIAL PAY AI",
        message = aiWelcomeMessage,
        timestampMillis = now + 500,
        languageCode = languageCode
      )
    )

    _comments.update { listOf(newComment) + it }
    saveToPrefs()
    return Result.success(newComment)
  }

  fun likeComment(commentId: String) {
    _comments.update { list ->
      list.map { item ->
        if (item.id == commentId) {
          if (item.isLikedByMe) {
            item.copy(likesCount = (item.likesCount - 1).coerceAtLeast(0), isLikedByMe = false)
          } else {
            item.copy(likesCount = item.likesCount + 1, isLikedByMe = true)
          }
        } else {
          item
        }
      }
    }
    saveToPrefs()
  }

  fun replyToComment(
    commentId: String,
    authorNameInput: String?,
    replyText: String,
    countryFlag: String
  ): Result<Unit> {
    val clean = replyText.trim()
    if (clean.length < 2) return Result.failure(IllegalArgumentException("Reply is too short"))
    val cleanAuthor = authorNameInput?.trim().orEmpty().ifBlank { "زائر / Visitor" }.take(50)
    val newReply = CommunityReply(
      id = UUID.randomUUID().toString(),
      authorName = cleanAuthor,
      countryFlag = countryFlag,
      message = clean.take(500),
      timestampMillis = System.currentTimeMillis()
    )
    _comments.update { list ->
      list.map { item ->
        if (item.id == commentId) {
          item.copy(replies = item.replies + newReply)
        } else {
          item
        }
      }
    }
    saveToPrefs()
    return Result.success(Unit)
  }

  private fun generateAiWelcome(languageCode: String, authorName: String): String {
    return when (languageCode) {
      "ar" -> listOf(
        "أهلاً بك يا $authorName في مجتمع SOCIAL PAY العالمي! سعداء بوجودك ومشاركتك في هذه المساحة الإنسانية الحرة 🌍✨",
        "مرحباً بك $authorName! تحياتنا وتقديرنا لك من مجتمع SOCIAL PAY المفتوح للتواصل الإنساني والمالي 💙🤝",
        "أهلاً وسهلاً بك في رحاب المجتمع العالمي! نسعد بتبادل الأفكار والتحيات معك ومع جميع الأصدقاء 💫"
      ).random()

      "fr" -> listOf(
        "Bienvenue dans la communauté mondiale SOCIAL PAY, $authorName ! Ravis de vous compter parmi nous dans cet espace libre 🌍✨",
        "Salutations chaleureuses $authorName ! Heureux de partager cet espace mondial d'échange et de convivialité avec vous 🤝💫"
      ).random()

      "ru" -> listOf(
        "Добро пожаловать в мировое сообщество SOCIAL PAY, $authorName! Очень рады вашему участию в этом свободном пространстве 🌍✨",
        "Приветствуем вас, $authorName! Прекрасного общения и новых друзей в глобальной сети SOCIAL PAY! 🤝🚀"
      ).random()

      "zh" -> listOf(
        "热烈欢迎加入 SOCIAL PAY 全球大家庭，$authorName！很高兴能与您在自由社交空间相聚 🌐✨",
        "欢迎来自世界各地的朋友 $authorName，祝您在这里交流愉快，共同见证全球互联！🤝🏮"
      ).random()

      else -> listOf(
        "Welcome to the SOCIAL PAY worldwide community, $authorName! Delighted to have you join our open global space 🌍✨",
        "Greetings $authorName! Great to have your voice and presence in our borderless social network 💫🤝"
      ).random()
    }
  }

  private fun saveToPrefs() {
    scope.launch {
      try {
        val array = JSONArray()
        _comments.value.take(150).forEach { c ->
          val obj = JSONObject().apply {
            put("id", c.id)
            put("authorName", c.authorName)
            put("isAnonymous", c.isAnonymous)
            put("countryCode", c.countryCode)
            put("countryFlag", c.countryFlag)
            put("cityOrRegion", c.cityOrRegion)
            put("message", c.message)
            put("languageCode", c.languageCode)
            put("timestampMillis", c.timestampMillis)
            put("likesCount", c.likesCount)
            put("isLikedByMe", c.isLikedByMe)

            val repArr = JSONArray()
            c.replies.forEach { r ->
              repArr.put(
                JSONObject().apply {
                  put("id", r.id)
                  put("authorName", r.authorName)
                  put("countryFlag", r.countryFlag)
                  put("message", r.message)
                  put("timestampMillis", r.timestampMillis)
                }
              )
            }
            put("replies", repArr)

            c.aiReply?.let { ai ->
              put(
                "aiReply",
                JSONObject().apply {
                  put("botName", ai.botName)
                  put("message", ai.message)
                  put("timestampMillis", ai.timestampMillis)
                  put("languageCode", ai.languageCode)
                }
              )
            }
          }
          array.put(obj)
        }
        prefs.edit().putString(KEY_POSTS, array.toString()).apply()
      } catch (_: Exception) {
      }
    }
  }

  companion object {
    private const val KEY_POSTS = "saved_community_posts_json"

    @Volatile
    private var instance: LiveCommentaireRepository? = null

    fun getInstance(context: Context): LiveCommentaireRepository {
      return instance ?: synchronized(this) {
        instance ?: LiveCommentaireRepository(context.applicationContext).also { instance = it }
      }
    }
  }
}
