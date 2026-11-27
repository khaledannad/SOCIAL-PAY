package com.example.community

import java.util.UUID

/**
 * Data structures for LIVE COMMENTAIRE 🌍 — Worldwide Open Social Space.
 *
 * ARCHITECTURAL ISOLATION RULE:
 * This community space is 100% decoupled from financial ledgers, wallets,
 * payment intents, balances, KYC, and system administration.
 * Participation is completely open and anonymous.
 */

data class CommunityComment(
  val id: String = UUID.randomUUID().toString(),
  val authorName: String,
  val isAnonymous: Boolean = false,
  val countryCode: String = "GLOBAL",
  val countryFlag: String = "🌍",
  val cityOrRegion: String = "",
  val message: String,
  val languageCode: String = "ar",
  val timestampMillis: Long = System.currentTimeMillis(),
  val likesCount: Int = 0,
  val isLikedByMe: Boolean = false,
  val replies: List<CommunityReply> = emptyList(),
  val aiReply: CommunityAiReply? = null
)

data class CommunityReply(
  val id: String = UUID.randomUUID().toString(),
  val authorName: String,
  val countryFlag: String = "🌍",
  val message: String,
  val timestampMillis: Long = System.currentTimeMillis()
)

data class CommunityAiReply(
  val botName: String = "SOCIAL PAY AI",
  val message: String,
  val timestampMillis: Long = System.currentTimeMillis(),
  val languageCode: String = "ar"
)

enum class CommentSortOrder(val titleAr: String, val titleEn: String, val titleFr: String, val titleRu: String, val titleZh: String) {
  NEWEST("الأحدث ⏱", "Newest ⏱", "Plus Récents ⏱", "Свежие ⏱", "最新 ⏱"),
  MOST_ACTIVE("الأكثر تفاعلاً 🔥", "Most Active 🔥", "Plus Actifs 🔥", "Популярные 🔥", "最热门 🔥")
}

data class CountryOption(
  val code: String,
  val flag: String,
  val nameAr: String,
  val nameEn: String
)

val GLOBAL_COUNTRY_OPTIONS = listOf(
  CountryOption("GLOBAL", "🌍", "عالمي", "Global"),
  CountryOption("SA", "🇸🇦", "السعودية", "Saudi Arabia"),
  CountryOption("AE", "🇦🇪", "الإمارات", "UAE"),
  CountryOption("EG", "🇪🇬", "مصر", "Egypt"),
  CountryOption("FR", "🇫🇷", "فرنسا", "France"),
  CountryOption("GB", "🇬🇧", "بريطانيا", "UK"),
  CountryOption("US", "🇺🇸", "أمريكا", "USA"),
  CountryOption("RU", "🇷🇺", "روسيا", "Russia"),
  CountryOption("CN", "🇨🇳", "الصين", "China"),
  CountryOption("DE", "🇩🇪", "ألمانيا", "Germany"),
  CountryOption("BR", "🇧🇷", "البرازيل", "Brazil"),
  CountryOption("JP", "🇯🇵", "اليابان", "Japan"),
  CountryOption("SG", "🇸🇬", "سنغافورة", "Singapore")
)
