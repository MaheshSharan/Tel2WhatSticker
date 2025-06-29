import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import 'package:file_picker/file_picker.dart';
import '../../../../core/router/app_router.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_text_styles.dart';
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
  
  @override
  void initState() {
    super.initState();
    _currentInputType = widget.inputType;
    _tabController = TabController(
      length: 3,
      vsync: this,
      initialIndex: _getInitialTabIndex(),
    );
    
    _packNameController.text = 'My Sticker Pack';
    _publisherController.text = 'Sticker Creator';
    
    _tabController.addListener(() {
      setState(() {
        _currentInputType = _getInputTypeFromIndex(_tabController.index);
        _selectedFiles.clear();
      });
    });
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
    _telegramUrlController.dispose();
    super.dispose();
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
            
            // Specific states
            initial: () {},
            loading: () {},
            processing: (progress) {
              // Show processing feedback if needed
              if (progress.status == ProcessingStatus.processing) {
                // Could add a snackbar or other UI feedback here
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
      child: Column(
        children: [
          TelegramUrlInput(
            controller: _telegramUrlController,
            onUrlChanged: _validateTelegramUrl,
          ),
          if (_telegramUrlController.text.isNotEmpty) ...[
            Padding(
              padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
              child: _buildPackConfiguration(),
            ),
          ],
        ],
      ),
    );
  }
  
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
            decoration: const InputDecoration(
              labelText: 'Pack Name',
              hintText: 'e.g., My Awesome Stickers',
            ),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _publisherController,
            decoration: const InputDecoration(
              labelText: 'Publisher',
              hintText: 'e.g., Your Name',
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
          final isLoading = state.maybeWhen(
            (isLoading, isWhatsAppInstalled, isProcessing, currentPack, processingProgress, validatedFiles, extractedDirectory, error, successMessage) => isLoading || isProcessing,
            loading: () => true,
            processing: (progress) => true,
            orElse: () => false,
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
  
  void _validateTelegramUrl(String url) {
    // Real-time validation can be added here
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
        return _telegramUrlController.text.isNotEmpty &&
            _packNameController.text.isNotEmpty;
      case 'images':
      case 'zip':
        return _selectedFiles.isNotEmpty &&
            _packNameController.text.isNotEmpty;
      default:
        return false;
    }
  }
  
  void _handleProceed() {
    switch (_currentInputType) {
      case 'telegram':
        context.read<StickerConverterBloc>().add(
          StickerConverterEvent.processTelegramUrl(
            url: _telegramUrlController.text,
            customPackName: _packNameController.text,
            customPublisher: _publisherController.text,
          ),
        );
        break;
      case 'images':
      case 'zip':
        context.read<StickerConverterBloc>().add(
          StickerConverterEvent.processImages(
            images: _selectedFiles,
            packName: _packNameController.text,
            publisher: _publisherController.text,
          ),
        );
        break;
    }
  }
}
