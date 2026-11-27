package com.example.model

enum class TradeType(val labelAr: String, val labelEn: String, val labelFr: String) {
  BUY("استلام / شراء مستقر", "Receive / Acquire", "Recevoir / Acquérir"),
  SELL("تسوية / تحويل", "Settle / Send", "Régler / Envoyer")
}

data class CryptoAsset(
  val symbol: String,
  val name: String,
  val iconEmoji: String,
  val basePriceUsd: Double,
  val network: String
)

data class FiatCurrency(
  val code: String,
  val nameAr: String,
  val nameEn: String,
  val symbol: String,
  val rateToUsd: Double // 1 USD = rate units of fiat
)

enum class PaymentCategory(val titleAr: String, val titleEn: String) {
  BANK("تحويل بنكي", "Bank Transfer"),
  DIGITAL_WALLET("محفظة إلكترونية", "E-Wallet"),
  INTERNATIONAL("دولي / Wise", "International"),
  CASH("نقداً يداً بيد", "Cash")
}

data class PaymentMethod(
  val id: String,
  val name: String,
  val category: PaymentCategory,
  val iconEmoji: String
)

data class Merchant(
  val id: String,
  val name: String,
  val avatarInitials: String,
  val ratingPercent: Double,
  val completedOrders: Int,
  val avgReleaseMinutes: Double,
  val isVerified: Boolean,
  val isProMerchant: Boolean,
  val depositCollateralUsdt: Double
)

data class MarketOffer(
  val id: String,
  val tradeType: TradeType,
  val asset: CryptoAsset,
  val fiat: FiatCurrency,
  val pricePerUnit: Double,
  val availableAmount: Double,
  val minLimitFiat: Double,
  val maxLimitFiat: Double,
  val merchant: Merchant,
  val acceptedPaymentMethods: List<PaymentMethod>,
  val termsAndConditions: String,
  val autoReplyMsg: String
)

enum class EscrowStatus(val labelAr: String, val labelEn: String, val labelFr: String) {
  LOCKED_IN_ESCROW("الأمانة محجوزة مؤقتاً", "Authorization Held", "Autorisation retenue"),
  PAID_PENDING_CONFIRMATION("تم الإرسال وبانتظار التحقق", "Sent - Awaiting Verification", "Envoyé - En vérification"),
  RELEASED_COMPLETED("تمت التسوية والإتمام", "Settled & Completed", "Réglé et terminé"),
  DISPUTED("معلقة - قيد المراجعة", "Under Review", "En examen"),
  CANCELLED("ملغاة بأمان", "Cancelled Safely", "Annulé en sécurité")
}

data class DealChatMessage(
  val id: String,
  val senderName: String,
  val text: String,
  val timestampText: String,
  val isSystem: Boolean = false,
  val isMe: Boolean = false
)

data class SmartDeal(
  val dealId: String,
  val offerId: String,
  val tradeType: TradeType,
  val assetSymbol: String,
  val fiatCode: String,
  val cryptoAmount: Double,
  val fiatAmount: Double,
  val unitPrice: Double,
  val fixedFeeUsd: Double = 0.80,
  val counterpartyName: String,
  val counterpartyRating: Double,
  val status: EscrowStatus,
  val createdAtMillis: Long,
  val expirySecondsRemaining: Int,
  val sellerDepositLocked: Boolean,
  val buyerPaidProofAttached: Boolean,
  val sellerConfirmedReceived: Boolean,
  val isShariahZeroRibaCertified: Boolean = true,
  val chatHistory: List<DealChatMessage> = emptyList(),
  val disputeReason: String? = null
)

enum class ButtonColorTheme(val titleAr: String, val hexCode: Long, val gradientSecond: Long) {
  EMERALD_GOLD("زمردي وذهبي ملكي", 0xFF00C076, 0xFFF5A623),
  TELEGRAM_CYBER("تيليجرام سايبر", 0xFF0088CC, 0xFF00C4FF),
  WHATSAPP_EMERALD("واتساب نيون", 0xFF25D366, 0xFF128C7E),
  BINANCE_GOLD("أصفر التداول العالمي", 0xFFF3BA2F, 0xFFFF9900),
  PURPLE_FUTURE("بنفسجي مشفر", 0xFF8B5CF6, 0xFFEC4899),
  STEALTH_DARK("أسود شيلد تكتيكي", 0xFF1E293B, 0xFF0F172A)
}

