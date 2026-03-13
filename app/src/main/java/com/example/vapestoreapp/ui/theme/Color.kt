package com.example.vapestoreapp.ui.theme

import androidx.compose.ui.graphics.Color

// === ТЁМНАЯ ТЕМА (высокий контраст для читаемости) ===

// Фоны
val BackgroundDark = Color(0xFF0B0F14)      // Глубокий фон (премиальнее, чуть мягче)
val SurfaceDark = Color(0xFF111821)        // Карточки и поверхности
val SurfaceVariantDark = Color(0xFF182231) // Вариант поверхности / панели
val CardElevatedDark = Color(0xFF151E2A)   // Приподнятые карточки (мягкий подъём)
val SurfaceElevatedDark = Color(0xFF1D2A3A) // Ещё один слой для акцентных панелей

// Текст — максимальная читаемость
val TextPrimaryDark = Color(0xFFFFFFFF)    // Основной текст (белый)
val TextSecondaryDark = Color(0xFFB1BAC4)  // Вторичный текст (светло-серый)
val TextTertiaryDark = Color(0xFF8B949E)  // Третичный текст
val TextMutedDark = Color(0xFF6E7681)      // Приглушённый текст

// Акценты
val AccentPrimary = Color(0xFF2EA043)      // Изумрудный — успех, деньги, основной акцент
val AccentPrimaryVariant = Color(0xFF3FB950) // Светлее
val AccentSecondary = Color(0xFF58A6FF)    // Синий — ссылки, кнопки
val AccentError = Color(0xFFF85149)        // Красный — ошибки, удаление
val AccentWarning = Color(0xFFD29922)     // Жёлтый — предупреждения
val AccentInfo = AccentSecondary

// Границы и контуры
val BorderDark = Color(0xFF2A3648)
val BorderSubtleDark = Color(0xFF223044)

// Контейнеры акцентов (для карточек статусов / выделений)
val PrimaryContainerDark = Color(0xFF123021)        // тёмно‑изумрудный
val OnPrimaryContainerDark = Color(0xFFCFFFE0)
val SecondaryContainerDark = Color(0xFF0E2A44)      // тёмно‑синий
val OnSecondaryContainerDark = Color(0xFFD6E7FF)
val TertiaryDark = Color(0xFF8A5CF6)                // фиолетовый для “второго акцента”
val TertiaryContainerDark = Color(0xFF2A1F49)
val OnTertiaryContainerDark = Color(0xFFE9DDFF)

val ErrorContainerDark = Color(0xFF3A1616)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val SuccessDark = AccentPrimaryVariant
val SuccessContainerDark = Color(0xFF0F2A1A)
val OnSuccessContainerDark = Color(0xFFCFFFE0)
val WarningContainerDark = Color(0xFF3A2A00)
val OnWarningContainerDark = Color(0xFFFFE8B5)
val InfoContainerDark = Color(0xFF0B223A)
val OnInfoContainerDark = Color(0xFFCFE8FF)

// === СВЕТЛАЯ ТЕМА ===

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

// Легаси (для совместимости с Theme.kt)
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
