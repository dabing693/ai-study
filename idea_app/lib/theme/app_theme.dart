import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class AppTheme {
  static ThemeData get lightTheme {
    return ThemeData(
      useMaterial3: true,
      colorScheme: const ColorScheme(
        brightness: Brightness.light,
        primary: Color(0xFF6750A4),
        onPrimary: Color(0xFFFFFFFF),
        primaryContainer: Color(0xFFEADDFF),
        onPrimaryContainer: Color(0xFF21005D),
        secondary: Color(0xFF625B71),
        onSecondary: Color(0xFFFFFFFF),
        secondaryContainer: Color(0xFFE8DEF8),
        onSecondaryContainer: Color(0xFF1D192B),
        tertiary: Color(0xFF7D5260),
        onTertiary: Color(0xFFFFFFFF),
        tertiaryContainer: Color(0xFFFFD8E4),
        onTertiaryContainer: Color(0xFF31111D),
        error: Color(0xFFB3261E),
        onError: Color(0xFFFFFFFF),
        errorContainer: Color(0xFFF9DEDC),
        onErrorContainer: Color(0xFF410E0B),
        surface: Color(0xFFFEF7FF),
        onSurface: Color(0xFF1D1B20),
        surfaceContainerHighest: Color(0xFFE7E0EC), // surface-variant
        onSurfaceVariant: Color(0xFF49454F),
        outline: Color(0xFF79747E),
        outlineVariant: Color(0xFFCAC4D0),
      ),
      scaffoldBackgroundColor: const Color(0xFFFEF7FF),
      fontFamily: GoogleFonts.roboto().fontFamily,
      textTheme: GoogleFonts.robotoTextTheme().copyWith(
        titleLarge: GoogleFonts.roboto(fontWeight: FontWeight.w500),
        titleMedium: GoogleFonts.roboto(fontWeight: FontWeight.w500),
        bodyMedium: GoogleFonts.notoSansSc(), // Support for Chinese
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: Colors.transparent,
        elevation: 0,
        surfaceTintColor: Colors.transparent,
      ),
    );
  }
}
