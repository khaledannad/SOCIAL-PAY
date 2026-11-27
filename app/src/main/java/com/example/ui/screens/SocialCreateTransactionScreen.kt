package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.asset.BlockchainNetwork
import com.example.core.asset.DigitalAssetEngine
import com.example.core.asset.StablecoinApprovalStatus
import com.example.core.asset.StablecoinRegistryEntry
import com.example.core.governance.tr
import com.example.core.transaction.RoutingPlanResult
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.TransactionEntity
import com.example.model.AppLanguage
import com.example.ui.components.QrCodeCard
import com.example.ui.components.SocialPayIconSymbol
import com.example.ui.components.SocialShareModalSheet
import com.example.ui.theme.*
import com.example.util.SocialIntegrationEngine
import java.util.Locale

@Composable
fun SocialCreateTransactionScreen(
  contacts: List<ContactEntity>,
  currentLanguage: AppLanguage,
  flatFeeUsd: Double = 0.80,
  prefilledQrRequest: com.example.util.ParsedPaymentRequest? = null,
  preselectedStablecoinEntry: StablecoinRegistryEntry? = null,
  onConsumePrefilledQr: () -> Unit = {},
  onConsumePreselectedStablecoin: () -> Unit = {},
  onEvaluatePlan: (
    recipientName: String,
    recipientAddressOrRail: String,
    stablecoinTicker: String,
    network: BlockchainNetwork,
    amount: Double,
    providerId: String
  ) -> RoutingPlanResult,
  onAuthorizeAndCreate: (
    type: String,
    title: String,
    description: String,
    amount: Double,
    stablecoinTicker: String,
    network: BlockchainNetwork,
    recipientName: String,
    recipientAddressOrRail: String,
    channel: String,
    providerId: String
  ) -> Unit,
  createdTransaction: TransactionEntity?,
  onDismissCreatedPreview: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val isRtl = currentLanguage.isRtl
  var showMultiChannelShareSheet by remember { mutableStateOf(false) }

  var selectedTypeIndex by remember { mutableStateOf(0) }
  val types = listOf(
    Triple("PAYMENT_REQUEST", tr(currentLanguage, "رابط دفع", "Payment Link", "Lien"), Icons.Default.Link),
    Triple("INVOICE", tr(currentLanguage, "فاتورة رسمية", "Invoice", "Facture"), Icons.Default.ReceiptLong),
    Triple("SELL", tr(currentLanguage, "تحويل صادر", "Outgoing Pay", "Envoi"), Icons.Default.ArrowUpward),
    Triple("BUY", tr(currentLanguage, "تحصيل وارد", "Incoming Collect", "Réception"), Icons.Default.ArrowDownward)
  )

  val approvedTickers = listOf("USDC", "USDT", "EURC")
  var selectedStablecoin by remember { mutableStateOf("USDC") }
  val availableNetworksForTicker = remember(selectedStablecoin) {
    DigitalAssetEngine.APPROVED_STABLECOIN_REGISTRY
      .filter { it.ticker == selectedStablecoin && it.approvalStatus == StablecoinApprovalStatus.APPROVED }
      .map { it.network }
      .distinct()
      .ifEmpty { listOf(BlockchainNetwork.SOLANA) }
  }
  var selectedNetwork by remember { mutableStateOf(BlockchainNetwork.SOLANA) }

  LaunchedEffect(availableNetworksForTicker) {
    if (selectedNetwork !in availableNetworksForTicker) {
      selectedNetwork = availableNetworksForTicker.first()
    }
  }

  val activeRegistryEntry = remember(selectedStablecoin, selectedNetwork) {
    DigitalAssetEngine.APPROVED_STABLECOIN_REGISTRY.find {
      it.ticker == selectedStablecoin && it.network == selectedNetwork
    }
  }

  var titleInput by remember {
    mutableStateOf(
      tr(currentLanguage, "طلب دفع بالعملات المستقرة", "Stablecoin Payment Request", "Demande de paiement Stablecoin")
    )
  }
  var amountInput by remember { mutableStateOf("100") }
  var recipientNameInput by remember { mutableStateOf("") }
  var recipientAddressInput by remember { mutableStateOf("") }
  var descriptionInput by remember {
    mutableStateOf(
      tr(
        currentLanguage,
        "طلب تسوية غير حافظ عبر الشبكة الموثقة",
        "Non-custodial settlement request via verified corridor",
        "Demande de règlement non dépositaire vérifiée"
      )
    )
  }

  var selectedChannel by remember { mutableStateOf("WHATSAPP") }
  val channels = listOf(
    "WHATSAPP" to "WhatsApp",
    "TELEGRAM_BOT" to "Telegram",
    "INSTAGRAM" to "Instagram",
    "MESSENGER" to "Messenger",
    "X_PLATFORM" to "X (Twitter)",
    "EMAIL" to "Email",
    "SMS" to "SMS"
  )

  var selectedProvider by remember { mutableStateOf("NON_CUSTODIAL_DIRECT") }
  val providers = listOf(
    "NON_CUSTODIAL_DIRECT" to tr(currentLanguage, "توجيه غير حافظ مباشر", "Non-Custodial Direct Rail", "Rail Direct Non-Dépositaire"),
    "SANDBOX_MOCK_GATEWAY" to tr(currentLanguage, "بوابة الاختبار (Sandbox)", "Sandbox Test Gateway", "Passerelle Sandbox"),
    "CIRCLE_CCTP" to "Circle CCTP (USDC/EURC)"
  )

  var pendingPlanReview by remember { mutableStateOf<RoutingPlanResult?>(null) }
  var blockingPlanError by remember { mutableStateOf<RoutingPlanResult?>(null) }

  LaunchedEffect(preselectedStablecoinEntry) {
    preselectedStablecoinEntry?.let { entry ->
      if (entry.ticker in approvedTickers) {
        selectedStablecoin = entry.ticker
        selectedNetwork = entry.network
      }
      onConsumePreselectedStablecoin()
    }
  }

  LaunchedEffect(prefilledQrRequest) {
    prefilledQrRequest?.let { req ->
      selectedTypeIndex = 0
      if (req.assetSymbol.uppercase() in approvedTickers) {
        selectedStablecoin = req.assetSymbol.uppercase()
      }
      BlockchainNetwork.entries.find {
        it.code.equals(req.network, ignoreCase = true) || it.name.equals(req.network, ignoreCase = true)
      }?.let { net ->
        if (net in availableNetworksForTicker) selectedNetwork = net
      }
      req.amount?.let { amountInput = it.toString() }
      if (req.recipientName.isNotBlank()) recipientNameInput = req.recipientName
      if (req.walletOrRailAddress.isNotBlank()) recipientAddressInput = req.walletOrRailAddress
      if (req.memo.isNotBlank()) descriptionInput = req.memo
      onConsumePrefilledQr()
    }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
  ) {
    // 1. Header
    item {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SocialPayIconSymbol(size = 36.dp)
        Column {
          Text(
            text = tr(
              currentLanguage,
              "إنشاء طلب دفع بالعملات المستقرة",
              "Create Stablecoin Payment Request",
              "Créer une demande en Stablecoin"
            ),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
          )
          Text(
            text = tr(
              currentLanguage,
              "يدعم USDC و USDT و EURC فقط • يتطلب مراجعة وموافقة صريحة قبل التنفيذ",
              "Approved Stablecoins Only (USDC, USDT, EURC) • Requires 7-Point Confirmation",
              "Stablecoins approuvés uniquement • Confirmation explicite requise"
            ),
            fontSize = 11.sp,
            color = BrandTextSecondary
          )
        }
      }
    }

    // 2. Request Type Tabs
    item {
      Surface(color = BrandSecondaryBg, shape = RoundedCornerShape(14.dp)) {
        ScrollableTabRow(
          selectedTabIndex = selectedTypeIndex,
          edgePadding = 4.dp,
          containerColor = BrandSecondaryBg,
          contentColor = BrandPrimaryAccent
        ) {
          types.forEachIndexed { index, (_, label, icon) ->
            Tab(
              selected = selectedTypeIndex == index,
              onClick = { selectedTypeIndex = index },
              text = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                  Text(label, fontSize = 11.sp, fontWeight = if (selectedTypeIndex == index) FontWeight.Bold else FontWeight.Normal)
                }
              }
            )
          }
        }
      }
    }

    // 3. Stablecoin, Network & Amount Card
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BrandPrimaryAccent.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = tr(currentLanguage, "العملة المستقرة والشبكة والمبلغ:", "Stablecoin, Network & Amount:", "Stablecoin, Réseau et Montant :"),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
          )

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            approvedTickers.forEach { ticker ->
              FilterChip(
                selected = selectedStablecoin == ticker,
                onClick = { selectedStablecoin = ticker },
                label = { Text(ticker, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                modifier = Modifier.testTag("stablecoin_chip_$ticker")
              )
            }
          }

          Text(
            text = tr(currentLanguage, "اختر شبكة التسوية الموثقة:", "Select Verified Settlement Chain:", "Sélectionnez le réseau vérifié :"),
            fontSize = 11.sp,
            color = BrandTextSecondary
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(availableNetworksForTicker) { net ->
              FilterChip(
                selected = selectedNetwork == net,
                onClick = { selectedNetwork = net },
                label = {
                  Text(
                    text = "${net.code} • ${tr(currentLanguage, net.displayNameAr, net.displayNameEn, net.displayNameFr)}",
                    fontSize = 11.sp
                  )
                },
                modifier = Modifier.testTag("network_chip_${net.code}")
              )
            }
          }

          OutlinedTextField(
            value = amountInput,
            onValueChange = { amountInput = it; blockingPlanError = null },
            label = { Text(tr(currentLanguage, "المبلغ ($selectedStablecoin)", "Amount ($selectedStablecoin)", "Montant ($selectedStablecoin)")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("create_txn_amount_input")
          )

          Surface(
            color = BrandSecondaryAccent.copy(alpha = 0.12f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = tr(currentLanguage, "الرسوم المعلنة بشفافية:", "Transparent Declared Fee:", "Frais déclarés :"),
                fontSize = 11.sp,
                color = BrandTextSecondary
              )
              val netGas = activeRegistryEntry?.estimatedNetworkFeeUsd ?: 0.0
              Text(
                text = "$${String.format(Locale.US, "%.2f", flatFeeUsd)} + gas ~$${String.format(Locale.US, "%.2f", netGas)}",
                fontWeight = FontWeight.Bold,
                color = BrandSecondaryAccent,
                fontSize = 11.sp
              )
            }
          }
        }
      }
    }

    // 4. Recipient Name & Verified Chain Address
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BrandSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = tr(currentLanguage, "بيانات المستلم وعنوان المحفظة غير الحافظة:", "Recipient & Non-Custodial Address:", "Bénéficiaire & Adresse :"),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
          )

          if (contacts.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(contacts) { contact ->
                Surface(
                  color = BrandPrimaryBg,
                  shape = RoundedCornerShape(14.dp),
                  border = BorderStroke(1.dp, BrandPrimaryAccent.copy(alpha = 0.35f)),
                  modifier = Modifier.clickable {
                    recipientNameInput = contact.name
                  }
                ) {
                  Text(
                    text = contact.name,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                  )
                }
              }
            }
          }

          OutlinedTextField(
            value = recipientNameInput,
            onValueChange = { recipientNameInput = it; blockingPlanError = null },
            label = { Text(tr(currentLanguage, "اسم المستلم الكامل", "Recipient Full Name", "Nom du bénéficiaire")) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("create_txn_recipient_name")
          )

          OutlinedTextField(
            value = recipientAddressInput,
            onValueChange = { recipientAddressInput = it; blockingPlanError = null },
            label = {
              Text(
                tr(
                  currentLanguage,
                  "عنوان الاستلام على شبكة ${selectedNetwork.code}",
                  "Destination Wallet Address (${selectedNetwork.code})",
                  "Adresse de réception (${selectedNetwork.code})"
                )
              )
            },
            placeholder = {
              Text(
                when (selectedNetwork) {
                  BlockchainNetwork.TRON_TRC20 -> "T..."
                  BlockchainNetwork.SOLANA -> "Base58 Solana Address"
                  else -> "0x..."
                },
                fontSize = 11.sp
              )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("create_txn_recipient_contact")
          )
        }
      }
    }

    // 5. Title, Notes, Social Channel & Provider
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BrandSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = titleInput,
            onValueChange = { titleInput = it },
            label = { Text(tr(currentLanguage, "عنوان الطلب / الفاتورة", "Request Title", "Titre")) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = descriptionInput,
            onValueChange = { descriptionInput = it },
            label = { Text(tr(currentLanguage, "الوصف والملاحظات", "Description & Terms", "Description")) },
            maxLines = 3,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          )

          Text(
            text = tr(currentLanguage, "قناة المشاركة الاجتماعية:", "Social Channel:", "Canal Social :"),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(channels) { (key, label) ->
              FilterChip(selected = selectedChannel == key, onClick = { selectedChannel = key }, label = { Text(label, fontSize = 11.sp) })
            }
          }

          Text(
            text = tr(currentLanguage, "مسار التسوية:", "Settlement Rail Provider:", "Fournisseur de règlement :"),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(providers) { (key, label) ->
              FilterChip(selected = selectedProvider == key, onClick = { selectedProvider = key }, label = { Text(label, fontSize = 11.sp) })
            }
          }
        }
      }
    }

    // Blocking Policy / Validation Report (if any)
    blockingPlanError?.let { failed ->
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = StatusError.copy(alpha = 0.12f)),
          border = BorderStroke(1.dp, StatusError),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = tr(
                currentLanguage,
                "تم إيقاف المسار — فشل الفحص المسبق:",
                "Pre-Execution Guard Blocked Request:",
                "Demande bloquée par la vérification :"
              ),
              fontWeight = FontWeight.Bold,
              color = StatusError,
              fontSize = 12.sp
            )
            Text(failed.blockingReason.orEmpty(), fontSize = 11.sp, color = BrandTextPrimary)
            failed.checks.filter { !it.passed }.forEach { step ->
              Text("• ${step.stepName}: ${step.detail}", fontSize = 10.sp, color = BrandTextSecondary)
            }
          }
        }
      }
    }

    // 6. Mandatory Pre-Execution Review Trigger Button
    item {
      Button(
        onClick = {
          val amt = amountInput.toDoubleOrNull() ?: 0.0
          val plan = onEvaluatePlan(
            recipientNameInput.trim(),
            recipientAddressInput.trim(),
            selectedStablecoin,
            selectedNetwork,
            amt,
            selectedProvider
          )
          if (plan.isEligible && plan.summary != null) {
            blockingPlanError = null
            pendingPlanReview = plan
          } else {
            blockingPlanError = plan
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent, contentColor = BrandPrimaryBg),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 52.dp)
          .testTag("submit_create_txn_btn")
      ) {
        Icon(Icons.Default.VerifiedUser, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = tr(
            currentLanguage,
            "مراجعة وتأكيد طلب الدفع (7 نقاط تحقق)",
            "Review & Authorize Request (7-Point Check)",
            "Vérifier et autoriser la demande"
          ),
          fontWeight = FontWeight.ExtraBold,
          fontSize = 14.sp
        )
      }
    }

    // 7. Created Payment Link & Scannable QR Preview
    createdTransaction?.let { txn ->
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(2.dp, BrandPrimaryAccent),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandPrimaryAccent)
                Text(
                  text = tr(currentLanguage, "تم توثيق الطلب وإنشاء الرابط!", "Request Authorized & Saved!", "Demande autorisée !"),
                  fontWeight = FontWeight.Bold,
                  style = MaterialTheme.typography.titleSmall,
                  color = BrandPrimaryAccent
                )
              }
              IconButton(onClick = onDismissCreatedPreview) {
                Icon(Icons.Default.Close, contentDescription = "Close")
              }
            }

            QrCodeCard(
              qrData = txn.qrCodeData,
              title = txn.title,
              amountText = "${String.format(Locale.US, "%,.2f", txn.amount)} ${txn.currency}",
              onShare = { SocialIntegrationEngine.shareViaGenericIntent(context, txn, isRtl) },
              isRtl = isRtl
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Button(
                onClick = { SocialIntegrationEngine.shareViaWhatsApp(context, txn, txn.recipientContact, isRtl) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
              ) {
                Text("WhatsApp", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
              }
              Button(
                onClick = { SocialIntegrationEngine.shareViaTelegram(context, txn, isRtl) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF229ED9)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
              ) {
                Text("Telegram", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
              }
            }

            Button(
              onClick = { showMultiChannelShareSheet = true },
              colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent, contentColor = BrandPrimaryBg),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = tr(currentLanguage, "مشاركة عبر الشبكات الاجتماعية", "Share via Social Channels", "Partager sur les réseaux"),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = {
                  clipboardManager.setText(AnnotatedString(txn.shareUrl))
                  Toast.makeText(
                    context,
                    tr(currentLanguage, "تم نسخ الرابط", "Link copied!", "Lien copié !"),
                    Toast.LENGTH_SHORT
                  ).show()
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(tr(currentLanguage, "نسخ الرابط", "Copy Link", "Copier"), fontSize = 11.sp)
              }
              OutlinedButton(
                onClick = {
                  val embedCode = SocialIntegrationEngine.generateWebEmbedCode(txn)
                  clipboardManager.setText(AnnotatedString(embedCode))
                  Toast.makeText(
                    context,
                    tr(currentLanguage, "تم نسخ كود التضمين", "Embed code copied!", "Code copié !"),
                    Toast.LENGTH_SHORT
                  ).show()
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(tr(currentLanguage, "كود التضمين", "Embed Code", "Intégrer"), fontSize = 11.sp)
              }
            }
          }
        }
      }
    }
  }

  // Mandatory 7-Point Pre-Execution Confirmation Dialog
  pendingPlanReview?.summary?.let { summary ->
    AlertDialog(
      onDismissRequest = { pendingPlanReview = null },
      containerColor = BrandSecondaryBg,
      title = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Default.Shield, contentDescription = null, tint = BrandPrimaryAccent)
          Text(
            text = tr(currentLanguage, "التأكيد النهائي قبل التفويض", "7-Point Pre-Execution Review", "Revue finale avant autorisation"),
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleMedium
          )
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          ReviewRow(tr(currentLanguage, "1. المستلم:", "1. Recipient:", "1. Bénéficiaire :"), summary.recipientName)
          ReviewRow(tr(currentLanguage, "2. المبلغ:", "2. Amount:", "2. Montant :"), "${summary.amount} ${summary.stablecoinTicker}")
          ReviewRow(tr(currentLanguage, "3. الشبكة والمزود:", "3. Network & Rail:", "3. Réseau :"), "${summary.network.code} • ${summary.providerId}")
          ReviewRow(
            tr(currentLanguage, "4. رسوم المنصة والشبكة:", "4. Platform + Gas Fee:", "4. Frais totaux :"),
            "$${String.format(Locale.US, "%.2f", summary.declaredPlatformFeeUsd)} + $${String.format(Locale.US, "%.2f", summary.estimatedNetworkFeeUsd)}"
          )
          ReviewRow(
            tr(currentLanguage, "5. المجموع النهائي التقديري:", "5. Final Total:", "5. Total estimé :"),
            "$${String.format(Locale.US, "%.2f", summary.finalTotalChargeUsd)}",
            highlight = true
          )
          ReviewRow(tr(currentLanguage, "6. حالة التحقق:", "6. Verification Status:", "6. Vérification :"), summary.verificationStatusLabel)

          Surface(
            color = BrandPrimaryBg,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = tr(currentLanguage, "7. إفصاح المخاطر: ", "7. Risk Disclosure: ", "7. Risques : ") + summary.riskDisclosure,
              fontSize = 10.sp,
              color = BrandTextSecondary,
              modifier = Modifier.padding(8.dp)
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val currentType = types[selectedTypeIndex].first
            onAuthorizeAndCreate(
              currentType,
              titleInput.ifBlank { "${summary.stablecoinTicker} Request" },
              descriptionInput,
              summary.amount,
              summary.stablecoinTicker,
              summary.network,
              summary.recipientName,
              recipientAddressInput.trim(),
              selectedChannel,
              selectedProvider
            )
            pendingPlanReview = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryAccent, contentColor = BrandPrimaryBg),
          modifier = Modifier.testTag("confirm_pre_execution_btn")
        ) {
          Text(
            text = tr(currentLanguage, "أوافق وأفوض الإنشاء", "Authorize & Create", "Autoriser et créer"),
            fontWeight = FontWeight.ExtraBold
          )
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingPlanReview = null }) {
          Text(tr(currentLanguage, "تراجع", "Cancel", "Annuler"), color = BrandTextSecondary)
        }
      }
    )
  }

  if (showMultiChannelShareSheet && createdTransaction != null) {
    SocialShareModalSheet(
      transaction = createdTransaction,
      isRtl = isRtl,
      onDismiss = { showMultiChannelShareSheet = false }
    )
  }
}

@Composable
private fun ReviewRow(label: String, value: String, highlight: Boolean = false) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(label, fontSize = 11.sp, color = BrandTextSecondary)
    Text(
      text = value,
      fontSize = 11.sp,
      fontWeight = if (highlight) FontWeight.ExtraBold else FontWeight.SemiBold,
      color = if (highlight) BrandPrimaryAccent else BrandTextPrimary
    )
  }
}
