import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/router/app_router.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_text_styles.dart';
import '../../../../core/theme/app_icons.dart';
import '../../domain/entities/sticker_pack_entity.dart';
import '../bloc/sticker_converter_bloc.dart';
import '../bloc/sticker_converter_event.dart';
import '../bloc/sticker_converter_state.dart';
import '../widgets/gradient_background.dart';

class PreviewPage extends StatelessWidget {
  final Map<String, dynamic>? packData;

  const PreviewPage({
    super.key,
    this.packData,
  });

  @override
  Widget build(BuildContext context) {
    if (packData == null || packData!['pack'] == null) {
      return _buildErrorScreen(context);
    }

    final pack = packData!['pack'] as StickerPackEntity;
    
    return Scaffold(
      body: GradientBackground(
        child: SafeArea(
          child: BlocListener<StickerConverterBloc, StickerConverterState>(
            listener: (context, state) {
              state.maybeWhen(
                (isLoading, isWhatsAppInstalled, isProcessing, currentPack, processingProgress, validatedFiles, extractedDirectory, error, successMessage) => null,
                addedToWhatsApp: (pack) {
                  context.go(AppRouter.success, extra: {
                    'pack': pack,
                    'message': 'Sticker pack added to WhatsApp successfully!',
                  });
                },
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
            child: Column(
              children: [
                _buildAppBar(context),
                Expanded(
                  child: SingleChildScrollView(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        _buildPackInfo(pack),
                        const SizedBox(height: 24),
                        _buildStickerGrid(pack),
                        const SizedBox(height: 24),
                        _buildPackStats(pack),
                      ],
                    ),
                  ),
                ),
                _buildBottomActions(context, pack),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildErrorScreen(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(
              Icons.error_outline,
              size: 64,
              color: AppColors.error,
            ),
            const SizedBox(height: 16),
            Text(
              'No sticker pack data found',
              style: AppTextStyles.h5,
            ),
            const SizedBox(height: 24),
            ElevatedButton(
              onPressed: () => context.go(AppRouter.home),
              child: const Text('Go Home'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAppBar(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      child: Row(
        children: [
          IconButton(
            icon: const Icon(Icons.arrow_back),
            onPressed: () => context.go(AppRouter.upload),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              'Preview Sticker Pack',
              style: AppTextStyles.h5.copyWith(
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
          IconButton(
            icon: const Icon(Icons.edit),
            onPressed: () {
              // Navigate back to edit
              context.go(AppRouter.upload);
            },
          ),
        ],
      ),
    );
  }

  Widget _buildPackInfo(StickerPackEntity pack) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  width: 64,
                  height: 64,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(12),
                    gradient: LinearGradient(
                      colors: AppColors.primaryGradient,
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                    ),
                  ),
                  child: const Icon(
                    Icons.collections,
                    color: AppColors.onPrimary,
                    size: 32,
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        pack.name,
                        style: AppTextStyles.h5.copyWith(
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        'by ${pack.publisher}',
                        style: AppTextStyles.bodyMedium.copyWith(
                          color: AppColors.onSurfaceVariant,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 8,
                          vertical: 4,
                        ),
                        decoration: BoxDecoration(
                          color: pack.animated 
                              ? AppColors.warning.withOpacity(0.1)
                              : AppColors.success.withOpacity(0.1),
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Text(
                          pack.animated ? 'Animated' : 'Static',
                          style: AppTextStyles.labelSmall.copyWith(
                            color: pack.animated 
                                ? AppColors.warning
                                : AppColors.success,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStickerGrid(StickerPackEntity pack) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Stickers (${pack.stickers.length})',
          style: AppTextStyles.h6.copyWith(
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 12),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: GridView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 3,
                mainAxisSpacing: 8,
                crossAxisSpacing: 8,
                childAspectRatio: 1,
              ),
              itemCount: pack.stickers.length,
              itemBuilder: (context, index) {
                final sticker = pack.stickers[index];
                return _buildStickerPreview(sticker, index);
              },
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildStickerPreview(StickerEntity sticker, int index) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.surfaceVariant,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(
          color: AppColors.progressBackground,
        ),
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(7),
        child: Stack(
          children: [
            // Image preview or placeholder
            Container(
              width: double.infinity,
              height: double.infinity,
              color: AppColors.surfaceVariant,
              child: sticker.imagePath.isNotEmpty
                  ? Image.file(
                      File(sticker.imagePath),
                      fit: BoxFit.cover,
                      errorBuilder: (context, error, stackTrace) {
                        return const Icon(
                          Icons.image_not_supported,
                          color: AppColors.onSurfaceVariant,
                        );
                      },
                    )
                  : const Icon(
                      Icons.image,
                      color: AppColors.onSurfaceVariant,
                    ),
            ),
            // Overlay with sticker info
            Positioned(
              bottom: 0,
              left: 0,
              right: 0,
              child: Container(
                padding: const EdgeInsets.all(4),
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.bottomCenter,
                    end: Alignment.topCenter,
                    colors: [
                      Colors.black.withOpacity(0.7),
                      Colors.transparent,
                    ],
                  ),
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    if (sticker.emojis.isNotEmpty)
                      Text(
                        sticker.emojis.take(3).join(' '),
                        style: AppTextStyles.labelSmall.copyWith(
                          color: Colors.white,
                        ),
                        textAlign: TextAlign.center,
                      ),
                    Text(
                      '${sticker.fileSizeKB}KB',
                      style: AppTextStyles.labelSmall.copyWith(
                        color: Colors.white70,
                        fontSize: 10,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPackStats(StickerPackEntity pack) {
    final totalSize = pack.stickers.fold<int>(
      0, 
      (sum, sticker) => sum + sticker.fileSizeKB,
    );
    
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Pack Statistics',
              style: AppTextStyles.h6.copyWith(
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 16),
            _buildStatRow('Total Stickers', '${pack.stickers.length}'),
            _buildStatRow('Total Size', '${totalSize}KB'),
            _buildStatRow('Average Size', '${(totalSize / pack.stickers.length).round()}KB'),
            _buildStatRow('WhatsApp Compatible', 
                pack.stickers.length >= 3 && pack.stickers.length <= 30 
                    ? 'Yes ✓' : 'No ✗'),
          ],
        ),
      ),
    );
  }

  Widget _buildStatRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            label,
            style: AppTextStyles.bodyMedium.copyWith(
              color: AppColors.onSurfaceVariant,
            ),
          ),
          Text(
            value,
            style: AppTextStyles.bodyMedium.copyWith(
              fontWeight: FontWeight.w600,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBottomActions(BuildContext context, StickerPackEntity pack) {
    return Container(
      padding: const EdgeInsets.all(16),
      child: BlocBuilder<StickerConverterBloc, StickerConverterState>(
        builder: (context, state) {
          final isLoading = state.maybeWhen(
            (isLoading, isWhatsAppInstalled, isProcessing, currentPack, processingProgress, validatedFiles, extractedDirectory, error, successMessage) => isLoading,
            loading: () => true,
            orElse: () => false,
          );
          
          return Column(
            children: [
              SizedBox(
                width: double.infinity,
                child: ElevatedButton.icon(
                  onPressed: !isLoading ? () => _addToWhatsApp(context, pack) : null,
                  icon: isLoading
                      ? const SizedBox(
                          width: 16,
                          height: 16,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            valueColor: AlwaysStoppedAnimation<Color>(
                              AppColors.onPrimary,
                            ),
                          ),
                        )
                      : Icon(AppIcons.whatsappAlt),
                  label: Text(
                    isLoading ? 'Adding to WhatsApp...' : 'Add to WhatsApp',
                    style: AppTextStyles.button,
                  ),
                  style: ElevatedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 16),
                  ),
                ),
              ),
              const SizedBox(height: 8),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton(
                  onPressed: () => context.go(AppRouter.upload),
                  child: const Text('Make Changes'),
                ),
              ),
            ],
          );
        },
      ),
    );
  }

  void _addToWhatsApp(BuildContext context, StickerPackEntity pack) {
    context.read<StickerConverterBloc>().add(
      StickerConverterEvent.addToWhatsApp(pack: pack),
    );
  }
}
