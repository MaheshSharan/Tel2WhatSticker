# Tel2What — PRD Gap Analysis (gapprd.md)

Source PRD: d:\Android\Tel2WhatSticker\PRD.md

This document maps PRD requirements to the current implementation, highlights gaps/deviations, and proposes concrete refinements with code pointers.

---

## 1) Executive Summary

### Current State (What Works)
- Core end-to-end pipeline exists:
  - Telegram pack link → metadata fetch → batch download → WebP conversion → local pack storage → WhatsApp “enable sticker pack” intent.
- Screens implemented (9/10):
  - Splash, Home, Telegram Import, Download/Conversion, Selection, Tray Icon, Export, Manual Upload, Storage Management.

### Major Gaps (High Impact)
1. Onboarding flow (3 screens, first-launch gating) is missing.
2. Selection (3–30 stickers) is not applied to export/provider; WhatsApp may receive more than the selected set.
3. Telegram Bot token handling is insecure (hardcoded in code and present in tracked local.properties).
4. Download & Conversion flagship UX in PRD (ETA/speed/status progression, enable Continue when >=1 ready) is not implemented as specified.
5. Tray Icon custom upload is missing.
6. Build config references a missing proguard-rules.pro file (release build likely fails).

---

## 2) PRD-to-Code Gap Matrix

Legend:
- Status: ✅ Implemented | ⚠️ Partial | ❌ Missing | 🚫 Deviates
- Priority: P0 (blocker) | P1 (high) | P2 (medium) | P3 (low)

### 2.1 Product Identity & Constraints

**PRD:**
- Android minSdk 30, Kotlin, Single Activity + Fragments + Navigation Component.
- “Fully Offline (Client-Side Only) … no backend service.”

**Implementation:**
- ✅ minSdk 30: app/build.gradle.kts (minSdk = 30)
- ✅ Kotlin + Single Activity + Fragments + Navigation: MainActivity + nav_graph.
- ⚠️ “Fully Offline” wording vs behavior:
  - Telegram import requires internet access (Telegram Bot API + file downloads).
  - There is no Tel2What backend (matches “no backend”), but it is not offline-only.

**Gap / Recommendation (P2):**
- Clarify PRD wording: “No Tel2What backend; processing is on-device; internet is required for Telegram imports.”
- If PRD truly means offline for Telegram imports too, that conflicts with current approach.

---

## 3) Screens (PRD Section 4)

### 3.1 Splash Screen (PRD 4.1)

**PRD:**
- 1.5–2s, logo + tagline, subtle fade-in, no spinners unless needed.

**Implementation:**
- ✅ Duration ~1.5s and animations exist:
  - app/src/main/java/.../ui/splash/SplashFragment.kt
- ✅ Matches “no spinners.”

**Notes:**
- Tagline/version appear in layout; animation code exists.

Status: ✅ | Priority: —

---

### 3.2 Onboarding (PRD 4.2)

**PRD:**
- 3-screen onboarding shown only on first launch; Skip/Next/Get Started.

**Implementation:**
- ❌ No onboarding fragment(s) and no navigation route.
- Splash has commented shared-preference gating logic.

Status: ❌ | Priority: P0

**Work needed:**
- Create onboarding flow + persistence flag (SharedPreferences/DataStore).
- Add start logic: Splash → Onboarding on first launch else Home.

---

### 3.3 Home Screen (PRD 4.3)

**PRD:**
- Play Store-inspired layout, primary actions (Import, Manual), recent packs horizontal list.
- Top right icons: Storage, optional theme toggle.

**Implementation:**
- ✅ Primary actions and recent packs list:
  - fragment_home.xml + HomeFragment/HomeViewModel
- ✅ Storage icon navigates to storage screen.
- ❌ Theme toggle not implemented (optional).
- 🚫 Bottom navigation mock exists but is not wired to navigation and not in PRD:
  - bottom_nav_menu.xml; BottomNavigationView in fragment_home.xml.

Status: ⚠️ | Priority: P2

**Work needed:**
- Either implement bottom nav destinations (My Packs/Settings) or remove to match “Zero unnecessary screens.”
- Optional: add theme toggle if desired.

---

### 3.4 Telegram Import Screen (PRD 4.4)

**PRD:**
- Paste Telegram sticker link; validation indicator; pack preview; primary “Download First 30”.

**Implementation:**
- ✅ Paste icon, validation via error text, pack preview, “Download First 30” behavior:
  - TelegramImportFragment + TelegramImportViewModel

Status: ✅ | Priority: —

**Deviations:**
- Validation indicator is error-based (TextInputLayout error) but not a green check state.

---

### 3.5 Download & Conversion Screen (PRD 4.5 — Flagship UX)

