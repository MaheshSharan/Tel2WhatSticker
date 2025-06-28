import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/router/app_router.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_text_styles.dart';
import '../../domain/entities/sticker_pack_entity.dart';
import '../bloc/sticker_converter_bloc.dart';
import '../bloc/sticker_converter_state.dart';
import '../widgets/gradient_background.dart';

class ProcessingPage extends StatefulWidget {
  final Map<String, dynamic>? processData;

  const ProcessingPage({
    super.key,
    this.processData,
  });

  @override
  State<ProcessingPage> createState() => _ProcessingPageState();
}

class _ProcessingPageState extends State<ProcessingPage>
    with TickerProviderStateMixin {
  late AnimationController _pulseController;
  late Animation<double> _pulseAnimation;

  @override
  void initState() {
    super.initState();
    _setupAnimations();
  }

  void _setupAnimations() {
    _pulseController = AnimationController(
      duration: const Duration(seconds: 2),
      vsync: this,
    )..repeat(reverse: true);

    _pulseAnimation = Tween<double>(
      begin: 0.8,
      end: 1.2,
    ).animate(CurvedAnimation(
      parent: _pulseController,
      curve: Curves.easeInOut,
    ));
  }

  @override
  void dispose() {
    _pulseController.dispose();
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
                processCompleted: (pack) {
                  context.go(AppRouter.preview, extra: {
                    'pack': pack,
                    'processData': widget.processData,
                  });
                },
                error: (message) {
                  _showErrorDialog(context, message);
                },
                orElse: () {},
              );
            },
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                children: [
                  _buildHeader(),
                  Expanded(
                    child: _buildProcessingContent(),
                  ),
                  _buildCancelButton(),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Column(
      children: [
        Text(
          'Processing Your Stickers',
          style: AppTextStyles.h4.copyWith(
            fontWeight: FontWeight.bold,
          ),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 8),
        Text(
          'Please wait while we convert your stickers to WhatsApp format',
          style: AppTextStyles.bodyMedium.copyWith(
            color: AppColors.onSurfaceVariant,
          ),
          textAlign: TextAlign.center,
        ),
      ],
    );
  }

  Widget _buildProcessingContent() {
    return BlocBuilder<StickerConverterBloc, StickerConverterState>(
      builder: (context, state) {
        return state.maybeWhen(
          (isLoading, isWhatsAppInstalled, isProcessing, currentPack, processingProgress, validatedFiles, extractedDirectory, error, successMessage) => 
              processingProgress != null ? _buildProcessingWidget(processingProgress) : _buildLoadingWidget(),
          processing: (progress) => _buildProcessingWidget(progress),
          loading: () => _buildLoadingWidget(),
          orElse: () => _buildLoadingWidget(),
        );
      },
    );
  }

  Widget _buildProcessingWidget(ProcessingState progress) {
    final overallProgress = progress.totalFiles > 0
        ? progress.completedFiles / progress.totalFiles
        : 0.0;

    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        // Main processing animation
        AnimatedBuilder(
          animation: _pulseAnimation,
          builder: (context, child) {
            return Transform.scale(
              scale: _pulseAnimation.value,
              child: Container(
                width: 120,
                height: 120,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  gradient: LinearGradient(
                    colors: AppColors.primaryGradient,
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  ),
                  boxShadow: [
                    BoxShadow(
                      color: AppColors.primary.withOpacity(0.3),
                      blurRadius: 20,
                      offset: const Offset(0, 10),
                    ),
                  ],
                ),
                child: const Icon(
                  Icons.auto_fix_high,
                  size: 60,
                  color: AppColors.onPrimary,
                ),
              ),
            );
          },
        ),
        
        const SizedBox(height: 40),
        
        // Overall progress
        _buildProgressSection(
          'Overall Progress',
          overallProgress,
          '${progress.completedFiles}/${progress.totalFiles} files',
        ),
        
        const SizedBox(height: 24),
        
        // Individual file progress
        if (progress.fileProgress.isNotEmpty) ...[
          Text(
            'Current Files',
            style: AppTextStyles.h6.copyWith(
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 16),
          SizedBox(
            height: 200,
            child: ListView.builder(
              itemCount: progress.fileProgress.length,
              itemBuilder: (context, index) {
                final entry = progress.fileProgress.entries.elementAt(index);
                return _buildFileProgressItem(entry.key, entry.value);
              },
            ),
          ),
        ],
        
        // Status indicators
        const SizedBox(height: 24),
        _buildStatusIndicators(progress),
      ],
    );
  }

  Widget _buildLoadingWidget() {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        AnimatedBuilder(
          animation: _pulseAnimation,
          builder: (context, child) {
            return Transform.scale(
              scale: _pulseAnimation.value,
              child: Container(
                width: 120,
                height: 120,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  gradient: LinearGradient(
                    colors: AppColors.primaryGradient,
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  ),
                  boxShadow: [
                    BoxShadow(
                      color: AppColors.primary.withOpacity(0.3),
                      blurRadius: 20,
                      offset: const Offset(0, 10),
                    ),
                  ],
                ),
                child: const CircularProgressIndicator(
                  valueColor: AlwaysStoppedAnimation<Color>(AppColors.onPrimary),
                  strokeWidth: 3,
                ),
              ),
            );
          },
        ),
        const SizedBox(height: 40),
        Text(
          'Initializing...',
          style: AppTextStyles.h6.copyWith(
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 8),
        Text(
          'Setting up the processing environment',
          style: AppTextStyles.bodyMedium.copyWith(
            color: AppColors.onSurfaceVariant,
          ),
          textAlign: TextAlign.center,
        ),
      ],
    );
  }

  Widget _buildProgressSection(String title, double progress, String subtitle) {
    return Column(
      children: [
        Text(
          title,
          style: AppTextStyles.h6.copyWith(
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 12),
        Container(
          width: double.infinity,
          height: 8,
          decoration: BoxDecoration(
            color: AppColors.progressBackground,
            borderRadius: BorderRadius.circular(4),
          ),
          child: FractionallySizedBox(
            alignment: Alignment.centerLeft,
            widthFactor: progress.clamp(0.0, 1.0),
            child: Container(
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: AppColors.primaryGradient,
                ),
                borderRadius: BorderRadius.circular(4),
              ),
            ),
          ),
        ),
        const SizedBox(height: 8),
        Text(
          subtitle,
          style: AppTextStyles.bodyMedium.copyWith(
            color: AppColors.onSurfaceVariant,
          ),
        ),
      ],
    );
  }

  Widget _buildFileProgressItem(String filePath, double progress) {
    final fileName = filePath.split('/').last;
    
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(
          color: AppColors.progressBackground,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  fileName,
                  style: AppTextStyles.labelMedium.copyWith(
                    fontWeight: FontWeight.w500,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              Text(
                '${(progress * 100).round()}%',
                style: AppTextStyles.labelSmall.copyWith(
                  color: AppColors.onSurfaceVariant,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          LinearProgressIndicator(
            value: progress,
            backgroundColor: AppColors.progressBackground,
            valueColor: AlwaysStoppedAnimation<Color>(AppColors.primary),
          ),
        ],
      ),
    );
  }

  Widget _buildStatusIndicators(ProcessingState progress) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      children: [
        _buildStatusItem(
          Icons.check_circle,
          'Completed',
          progress.completedFiles.toString(),
          AppColors.success,
        ),
        _buildStatusItem(
          Icons.hourglass_empty,
          'Processing',
          (progress.totalFiles - progress.completedFiles - progress.errorFiles).toString(),
          AppColors.warning,
        ),
        _buildStatusItem(
          Icons.error,
          'Errors',
          progress.errorFiles.toString(),
          AppColors.error,
        ),
      ],
    );
  }

  Widget _buildStatusItem(IconData icon, String label, String count, Color color) {
    return Column(
      children: [
        Container(
          width: 48,
          height: 48,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: color.withOpacity(0.1),
          ),
          child: Icon(
            icon,
            color: color,
            size: 24,
          ),
        ),
        const SizedBox(height: 8),
        Text(
          count,
          style: AppTextStyles.h6.copyWith(
            fontWeight: FontWeight.bold,
            color: color,
          ),
        ),
        Text(
          label,
          style: AppTextStyles.labelSmall.copyWith(
            color: AppColors.onSurfaceVariant,
          ),
        ),
      ],
    );
  }

  Widget _buildCancelButton() {
    return SizedBox(
      width: double.infinity,
      child: OutlinedButton(
        onPressed: () {
          _showCancelDialog();
        },
        style: OutlinedButton.styleFrom(
          foregroundColor: AppColors.error,
          side: const BorderSide(color: AppColors.error),
          padding: const EdgeInsets.symmetric(vertical: 12),
        ),
        child: const Text('Cancel Processing'),
      ),
    );
  }

  void _showCancelDialog() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Cancel Processing'),
        content: const Text(
          'Are you sure you want to cancel the processing? '
          'You will lose all progress and need to start over.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Continue'),
          ),
          TextButton(
            onPressed: () {
              Navigator.of(context).pop();
              context.go(AppRouter.home);
            },
            style: TextButton.styleFrom(
              foregroundColor: AppColors.error,
            ),
            child: const Text('Cancel'),
          ),
        ],
      ),
    );
  }

  void _showErrorDialog(BuildContext context, String message) {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        title: const Text('Processing Error'),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.of(context).pop();
              context.go(AppRouter.home);
            },
            child: const Text('Go Home'),
          ),
          TextButton(
            onPressed: () {
              Navigator.of(context).pop();
              context.go(AppRouter.upload);
            },
            child: const Text('Try Again'),
          ),
        ],
      ),
    );
  }
}
