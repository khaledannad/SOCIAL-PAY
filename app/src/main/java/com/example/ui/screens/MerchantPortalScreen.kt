package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.asset.BlockchainNetwork
import com.example.core.asset.DigitalAssetEngine
import com.example.core.governance.tr
import com.example.core.privacy.VerificationWorkflowStatus
import com.example.data.MerchantProfileRecord
import com.example.data.MerchantStaffRecord
import com.example.data.MerchantStaffRole
import com.example.model.AppLanguage
import com.example.ui.components.QrCodeCard
import com.example.ui.theme.*

@Composable
fun MerchantPortalScreen(
  currentLanguage: AppLanguage,
  merchantProfile: MerchantProfileRecord?,
  merchantStaff: List<MerchantStaffRecord>,
  isBusy: Boolean = false,
  onSaveMerchantProfile: (
    businessName: String,
    countryCode: String,
    webhookUrl: String,
    stablecoin: String,
    networkCode: String
  ) -> Unit,
  onAddStaff: (name: String, email: String, role: MerchantStaffRole) -> Unit,
  onRemoveStaff: (staffId: String) -> Unit,
  onOpenInvoices: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val isRtl = currentLanguage.isRtl

  var businessName by remember(merchantProfile) { mutableStateOf(merchantProfile?.businessName.orEmpty()) }
  var countryCode by remember(merchantProfile) { mutableStateOf(merchantProfile?.countryCode ?: "DZ") }
  var webhookUrl by remember(merchantProfile) { mutableStateOf(merchantProfile?.webhookUrl.orEmpty()) }
  var selectedStablecoin by remember(merchantProfile) { mutableStateOf(merchantProfile?.defaultStablecoin ?: "USDC") }
  var selectedNetworkCode by remember(merchantProfile) { mutableStateOf(merchantProfile?.defaultNetwork ?: "SOL") }
  var checkoutAmountInput by remember { mutableStateOf("100.00") }

  var showAddStaffDialog by remember { mutableStateOf(false) }

  val kybEnum = enumValues<VerificationWorkflowStatus>()
    .firstOrNull { it.name == merchantProfile?.kybStatus }
    ?: VerificationWorkflowStatus.NOT_STARTED

  val stablecoinTickers = DigitalAssetEngine.APPROVED_STABLECOIN_REGISTRY
    .map { it.ticker }
    .distinct()
    .ifEmpty { listOf("USDC", "USDT", "EURC") }

  val networkChoices = BlockchainNetwork.entries.filter { it != BlockchainNetwork.FIAT_BANKING }

  val parsedCheckoutAmount = checkoutAmountInput.toDoubleOrNull()?.takeIf { it > 0.0 } ?: 0.0
  val merchantIdRef = merchantProfile?.id?.ifBlank { "UNREGISTERED" } ?: "UNREGISTERED"
  val merchantCheckoutUri = "socialpay://checkout?merchant=$merchantIdRef&asset=$selectedStablecoin&net=$selectedNetworkCode&amount=$parsedCheckoutAmount"

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
  ) {
    // 1. Merchant & Business Header
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg),
        border = BorderStroke(1.dp, BrandSecondaryAccent.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Icon(Icons.Default.Storefront, contentDescription = null, tint = BrandPrimaryAccent, modifier = Modifier.size(28.dp))
              Column {
                Text(
                  text = tr(currentLanguage, "بوابة التجار والشركات", "Merchant & Business Portal", "Portail Marchands & Entreprises"),
                  fontWeight = FontWeight.ExtraBold,
                  color = BrandTextPrimary,
                  style = MaterialTheme.typography.titleMedium
                )
                Text(
                  text = tr(currentLanguage, "فواتير B2B • روابط الدفع السريعة • صلاحيات الفريق", "Checkout QR • Webhooks • Scoped Staff RBAC", "QR Paiement • Webhooks • RBAC Équipe"),
                  fontSize = 11.sp,
                  color = BrandTextSecondary
                )
              }
            }

            Surface(
              color = if (kybEnum == VerificationWorkflowStatus.VERIFIED) BrandPrimaryAccent.copy(alpha = 0.16f) else StatusWarning.copy(alpha = 0.16f),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                text = tr(currentLanguage, kybEnum.titleAr, kybEnum.titleEn, kybEnum.titleFr),
                color = if (kybEnum == VerificationWorkflowStatus.VERIFIED) BrandPrimaryAccent else StatusWarning,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          if (!merchantProfile?.apiKeyReference.isNullOrBlank()) {
            Surface(color = BrandPrimaryBg, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, BrandSurfaceBorder)) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = tr(currentLanguage, "مرجع API التاجر (معرف سحابي فقط):", "Merchant Key Reference (Masked):", "Référence API Marchand :"),
                  fontSize = 10.sp,
                  color = BrandTextMuted
                )
                Text(
                  text = merchantProfile?.apiKeyReference.orEmpty(),
                  fontSize = 11.sp,
                  fontFamily = FontFamily.Monospace,
                  color = BrandPrimaryAccent,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          OutlinedButton(
            onClick = onOpenInvoices,
            border = BorderStroke(1.dp, BrandPrimaryAccent.copy(alpha = 0.65f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("merchant_open_invoices_btn")
          ) {
            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = BrandPrimaryAccent, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = tr(currentLanguage, "فتح سجل الفواتير الرسمية", "Open Official Invoices Center", "Ouvrir les Factures Officielles"),
              color = BrandPrimaryAccent,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    // 2. Business Profile Setup
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg),
        border = BorderStroke(1.dp, BrandSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = tr(currentLanguage, "إعداد ملف المنشأة ومسار التسوية الافتراضي", "Business Profile & Default Settlement Rail", "Profil Entreprise & Rail par défaut"),
            fontWeight = FontWeight.Bold,
            color = BrandTextPrimary,
            style = MaterialTheme.typography.titleSmall
          )

          OutlinedTextField(
            value = businessName,
            onValueChange = { businessName = it },
            label = { Text(tr(currentLanguage, "اسم المتجر أو الشركة", "Business / Store Name", "Nom de l'entreprise")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("merchant_business_name_input")
          )

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
              value = countryCode,
              onValueChange = { countryCode = it.uppercase().take(3) },
              label = { Text(tr(currentLanguage, "الدولة", "Country", "Pays")) },
              singleLine = true,
              modifier = Modifier.weight(0.35f)
            )
            OutlinedTextField(
              value = webhookUrl,
              onValueChange = { webhookUrl = it },
              label = { Text(tr(currentLanguage, "رابط Webhook للإشعارات", "Webhook Endpoint URL", "URL Webhook")) },
              singleLine = true,
              modifier = Modifier.weight(0.65f)
            )
          }

          Text(
            text = tr(currentLanguage, "العملة المستقرة الافتراضية", "Default Approved Stablecoin", "Stablecoin par défaut"),
            fontSize = 11.sp,
            color = BrandTextSecondary
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(stablecoinTickers) { ticker ->
              FilterChip(
                selected = selectedStablecoin == ticker,
                onClick = { selectedStablecoin = ticker },
                label = { Text(ticker, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
              )
            }
          }

          Text(
            text = tr(currentLanguage, "الشبكة المعتمدة", "Default Blockchain Network", "Réseau Blockchain"),
            fontSize = 11.sp,
            color = BrandTextSecondary
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(networkChoices) { net ->
              FilterChip(
                selected = selectedNetworkCode == net.code,
                onClick = { selectedNetworkCode = net.code },
                label = { Text(net.code, fontSize = 11.sp) }
              )
            }
          }

          Button(
            onClick = {
              if (businessName.isNotBlank()) {
                onSaveMerchantProfile(businessName.trim(), countryCode.ifBlank { "DZ" }, webhookUrl.trim(), selectedStablecoin, selectedNetworkCode)
              }
            },
            enabled = !isBusy && businessName.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("save_merchant_profile_btn")
          ) {
            Text(
              text = tr(currentLanguage, "حفظ وتحديث بوابة التاجر", "Save Business Portal Config", "Enregistrer le portail"),
              color = BrandPrimaryBg,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    // 3. Scannable Checkout QR Generator (ZXing)
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurfaceVariant),
        border = BorderStroke(1.dp, BrandPrimaryAccent.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = tr(currentLanguage, "مولد رمز دفع نقطة البيع (POS Checkout QR)", "POS Merchant Checkout QR", "Générateur QR Caisse"),
            fontWeight = FontWeight.Bold,
            color = BrandTextPrimary,
            style = MaterialTheme.typography.titleSmall
          )
          OutlinedTextField(
            value = checkoutAmountInput,
            onValueChange = { checkoutAmountInput = it },
            label = { Text("${tr(currentLanguage, "المبلغ المطلوب", "Checkout Amount", "Montant")} ($selectedStablecoin)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          QrCodeCard(
            qrData = merchantCheckoutUri,
            title = businessName.ifBlank { tr(currentLanguage, "متجر Social Pay", "Social Pay Merchant", "Marchand Social Pay") },
            amountText = "$parsedCheckoutAmount $selectedStablecoin ($selectedNetworkCode)",
            isRtl = isRtl,
            onShare = {
              val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, merchantCheckoutUri)
              }
              context.startActivity(Intent.createChooser(intent, "Share Merchant Checkout"))
            }
          )
        }
      }
    }

    // 4. Scoped Merchant Staff RBAC
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = tr(currentLanguage, "فريق العمل وصلاحيات الوصول (RBAC)", "Company Staff & Role Scoping", "Équipe & Contrôle d'accès"),
            fontWeight = FontWeight.Bold,
            color = BrandTextPrimary,
            style = MaterialTheme.typography.titleSmall
          )
          Text(
            text = tr(currentLanguage, "صلاحيات معزولة لكل منشأة — دون وصول لإدارة المنصة", "Strictly scoped to your merchant tenant", "Strictement limité à votre entreprise"),
            fontSize = 10.sp,
            color = BrandTextMuted
          )
        }
        Button(
          onClick = { showAddStaffDialog = true },
          colors = ButtonDefaults.buttonColors(containerColor = BrandSecondaryAccent),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.testTag("merchant_add_staff_btn")
        ) {
          Icon(Icons.Default.PersonAdd, contentDescription = null, tint = BrandPrimaryBg, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(tr(currentLanguage, "إضافة موظف", "Add Staff", "Ajouter"), color = BrandPrimaryBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    if (merchantStaff.isEmpty()) {
      item {
        Surface(color = BrandSecondaryBg, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, BrandSurfaceBorder), modifier = Modifier.fillMaxWidth()) {
          Text(
            text = tr(currentLanguage, "لا يوجد موظفون مضافون بعد. أضف محاسباً أو أمين صندوق بصلاحية محددة.", "No staff added yet. Assign Cashiers, Finance Viewers, or API Developers.", "Aucun collaborateur ajouté."),
            fontSize = 11.sp,
            color = BrandTextSecondary,
            modifier = Modifier.padding(14.dp)
          )
        }
      }
    } else {
      items(merchantStaff, key = { it.id }) { staff ->
        val roleEnum = enumValues<MerchantStaffRole>().firstOrNull { it.name == staff.role } ?: MerchantStaffRole.CASHIER
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg),
          border = BorderStroke(1.dp, BrandSurfaceBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(staff.staffName, fontWeight = FontWeight.Bold, color = BrandTextPrimary, fontSize = 13.sp)
              Text(staff.staffEmail, fontSize = 11.sp, color = BrandTextSecondary)
              Surface(color = BrandPrimaryAccent.copy(alpha = 0.14f), shape = RoundedCornerShape(4.dp)) {
                Text(
                  text = tr(currentLanguage, roleEnum.titleAr, roleEnum.titleEn, roleEnum.titleFr),
                  color = BrandPrimaryAccent,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
            IconButton(onClick = { onRemoveStaff(staff.id) }) {
              Icon(Icons.Default.DeleteOutline, contentDescription = "Remove staff", tint = StatusError)
            }
          }
        }
      }
    }
  }

  if (showAddStaffDialog) {
    var staffName by remember { mutableStateOf("") }
    var staffEmail by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(MerchantStaffRole.CASHIER) }

    AlertDialog(
      onDismissRequest = { showAddStaffDialog = false },
      title = { Text(tr(currentLanguage, "إضافة عضو فريق بصلاحية محددة", "Add Scoped Staff Member", "Ajouter un membre")) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = staffName, onValueChange = { staffName = it }, label = { Text(tr(currentLanguage, "الاسم الكامل", "Full Name", "Nom")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = staffEmail, onValueChange = { staffEmail = it }, label = { Text(tr(currentLanguage, "البريد الإلكتروني", "Work Email", "Email")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
          Text(tr(currentLanguage, "الدور الوظيفي", "Role Scope", "Rôle"), fontSize = 11.sp, color = BrandTextSecondary)
          MerchantStaffRole.entries.forEach { role ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
              RadioButton(selected = selectedRole == role, onClick = { selectedRole = role })
              Text(tr(currentLanguage, role.titleAr, role.titleEn, role.titleFr), fontSize = 12.sp)
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (staffName.isNotBlank() && staffEmail.contains("@")) {
              onAddStaff(staffName.trim(), staffEmail.trim(), selectedRole)
              showAddStaffDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent)
        ) {
          Text(tr(currentLanguage, "إضافة", "Add", "Ajouter"), color = BrandPrimaryBg, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddStaffDialog = false }) { Text(tr(currentLanguage, "إلغاء", "Cancel", "Annuler")) }
      }
    )
  }
}
