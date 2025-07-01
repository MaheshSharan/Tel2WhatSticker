import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/router/app_router.dart';
import '../bloc/sticker_converter_bloc.dart';
import '../bloc/sticker_converter_event.dart';
import '../bloc/sticker_converter_state.dart';
import '../widgets/gradient_background.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> with TickerProviderStateMixin {
  late AnimationController _animationController;
  late Animation<double> _fadeAnimation;
  late Animation<Offset> _slideAnimation;
  
  @override
  void initState() {
    super.initState();
    _setupAnimations();
    _checkWhatsAppInstallation();
  }
  
  void _setupAnimations() {
    _animationController = AnimationController(
      duration: const Duration(milliseconds: 1500),
      vsync: this,
    );
    
    _fadeAnimation = Tween<double>(
      begin: 0.0,
      end: 1.0,
    ).animate(CurvedAnimation(
      parent: _animationController,
      curve: const Interval(0.0, 0.6, curve: Curves.easeOut),
    ));
    
    _slideAnimation = Tween<Offset>(
      begin: const Offset(0, 0.3),
      end: Offset.zero,
    ).animate(CurvedAnimation(
      parent: _animationController,
      curve: const Interval(0.2, 1.0, curve: Curves.elasticOut),
    ));
    
    _animationController.forward();
  }
  
  void _checkWhatsAppInstallation() {
    context.read<StickerConverterBloc>().add(
      const StickerConverterEvent.checkWhatsAppInstallation(),
    );
  }
  
  @override
  void dispose() {
    _animationController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: GradientBackground(
        child: SafeArea(
          child: BlocListener<StickerConverterBloc, StickerConverterState>(
            listener: (context, state) {
              state.maybeWhen(
                (isLoading, isWhatsAppInstalled, isProcessing, currentPack, processingProgress, validatedFiles, extractedDirectory, error, successMessage) => null,
                error: (message) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text(message),
                      backgroundColor: AppColors.error,
                    ),
                  );
                },
                orElse: () {},
              );
            },
            child: CustomScrollView(
              physics: const BouncingScrollPhysics(),
              slivers: [
                _buildAppBar(),
                SliverToBoxAdapter(
                  child: Column(
                    children: [
                      const SizedBox(height: 20), // Reduced from 40
                      _buildHeader(),
                      const SizedBox(height: 24), // Reduced from 40
                      _buildFeatureCards(),
                      const SizedBox(height: 40),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
  
  Widget _buildAppBar() {
    return SliverAppBar(
      expandedHeight: 80,
      floating: false,
      pinned: false,
      backgroundColor: Colors.transparent,
      elevation: 0,
      flexibleSpace: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [
              AppColors.background,
              AppColors.background.withOpacity(0.0),
            ],
          ),
        ),
      ),
      actions: [
        // WhatsApp Status Indicator
        BlocBuilder<StickerConverterBloc, StickerConverterState>(
          builder: (context, state) {
            final isInstalled = state.maybeWhen(
              (isLoading, isWhatsAppInstalled, isProcessing, currentPack, processingProgress, validatedFiles, extractedDirectory, error, successMessage) => isWhatsAppInstalled,
              whatsAppCheckCompleted: (isInstalled) => isInstalled,
              orElse: () => false,
            );

            final isChecking = state.maybeWhen(
              (isLoading, isWhatsAppInstalled, isProcessing, currentPack, processingProgress, validatedFiles, extractedDirectory, error, successMessage) => isLoading,
              orElse: () => false,
            );

            return Padding(
              padding: const EdgeInsets.only(right: 8.0),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  // WhatsApp Status Icon
                  GestureDetector(
                    onTap: () => _showWhatsAppStatusBottomSheet(context, isInstalled),
                    child: Container(
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(12),
                        color: AppColors.surface.withOpacity(0.8),
                        border: Border.all(
                          color: isInstalled 
                              ? AppColors.success.withOpacity(0.5)
                              : AppColors.warning.withOpacity(0.5),
                          width: 1,
                        ),
                      ),
                      child: Icon(
                        isInstalled ? Icons.check_circle : Icons.warning,
                        color: isInstalled ? AppColors.success : AppColors.warning,
                        size: 20,
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  // Refresh Button
                  GestureDetector(
                    onTap: isChecking ? null : _checkWhatsAppInstallation,
                    child: Container(
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(12),
                        color: AppColors.surface.withOpacity(0.8),
                        border: Border.all(
                          color: AppColors.outline.withOpacity(0.3),
                          width: 1,
                        ),
                      ),
                      child: isChecking
                          ? SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(
                                strokeWidth: 2,
                                valueColor: AlwaysStoppedAnimation<Color>(
                                  AppColors.primary,
                                ),
                              ),
                            )
                          : Icon(
                              Icons.refresh,
                              color: AppColors.onSurfaceVariant,
                              size: 20,
                            ),
                    ),
                  ),
                ],
              ),
            );
          },
        ),
        Padding(
          padding: const EdgeInsets.only(right: 16.0),
          child: IconButton(
            icon: Icon(
              Icons.info_outline_rounded,
              color: AppColors.onBackground.withOpacity(0.7),
              size: 24,
            ),
            onPressed: () => _showInfoDialog(),
          ),
        ),
      ],
    );
  }
  
  Widget _buildHeader() {
    return FadeTransition(
      opacity: _fadeAnimation,
      child: SlideTransition(
        position: _slideAnimation,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16), // Reduced from 32
          child: Column(
            children: [
              // Modern logo container
              Container(
                width: 70, // Reduced from 80
                height: 70, // Reduced from 80
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(20), // Reduced from 24
                  boxShadow: [
                    BoxShadow(
                      color: AppColors.primary.withOpacity(0.4),
                      blurRadius: 20,
                      spreadRadius: 2,
                      offset: const Offset(0, 8),
                    ),
                  ],
                ),
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(20), // Reduced from 24
                  child: Image.asset(
                    'assets/images/logos/logo_splash.png',
                    width: 70, // Reduced from 80
                    height: 70, // Reduced from 80
                    fit: BoxFit.cover,
                  ),
                ),
              ),
              const SizedBox(height: 20), // Reduced from 32
              // Single modern heading
              ShaderMask(
                shaderCallback: (bounds) => LinearGradient(
                  colors: AppColors.primaryGradient,
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ).createShader(bounds),
                child: Text(
                  'Sticker Converter',
                  style: GoogleFonts.inter(
                    fontSize: 32, // Reduced from 36
                    fontWeight: FontWeight.w900,
                    height: 1.1,
                    letterSpacing: -1.0,
                    color: Colors.white,
                  ),
                  textAlign: TextAlign.center,
                ),
              ),
              const SizedBox(height: 12), // Reduced from 16
              Text(
                'Transform Telegram stickers into WhatsApp format\nwith professional quality and ease',
                style: GoogleFonts.inter(
                  fontSize: 15, // Reduced from 16
                  fontWeight: FontWeight.w400,
                  height: 1.5,
                  color: AppColors.onSurfaceVariant,
                ),
                textAlign: TextAlign.center,
              ),
            ],
          ),
        ),
      ),
    );
  }
  
  Widget _buildFeatureCards() {
    return AnimatedBuilder(
      animation: _animationController,
      builder: (context, child) {
        return Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24),
          child: Column(
            children: [
              _buildFeatureCard(
                icon: Icons.image_rounded,
                title: 'Convert Images',
                description: 'Upload and convert your image files',
                color: const Color(0xFF6366F1),
                onTap: () => context.go('${AppRouter.upload}?type=images'),
                delay: 0.0,
              ),
              const SizedBox(height: 16),
              _buildFeatureCard(
                icon: Icons.telegram_rounded,
                title: 'Telegram Pack',
                description: 'Import directly from Telegram URL',
                color: const Color(0xFF06B6D4),
                onTap: () => context.go('${AppRouter.upload}?type=telegram'),
                delay: 0.2,
              ),
              const SizedBox(height: 16),
              _buildFeatureCard(
                icon: Icons.folder_zip_rounded,
                title: 'ZIP Archive',
                description: 'Extract from compressed files',
                color: const Color(0xFF8B5CF6),
                onTap: () => context.go('${AppRouter.upload}?type=zip'),
                delay: 0.4,
              ),
            ],
          ),
        );
      },
    );
  }
  
  Widget _buildFeatureCard({
    required IconData icon,
    required String title,
    required String description,
    required Color color,
    required VoidCallback onTap,
    required double delay,
  }) {
    final animation = Tween<double>(
      begin: 0.0,
      end: 1.0,
    ).animate(CurvedAnimation(
      parent: _animationController,
      curve: Interval(0.4 + delay, 1.0, curve: Curves.easeOutCubic),
    ));
    
    return FadeTransition(
      opacity: animation,
      child: SlideTransition(
        position: Tween<Offset>(
          begin: const Offset(0, 0.3),
          end: Offset.zero,
        ).animate(CurvedAnimation(
          parent: _animationController,
          curve: Interval(0.5 + delay, 1.0, curve: Curves.easeOutCubic),
        )),
        child: Container(
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(20),
            gradient: LinearGradient(
              colors: [
                AppColors.surface,
                AppColors.surface.withOpacity(0.8),
              ],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            border: Border.all(
              color: AppColors.outline.withOpacity(0.2),
              width: 1,
            ),
            boxShadow: [
              BoxShadow(
                color: color.withOpacity(0.1),
                blurRadius: 20,
                spreadRadius: 0,
                offset: const Offset(0, 8),
              ),
            ],
          ),
          child: Material(
            color: Colors.transparent,
            child: InkWell(
              onTap: onTap,
              borderRadius: BorderRadius.circular(20),
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Row(
                  children: [
                    Container(
                      width: 56,
                      height: 56,
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(16),
                        color: color.withOpacity(0.1),
                      ),
                      child: Icon(
                        icon,
                        color: color,
                        size: 28,
                      ),
                    ),
                    const SizedBox(width: 20),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            title,
                            style: GoogleFonts.inter(
                              fontSize: 18,
                              fontWeight: FontWeight.w600,
                              color: AppColors.onSurface,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            description,
                            style: GoogleFonts.inter(
                              fontSize: 14,
                              fontWeight: FontWeight.w400,
                              color: AppColors.onSurfaceVariant,
                            ),
                          ),
                        ],
                      ),
                    ),
                    Icon(
                      Icons.arrow_forward_ios_rounded,
                      color: AppColors.onSurfaceVariant.withOpacity(0.5),
                      size: 18,
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
  
  void _showInfoDialog() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: AppColors.surface,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(20),
        ),
        title: Text(
          'About this app',
          style: GoogleFonts.inter(
            fontSize: 20,
            fontWeight: FontWeight.w600,
            color: AppColors.onSurface,
          ),
        ),
        content: Text(
          'This app helps you convert images and Telegram sticker packs '
          'to WhatsApp format. All processing is done locally on your device '
          'for privacy and speed.',
          style: GoogleFonts.inter(
            fontSize: 14,
            fontWeight: FontWeight.w400,
            color: AppColors.onSurfaceVariant,
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: Text(
              'Got it',
              style: GoogleFonts.inter(
                fontSize: 14,
                fontWeight: FontWeight.w600,
                color: AppColors.primary,
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _showWhatsAppStatusBottomSheet(BuildContext context, bool isInstalled) {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (context) => Container(
        decoration: BoxDecoration(
          color: AppColors.surface,
          borderRadius: const BorderRadius.vertical(top: Radius.circular(20)),
        ),
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Handle
            Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: AppColors.outline.withOpacity(0.3),
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            const SizedBox(height: 20),
            // Status Icon
            Container(
              width: 60,
              height: 60,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(16),
                color: (isInstalled ? AppColors.success : AppColors.warning).withOpacity(0.1),
              ),
              child: Icon(
                isInstalled ? Icons.check_circle : Icons.warning,
                color: isInstalled ? AppColors.success : AppColors.warning,
                size: 32,
              ),
            ),
            const SizedBox(height: 16),
            // Title
            Text(
              isInstalled ? 'WhatsApp Ready!' : 'WhatsApp Not Found',
              style: GoogleFonts.inter(
                fontSize: 20,
                fontWeight: FontWeight.w700,
                color: AppColors.onSurface,
              ),
            ),
            const SizedBox(height: 8),
            // Description
            Text(
              isInstalled
                  ? 'WhatsApp is installed and ready to receive sticker packs.'
                  : 'Please install WhatsApp to use sticker packs.',
              style: GoogleFonts.inter(
                fontSize: 14,
                fontWeight: FontWeight.w400,
                color: AppColors.onSurfaceVariant,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 24),
            // Action buttons
            Row(
              children: [
                Expanded(
                  child: TextButton(
                    onPressed: () => Navigator.of(context).pop(),
                    style: TextButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 12),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                        side: BorderSide(
                          color: AppColors.outline.withOpacity(0.3),
                        ),
                      ),
                    ),
                    child: Text(
                      'Close',
                      style: GoogleFonts.inter(
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                        color: AppColors.onSurfaceVariant,
                      ),
                    ),
                  ),
                ),
                if (!isInstalled) ...[
                  const SizedBox(width: 12),
                  Expanded(
                    child: ElevatedButton(
                      onPressed: () {
                        Navigator.of(context).pop();
                        _openWhatsAppDownload();
                      },
                      style: ElevatedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        backgroundColor: AppColors.warning,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                      child: Text(
                        'Install WhatsApp',
                        style: GoogleFonts.inter(
                          fontSize: 14,
                          fontWeight: FontWeight.w600,
                          color: Colors.white,
                        ),
                      ),
                    ),
                  ),
                ],
              ],
            ),
            // Safe area padding
            SizedBox(height: MediaQuery.of(context).padding.bottom),
          ],
        ),
      ),
    );
  }

  Future<void> _openWhatsAppDownload() async {
    try {
      // Try to open Google Play Store
      const playStoreUrl = 'https://play.google.com/store/apps/details?id=com.whatsapp';
      if (await canLaunchUrl(Uri.parse(playStoreUrl))) {
        await launchUrl(
          Uri.parse(playStoreUrl),
          mode: LaunchMode.externalApplication,
        );
      } else {
        // Fallback to web browser
        await launchUrl(
          Uri.parse(playStoreUrl),
          mode: LaunchMode.platformDefault,
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: const Text('Please install WhatsApp from your app store'),
            backgroundColor: AppColors.warning,
          ),
        );
      }
    }
  }
}

