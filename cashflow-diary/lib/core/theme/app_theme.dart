import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'app_colors.dart';

class AppTheme {
  AppTheme._();

  static ThemeData get lightTheme {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.light,
      colorScheme: ColorScheme.fromSeed(
        seedColor: AppColors.primary,
        brightness: Brightness.light,
        surface: AppColors.bgPrimary,
        surfaceContainerHighest: AppColors.bgSecondary,
      ),
      scaffoldBackgroundColor: AppColors.bgSecondary,
      fontFamily: '.SF Pro Text',
      appBarTheme: const AppBarTheme(
        centerTitle: true,
        elevation: 0,
        scrolledUnderElevation: 0,
        backgroundColor: AppColors.bgSecondary,
        foregroundColor: AppColors.labelPrimary,
        systemOverlayStyle: SystemUiOverlayStyle.dark,
        titleTextStyle: TextStyle(
          fontFamily: '.SF Pro Display',
          fontSize: 17,
          fontWeight: FontWeight.w600,
          color: AppColors.labelPrimary,
          letterSpacing: -0.4,
        ),
      ),
      cardTheme: CardThemeData(
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
        ),
        color: AppColors.bgPrimary,
      ),
      listTileTheme: ListTileThemeData(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        tileColor: AppColors.bgPrimary,
      ),
      floatingActionButtonTheme: const FloatingActionButtonThemeData(
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        elevation: 2,
        shape: CircleBorder(),
      ),
      bottomNavigationBarTheme: const BottomNavigationBarThemeData(
        type: BottomNavigationBarType.fixed,
        selectedItemColor: AppColors.primary,
        unselectedItemColor: AppColors.labelSecondary,
        backgroundColor: AppColors.bgPrimary,
        elevation: 0,
        selectedLabelStyle: TextStyle(fontSize: 10, fontWeight: FontWeight.w500),
        unselectedLabelStyle: TextStyle(fontSize: 10),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: AppColors.bgPrimary,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: AppColors.separator, width: 0.5),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: AppColors.separator, width: 0.5),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: AppColors.systemBlue, width: 1.5),
        ),
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        labelStyle: const TextStyle(color: AppColors.labelSecondary),
        hintStyle: const TextStyle(color: AppColors.labelTertiary),
      ),
      dividerTheme: const DividerThemeData(
        space: 0,
        thickness: 0.5,
        color: AppColors.separator,
        indent: 16,
      ),
      chipTheme: ChipThemeData(
        backgroundColor: AppColors.bgTertiary,
        selectedColor: AppColors.primary.withValues(alpha: 0.15),
        labelStyle: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500),
        side: BorderSide.none,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      ),
      textTheme: const TextTheme(
        displayLarge:  TextStyle(fontSize: 34, fontWeight: FontWeight.w700, letterSpacing: 0.37),
        displayMedium: TextStyle(fontSize: 28, fontWeight: FontWeight.w700, letterSpacing: 0.34),
        headlineLarge: TextStyle(fontSize: 28, fontWeight: FontWeight.w700, letterSpacing: 0.34),
        headlineMedium:TextStyle(fontSize: 22, fontWeight: FontWeight.w600, letterSpacing: 0.35),
        headlineSmall: TextStyle(fontSize: 20, fontWeight: FontWeight.w600, letterSpacing: 0.38),
        titleLarge:    TextStyle(fontSize: 17, fontWeight: FontWeight.w600, letterSpacing: -0.4),
        titleMedium:   TextStyle(fontSize: 16, fontWeight: FontWeight.w500, letterSpacing: -0.3),
        titleSmall:    TextStyle(fontSize: 13, fontWeight: FontWeight.w500, letterSpacing: -0.1),
        bodyLarge:     TextStyle(fontSize: 17, fontWeight: FontWeight.w400, letterSpacing: -0.4),
        bodyMedium:    TextStyle(fontSize: 15, fontWeight: FontWeight.w400, letterSpacing: -0.2),
        bodySmall:     TextStyle(fontSize: 13, fontWeight: FontWeight.w400, letterSpacing: -0.1),
        labelLarge:    TextStyle(fontSize: 13, fontWeight: FontWeight.w500, letterSpacing: -0.1),
        labelMedium:   TextStyle(fontSize: 11, fontWeight: FontWeight.w400),
        labelSmall:    TextStyle(fontSize: 10, fontWeight: FontWeight.w400),
      ),
    );
  }

  static ThemeData get darkTheme {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      colorScheme: ColorScheme.fromSeed(
        seedColor: AppColors.primary,
        brightness: Brightness.dark,
        surface: AppColors.bgPrimaryDark,
        surfaceContainerHighest: AppColors.bgSecondaryDark,
      ),
      scaffoldBackgroundColor: AppColors.bgPrimaryDark,
      fontFamily: '.SF Pro Text',
      appBarTheme: const AppBarTheme(
        centerTitle: true,
        elevation: 0,
        scrolledUnderElevation: 0,
        backgroundColor: AppColors.bgPrimaryDark,
        foregroundColor: AppColors.labelPrimaryDark,
        systemOverlayStyle: SystemUiOverlayStyle.light,
        titleTextStyle: TextStyle(
          fontFamily: '.SF Pro Display',
          fontSize: 17,
          fontWeight: FontWeight.w600,
          color: AppColors.labelPrimaryDark,
          letterSpacing: -0.4,
        ),
      ),
      cardTheme: CardThemeData(
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        color: AppColors.bgSecondaryDark,
      ),
      listTileTheme: ListTileThemeData(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        tileColor: AppColors.bgSecondaryDark,
      ),
      floatingActionButtonTheme: const FloatingActionButtonThemeData(
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        elevation: 2,
        shape: CircleBorder(),
      ),
      bottomNavigationBarTheme: const BottomNavigationBarThemeData(
        type: BottomNavigationBarType.fixed,
        selectedItemColor: AppColors.primary,
        unselectedItemColor: AppColors.labelSecondaryDark,
        backgroundColor: AppColors.bgSecondaryDark,
        elevation: 0,
        selectedLabelStyle: TextStyle(fontSize: 10, fontWeight: FontWeight.w500),
        unselectedLabelStyle: TextStyle(fontSize: 10),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: AppColors.bgTertiaryDark,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: AppColors.separatorDark, width: 0.5),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: AppColors.separatorDark, width: 0.5),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: AppColors.systemBlue, width: 1.5),
        ),
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        labelStyle: const TextStyle(color: AppColors.labelSecondaryDark),
      ),
      dividerTheme: const DividerThemeData(
        space: 0,
        thickness: 0.5,
        color: AppColors.separatorDark,
        indent: 16,
      ),
      chipTheme: ChipThemeData(
        backgroundColor: AppColors.bgTertiaryDark,
        selectedColor: AppColors.primary.withValues(alpha: 0.25),
        labelStyle: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500),
        side: BorderSide.none,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      ),
      textTheme: const TextTheme(
        displayLarge:  TextStyle(fontSize: 34, fontWeight: FontWeight.w700, letterSpacing: 0.37),
        displayMedium: TextStyle(fontSize: 28, fontWeight: FontWeight.w700, letterSpacing: 0.34),
        headlineLarge: TextStyle(fontSize: 28, fontWeight: FontWeight.w700, letterSpacing: 0.34),
        headlineMedium:TextStyle(fontSize: 22, fontWeight: FontWeight.w600, letterSpacing: 0.35),
        headlineSmall: TextStyle(fontSize: 20, fontWeight: FontWeight.w600, letterSpacing: 0.38),
        titleLarge:    TextStyle(fontSize: 17, fontWeight: FontWeight.w600, letterSpacing: -0.4),
        titleMedium:   TextStyle(fontSize: 16, fontWeight: FontWeight.w500, letterSpacing: -0.3),
        titleSmall:    TextStyle(fontSize: 13, fontWeight: FontWeight.w500, letterSpacing: -0.1),
        bodyLarge:     TextStyle(fontSize: 17, fontWeight: FontWeight.w400, letterSpacing: -0.4),
        bodyMedium:    TextStyle(fontSize: 15, fontWeight: FontWeight.w400, letterSpacing: -0.2),
        bodySmall:     TextStyle(fontSize: 13, fontWeight: FontWeight.w400, letterSpacing: -0.1),
        labelLarge:    TextStyle(fontSize: 13, fontWeight: FontWeight.w500, letterSpacing: -0.1),
        labelMedium:   TextStyle(fontSize: 11, fontWeight: FontWeight.w400),
        labelSmall:    TextStyle(fontSize: 10, fontWeight: FontWeight.w400),
      ),
    );
  }
}
