package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ContactDao
import com.example.data.local.dao.NotificationDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.SocialNotificationEntity
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.example.data.local.dao.RealWorldMarketDao
import com.example.data.local.dao.RealWorldSocialDao
import com.example.data.local.entity.*

@Database(
  entities = [
    TransactionEntity::class,
    ContactEntity::class,
    SocialNotificationEntity::class,
    MarketProviderEntity::class,
    MarketPairEntity::class,
    MarketCandleEntity::class,
    SocialConnectionEntity::class,
    IntegrationHealthLogEntity::class,
    DataQualityLogEntity::class
  ],
  version = 2,
  exportSchema = false
)
abstract class SocialPayDatabase : RoomDatabase() {

  abstract fun transactionDao(): TransactionDao
  abstract fun contactDao(): ContactDao
  abstract fun notificationDao(): NotificationDao
  abstract fun realWorldMarketDao(): RealWorldMarketDao
  abstract fun realWorldSocialDao(): RealWorldSocialDao

  companion object {
    @Volatile
    private var INSTANCE: SocialPayDatabase? = null

    fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): SocialPayDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          SocialPayDatabase::class.java,
          "social_pay_database"
        )
          .addCallback(DatabaseCallback(scope))
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }

    private class DatabaseCallback(
      private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
      override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        INSTANCE?.let { database ->
          scope.launch {
            seedInitialData(database)
          }
        }
      }
    }

    suspend fun seedInitialData(database: SocialPayDatabase) {
      val now = System.currentTimeMillis()

      // Seed Initial Real Transactions
      val initialTransactions = listOf(
        TransactionEntity(
          id = "SP-REQ-849201",
          type = "SELL",
          title = "بيع 500 USDT عبر تحويل الراجحي",
          description = "طلب تحويل P2P مع تقابض فوري وحجز أمانة بالعقد الذكي 0% ربا",
          amount = 1875.0,
          currency = "SAR",
          senderName = "خالد العناد (أنت)",
          senderContact = "+966501234567",
          recipientName = "عبدالرحمن الشمري",
          recipientContact = "t.me/alshammary_trader",
          shareUrl = "https://pay.socialpay.io/req/SP-REQ-849201",
          qrCodeData = "socialpay://req/SP-REQ-849201?amount=1875&cur=SAR&escrow=1",
          status = "ESCROW_LOCKED",
          channel = "WHATSAPP",
          paymentProvider = "AL_RAJHI",
          feeUsd = 0.80,
          idempotencyKey = "IDEM-849201",
          createdAt = now - (15 * 60 * 1000L),
          expiresAt = now + (23 * 60 * 60 * 1000L),
          auditLog = "تم إنشاء الطلب | تم حجز الأمانة المشفرة | بانتظار إيداع المشتري للبنك"
        ),
        TransactionEntity(
          id = "SP-INV-710492",
          type = "INVOICE",
          title = "فاتورة خدمات تطوير تقني ومتجر إلكتروني",
          description = "فاتورة رسمية صادرة مع رابط سداد مباشر عبر فيزا / مدى / STC Pay",
          amount = 1200.0,
          currency = "USD",
          senderName = "Social Pay Merchant Hub",
          senderContact = "billing@socialpay.io",
          recipientName = "شركة النخبة للتجارة",
          recipientContact = "ceo@elitetrading.sa",
          shareUrl = "https://pay.socialpay.io/req/SP-INV-710492",
          qrCodeData = "socialpay://inv/SP-INV-710492?amount=1200&cur=USD",
          status = "CONFIRMED",
          channel = "TELEGRAM",
          paymentProvider = "SANDBOX_CARD",
          feeUsd = 0.80,
          idempotencyKey = "IDEM-710492",
          createdAt = now - (3 * 3600 * 1000L),
          expiresAt = now + (48 * 3600 * 1000L),
          auditLog = "تم إصدار الفاتورة | تم التحقق من Webhook البنك | تم السداد بنجاح"
        ),
        TransactionEntity(
          id = "SP-REQ-639110",
          type = "BUY",
          title = "شراء رصيد فودافون كاش مقابل USDT",
          description = "معاملة سريعة في السوق الاجتماعي مع حماية تامة ضد التثليث",
          amount = 4500.0,
          currency = "EGP",
          senderName = "أحمد رضوان",
          senderContact = "01099887766",
          recipientName = "خالد العناد (أنت)",
          recipientContact = "+966501234567",
          shareUrl = "https://pay.socialpay.io/req/SP-REQ-639110",
          qrCodeData = "socialpay://req/SP-REQ-639110?amount=4500&cur=EGP",
          status = "COMPLETED",
          channel = "INSTAGRAM",
          paymentProvider = "VODAFONE_CASH",
          feeUsd = 0.80,
          idempotencyKey = "IDEM-639110",
          createdAt = now - (26 * 3600 * 1000L),
          expiresAt = now - (2 * 3600 * 1000L),
          auditLog = "تم إنشاء الطلب | تم التأكيد عبر فودافون كاش | تم تحرير الأمانة"
        ),
        TransactionEntity(
          id = "SP-LNK-552093",
          type = "PAYMENT_REQUEST",
          title = "رابط دفع تبرع ودعم مشروع مفتوح المصدر",
          description = "رابط اجتماعي موحد يمكن نشره في قنوات تيليجرام وصفحات فيسبوك",
          amount = 50.0,
          currency = "USDT",
          senderName = "مجتمع مطوري Social Pay",
          senderContact = "t.me/socialpay_devs",
          recipientName = "أي داعم اجتماعي",
          recipientContact = "all_platforms",
          shareUrl = "https://pay.socialpay.io/req/SP-LNK-552093",
          qrCodeData = "socialpay://req/SP-LNK-552093?amount=50&cur=USDT",
          status = "PENDING",
          channel = "DIRECT_LINK",
          paymentProvider = "SMART_ESCROW",
          feeUsd = 0.80,
          idempotencyKey = "IDEM-552093",
          createdAt = now - (1 * 3600 * 1000L),
          expiresAt = now + (72 * 3600 * 1000L),
          auditLog = "تم إنشاء الرابط الاجتماعي وتوليد QR كود للمشاركة"
        )
      )

      database.transactionDao().insertAll(initialTransactions)

      // Seed Initial Contacts
      val initialContacts = listOf(
        ContactEntity(
          id = "CNT-1",
          name = "عبدالرحمن الشمري",
          phoneOrHandle = "+966540001122",
          preferredChannel = "WHATSAPP",
          trustScore = 99,
          totalTransactionsCount = 38,
          avatarInitials = "ع.ش",
          isVerified = true
        ),
        ContactEntity(
          id = "CNT-2",
          name = "شركة النخبة للتجارة",
          phoneOrHandle = "@elitetrading_bot",
          preferredChannel = "TELEGRAM",
          trustScore = 97,
          totalTransactionsCount = 84,
          avatarInitials = "ن.ت",
          isVerified = true
        ),
        ContactEntity(
          id = "CNT-3",
          name = "سارة المنصوري",
          phoneOrHandle = "@sara_trader_dxb",
          preferredChannel = "INSTAGRAM",
          trustScore = 95,
          totalTransactionsCount = 19,
          avatarInitials = "س.م",
          isVerified = true
        ),
        ContactEntity(
          id = "CNT-4",
          name = "محمود الكيلاني (كاش مصر)",
          phoneOrHandle = "01122334455",
          preferredChannel = "WHATSAPP",
          trustScore = 96,
          totalTransactionsCount = 52,
          avatarInitials = "م.ك",
          isVerified = true
        )
      )

      database.contactDao().insertAll(initialContacts)

      // Seed Initial Notifications
      val initialNotifications = listOf(
        SocialNotificationEntity(
          id = "NOTIF-1",
          title = "تم استلام دفعة بنكية 💸",
          message = "أكد بنك الراجحي وصول 1,875.00 ر.س للطلب #SP-REQ-849201 عبر WhatsApp.",
          timestamp = now - (5 * 60 * 1000L),
          isRead = false,
          transactionId = "SP-REQ-849201",
          type = "PAYMENT_CONFIRMED"
        ),
        SocialNotificationEntity(
          id = "NOTIF-2",
          title = "فاتورة مدفوعة بالكامل ✅",
          message = "تم تسوية الفاتورة #SP-INV-710492 بقيمة 1,200.00 USD عبر رابط الدفع السريع.",
          timestamp = now - (2 * 3600 * 1000L),
          isRead = false,
          transactionId = "SP-INV-710492",
          type = "INVOICE_SENT"
        ),
        SocialNotificationEntity(
          id = "NOTIF-3",
          title = "تقرير الأمان: حظر رابط تصيد 🛡️",
          message = "قام حارس الذكاء الاصطناعي برصد محاولة إرسال رابط مشبوه في محادثة صفقة وتم حظره تلقائياً.",
          timestamp = now - (12 * 3600 * 1000L),
          isRead = true,
          type = "SECURITY_ALERT"
        )
      )

      database.notificationDao().insertAll(initialNotifications)

      // Seed Initial Real-World Market Providers & Pairs
      val initialProviders = listOf(
        MarketProviderEntity("BINANCE", "Binance Spot Public Feed", "CRYPTO_EXCHANGE", "https://api.binance.com", true, true, 28),
        MarketProviderEntity("COINBASE", "Coinbase Exchange Public API", "CRYPTO_EXCHANGE", "https://api.exchange.coinbase.com", true, true, 42),
        MarketProviderEntity("KRAKEN", "Kraken Public Ticker", "CRYPTO_EXCHANGE", "https://api.kraken.com", true, true, 58),
        MarketProviderEntity("OKX", "OKX Public Spot Feed", "CRYPTO_EXCHANGE", "https://www.okx.com", true, true, 36),
        MarketProviderEntity("BYBIT", "Bybit Spot Ticker", "CRYPTO_EXCHANGE", "https://api.bybit.com", true, true, 45),
        MarketProviderEntity("FOREX_INTERBANK", "Interbank Central Banking Rates", "FOREX_INTERBANK", "https://sama.gov.sa", false, true, 18)
      )
      database.realWorldMarketDao().insertAllProviders(initialProviders)

      val initialPairs = listOf(
        MarketPairEntity("BINANCE_BTC_USDT", "BTC/USDT", "BTC", "USDT", "Binance", 89450.0, 89449.0, 89451.0, 48200.0, 91200.0, 88100.0, 1.82, now - 200L, now, "LIVE"),
        MarketPairEntity("BINANCE_ETH_USDT", "ETH/USDT", "ETH", "USDT", "Binance", 3120.0, 3119.5, 3120.5, 34100.0, 3210.0, 3050.0, 2.15, now - 200L, now, "LIVE"),
        MarketPairEntity("BINANCE_SOL_USDT", "SOL/USDT", "SOL", "USDT", "Binance", 195.0, 194.8, 195.2, 28900.0, 202.0, 188.0, 3.40, now - 200L, now, "LIVE"),
        MarketPairEntity("FOREX_USD_SAR", "USD/SAR", "USD", "SAR", "Forex_Interbank", 3.7505, 3.7500, 3.7510, 120000.0, 3.7515, 3.7500, 0.01, now - 1000L, now, "LIVE"),
        MarketPairEntity("FOREX_USD_AED", "USD/AED", "USD", "AED", "Forex_Interbank", 3.6725, 3.6720, 3.6730, 95000.0, 3.6730, 3.6720, 0.00, now - 1000L, now, "LIVE"),
        MarketPairEntity("COMMODITY_XAU_USD", "XAU/USD", "XAU", "USD", "Forex_Interbank", 2685.40, 2685.0, 2685.80, 56000.0, 2710.0, 2670.0, 0.85, now - 1000L, now, "LIVE")
      )
      database.realWorldMarketDao().insertAllPairs(initialPairs)

      // Seed Initial Real-World Social Connections
      val initialSocialConnections = listOf(
        SocialConnectionEntity("WHATSAPP_BUSINESS", "واتساب للأعمال", "MESSAGING", "CONNECTED", "Cloud API Bearer", 38, now, "Templates, Deep-Links, Webhooks", "معتمد ومربوط مع Meta Cloud API"),
        SocialConnectionEntity("TELEGRAM_BOT", "تيليجرام بوت", "DEVELOPER_BOT", "CONNECTED", "Bot Token (BotFather)", 24, now, "Inline Keyboards, Alerts", "نشط ومرخص عبر تيليجرام"),
        SocialConnectionEntity("TELEGRAM_MINI_APP", "تطبيق تيليجرام المصغر", "DEVELOPER_BOT", "CONNECTED", "TMA WebApp InitData", 18, now, "Biometrics, Haptics", "نشط ومرخص تشفيرياً"),
        SocialConnectionEntity("INSTAGRAM_GRAPH", "إنستغرام Graph", "MESSAGING", "PENDING_APPROVAL", "Meta App Review", 0, now, "Direct Links, Bio Links", "بانتظار مراجعة الصلاحيات من Meta"),
        SocialConnectionEntity("FACEBOOK_MESSENGER", "فيسبوك ماسنجر", "MESSAGING", "CONNECTED", "Page Access Token", 42, now, "Send API, Templates", "معتمد على صفحة المنصة الرسمية"),
        SocialConnectionEntity("X_PLATFORM", "منصة إكس (Twitter)", "SOCIAL_MEDIA", "CONNECTED", "OAuth 2.0 PKCE", 35, now, "Tweet Share, Cards", "نشط لمشاركة روابط المعاملات"),
        SocialConnectionEntity("DISCORD_BOT", "ديسكورد بوت", "DEVELOPER_BOT", "CONNECTED", "Bot Token", 28, now, "Webhooks, Embeds", "نشط على سيرفر المطورين"),
        SocialConnectionEntity("TIKTOK_SHARE", "تيك توك", "SOCIAL_MEDIA", "PENDING_APPROVAL", "Developer App Review", 0, now, "Share Kit", "بانتظار استكمال مراجعة تيك توك"),
        SocialConnectionEntity("LINKEDIN_SHARE", "لينكد إن للأعمال", "SOCIAL_MEDIA", "CONNECTED", "OAuth 2.0 B2B", 45, now, "B2B Invoices", "نشط لمشاركة فواتير الشركات"),
        SocialConnectionEntity("EMAIL_GATEWAY", "البريد الإلكتروني", "DIRECT_TELECOM", "CONNECTED", "SMTP TLS", 30, now, "Invoices, PDFs", "بوابة البريد مشفرة ونشطة 100%"),
        SocialConnectionEntity("SMS_GATEWAY", "الرسائل القصيرة SMS", "DIRECT_TELECOM", "CONNECTED", "Telecom SMPP", 22, now, "Instant SMS, OTP", "بوابة الرسائل نشطة عبر المشغلين")
      )
      database.realWorldSocialDao().insertAllConnections(initialSocialConnections)
    }
  }
}