**PRD:**
- Shows: pack name, 30/180, linear progress, ETA, download speed.
- Grid tiles: thumbnail + circular progress overlay + status labels (Downloading/Converting/Optimizing/Ready/Failed).
- Bottom: Download 30 More; Continue enabled once at least 1 ready.
- Incremental batches of 30 with user prompt each batch.

**Implementation:**
- ✅ Batch-of-30 logic exists in ConversionViewModel (downloadNextBatch).
- ✅ Bottom actions exist in fragment_download_conversion.xml.
- ⚠️ Status labels exist but abbreviated (“Down…”, “Conv…”, “Failed”) and no “Optimizing”.
- ❌ ETA and download speed not implemented.
- ❌ Continue gating differs:
  - Continue is enabled only when entire batch finishes (progress.isFinished), not when >=1 ready.
- ⚠️ Progress UI is not fully wired (comment in fragment says TextViews lack IDs).
- 🚫 Gradient used in bottom actions background (PRD says no gradients).

Status: ⚠️ | Priority: P1

**Critical correctness issue (P0):**
- ConversionViewModel inserts the entire in-memory sticker list to DB after each batch, likely duplicating rows across batches due to autoGenerate primary keys (StickerEntity id defaults to 0). This can corrupt “pack content” over time.

Work needed:
- Add IDs to progress TextViews + wire updates (downloaded/total, ETA, speed).
- Enable Continue when at least one item is READY.
- Implement/rename status phases; if “Optimizing” is a real phase, add it.
- Remove gradient usage to match PRD design.
- Fix DB insert logic to avoid duplicates.

---

### 3.6 Sticker Selection Screen (PRD 4.6)

**PRD:**
- Grid, selection indicator, hard limit 30, Continue.

**Implementation:**
- ✅ Hard cap at 30 enforced in SelectionViewModel.toggleSelection().
- ✅ UI shows “x / 30 selected” and Continue enabled for 3..30.

Status: ✅ | Priority: —

**Major integration gap (P0):**
- The selected sticker set is not enforced downstream at export/provider time (see Export/Provider section).

---

### 3.7 Tray Icon Selection Screen (PRD 4.7)

**PRD:**
- Choose tray icon from chosen stickers or upload custom image; auto-resize; preview.

**Implementation:**
- ✅ Choose from chosen stickers + preview.
- ❌ Upload custom image is missing.
- ✅ Auto-resize exists via ImageProcessor.processTrayIcon().

Status: ⚠️ | Priority: P1

Work needed:
- Add image picker for tray icon upload; validate and preview; persist tray.webp.

---

### 3.8 Export Screen (PRD 4.8)

**PRD:**
- Pack name editable, author name, preview, “Add to WhatsApp”, success animation.

**Implementation:**
- ✅ Pack name + author inputs exist; tray icon preview exists.
- ✅ Launches WhatsApp enable intent.
- ❌ Success animation not implemented.
- 🚫 Pack content not constrained to selected stickers; ExportViewModel counts READY stickers “in reality we'd filter by what's selected”.

Status: ⚠️ | Priority: P0

Work needed:
- Persist selected sticker IDs (or a “selected” flag) and ensure provider exposes only selected.
- Add success confirmation animation if required by PRD polish.

---

### 3.9 Manual Upload Screen (PRD 4.9)

**PRD:**
- Add images or GIFs, grid with conversion status + delete, then Continue to Selection.

