import 'dart:io';
import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';

/// Enhanced image widget that properly handles animated WebP files with cache management
class AnimatedWebPPreview extends StatefulWidget {
  final String? filePath;
  final String? networkUrl;
  final double width;
  final double height;
  final BoxFit fit;
  final Widget? placeholder;
  final Widget? errorWidget;
  final String cacheKey;
  final bool enableCache;

  const AnimatedWebPPreview({
    super.key,
    this.filePath,
    this.networkUrl,
    this.width = 48,
    this.height = 48,
    this.fit = BoxFit.contain,
    this.placeholder,
    this.errorWidget,
    required this.cacheKey,
    this.enableCache = true,
  }) : assert(filePath != null || networkUrl != null, 'Either filePath or networkUrl must be provided');

  @override
  State<AnimatedWebPPreview> createState() => _AnimatedWebPPreviewState();
}

class _AnimatedWebPPreviewState extends State<AnimatedWebPPreview> {
  bool _hasError = false;
  bool _isLoading = true;

  @override
  Widget build(BuildContext context) {
    if (widget.filePath != null) {
      return _buildFileImage();
    } else if (widget.networkUrl != null) {
      return _buildNetworkImage();
    } else {
      return _buildErrorWidget();
    }
  }

  Widget _buildFileImage() {
    return Container(
      width: widget.width,
      height: widget.height,
      child: _hasError
          ? _buildErrorWidget()
          : Stack(
              children: [
                if (_isLoading) _buildPlaceholder(),
                Image.file(
                  File(widget.filePath!),
                  width: widget.width,
                  height: widget.height,
                  fit: widget.fit,
                  frameBuilder: (context, child, frame, wasSynchronouslyLoaded) {
                    if (wasSynchronouslyLoaded || frame != null) {
                      WidgetsBinding.instance.addPostFrameCallback((_) {
                        if (mounted && _isLoading) {
                          setState(() => _isLoading = false);
                        }
                      });
                      return child;
                    }
                    return _buildPlaceholder();
                  },
                  errorBuilder: (context, error, stackTrace) {
                    WidgetsBinding.instance.addPostFrameCallback((_) {
                      if (mounted) {
                        setState(() {
                          _hasError = true;
                          _isLoading = false;
                        });
                      }
                    });
                    return _buildErrorWidget();
                  },
                ),
              ],
            ),
    );
  }

  Widget _buildNetworkImage() {
    if (!widget.enableCache) {
      return Image.network(
        widget.networkUrl!,
        width: widget.width,
        height: widget.height,
        fit: widget.fit,
        loadingBuilder: (context, child, loadingProgress) {
          if (loadingProgress == null) return child;
          return _buildPlaceholder();
        },
        errorBuilder: (context, error, stackTrace) => _buildErrorWidget(),
      );
    }

    return CachedNetworkImage(
      imageUrl: widget.networkUrl!,
      width: widget.width,
      height: widget.height,
      fit: widget.fit,
      cacheKey: widget.cacheKey,
      placeholder: (context, url) => _buildPlaceholder(),
      errorWidget: (context, url, error) => _buildErrorWidget(),
      fadeInDuration: const Duration(milliseconds: 200),
      fadeOutDuration: const Duration(milliseconds: 100),
    );
  }

  Widget _buildPlaceholder() {
    return widget.placeholder ??
        Container(
          width: widget.width,
          height: widget.height,
          decoration: BoxDecoration(
            color: Colors.grey.shade200,
            borderRadius: BorderRadius.circular(8),
          ),
          child: const Center(
            child: SizedBox(
              width: 16,
              height: 16,
              child: CircularProgressIndicator(
                strokeWidth: 2,
                valueColor: AlwaysStoppedAnimation<Color>(Colors.grey),
              ),
            ),
          ),
        );
  }

  Widget _buildErrorWidget() {
    return widget.errorWidget ??
        Container(
          width: widget.width,
          height: widget.height,
          decoration: BoxDecoration(
            color: Colors.red.shade50,
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: Colors.red.shade200),
          ),
          child: Icon(
            Icons.error_outline,
            color: Colors.red.shade400,
            size: widget.width * 0.4,
          ),
        );
  }

  /// Force refresh the image cache for this widget
  void refreshCache() {
    if (widget.enableCache && widget.networkUrl != null) {
      CachedNetworkImage.evictFromCache(widget.networkUrl!, cacheKey: widget.cacheKey);
    }
    
    if (mounted) {
      setState(() {
        _hasError = false;
        _isLoading = true;
      });
    }
  }

  /// Clear the cache for this specific image
  static Future<void> clearCacheForKey(String? url, String cacheKey) async {
    if (url != null) {
      await CachedNetworkImage.evictFromCache(url, cacheKey: cacheKey);
    }
  }

  /// Clear all cached images
  static Future<void> clearAllCache() async {
    // Method removed: CachedNetworkImage.clearImageCacheDirectory() does not exist in the package.
    // If you need to clear cache, consider evicting specific URLs or use another cache clearing approach.
  }
}

/// Enhanced loading state widget for sticker previews
class StickerPreviewLoadingState extends StatelessWidget {
  final double size;
  final String status;
  final double progress;
  final String? emoji;

  const StickerPreviewLoadingState({
    super.key,
    this.size = 48,
    required this.status,
    this.progress = 0.0,
    this.emoji,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: _getBackgroundColor(),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: _getBorderColor(), width: 1),
      ),
      child: Stack(
        children: [
          // Background emoji if available
          if (emoji != null)
            Center(
              child: Text(
                emoji!,
                style: TextStyle(
                  fontSize: size * 0.5,
                  color: Colors.grey.shade400,
                ),
              ),
            ),
          
          // Status overlay
          Center(
            child: _buildStatusWidget(),
          ),
          
          // Progress indicator
          if (progress > 0 && progress < 1)
            Positioned.fill(
              child: CircularProgressIndicator(
                value: progress,
                strokeWidth: 2,
                backgroundColor: Colors.grey.shade300,
                valueColor: AlwaysStoppedAnimation<Color>(_getProgressColor()),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildStatusWidget() {
    switch (status) {
      case 'downloading':
        return Icon(
          Icons.download,
          size: size * 0.3,
          color: Colors.blue.shade600,
        );
      case 'converting':
        return Icon(
          Icons.sync,
          size: size * 0.3,
          color: Colors.orange.shade600,
        );
      case 'error':
        return Icon(
          Icons.error_outline,
          size: size * 0.4,
          color: Colors.red.shade600,
        );
      case 'pending':
      default:
        return Icon(
          Icons.hourglass_empty,
          size: size * 0.3,
          color: Colors.grey.shade600,
        );
    }
  }

  Color _getBackgroundColor() {
    switch (status) {
      case 'downloading':
        return Colors.blue.shade50;
      case 'converting':
        return Colors.orange.shade50;
      case 'error':
        return Colors.red.shade50;
      case 'pending':
      default:
        return Colors.grey.shade100;
    }
  }

  Color _getBorderColor() {
    switch (status) {
      case 'downloading':
        return Colors.blue.shade200;
      case 'converting':
        return Colors.orange.shade200;
      case 'error':
        return Colors.red.shade200;
      case 'pending':
      default:
        return Colors.grey.shade300;
    }
  }

  Color _getProgressColor() {
    switch (status) {
      case 'downloading':
        return Colors.blue.shade600;
      case 'converting':
        return Colors.orange.shade600;
      default:
        return Colors.grey.shade600;
    }
  }
}
