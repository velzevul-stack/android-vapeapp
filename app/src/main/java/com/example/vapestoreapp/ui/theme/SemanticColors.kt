package com.example.vapestoreapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppSemanticColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color
)

internal val LocalAppSemanticColors = staticCompositionLocalOf {
    AppSemanticColors(
        success = SuccessDark,
        onSuccess = Color.White,
        successContainer = SuccessContainerDark,
        onSuccessContainer = OnSuccessContainerDark,
        warning = AccentWarning,
        onWarning = Color.Black,
        warningContainer = WarningContainerDark,
        onWarningContainer = OnWarningContainerDark,
        info = AccentInfo,
        onInfo = Color.White,
        infoContainer = InfoContainerDark,
        onInfoContainer = OnInfoContainerDark
    )
}

val MaterialTheme.semanticColors: AppSemanticColors
    @Composable get() = LocalAppSemanticColors.current

