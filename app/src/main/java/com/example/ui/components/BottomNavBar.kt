package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.governance.tr
import com.example.model.AppLanguage
import com.example.ui.theme.*

enum class P2PNavigationItem(
  val titleAr: String,
  val titleEn: String,
  val titleFr: String,
  val icon: ImageVector,
  val testTag: String
) {
  HOME("الرئيسية", "Home", "Accueil", Icons.Default.Home, "nav_home_tab"),
  STABLECOINS("المستقرة", "Stablecoins", "Stablecoins", Icons.Default.VerifiedUser, "nav_market_tab"),
  CREATE("طلب دفع", "Send/Link", "Envoyer", Icons.Default.AddCircle, "nav_create_tab"),
  TRANSACTIONS("السجل", "Ledger", "Registre", Icons.Default.History, "nav_transactions_tab"),
  MERCHANT("التجار", "Business", "Entreprise", Icons.Default.Storefront, "nav_merchant_tab")
}

@Composable
fun BottomNavBar(
  currentLanguage: AppLanguage,
  selectedItem: P2PNavigationItem,
  onItemSelected: (P2PNavigationItem) -> Unit,
  activeTransactionsCount: Int = 0,
  modifier: Modifier = Modifier
) {
  Surface(
    color = BrandSecondaryBg,
    border = BorderStroke(1.dp, BrandSurfaceBorder),
    modifier = modifier.fillMaxWidth()
  ) {
    NavigationBar(
      modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
      containerColor = BrandSecondaryBg,
      tonalElevation = 0.dp
    ) {
      P2PNavigationItem.entries.forEach { item ->
        val isSelected = item == selectedItem
        val labelText = tr(currentLanguage, item.titleAr, item.titleEn, item.titleFr)
        NavigationBarItem(
          selected = isSelected,
          onClick = { onItemSelected(item) },
          modifier = Modifier.testTag(item.testTag),
          icon = {
            BadgedBox(
              badge = {
                if (item == P2PNavigationItem.TRANSACTIONS && activeTransactionsCount > 0) {
                  Badge(
                    containerColor = BrandPrimaryAccent
                  ) {
                    Text(
                      text = activeTransactionsCount.toString(),
                      color = BrandPrimaryBg,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            ) {
              Icon(
                imageVector = item.icon,
                contentDescription = labelText,
                modifier = Modifier.size(20.dp)
              )
            }
          },
          label = {
            Text(
              text = labelText,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = BrandPrimaryAccent,
            selectedTextColor = BrandPrimaryAccent,
            indicatorColor = BrandPrimaryAccent.copy(alpha = 0.15f),
            unselectedIconColor = BrandTextSecondary,
            unselectedTextColor = BrandTextSecondary
          )
        )
      }
    }
  }
}
