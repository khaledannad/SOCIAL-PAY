package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.governance.tr
import com.example.data.local.entity.TransactionEntity
import com.example.model.AppLanguage
import com.example.ui.components.SocialPayLogo
import com.example.ui.components.SocialPayLogoStyle
import com.example.ui.theme.*

@Composable
fun SocialHomeScreen(
  recentTransactions: List<TransactionEntity>,
  totalCount: Int,
  confirmedCount: Int,
  pendingCount: Int,
  currentFeeUsd: Double = 0.80,
  currentLanguage: AppLanguage,
  onNavigateToCreate: () -> Unit,
  onNavigateToStablecoins: () -> Unit,
  onNavigateToTransactions: () -> Unit,
  onNavigateToInvoices: () -> Unit,
  onNavigateToMerchant: () -> Unit,
  onOpenWalletQr: () -> Unit = {},
  onOpenPartners: () -> Unit = {},
  onOpenFeedback: () -> Unit = {},
  onSelectTransaction: (TransactionEntity) -> Unit,
  modifier: Modifier = Modifier
) {
  val totalVolume = recentTransactions.sumOf { it.amount }
  val successRateText = if (totalCount > 0) {
    val pct = (confirmedCount.toDouble() / totalCount.toDouble()) * 100.0
    String.format(java.util.Locale.US, "%.1f%%", pct)
  } else {
    "N/A"
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
  ) {
    // 1. Hero Brand Banner & Non-Custodial Stablecoin Overview Card
    item {
      Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg),
        border = BorderStroke(
          1.5.dp,
          Brush.linearGradient(listOf(BrandPrimaryAccent, BrandSecondaryAccent, BrandIntelligenceAccent))
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(128.dp)
          ) {
            Image(
              painter = painterResource(id = R.drawable.social_pay_hero),
              contentDescription = tr(currentLanguage, "شبكة سوشيال باي العالمية", "Social Pay Global Network", "Réseau Mondial Social Pay"),
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(
                  Brush.verticalGradient(
                    listOf(
                      Color.Transparent,
                      BrandSecondaryBg.copy(alpha = 0.78f),
                      BrandSecondaryBg
                    )
                  )
                )
            )
            Surface(
              color = BrandPrimaryBg.copy(alpha = 0.88f),
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, BrandPrimaryAccent.copy(alpha = 0.55f)),
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
            ) {
              Text(
                text = tr(
                  currentLanguage,
                  "الرسوم الثابتة $${String.format(java.util.Locale.US, "%.2f", currentFeeUsd)}",
                  "Flat Fee $${String.format(java.util.Locale.US, "%.2f", currentFeeUsd)}",
                  "Frais fixes $${String.format(java.util.Locale.US, "%.2f", currentFeeUsd)}"
                ),
                color = BrandPrimaryAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            SocialPayLogo(
              style = SocialPayLogoStyle.FULL_HORIZONTAL,
              iconSize = 34.dp,
              showTagline = true
            )

            // Non-Custodial Policy Pill
            Surface(
              color = BrandPrimaryBg.copy(alpha = 0.65f),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, BrandSecondaryAccent.copy(alpha = 0.35f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = BrandPrimaryAccent, modifier = Modifier.size(16.dp))
                Text(
                  text = tr(
                    currentLanguage,
                    "العملات المستقرة المعتمدة فقط (USDC / USDT / EURC) • غير حافظة للأموال",
                    "Approved Stablecoins Only (USDC / USDT / EURC) • Non-Custodial",
                    "Stablecoins approuvés uniquement (USDC / USDT / EURC) • Non dépositaire"
                  ),
                  fontSize = 11.sp,
                  color = BrandTextSecondary
                )
              }
            }

            // Honest Local Ledger Summary
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Bottom
            ) {
              Column {
                Text(
                  text = tr(
                    currentLanguage,
                    "حجم الطلبات المسجلة محلياً",
                    "Recorded Ledger Volume",
                    "Volume enregistré localement"
                  ),
                  fontSize = 11.sp,
                  color = BrandTextSecondary
                )
                Text(
                  text = if (totalCount > 0) String.format(java.util.Locale.US, "%,.2f USD", totalVolume) else "0.00 USD",
                  fontSize = 22.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = BrandPrimaryAccent
                )
              }

              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = tr(currentLanguage, "العمليات المسوّاة", "Settled", "Réglées"),
                  fontSize = 11.sp,
                  color = BrandTextSecondary
                )
                Text(
                  text = tr(
                    currentLanguage,
                    "$confirmedCount مكتملة",
                    "$confirmedCount settled",
                    "$confirmedCount réglées"
                  ),
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = StatusSuccess
                )
              }
            }

            HorizontalDivider(color = BrandSurfaceBorder.copy(alpha = 0.5f))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceAround
            ) {
              QuickStatItem(
                title = tr(currentLanguage, "إجمالي الطلبات", "Total Requests", "Total"),
                value = "$totalCount",
                color = EmeraldLight
              )
              QuickStatItem(
                title = tr(currentLanguage, "قيد الانتظار", "Pending", "En attente"),
                value = "$pendingCount",
                color = StatusWarning
              )
              QuickStatItem(
                title = tr(currentLanguage, "نسبة التسوية", "Settlement Rate", "Taux"),
                value = successRateText,
                color = BrandPrimaryAccent
              )
            }
          }
        }
      }
    }

    // 2. Quick Actions (Strictly Customer / Business Tools — ZERO Owner Admin Surfaces)
    item {
      Text(
        text = tr(currentLanguage, "إجراءات سريعة:", "Quick Actions:", "Actions rapides :"),
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleSmall
      )
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        QuickActionCard(
          title = tr(currentLanguage, "QR المحفظة و P2P", "Wallet QR Pay", "QR Portefeuille"),
          subtitle = tr(currentLanguage, "إنشاء ومسح رمز الدفع", "Generate & Scan QR", "Générer & Scanner"),
          icon = Icons.Default.QrCode2,
          tint = BrandPrimaryAccent,
          onClick = onOpenWalletQr,
          modifier = Modifier.weight(1f).testTag("home_wallet_qr_card")
        )
        QuickActionCard(
          title = tr(currentLanguage, "طلب دفع مستقر", "New Stablecoin Link", "Nouveau Paiement"),
          subtitle = tr(currentLanguage, "إنشاء ومشاركة رابط الدفع", "Create & Share Link", "Créer un lien"),
          icon = Icons.Default.AddCircle,
          tint = EmeraldPrimary,
          onClick = onNavigateToCreate,
          modifier = Modifier.weight(1f).testTag("home_create_card")
        )
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        QuickActionCard(
          title = tr(currentLanguage, "العملات المستقرة", "Approved Stablecoins", "Stablecoins"),
          subtitle = tr(currentLanguage, "مراقب الربط والشبكات", "Peg & Network Registry", "Registre & Réseaux"),
          icon = Icons.Default.VerifiedUser,
          tint = BrandSecondaryAccent,
          onClick = onNavigateToStablecoins,
          modifier = Modifier.weight(1f).testTag("home_stablecoins_card")
        )
        QuickActionCard(
          title = tr(currentLanguage, "بوابة التاجر والشركة", "Merchant Portal", "Portail Marchand"),
          subtitle = tr(currentLanguage, "صلاحيات الفريق والـ QR", "Checkout & Staff RBAC", "Équipe & Paiements"),
          icon = Icons.Default.Storefront,
          tint = BrandIntelligenceAccent,
          onClick = onNavigateToMerchant,
          modifier = Modifier.weight(1f).testTag("home_merchant_card")
        )
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        QuickActionCard(
          title = tr(currentLanguage, "الفواتير الرسمية", "Invoices", "Factures"),
          subtitle = tr(currentLanguage, "إصدار ومتابعة التحصيل", "Issue & Collect", "Émettre & Suivre"),
          icon = Icons.Default.ReceiptLong,
          tint = StatusWarning,
          onClick = onNavigateToInvoices,
          modifier = Modifier.weight(1f).testTag("home_invoices_card")
        )
        QuickActionCard(
          title = tr(currentLanguage, "الشركاء والشركات", "Business Partners", "Partenaires"),
          subtitle = tr(currentLanguage, "تسجيل 12 قطاعاً", "All-Sector Onboarding", "12 Secteurs"),
          icon = Icons.Default.Handshake,
          tint = BrandSecondaryAccent,
          onClick = onOpenPartners,
          modifier = Modifier.weight(1f).testTag("home_partners_card")
        )
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        QuickActionCard(
          title = tr(currentLanguage, "الدفتر والسجل", "Transaction Ledger", "Registre"),
          subtitle = tr(currentLanguage, "متابعة الحالات والتدقيق", "States & Proof", "Suivi & Preuves"),
          icon = Icons.Default.History,
          tint = EmeraldLight,
          onClick = onNavigateToTransactions,
          modifier = Modifier.weight(1f).testTag("home_ledger_card")
        )
        QuickActionCard(
          title = "LIVE COMMENTAIRE 🌍",
          subtitle = tr(currentLanguage, "مجتمع عالمي مفتوح", "Global Social Space", "Espace Social Mondial", "Мировое сообщество", "全球开放社区"),
          icon = Icons.Default.Public,
          tint = BrandPrimaryAccent,
          onClick = onOpenFeedback,
          modifier = Modifier.weight(1f).testTag("home_feedback_card")
        )
      }
    }

    // 3. Official Privacy-Preserving Social Share Rails
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSecondaryBg.copy(alpha = 0.72f)),
        border = BorderStroke(1.dp, BrandPrimaryAccent.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.Hub, contentDescription = null, tint = BrandPrimaryAccent, modifier = Modifier.size(18.dp))
            Text(
              text = tr(
                currentLanguage,
                "محرك المشاركة الاجتماعية الآمن:",
                "Privacy-Preserving Social Rail:",
                "Partage Social Sécurisé :"
              ),
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp
            )
          }

          Text(
            text = tr(
              currentLanguage,
              "مشاركة روابط الدفع الرسمية عبر النظام فقط دون الوصول لرسائلك الخاصة أو جهات اتصالك.",
              "Share payment requests via official OS share links without scraping private chats or contacts.",
              "Partagez des liens de paiement officiels sans accès aux discussions privées ni aux contacts."
            ),
            fontSize = 11.sp,
            color = BrandTextSecondary
          )

          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { IntegrationPill(name = "WhatsApp", status = tr(currentLanguage, "مشاركة رابط", "Deep Link", "Lien"), color = Color(0xFF25D366)) }
            item { IntegrationPill(name = "Telegram", status = tr(currentLanguage, "تطبيق/رابط", "Mini App/Link", "Lien"), color = Color(0xFF229ED9)) }
            item { IntegrationPill(name = "Instagram", status = tr(currentLanguage, "رابط مباشر", "Direct Link", "Lien direct"), color = Color(0xFFE1306C)) }
            item { IntegrationPill(name = "Web SDK", status = tr(currentLanguage, "تضمين QR", "Embed QR", "Intégration"), color = BrandPrimaryAccent) }
          }
        }
      }
    }

    // 4. Recent Ledger Activity
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = tr(currentLanguage, "أحدث طلبات المعاملات:", "Recent Payment Requests:", "Demandes récentes :"),
          fontWeight = FontWeight.Bold,
          style = MaterialTheme.typography.titleSmall
        )
        TextButton(onClick = onNavigateToTransactions) {
          Text(
            text = tr(currentLanguage, "عرض الكل ($totalCount)", "View All ($totalCount)", "Tout voir ($totalCount)"),
            color = BrandPrimaryAccent,
            fontSize = 11.sp
          )
        }
      }
    }

    if (recentTransactions.isEmpty()) {
      item {
        Surface(
          color = BrandSecondaryBg.copy(alpha = 0.55f),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, BrandSurfaceBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = BrandTextSecondary, modifier = Modifier.size(28.dp))
            Text(
              text = tr(
                currentLanguage,
                "لا توجد معاملات مسجلة بعد — أنشئ أول طلب دفع بعملة مستقرة",
                "No transactions recorded yet — create your first stablecoin request",
                "Aucune transaction enregistrée — créez votre première demande"
              ),
              fontSize = 12.sp,
              color = BrandTextSecondary
            )
          }
        }
      }
    } else {
      items(recentTransactions.take(4)) { txn ->
        TransactionMiniCard(
          txn = txn,
          currentLanguage = currentLanguage,
          onClick = { onSelectTransaction(txn) }
        )
      }
    }
  }
}

