package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.custody.CustodyArchitectureModel
import com.example.core.governance.tr
import com.example.core.privacy.PrivacyAndComplianceEngine
import com.example.core.privacy.PrivacyConsentState
import com.example.core.privacy.VerificationApplicantType
import com.example.core.privacy.VerificationWorkflowStatus
import com.example.data.KycKybSubmissionRecord
import com.example.data.PrivacyRequestRecord
import com.example.model.AppLanguage
import com.example.model.PaymentAccountType
import com.example.model.UserOfficeProfile
import com.example.security.AntiFraudEngine
import com.example.security.SmartAuthDialog
import com.example.ui.theme.*
import com.example.viewmodel.ProfileSyncState
import java.security.MessageDigest
import java.util.Locale

@Composable
fun PrivateOfficeScreen(
  profile: UserOfficeProfile,
  currentLanguage: AppLanguage,
  userCountryCode: String = "DZ",
  profileSyncState: ProfileSyncState = ProfileSyncState.Ready(profile),
  isBusy: Boolean = false,
  isUserSignedIn: Boolean,
  privacyConsents: PrivacyConsentState = PrivacyConsentState(),
  myPrivacyRequests: List<PrivacyRequestRecord> = emptyList(),
  latestDataExportJson: String? = null,
  myKycKybSubmissions: List<KycKybSubmissionRecord> = emptyList(),
  onSignInWithGoogle: () -> Unit,
  onSignOut: () -> Unit,
  onUpdateKyc: (String, String, String) -> Unit,
  onSubmitKycKyb: (VerificationApplicantType, String, String, String, String) -> Unit = { _, _, _, _, _ -> },
  onUpdatePrivacyConsents: (PrivacyConsentState) -> Unit = {},
  onGeneratePrivacyExport: () -> Unit = {},
  onRequestAccountDeletion: (String) -> Unit = {},
  onAddBankAccount: (String, String, String, PaymentAccountType, String, Boolean) -> Unit,
  onSetDefaultBankAccount: (String) -> Unit = {},
  onRemoveBankAccount: (String) -> Unit,
  onDepositCollateral: (Double) -> Unit,
  onDepositSpot: (Double) -> Unit = {},
  onWithdrawSpot: (Double, String) -> Unit = { _, _ -> },
  onClaimPartnerCommissions: () -> Unit = {},
  onOpenWalletQr: () -> Unit = {},
  onToggleOnline: (Boolean) -> Unit,
  onOpenCreatePaymentLink: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val isRtl = currentLanguage.isRtl
  var selectedOfficeTab by remember { mutableStateOf(0) }

  var showAddBankDialog by remember { mutableStateOf(false) }
  var showKycDialog by remember { mutableStateOf(false) }
  var showSubmitVerificationDialog by remember { mutableStateOf(false) }
  var showDeleteAccountDialog by remember { mutableStateOf(false) }
  var showTestSmartAuthDialog by remember { mutableStateOf(false) }
  var testAuthDescription by remember { mutableStateOf("") }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
  ) {
    // 1. Account Identity Header
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg),
        border = BorderStroke(1.dp, BrandPrimaryAccent.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(50.dp)
                  .clip(CircleShape)
                  .background(Brush.linearGradient(listOf(BrandPrimaryAccent, BrandSecondaryAccent))),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = profile.fullName.trim().take(2).uppercase().ifBlank { "SP" },
                  fontWeight = FontWeight.ExtraBold,
                  color = BrandPrimaryBg,
                  fontSize = 18.sp
                )
              }

              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = profile.fullName.ifBlank { tr(currentLanguage, "مستخدم Social Pay", "Social Pay User", "Utilisateur") },
                    fontWeight = FontWeight.Bold,
                    color = BrandTextPrimary,
                    style = MaterialTheme.typography.titleMedium
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(Icons.Default.Verified, contentDescription = null, tint = BrandPrimaryAccent, modifier = Modifier.size(16.dp))
                }
                Text(profile.email.ifBlank { "—" }, fontSize = 11.sp, color = BrandTextSecondary)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Surface(color = BrandPrimaryAccent.copy(alpha = 0.14f), shape = RoundedCornerShape(6.dp)) {
                    Text(
                      text = tr(currentLanguage, profile.kycLevel.titleAr, profile.kycLevel.titleEn, profile.kycLevel.titleEn),
                      color = BrandPrimaryAccent,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                  Text("ID #${profile.userId.takeLast(6).ifBlank { "LOCAL" }}", fontSize = 10.sp, color = BrandTextMuted)
                }
              }
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = if (profile.isOnlineMerchant) {
                  tr(currentLanguage, "متاح للاستلام", "Active", "Actif")
                } else {
                  tr(currentLanguage, "غير متصل", "Paused", "En pause")
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (profile.isOnlineMerchant) BrandPrimaryAccent else BrandTextMuted
              )
              Switch(
                checked = profile.isOnlineMerchant,
                onCheckedChange = onToggleOnline,
                modifier = Modifier.testTag("merchant_online_switch")
              )
            }
          }

          HorizontalDivider(color = BrandSurfaceBorder)

          // Honest Account Telemetry (no fake hardcoded averages)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            OfficeStatColumn(
              value = "${profile.trustScorePercent}%",
              label = tr(currentLanguage, "مؤشر الأمان", "Trust Score", "Indice")
            )
            OfficeStatColumn(
              value = "${profile.totalTradesCount}",
              label = tr(currentLanguage, "معاملات موثقة", "Settled", "Réglées")
            )
            OfficeStatColumn(
              value = if (profile.totalTradesCount > 0) "${profile.completionRatePercent}%" else "N/A",
              label = tr(currentLanguage, "نسبة الإنجاز", "Completion", "Taux")
            )
            OfficeStatColumn(
              value = "${profile.linkedBankAccounts.size}",
              label = tr(currentLanguage, "قنوات مربوطة", "Rails", "Canaux")
            )
          }

          if (profileSyncState is ProfileSyncState.Loading || isBusy) {
            LinearProgressIndicator(
              color = BrandPrimaryAccent,
              trackColor = BrandSurfaceVariant,
              modifier = Modifier.fillMaxWidth()
            )
          } else if (profileSyncState is ProfileSyncState.Error) {
            Surface(color = StatusError.copy(alpha = 0.14f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
              Text(profileSyncState.message, color = StatusError, fontSize = 11.sp, modifier = Modifier.padding(10.dp))
            }
          }

          if (!isUserSignedIn) {
            Button(
              onClick = onSignInWithGoogle,
              colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().testTag("google_signin_btn")
            ) {
              Icon(Icons.Default.AccountCircle, contentDescription = null, tint = BrandPrimaryBg)
              Spacer(modifier = Modifier.width(8.dp))
              Text(tr(currentLanguage, "تسجيل الدخول عبر Google", "Sign in with Google", "Connexion Google"), color = BrandPrimaryBg, fontWeight = FontWeight.Bold)
            }
          } else {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = tr(currentLanguage, "✓ مرتبط بمصادقة Firebase الآمنة", "✓ Connected via Firebase Auth", "✓ Connecté via Firebase Auth"),
                fontSize = 11.sp,
                color = BrandPrimaryAccent,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
              )
              TextButton(onClick = onSignOut, modifier = Modifier.testTag("office_sign_out_btn")) {
                Text(tr(currentLanguage, "تسجيل الخروج", "Sign Out", "Déconnexion"), color = StatusError, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    // 2. Non-Custodial Transparency Card
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurfaceVariant),
        border = BorderStroke(1.dp, BrandSecondaryAccent.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Policy, contentDescription = null, tint = BrandSecondaryAccent, modifier = Modifier.size(20.dp))
            Text(
              text = tr(currentLanguage, "بنية الحفظ غير الوصائية (Non-Custodial)", "Non-Custodial Routing Architecture", "Architecture Non-Dépositaire"),
              fontWeight = FontWeight.Bold,
              color = BrandTextPrimary,
              fontSize = 13.sp
            )
          }
          Text(
            text = CustodyArchitectureModel.policyBanner(currentLanguage),
            fontSize = 11.sp,
            color = BrandTextSecondary
          )
          OutlinedButton(
            onClick = onOpenWalletQr,
            border = BorderStroke(1.dp, BrandPrimaryAccent.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("office_wallet_qr_btn")
          ) {
            Icon(Icons.Default.QrCode2, contentDescription = null, tint = BrandPrimaryAccent, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = tr(currentLanguage, "رمز QR للاستلام الخارجي", "External Wallet Receive QR", "QR de réception externe"),
              color = BrandPrimaryAccent,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    // 3. Tabs
    item {
      val tabs = listOf(
        tr(currentLanguage, "البنية والروابط", "Architecture", "Architecture"),
        tr(currentLanguage, "حسابات الدفع", "Payout Rails", "Comptes"),
        tr(currentLanguage, "التوثيق KYC/KYB", "KYC / KYB", "Vérification"),
        tr(currentLanguage, "مركز الخصوصية", "Privacy", "Confidentialité"),
        tr(currentLanguage, "الأمان والشراكة", "Security", "Sécurité")
      )
      Surface(color = BrandSecondaryBg, shape = RoundedCornerShape(12.dp)) {
        ScrollableTabRow(
          selectedTabIndex = selectedOfficeTab,
          edgePadding = 6.dp,
          containerColor = BrandSecondaryBg,
          contentColor = BrandPrimaryAccent
        ) {
          tabs.forEachIndexed { idx, label ->
            Tab(
              selected = selectedOfficeTab == idx,
              onClick = { selectedOfficeTab = idx },
              text = {
                Text(
                  text = label,
                  fontSize = 11.sp,
                  fontWeight = if (selectedOfficeTab == idx) FontWeight.Bold else FontWeight.Normal
                )
              }
            )
          }
        }
      }
    }

    when (selectedOfficeTab) {
      0 -> {
        // Non-Custodial vs Regulated Custody Profiles + Payment Link Launch
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = tr(currentLanguage, "مقارنة نماذج الحفظ التنظيمية", "Custody Governance Models", "Modèles de Conservation"),
              fontWeight = FontWeight.Bold,
              color = BrandTextPrimary,
              style = MaterialTheme.typography.titleSmall
            )
            Button(
              onClick = onOpenCreatePaymentLink,
              colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Icon(Icons.Default.AddLink, contentDescription = null, tint = BrandPrimaryBg, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(tr(currentLanguage, "رابط دفع", "New Link", "Nouveau lien"), color = BrandPrimaryBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        items(CustodyArchitectureModel.COMPARISON_PROFILES) { item ->
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg),
            border = BorderStroke(
              1.dp,
              if (item.isCurrentlyActive) BrandPrimaryAccent.copy(alpha = 0.6f) else BrandSurfaceBorder
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = tr(currentLanguage, item.titleAr, item.titleEn, item.titleFr),
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = BrandTextPrimary,
                  modifier = Modifier.weight(1f)
                )
                Surface(
                  color = if (item.isCurrentlyActive) BrandPrimaryAccent.copy(alpha = 0.16f) else StatusWarning.copy(alpha = 0.16f),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text(
                    text = if (item.isCurrentlyActive) tr(currentLanguage, "مفعل", "ACTIVE", "ACTIF")
                    else tr(currentLanguage, "يتطلب ترخيصاً", "LICENSE REQ", "LICENCE REQ"),
                    color = if (item.isCurrentlyActive) BrandPrimaryAccent else StatusWarning,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
              Text(tr(currentLanguage, item.keyControlAr, item.keyControlEn, item.keyControlFr), fontSize = 11.sp, color = BrandTextSecondary)
              Text(tr(currentLanguage, item.regulatoryProfileAr, item.regulatoryProfileEn, item.regulatoryProfileFr), fontSize = 10.sp, color = BrandTextMuted)
              item.technicalControls.forEach { ctrl ->
                Text("• $ctrl", fontSize = 10.sp, color = BrandPrimaryAccent)
              }
            }
          }
        }
      }

      1 -> {
        // External Payout Rails (Non-custodial bank IBANs & Wallets)
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = tr(currentLanguage, "قنوات الاستلام الخارجية المرتبطة", "Linked External Payout Rails", "Canaux de paiement liés"),
                fontWeight = FontWeight.Bold,
                color = BrandTextPrimary,
                style = MaterialTheme.typography.titleSmall
              )
              Text(
                text = tr(currentLanguage, "لا يتم تخزين مفاتيح خاصة أو أرقام بطاقات سرية", "Zero private keys or CVV storage", "Zéro clé privée ni CVV stocké"),
                fontSize = 10.sp,
                color = BrandTextSecondary
              )
            }
            Button(
              onClick = { showAddBankDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("add_bank_btn")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, tint = BrandPrimaryBg, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(tr(currentLanguage, "ربط قناة", "Add Rail", "Ajouter"), color = BrandPrimaryBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        if (profile.linkedBankAccounts.isEmpty()) {
          item {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg),
              border = BorderStroke(1.dp, BrandSurfaceBorder),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = BrandSecondaryAccent, modifier = Modifier.size(34.dp))
                Text(tr(currentLanguage, "لا توجد قنوات دفع مربوطة بعد", "No External Payout Rails Linked", "Aucun canal lié"), fontWeight = FontWeight.Bold, color = BrandTextPrimary)
                OutlinedButton(onClick = { showAddBankDialog = true }, border = BorderStroke(1.dp, BrandPrimaryAccent)) {
                  Text(tr(currentLanguage, "ربط IBAN أو محفظة خارجية", "Link External IBAN or Wallet", "Lier un compte externe"), color = BrandPrimaryAccent)
                }
              }
            }
          }
        } else {
          items(profile.linkedBankAccounts, key = { it.id }) { bank ->
            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg),
              border = BorderStroke(1.dp, if (bank.isDefault) BrandPrimaryAccent.copy(alpha = 0.65f) else BrandSurfaceBorder),
              modifier = Modifier.fillMaxWidth().testTag("linked_account_card_${bank.id}")
            ) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      Text(bank.bankName, fontWeight = FontWeight.Bold, color = BrandTextPrimary)
                      Surface(color = BrandSecondaryAccent.copy(alpha = 0.16f), shape = RoundedCornerShape(4.dp)) {
                        Text(bank.currency, color = BrandSecondaryAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                      }
                      if (bank.isDefault) {
                        Surface(color = BrandPrimaryAccent.copy(alpha = 0.16f), shape = RoundedCornerShape(4.dp)) {
                          Text(tr(currentLanguage, "أساسي", "Primary", "Principal"), color = BrandPrimaryAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                        }
                      }
                    }
                    Text(bank.accountNumberOrIban, fontSize = 12.sp, color = BrandTextSecondary)
                    Text("${bank.accountHolderName} • ${if (isRtl) bank.accountType.titleAr else bank.accountType.titleEn}", fontSize = 10.sp, color = BrandPrimaryAccent)
                  }
                  IconButton(onClick = { onRemoveBankAccount(bank.id) }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Unlink", tint = StatusError)
                  }
                }
                if (!bank.isDefault) {
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onSetDefaultBankAccount(bank.id) }) {
                      Text(tr(currentLanguage, "تعيين كأساسي", "Set Primary", "Définir principal"), color = BrandSecondaryAccent, fontSize = 11.sp)
                    }
                  }
                }
              }
            }
          }
        }
      }

      2 -> {
        // KYC / KYB Identity & Document Verification (Zero-Permission Photo Picker)
        item {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg),
            border = BorderStroke(1.dp, BrandPrimaryAccent.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = BrandPrimaryAccent)
                Text(
                  text = tr(currentLanguage, "التحقق التدريجي للهوية والشركات (KYC / KYB)", "Individual KYC & Business KYB", "Vérification KYC & KYB"),
                  fontWeight = FontWeight.Bold,
                  color = BrandTextPrimary,
                  style = MaterialTheme.typography.titleSmall
                )
              }
              Text(
                text = "${tr(currentLanguage, "الاسم القانوني:", "Legal Name:", "Nom légal :")} ${profile.fullName.ifBlank { "—" }}",
                fontSize = 12.sp,
                color = BrandTextPrimary
              )
              Text(
                text = "${tr(currentLanguage, "المستوى الحالي:", "Current Tier:", "Niveau actuel :")} ${tr(currentLanguage, profile.kycLevel.titleAr, profile.kycLevel.titleEn, profile.kycLevel.titleEn)}",
                fontSize = 12.sp,
                color = BrandPrimaryAccent,
                fontWeight = FontWeight.Bold
              )
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                  onClick = { showKycDialog = true },
                  modifier = Modifier.weight(1f).testTag("update_kyc_btn")
                ) {
                  Text(tr(currentLanguage, "تحديث البيانات", "Edit Profile", "Modifier"), fontSize = 11.sp)
                }
                Button(
                  onClick = { showSubmitVerificationDialog = true },
                  colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent),
                  modifier = Modifier.weight(1f).testTag("submit_kyc_kyb_btn")
                ) {
                  Text(tr(currentLanguage, "رفع ملف توثيق", "Submit KYC/KYB", "Soumettre KYC/KYB"), color = BrandPrimaryBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }

        items(myKycKybSubmissions, key = { it.id }) { sub ->
          val statusEnum = enumValues<VerificationWorkflowStatus>().firstOrNull { it.name == sub.status }
            ?: VerificationWorkflowStatus.SUBMITTED
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = BrandSurfaceVariant),
            border = BorderStroke(1.dp, BrandSurfaceBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(sub.entityOrFullName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandTextPrimary)
                Surface(color = BrandPrimaryAccent.copy(alpha = 0.14f), shape = RoundedCornerShape(6.dp)) {
                  Text(
                    text = tr(currentLanguage, statusEnum.titleAr, statusEnum.titleEn, statusEnum.titleFr),
                    color = BrandPrimaryAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
              Text("${sub.applicantType} • ${sub.countryCode} • #${sub.registryOrIdNumber}", fontSize = 11.sp, color = BrandTextSecondary)
              if (sub.documentReferenceHash.isNotBlank()) {
                Text("Doc SHA-256: ${sub.documentReferenceHash.take(20)}...", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = BrandTextMuted)
              }
              if (sub.reviewerNote.isNotBlank()) {
                Text(sub.reviewerNote, fontSize = 11.sp, color = StatusWarning)
              }
            }
          }
        }
      }

      3 -> {
        // Privacy Center: Strict Consents, Data Sale Prohibition, Portable JSON Export & Account Deletion
        item {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg),
            border = BorderStroke(1.dp, BrandIntelligenceAccent.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = BrandIntelligenceAccent)
                Text(
                  text = tr(currentLanguage, "مركز الخصوصية والتحكم بالبيانات", "Privacy & Consent Center", "Centre de Confidentialité"),
                  fontWeight = FontWeight.Bold,
                  color = BrandTextPrimary,
                  style = MaterialTheme.typography.titleSmall
                )
              }

              Text(
                text = PrivacyAndComplianceEngine.privacyPolicySummary(currentLanguage),
                fontSize = 11.sp,
                color = BrandTextSecondary
              )

              HorizontalDivider(color = BrandSurfaceBorder)

              // Opt-in toggles
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = tr(currentLanguage, "ذاكرة المساعد الذكي (اختياري)", "AI Context Memory (Opt-in)", "Mémoire IA (Optionnel)"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandTextPrimary
                  )
                  Text(
                    text = tr(currentLanguage, "لا يقرأ الرسائل الخاصة أو جهات الاتصال", "Never accesses private chats or contacts", "Jamais de lecture de messages privés"),
                    fontSize = 10.sp,
                    color = BrandTextMuted
                  )
                }
                Switch(
                  checked = privacyConsents.aiAssistantContextMemory,
                  onCheckedChange = {
                    onUpdatePrivacyConsents(privacyConsents.copy(aiAssistantContextMemory = it))
                  },
                  modifier = Modifier.testTag("privacy_ai_memory_switch")
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = tr(currentLanguage, "مشاركة بيانات الشراكة عند الطلب", "External Partner Sharing (Opt-in)", "Partage partenaire externe"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandTextPrimary
                  )
                }
                Switch(
                  checked = privacyConsents.externalPartnerSharing,
                  onCheckedChange = {
                    onUpdatePrivacyConsents(privacyConsents.copy(externalPartnerSharing = it))
                  },
                  modifier = Modifier.testTag("privacy_partner_sharing_switch")
                )
              }

              // Data Sale Opt-Out Locked Badge
              Surface(color = BrandPrimaryAccent.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Icon(Icons.Default.Lock, contentDescription = null, tint = BrandPrimaryAccent, modifier = Modifier.size(16.dp))
                  Text(
                    text = tr(currentLanguage, "بيع البيانات لطرف ثالث محظور ومغلق نهائياً", "Data Sale Opt-Out: LOCKED PROHIBITED", "Vente de données : INTERDITE"),
                    fontSize = 11.sp,
                    color = BrandPrimaryAccent,
                    fontWeight = FontWeight.Bold
                  )
                }
              }

              Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                  onClick = onGeneratePrivacyExport,
                  colors = ButtonDefaults.buttonColors(containerColor = BrandSecondaryAccent),
                  modifier = Modifier.weight(1f).testTag("privacy_export_json_btn")
                ) {
                  Icon(Icons.Default.Download, contentDescription = null, tint = BrandPrimaryBg, modifier = Modifier.size(15.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(tr(currentLanguage, "تصدير JSON", "Export JSON", "Exporter JSON"), color = BrandPrimaryBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                  onClick = { showDeleteAccountDialog = true },
                  border = BorderStroke(1.dp, StatusError.copy(alpha = 0.7f)),
                  modifier = Modifier.weight(1f).testTag("privacy_delete_account_btn")
                ) {
                  Icon(Icons.Default.DeleteForever, contentDescription = null, tint = StatusError, modifier = Modifier.size(15.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(tr(currentLanguage, "طلب حذف الحساب", "Delete Account", "Supprimer compte"), color = StatusError, fontSize = 11.sp)
                }
              }

              latestDataExportJson?.let { json ->
                Surface(color = BrandPrimaryBg, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, BrandSurfaceBorder)) {
                  Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                      Text("Portable JSON Export", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandPrimaryAccent)
                      TextButton(onClick = {
                        clipboardManager.setText(AnnotatedString(json))
                        Toast.makeText(context, "JSON copied", Toast.LENGTH_SHORT).show()
                      }) {
                        Text(tr(currentLanguage, "نسخ", "Copy", "Copier"), fontSize = 11.sp)
                      }
                    }
                    Text(json.take(700), fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = BrandTextSecondary)
                  }
                }
              }
            }
          }
        }

        items(myPrivacyRequests, key = { it.id }) { req ->
          Surface(color = BrandSurfaceVariant, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text("${req.requestType} • ${req.status}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandPrimaryAccent)
              Text(req.details, fontSize = 11.sp, color = BrandTextSecondary)
            }
          }
        }
      }

      4 -> {
        // Security Center & Honest Partner Earnings
        item {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg),
            border = BorderStroke(1.dp, BrandPrimaryAccent.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Security, contentDescription = null, tint = BrandPrimaryAccent)
                Text(
                  text = tr(currentLanguage, "مركز المصادقة الذكية والحماية", "Multi-Modal Auth & Anti-Phishing", "Authentification & Sécurité"),
                  fontWeight = FontWeight.Bold,
                  color = BrandTextPrimary,
                  style = MaterialTheme.typography.titleSmall
                )
              }

              SecurityMethodRow(
                title = tr(currentLanguage, "البصمة الحيوية (Biometric)", "Biometric / Hardware Keystore", "Biométrie"),
                icon = Icons.Default.Fingerprint,
                onTest = {
                  testAuthDescription = tr(currentLanguage, "اختبار المصادقة الحيوية", "Biometric Verification Test", "Test biométrique")
                  showTestSmartAuthDialog = true
                },
                testLabel = tr(currentLanguage, "اختبار", "Test", "Tester")
              )

              SecurityMethodRow(
                title = tr(currentLanguage, "لوحة PIN المشفرة ضد التلصص", "Scrambled Anti-Spy PIN Keypad", "Clavier PIN brouillé"),
                icon = Icons.Default.Dialpad,
                onTest = {
                  testAuthDescription = tr(currentLanguage, "اختبار لوحة PIN العشوائية", "Scrambled PIN Keypad Test", "Test clavier PIN")
                  showTestSmartAuthDialog = true
                },
                testLabel = tr(currentLanguage, "اختبار", "Test", "Tester")
              )

              Surface(
                color = BrandSecondaryAccent.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, BrandSecondaryAccent.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text(tr(currentLanguage, "ختم مكافحة التصيد الرسمي:", "Anti-Phishing Security Seal:", "Sceau anti-hameçonnage :"), fontSize = 10.sp, color = BrandTextSecondary)
                    Text(AntiFraudEngine.ANTI_PHISHING_CODE, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = BrandSecondaryAccent)
                  }
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandSecondaryAccent)
                }
              }
            }
          }
        }

        // Honest Partner Commissions Card (Zero hardcoded trade counts)
        item {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg),
            border = BorderStroke(1.dp, BrandSecondaryAccent.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = tr(currentLanguage, "عمولات الشراكة المسجلة", "Recorded Partner Commissions", "Commissions partenaire enregistrées"),
                fontWeight = FontWeight.Bold,
                color = BrandTextPrimary,
                style = MaterialTheme.typography.titleSmall
              )
              Text(
                text = "$${String.format(Locale.US, "%,.2f", profile.partnerCommissionsEarnedUsd)} USD",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BrandPrimaryAccent
              )
              Text(
                text = tr(
                  currentLanguage,
                  "تُحتسب فقط من التسويات الموثقة عبر روابط الشركاء المعتمدين ($0.40 لكل معاملة مكتملة).",
                  "Computed strictly from settled verified partner referrals ($0.40 per settled referral).",
                  "Calculé uniquement sur les règlements vérifiés ($0,40 par règlement)."
                ),
                fontSize = 11.sp,
                color = BrandTextSecondary
              )
              Button(
                onClick = onClaimPartnerCommissions,
                enabled = !isBusy && profile.partnerCommissionsEarnedUsd > 0.0,
                colors = ButtonDefaults.buttonColors(containerColor = BrandSecondaryAccent),
                modifier = Modifier.fillMaxWidth().testTag("claim_commissions_btn")
              ) {
                Text(
                  text = if (profile.partnerCommissionsEarnedUsd > 0.0) {
                    tr(currentLanguage, "تحويل العمولات", "Claim Commissions", "Réclamer")
                  } else {
                    tr(currentLanguage, "لا توجد عمولات مستحقة حالياً", "No Commissions Accrued Yet", "Aucune commission")
                  },
                  color = BrandPrimaryBg,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }
    }
  }

  // Dialog: Link External Payout Account
  if (showAddBankDialog) {
    var bankNameInput by remember { mutableStateOf("") }
    var ibanInput by remember { mutableStateOf("") }
    var holderNameInput by remember { mutableStateOf(profile.fullName) }
    var selectedType by remember { mutableStateOf(PaymentAccountType.BANK_IBAN) }
    var selectedCurrency by remember { mutableStateOf("USD") }
    var setAsDefault by remember { mutableStateOf(profile.linkedBankAccounts.isEmpty()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { showAddBankDialog = false },
      title = { Text(tr(currentLanguage, "ربط قناة دفع خارجية", "Link External Payout Rail", "Lier un canal externe")) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PaymentAccountType.entries.forEach { type ->
              FilterChip(
                selected = selectedType == type,
                onClick = { selectedType = type },
                label = { Text(if (isRtl) type.titleAr else type.titleEn, fontSize = 10.sp) }
              )
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("USD", "EUR", "DZD", "SAR").forEach { curr ->
              FilterChip(
                selected = selectedCurrency == curr,
                onClick = { selectedCurrency = curr },
                label = { Text(curr, fontSize = 10.sp) }
              )
            }
          }
          OutlinedTextField(
            value = bankNameInput,
            onValueChange = { bankNameInput = it; validationError = null },
            label = { Text(tr(currentLanguage, "اسم البنك أو المحفظة", "Bank or Wallet Name", "Banque ou portefeuille")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("link_bank_name_input")
          )
          OutlinedTextField(
            value = ibanInput,
            onValueChange = { ibanInput = it; validationError = null },
            label = { Text(tr(currentLanguage, "IBAN أو عنوان المحفظة", "IBAN / External Address", "IBAN / Adresse")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("link_iban_input")
          )
          OutlinedTextField(
            value = holderNameInput,
            onValueChange = { holderNameInput = it; validationError = null },
            label = { Text(tr(currentLanguage, "اسم المستفيد", "Beneficiary Name", "Nom du bénéficiaire")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("link_holder_input")
          )
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { setAsDefault = !setAsDefault }) {
            Checkbox(checked = setAsDefault, onCheckedChange = { setAsDefault = it })
            Text(tr(currentLanguage, "تعيين كأساسي", "Set as primary rail", "Définir principal"), fontSize = 12.sp)
          }
          validationError?.let { Text(it, color = StatusError, fontSize = 11.sp) }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (bankNameInput.trim().length < 2 || ibanInput.trim().length < 4 || holderNameInput.trim().length < 2) {
              validationError = tr(currentLanguage, "يرجى إكمال الحقول المطلوبة.", "Please complete all required fields.", "Veuillez remplir tous les champs.")
            } else {
              onAddBankAccount(bankNameInput.trim(), ibanInput.trim(), holderNameInput.trim(), selectedType, selectedCurrency, setAsDefault)
              showAddBankDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent),
          modifier = Modifier.testTag("save_linked_account_btn")
        ) {
          Text(tr(currentLanguage, "حفظ", "Save", "Enregistrer"), color = BrandPrimaryBg, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddBankDialog = false }) { Text(tr(currentLanguage, "إلغاء", "Cancel", "Annuler")) }
      }
    )
  }

  // Dialog: Edit Basic Profile KYC
  if (showKycDialog) {
    var nameInput by remember { mutableStateOf(profile.fullName) }
    var phoneInput by remember { mutableStateOf(profile.phoneNumber) }
    var idInput by remember { mutableStateOf(profile.nationalIdOrPassport) }
    AlertDialog(
      onDismissRequest = { showKycDialog = false },
      title = { Text(tr(currentLanguage, "تحديث بيانات الملف الشخصي", "Update Profile Identity", "Mettre à jour le profil")) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text(tr(currentLanguage, "الاسم الكامل", "Full Name", "Nom complet")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = phoneInput, onValueChange = { phoneInput = it }, label = { Text(tr(currentLanguage, "رقم الهاتف", "Phone Number", "Téléphone")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = idInput, onValueChange = { idInput = it }, label = { Text(tr(currentLanguage, "رقم الهوية / الجواز", "National ID / Passport", "ID / Passeport")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onUpdateKyc(nameInput, phoneInput, idInput)
            showKycDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent)
        ) {
          Text(tr(currentLanguage, "حفظ", "Save", "Enregistrer"), color = BrandPrimaryBg, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showKycDialog = false }) { Text(tr(currentLanguage, "إلغاء", "Cancel", "Annuler")) }
      }
    )
  }

  // Dialog: Submit KYC / KYB with Zero-Permission Photo Picker (PickVisualMedia)
  if (showSubmitVerificationDialog) {
    var applicantType by remember { mutableStateOf(VerificationApplicantType.INDIVIDUAL_KYC) }
    var entityName by remember { mutableStateOf(profile.fullName) }
    var countryInput by remember { mutableStateOf(userCountryCode) }
    var docNumberInput by remember { mutableStateOf(profile.nationalIdOrPassport) }
    var pickedDocHash by remember { mutableStateOf("") }
    var pickedLabel by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
      if (uri != null) {
        val bytes = try {
          context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (_: Exception) {
          null
        }
        val hashSource = bytes ?: uri.toString().toByteArray()
        pickedDocHash = MessageDigest.getInstance("SHA-256").digest(hashSource).joinToString("") { "%02x".format(it) }
        pickedLabel = uri.lastPathSegment ?: "document_selected"
      }
    }

    AlertDialog(
      onDismissRequest = { showSubmitVerificationDialog = false },
      title = { Text(tr(currentLanguage, "تقديم طلب توثيق KYC / KYB", "Submit KYC / KYB File", "Soumettre dossier KYC / KYB")) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            VerificationApplicantType.entries.forEach { t ->
              FilterChip(
                selected = applicantType == t,
                onClick = { applicantType = t },
                label = {
                  Text(
                    text = if (t == VerificationApplicantType.INDIVIDUAL_KYC) tr(currentLanguage, "فرد KYC", "Individual KYC", "Particulier")
                    else tr(currentLanguage, "شركة KYB", "Business KYB", "Entreprise"),
                    fontSize = 11.sp
                  )
                }
              )
            }
          }
          OutlinedTextField(
            value = entityName,
            onValueChange = { entityName = it },
            label = { Text(tr(currentLanguage, "الاسم الكامل أو اسم الشركة", "Full Name or Company Name", "Nom / Raison sociale")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = countryInput,
            onValueChange = { countryInput = it.uppercase() },
            label = { Text(tr(currentLanguage, "رمز الدولة (مثال: DZ, SA, FR)", "Country Code (DZ, SA, FR)", "Code Pays")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = docNumberInput,
            onValueChange = { docNumberInput = it },
            label = { Text(tr(currentLanguage, "رقم الهوية أو السجل التجاري", "ID or Commercial Registry #", "N° ID ou Registre")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedButton(
            onClick = {
              photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            modifier = Modifier.fillMaxWidth().testTag("pick_kyc_document_btn")
          ) {
            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (pickedDocHash.isBlank()) tr(currentLanguage, "اختيار صورة المستند (Photo Picker)", "Select Document Image", "Choisir image document")
              else "✓ ${pickedLabel.take(18)} (${pickedDocHash.take(10)}...)",
              fontSize = 11.sp
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (entityName.isNotBlank() && docNumberInput.isNotBlank()) {
              onSubmitKycKyb(
                applicantType,
                entityName.trim(),
                countryInput.trim().ifBlank { "DZ" },
                docNumberInput.trim(),
                pickedDocHash.ifBlank { "SHA256_PENDING_PICK" }
              )
              showSubmitVerificationDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent)
        ) {
          Text(tr(currentLanguage, "إرسال للمراجعة", "Submit", "Soumettre"), color = BrandPrimaryBg, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showSubmitVerificationDialog = false }) { Text(tr(currentLanguage, "إلغاء", "Cancel", "Annuler")) }
      }
    )
  }

  // Dialog: Account Deletion Request
  if (showDeleteAccountDialog) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showDeleteAccountDialog = false },
      title = { Text(tr(currentLanguage, "طلب حذف الحساب والبيانات", "Request Account Deletion", "Demander la suppression")) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = tr(
              currentLanguage,
              "تُحذف بياناتك الشخصية والقنوات المربوطة، مع الاحتفاظ فقط بسجلات الدفتر الملزمة قانونياً.",
              "Personal profile and linked rails are scheduled for removal; legally mandated audit/ledger entries remain archived.",
              "Votre profil sera supprimé ; seules les écritures comptables légales sont archivées."
            ),
            fontSize = 11.sp
          )
          OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text(tr(currentLanguage, "السبب (اختياري)", "Reason (optional)", "Motif (optionnel)")) },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onRequestAccountDeletion(reason)
            showDeleteAccountDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = StatusError)
        ) {
          Text(tr(currentLanguage, "تأكيد الطلب", "Submit Request", "Confirmer"), color = BrandTextPrimary, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteAccountDialog = false }) { Text(tr(currentLanguage, "إلغاء", "Cancel", "Annuler")) }
      }
    )
  }

  if (showTestSmartAuthDialog) {
    SmartAuthDialog(
      title = tr(currentLanguage, "اختبار المصادقة الذكية", "Test Smart Authentication", "Test Authentification"),
      subtitle = testAuthDescription,
      isRtl = isRtl,
      onAuthSuccess = {
        showTestSmartAuthDialog = false
        Toast.makeText(context, "✓ Verified", Toast.LENGTH_SHORT).show()
      },
      onDismiss = { showTestSmartAuthDialog = false }
    )
  }
}

@Composable
private fun OfficeStatColumn(value: String, label: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BrandPrimaryAccent)
    Text(label, fontSize = 10.sp, color = BrandTextSecondary)
  }
}

@Composable
private fun SecurityMethodRow(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onTest: () -> Unit,
  testLabel: String
) {
  Surface(
    color = BrandSurfaceVariant,
    shape = RoundedCornerShape(10.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, tint = BrandPrimaryAccent, modifier = Modifier.size(22.dp))
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrandTextPrimary)
      }
      Button(
        onClick = onTest,
        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
      ) {
        Text(testLabel, color = BrandPrimaryBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}
