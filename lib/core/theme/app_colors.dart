import 'package:flutter/material.dart';

class AppColors {
  // Primary Colors - Modern vibrant purple/blue
  static const Color primary = Color(0xFF7C4DFF);
  static const Color primaryDark = Color(0xFF5E35FF);
  static const Color primaryLight = Color(0xFF9575FF);

  // Secondary Colors - Vibrant cyan accent
  static const Color secondary = Color(0xFF00BCD4);
  static const Color secondaryDark = Color(0xFF00ACC1);
  static const Color secondaryLight = Color(0xFF26C6DA);

  // Dark Background Colors
  static const Color background = Color(0xFF0A0A0A); // Deep black
  static const Color surface = Color(0xFF1A1A1A); // Dark grey
  static const Color surfaceVariant = Color(0xFF2A2A2A); // Lighter dark grey

  // Text Colors for dark theme
  static const Color onPrimary = Color(0xFFFFFFFF);
  static const Color onSecondary = Color(0xFF000000);
  static const Color onBackground = Color(0xFFFFFFFF); // White text on dark bg
  static const Color onSurface = Color(0xFFE5E5E5); // Light grey text
  static const Color onSurfaceVariant = Color(0xFFB0B0B0); // Medium grey text

  // Border Colors
  static const Color outline = Color(0xFF404040); // Dark border

  // Status Colors
  static const Color success = Color(0xFF4CAF50);
  static const Color warning = Color(0xFFFF9800);
  static const Color error = Color(0xFFF44336);
  static const Color info = Color(0xFF2196F3);

  // Progress Colors
  static const Color progressBackground = Color(0xFF333333);
  static const Color progressForeground = Color(0xFF7C4DFF);

  // Shimmer Colors for dark theme
  static const Color shimmerBase = Color(0xFF2A2A2A);
  static const Color shimmerHighlight = Color(0xFF3A3A3A);
  
  // Additional surface colors
  static const Color surfaceContainerHigh = Color(0xFF3A3A3A);
  static const Color shadowColor = Color(0xFF000000);

  // Gradient Colors
  static const List<Color> primaryGradient = [
    primary,
    Color(0xFF9C27B0), // Purple accent
  ];

  static const List<Color> backgroundGradient = [
    Color(0xFF0A0A0A),
    Color(0xFF1A1A1A),
  ];
}
