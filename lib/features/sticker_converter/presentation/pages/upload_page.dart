import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import 'package:file_picker/file_picker.dart';
import '../../../../core/router/app_router.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_text_styles.dart';
import '../../../../core/constants/app_constants.dart';
import '../../domain/entities/sticker_pack_entity.dart';
import '../bloc/sticker_converter_bloc.dart';
import '../bloc/sticker_converter_event.dart';
import '../bloc/sticker_converter_state.dart';
import '../widgets/gradient_background.dart';
import '../widgets/upload_area.dart';
import '../widgets/telegram_url_input.dart';
import '../widgets/selected_files_list.dart';

class UploadPage extends StatefulWidget {
  final String inputType;

  const UploadPage({
    super.key,
    required this.inputType,
  });

  @override
  State<UploadPage> createState() => _UploadPageState();
}

class _UploadPageState extends State<UploadPage> with TickerProviderStateMixin {
  late TabController _tabController;
  final _packNameController = TextEditingController();
  final _publisherController = TextEditingController();
  final _telegramUrlController = TextEditingController();
  
  List<File> _selectedFiles = [];
  String _currentInputType = 'images';
  
  // Telegram-related state - simplified for unified approach
  bool _telegramMetadataLoading = false;
  String? _telegramPackError;
  
  // Telegram download progress tracking
  List<Map<String, dynamic>> _telegramStickers = [];
  int _telegramCurrentIndex = 0;
  int _telegramTotalStickers = 0;
  bool _isTelegramDownloading = false;
  
  @override
  void initState() {
    super.initState();
    _currentInputType = widget.inputType;
    _tabController = TabController(
      length: 3,
      vsync: this,
      initialIndex: _getInitialTabIndex(),
    );
    
    // Don't set default text - let users enter their own
    
    _tabController.addListener(() {
      setState(() {
        _currentInputType = _getInputTypeFromIndex(_tabController.index);
        _selectedFiles.clear();
        // Clear Telegram state when switching tabs
        _telegramMetadataLoading = false;
        _telegramPackError = null;
        _isTelegramDownloading = false;
        _telegramStickers.clear();
        _telegramCurrentIndex = 0;
        _telegramTotalStickers = 0;
      });
    });

    _telegramUrlController.addListener(_onTelegramUrlChanged);
  }
  
  int _getInitialTabIndex() {
    switch (widget.inputType) {
      case 'telegram':
        return 1;
      case 'zip':
        return 2;
      default:
        return 0;
    }
  }
  
  String _getInputTypeFromIndex(int index) {
    switch (index) {
      case 1:
        return 'telegram';
      case 2:
        return 'zip';
      default:
        return 'images';
    }
  }
  
  @override
  void dispose() {
    _tabController.dispose();
    _packNameController.dispose();
    _publisherController.dispose();
    _telegramUrlController.removeListener(_onTelegramUrlChanged);
    _telegramUrlController.dispose();
    super.dispose();
  }