@Composable
fun QuickStatItem(title: String, value: String, color: Color) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
    Text(text = title, fontSize = 10.sp, color = BrandTextSecondary)
  }
}

@Composable
fun QuickActionCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  tint: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    modifier = modifier.clickable { onClick() }
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
      }
      Column {
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
        Text(text = subtitle, fontSize = 10.sp, color = BrandTextSecondary, maxLines = 1)
      }
    }
  }
}

@Composable
fun IntegrationPill(name: String, status: String, color: Color) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(8.dp),
    border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
      Text(text = name, fontWeight = FontWeight.Bold, fontSize = 10.sp)
      Text(text = "• $status", fontSize = 9.sp, color = BrandTextSecondary)
    }
  }
}

@Composable
fun TransactionMiniCard(
  txn: TransactionEntity,
  currentLanguage: AppLanguage,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isSettled = txn.status in setOf("SETTLED", "CONFIRMED", "COMPLETED")
  val isCancelled = txn.status in setOf("CANCELLED", "FAILED")
  val isReview = txn.status == "UNDER_REVIEW"
  val badgeColor = when {
    isSettled -> StatusSuccess
    isCancelled -> StatusError
    isReview -> BrandIntelligenceAccent
    else -> StatusWarning
  }

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    modifier = modifier
      .fillMaxWidth()
      .clickable { onClick() }
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(badgeColor.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = when (txn.type) {
              "INVOICE" -> Icons.Default.ReceiptLong
              "BUY" -> Icons.Default.ArrowDownward
              "SELL" -> Icons.Default.ArrowUpward
              else -> Icons.Default.Share
            },
            contentDescription = null,
            tint = badgeColor,
            modifier = Modifier.size(20.dp)
          )
        }

        Column {
          Text(text = txn.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
          Text(
            text = "#${txn.id} • ${txn.channel} • ${txn.recipientName}",
            fontSize = 10.sp,
            color = BrandTextSecondary,
            maxLines = 1
          )
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = "${String.format(java.util.Locale.US, "%,.2f", txn.amount)} ${txn.currency}",
          fontWeight = FontWeight.ExtraBold,
          fontSize = 13.sp,
          color = BrandPrimaryAccent
        )
        Surface(
          color = badgeColor.copy(alpha = 0.15f),
          shape = RoundedCornerShape(4.dp)
        ) {
          Text(
            text = when (txn.status) {
              "SETTLED", "CONFIRMED", "COMPLETED" -> tr(currentLanguage, "تمت التسوية", "Settled", "Réglée")
              "AUTHORIZED" -> tr(currentLanguage, "مفوّضة", "Authorized", "Autorisée")
              "PROCESSING" -> tr(currentLanguage, "قيد التنفيذ", "Processing", "En cours")
              "UNDER_REVIEW" -> tr(currentLanguage, "قيد المراجعة", "Under Review", "En examen")
              "ESCROW_LOCKED" -> tr(currentLanguage, "بانتظار الإثبات", "Awaiting Proof", "En attente")
              "FAILED" -> tr(currentLanguage, "فشلت", "Failed", "Échouée")
              "CANCELLED" -> tr(currentLanguage, "ملغاة", "Cancelled", "Annulée")
              else -> tr(currentLanguage, "قيد الانتظار", "Pending", "En attente")
            },
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = badgeColor,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
          )
        }
      }
    }
  }
}
