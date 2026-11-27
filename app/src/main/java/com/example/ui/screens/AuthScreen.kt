package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
            infoMessage = when (currentLanguage) {
              AppLanguage.ARABIC -> "تم إلغاء نافذة تسجيل الدخول. اختر حساب Google للمتابعة."
              AppLanguage.FRENCH -> "Connexion annulée. Sélectionnez un compte Google pour continuer."
              AppLanguage.ENGLISH -> "Sign-in was cancelled. Select a Google account to continue."
            }
          } else {
            errorMessage = err.localizedMessage ?: when (currentLanguage) {
              AppLanguage.ARABIC -> "تعذر تسجيل الدخول عبر Google. تحقق من الاتصال وحاول مجدداً."
              AppLanguage.FRENCH -> "Échec de la connexion Google. Vérifiez votre réseau et réessayez."
              AppLanguage.ENGLISH -> "Google Sign-In failed. Check your connection and try again."
            }
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
              infoMessage = when (currentLanguage) {
                AppLanguage.ARABIC -> "اختر حساب المالك عبر Google ثم أدخل الرقم السري للمالك."
                AppLanguage.FRENCH -> "Connectez-vous avec le compte propriétaire puis saisissez le PIN."
                AppLanguage.ENGLISH -> "Sign in with the owner Google account to verify your Owner PIN."
              }
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
              text = when (currentLanguage) {
                AppLanguage.ARABIC -> "دخول المالك"
                AppLanguage.FRENCH -> "Accès Propriétaire"
                AppLanguage.ENGLISH -> "Owner PIN"
              },
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = GoldAccent
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            AppLanguage.entries.forEach { lang ->
              val selected = lang == currentLanguage
              FilterChip(
                selected = selected,
                onClick = { onLanguageSelected(lang) },
                label = {
                  Text(
                    text = lang.code.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                  )
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = BrandPrimaryAccent.copy(alpha = 0.2f),
                  selectedLabelColor = BrandPrimaryAccent,
                  labelColor = BrandTextSecondary
                ),
                modifier = Modifier.testTag("auth_lang_${lang.code}")
              )
            }
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
          text = when (currentLanguage) {
            AppLanguage.ARABIC -> "تسجيل الدخول الآمن لإدارة حسابك"
            AppLanguage.FRENCH -> "Connexion sécurisée à votre profil"
            AppLanguage.ENGLISH -> "Secure Sign-In & Account Portal"
          },
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.ExtraBold,
          color = BrandTextPrimary,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = when (currentLanguage) {
            AppLanguage.ARABIC -> "قم بمزامنة ملفك الشخصي وربط حساباتك البنكية وإصدار رموز QR للمدفوعات الفورية عبر Firebase Authentication."
            AppLanguage.FRENCH -> "Synchronisez votre profil, liez vos comptes et générez des QR codes P2P via Google et Firebase."
            AppLanguage.ENGLISH -> "Manage your profile, link bank payout accounts, and generate scannable P2P wallet QR requests securely."
          },
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
              title = when (currentLanguage) {
                AppLanguage.ARABIC -> "محفظة QR وطلبات P2P قابلة للمسح"
                AppLanguage.FRENCH -> "QR Code Portefeuille P2P Scannable"
                AppLanguage.ENGLISH -> "Scannable ZXing Wallet QR Requests"
              },
              subtitle = when (currentLanguage) {
                AppLanguage.ARABIC -> "أنشئ وامسح رموز QR الحقيقية لطلبات الدفع بين النظراء دون حجز أموال."
                AppLanguage.FRENCH -> "Générez et scannez de vrais QR codes de paiement P2P."
                AppLanguage.ENGLISH -> "Create and scan real ISO QR payment requests peer-to-peer with non-custodial rails."
              }
            )
            HorizontalDivider(color = BrandSurfaceBorder.copy(alpha = 0.6f))
            SecurityFeatureRow(
              icon = Icons.Default.AccountBalance,
              accent = BrandSecondaryAccent,
              title = when (currentLanguage) {
                AppLanguage.ARABIC -> "ربط الحسابات البنكية والتسجيل للشركاء"
                AppLanguage.FRENCH -> "Comptes Bancaires & Partenaires Multi-Secteurs"
                AppLanguage.ENGLISH -> "Linked Payouts & All-Sector Partners"
              },
              subtitle = when (currentLanguage) {
                AppLanguage.ARABIC -> "اربط حساباتك أو سجّل شركتك في جميع القطاعات التجارية مع عزل سحابي صارم."
                AppLanguage.FRENCH -> "Liez vos IBAN et enregistrez votre entreprise dans tous les secteurs."
                AppLanguage.ENGLISH -> "Link IBANs and register companies across 12 sectors under zero-trust Firestore isolation."
              }
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
              text = when {
                isAutoSigningIn -> when (currentLanguage) {
                  AppLanguage.ARABIC -> "جاري التحقق من الجلسة الآمنة..."
                  AppLanguage.FRENCH -> "Vérification de la session..."
                  AppLanguage.ENGLISH -> "Checking secure session..."
                }
                else -> when (currentLanguage) {
                  AppLanguage.ARABIC -> "جاري الاتصال بحساب Google..."
                  AppLanguage.FRENCH -> "Connexion à Google..."
                  AppLanguage.ENGLISH -> "Signing in with Google..."
                }
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
              text = when (currentLanguage) {
                AppLanguage.ARABIC -> "المتابعة بحساب Google"
                AppLanguage.FRENCH -> "Continuer avec Google"
                AppLanguage.ENGLISH -> "Continue with Google"
              },
              fontWeight = FontWeight.ExtraBold,
              fontSize = 16.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Secondary Registration Actions Row: Partner Portal (All Sectors) + Direct Feedback
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = {
              onRequestPartnerAfterAuth()
              infoMessage = when (currentLanguage) {
                AppLanguage.ARABIC -> "سجّل الدخول بحساب Google لإكمال تسجيل شركتك في أي قطاع تجاري."
                AppLanguage.FRENCH -> "Connectez-vous avec Google pour enregistrer votre entreprise partenaire."
                AppLanguage.ENGLISH -> "Sign in with Google to open All-Sector Partner & Company Registration."
              }
              startGoogleSignIn()
            },
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, BrandSecondaryAccent.copy(alpha = 0.65f)),
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 50.dp)
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
              text = when (currentLanguage) {
                AppLanguage.ARABIC -> "تسجيل الشركاء"
                AppLanguage.FRENCH -> "Partenaires"
                AppLanguage.ENGLISH -> "Partner Signup"
              },
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = BrandSecondaryAccent
            )
          }

          OutlinedButton(
            onClick = { showDirectFeedbackDialog = true },
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, BrandIntelligenceAccent.copy(alpha = 0.65f)),
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 50.dp)
              .testTag("auth_direct_feedback_btn")
          ) {
            Icon(
              imageVector = Icons.Default.RateReview,
              contentDescription = "Direct User Feedback",
              tint = BrandIntelligenceAccent,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = when (currentLanguage) {
                AppLanguage.ARABIC -> if (queuedFeedbackCount > 0) "تعليقاتك ($queuedFeedbackCount)" else "ملاحظات التحسين"
                AppLanguage.FRENCH -> "Avis Directs"
                AppLanguage.ENGLISH -> if (queuedFeedbackCount > 0) "Feedback ($queuedFeedbackCount)" else "Direct Feedback"
              },
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = BrandIntelligenceAccent
            )
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