  void _onTelegramUrlChanged() {
    final url = _telegramUrlController.text.trim();
    if (url.isNotEmpty) {
      setState(() {
        _telegramMetadataLoading = true;
        _telegramPackError = null;
        _isTelegramDownloading = false;
        _telegramStickers.clear();
        _telegramCurrentIndex = 0;
        _telegramTotalStickers = 0;
        _selectedFiles.clear(); // Clear any existing files
      });
      context.read<StickerConverterBloc>().add(
        StickerConverterEvent.downloadTelegramStickers(url: url),
      );
    } else {
      setState(() {
        _telegramMetadataLoading = false;
        _telegramPackError = null;
        _isTelegramDownloading = false;
        _telegramStickers.clear();
        _telegramCurrentIndex = 0;
        _telegramTotalStickers = 0;
        _selectedFiles.clear();
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: _buildAppBar(),
      body: BlocListener<StickerConverterBloc, StickerConverterState>(
        listener: (context, state) {
          state.when(
            // General state with all fields
            (isLoading, isWhatsAppInstalled, isProcessing, currentPack, processingProgress, validatedFiles, extractedDirectory, error, successMessage) {
              // Update local files list when files are validated
              if (validatedFiles != null && validatedFiles.isNotEmpty) {
                setState(() {
                  _selectedFiles = validatedFiles.map((path) => File(path)).toList();
                });
              }
                 // Handle extracted directory
            if (extractedDirectory != null && extractedDirectory.isNotEmpty) {
              _scanExtractedDirectory(extractedDirectory);
            }
            
            // Show error if any
            if (error != null) {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: Text(error),
                  backgroundColor: AppColors.error,
                ),
              );
            }
            
            // Show success message
            if (successMessage != null) {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: Text(successMessage),
                  backgroundColor: AppColors.success,
                ),
              );
            }
          },
          
          // Handle Telegram stickers downloaded
          telegramStickersDownloaded: (filePaths, packName, packTitle) {
            setState(() {
              _telegramMetadataLoading = false;
              _telegramPackError = null;
              _isTelegramDownloading = false;
              _selectedFiles = filePaths.map((path) => File(path)).toList();
              
              // Auto-fill pack details
              _packNameController.text = packTitle.isNotEmpty ? packTitle : packName;
              _publisherController.text = 'Telegram';
            });
          },
          
          // Specific states
          initial: () {},
          loading: () {},
          processing: (progress) {
            // Show processing feedback if needed
            if (progress.status == ProcessingStatus.processing) {
              print('Processing stickers: ${progress.completedFiles}/${progress.totalFiles}');
            }
          },
            processCompleted: (pack) {
              // Navigate to preview page with the created pack
              context.go(AppRouter.preview, extra: {
                'pack': pack,
              });
            },
            whatsAppCheckCompleted: (isInstalled) {},
            addedToWhatsApp: (pack) {},
            filesValidated: (validFiles) {
              // Handle specific file validation state
              setState(() {
                _selectedFiles = validFiles.map((path) => File(path)).toList();
              });
            },
            zipExtracted: (directory, extractedCount, totalCount) {
              _scanExtractedDirectory(directory);
              
              // Show message if 30-image limit was exceeded
              if (extractedCount != null && totalCount != null && totalCount > 30) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text(
                      'ZIP contained $totalCount images, but only the first 30 were processed due to WhatsApp limitations.',
                    ),
                    backgroundColor: AppColors.warning,
                    duration: const Duration(seconds: 5),
                  ),
                );
              }
            },
            error: (message) {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: Text(message),
                  backgroundColor: AppColors.error,
                ),
              );
            },
            success: (message, pack) {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: Text(message),
                  backgroundColor: AppColors.success,
                ),
              );
            },          telegramPackMetadataLoaded: (metadata) {
            // Handle telegram pack metadata loaded if needed
            // This is called when metadata is loaded but before stickers are downloaded
          },
          telegramStickerDownloadProgress: (currentIndex, totalStickers, currentUrl, downloadedFiles, allStickers) {
            setState(() {
              _isTelegramDownloading = true;
              _telegramCurrentIndex = currentIndex;
              _telegramTotalStickers = totalStickers;
              _telegramStickers = List<Map<String, dynamic>>.from(allStickers);
              
              // Update selected files as downloads complete
              if (downloadedFiles.isNotEmpty) {
                _selectedFiles = downloadedFiles.map((path) => File(path)).toList();
              }
            });
          },
        );
        },
        child: GradientBackground(
          child: Column(
            children: [
              _buildTabBar(),
              Expanded(
                child: TabBarView(
                  controller: _tabController,
                  children: [
                    _buildImageUploadTab(),
                    _buildTelegramUrlTab(),
                    _buildZipUploadTab(),
                  ],
                ),
              ),
              _buildBottomSection(),
            ],
          ),
        ),
      ),
    );
  }
  
  PreferredSizeWidget _buildAppBar() {
    return AppBar(
      title: const Text('Create Sticker Pack'),
      centerTitle: false,
      leading: IconButton(
        icon: const Icon(Icons.arrow_back_ios_new),
        onPressed: () => context.go(AppRouter.home),
      ),
      backgroundColor: AppColors.background.withOpacity(0.8),
      elevation: 0,
    );
  }
  
  Widget _buildTabBar() {
    return Container(
      margin: const EdgeInsets.fromLTRB(16, 0, 16, 16),
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        color: AppColors.surface.withOpacity(0.8),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.outline.withOpacity(0.3)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.1),
            blurRadius: 8,
            spreadRadius: 1,
          ),
        ],
      ),
      child: Theme(
        data: Theme.of(context).copyWith(
          tabBarTheme: TabBarThemeData(
            indicator: BoxDecoration(
              gradient: LinearGradient(
                colors: AppColors.primaryGradient,
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
              borderRadius: BorderRadius.circular(12),
              boxShadow: [
                BoxShadow(
                  color: AppColors.primary.withOpacity(0.3),
                  blurRadius: 8,
                  spreadRadius: 1,
                ),
              ],
            ),
            indicatorSize: TabBarIndicatorSize.tab,
            labelColor: AppColors.onPrimary,
            unselectedLabelColor: AppColors.onSurfaceVariant,
            labelStyle: AppTextStyles.labelLarge.copyWith(
              fontWeight: FontWeight.w700,
              letterSpacing: 0.5,
            ),
            unselectedLabelStyle: AppTextStyles.labelLarge.copyWith(
              fontWeight: FontWeight.w500,
            ),
          ),
        ),
        child: TabBar(
          controller: _tabController,
          indicatorPadding: EdgeInsets.zero,
          dividerHeight: 0,
          labelPadding: const EdgeInsets.symmetric(horizontal: 8, vertical: 12),
          splashBorderRadius: BorderRadius.circular(12),
          overlayColor: WidgetStateProperty.resolveWith<Color?>(
            (Set<WidgetState> states) {
              if (states.contains(WidgetState.hovered)) {
                return AppColors.primary.withOpacity(0.1);
              }
              if (states.contains(WidgetState.pressed)) {
                return AppColors.primary.withOpacity(0.2);
              }
              return null;
            },
          ),
          tabs: [
            _buildCustomTab('Images', Icons.image_outlined),
            _buildCustomTab('Telegram', Icons.telegram_outlined),
            _buildCustomTab('ZIP', Icons.folder_zip_outlined),
          ],
        ),
      ),
    );
  }

  Widget _buildCustomTab(String text, IconData icon) {
    return Tab(
      height: 48,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 4),
        child: LayoutBuilder(
          builder: (context, constraints) {
            // Check if we have enough space for icon + text
            final hasSpace = constraints.maxWidth > 80;
            
            if (hasSpace) {
              return Row(
                mainAxisAlignment: MainAxisAlignment.center,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    icon,
                    size: 16,
                  ),
                  const SizedBox(width: 6),
                  Flexible(
                    child: Text(
                      text,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(fontSize: 13),
                    ),
                  ),
                ],
              );
            } else {
              // Show only icon for very small spaces
              return Icon(
                icon,
                size: 18,
              );
            }
          },
        ),
      ),
    );
  }
  
  Widget _buildImageUploadTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
      child: Column(
        children: [
          if (_selectedFiles.isEmpty)
            UploadArea(
              onFilesSelected: _handleFileSelection,
              acceptedTypes: const ['jpg', 'png', 'webp'],
              title: 'Upload Images',
              subtitle: 'Select up to 30 images for your sticker pack',
            )
          else
            SelectedFilesList(
              files: _selectedFiles,
              onRemoveFile: _removeFile,
              onAddMore: () => _handleFileSelection([]),
            ),
          if (_selectedFiles.isNotEmpty) ...[
            const SizedBox(height: 24),
            _buildPackConfiguration(),
          ],
        ],
      ),
    );
  }
  
  Widget _buildTelegramUrlTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
      child: Column(
        children: [
          TelegramUrlInput(
            controller: _telegramUrlController,
            onUrlChanged: (_) {},
          ),
          
          if (_telegramMetadataLoading && !_isTelegramDownloading)
            const Padding(
              padding: EdgeInsets.all(24),
              child: Center(child: CircularProgressIndicator()),
            ),
            
          if (_telegramPackError != null)
            Padding(
              padding: const EdgeInsets.all(24),
              child: Text(
                _telegramPackError!,
                style: TextStyle(color: AppColors.error),
                textAlign: TextAlign.center,
              ),
            ),
          
          // Show Telegram download progress grid
          if (_isTelegramDownloading && _telegramStickers.isNotEmpty) ...[
            const SizedBox(height: 24),
            _buildTelegramProgressGrid(),
          ]
          // Show final sticker selection once download is complete  
          else if (_selectedFiles.isNotEmpty && !_isTelegramDownloading) ...[
            const SizedBox(height: 24),
            _buildTelegramStickerSelection(),
          ],
          
          if (_selectedFiles.isNotEmpty || _isTelegramDownloading) ...[
            const SizedBox(height: 24),
            _buildPackConfiguration(),
          ],
        ],
      ),
    );
  }
  // Unified ZIP upload tab
  Widget _buildZipUploadTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
      child: Column(
        children: [
          if (_selectedFiles.isEmpty)
            UploadArea(
              onFilesSelected: _handleZipSelection,
              acceptedTypes: const ['zip'],
              title: 'Upload ZIP Archive',
              subtitle: 'Select a single ZIP file containing your images\n(First 30 images will be processed)',
            )
          else
            SelectedFilesList(
              files: _selectedFiles,
              onRemoveFile: _removeFile,
              onAddMore: () => _handleZipSelection([]),
            ),
          if (_selectedFiles.isNotEmpty) ...[
            const SizedBox(height: 24),
            _buildPackConfiguration(),
          ],
        ],
      ),
    );
  }
  
  Widget _buildPackConfiguration() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppColors.surface.withOpacity(0.5),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.outline.withOpacity(0.2)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Pack Details',
            style: AppTextStyles.h6.copyWith(
              fontWeight: FontWeight.bold,
              color: AppColors.onSurface,
            ),
          ),
          const SizedBox(height: 20),
          TextField(
            controller: _packNameController,
            inputFormatters: [
              FilteringTextInputFormatter.deny(
                RegExp(r'[\u{1f600}-\u{1f64f}]|[\u{1f300}-\u{1f5ff}]|[\u{1f680}-\u{1f6ff}]|[\u{1f1e0}-\u{1f1ff}]|[\u{2600}-\u{26ff}]|[\u{2700}-\u{27bf}]', unicode: true),
              ),
            ],
            maxLength: 128,
            decoration: const InputDecoration(
              labelText: 'Pack Name',
              hintText: 'Enter pack name (no emojis)',
              counterText: '',
            ),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _publisherController,
            inputFormatters: [
              FilteringTextInputFormatter.deny(
                RegExp(r'[\u{1f600}-\u{1f64f}]|[\u{1f300}-\u{1f5ff}]|[\u{1f680}-\u{1f6ff}]|[\u{1f1e0}-\u{1f1ff}]|[\u{2600}-\u{26ff}]|[\u{2700}-\u{27bf}]', unicode: true),
              ),
            ],
            maxLength: 128,
            decoration: const InputDecoration(
              labelText: 'Publisher',
              hintText: 'Enter publisher name (no emojis)',
              counterText: '',
            ),
          ),
        ],
      ),
    );
  }
  
  Widget _buildBottomSection() {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: AppColors.surface.withOpacity(0.8),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.1),
            blurRadius: 20,
            spreadRadius: 5,
          )
        ],
        border: Border(top: BorderSide(color: AppColors.outline.withOpacity(0.2)))
      ),
      child: BlocBuilder<StickerConverterBloc, StickerConverterState>(
        builder: (context, state) {
          final isLoading = state.when(
            (isLoading, isWhatsAppInstalled, isProcessing, currentPack, processingProgress, validatedFiles, extractedDirectory, error, successMessage) => isLoading || isProcessing,
            initial: () => false,
            loading: () => true,
            processing: (progress) => true,
            processCompleted: (pack) => false,
            whatsAppCheckCompleted: (isInstalled) => false,
            addedToWhatsApp: (pack) => false,
            filesValidated: (validFiles) => false,
            zipExtracted: (directory, extractedCount, totalCount) => false,
            error: (message) => false,
            success: (message, pack) => false,
            telegramPackMetadataLoaded: (metadata) => false,
            telegramStickersDownloaded: (filePaths, packName, packTitle) => false,
            telegramStickerDownloadProgress: (currentIndex, totalStickers, currentUrl, downloadedFiles, allStickers) => true,
          );
          
          return SizedBox(
            width: double.infinity,
            child: ElevatedButton.icon(
              onPressed: _canProceed() && !isLoading ? _handleProceed : null,
              icon: isLoading
                  ? const SizedBox.shrink()
                  : const Icon(Icons.auto_fix_high_rounded),
              label: isLoading
                  ? const SizedBox(
                      height: 24,
                      width: 24,
                      child: CircularProgressIndicator(
                        strokeWidth: 3,
                        valueColor: AlwaysStoppedAnimation<Color>(
                          AppColors.onPrimary,
                        ),
                      ),
                    )
                  : Text(
                      'Create Sticker Pack',
                      style: AppTextStyles.button,
                    ),
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 18),
              ),
            ),
          );
        },
      ),
    );
  }
  
  void _handleFileSelection(List<File> files) async {
    if (files.isNotEmpty) {
      // Apply 30-image limit for drag and drop
      const maxImages = 30;
      final limitedFiles = files.take(maxImages).toList();
      
      // Show warning if we hit the limit
      if (files.length > maxImages) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              'You provided ${files.length} images, but only the first $maxImages will be processed due to WhatsApp limitations.',
            ),
            backgroundColor: AppColors.warning,
            duration: const Duration(seconds: 5),
          ),
        );
      }
      
      context.read<StickerConverterBloc>().add(
        StickerConverterEvent.validateFiles(files: limitedFiles),
      );
    } else {
      // Open file picker
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['jpg', 'jpeg', 'png', 'gif', 'webp'],
        allowMultiple: true,
      );
      
      if (result != null) {
        final selectedFiles = result.paths
            .where((path) => path != null)
            .map((path) => File(path!))
            .toList();
        
        // Apply 30-image limit for manual selection
        const maxImages = 30;
        final limitedFiles = selectedFiles.take(maxImages).toList();
        
        // Show warning if we hit the limit
        if (selectedFiles.length > maxImages) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(
                'You selected ${selectedFiles.length} images, but only the first $maxImages will be processed due to WhatsApp limitations.',
              ),
              backgroundColor: AppColors.warning,
              duration: const Duration(seconds: 5),
            ),
          );
        }
        
        context.read<StickerConverterBloc>().add(
          StickerConverterEvent.validateFiles(files: limitedFiles),
        );
      }
    }
  }
  
  void _handleZipSelection(List<File> files) async {
    if (files.isNotEmpty && files.first.path.endsWith('.zip')) {
      context.read<StickerConverterBloc>().add(
        StickerConverterEvent.extractZipFile(zipFile: files.first),
      );
    } else {
      // Open file picker for ZIP
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['zip'],
      );
      
      if (result != null && result.files.single.path != null) {
        final zipFile = File(result.files.single.path!);
        context.read<StickerConverterBloc>().add(
          StickerConverterEvent.extractZipFile(zipFile: zipFile),
        );
      }
    }
  }
  
  void _removeFile(File file) {
    setState(() {
      _selectedFiles.remove(file);
    });
  }
  
  void _scanExtractedDirectory(String directory) async {
    try {
      print('Scanning extracted directory: $directory');
      
      final dir = Directory(directory);
      if (!await dir.exists()) {
        print('Directory does not exist: $directory');
        return;
      }
      
      final entities = await dir.list().toList();
      print('Found ${entities.length} entities in directory');
      
      final files = entities
          .where((entity) => entity is File)
          .cast<File>()
          .where((file) {
            final fileName = file.path.split(Platform.pathSeparator).last;
            final extension = fileName.toLowerCase().split('.').last;
            final isValidExtension = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].contains(extension);
            
            print('Checking file: $fileName, extension: $extension, valid: $isValidExtension');
            return isValidExtension;
          })
          .toList();
      
      print('Found ${files.length} valid image files');
      
      if (files.isNotEmpty) {
        // Validate each file to ensure it's a valid image
        final validFiles = <File>[];
        
        for (final file in files) {
          try {
            // Check if file exists and has content
            if (await file.exists()) {
              final size = await file.length();
              print('File ${file.path} exists with size: $size bytes');
              
              if (size > 0) {
                validFiles.add(file);
              } else {
                print('File ${file.path} is empty, skipping');
              }
            } else {
              print('File ${file.path} does not exist, skipping');
            }
          } catch (e) {
            print('Error checking file ${file.path}: ${e.toString()}');
          }
        }
        
        print('Final valid files count: ${validFiles.length}');
        
        if (validFiles.isNotEmpty) {
          // The ZIP extraction already limits to 30 files, but just to be safe and provide UI feedback
          const maxImages = 30;
          final limitedFiles = validFiles.take(maxImages).toList();
          
          context.read<StickerConverterBloc>().add(
            StickerConverterEvent.validateFiles(files: limitedFiles),
          );
          
          // Show info about WebP files if any are present
          final hasWebP = validFiles.any((file) => 
            file.path.toLowerCase().endsWith('.webp'));
          
          if (hasWebP) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(
                content: Text('WebP files detected. Preview may not work, but they will be processed correctly.'),
                backgroundColor: AppColors.info,
                duration: Duration(seconds: 4),
              ),
            );
          }
        } else {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('No valid image files found in the extracted ZIP'),
              backgroundColor: AppColors.error,
            ),
          );
        }
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('No image files found in the ZIP archive'),
            backgroundColor: AppColors.warning,
          ),
        );
      }
    } catch (e) {
      print('Error scanning extracted directory: ${e.toString()}');
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Error processing extracted files: ${e.toString()}'),
          backgroundColor: AppColors.error,
        ),
      );
    }
  }
  
  bool _canProceed() {
    switch (_currentInputType) {
      case 'telegram':
        return _selectedFiles.isNotEmpty &&
            _packNameController.text.isNotEmpty &&
            !_telegramMetadataLoading;
      case 'images':
      case 'zip':
        return _selectedFiles.isNotEmpty &&
            _packNameController.text.isNotEmpty;
      default:
        return false;
    }
  }
  
  void _handleProceed() {
    // All upload types now use the same unified approach
    context.read<StickerConverterBloc>().add(
      StickerConverterEvent.processImages(
        images: _selectedFiles,
        packName: _packNameController.text,
        publisher: _publisherController.text,
      ),
    );
  }

  Widget _buildTelegramProgressGrid() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppColors.surface.withOpacity(0.5),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.outline.withOpacity(0.2)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text(
                'Downloading Stickers',
                style: AppTextStyles.h6.copyWith(
                  fontWeight: FontWeight.bold,
                  color: AppColors.onSurface,
                ),
              ),
              const Spacer(),
              Text(
                '${_telegramCurrentIndex}/${_telegramTotalStickers}',
                style: AppTextStyles.bodyMedium.copyWith(
                  color: AppColors.onSurfaceVariant,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          LinearProgressIndicator(
            value: _telegramTotalStickers > 0 ? _telegramCurrentIndex / _telegramTotalStickers : 0,
            backgroundColor: AppColors.outline.withOpacity(0.3),
            valueColor: AlwaysStoppedAnimation<Color>(AppColors.primary),
          ),
          const SizedBox(height: 20),
          
          // Grid of sticker progress indicators
          GridView.builder(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 5,
              childAspectRatio: 1,
              crossAxisSpacing: 8,
              mainAxisSpacing: 8,
            ),
            itemCount: _telegramStickers.length,
            itemBuilder: (context, index) {
              final sticker = _telegramStickers[index];
              final status = sticker['status'] as String;
              final progress = sticker['progress'] as double;
              final localPath = sticker['local_path'] as String?;
              
              return Container(
                decoration: BoxDecoration(
                  color: AppColors.surface,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(
                    color: status == 'completed' 
                      ? AppColors.success 
                      : status == 'downloading'
                        ? AppColors.primary
                        : status == 'error'
                          ? AppColors.error
                          : AppColors.outline.withOpacity(0.3),
                    width: 2,
                  ),
                ),
                child: Stack(
                  children: [
                    // Sticker preview if available
                    if (localPath != null && status == 'completed')
                      ClipRRect(
                        borderRadius: BorderRadius.circular(6),
                        child: Image.file(
                          File(localPath),
                          width: double.infinity,
                          height: double.infinity,
                          fit: BoxFit.cover,
                          errorBuilder: (context, error, stackTrace) {
                            return Container(
                              color: AppColors.surfaceVariant,
                              child: Icon(
                                Icons.image_not_supported,
                                color: AppColors.onSurfaceVariant,
                                size: 20,
                              ),
                            );
                          },
                        ),
                      )
                    else
                      Container(
                        color: AppColors.surfaceVariant.withOpacity(0.3),
                        child: Center(
                          child: Icon(
                            Icons.image,
                            color: AppColors.onSurfaceVariant.withOpacity(0.5),
                            size: 20,
                          ),
                        ),
                      ),
                    
                    // Progress overlay
                    if (status == 'downloading')
                      Container(
                        decoration: BoxDecoration(
                          color: AppColors.surface.withOpacity(0.8),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Center(
                          child: SizedBox(
                            width: 24,
                            height: 24,
                            child: CircularProgressIndicator(
                              value: progress,
                              strokeWidth: 3,
                              valueColor: AlwaysStoppedAnimation<Color>(AppColors.primary),
                            ),
                          ),
                        ),
                      ),
                    
                    // Status indicator
                    Positioned(
                      top: 4,
                      right: 4,
                      child: Container(
                        width: 16,
                        height: 16,
                        decoration: BoxDecoration(
                          color: status == 'completed' 
                            ? AppColors.success 
                            : status == 'downloading'
                              ? AppColors.primary
                              : status == 'error'
                                ? AppColors.error
                                : AppColors.outline.withOpacity(0.5),
                          shape: BoxShape.circle,
                        ),
                        child: Icon(
                          status == 'completed' 
                            ? Icons.check 
                            : status == 'error'
                              ? Icons.close
                              : null,
                          size: 10,
                          color: AppColors.onPrimary,
                        ),
                      ),
                    ),
                  ],
                ),
              );
            },
          ),
        ],
      ),
    );
  }

  Widget _buildTelegramStickerSelection() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppColors.surface.withOpacity(0.5),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.outline.withOpacity(0.2)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text(
                'Select Stickers',
                style: AppTextStyles.h6.copyWith(
                  fontWeight: FontWeight.bold,
                  color: AppColors.onSurface,
                ),
              ),
              const Spacer(),
              Text(
                '${_selectedFiles.length}/${AppConstants.maxStickersInPack} selected',
                style: AppTextStyles.bodyMedium.copyWith(
                  color: AppColors.onSurfaceVariant,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            'First ${AppConstants.maxStickersInPack} stickers are selected by default. Tap to toggle selection.',
            style: AppTextStyles.bodySmall.copyWith(
              color: AppColors.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 20),
          
          // Use the existing SelectedFilesList widget with some modifications
          SelectedFilesList(
            files: _selectedFiles,
            onRemoveFile: _removeFile,
            onAddMore: () {
              // For Telegram, we don't allow adding more files manually
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  content: Text('To add more stickers, use a different Telegram pack URL'),
                ),
              );
            },
          ),
        ],
      ),
    );
  }
}