**Implementation:**
- ✅ Add images (file picker), grid status, delete, process to WebP, navigate to selection.
- ❌ GIF support not implemented (picker is image/*; no GIF decode pipeline).
- ⚠️ After processing, it navigates to selection (good), but comment says “navigate to Export directly” while actually navigating to selection.

Status: ⚠️ | Priority: P2

Work needed:
- Either implement GIF support or update PRD scope to “images only” for V1.

---

### 3.10 Storage Management Screen (PRD 4.10)

**PRD:**
- List packs with storage used; per-pack clear cache; delete converted pack; clear all cache; confirmations required.

**Implementation:**
- ✅ Lists packs and computes directory size.
- ✅ Delete pack confirmation exists.
- ✅ “Delete all” confirmation exists.
- ⚠️ Clear cache is global; no confirmation dialog for clear cache.
- ❌ “Clear cache for pack” not implemented.

Status: ⚠️ | Priority: P2

Work needed:
- Add per-pack cache clearing (or define what “cache” means vs “converted pack”).
- Add confirmation for global clear cache.

---

## 4) Data Model & Export Correctness (WhatsApp Integration)

### 4.1 WhatsApp ContentProvider Contract
**Implementation:**
- Provider exports metadata and stickers:
  - StickerContentProvider.kt
- Metadata query returns all packs; stickers query returns all READY stickers for pack identifier.

**PRD requirement (implicit via selection flow):**
- Export should include only the selected 3–30 stickers.

**Current gap (P0):**
- Provider does not filter by selection; it filters only by status == READY.
- Export receives only packName and cannot indicate which stickers were selected.

**Work needed (P0):**
- Persist selection:
  - Option A: Add `isSelected` column on StickerEntity.
  - Option B: Store selected IDs in a separate table keyed by packId.
  - Option C: Store a pack “active subset” in pack metadata and filter by that.
- Update provider getStickers() to return only selected stickers.

---

## 5) Security & Privacy Gaps

### 5.1 Telegram Bot Token Exposure (P0)
**Observed:**
- Hardcoded token in source: TelegramBotApi.kt
- local.properties contains a token and is not ignored by .gitignore.
- app/build.gradle.kts reads local.properties into BuildConfig, but TelegramBotApi does not use BuildConfig token.

**Risks:**
- Token is extractable from APK if embedded.
- If repo is shared, token is already compromised.

**Work needed (P0):**
- Remove hardcoded token from source and rotate the token.
- Ignore local.properties in .gitignore.
- Decide product approach:
  - If “no backend” is strict: consider user-provided token (still risky UX), or alternative import approach.
  - If acceptable: use a backend proxy to keep tokens server-side (conflicts with PRD as written).

---

## 6) Performance & Scalability Gaps (PRD Section 6)

**PRD:**
- No UI thread blocking, background coroutines, controlled parallel conversion pool, memory safe handling.

**Implementation:**
- ✅ Coroutines and Dispatchers.IO used for download/conversion.
- ✅ Concurrency cap at 4 exists in ConversionViewModel.

**Gaps:**
- ⚠️ Progress updates are not concurrency-safe (shared counter increment across async jobs).
- ⚠️ Bitmap decoding does not downsample; large input images may cause OOM.
- ⚠️ OkHttpClient is created multiple times across fragments/classes.

Priority: P1/P2

Work needed:
- Use atomic counters or channel-based progress aggregation.
- Decode bitmaps with inSampleSize or stream-based resizing.
- Reuse a single OkHttpClient instance.

---

## 7) Build & Configuration Gaps

### 7.1 Missing ProGuard Rules File (P0)
- app/build.gradle.kts references "proguard-rules.pro" but file does not exist.
- Release build with minifyEnabled=true is likely broken.

Work needed:
- Add proguard-rules.pro or remove reference / disable minify until rules exist.

### 7.2 Unused Dependency (P3)
- jsoup is declared but appears unused.

Work needed:
- Remove dependency if not needed.

---

## 8) Design System Deviations (PRD Section 3)

**PRD:**
- No gradients, muted green accent, Play Store polish, generous whitespace.

**Implementation issues:**
- 🚫 Gradient exists: bg_bottom_gradient.xml used on download screen.
- 🚫 Accent green is very bright (#1AE07A) and may violate “muted” requirement.

Priority: P2

Work needed:
- Replace gradient with solid surface.
- Adjust color palette to a more muted green.

---

## 9) Recommended Fix Order (Minimal Risk Path)

P0 (Blockers)
- Fix export correctness: enforce selection in DB/provider/export.
- Remove hardcoded token + rotate token + stop tracking local.properties.
- Fix ConversionViewModel DB insert duplication issue.
- Fix release build by adding/removing proguard-rules.pro reference.

P1 (High)
- Download screen: enable Continue when >=1 READY; wire proper progress text; implement basic speed/ETA.
- Tray icon custom upload.

P2 (Medium)
- Onboarding flow.
- Storage per-pack cache clear + confirmations.
- Muted palette + remove gradients.

P3 (Low)
- Remove unused deps (jsoup) and dead bottom-nav or fully implement it.

---

## 10) Concrete Code Pointers (Where Each Gap Lives)

- Onboarding missing:
  - ui/splash/SplashFragment.kt (commented gating logic)
  - nav_graph.xml (no onboarding destination)
- Selection not applied to export:
  - ui/selection/StickerSelectionFragment.kt (selectedIds passed)
  - ui/export/ExportFragment.kt (selectedIds not used)
  - provider/StickerContentProvider.kt (returns all READY stickers)
- Token insecurity:
  - data/network/TelegramBotApi.kt (hardcoded botToken)
  - local.properties (token present)
  - .gitignore (does not ignore local.properties)
- Conversion batch + DB duplication:
  - ui/conversion/ConversionViewModel.kt (insertStickers(_stickers.value))
  - data/local/entity/StickerEntity.kt (autoGenerate id)
- Download UX wiring:
  - ui/conversion/DownloadConversionFragment.kt (comment about missing IDs)
  - res/layout/fragment_download_conversion.xml (text views without IDs + gradient)
- Tray icon upload missing:
  - ui/trayicon/TrayIconSelectionFragment.kt (no picker)
- Storage per-pack cache clear missing:
  - ui/storage/StorageManagementFragment.kt (global clear cache only)
- Proguard rules missing:
  - app/build.gradle.kts (references proguard-rules.pro)

---