import 'package:flutter/material.dart';

class AppTheme {
  static const Color primary = Color(0xFF8A9A5B);
  static const Color background = Color(0xFFF5F5DC);
  static const Color textDark = Color(0xFF333333);
  static const Color accent = Color(0xFF5C6B3E);
  static const Color surface = Color(0xFFECECCC);
  static const Color disabled = Color(0xFFBBBBBB);

  static ThemeData get theme => ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: primary,
          surface: surface,
        ),
        scaffoldBackgroundColor: background,
        textTheme: const TextTheme(
          bodyLarge: TextStyle(color: textDark),
          bodyMedium: TextStyle(color: textDark),
        ),
        elevatedButtonTheme: ElevatedButtonThemeData(
          style: ElevatedButton.styleFrom(
            backgroundColor: primary,
            foregroundColor: Colors.white,
            minimumSize: const Size(64, 48),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.all(Radius.circular(12)),
            ),
          ),
        ),
        outlinedButtonTheme: OutlinedButtonThemeData(
          style: OutlinedButton.styleFrom(
            foregroundColor: accent,
            side: BorderSide(color: primary),
            minimumSize: const Size(48, 48),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.all(Radius.circular(12)),
            ),
          ),
        ),
        useMaterial3: true,
      );
}
