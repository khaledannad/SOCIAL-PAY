package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.core.governance.tr
import com.example.data.CredentialAuthHelper
import com.example.data.PlatformGovernanceRepository
import com.example.model.AppLanguage
import com.example.ui.components.DirectFeedbackDialog
import com.example.ui.components.SocialPayLogo
import com.example.ui.components.SocialPayLogoStyle
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
  currentLanguage: AppLanguage,
  onLanguageSelected: (AppLanguage) -> Unit,
  onRequestPartnerAfterAuth: () -> Unit = {},
  onRequestOwnerAfterAuth: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val auth = remember { FirebaseAuth.getInstance() }
  val governanceRepo = remember(context) { PlatformGovernanceRepository(context) }
  val isRtl = currentLanguage.isRtl

  var isAutoSigningIn by remember { mutableStateOf(true) }
  var isInteractiveSigningIn by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var infoMessage by remember { mutableStateOf<String?>(null) }
  var showDirectFeedbackDialog by remember { mutableStateOf(false) }
  var showLiveCommentaireScreen by remember { mutableStateOf(false) }
  var queuedFeedbackCount by remember { mutableStateOf(governanceRepo.pendingLocalFeedbackCount()) }

  // Silent single-option auto sign-in for returning authorized accounts
  LaunchedEffect(Unit) {
    CredentialAuthHelper.attemptSilentGoogleSignIn(context, auth)
    isAutoSigningIn = false
  }

  val startGoogleSignIn: () -> Unit = {
    if (!isInteractiveSigningIn) {
      coroutineScope.launch {
        isInteractiveSigningIn = true
        errorMessage = null
        infoMessage = null
        val result = CredentialAuthHelper.signInWithGoogleInteractive(context, auth)
        isInteractiveSigningIn = false
        result.onFailure { err ->
          if (err is GetCredentialCancellationException) {
            infoMessage = tr(
              currentLanguage,
              ar = "تم إلغاء نافذة تسجيل الدخول. اختر حساب Google للمتابعة.",
              en = "Sign-in was cancelled. Select a Google account to continue.",
              fr = "Connexion annulée. Sélectionnez un compte Google pour continuer.",
              ru = "Вход отменен. Выберите аккаунт Google для продолжения.",
              zh = "登录已取消。请选择 Google 账户以继续。"
            )
          } else {
            errorMessage = err.localizedMessage ?: tr(
              currentLanguage,
              ar = "تعذر تسجيل الدخول عبر Google. تحقق من الاتصال وحاول مجدداً.",
              en = "Google Sign-In failed. Check your connection and try again.",
              fr = "Échec de la connexion Google. Vérifiez votre réseau et réessayez.",
              ru = "Ошибка входа через Google. Проверьте сеть и повторите попытку.",
              zh = "Google 登录失败。请检查网络后重试。"
            )
          }
        }
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            BrandPrimaryBg,
            Color(0xFF0A2230),
            BrandPrimaryBg
          )
        )
      )
      .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 520.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Top Language Bar + Owner PIN Entry Button
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = {
              onRequestOwnerAfterAuth()
              infoMessage = tr(
                currentLanguage,
                ar = "اختر حساب المالك عبر Google ثم أدخل الرقم السري للمالك.",
                en = "Sign in with the owner Google account to verify your Owner PIN.",
                fr = "Connectez-vous avec le compte propriétaire puis saisissez le PIN.",
                ru = "Войдите в аккаунт владельца Google для ввода секретного PIN.",
                zh = "使用所有者 Google 账户登录以验证所有者密码。"
              )
              startGoogleSignIn()
            },
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.55f)),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier
              .heightIn(min = 40.dp)
              .testTag("auth_owner_login_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = "Owner Login with PIN",
              tint = GoldAccent,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = tr(
                currentLanguage,
                ar = "دخول المالك",
                en = "Owner Portal",
                fr = "Accès Propriétaire",
                ru = "Вход владельца",
                zh = "所有者入口"
              ),
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = GoldAccent
            )
          }

          // Single Unified Language Selector Button
          var showLanguageMenuDialog by remember { mutableStateOf(false) }

          OutlinedButton(
            onClick = { showLanguageMenuDialog = true },
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, BrandPrimaryAccent.copy(alpha = 0.55f)),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = BrandSecondaryBg,
              contentColor = BrandPrimaryAccent
            ),
            modifier = Modifier
              .heightIn(min = 40.dp)
              .testTag("auth_single_language_btn")
          ) {
            Text(text = currentLanguage.flag, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = currentLanguage.displayName,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = BrandTextPrimary
            )
            Spacer(modifier = Modifier.width(3.dp))
            Icon(
              imageVector = Icons.Default.ArrowDropDown,
              contentDescription = "Select Language",
              tint = BrandPrimaryAccent,
              modifier = Modifier.size(16.dp)
            )
          }

          if (showLanguageMenuDialog) {
            AlertDialog(
              onDismissRequest = { showLanguageMenuDialog = false },
              containerColor = BrandSecondaryBg,
              title = {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(Icons.Default.Translate, contentDescription = null, tint = BrandPrimaryAccent)
                  Text(
                    text = tr(currentLanguage, "اختر لغة التطبيق", "Select Language", "Choisir la langue", "Выберите язык", "选择语言"),
                    fontWeight = FontWeight.Bold,
                    color = BrandTextPrimary,
                    style = MaterialTheme.typography.titleMedium
                  )
                }
              },
              text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  AppLanguage.entries.forEach { lang ->
                    val isSelected = lang == currentLanguage
                    Surface(
                      color = if (isSelected) BrandPrimaryAccent.copy(alpha = 0.2f) else BrandPrimaryBg,
                      shape = RoundedCornerShape(10.dp),
                      border = if (isSelected) BorderStroke(1.5.dp, BrandPrimaryAccent) else BorderStroke(1.dp, BrandSurfaceBorder),
                      modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                          onLanguageSelected(lang)
                          showLanguageMenuDialog = false
                        }
                        .testTag("lang_option_${lang.code}")
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                      ) {
                        Text(text = lang.flag, fontSize = 20.sp)
                        Text(
                          text = lang.displayName,
                          fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                          color = if (isSelected) BrandPrimaryAccent else BrandTextPrimary,
                          fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (isSelected) {
                          Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = BrandPrimaryAccent,
                            modifier = Modifier.size(18.dp)
                          )
                        }
                      }
                    }
                  }
                }
              },
              confirmButton = {
                TextButton(onClick = { showLanguageMenuDialog = false }) {
                  Text(text = tr(currentLanguage, "إغلاق", "Close", "Fermer", "Закрыть", "关闭"), color = BrandPrimaryAccent)
                }
              }
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Hero Banner Card with Custom Art + Vertical Brand Logo
        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg),
          border = BorderStroke(
            1.2.dp,
            Brush.linearGradient(
              listOf(
                BrandPrimaryAccent.copy(alpha = 0.6f),
                BrandSecondaryAccent.copy(alpha = 0.4f),
                BrandIntelligenceAccent.copy(alpha = 0.6f)
              )
            )
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(144.dp)
          ) {
            Image(
              painter = painterResource(id = R.drawable.social_pay_hero),
              contentDescription = "Social Pay Secure Hub",
              contentScale = ContentScale.Crop,
              alpha = 0.28f,
              modifier = Modifier.fillMaxSize()
            )
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(
                  Brush.verticalGradient(
                    listOf(
                      BrandPrimaryBg.copy(alpha = 0.35f),
                      BrandSecondaryBg.copy(alpha = 0.92f)
                    )
                  )
                ),
              contentAlignment = Alignment.Center
            ) {
              SocialPayLogo(
                style = SocialPayLogoStyle.FULL_VERTICAL,
                iconSize = 58.dp,
                showTagline = true
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
          text = tr(
            currentLanguage,
            ar = "تسجيل الدخول الآمن لإدارة حسابك",
            en = "Secure Sign-In & Account Portal",
            fr = "Connexion sécurisée à votre profil",
            ru = "Безопасный вход и управление аккаунтом",
            zh = "安全登录与账户管理"
          ),
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.ExtraBold,
          color = BrandTextPrimary,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = tr(
            currentLanguage,
            ar = "قم بمزامنة ملفك الشخصي وربط حساباتك البنكية وإصدار رموز QR للمدفوعات الفورية عبر Firebase Authentication.",
            en = "Manage your profile, link bank payout accounts, and generate scannable P2P wallet QR requests securely.",
            fr = "Synchronisez votre profil, liez vos comptes et générez des QR codes P2P via Google et Firebase.",
            ru = "Синхронизируйте профиль, привязывайте счета и создавайте QR-коды для мгновенных платежей.",
            zh = "同步您的个人资料，绑定银行收款账户并通过二维码进行点对点即时收付款。"
          ),
          style = MaterialTheme.typography.bodyMedium,
          color = BrandTextSecondary,
          textAlign = TextAlign.Center,
          lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Security Pillars Card
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg.copy(alpha = 0.85f)),
          border = BorderStroke(1.dp, BrandSurfaceBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            SecurityFeatureRow(
              icon = Icons.Default.QrCode2,
              accent = BrandPrimaryAccent,
              title = tr(
                currentLanguage,
                ar = "محفظة QR وطلبات P2P قابلة للمسح",
                en = "Scannable ZXing Wallet QR Requests",
                fr = "QR Code Portefeuille P2P Scannable",
                ru = "QR-кошелек и сканируемые P2P запросы",
                zh = "可扫描的 ZXing 钱包二维码收付款"
              ),
              subtitle = tr(
                currentLanguage,
                ar = "أنشئ وامسح رموز QR الحقيقية لطلبات الدفع بين النظراء دون حجز أموال.",
                en = "Create and scan real ISO QR payment requests peer-to-peer with non-custodial rails.",
                fr = "Générez et scannez de vrais QR codes de paiement P2P.",
                ru = "Создавайте и сканируйте реальные QR-коды для прямых платежей.",
                zh = "创建并扫描标准二维码进行点对点支付，非托管结算。"
              )
            )
            HorizontalDivider(color = BrandSurfaceBorder.copy(alpha = 0.6f))
            SecurityFeatureRow(
              icon = Icons.Default.AccountBalance,
              accent = BrandSecondaryAccent,
              title = tr(
                currentLanguage,
                ar = "ربط الحسابات البنكية والتسجيل للشركاء",
                en = "Linked Payouts & All-Sector Partners",
                fr = "Comptes Bancaires & Partenaires Multi-Secteurs",
                ru = "Привязка счетов и партнерская регистрация",
                zh = "绑定收款账户与全行业合作入驻"
              ),
              subtitle = tr(
                currentLanguage,
                ar = "اربط حساباتك أو سجّل شركتك في جميع القطاعات التجارية مع عزل سحابي صارم.",
                en = "Link IBANs and register companies across 12 sectors under zero-trust Firestore isolation.",
                fr = "Liez vos IBAN et enregistrez votre entreprise dans tous les secteurs.",
                ru = "Привязывайте IBAN и регистрируйте компании в 12 секторах бизнеса.",
                zh = "绑定 IBAN 并在 12 个行业领域入驻您的企业，零信任数据隔离。"
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error or Info Banner
        AnimatedVisibility(visible = errorMessage != null || infoMessage != null) {
          val isErr = errorMessage != null
          Surface(
            color = if (isErr) StatusError.copy(alpha = 0.14f) else BrandSecondaryAccent.copy(alpha = 0.14f),
            border = BorderStroke(
              1.dp,
              if (isErr) StatusError.copy(alpha = 0.6f) else BrandSecondaryAccent.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 12.dp)
              .testTag("auth_status_banner")
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(
                imageVector = if (isErr) Icons.Default.ErrorOutline else Icons.Default.Info,
                contentDescription = null,
                tint = if (isErr) StatusError else BrandSecondaryAccent,
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = errorMessage ?: infoMessage.orEmpty(),
                color = BrandTextPrimary,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
              )
            }
          }
        }

        // Primary Google Sign-In CTA
        Button(
          onClick = startGoogleSignIn,
          enabled = !isInteractiveSigningIn && !isAutoSigningIn,
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = BrandPrimaryAccent,
            contentColor = BrandPrimaryBg,
            disabledContainerColor = BrandPrimaryAccent.copy(alpha = 0.45f),
            disabledContentColor = BrandPrimaryBg
          ),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag("auth_google_signin_button")
        ) {
          if (isInteractiveSigningIn || isAutoSigningIn) {
            CircularProgressIndicator(
              color = BrandPrimaryBg,
              strokeWidth = 2.5.dp,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = if (isAutoSigningIn) {
                tr(
                  currentLanguage,
                  ar = "جاري التحقق من الجلسة الآمنة...",
                  en = "Checking secure session...",
                  fr = "Vérification de la session...",
                  ru = "Проверка безопасной сессии...",
                  zh = "正在验证安全会话..."
                )
              } else {
                tr(
                  currentLanguage,
                  ar = "جاري الاتصال بحساب Google...",
                  en = "Signing in with Google...",
                  fr = "Connexion à Google...",
                  ru = "Подключение к Google...",
                  zh = "正在连接 Google..."
                )
              },
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp
            )
          } else {
            Icon(
              imageVector = Icons.Default.AccountCircle,
              contentDescription = "Google Sign-In",
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = tr(
                currentLanguage,
                ar = "المتابعة بحساب Google",
                en = "Continue with Google",
                fr = "Continuer avec Google",
                ru = "Продолжить с Google",
                zh = "通过 Google 账户继续"
              ),
              fontWeight = FontWeight.ExtraBold,
              fontSize = 16.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Secondary Registration Actions Row: Partner Portal (All Sectors) + LIVE COMMENTAIRE 🌍
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = {
              onRequestPartnerAfterAuth()
              infoMessage = tr(
                currentLanguage,
                ar = "سجّل الدخول بحساب Google لإكمال تسجيل شركتك في أي قطاع تجاري.",
                en = "Sign in with Google to open All-Sector Partner & Company Registration.",
                fr = "Connectez-vous avec Google pour enregistrer votre entreprise partenaire.",
                ru = "Войдите через Google для регистрации партнерской компании.",
                zh = "通过 Google 登录以完成全行业企业合作入驻。"
              )
              startGoogleSignIn()
            },
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, BrandSecondaryAccent.copy(alpha = 0.65f)),
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 52.dp)
              .testTag("auth_partner_portal_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Handshake,
              contentDescription = "Partner Registration",
              tint = BrandSecondaryAccent,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = tr(
                currentLanguage,
                ar = "تسجيل الشركاء",
                en = "Partner Signup",
                fr = "Partenaires",
                ru = "Партнеры",
                zh = "合作注册"
              ),
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = BrandSecondaryAccent
            )
          }

          // LIVE COMMENTAIRE 🌍 — Worldwide Open Social Space
          OutlinedButton(
            onClick = { showLiveCommentaireScreen = true },
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(
              1.2.dp,
              Brush.horizontalGradient(
                listOf(BrandPrimaryAccent, BrandSecondaryAccent, BrandIntelligenceAccent)
              )
            ),
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = BrandSecondaryBg.copy(alpha = 0.9f)
            ),
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 52.dp)
              .testTag("auth_live_commentaire_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Public,
              contentDescription = "LIVE COMMENTAIRE 🌍",
              tint = BrandPrimaryAccent,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
              Text(
                text = "LIVE COMMENTAIRE",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BrandPrimaryAccent
              )
              Text(
                text = tr(
                  currentLanguage,
                  ar = "مجتمع عالمي 🌍",
                  en = "Global Space 🌍",
                  fr = "Espace Mondial 🌍",
                  ru = "Мировое сообщество 🌍",
                  zh = "全球社区 🌍"
                ),
                fontSize = 9.sp,
                color = BrandTextSecondary
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Footer note
      Text(
        text = if (isRtl) {
          "محمي بواسطة Firebase Authentication و Google Credential Manager"
        } else {
          "Protected by Firebase Authentication & Google Credential Manager"
        },
        fontSize = 11.sp,
        color = BrandTextMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp)
      )
    }
  }

  if (showDirectFeedbackDialog) {
    DirectFeedbackDialog(
      isRtl = isRtl,
      isUserSignedIn = false,
      onDismiss = { showDirectFeedbackDialog = false },
      onSubmitFeedback = { name, email, category, rating, message ->
        val res = governanceRepo.queuePreLoginFeedback(name, email, category, rating, message)
        showDirectFeedbackDialog = false
        res.onSuccess { count ->
          queuedFeedbackCount = count
          errorMessage = null
          infoMessage = if (isRtl) {
            "شكراً لمساهمتك! تم حفظ تعليقك وسيتم رفعه للسحابة فور تسجيل الدخول."
          } else {
            "Thank you! Your feedback note is queued and will sync automatically when you sign in."
          }
        }.onFailure { err ->
          errorMessage = err.localizedMessage
        }
      }
    )
  }

  if (showLiveCommentaireScreen) {
    LiveCommentaireScreen(
      currentLanguage = currentLanguage,
      onDismiss = { showLiveCommentaireScreen = false }
    )
  }
}

@Composable
private fun SecurityFeatureRow(
  icon: ImageVector,
  accent: Color,
  title: String,
  subtitle: String
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Box(
      modifier = Modifier
        .size(38.dp)
        .clip(CircleShape)
        .background(accent.copy(alpha = 0.16f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = accent,
        modifier = Modifier.size(20.dp)
      )
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = BrandTextPrimary
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        color = BrandTextSecondary
      )
    }
  }
}
