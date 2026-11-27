package com.example.data

import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class P2PRepository {

  val supportedCrypto = listOf(
    CryptoAsset("USDT", "Tether USD", "💵", 1.00, "TRC20 / Solana / ERC20"),
    CryptoAsset("USDC", "USD Coin", "💲", 1.00, "Solana / Base / Polygon / ERC20"),
    CryptoAsset("EURC", "Euro Coin (MiCA)", "💶", 1.08, "Base L2"),
    CryptoAsset("PYUSD", "PayPal USD (Review)", "💠", 1.00, "ERC20")
  )

  val supportedFiat = listOf(
    FiatCurrency("USD", "دولار أمريكي", "US Dollar", "$", 1.00),
    FiatCurrency("EUR", "يورو أوروبي", "Euro", "€", 0.92),
    FiatCurrency("SAR", "ريال سعودي", "Saudi Riyal", "ر.س", 3.75),
    FiatCurrency("AED", "درهم إماراتي", "UAE Dirham", "د.إ", 3.67),
    FiatCurrency("EGP", "جنيه مصري", "Egyptian Pound", "ج.م", 49.20),
    FiatCurrency("DZD", "دينار جزائري", "Algerian Dinar", "د.ج", 134.50),
    FiatCurrency("MAD", "درهم مغربي", "Moroccan Dirham", "د.م", 9.95),
    FiatCurrency("TRY", "ليرة تركية", "Turkish Lira", "₺", 34.20)
  )

  val paymentMethods = listOf(
    PaymentMethod("bank_rajhi", "مصرف محلي / تحويل بنكي IBAN", PaymentCategory.BANK, "🏦"),
    PaymentMethod("stc_pay", "محفظة فورية / STC Pay", PaymentCategory.DIGITAL_WALLET, "📱"),
    PaymentMethod("wise", "Wise / SEPA International", PaymentCategory.INTERNATIONAL, "🌐"),
    PaymentMethod("baridimob", "بريد الجزائر BaridiMob", PaymentCategory.DIGITAL_WALLET, "✉️"),
    PaymentMethod("vodafone_cash", "فودافون كاش / إنستاباي", PaymentCategory.DIGITAL_WALLET, "⚡"),
    PaymentMethod("revolut", "Revolut Global", PaymentCategory.INTERNATIONAL, "💳"),
    PaymentMethod("non_custodial", "تحويل محفظة غير حافظة مباشر", PaymentCategory.DIGITAL_WALLET, "🛡️")
  )

  private val _marketOffers = MutableStateFlow<List<MarketOffer>>(createInitialOffers())
  val marketOffers: StateFlow<List<MarketOffer>> = _marketOffers.asStateFlow()

  private val _activeDeals = MutableStateFlow<List<SmartDeal>>(emptyList())
  val activeDeals: StateFlow<List<SmartDeal>> = _activeDeals.asStateFlow()

  private val _revenueMilestone = MutableStateFlow(
    RevenueMilestoneInfo(
      currentFeeUsd = 0.80,
      totalPlatformRevenueUsd = 0.0,
      milestoneGoalUsd = 1_000_000.0,
      nextPermanentFeeUsd = 0.90,
      totalCompletedDeals = 0L
    )
  )
  val revenueMilestone: StateFlow<RevenueMilestoneInfo> = _revenueMilestone.asStateFlow()

  private val _airdropRewards = MutableStateFlow<List<AirdropReward>>(createInitialAirdrops())
  val airdropRewards: StateFlow<List<AirdropReward>> = _airdropRewards.asStateFlow()

  private val _userTrustScore = MutableStateFlow(99)
  val userTrustScore: StateFlow<Int> = _userTrustScore.asStateFlow()

  private val _userPoints = MutableStateFlow(1420)
  val userPoints: StateFlow<Int> = _userPoints.asStateFlow()

  fun createDealFromOffer(
    offer: MarketOffer,
    cryptoAmount: Double,
    fiatAmount: Double,
    isUserBuying: Boolean
  ): SmartDeal {
    val fee = if (_revenueMilestone.value.totalPlatformRevenueUsd >= 1_000_000.0) 0.90 else 0.80
    val newDeal = SmartDeal(
      dealId = "ESCROW-" + System.currentTimeMillis().toString().takeLast(6),
      offerId = offer.id,
      tradeType = if (isUserBuying) TradeType.BUY else TradeType.SELL,
      assetSymbol = offer.asset.symbol,
      fiatCode = offer.fiat.code,
      cryptoAmount = cryptoAmount,
      fiatAmount = fiatAmount,
      unitPrice = offer.pricePerUnit,
      fixedFeeUsd = fee,
      counterpartyName = offer.merchant.name,
      counterpartyRating = offer.merchant.ratingPercent,
      status = EscrowStatus.LOCKED_IN_ESCROW,
      createdAtMillis = System.currentTimeMillis(),
      expirySecondsRemaining = 900, // 15 minutes
      sellerDepositLocked = true,
      buyerPaidProofAttached = false,
      sellerConfirmedReceived = false,
      isShariahZeroRibaCertified = true,
      chatHistory = listOf(
        DealChatMessage(
          id = "sys-1",
          senderName = "ذكاء العقود الذكية (AI Escrow)",
          text = "تم حجز مبلغ ${String.format("%.2f", cryptoAmount)} ${offer.asset.symbol} في الأمانة المشفرة بنجاح. المعاملة خالية من الربا 0% بفورية التقابض. الرسوم الثابتة هي $fee$.",
          timestampText = "الآن",
          isSystem = true
        ),
        DealChatMessage(
          id = "mer-1",
          senderName = offer.merchant.name,
          text = offer.autoReplyMsg,
          timestampText = "الآن",
          isMe = false
        )
      )
    )

    _activeDeals.update { listOf(newDeal) + it }
    return newDeal
  }

  fun markDealPaid(dealId: String) {
    _activeDeals.update { list ->
      list.map { deal ->
        if (deal.dealId == dealId) {
          deal.copy(
            status = EscrowStatus.PAID_PENDING_CONFIRMATION,
            buyerPaidProofAttached = true,
            chatHistory = deal.chatHistory + DealChatMessage(
              id = "msg-${System.currentTimeMillis()}",
              senderName = "أنا",
              text = "تم تحويل المبلغ عبر الحساب البنكي وإرفاق إشعار الدفع بنجاح. يرجى مراجعة الحساب وتحرير الأمانة.",
              timestampText = "الآن",
              isMe = true
            )
          )
        } else deal
      }
    }
  }

  fun releaseEscrow(dealId: String) {
    _activeDeals.update { list ->
      list.map { deal ->
        if (deal.dealId == dealId) {
          // Increment revenue by the fixed fee ($0.80 or $0.90)
          val fee = deal.fixedFeeUsd
          _revenueMilestone.update { current ->
            val updatedRev = current.totalPlatformRevenueUsd + fee
            val updatedDeals = current.totalCompletedDeals + 1
            val updatedFee = if (updatedRev >= 1_000_000.0) 0.90 else 0.80
            current.copy(
              totalPlatformRevenueUsd = updatedRev,
              totalCompletedDeals = updatedDeals,
              currentFeeUsd = updatedFee
            )
          }

          // Reward user with trust points for honest transaction
          _userPoints.update { it + 50 }

          deal.copy(
            status = EscrowStatus.RELEASED_COMPLETED,
            sellerConfirmedReceived = true,
            chatHistory = deal.chatHistory + DealChatMessage(
              id = "sys-rel-${System.currentTimeMillis()}",
              senderName = "نظام الأمانة المشفرة",
              text = "✅ تم تأكيد استلام الدفع وتحرير العملات للمشتري فوراً. تمت الصفقة بأمان وموثوقية 100%. تمت مكافأتك بـ +50 نقطة أيردروب!",
              timestampText = "الآن",
              isSystem = true
            )
          )
        } else deal
      }
    }
  }

  fun cancelDeal(dealId: String) {
    _activeDeals.update { list ->
      list.map { deal ->
        if (deal.dealId == dealId) {
          deal.copy(
            status = EscrowStatus.CANCELLED,
            chatHistory = deal.chatHistory + DealChatMessage(
              id = "sys-cancel-${System.currentTimeMillis()}",
              senderName = "نظام الأمانة المشفرة",
              text = "تم إلغاء الصفقة بأمان وتم إرجاع الأمانة المحجوزة للمحفظة بدون أي غرامات.",
              timestampText = "الآن",
              isSystem = true
            )
          )
        } else deal
      }
    }
  }

  fun disputeDeal(dealId: String, reason: String) {
    _activeDeals.update { list ->
      list.map { deal ->
        if (deal.dealId == dealId) {
          deal.copy(
            status = EscrowStatus.DISPUTED,
            disputeReason = reason,
            chatHistory = deal.chatHistory + DealChatMessage(
              id = "sys-dispute-${System.currentTimeMillis()}",
              senderName = "نظام التحكيم والنزاعات الذكي",
              text = "⚠️ تم فتح نزاع رسمي بسبب: $reason. سيقوم فريق التحكيم المالي بفحص كشف الحساب البنكي والبت في النزاع خلال 10 دقائق لحماية الطرفين.",
              timestampText = "الآن",
              isSystem = true
            )
          )
        } else deal
      }
    }
  }

  fun sendChatMessage(dealId: String, messageText: String) {
    _activeDeals.update { list ->
      list.map { deal ->
        if (deal.dealId == dealId) {
          deal.copy(
            chatHistory = deal.chatHistory + DealChatMessage(
              id = "user-${System.currentTimeMillis()}",
              senderName = "أنا",
              text = messageText,
              timestampText = "الآن",
              isMe = true
            )
          )
        } else deal
      }
    }
  }

  fun claimAirdrop(rewardId: String): Boolean {
    val reward = _airdropRewards.value.find { it.id == rewardId } ?: return false
    if (_userPoints.value >= reward.pointsRequired && !reward.isClaimed) {
      _userPoints.update { it - reward.pointsRequired }
      _airdropRewards.update { list ->
        list.map { if (it.id == rewardId) it.copy(isClaimed = true) else it }
      }
      return true
    }
    return false
  }

  fun postNewOffer(newOffer: MarketOffer) {
    _marketOffers.update { listOf(newOffer) + it }
  }

  private fun createInitialOffers(): List<MarketOffer> {
    val usdt = supportedCrypto[0]
    val usdc = supportedCrypto[1]
    val eurc = supportedCrypto[2]

    val usd = supportedFiat[0]
    val eur = supportedFiat[1]
    val sar = supportedFiat[2]
    val aed = supportedFiat[3]
    val egp = supportedFiat[4]
    val dzd = supportedFiat[5]

    return listOf(
      MarketOffer(
        id = "CORRIDOR-USDC-USD",
        tradeType = TradeType.BUY,
        asset = usdc,
        fiat = usd,
        pricePerUnit = 1.00,
        availableAmount = 50000.0,
        minLimitFiat = 10.0,
        maxLimitFiat = 10000.0,
        merchant = Merchant(
          id = "PROV-CIRCLE-RAIL",
          name = "USDC Direct Settlement Corridor",
          avatarInitials = "UC",
          ratingPercent = 100.0,
          completedOrders = 0,
          avgReleaseMinutes = 1.0,
          isVerified = true,
          isProMerchant = true,
          depositCollateralUsdt = 0.0
        ),
        acceptedPaymentMethods = listOf(paymentMethods[6], paymentMethods[2]),
        termsAndConditions = "Approved Stablecoin corridor (USDC Solana/Base). Non-custodial user authorization required.",
        autoReplyMsg = "Approved USDC settlement rail active. Verify recipient address before authorizing."
      ),
      MarketOffer(
        id = "CORRIDOR-USDT-SAR",
        tradeType = TradeType.BUY,
        asset = usdt,
        fiat = sar,
        pricePerUnit = 3.75,
        availableAmount = 45000.0,
        minLimitFiat = 50.0,
        maxLimitFiat = 25000.0,
        merchant = Merchant(
          id = "PROV-MENA-SAR",
          name = "SAR - USDT Regulated Corridor",
          avatarInitials = "SR",
          ratingPercent = 100.0,
          completedOrders = 0,
          avgReleaseMinutes = 1.5,
          isVerified = true,
          isProMerchant = true,
          depositCollateralUsdt = 0.0
        ),
        acceptedPaymentMethods = listOf(paymentMethods[0], paymentMethods[1]),
        termsAndConditions = "Settled via verified account name matching (Anti-Triangulation enforced).",
        autoReplyMsg = "Name verification and explicit 7-point confirmation required prior to execution."
      ),
      MarketOffer(
        id = "CORRIDOR-USDT-AED",
        tradeType = TradeType.BUY,
        asset = usdt,
        fiat = aed,
        pricePerUnit = 3.67,
        availableAmount = 40000.0,
        minLimitFiat = 100.0,
        maxLimitFiat = 30000.0,
        merchant = Merchant(
          id = "PROV-UAE-AED",
          name = "AED - USDT Settlement Rail",
          avatarInitials = "AE",
          ratingPercent = 100.0,
          completedOrders = 0,
          avgReleaseMinutes = 1.5,
          isVerified = true,
          isProMerchant = true,
          depositCollateralUsdt = 0.0
        ),
        acceptedPaymentMethods = listOf(paymentMethods[0], paymentMethods[6]),
        termsAndConditions = "UAE IBAN / Non-custodial TRC20 or Solana settlement.",
        autoReplyMsg = "Ready for transparent USD-pegged stablecoin settlement."
      ),
      MarketOffer(
        id = "CORRIDOR-USDT-DZD",
        tradeType = TradeType.BUY,
        asset = usdt,
        fiat = dzd,
        pricePerUnit = 134.50,
        availableAmount = 25000.0,
        minLimitFiat = 2000.0,
        maxLimitFiat = 500000.0,
        merchant = Merchant(
          id = "PROV-DZ-CORRIDOR",
          name = "DZ Stablecoin Corridor (Pending Bank Licensing)",
          avatarInitials = "DZ",
          ratingPercent = 100.0,
          completedOrders = 0,
          avgReleaseMinutes = 2.0,
          isVerified = true,
          isProMerchant = false,
          depositCollateralUsdt = 0.0
        ),
        acceptedPaymentMethods = listOf(paymentMethods[3], paymentMethods[6]),
        termsAndConditions = "DZD reference rail subject to local financial compliance and BaridiMob bilateral integration.",
        autoReplyMsg = "Non-custodial stablecoin routing with transparent fees."
      ),
      MarketOffer(
        id = "CORRIDOR-EURC-EUR",
        tradeType = TradeType.BUY,
        asset = eurc,
        fiat = eur,
        pricePerUnit = 1.00,
        availableAmount = 35000.0,
        minLimitFiat = 20.0,
        maxLimitFiat = 20000.0,
        merchant = Merchant(
          id = "PROV-EU-EURC",
          name = "EURC MiCA Euro Corridor",
          avatarInitials = "EU",
          ratingPercent = 100.0,
          completedOrders = 0,
          avgReleaseMinutes = 1.2,
          isVerified = true,
          isProMerchant = true,
          depositCollateralUsdt = 0.0
        ),
        acceptedPaymentMethods = listOf(paymentMethods[2], paymentMethods[5]),
        termsAndConditions = "MiCA-aligned EURC on Base L2 / SEPA reference corridor.",
        autoReplyMsg = "EURC corridor verified under EU stablecoin registry."
      ),
      MarketOffer(
        id = "CORRIDOR-USDT-EGP",
        tradeType = TradeType.BUY,
        asset = usdt,
        fiat = egp,
        pricePerUnit = 49.20,
        availableAmount = 20000.0,
        minLimitFiat = 500.0,
        maxLimitFiat = 150000.0,
        merchant = Merchant(
          id = "PROV-EG-CORRIDOR",
          name = "EGP - USDT Settlement Corridor",
          avatarInitials = "EG",
          ratingPercent = 100.0,
          completedOrders = 0,
          avgReleaseMinutes = 2.5,
          isVerified = true,
          isProMerchant = false,
          depositCollateralUsdt = 0.0
        ),
        acceptedPaymentMethods = listOf(paymentMethods[4], paymentMethods[0]),
        termsAndConditions = "InstaPay / Bank settlement reference with mandatory sender proof.",
        autoReplyMsg = "Verified stablecoin corridor ready."
      )
    )
  }

  private fun createInitialAirdrops(): List<AirdropReward> {
    return listOf(
      AirdropReward(
        id = "AIR-1",
        titleAr = "أيردروب شكر الثقة (20$ USDT)",
        titleEn = "Trust Airdrop (20$ USDT)",
        pointsRequired = 500,
        rewardValueUsdt = 20.0,
        isClaimed = false,
        tag = "متاح فوراً"
      ),
      AirdropReward(
        id = "AIR-2",
        titleAr = "قسيمة 10 صفقات مجانية (بدون رسوم 0.8$)",
        titleEn = "10 Zero-Fee Trade Pass",
        pointsRequired = 800,
        rewardValueUsdt = 8.0,
        isClaimed = false,
        tag = "شائع جداً"
      ),
      AirdropReward(
        id = "AIR-3",
        titleAr = "أيردروب المتداول الذهبي النزيه (100$ USDT)",
        titleEn = "Golden Honest Trader Drop (100$)",
        pointsRequired = 2500,
        rewardValueUsdt = 100.0,
        isClaimed = false,
        tag = "VIP حصري"
      )
    )
  }
}
