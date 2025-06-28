import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class AppTextStyles {
  // Base Text Style
  static final TextStyle _base = GoogleFonts.inter(
    fontWeight: FontWeight.normal,
    height: 1.5,
  );

  // Headings
  static final TextStyle h1 = _base.copyWith(fontSize: 36, fontWeight: FontWeight.bold, height: 1.2);
  static final TextStyle h2 = _base.copyWith(fontSize: 30, fontWeight: FontWeight.bold, height: 1.2);
  static final TextStyle h3 = _base.copyWith(fontSize: 24, fontWeight: FontWeight.w600, height: 1.3);
  static final TextStyle h4 = _base.copyWith(fontSize: 20, fontWeight: FontWeight.w600, height: 1.3);
  static final TextStyle h5 = _base.copyWith(fontSize: 18, fontWeight: FontWeight.w600, height: 1.4);
  static final TextStyle h6 = _base.copyWith(fontSize: 16, fontWeight: FontWeight.w600, height: 1.4);

  // Body Text
  static final TextStyle bodyLarge = _base.copyWith(fontSize: 16);
  static final TextStyle bodyMedium = _base.copyWith(fontSize: 14);
  static final TextStyle bodySmall = _base.copyWith(fontSize: 12);

  // Labels
  static final TextStyle labelLarge = _base.copyWith(fontSize: 14, fontWeight: FontWeight.w500);
  static final TextStyle labelMedium = _base.copyWith(fontSize: 12, fontWeight: FontWeight.w500);
  static final TextStyle labelSmall = _base.copyWith(fontSize: 11, fontWeight: FontWeight.w500);
  
  // Title styles
  static final TextStyle titleLarge = _base.copyWith(fontSize: 22, fontWeight: FontWeight.w600);
  static final TextStyle titleMedium = _base.copyWith(fontSize: 16, fontWeight: FontWeight.w600);
  static final TextStyle titleSmall = _base.copyWith(fontSize: 14, fontWeight: FontWeight.w600);

  // Special
  static final TextStyle button = GoogleFonts.inter(fontSize: 14, fontWeight: FontWeight.w600, height: 1.2);
  static final TextStyle caption = _base.copyWith(fontSize: 12);
  static final TextStyle overline = _base.copyWith(fontSize: 10, fontWeight: FontWeight.w500, letterSpacing: 1.5);
}
