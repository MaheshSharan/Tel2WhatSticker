import 'package:flutter/material.dart';
import 'package:file_picker/file_picker.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_text_styles.dart';

class ModernFileList extends StatelessWidget {
  final List<PlatformFile> files;
  final Function(PlatformFile) onRemoveFile;
  final bool isProcessing;

  const ModernFileList({
    super.key,
    required this.files,
    required this.onRemoveFile,
    this.isProcessing = false,
  });

  @override
  Widget build(BuildContext context) {
    if (files.isEmpty) {
      return const SizedBox.shrink();
    }

    return Container(
      margin: const EdgeInsets.only(top: 24),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: AppColors.surfaceContainerHigh.withOpacity(0.5),
          width: 1,
        ),
        boxShadow: [
          BoxShadow(
            color: AppColors.shadowColor.withOpacity(0.05),
            offset: const Offset(0, 4),
            blurRadius: 12,
            spreadRadius: 0,
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header
          Padding(
            padding: const EdgeInsets.all(20),
            child: Row(
              children: [
                Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    gradient: const LinearGradient(
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                      colors: [AppColors.primary, AppColors.secondary],
                    ),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Icon(
                    Icons.folder_rounded,
                    color: AppColors.onPrimary,
                    size: 20,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Selected Files',
                        style: AppTextStyles.titleMedium.copyWith(
                          fontWeight: FontWeight.w600,
                          color: AppColors.onSurface,
                        ),
                      ),
                      Text(
                        '${files.length} file${files.length == 1 ? '' : 's'} selected',
                        style: AppTextStyles.bodySmall.copyWith(
                          color: AppColors.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                if (files.isNotEmpty && !isProcessing)
                  TextButton.icon(
                    onPressed: () {
                      for (final file in files.toList()) {
                        onRemoveFile(file);
                      }
                    },
                    icon: const Icon(Icons.clear_all_rounded, size: 18),
                    label: const Text('Clear All'),
                    style: TextButton.styleFrom(
                      foregroundColor: AppColors.error,
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                    ),
                  ),
              ],
            ),
          ),
          
          // File List
          Container(
            constraints: const BoxConstraints(maxHeight: 300),
            child: ListView.separated(
              shrinkWrap: true,
              padding: const EdgeInsets.only(bottom: 20),
              itemCount: files.length,
              separatorBuilder: (context, index) => Container(
                margin: const EdgeInsets.symmetric(horizontal: 20),
                height: 1,
                color: AppColors.surfaceContainerHigh.withOpacity(0.3),
              ),
              itemBuilder: (context, index) {
                final file = files[index];
                return _buildFileItem(file, index);
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFileItem(PlatformFile file, int index) {
    final fileName = file.name;
    final fileSize = file.size;
    final fileSizeText = _formatFileSize(fileSize);
    final fileExtension = fileName.split('.').last.toUpperCase();
    
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
      child: Row(
        children: [
          // File Icon
          Container(
            width: 48,
            height: 48,
            decoration: BoxDecoration(
              color: _getFileTypeColor(fileExtension).withOpacity(0.1),
              borderRadius: BorderRadius.circular(12),
              border: Border.all(
                color: _getFileTypeColor(fileExtension).withOpacity(0.3),
                width: 1,
              ),
            ),
            child: Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    _getFileTypeIcon(fileExtension),
                    color: _getFileTypeColor(fileExtension),
                    size: 20,
                  ),
                  Text(
                    fileExtension,
                    style: AppTextStyles.overline.copyWith(
                      color: _getFileTypeColor(fileExtension),
                      fontSize: 8,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(width: 16),
          
          // File Info
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  fileName,
                  style: AppTextStyles.bodyMedium.copyWith(
                    fontWeight: FontWeight.w500,
                    color: AppColors.onSurface,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 2),
                Row(
                  children: [
                    Text(
                      fileSizeText,
                      style: AppTextStyles.bodySmall.copyWith(
                        color: AppColors.onSurfaceVariant,
                      ),
                    ),
                    Container(
                      margin: const EdgeInsets.symmetric(horizontal: 8),
                      width: 4,
                      height: 4,
                      decoration: BoxDecoration(
                        color: AppColors.onSurfaceVariant.withOpacity(0.5),
                        shape: BoxShape.circle,
                      ),
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(
                        color: AppColors.success.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: Text(
                        'Ready',
                        style: AppTextStyles.labelSmall.copyWith(
                          color: AppColors.success,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
          
          // Processing Indicator or Remove Button
          if (isProcessing)
            Container(
              width: 24,
              height: 24,
              child: const CircularProgressIndicator(
                strokeWidth: 2,
                valueColor: AlwaysStoppedAnimation<Color>(AppColors.primary),
              ),
            )
          else
            IconButton(
              onPressed: () => onRemoveFile(file),
              icon: const Icon(Icons.close_rounded),
              color: AppColors.error,
              iconSize: 20,
              constraints: const BoxConstraints(minWidth: 32, minHeight: 32),
              tooltip: 'Remove file',
            ),
        ],
      ),
    );
  }

  Color _getFileTypeColor(String extension) {
    switch (extension.toLowerCase()) {
      case 'png':
        return const Color(0xFF4CAF50);
      case 'jpg':
      case 'jpeg':
        return const Color(0xFF2196F3);
      case 'gif':
        return const Color(0xFFFF9800);
      case 'webp':
        return const Color(0xFF9C27B0);
      case 'zip':
        return const Color(0xFFFF5722);
      default:
        return AppColors.onSurfaceVariant;
    }
  }

  IconData _getFileTypeIcon(String extension) {
    switch (extension.toLowerCase()) {
      case 'png':
      case 'jpg':
      case 'jpeg':
      case 'webp':
        return Icons.image_rounded;
      case 'gif':
        return Icons.gif_rounded;
      case 'zip':
        return Icons.archive_rounded;
      default:
        return Icons.insert_drive_file_rounded;
    }
  }

  String _formatFileSize(int bytes) {
    if (bytes < 1024) return '$bytes B';
    if (bytes < 1024 * 1024) return '${(bytes / 1024).toStringAsFixed(1)} KB';
    if (bytes < 1024 * 1024 * 1024) return '${(bytes / (1024 * 1024)).toStringAsFixed(1)} MB';
    return '${(bytes / (1024 * 1024 * 1024)).toStringAsFixed(1)} GB';
  }
}
