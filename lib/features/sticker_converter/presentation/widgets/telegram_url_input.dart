import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_text_styles.dart';

class TelegramUrlInput extends StatefulWidget {
  final TextEditingController controller;
  final Function(String) onUrlChanged;

  const TelegramUrlInput({
    super.key,
    required this.controller,
    required this.onUrlChanged,
  });

  @override
  State<TelegramUrlInput> createState() => _TelegramUrlInputState();
}

class _TelegramUrlInputState extends State<TelegramUrlInput>
    with SingleTickerProviderStateMixin {
  late AnimationController _animationController;
  late Animation<double> _fadeAnimation;
  bool _isValidUrl = false;
  String? _packName;

  @override
  void initState() {
    super.initState();
    _animationController = AnimationController(
      duration: const Duration(milliseconds: 300),
      vsync: this,
    );
    _fadeAnimation = Tween<double>(
      begin: 0.0,
      end: 1.0,
    ).animate(_animationController);
    
    widget.controller.addListener(_onUrlChanged);
  }

  @override
  void dispose() {
    widget.controller.removeListener(_onUrlChanged);
    _animationController.dispose();
    super.dispose();
  }

  void _onUrlChanged() {
    final url = widget.controller.text;
    final isValid = _validateTelegramUrl(url);
    
    setState(() {
      _isValidUrl = isValid;
      if (isValid) {
        _packName = _extractPackName(url);
        _animationController.forward();
      } else {
        _packName = null;
        _animationController.reverse();
      }
    });
    
    widget.onUrlChanged(url);
  }

  bool _validateTelegramUrl(String url) {
    if (url.isEmpty) return false;
    
    try {
      final uri = Uri.parse(url);
      return uri.host == 't.me' && 
             uri.pathSegments.isNotEmpty && 
             uri.pathSegments[0] == 'addstickers' &&
             uri.pathSegments.length > 1;
    } catch (e) {
      return false;
    }
  }

  String? _extractPackName(String url) {
    try {
      final uri = Uri.parse(url);
      if (uri.pathSegments.length > 1) {
        return uri.pathSegments[1];
      }
    } catch (e) {
      // Ignore error
    }
    return null;
  }

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildHeader(),
          const SizedBox(height: 32),
          _buildUrlInput(),
          const SizedBox(height: 16),
          _buildValidationInfo(),
        ],
      ),
    );
  }

  Widget _buildHeader() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Telegram Sticker Pack',
          style: AppTextStyles.h4.copyWith(
            fontWeight: FontWeight.bold,
            color: AppColors.onBackground,
          ),
        ),
        const SizedBox(height: 8),
        Text(
          'Paste the URL of a public Telegram sticker pack to begin.',
          style: AppTextStyles.bodyLarge.copyWith(
            color: AppColors.onSurfaceVariant,
          ),
        ),
      ],
    );
  }

  Widget _buildUrlInput() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Sticker Pack URL',
          style: AppTextStyles.labelLarge.copyWith(
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 8),
        TextField(
          controller: widget.controller,
          decoration: InputDecoration(
            hintText: 'https://t.me/addstickers/pack_name',
            prefixIcon: const Icon(Icons.link, color: AppColors.onSurfaceVariant),
            suffixIcon: _isValidUrl
                ? const Icon(
                    Icons.check_circle,
                    color: AppColors.success,
                  )
                : widget.controller.text.isNotEmpty
                    ? const Icon(
                        Icons.error_outline,
                        color: AppColors.error,
                      )
                    : null,
          ),
          keyboardType: TextInputType.url,
          textInputAction: TextInputAction.done,
        ),
      ],
    );
  }

  Widget _buildValidationInfo() {
    return AnimatedBuilder(
      animation: _fadeAnimation,
      builder: (context, child) {
        return Opacity(
          opacity: _fadeAnimation.value,
          child: _isValidUrl && _packName != null
              ? _buildInfoCard(
                  icon: Icons.check_circle_outline,
                  iconColor: AppColors.success,
                  title: 'URL is Valid',
                  subtitle: 'Ready to process pack: $_packName',
                )
              : widget.controller.text.isNotEmpty && !_isValidUrl
                  ? _buildInfoCard(
                      icon: Icons.error_outline,
                      iconColor: AppColors.error,
                      title: 'Invalid URL Format',
                      subtitle: 'Please check the URL and try again.',
                    )
                  : const SizedBox.shrink(),
        );
      },
    );
  }

  Widget _buildInfoCard({
    required IconData icon,
    required Color iconColor,
    required String title,
    required String subtitle,
  }) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: iconColor.withOpacity(0.05),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: iconColor.withOpacity(0.2),
        ),
      ),
      child: Row(
        children: [
          Icon(
            icon,
            color: iconColor,
            size: 24,
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: AppTextStyles.labelLarge.copyWith(
                    color: AppColors.onSurface,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  subtitle,
                  style: AppTextStyles.bodyMedium.copyWith(
                    color: AppColors.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildExampleUrl(String url) {
    return GestureDetector(
      onTap: () {
        widget.controller.text = url;
      },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          color: AppColors.surface.withOpacity(0.5),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: AppColors.outline.withOpacity(0.2)),
        ),
        child: Row(
          children: [
            Icon(
              Icons.content_copy,
              size: 18,
              color: AppColors.onSurfaceVariant,
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                url,
                style: AppTextStyles.bodyMedium.copyWith(
                  color: AppColors.onSurfaceVariant,
                  fontFamily: 'monospace',
                  letterSpacing: 0.8,
                ),
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