enum class ButtonSizePreset(val titleAr: String, val sizeDp: Int) {
  COMPACT("مدمج أنيق (48dp)", 48),
  STANDARD("قياسي مثالي (58dp)", 58),
  LARGE("بارز سريع (68dp)", 68)
}

data class FloatingButtonConfig(
  val isVisible: Boolean = true,
  val theme: ButtonColorTheme = ButtonColorTheme.EMERALD_GOLD,
  val sizePreset: ButtonSizePreset = ButtonSizePreset.STANDARD,
  val pulseAnimation: Boolean = true,
  val dockToEdge: Boolean = true,
  val opacity: Float = 0.95f,
  val offsetX: Float = 0.82f, // percentage of screen width
  val offsetY: Float = 0.45f, // percentage of screen height
  val showLiveRateBadge: Boolean = true
)

data class RevenueMilestoneInfo(
  val currentFeeUsd: Double = 0.80,
  val totalPlatformRevenueUsd: Double = 0.0,
  val milestoneGoalUsd: Double = 1_000_000.0,
  val nextPermanentFeeUsd: Double = 0.90,
  val totalCompletedDeals: Long = 0L
) {
  val progressFraction: Float
    get() = (totalPlatformRevenueUsd / milestoneGoalUsd).toFloat().coerceIn(0f, 1f)

  val remainingAmountUsd: Double
    get() = (milestoneGoalUsd - totalPlatformRevenueUsd).coerceAtLeast(0.0)
}

data class AirdropReward(
  val id: String,
  val titleAr: String,
  val titleEn: String,
  val pointsRequired: Int,
  val rewardValueUsdt: Double,
  val isClaimed: Boolean = false,
  val tag: String
)

enum class AppLanguage(
  val code: String,
  val displayName: String,
  val isRtl: Boolean,
  val flag: String = "🌐"
) {
  ARABIC("ar", "العربية", true, "🇸🇦"),
  ENGLISH("en", "English", false, "🇬🇧"),
  RUSSIAN("ru", "Русский", false, "🇷🇺"),
  CHINESE("zh", "中文", false, "🇨🇳"),
  FRENCH("fr", "Français", false, "🇫🇷")
}

enum class PaymentAccountType(
  val titleAr: String,
  val titleEn: String,
  val titleFr: String,
  val titleRu: String = titleEn,
  val titleZh: String = titleEn
) {
  BANK_IBAN("حساب بنكي / IBAN", "Bank Account / IBAN", "Compte Bancaire / IBAN", "Банковский счет / IBAN", "银行账户 / IBAN"),
  DIGITAL_WALLET("محفظة إلكترونية", "Digital Wallet", "Portefeuille Électronique", "Электронный кошелек", "电子钱包"),
  INSTANT_RAIL("تحويل فوري / BaridiMob", "Instant Payment Rail", "Paiement Instantané", "Мгновенный платеж", "即时支付通道")
}

data class UserBankAccount(
  val id: String = "",
  val bankName: String = "",
  val accountNumberOrIban: String = "",
  val accountHolderName: String = "",
  val isDefault: Boolean = false,
  val accountType: PaymentAccountType = PaymentAccountType.BANK_IBAN,
  val currency: String = "SAR",
  val verifiedStatus: String = "VERIFIED"
)

enum class KycLevel(val titleAr: String, val titleEn: String) {
  UNVERIFIED("غير موثق", "Unverified"),
  BASIC_LEVEL_1("توثيق أساسي (المستوى 1)", "Basic Level 1"),
  VERIFIED_PRO_LEVEL_2("تاجر موثق معتمد (المستوى 2 VIP)", "Verified Pro Level 2 VIP")
}

data class UserOfficeProfile(
  val userId: String = "",
  val fullName: String = "",
  val email: String = "",
  val phoneNumber: String = "",
  val nationalIdOrPassport: String = "",
  val kycLevel: KycLevel = KycLevel.BASIC_LEVEL_1,
  val trustScorePercent: Double = 99.8,
  val totalTradesCount: Int = 0,
  val completionRatePercent: Double = 100.0,
  val spotBalanceUsdt: Double = 0.0,
  val lockedEscrowUsdt: Double = 0.0,
  val merchantCollateralUsdt: Double = 0.0,
  val partnerCommissionsEarnedUsd: Double = 0.0,
  val linkedBankAccounts: List<UserBankAccount> = emptyList(),
  val myActiveAdsCount: Int = 0,
  val isOnlineMerchant: Boolean = true
)

