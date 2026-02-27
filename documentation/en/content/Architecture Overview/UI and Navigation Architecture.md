# UI and Navigation Architecture

<cite>
**Referenced Files in This Document**
- [nav_graph.xml](file://app/src/main/res/navigation/nav_graph.xml)
- [activity_main.xml](file://app/src/main/res/layout/activity_main.xml)
- [bottom_nav_menu.xml](file://app/src/main/res/menu/bottom_nav_menu.xml)
- [MainActivity.kt](file://app/src/main/java/com/maheshsharan/tel2what/MainActivity.kt)
- [SplashFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/splash/SplashFragment.kt)
- [OnboardingFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/onboarding/OnboardingFragment.kt)
- [HomeFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/home/HomeFragment.kt)
- [TelegramImportFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/importpack/TelegramImportFragment.kt)
- [DownloadConversionFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/conversion/DownloadConversionFragment.kt)
- [StickerSelectionFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/selection/StickerSelectionFragment.kt)
- [TrayIconSelectionFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/trayicon/TrayIconSelectionFragment.kt)
- [ExportFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/export/ExportFragment.kt)
- [ManualUploadFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/manual/ManualUploadFragment.kt)
- [StorageManagementFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/storage/StorageManagementFragment.kt)
- [SettingsFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/settings/SettingsFragment.kt)
</cite>

## Table of Contents
1. [Introduction](#introduction)
2. [Project Structure](#project-structure)
3. [Core Components](#core-components)
4. [Architecture Overview](#architecture-overview)
5. [Detailed Component Analysis](#detailed-component-analysis)
6. [Dependency Analysis](#dependency-analysis)
7. [Performance Considerations](#performance-considerations)
8. [Troubleshooting Guide](#troubleshooting-guide)
9. [Conclusion](#conclusion)

## Introduction
This document explains the UI and navigation architecture of Tel2What, focusing on the Navigation Component implementation, the navigation graph, and the user journeys across import, conversion, selection, and export. It also covers fragment lifecycle management, state preservation, memory optimization, bottom navigation integration, back stack behavior, onboarding and splash handling, and fragment-to-fragment communication via safe args and shared view models. Deep linking and navigation patterns are addressed conceptually, along with practical guidance for extending the system.

## Project Structure
The app uses Android’s Navigation Component with a single-activity architecture. The NavHost is declared in the activity layout and bound to a navigation graph that defines all screens and transitions. Feature fragments are organized under dedicated packages for import, conversion, selection, export, storage, settings, onboarding, home, and splash.

```mermaid
graph TB
A["MainActivity<br/>activity_main.xml"] --> B["NavHostFragment<br/>navGraph=nav_graph.xml"]
B --> C["SplashFragment"]
C --> D["OnboardingFragment"]
C --> E["HomeFragment"]
D --> E
E --> F["TelegramImportFragment"]
E --> G["ManualUploadFragment"]
E --> H["StorageManagementFragment"]
E --> I["SettingsFragment"]
F --> J["DownloadConversionFragment"]
G --> K["StickerSelectionFragment"]
J --> K
K --> L["TrayIconSelectionFragment"]
L --> M["ExportFragment"]
H --> E
I --> E
```

**Diagram sources**
- [activity_main.xml](file://app/src/main/res/layout/activity_main.xml#L8-L14)
- [nav_graph.xml](file://app/src/main/res/navigation/nav_graph.xml#L1-L122)

**Section sources**
- [activity_main.xml](file://app/src/main/res/layout/activity_main.xml#L1-L16)
- [nav_graph.xml](file://app/src/main/res/navigation/nav_graph.xml#L1-L122)

## Core Components
- Navigation Graph: Central definition of all destinations and actions, including start destination and back stack behavior.
- NavHost: Hosts the navigation container and binds to the navigation graph.
- Fragments: Feature-specific screens implementing UI and navigation actions.
- Bottom Navigation: Integrated in HomeFragment to switch between primary destinations.
- ViewModel Factories: Provide repositories and context to view models per feature.

Key responsibilities:
- Navigation Graph: Defines user flows, transitions, and back stack policies.
- NavHost: Manages fragment transactions and back stack.
- Fragments: Handle UI, observe view models, and trigger navigation.
- Bottom Navigation: Provides quick access to secondary destinations from Home.

**Section sources**
- [nav_graph.xml](file://app/src/main/res/navigation/nav_graph.xml#L1-L122)
- [activity_main.xml](file://app/src/main/res/layout/activity_main.xml#L8-L14)
- [HomeFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/home/HomeFragment.kt#L82-L97)

## Architecture Overview
The navigation architecture follows a layered pattern:
- Activity hosts the NavHost.
- NavHost delegates to the navigation graph.
- Graph defines destinations and actions.
- Fragments implement UI and call findNavController().navigate or navigateUp().
- Bottom navigation triggers navigation actions directly.

```mermaid
sequenceDiagram
participant Act as "MainActivity"
participant Host as "NavHostFragment"
participant Graph as "nav_graph.xml"
participant Splash as "SplashFragment"
participant Onb as "OnboardingFragment"
participant Home as "HomeFragment"
Act->>Host : "Set content view"
Host->>Graph : "Load startDestination"
Graph-->>Splash : "Navigate to splash"
Splash->>Splash : "Show animations"
Splash->>Graph : "Decide next destination"
alt "First launch"
Graph-->>Onb : "Navigate to onboarding"
else "Returning user"
Graph-->>Home : "Navigate to home"
end
```

**Diagram sources**
- [MainActivity.kt](file://app/src/main/java/com/maheshsharan/tel2what/MainActivity.kt#L6-L11)
- [activity_main.xml](file://app/src/main/res/layout/activity_main.xml#L8-L14)
- [nav_graph.xml](file://app/src/main/res/navigation/nav_graph.xml#L6-L24)
- [SplashFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/splash/SplashFragment.kt#L63-L76)

## Detailed Component Analysis

### Navigation Graph and User Flows
The navigation graph defines:
- Start destination: splashFragment.
- Conditional routing after splash based on onboarding completion.
- Hierarchical flows: import → conversion → selection → tray icon → export.
- Bottom navigation shortcuts from home to storage and settings.
- Back stack behavior using popUpTo and popUpToInclusive to avoid redundant entries.

```mermaid
flowchart TD
Start(["Splash"]) --> CheckOnb{"Onboarding completed?"}
CheckOnb --> |No| ToOnb["Navigate to OnboardingFragment"]
CheckOnb --> |Yes| ToHome["Navigate to HomeFragment"]
ToOnb --> Home
ToHome --> Home
Home --> Import["TelegramImportFragment"]
Home --> Manual["ManualUploadFragment"]
Home --> Storage["StorageManagementFragment"]
Home --> Settings["SettingsFragment"]
Import --> Conv["DownloadConversionFragment"]
Manual --> Sel["StickerSelectionFragment"]
Conv --> Sel
Sel --> Tray["TrayIconSelectionFragment"]
Tray --> Export["ExportFragment"]
Export --> Home
```

**Diagram sources**
- [nav_graph.xml](file://app/src/main/res/navigation/nav_graph.xml#L6-L122)

**Section sources**
- [nav_graph.xml](file://app/src/main/res/navigation/nav_graph.xml#L1-L122)

### Splash Screen Handling
- Animates UI elements progressively.
- Waits briefly, then checks a shared preference to decide next destination.
- Navigates either to onboarding or home based on onboarding completion.

```mermaid
sequenceDiagram
participant Splash as "SplashFragment"
participant Prefs as "SharedPreferences"
participant Nav as "NavController"
Splash->>Splash : "Animate logo/title/subtitle/version"
Splash->>Splash : "delay(1500ms)"
Splash->>Prefs : "Read onboarding_complete"
alt "Not completed"
Splash->>Nav : "navigate(action_splash_to_onboarding)"
else "Completed"
Splash->>Nav : "navigate(action_splash_to_home)"
end
```

**Diagram sources**
- [SplashFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/splash/SplashFragment.kt#L63-L76)
- [nav_graph.xml](file://app/src/main/res/navigation/nav_graph.xml#L13-L23)

**Section sources**
- [SplashFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/splash/SplashFragment.kt#L1-L79)

### Onboarding Flow
- ViewPager2 pages guide the user through three steps.
- Next button switches pages or completes onboarding.
- Completing onboarding writes a flag and navigates to home.

```mermaid
sequenceDiagram
participant Onb as "OnboardingFragment"
participant Pager as "ViewPager2"
participant Nav as "NavController"
Onb->>Pager : "Set adapter"
Pager-->>Onb : "Page selected"
Onb->>Onb : "Update UI (Next/Skip)"
Onb->>Onb : "If last page, write onboarding_complete=true"
Onb->>Nav : "navigate(action_onboarding_to_home)"
```

**Diagram sources**
- [OnboardingFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/onboarding/OnboardingFragment.kt#L35-L67)
- [nav_graph.xml](file://app/src/main/res/navigation/nav_graph.xml#L31-L35)

**Section sources**
- [OnboardingFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/onboarding/OnboardingFragment.kt#L1-L69)

### Home Fragment and Bottom Navigation
- Initializes ViewModel with repository and network clients.
- Sets up recent packs list and bottom navigation item selection.
- Bottom navigation routes to storage and settings; home remains selected.

```mermaid
sequenceDiagram
participant Home as "HomeFragment"
participant Bottom as "BottomNavigationView"
participant Nav as "NavController"
Home->>Bottom : "Set selected item to nav_home"
Bottom->>Home : "onNavigationItemSelected(itemId)"
alt "nav_packs"
Home->>Nav : "navigate(action_home_to_storage)"
else "nav_settings"
Home->>Nav : "navigate(action_home_to_settings)"
else "nav_home"
Home->>Home : "no-op"
end
```

**Diagram sources**
- [HomeFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/home/HomeFragment.kt#L82-L97)
- [bottom_nav_menu.xml](file://app/src/main/res/menu/bottom_nav_menu.xml#L1-L15)

**Section sources**
- [HomeFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/home/HomeFragment.kt#L1-L106)
- [bottom_nav_menu.xml](file://app/src/main/res/menu/bottom_nav_menu.xml#L1-L15)

### Telegram Import Flow
- Accepts a Telegram sticker pack link, fetches metadata, and previews details.
- Supports clipboard paste and focus change to trigger fetch.
- On success, passes pack metadata via arguments to conversion.

```mermaid
sequenceDiagram
participant Import as "TelegramImportFragment"
participant VM as "TelegramImportViewModel"
participant Nav as "NavController"
Import->>VM : "fetchPackMetadata(link)"
VM-->>Import : "State.Success with pack info"
Import->>Nav : "navigate(action_import_to_conversion, args)"
```

**Diagram sources**
- [TelegramImportFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/importpack/TelegramImportFragment.kt#L94-L151)
- [nav_graph.xml](file://app/src/main/res/navigation/nav_graph.xml#L60-L62)

**Section sources**
- [TelegramImportFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/importpack/TelegramImportFragment.kt#L1-L154)

### Conversion and Batch Progress
- Receives pack metadata from import.
- Observes progress and updates UI (ETA, speed, percentage).
- Enables “download more” and “continue” based on thresholds.
- Uses navigateUp() for back navigation.

```mermaid
sequenceDiagram
participant Conv as "DownloadConversionFragment"
participant VM as "ConversionViewModel"
participant Nav as "NavController"
Conv->>VM : "initAndStart(packName, packTitle)"
VM-->>Conv : "progressData (batch, ETA, speed)"
Conv->>Conv : "Update UI (progress, ETA, speed)"
Conv->>VM : "downloadNextBatch()"
Conv->>Nav : "navigate(action_conversion_to_selection, args)"
```

**Diagram sources**
- [DownloadConversionFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/conversion/DownloadConversionFragment.kt#L62-L124)
- [nav_graph.xml](file://app/src/main/res/navigation/nav_graph.xml#L69-L72)

**Section sources**
- [DownloadConversionFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/conversion/DownloadConversionFragment.kt#L1-L139)

### Sticker Selection
- Loads stickers for a given pack and allows selection.
- Enforces selection bounds (3–30).
- Passes selected IDs to tray icon selection.

```mermaid
sequenceDiagram
participant Sel as "StickerSelectionFragment"
participant VM as "SelectionViewModel"
participant Nav as "NavController"
Sel->>VM : "loadStickers(packName)"
VM-->>Sel : "stickers list"
Sel->>VM : "toggleSelection / selectAll"
Sel->>VM : "getSelectedStickerIds()"
Sel->>Nav : "navigate(action_selection_to_tray, args)"
```

**Diagram sources**
- [StickerSelectionFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/selection/StickerSelectionFragment.kt#L53-L91)
- [nav_graph.xml](file://app/src/main/res/navigation/nav_graph.xml#L78-L81)

**Section sources**
- [StickerSelectionFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/selection/StickerSelectionFragment.kt#L1-L93)

### Tray Icon Selection and Customization
- Presents tray icon options and allows selecting a custom image.
- Processes and saves the custom tray icon to app storage.
- Navigates to export upon successful save or skips to export.

```mermaid
sequenceDiagram
participant Tray as "TrayIconSelectionFragment"
participant VM as "TrayIconViewModel"
participant Repo as "StickerRepository"
participant Nav as "NavController"
Tray->>VM : "loadSelectedStickers(packName, selectedIds)"
VM-->>Tray : "stickers list"
Tray->>VM : "selectIcon(path) or saveTrayIconAndContinue()"
VM->>Repo : "update pack tray icon"
Repo-->>VM : "success"
VM-->>Tray : "isSaved=true"
Tray->>Nav : "navigate(action_tray_to_export, args)"
```

**Diagram sources**
- [TrayIconSelectionFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/trayicon/TrayIconSelectionFragment.kt#L71-L119)
- [nav_graph.xml](file://app/src/main/res/navigation/nav_graph.xml#L87-L90)

**Section sources**
- [TrayIconSelectionFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/trayicon/TrayIconSelectionFragment.kt#L1-L164)

### Export and WhatsApp Integration
- Loads pack details and displays tray preview.
- Validates required fields and updates pack metadata.
- Launches WhatsApp intent to enable the sticker pack and returns to home.

```mermaid
sequenceDiagram
participant Exp as "ExportFragment"
participant VM as "ExportViewModel"
participant WA as "WhatsApp"
participant Nav as "NavController"
Exp->>VM : "loadPackDetails(packName)"
VM-->>Exp : "pack info"
Exp->>VM : "updatePackDetailsAndSave(name, author)"
VM-->>Exp : "success"
Exp->>WA : "Start activity with ENABLE_STICKER_PACK"
WA-->>Exp : "returns"
Exp->>Nav : "navigate(action_export_to_home)"
```

**Diagram sources**
- [ExportFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/export/ExportFragment.kt#L87-L111)
- [nav_graph.xml](file://app/src/main/res/navigation/nav_graph.xml#L96-L100)

**Section sources**
- [ExportFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/export/ExportFragment.kt#L1-L113)

### Manual Upload Flow
- Allows selecting up to 30 images.
- Processes files and navigates to selection upon success.

```mermaid
sequenceDiagram
participant Manual as "ManualUploadFragment"
participant VM as "ManualUploadViewModel"
participant Nav as "NavController"
Manual->>VM : "addFiles(uris) / processFiles()"
VM-->>Manual : "processSuccess(packId)"
Manual->>Nav : "navigate(action_manual_to_selection, args)"
```

**Diagram sources**
- [ManualUploadFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/manual/ManualUploadFragment.kt#L78-L88)
- [nav_graph.xml](file://app/src/main/res/navigation/nav_graph.xml#L107-L110)

**Section sources**
- [ManualUploadFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/manual/ManualUploadFragment.kt#L1-L111)

### Storage Management
- Lists stored packs, supports clearing cache and deleting packs.
- Integrates with Glide disk cache and clears app cache safely.

```mermaid
flowchart TD
SM["StorageManagementFragment"] --> List["Load storage info"]
SM --> ClearCache["Clear app cache"]
SM --> DeleteOne["Delete selected pack"]
SM --> DeleteAll["Delete all packs"]
```

**Diagram sources**
- [StorageManagementFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/storage/StorageManagementFragment.kt#L62-L106)

**Section sources**
- [StorageManagementFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/storage/StorageManagementFragment.kt#L1-L118)

### Settings
- Opens external links for GitHub, privacy policy, terms, and licenses.
- Shows current app version.

```mermaid
sequenceDiagram
participant Settings as "SettingsFragment"
participant Browser as "Browser"
Settings->>Browser : "Open URL on card click"
Browser-->>Settings : "Return"
```

**Diagram sources**
- [SettingsFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/settings/SettingsFragment.kt#L34-L58)

**Section sources**
- [SettingsFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/settings/SettingsFragment.kt#L1-L60)

## Dependency Analysis
- Activity depends on NavHost and navigation graph.
- Fragments depend on view models and repositories.
- Navigation graph defines explicit dependencies between destinations.
- Bottom navigation is coupled to HomeFragment.

```mermaid
graph LR
MainActivity["MainActivity"] --> NavHost["NavHostFragment"]
NavHost --> NavGraph["nav_graph.xml"]
NavGraph --> Splash["SplashFragment"]
NavGraph --> Onboarding["OnboardingFragment"]
NavGraph --> Home["HomeFragment"]
Home --> Storage["StorageManagementFragment"]
Home --> Settings["SettingsFragment"]
Home --> Import["TelegramImportFragment"]
Import --> Conv["DownloadConversionFragment"]
Conv --> Selection["StickerSelectionFragment"]
Selection --> Tray["TrayIconSelectionFragment"]
Tray --> Export["ExportFragment"]
```

**Diagram sources**
- [activity_main.xml](file://app/src/main/res/layout/activity_main.xml#L8-L14)
- [nav_graph.xml](file://app/src/main/res/navigation/nav_graph.xml#L1-L122)

**Section sources**
- [activity_main.xml](file://app/src/main/res/layout/activity_main.xml#L1-L16)
- [nav_graph.xml](file://app/src/main/res/navigation/nav_graph.xml#L1-L122)

## Performance Considerations
- Lifecycle-aware observation: All fragments collect flows inside viewLifecycleOwner lifecycleScope to avoid leaks and unnecessary work.
- Adapter detachment: HomeFragment clears RecyclerView adapters in onDestroyView to prevent memory retention.
- Background work: Tray icon customization runs on Dispatchers.IO and updates UI on main thread.
- Cache management: Storage screen clears Glide disk cache and app cache safely.
- UI responsiveness: Buttons and cards are disabled during processing to avoid invalid states.

[No sources needed since this section provides general guidance]

## Troubleshooting Guide
- Navigation does not return to previous screen:
  - Ensure navigateUp() is used for back navigation in conversion and selection fragments.
- Bottom navigation not switching destinations:
  - Verify selectedItemId initialization and item selection listener logic in HomeFragment.
- Export not opening WhatsApp:
  - Confirm intent action and extras; handle ActivityNotFoundException gracefully.
- Onboarding not completing:
  - Check onboarding preference write and navigation action.
- Splash stuck or incorrect routing:
  - Validate onboarding preference read and navigation actions.

**Section sources**
- [HomeFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/home/HomeFragment.kt#L82-L97)
- [ExportFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/export/ExportFragment.kt#L98-L111)
- [OnboardingFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/onboarding/OnboardingFragment.kt#L63-L67)
- [SplashFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/splash/SplashFragment.kt#L67-L76)

## Conclusion
Tel2What’s UI and navigation architecture centers on a clear navigation graph, a single-activity NavHost setup, and modular fragments that encapsulate feature logic. The design emphasizes predictable user flows, robust back stack management, and lifecycle-safe UI updates. Bottom navigation complements the graph by offering quick access to secondary destinations. The system balances usability and performance through careful state observation, adapter cleanup, and background processing. Extending the architecture involves adding destinations to the navigation graph and wiring fragment actions while preserving existing patterns.