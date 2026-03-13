import 'package:flutter/material.dart';

/// Premium Dark theme colors - VapeStore Dashboard Design
class AppColors {
  // Background
  static const Color backgroundDark = Color(0xFF0F1115);
  static const Color surfaceDark = Color(0xFF151922);
  static const Color surfaceElevatedDark = Color(0xFF1B2030);

  // Pastel accents (KPI cards)
  static const Color pastelMint = Color(0xFFBFE7E5);
  static const Color pastelBlue = Color(0xFFCFE6F2);
  static const Color pastelLavender = Color(0xFFDED8F6);
  static const Color pastelPink = Color(0xFFF2D6DE);
  static const Color pastelTeal = Color(0xFFA5D4D2);

  // Text on dark
  static const Color textPrimaryDark = Color(0xFFF5F5F7);
  static const Color textSecondaryDark = Color(0xFF9CA3AF);
  static const Color textTertiaryDark = Color(0xFF6B7280);

  // Text on pastel
  static const Color textOnPastel = Color(0xFF111111);

  // Accents
  static const Color accentPrimary = pastelMint;
  static const Color accentError = Color(0xFFF85149);
  static const Color accentWarning = Color(0xFFD29922);
  static const Color accentInfo = pastelBlue;

  // Borders
  static const Color borderDark = Color(0xFF2A3648);
  static const Color borderSubtleDark = Color(0x14FFFFFF);

  // Semantic containers
  static const Color primaryContainerDark = Color(0xFF1B3540);
  static const Color successContainerDark = Color(0xFF0F2A1A);
  static const Color warningContainerDark = Color(0xFF3A2A00);
  static const Color infoContainerDark = Color(0xFF0B223A);

  // Light theme
  static const Color backgroundLight = Color(0xFFF6F8FA);
  static const Color surfaceLight = Color(0xFFFFFFFF);
  static const Color textPrimaryLight = Color(0xFF1F2328);
  static const Color textSecondaryLight = Color(0xFF57606A);
  static const Color borderLight = Color(0xFFD0D7DE);
}

/// KPI card color variants
enum KpiCardColor {
  mint(AppColors.pastelMint, AppColors.textOnPastel),
  blue(AppColors.pastelBlue, AppColors.textOnPastel),
  lavender(AppColors.pastelLavender, AppColors.textOnPastel),
  pink(AppColors.pastelPink, AppColors.textOnPastel),
  teal(AppColors.pastelTeal, AppColors.textOnPastel);

  const KpiCardColor(this.bg, this.textColor);
  final Color bg;
  final Color textColor;
}
