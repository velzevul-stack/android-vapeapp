package com.example.vapestoreapp.ui.theme

import androidx.compose.ui.graphics.Color

// === PREMIUM DARK THEME (VapeStore Dashboard Design) ===

// Background Colors
val BackgroundDark = Color(0xFF0F1115)       // --dark-bg, основной фон
val SurfaceDark = Color(0xFF151922)           // --dark-surface, карточки
val SurfaceElevatedDark = Color(0xFF1B2030)   // --dark-surface-elevated, приподнятые элементы
val SurfaceVariantDark = SurfaceElevatedDark  // алиас для MaterialTheme

// Pastel Accent Colors (для KPI карточек)
val PastelMint = Color(0xFFBFE7E5)          // --pastel-mint
val PastelBlue = Color(0xFFCFE6F2)           // --pastel-blue
val PastelLavender = Color(0xFFDED8F6)        // --pastel-lavender
val PastelPink = Color(0xFFF2D6DE)          // --pastel-pink
val PastelTeal = Color(0xFFA5D4D2)           // --chart-5, лучший день в графике

// Text on Dark backgrounds
val TextPrimaryDark = Color(0xFFF5F5F7)     // --text-dark-primary
val TextSecondaryDark = Color(0xFF9CA3AF)    // --text-dark-secondary
val TextTertiaryDark = Color(0xFF6B7280)     // --text-dark-tertiary

// Text on Pastel backgrounds
val TextOnPastel = Color(0xFF111111)         // --text-on-pastel
val TextOnPastelSecondary = Color(0xFF1A1A1A)

// Accents (для кнопок, активных состояний)
val AccentPrimary = PastelMint               // Основной акцент
val AccentPrimaryVariant = PastelTeal        // Вариант для hover/best
val AccentSecondary = PastelBlue             // Вторичный акцент
val AccentError = Color(0xFFF85149)         // Красный — ошибки
val AccentWarning = Color(0xFFD29922)        // Жёлтый — предупреждения
val AccentInfo = AccentSecondary

// Borders
val BorderDark = Color(0xFF2A3648)
val BorderSubtleDark = Color(0x14FFFFFF)    // rgba(255,255,255,0.08)

// Semantic containers (для совместимости)
val PrimaryContainerDark = Color(0xFF1B3540)
val OnPrimaryContainerDark = PastelMint
val SecondaryContainerDark = SurfaceElevatedDark
val OnSecondaryContainerDark = TextPrimaryDark
val TertiaryDark = PastelLavender
val TertiaryContainerDark = Color(0xFF2A2540)
val OnTertiaryContainerDark = PastelLavender

val ErrorContainerDark = Color(0xFF3A1616)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val SuccessDark = AccentPrimaryVariant
val SuccessContainerDark = Color(0xFF0F2A1A)
val OnSuccessContainerDark = PastelMint
val WarningContainerDark = Color(0xFF3A2A00)
val OnWarningContainerDark = Color(0xFFFFE8B5)
val InfoContainerDark = Color(0xFF0B223A)
val OnInfoContainerDark = PastelBlue

// === LIGHT THEME (fallback) ===

val BackgroundLight = Color(0xFFF6F8FA)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF0F3F6)
val TextPrimaryLight = Color(0xFF1F2328)
val TextSecondaryLight = Color(0xFF57606A)
val TextTertiaryLight = Color(0xFF6E7781)
val BorderLight = Color(0xFFD0D7DE)

val PrimaryContainerLight = Color(0xFFE0F2E6)
val OnPrimaryContainerLight = Color(0xFF0D2B17)
val SecondaryContainerLight = Color(0xFFD8E9FF)
val OnSecondaryContainerLight = Color(0xFF0A2236)
val TertiaryLight = Color(0xFF6F4BD8)
val TertiaryContainerLight = Color(0xFFE9DDFF)
val OnTertiaryContainerLight = Color(0xFF24114A)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)
val SuccessContainerLight = Color(0xFFDFF7E7)
val OnSuccessContainerLight = Color(0xFF0B2B16)
val WarningContainerLight = Color(0xFFFFF0C2)
val OnWarningContainerLight = Color(0xFF2A1E00)
val InfoContainerLight = Color(0xFFD8E9FF)
val OnInfoContainerLight = Color(0xFF0A2236)

// Legacy aliases
val Slate900 = BackgroundDark
val Slate800 = SurfaceDark
val Slate700 = BorderDark
val Slate400 = TextTertiaryDark
val Slate100 = TextPrimaryDark
val Indigo500 = AccentPrimary
val Indigo400 = AccentPrimaryVariant
val Teal500 = AccentPrimary
val Rose500 = AccentError
val Slate50 = BackgroundLight
val White = Color(0xFFFFFFFF)
val Slate200 = SurfaceVariantLight
val Slate600 = TextSecondaryLight
val Slate950 = TextPrimaryLight
