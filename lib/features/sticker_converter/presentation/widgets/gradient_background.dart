import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';

class GradientBackground extends StatelessWidget {
  final Widget child;
  final List<Color>? colors;
  final AlignmentGeometry? begin;
  final AlignmentGeometry? end;

  const GradientBackground({
    super.key,
    required this.child,
    this.colors,
    this.begin,
    this.end,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: colors ?? AppColors.backgroundGradient,
          begin: begin ?? Alignment.topCenter,
          end: end ?? Alignment.bottomCenter,
        ),
      ),
      child: child,
    );
  }
}
