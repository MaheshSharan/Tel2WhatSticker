# Overall System Architecture

<cite>
**Referenced Files in This Document**
- [MainActivity.kt](file://app/src/main/java/com/maheshsharan/tel2what/MainActivity.kt)
- [activity_main.xml](file://app/src/main/res/layout/activity_main.xml)
- [nav_graph.xml](file://app/src/main/res/navigation/nav_graph.xml)
- [Tel2WhatApplication.kt](file://app/src/main/java/com/maheshsharan/tel2what/Tel2WhatApplication.kt)
- [HomeFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/home/HomeFragment.kt)
- [HomeViewModel.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/home/HomeViewModel.kt)
- [ConversionViewModel.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/conversion/ConversionViewModel.kt)
- [StickerRepository.kt](file://app/src/main/java/com/maheshsharan/tel2what/data/repository/StickerRepository.kt)
- [AppDatabase.kt](file://app/src/main/java/com/maheshsharan/tel2what/data/local/AppDatabase.kt)
- [TelegramBotApi.kt](file://app/src/main/java/com/maheshsharan/tel2what/data/network/TelegramBotApi.kt)
- [StickerConversionEngine.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/StickerConversionEngine.kt)
- [WhatsAppStickerValidator.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/WhatsAppStickerValidator.kt)
- [AndroidManifest.xml](file://app/src/main/AndroidManifest.xml)
- [README.md](file://README.md)
- [privacy-policy.html](file://docs/privacy-policy.html)
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
This document describes the overall system architecture of Tel2What, a Telegram to WhatsApp sticker converter. The application follows a single-activity architecture with fragment-based navigation, an MVVM pattern with ViewModels managing UI state and business logic, and a layered design separating UI, business logic, and data. It emphasizes offline-first processing and privacy-focused operations, performing all conversions and validations locally on-device.

## Project Structure
The project is organized into distinct layers:
- UI layer: Activities and Fragments with Navigation Component
- Presentation layer: ViewModels implementing MVVM
- Domain/business layer: Repository coordinating business logic
- Data layer: Room database, Telegram APIs, and file/network utilities
- Engine layer: Native and specialized media processing pipelines
- Utilities: Shared helpers and validators

```mermaid
graph TB
subgraph "UI Layer"
MA["MainActivity<br/>Single Activity"]
NAV["NavHostFragment<br/>Navigation Graph"]
FRAG_HOME["HomeFragment"]
FRAG_CONV["ConversionFragment(s)"]
end
subgraph "Presentation Layer"
VM_HOME["HomeViewModel"]
VM_CONV["ConversionViewModel"]
end
subgraph "Domain Layer"
REPO["StickerRepository"]
end
subgraph "Data Layer"
DB["Room AppDatabase"]
DAO["StickerDao"]
API["TelegramBotApi"]
DL["FileDownloader"]
end
subgraph "Engine Layer"
ENG["StickerConversionEngine"]
DEC["Decoders (TGS/WebM)"]
ENC["AnimatedWebpEncoder"]
VAL["WhatsAppStickerValidator"]
end
MA --> NAV
NAV --> FRAG_HOME
NAV --> FRAG_CONV
FRAG_HOME --> VM_HOME
FRAG_CONV --> VM_CONV
VM_HOME --> REPO
VM_CONV --> REPO
REPO --> DAO
REPO --> API
REPO --> DL
DAO --> DB
VM_CONV --> ENG
ENG --> DEC
ENG --> ENC
ENG --> VAL
```

**Diagram sources**
- [MainActivity.kt](file://app/src/main/java/com/maheshsharan/tel2what/MainActivity.kt#L6-L12)
- [activity_main.xml](file://app/src/main/res/layout/activity_main.xml#L8-L14)
- [nav_graph.xml](file://app/src/main/res/navigation/nav_graph.xml#L1-L122)
- [HomeFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/home/HomeFragment.kt#L20-L92)
- [HomeViewModel.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/home/HomeViewModel.kt#L8-L14)
- [ConversionViewModel.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/conversion/ConversionViewModel.kt#L39-L442)
- [StickerRepository.kt](file://app/src/main/java/com/maheshsharan/tel2what/data/repository/StickerRepository.kt#L10-L79)
- [AppDatabase.kt](file://app/src/main/java/com/maheshsharan/tel2what/data/local/AppDatabase.kt#L13-L41)
- [TelegramBotApi.kt](file://app/src/main/java/com/maheshsharan/tel2what/data/network/TelegramBotApi.kt#L14-L111)
- [StickerConversionEngine.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/StickerConversionEngine.kt#L17-L274)

**Section sources**
- [README.md](file://README.md#L98-L110)

## Core Components
- Single-Activity Container: MainActivity inflates a FragmentContainerView hosting NavHostFragment.
- Navigation Graph: nav_graph.xml defines the navigation flow among fragments.
- Fragment Responsibilities: Each fragment initializes its ViewModel via a Factory and observes state via Flows.
- MVVM: ViewModels expose StateFlow/Flow to the UI and encapsulate business logic.
- Repository Pattern: StickerRepository mediates between DAO, Telegram API, and FileDownloader.
- Local-Only Processing: Room database stores sticker packs and metadata; conversion runs entirely on-device.

**Section sources**
- [MainActivity.kt](file://app/src/main/java/com/maheshsharan/tel2what/MainActivity.kt#L6-L12)
- [activity_main.xml](file://app/src/main/res/layout/activity_main.xml#L8-L14)
- [nav_graph.xml](file://app/src/main/res/navigation/nav_graph.xml#L1-L122)
- [HomeFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/home/HomeFragment.kt#L20-L92)
- [HomeViewModel.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/home/HomeViewModel.kt#L8-L24)
- [ConversionViewModel.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/conversion/ConversionViewModel.kt#L39-L442)
- [StickerRepository.kt](file://app/src/main/java/com/maheshsharan/tel2what/data/repository/StickerRepository.kt#L10-L79)
- [AppDatabase.kt](file://app/src/main/java/com/maheshsharan/tel2what/data/local/AppDatabase.kt#L13-L41)

## Architecture Overview
Tel2What employs a layered architecture:
- UI Layer: Fragments and Activities driven by Navigation Component
- Presentation Layer: ViewModels manage UI state and orchestrate work
- Domain Layer: Repository coordinates data access and business rules
- Data Layer: Room for persistence, TelegramBotApi for remote metadata and files
- Engine Layer: Specialized decoders, encoders, and validators for media processing

```mermaid
graph TB
UI["UI Layer<br/>Fragments + Navigation"] --> VM["Presentation Layer<br/>ViewModels"]
VM --> DOMAIN["Domain Layer<br/>Repository"]
DOMAIN --> DATA["Data Layer<br/>Room + Telegram API"]
DATA --> ENGINE["Engine Layer<br/>Decoders + Encoder + Validator"]
ENGINE --> DATA
```

**Diagram sources**
- [HomeFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/home/HomeFragment.kt#L20-L92)
- [HomeViewModel.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/home/HomeViewModel.kt#L8-L14)
- [ConversionViewModel.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/conversion/ConversionViewModel.kt#L39-L442)
- [StickerRepository.kt](file://app/src/main/java/com/maheshsharan/tel2what/data/repository/StickerRepository.kt#L10-L79)
- [AppDatabase.kt](file://app/src/main/java/com/maheshsharan/tel2what/data/local/AppDatabase.kt#L13-L41)
- [TelegramBotApi.kt](file://app/src/main/java/com/maheshsharan/tel2what/data/network/TelegramBotApi.kt#L14-L111)
- [StickerConversionEngine.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/StickerConversionEngine.kt#L17-L274)

## Detailed Component Analysis

### Single-Activity and Fragment-Based Navigation
- MainActivity hosts a FragmentContainerView configured as NavHost with nav_graph.
- nav_graph.xml defines the start destination and actions between fragments.
- Fragments observe ViewModel state and navigate declaratively via NavController.

```mermaid
sequenceDiagram
participant Act as "MainActivity"
participant Host as "NavHostFragment"
participant Graph as "nav_graph.xml"
participant Home as "HomeFragment"
participant VM as "HomeViewModel"
Act->>Host : Set content view
Host->>Graph : Load navigation graph
Graph-->>Home : Navigate to start destination
Home->>VM : Initialize ViewModel via Factory
VM-->>Home : Expose Flow of recent packs
Home->>Home : Observe Flow and render
```

**Diagram sources**
- [MainActivity.kt](file://app/src/main/java/com/maheshsharan/tel2what/MainActivity.kt#L6-L12)
- [activity_main.xml](file://app/src/main/res/layout/activity_main.xml#L8-L14)
- [nav_graph.xml](file://app/src/main/res/navigation/nav_graph.xml#L1-L122)
- [HomeFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/home/HomeFragment.kt#L20-L92)
- [HomeViewModel.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/home/HomeViewModel.kt#L8-L14)

**Section sources**
- [MainActivity.kt](file://app/src/main/java/com/maheshsharan/tel2what/MainActivity.kt#L6-L12)
- [activity_main.xml](file://app/src/main/res/layout/activity_main.xml#L8-L14)
- [nav_graph.xml](file://app/src/main/res/navigation/nav_graph.xml#L1-L122)

### MVVM Implementation with ViewModels
- HomeViewModel exposes a Flow of sticker packs from the repository and sorts them by date.
- ConversionViewModel manages conversion progress, batch processing, and state updates.
- Both use ViewModelProvider.Factory for construction and lifecycle-aware coroutine scopes.

```mermaid
classDiagram
class HomeViewModel {
+recentPacks : Flow<List<Pack>>
-repository : StickerRepository
}
class ConversionViewModel {
+progressData : StateFlow<ConversionProgress>
+stickers : StateFlow<List<Sticker>>
-repository : StickerRepository
-conversionEngine : StickerConversionEngine
+initAndStart(...)
+downloadNextBatch()
+stopConversion()
}
class StickerRepository {
+getAllPacks() : Flow<List<Pack>>
+fetchTelegramPackMetadata(...)
+downloadBinary(...)
+insertPack(...)
+updateStickerStatus(...)
}
HomeViewModel --> StickerRepository : "uses"
ConversionViewModel --> StickerRepository : "uses"
```

**Diagram sources**
- [HomeViewModel.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/home/HomeViewModel.kt#L8-L24)
- [ConversionViewModel.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/conversion/ConversionViewModel.kt#L39-L442)
- [StickerRepository.kt](file://app/src/main/java/com/maheshsharan/tel2what/data/repository/StickerRepository.kt#L10-L79)

**Section sources**
- [HomeViewModel.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/home/HomeViewModel.kt#L8-L24)
- [ConversionViewModel.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/conversion/ConversionViewModel.kt#L39-L442)

### Layered Architecture: UI, Business Logic, Data
- UI: Fragments inflate layouts and bind to ViewModels.
- Business Logic: Repository encapsulates domain rules and orchestrates data sources.
- Data: Room DAO persists packs and stickers; TelegramBotApi retrieves metadata and files.

```mermaid
graph LR
UI["Fragments"] --> VM["ViewModels"]
VM --> Repo["Repository"]
Repo --> DAO["Room DAO"]
Repo --> API["TelegramBotApi"]
DAO --> DB["Room Database"]
Repo --> DL["FileDownloader"]
```

**Diagram sources**
- [HomeFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/home/HomeFragment.kt#L20-L92)
- [StickerRepository.kt](file://app/src/main/java/com/maheshsharan/tel2what/data/repository/StickerRepository.kt#L10-L79)
- [AppDatabase.kt](file://app/src/main/java/com/maheshsharan/tel2what/data/local/AppDatabase.kt#L13-L41)
- [TelegramBotApi.kt](file://app/src/main/java/com/maheshsharan/tel2what/data/network/TelegramBotApi.kt#L14-L111)

**Section sources**
- [HomeFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/home/HomeFragment.kt#L20-L92)
- [StickerRepository.kt](file://app/src/main/java/com/maheshsharan/tel2what/data/repository/StickerRepository.kt#L10-L79)
- [AppDatabase.kt](file://app/src/main/java/com/maheshsharan/tel2what/data/local/AppDatabase.kt#L13-L41)
- [TelegramBotApi.kt](file://app/src/main/java/com/maheshsharan/tel2what/data/network/TelegramBotApi.kt#L14-L111)

### Conversion Workflow and Media Processing
- ConversionViewModel drives batched downloads and conversions.
- StickerConversionEngine selects pipeline based on input type and pack characteristics.
- WhatsAppStickerValidator enforces size and dimension constraints.

```mermaid
sequenceDiagram
participant UI as "Conversion UI"
participant VM as "ConversionViewModel"
participant Repo as "StickerRepository"
participant API as "TelegramBotApi"
participant Eng as "StickerConversionEngine"
participant Val as "WhatsAppStickerValidator"
UI->>VM : initAndStart(packName, packTitle)
VM->>Repo : fetchTelegramPackMetadata(packName)
Repo->>API : getStickerSet(name)
API-->>Repo : TelegramStickerSet
Repo-->>VM : Result<TelegramStickerSet>
loop Batch Download + Convert
VM->>Repo : insertStickers(placeholders)
VM->>Repo : fetchFilePath(fileId)
Repo->>API : getFile(file_id)
API-->>Repo : filePath
Repo-->>VM : filePath
VM->>Repo : getDownloadUrl(filePath)
VM->>Repo : downloadBinary(url, cacheFile)
VM->>Eng : convertSticker(input, output, isAnimated)
Eng->>Val : validateOutput(file, isAnimated, config)
Val-->>Eng : Validation result
Eng-->>VM : Success/Failed/ValidationFailed
VM->>Repo : updateStickerStatus(...)
end
```

**Diagram sources**
- [ConversionViewModel.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/conversion/ConversionViewModel.kt#L66-L329)
- [StickerRepository.kt](file://app/src/main/java/com/maheshsharan/tel2what/data/repository/StickerRepository.kt#L24-L30)
- [TelegramBotApi.kt](file://app/src/main/java/com/maheshsharan/tel2what/data/network/TelegramBotApi.kt#L22-L111)
- [StickerConversionEngine.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/StickerConversionEngine.kt#L33-L88)
- [WhatsAppStickerValidator.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/WhatsAppStickerValidator.kt#L14-L70)

**Section sources**
- [ConversionViewModel.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/conversion/ConversionViewModel.kt#L66-L329)
- [StickerConversionEngine.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/StickerConversionEngine.kt#L33-L88)
- [WhatsAppStickerValidator.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/WhatsAppStickerValidator.kt#L14-L70)

### Offline-First Design and Privacy
- Offline-first: After initial Telegram metadata and file retrieval, subsequent operations (conversion, validation, selection, export) run locally.
- Privacy: The app does not collect personal data, operates without analytics, and keeps all processed stickers on-device.
- Manifest permissions are minimal and scoped to core functionality.

```mermaid
flowchart TD
Start(["User Action"]) --> Import["Import Telegram Pack"]
Import --> Download["Download Metadata + Files"]
Download --> Local["Local Conversion & Validation"]
Local --> Store["Store on Device"]
Store --> Use["Select and Export to WhatsApp"]
Use --> End(["Done"])
Privacy["No cloud upload<br/>No analytics<br/>No personal data"] --> Local
```

**Diagram sources**
- [README.md](file://README.md#L11-L19)
- [privacy-policy.html](file://docs/privacy-policy.html#L46-L69)
- [AndroidManifest.xml](file://app/src/main/AndroidManifest.xml#L3-L9)

**Section sources**
- [README.md](file://README.md#L11-L19)
- [privacy-policy.html](file://docs/privacy-policy.html#L46-L69)
- [AndroidManifest.xml](file://app/src/main/AndroidManifest.xml#L3-L9)

## Dependency Analysis
Key dependencies and their roles:
- Room: Local persistence for sticker packs and stickers
- OkHttp: Network client for Telegram API
- Kotlin Coroutines + Flow: Reactive state management in ViewModels
- Navigation Component: Declarative navigation between fragments
- StrictMode: Development-time strictness for detecting violations

```mermaid
graph LR
VM["ViewModels"] --> Repo["Repository"]
Repo --> DB["Room"]
Repo --> Net["OkHttp"]
UI["Fragments"] --> Nav["Navigation Component"]
App["Application"] --> Strict["StrictMode"]
```

**Diagram sources**
- [AppDatabase.kt](file://app/src/main/java/com/maheshsharan/tel2what/data/local/AppDatabase.kt#L13-L41)
- [TelegramBotApi.kt](file://app/src/main/java/com/maheshsharan/tel2what/data/network/TelegramBotApi.kt#L14-L111)
- [Tel2WhatApplication.kt](file://app/src/main/java/com/maheshsharan/tel2what/Tel2WhatApplication.kt#L7-L44)
- [HomeFragment.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/home/HomeFragment.kt#L20-L92)

**Section sources**
- [AppDatabase.kt](file://app/src/main/java/com/maheshsharan/tel2what/data/local/AppDatabase.kt#L13-L41)
- [TelegramBotApi.kt](file://app/src/main/java/com/maheshsharan/tel2what/data/network/TelegramBotApi.kt#L14-L111)
- [Tel2WhatApplication.kt](file://app/src/main/java/com/maheshsharan/tel2what/Tel2WhatApplication.kt#L7-L44)

## Performance Considerations
- Concurrency control: Semaphores limit parallel animated processing to prevent thermal throttling and OOM; static processing allows higher concurrency.
- Adaptive compression: Animated pipeline reduces FPS or quality to meet size targets.
- Memory hygiene: Bitmaps are recycled promptly; caches are cleaned post-processing.
- Native encoding: JNI-backed libwebp accelerates WebP encoding for fast conversion times.

[No sources needed since this section provides general guidance]

## Troubleshooting Guide
Common issues and mitigations:
- Telegram API errors: Invalid token, rate limits, or missing packs. The API layer surfaces descriptive errors for 401/404/400 cases.
- Network connectivity: UnknownHostException is handled gracefully with user-facing messages.
- Conversion failures: ConversionViewModel and engine log detailed reasons; validation failures return explicit messages.
- Lifecycle: ViewModels cancel child coroutines on cleared; ensure UI observes flows within lifecycle owners.

**Section sources**
- [TelegramBotApi.kt](file://app/src/main/java/com/maheshsharan/tel2what/data/network/TelegramBotApi.kt#L34-L73)
- [ConversionViewModel.kt](file://app/src/main/java/com/maheshsharan/tel2what/ui/conversion/ConversionViewModel.kt#L408-L441)
- [StickerConversionEngine.kt](file://app/src/main/java/com/maheshsharan/tel2what/engine/StickerConversionEngine.kt#L266-L273)

## Conclusion
Tel2What’s architecture centers on a single-activity, fragment-driven UI with MVVM and a layered design. The system is offline-first and privacy-focused, performing all processing locally while leveraging a robust media conversion pipeline. The modular structure, combined with reactive state management and strict lifecycle handling, yields a responsive, maintainable, and trustworthy application.