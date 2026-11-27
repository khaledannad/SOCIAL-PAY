package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.governance.tr
import com.example.data.*
import com.example.model.AppLanguage
import com.example.model.FiatCurrency
import com.example.ui.components.QrCodeCard
import com.example.ui.theme.*

@Composable
fun PartnerAndSettingsScreen(
  currentLanguage: AppLanguage,
  selectedFiat: String,
  userCountryCode: String = "DZ",
  userTimezone: String = "Africa/Algiers",
  supportedFiat: List<FiatCurrency>,
  defaultContactName: String = "",
  defaultContactEmail: String = "",
  myPartners: List<PartnerApplicationItem> = emptyList(),
  platformSettings: OwnerPlatformSettings = OwnerPlatformSettings(),
  isBusy: Boolean = false,
  onLanguageSelected: (AppLanguage) -> Unit,
  onCountryTimezoneSelected: (countryCode: String, timezoneId: String) -> Unit = { _, _ -> },
  onFiatSelected: (String) -> Unit,
  onSubmitPartnerApplication: (
    companyName: String,
    sector: PartnerSector,
    platformType: PartnerPlatformType,
    websiteOrChannel: String,
    contactName: String,
    contactEmail: String,
    contactPhone: String,
    countryCode: String,
    monthlyVolumeEstimateUsd: Double,
    notes: String
  ) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> },
  onOpenDirectFeedback: () -> Unit = {},
  onOpenWalletQr: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val isRtl = currentLanguage.isRtl

  var companyNameInput by remember { mutableStateOf("") }
  var selectedSector by remember { mutableStateOf(PartnerSector.ECOMMERCE_RETAIL) }
  var selectedPlatformType by remember { mutableStateOf(PartnerPlatformType.WEB_SDK) }
  var websiteOrChannelInput by remember { mutableStateOf("") }
  var contactNameInput by remember(defaultContactName) { mutableStateOf(defaultContactName) }
  var contactEmailInput by remember(defaultContactEmail) { mutableStateOf(defaultContactEmail) }
  var contactPhoneInput by remember { mutableStateOf("") }
  var countryCodeInput by remember(userCountryCode) { mutableStateOf(userCountryCode) }
  var monthlyVolumeInput by remember { mutableStateOf("25000") }
  var businessNotesInput by remember { mutableStateOf("") }
  var validationError by remember { mutableStateOf<String?>(null) }

  var partnerChannelName by remember { mutableStateOf("SocialPay_Partner") }
  var partnerPlatform by remember { mutableStateOf("Telegram WebApp") }

  val generatedEmbedLink = remember(partnerChannelName, partnerPlatform, companyNameInput) {
    val clean = (companyNameInput.takeIf { it.isNotBlank() } ?: partnerChannelName)
      .trim()
      .replace(" ", "_")
    when (partnerPlatform) {
      "Telegram WebApp" -> "https://t.me/P2PSocialBot/app?partner=$clean"
      "WhatsApp Business" -> "https://wa.me/p2p_smart_button?ref=$clean"
      "Instagram Bio" -> "https://p2p.social/go/$clean"
      else -> "https://socialpay.app/partner?id=$clean"
    }
  }

  val countryPresets = listOf(
    Triple("DZ", "Algeria (الجزائر)", "Africa/Algiers"),
    Triple("SA", "Saudi Arabia", "Asia/Riyadh"),
    Triple("AE", "UAE", "Asia/Dubai"),
    Triple("FR", "France", "Europe/Paris"),
    Triple("US", "United States", "America/New_York"),
    Triple("GB", "United Kingdom", "Europe/London"),
    Triple("EG", "Egypt", "Africa/Cairo")
  )

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
  ) {
    // 1. Partner Program Banner
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, BrandPrimaryAccent.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Box(
                modifier = Modifier
                  .size(44.dp)
                  .clip(CircleShape)
                  .background(BrandPrimaryAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Handshake,
                  contentDescription = null,
                  tint = BrandPrimaryAccent,
                  modifier = Modifier.size(26.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = tr(currentLanguage, "تسجيل الشركاء والشركات (جميع القطاعات)", "All-Sector Partner & Company Program", "Programme Partenaires Multi-Secteurs"),
                  fontWeight = FontWeight.Bold,
                  style = MaterialTheme.typography.titleMedium
                )
                Text(
                  text = tr(
                    currentLanguage,
                    "عمولة ${platformSettings.partnerCommissionPercent.toInt()}% • 12 قطاعاً تجارياً",
                    "${platformSettings.partnerCommissionPercent.toInt()}% Revenue Share • 12 Sectors",
                    "${platformSettings.partnerCommissionPercent.toInt()}% de partage • 12 secteurs"
                  ),
                  style = MaterialTheme.typography.labelSmall,
                  color = BrandPrimaryAccent,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
            Surface(color = BrandSecondaryAccent.copy(alpha = 0.18f), shape = RoundedCornerShape(8.dp)) {
              Text(
                text = "${platformSettings.partnerCommissionPercent.toInt()}%",
                color = BrandSecondaryAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = tr(
              currentLanguage,
              "اربط شركتك أو متجرك بعملات مستقرة معتمدة (USDC / USDT / EURC) عبر روابط QR وبوابات API غير حافظة.",
              "Onboard any business across 12 sectors using non-custodial Stablecoin QR & API settlement rails.",
              "Intégrez votre entreprise dans 12 secteurs via des paiements Stablecoins non dépositaires."
            ),
            style = MaterialTheme.typography.bodySmall,
            color = BrandTextSecondary
          )
        }
      }
    }

    // 2. All-Sector Partner Registration Form (Firestore)
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BrandPrimaryAccent.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Business, contentDescription = null, tint = BrandPrimaryAccent)
            Text(
              text = tr(currentLanguage, "استمارة تسجيل شركة أو شريك جديد", "Register Partner or Company", "Inscription Partenaire / Entreprise"),
              fontWeight = FontWeight.ExtraBold,
              style = MaterialTheme.typography.titleSmall
            )
          }

          if (!platformSettings.allowPartnerRegistrations) {
            Surface(color = StatusWarning.copy(alpha = 0.14f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
              Text(
                text = tr(currentLanguage, "تسجيل الشركاء متوقف مؤقتاً.", "New partner registrations are paused.", "Inscriptions temporairement suspendues."),
                color = StatusWarning,
                fontSize = 12.sp,
                modifier = Modifier.padding(10.dp)
              )
            }
          }

          OutlinedTextField(
            value = companyNameInput,
            onValueChange = { companyNameInput = it; validationError = null },
            label = { Text(tr(currentLanguage, "اسم الشركة / العلامة التجارية *", "Company / Brand Name *", "Nom de l'entreprise *")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("partner_company_input")
          )

          Text(
            text = tr(currentLanguage, "اختر القطاع (12 قطاعاً):", "Business Sector (12 Sectors):", "Secteur d'activité (12) :"),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(PartnerSector.entries) { sector ->
              FilterChip(
                selected = selectedSector == sector,
                onClick = { selectedSector = sector },
                label = { Text(tr(currentLanguage, sector.titleAr, sector.titleEn, sector.titleEn), fontSize = 11.sp) }
              )
            }
          }

          Text(
            text = tr(currentLanguage, "قناة الربط التقني:", "Integration Channel:", "Canal d'intégration :"),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(PartnerPlatformType.entries) { platform ->
              FilterChip(
                selected = selectedPlatformType == platform,
                onClick = { selectedPlatformType = platform },
                label = { Text(tr(currentLanguage, platform.titleAr, platform.titleEn, platform.titleEn), fontSize = 11.sp) }
              )
            }
          }

          OutlinedTextField(
            value = websiteOrChannelInput,
            onValueChange = { websiteOrChannelInput = it; validationError = null },
            label = { Text(tr(currentLanguage, "الموقع الإلكتروني أو القناة *", "Website URL or Channel *", "Site web ou canal *")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("partner_website_input")
          )

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
              value = contactNameInput,
              onValueChange = { contactNameInput = it; validationError = null },
              label = { Text(tr(currentLanguage, "اسم المسؤول *", "Contact Name *", "Responsable *")) },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = countryCodeInput,
              onValueChange = { countryCodeInput = it.uppercase().take(4) },
              label = { Text(tr(currentLanguage, "الدولة", "Country", "Pays")) },
              singleLine = true,
              modifier = Modifier.weight(0.55f)
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
              value = contactEmailInput,
              onValueChange = { contactEmailInput = it; validationError = null },
              label = { Text(tr(currentLanguage, "البريد الرسمي *", "Business Email *", "Email pro *")) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
              singleLine = true,
              modifier = Modifier.weight(1.2f).testTag("partner_email_input")
            )
            OutlinedTextField(
              value = contactPhoneInput,
              onValueChange = { contactPhoneInput = it },
              label = { Text(tr(currentLanguage, "الهاتف", "Phone", "Téléphone")) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
          }

          OutlinedTextField(
            value = monthlyVolumeInput,
            onValueChange = { monthlyVolumeInput = it },
            label = { Text(tr(currentLanguage, "الحجم الشهري المتوقع (USD)", "Est. Monthly Volume (USD)", "Volume mensuel estimé (USD)")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = businessNotesInput,
            onValueChange = { businessNotesInput = it },
            label = { Text(tr(currentLanguage, "وصف النشاط", "Business Scope & Notes", "Notes d'intégration")) },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth()
          )

          validationError?.let { err ->
            Text(text = err, color = StatusError, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
          }

          Button(
            onClick = {
              val comp = companyNameInput.trim()
              val web = websiteOrChannelInput.trim()
              val cname = contactNameInput.trim()
              val cemail = contactEmailInput.trim()
              val vol = monthlyVolumeInput.toDoubleOrNull() ?: 0.0
              if (comp.length < 2 || web.length < 2 || cname.length < 2 || !cemail.contains("@")) {
                validationError = tr(
                  currentLanguage,
                  "يرجى تعبئة اسم الشركة والرابط واسم المسؤول والبريد الصحيح.",
                  "Enter valid company name, website/channel, contact name, and email.",
                  "Saisissez un nom d'entreprise, site, contact et e-mail valides."
                )
              } else {
                onSubmitPartnerApplication(
                  comp,
                  selectedSector,
                  selectedPlatformType,
                  web,
                  cname,
                  cemail,
                  contactPhoneInput.trim(),
                  countryCodeInput.ifBlank { "DZ" },
                  vol,
                  businessNotesInput.trim()
                )
              }
            },
            enabled = !isBusy && platformSettings.allowPartnerRegistrations,
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent, contentColor = BrandPrimaryBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 50.dp)
              .testTag("submit_partner_registration_btn")
          ) {
            if (isBusy) {
              CircularProgressIndicator(color = BrandPrimaryBg, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
              Icon(Icons.Default.AppRegistration, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                tr(currentLanguage, "إرسال طلب الشراكة", "Submit Partner Registration", "Soumettre l'inscription"),
                fontWeight = FontWeight.ExtraBold
              )
            }
          }
        }
      }
    }

    // 3. My Registered Partner Applications
    if (myPartners.isNotEmpty()) {
      item {
        Text(
          text = tr(currentLanguage, "طلبات الشراكة المسجلة (${myPartners.size}):", "Your Partner Applications (${myPartners.size}):", "Vos demandes (${myPartners.size}) :"),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
        )
      }
      items(myPartners, key = { it.id }) { partner ->
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, BrandSecondaryAccent.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text(partner.companyName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
              Surface(color = BrandSecondaryAccent.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
                Text(
                  text = tr(currentLanguage, partner.status.titleAr, partner.status.titleEn, partner.status.titleEn),
                  color = BrandSecondaryAccent,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
            Text(
              text = "${tr(currentLanguage, partner.sector.titleAr, partner.sector.titleEn, partner.sector.titleEn)} • ${partner.countryCode}",
              fontSize = 11.sp,
              color = BrandPrimaryAccent
            )
            if (partner.ownerReviewNote.isNotBlank()) {
              Text(
                text = tr(currentLanguage, "ملاحظة: ", "Note: ", "Note : ") + partner.ownerReviewNote,
                fontSize = 11.sp,
                color = BrandSecondaryAccent
              )
            }
          }
        }
      }
    }

    // 4. Scannable Partner QR & Embed Generator
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = tr(currentLanguage, "رمز QR ورابط تضمين الشريك:", "Partner QR & Embed Link:", "QR & Lien d'intégration :"),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
          )
          OutlinedTextField(
            value = partnerChannelName,
            onValueChange = { partnerChannelName = it },
            label = { Text(tr(currentLanguage, "معرف الشريك", "Partner Handle ID", "Identifiant partenaire")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("partner_name_input")
          )
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Telegram WebApp", "WhatsApp Business", "Instagram Bio", "Web SDK").forEach { opt ->
              FilterChip(selected = partnerPlatform == opt, onClick = { partnerPlatform = opt }, label = { Text(opt.split(" ").first(), fontSize = 11.sp) })
            }
          }
          Surface(color = BrandPrimaryBg, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
              Text(generatedEmbedLink, fontSize = 11.sp, color = BrandPrimaryAccent, modifier = Modifier.weight(1f))
              IconButton(
                onClick = {
                  clipboardManager.setText(AnnotatedString(generatedEmbedLink))
                  Toast.makeText(context, tr(currentLanguage, "تم نسخ الرابط", "Link copied!", "Lien copié !"), Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.testTag("copy_embed_btn")
              ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = BrandPrimaryAccent)
              }
            }
          }
          QrCodeCard(
            qrData = generatedEmbedLink,
            title = tr(currentLanguage, "رمز QR الشريك الرسمي", "Official Partner QR", "QR Partenaire Officiel"),
            amountText = "${platformSettings.partnerCommissionPercent.toInt()}% Share",
            isRtl = isRtl,
            onShare = {
              val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Join Social Pay Partner Network: $generatedEmbedLink")
              }
              context.startActivity(Intent.createChooser(intent, tr(currentLanguage, "مشاركة رابط الشراكة", "Share Partner QR", "Partager")))
            }
          )
        }
      }
    }

    // 5. Quick Actions
    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
          onClick = onOpenWalletQr,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f).heightIn(min = 48.dp)
        ) {
          Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(tr(currentLanguage, "QR المحفظة", "Wallet QR", "QR Portefeuille"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
          onClick = onOpenDirectFeedback,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f).heightIn(min = 48.dp)
        ) {
          Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp), tint = BrandPrimaryAccent)
          Spacer(modifier = Modifier.width(6.dp))
          Text("LIVE COMMENTAIRE 🌍", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = BrandPrimaryAccent)
        }
      }
    }

    // 6. Language, Country & Timezone + Currency Settings
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(tr(currentLanguage, "اللغة، المنطقة والتوقيت:", "Language, Region & Timezone:", "Langue, Région et Fuseau :"), fontWeight = FontWeight.Bold)

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AppLanguage.entries.forEach { lang ->
              val isSelected = lang == currentLanguage
              Surface(
                color = if (isSelected) BrandPrimaryAccent else BrandPrimaryBg,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable { onLanguageSelected(lang) }.testTag("lang_btn_${lang.code}")
              ) {
                Text(
                  text = "${lang.flag} ${lang.displayName}",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  color = if (isSelected) BrandPrimaryBg else BrandTextPrimary,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
              }
            }
          }

          HorizontalDivider(color = BrandSurfaceBorder.copy(alpha = 0.35f))

          Text(
            text = tr(
              currentLanguage,
              "الدولة والمنطقة الزمنية ($userCountryCode • $userTimezone):",
              "Country & Timezone ($userCountryCode • $userTimezone):",
              "Pays & Fuseau ($userCountryCode • $userTimezone) :"
            ),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(countryPresets) { (code, label, tz) ->
              FilterChip(
                selected = userCountryCode.equals(code, ignoreCase = true),
                onClick = { onCountryTimezoneSelected(code, tz) },
                label = { Text("$code • $tz", fontSize = 10.sp) },
                modifier = Modifier.testTag("country_tz_chip_$code")
              )
            }
          }

          HorizontalDivider(color = BrandSurfaceBorder.copy(alpha = 0.35f))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(tr(currentLanguage, "العملة المرجعية:", "Reference Currency:", "Devise de référence :"), fontWeight = FontWeight.SemiBold)
            var showCurrencyMenu by remember { mutableStateOf(false) }
            val activeFiat = supportedFiat.find { it.code == selectedFiat } ?: supportedFiat.first()
            Box {
              OutlinedButton(onClick = { showCurrencyMenu = true }, shape = RoundedCornerShape(8.dp)) {
                Text("${activeFiat.symbol} (${activeFiat.code})", fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
              }
              DropdownMenu(expanded = showCurrencyMenu, onDismissRequest = { showCurrencyMenu = false }) {
                supportedFiat.forEach { fiat ->
                  DropdownMenuItem(
                    text = { Text("${fiat.code} - ${if (isRtl) fiat.nameAr else fiat.nameEn} (${fiat.symbol})") },
                    onClick = {
                      showCurrencyMenu = false
                      onFiatSelected(fiat.code)
                    }
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
