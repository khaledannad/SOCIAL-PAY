package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EscrowStatus
import com.example.model.SmartDeal
import com.example.model.TradeType
import com.example.ui.theme.*

@Composable
fun ActiveDealsListScreen(
  deals: List<SmartDeal>,
  isRtl: Boolean,
  onSelectDeal: (SmartDeal) -> Unit,
  onNavigateToMarket: () -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableStateOf(0) } // 0: All, 1: Active, 2: Completed

  val filteredDeals = remember(deals, selectedTab) {
    when (selectedTab) {
      1 -> deals.filter { it.status == EscrowStatus.LOCKED_IN_ESCROW || it.status == EscrowStatus.PAID_PENDING_CONFIRMATION }
      2 -> deals.filter { it.status == EscrowStatus.RELEASED_COMPLETED }
      else -> deals
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    // Header & Tab Switcher
    Text(
      text = if (isRtl) "صفقات الأمانة المشفرة (Smart Escrow)" else "Smart Escrow Deals",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(10.dp))

    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceVariant
    ) {
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.primary
      ) {
        val tabs = if (isRtl) listOf("جميع الصفقات", "الجارية الآن", "المكتملة") else listOf("All Deals", "In Progress", "Completed")
        tabs.forEachIndexed { index, label ->
          Tab(
            selected = selectedTab == index,
            onClick = { selectedTab = index },
            text = {
              Text(
                text = label,
                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
              )
            }
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    if (filteredDeals.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.Inbox,
            contentDescription = "Empty",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp)
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = if (isRtl) "لا توجد صفقات في هذه القائمة حالياً" else "No deals found in this view",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(8.dp))
          Button(
            onClick = onNavigateToMarket,
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
          ) {
            Text(if (isRtl) "ابدأ صفقة جديدة من السوق" else "Start a deal from Market", color = BackgroundDark)
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
      ) {
        items(filteredDeals) { deal ->
          DealCardItem(
            deal = deal,
            isRtl = isRtl,
            onClick = { onSelectDeal(deal) }
          )
        }
      }
    }
  }
}

@Composable
fun DealCardItem(
  deal: SmartDeal,
  isRtl: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("deal_item_${deal.dealId}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(
                if (deal.tradeType == TradeType.BUY) EmeraldPrimary.copy(alpha = 0.15f) else StatusError.copy(alpha = 0.15f)
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (deal.tradeType == TradeType.BUY) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
              contentDescription = null,
              tint = if (deal.tradeType == TradeType.BUY) EmeraldPrimary else StatusError,
              modifier = Modifier.size(18.dp)
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "${deal.tradeType.labelAr} ${deal.cryptoAmount} ${deal.assetSymbol}",
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.titleSmall
            )
            Text(
              text = "${if (isRtl) "مع:" else "With:"} ${deal.counterpartyName}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Status Tag
        Surface(
          color = when (deal.status) {
            EscrowStatus.LOCKED_IN_ESCROW -> GoldAccent.copy(alpha = 0.15f)
            EscrowStatus.PAID_PENDING_CONFIRMATION -> CyberBlue.copy(alpha = 0.15f)
            EscrowStatus.RELEASED_COMPLETED -> StatusSuccess.copy(alpha = 0.15f)
            EscrowStatus.DISPUTED -> StatusError.copy(alpha = 0.15f)
            EscrowStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant
          },
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = if (isRtl) deal.status.labelAr else deal.status.labelEn,
            color = when (deal.status) {
              EscrowStatus.LOCKED_IN_ESCROW -> GoldAccent
              EscrowStatus.PAID_PENDING_CONFIRMATION -> CyberBlue
              EscrowStatus.RELEASED_COMPLETED -> StatusSuccess
              EscrowStatus.DISPUTED -> StatusError
              EscrowStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = if (isRtl) "المبلغ المطلوب" else "Fiat Total",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "${String.format("%,.2f", deal.fiatAmount)} ${deal.fiatCode}",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "رسوم ثابتة: $${String.format("%.2f", deal.fixedFeeUsd)}",
            fontSize = 11.sp,
            color = GoldAccent,
            fontWeight = FontWeight.Bold
          )
          Icon(
            imageVector = if (isRtl) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
            contentDescription = "Details",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}
