import 'package:flutter/material.dart';

/// iOS / Apple Human Interface Guideline color palette
class AppColors {
  AppColors._();

  // iOS System Colors
  static const Color systemBlue   = Color(0xFF007AFF);
  static const Color systemGreen  = Color(0xFF34C759);
  static const Color systemRed    = Color(0xFFFF3B30);
  static const Color systemOrange = Color(0xFFFF9500);
  static const Color systemYellow = Color(0xFFFFCC00);
  static const Color systemPurple = Color(0xFFAF52DE);
  static const Color systemTeal   = Color(0xFF5AC8FA);
  static const Color systemIndigo = Color(0xFF5856D6);

  // Primary (iOS Green — money / success)
  static const Color primary      = systemGreen;
  static const Color primaryDark  = Color(0xFF248A3D); // darker green for dark mode

  // Expense amount color
  static const Color expense      = systemRed;

  // iOS Background (Light)
  static const Color bgPrimary    = Color(0xFFFFFFFF); // primary grouped
  static const Color bgSecondary  = Color(0xFFF2F2F7); // secondary grouped
  static const Color bgTertiary   = Color(0xFFE5E5EA); // tertiary

  // iOS Background (Dark)
  static const Color bgPrimaryDark   = Color(0xFF1C1C1E);
  static const Color bgSecondaryDark = Color(0xFF2C2C2E);
  static const Color bgTertiaryDark  = Color(0xFF3A3A3C);

  // iOS Label Colors (Light)
  static const Color labelPrimary   = Color(0xFF000000);
  static const Color labelSecondary = Color(0xFF8E8E93);
  static const Color labelTertiary  = Color(0xFFC7C7CC);
  static const Color separator      = Color(0xFFE5E5EA);

  // iOS Label Colors (Dark)
  static const Color labelPrimaryDark   = Color(0xFFFFFFFF);
  static const Color labelSecondaryDark = Color(0xFF8E8E93);
  static const Color separatorDark      = Color(0xFF38383A);

  // Calculator keyboard (iOS Calculator style)
  static const Color keyDigitLight  = Color(0xFFFFFFFF);
  static const Color keyDigitDark   = Color(0xFF333333);
  static const Color keyActionLight = Color(0xFFD4D4D2); // gray keys
  static const Color keyActionDark  = Color(0xFF636366);
  static const Color keyConfirm     = systemGreen;

  // Chart colors (iOS-style vibrant palette)
  static const List<Color> chartColors = [
    systemGreen,
    systemBlue,
    systemOrange,
    systemPurple,
    systemRed,
    systemTeal,
    systemYellow,
    systemIndigo,
    Color(0xFF30D158), // mint
    Color(0xFFFF6961), // salmon
  ];
}
