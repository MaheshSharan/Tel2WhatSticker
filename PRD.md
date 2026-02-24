# Tel2What – Product Requirements Document (PRD)

## 1. Product Identity

**App Name:** Tel2What
**Repository:** [https://github.com/MaheshSharan/Tel2WhatSticker](https://github.com/MaheshSharan/Tel2WhatSticker)
**Platform:** Android (Minimum SDK: Android 11 / API 30)
**Architecture:** Fully Offline (Client-Side Only)
**Language:** Kotlin (Single Activity + Fragments + Navigation Component)

Tel2What is a high-performance Android utility app that converts Telegram sticker packs into WhatsApp-compatible sticker packs. The app performs all downloading and conversion locally on the user’s device, with no backend service.

Design philosophy:

- Clean
- Structured
- Professional
- Play Store–level UI polish
- Zero ads
- Zero unnecessary screens

No editing tools in V1.

---

# 2. Total Screens Overview

Total Screens (V1): **10 Screens**

1. Splash Screen
2. Onboarding (3 screens flow)
3. Home Screen
4. Telegram Import Screen
5. Download & Conversion Screen
6. Sticker Selection Screen
7. Tray Icon Selection Screen
8. Export Screen
9. Manual Upload Screen
10. Storage Management Screen

All implemented using:
Single Activity + Fragments + Navigation Graph.

---

# 3. UI/UX Design System (Play Store Inspired)

We are following a design language inspired by Google Play Store:

## 3.1 Theme Support

- Light + Dark mode from V1
- Automatic system theme detection

## 3.2 Color System

Primary:

- Neutral surface background
- Clean whites (Light mode)
- Deep charcoal/near-black (Dark mode)

Accent:

- Subtle green (inspired by WhatsApp tone but muted)
- Used only for:
  - Primary buttons
  - Progress highlights
  - Selected states

No neon colors.
No gradients.
No playful cartoon visuals.

## 3.3 Typography

Use:

- Google Sans or Inter

Hierarchy:

- Large Titles: Bold
- Section Titles: SemiBold
- Body: Regular
- Metadata: Medium / 14sp

Spacing:

- 8dp spacing grid
- Generous white space
- Rounded corners (12dp–16dp)

## 3.4 Components

Buttons:

- Rounded 16dp
- Filled primary button
- Outlined secondary button

Cards:

- Elevated 2dp
- Subtle shadow in light mode
- Soft surface tint in dark mode

Progress Indicators:

- Circular for per-sticker
- Linear for pack-level progress

---

# 4. Screen-by-Screen Breakdown

## 4.1 Splash Screen

Duration: 1.5–2 seconds

UI:

- Centered Tel2What logo
- Minimal tagline: "Telegram to WhatsApp Stickers"
- Subtle fade-in animation

Background:

- Solid theme surface color

No loading spinners unless initialization required.

---

## 4.2 Onboarding (3 Screens)

### Screen 1 – Import Made Easy

- Illustration (minimal flat vector)
- Text: "Import any Telegram sticker pack"
- Subtext explaining link usage

### Screen 2 – Fast & Offline

- Icon showing download + device
- Text: "All processing happens on your device"

### Screen 3 – WhatsApp Ready

- WhatsApp style preview mock
- Text: "Select up to 30 and export instantly"

Bottom:

- Skip
- Next
- Get Started (on final screen)

Only shown first launch.

---

## 4.3 Home Screen

Structure inspired by Play Store:

Top App Bar:

- App name: Tel2What
- Right side icons:
  - Storage
  - Theme toggle (optional small icon)

Main Content:

Section 1: Primary Actions (Card Layout)

- Import from Telegram (large primary button card)
- Manual Upload (secondary card)

Section 2: Recent Packs

- Horizontal scroll list
- Pack tray icon
- Pack name
- Sticker count

Spacing and layout similar to Play Store sections.

---

## 4.4 Telegram Import Screen

Top Bar:

- Back button
- Title: "Import from Telegram"

Content:

Input Field:

- Rounded text input
- Hint: "Paste Telegram sticker link"
- Validation indicator (green check / red error)

Below:

- Pack preview (after fetch)
  - Pack name
  - Total stickers count

Primary Button:

- "Download First 30"

---

## 4.5 Download & Conversion Screen

This is flagship UX screen.

Top Section:

- Pack name
- Total stickers downloaded (e.g., 30 / 180)
- Linear progress bar
- ETA
- Download speed

Grid Section:

- 3-column grid
- Each tile:
  - Rounded 12dp
  - Thumbnail preview
  - Overlay circular progress
  - Status label at bottom:
    - Downloading
    - Converting
    - Optimizing
    - Ready
    - Failed

Bottom Action Bar:

- Download 30 More
- Continue to Selection (enabled once at least 1 ready)

Incremental logic:

- Download in batches of 30
- User prompted after each batch

---

## 4.6 Sticker Selection Screen

Grid Layout:

- 3 or 4 columns
- Animated preview auto-loop

Selection Indicator:

- Checkbox overlay
- Selected count at top: "12 / 30 selected"

Hard limit:

- Cannot exceed 30

Bottom Button:

- Continue

---

## 4.7 Tray Icon Selection Screen

Header:

- Title: "Choose Tray Icon"

Options:

- Select from chosen stickers (horizontal list)
- Upload custom image

Validation:

- Auto resize to required dimension
- Show preview before continue

Primary Button:

- Continue

---

## 4.8 Export Screen

Fields:

- Pack Name (editable)
- Author Name

Preview Section:

- Tray icon preview
- Sticker preview grid

Primary Button:

- Add to WhatsApp

On success:

- Success confirmation animation

---

## 4.9 Manual Upload Screen

Top:

- "Add Images or GIFs"

File picker

Grid preview with:

- Conversion status
- Delete option

After processing:

- Continue to Selection

---

## 4.10 Storage Management Screen

List of packs:

- Pack name
- Storage used
- Sticker count

Options:

- Clear cache for pack
- Delete converted pack
- Clear all cache

Confirmation dialog required.

---

# 5. Functional Flow Summary

Telegram Flow:
Home → Telegram Import → Download → Selection → Tray → Export

Manual Flow:
Home → Manual Upload → Selection → Tray → Export

Storage accessible from Home.

---

# 6. Performance Requirements

- No UI thread blocking
- Background processing using Coroutines
- Controlled parallel conversion pool
- Memory safe animated frame handling

---

# 7. Out of Scope (V1)

- Editing tools
- Background removal
- Cloud sync
- Ads
- Monetization
- Analytics

---

End of PRD – Tel2What V1
