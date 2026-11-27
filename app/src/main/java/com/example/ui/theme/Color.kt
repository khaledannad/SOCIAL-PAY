package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ============================================================================
// SOCIAL PAY — UNIFIED GLOBAL SMART BRAND PALETTE
// ============================================================================

val BrandPrimaryBg = Color(0xFF071923)          // #071923 Obsidian Navy Deep
val BrandSecondaryBg = Color(0xFF102A36)        // #102A36 Slate Petrol Surface
val BrandSurfaceVariant = Color(0xFF163442)     // Elevated surface layer
val BrandSurfaceBorder = Color(0xFF1F4355)      // Elegant boundary stroke

val BrandPrimaryAccent = Color(0xFF38E6D0)      // #38E6D0 Luminous Turquoise
val BrandSecondaryAccent = Color(0xFF35A9F5)    // #35A9F5 Electric Cobalt Blue
val BrandIntelligenceAccent = Color(0xFF9478FF) // #9478FF Genius AI Violet

val BrandTextPrimary = Color(0xFFFFFFFF)        // #FFFFFF Pure White
val BrandTextSecondary = Color(0xFFA9BEC8)      // #A9BEC8 Cool Platinum Slate
val BrandTextMuted = Color(0xFF6B8694)          // Soft Subtitle

// Unified Signature Brand Gradients
val BrandGradient = Brush.linearGradient(
  colors = listOf(BrandPrimaryAccent, BrandSecondaryAccent, BrandIntelligenceAccent)
)

val BrandDualGradient = Brush.linearGradient(
  colors = listOf(BrandPrimaryAccent, BrandSecondaryAccent)
)

val BrandVioletGradient = Brush.linearGradient(
  colors = listOf(BrandSecondaryAccent, BrandIntelligenceAccent)
)

val BrandSurfaceGradient = Brush.verticalGradient(
  colors = listOf(BrandSecondaryBg, BrandPrimaryBg)
)

// Status & Security Semantics
val StatusSuccess = Color(0xFF38E6D0)           // Connected & Confirmed (Turquoise)
val StatusWarning = Color(0xFFF5A623)           // Escrow / Pending Review (Amber)
val StatusError = Color(0xFFFF5252)             // Rejected / Phishing Alert (Crimson)
val StatusInfo = Color(0xFF35A9F5)              // Notice / Feed latency (Blue)
val EscrowShield = Color(0xFF35A9F5)            // Smart Escrow Trust (Cobalt)

// ============================================================================
// BACKWARD-COMPATIBILITY SYSTEM TOKENS (Mapped to New Global Brand)
// ============================================================================
val EmeraldPrimary = BrandPrimaryAccent
val EmeraldLight = Color(0xFF6CF0DF)
val EmeraldDark = Color(0xFF1CB5A2)

val GoldAccent = BrandSecondaryAccent
val GoldLight = Color(0xFF72C4FA)

val CyberBlue = BrandSecondaryAccent
val WhatsAppGreen = BrandPrimaryAccent
val PurpleNeon = BrandIntelligenceAccent

val BackgroundDark = BrandPrimaryBg
val SurfaceDark = BrandSecondaryBg
val SurfaceVariantDark = BrandSurfaceVariant
val SurfaceBorderDark = BrandSurfaceBorder

val TextPrimaryDark = BrandTextPrimary
val TextSecondaryDark = BrandTextSecondary
val TextMutedDark = BrandTextMuted
