import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_staggered_grid_view/flutter_staggered_grid_view.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_text_styles.dart';

class SelectedFilesList extends StatefulWidget {
  final List<File> files;
  final Function(File) onRemoveFile;
  final VoidCallback onAddMore;

  const SelectedFilesList({
    super.key,
    required this.files,
    required this.onRemoveFile,
    required this.onAddMore,
  });

  @override
  State<SelectedFilesList> createState() => _SelectedFilesListState();
}

class _SelectedFilesListState extends State<SelectedFilesList>
    with TickerProviderStateMixin {
  late AnimationController _listAnimationController;
  final List<AnimationController> _itemControllers = [];

  @override
  void initState() {
    super.initState();
    _listAnimationController = AnimationController(
      duration: const Duration(milliseconds: 300),
      vsync: this,
    );
    
    _setupItemControllers();
    _listAnimationController.forward();
  }

  void _setupItemControllers() {
    for (int i = 0; i < widget.files.length; i++) {
      final controller = AnimationController(
        duration: Duration(milliseconds: 300 + (i * 50)),
        vsync: this,
      );
      _itemControllers.add(controller);
      controller.forward();
    }
  }

  @override
  void didUpdateWidget(SelectedFilesList oldWidget) {
    super.didUpdateWidget(oldWidget);
    
    // Handle new files
    if (widget.files.length > _itemControllers.length) {
      for (int i = _itemControllers.length; i < widget.files.length; i++) {
        final controller = AnimationController(
          duration: const Duration(milliseconds: 300),
          vsync: this,
        );
        _itemControllers.add(controller);
        controller.forward();
      }
    }
    
    // Handle removed files
    if (widget.files.length < _itemControllers.length) {
      for (int i = widget.files.length; i < _itemControllers.length; i++) {
        _itemControllers[i].dispose();
      }
      _itemControllers.removeRange(widget.files.length, _itemControllers.length);
    }
  }

  @override
  void dispose() {
    _listAnimationController.dispose();
    for (final controller in _itemControllers) {
      controller.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _buildHeader(),
        const SizedBox(height: 24),
        SizedBox(
          height: 400, // Fixed height instead of Expanded
          child: _buildFileGrid(),
        ),
        const SizedBox(height: 16),
        _buildAddMoreButton(),
      ],
    );
  }

  Widget _buildHeader() {
    return FadeTransition(
      opacity: _listAnimationController,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '${widget.files.length} Files Selected',
            style: AppTextStyles.h4.copyWith(
              fontWeight: FontWeight.bold,
              color: AppColors.onBackground,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'You can tap a file to remove it from the list.',
            style: AppTextStyles.bodyLarge.copyWith(
              color: AppColors.onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFileGrid() {
    return AnimatedBuilder(
      animation: _listAnimationController,
      builder: (context, child) {
        return MasonryGridView.count(
          crossAxisCount: 2,
          mainAxisSpacing: 12,
          crossAxisSpacing: 12,
          itemCount: widget.files.length,
          itemBuilder: (context, index) {
            if (index >= _itemControllers.length) {
              return const SizedBox.shrink();
            }
            
            return AnimatedBuilder(
              animation: _itemControllers[index],
              builder: (context, child) {
                return FadeTransition(
                  opacity: _itemControllers[index],
                  child: SlideTransition(
                    position: Tween<Offset>(
                      begin: const Offset(0, 0.2),
                      end: Offset.zero,
                    ).animate(CurvedAnimation(
                      parent: _itemControllers[index],
                      curve: Curves.easeOutCubic,
                    )),
                    child: _buildFileCard(widget.files[index], index),
                  ),
                );
              },
            );
          },
        );
      },
    );
  }

  Widget _buildFileCard(File file, int index) {
    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: AppColors.outline.withOpacity(0.3)),
      ),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => _showRemoveDialog(file),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              height: 120,
              width: double.infinity,
              decoration: BoxDecoration(
                color: AppColors.surfaceVariant.withOpacity(0.5),
              ),
              child: _buildFilePreview(file),
            ),
            Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    _getFileName(file),
                    style: AppTextStyles.labelLarge.copyWith(
                      fontWeight: FontWeight.w600,
                      color: AppColors.onSurface,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: 4),
                  FutureBuilder<int>(
                    future: file.length(),
                    builder: (context, snapshot) {
                      return Text(
                        snapshot.hasData
                            ? _formatFileSize(snapshot.data!)
                            : '...',
                        style: AppTextStyles.bodySmall.copyWith(
                          color: AppColors.onSurfaceVariant,
                        ),
                      );
                    },
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFilePreview(File file) {
    final extension = file.path.split('.').last.toLowerCase();
    
    if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].contains(extension)) {
      return FutureBuilder<bool>(
        future: _checkFileExists(file),
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return Container(
              color: AppColors.surface,
              child: const Center(
                child: SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(strokeWidth: 2),
                ),
              ),
            );
          }
          
          if (snapshot.data == false) {
            return _buildFileIcon(Icons.broken_image_outlined, AppColors.error);
          }
          
          return Image.file(
            file,
            fit: BoxFit.cover,
            errorBuilder: (context, error, stackTrace) {
              print('Image loading error for ${file.path}: $error');
              
              // For WebP files that can't be displayed, show a special icon
              final extension = file.path.toLowerCase().split('.').last;
              if (extension == 'webp') {
                return Container(
                  color: AppColors.surfaceVariant,
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(
                        Icons.image_outlined,
                        size: 30,
                        color: AppColors.primary,
                      ),
                      const SizedBox(height: 4),
                      Text(
                        'WebP',
                        style: AppTextStyles.caption.copyWith(
                          color: AppColors.primary,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                );
              }
              
              return _buildFileIcon(Icons.image_not_supported_outlined, AppColors.warning);
            },
            frameBuilder: (context, child, frame, wasSynchronouslyLoaded) {
              if (wasSynchronouslyLoaded) {
                return child;
              }
              return AnimatedOpacity(
                opacity: frame == null ? 0 : 1,
                duration: const Duration(milliseconds: 300),
                child: child,
              );
            },
          );
        },
      );
    } else {
      return _buildFileIcon(Icons.insert_drive_file_outlined, AppColors.primary);
    }
  }
  
  Future<bool> _checkFileExists(File file) async {
    try {
      final exists = await file.exists();
      if (!exists) {
        print('File does not exist: ${file.path}');
        return false;
      }
      
      final size = await file.length();
      if (size == 0) {
        print('File is empty: ${file.path}');
        return false;
      }
      
      return true;
    } catch (e) {
      print('Error checking file ${file.path}: ${e.toString()}');
      return false;
    }
  }

  Widget _buildFileIcon(IconData icon, Color color) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Icon(
          icon,
          size: 40,
          color: color.withOpacity(0.8),
        ),
      ],
    );
  }

  Widget _buildAddMoreButton() {
    return Padding(
      padding: const EdgeInsets.only(top: 8.0),
      child: ElevatedButton.icon(
        onPressed: widget.onAddMore,
        icon: const Icon(Icons.add_circle_outline),
        label: const Text('Add More Files'),
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.primary.withOpacity(0.1),
          foregroundColor: AppColors.primary,
          elevation: 0,
          padding: const EdgeInsets.symmetric(vertical: 16),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
        ),
      ),
    );
  }

  String _getFileName(File file) {
    return file.path.split('/').last;
  }

  String _formatFileSize(int bytes) {
    if (bytes < 1024) return '$bytes B';
    if (bytes < 1024 * 1024) return '${(bytes / 1024).toStringAsFixed(1)} KB';
    return '${(bytes / (1024 * 1024)).toStringAsFixed(1)} MB';
  }

  void _showRemoveDialog(File file) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Remove File'),
        content: Text(
          'Are you sure you want to remove "${_getFileName(file)}" from the selection?',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () {
              Navigator.of(context).pop();
              widget.onRemoveFile(file);
            },
            style: TextButton.styleFrom(
              foregroundColor: AppColors.error,
            ),
            child: const Text('Remove'),
          ),
        ],
      ),
    );
  }
}
